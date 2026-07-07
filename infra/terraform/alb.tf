# -----------------------------------
# ALB + ASG — multi-AZ high availability for the app tier
#
# Cost/architecture tradeoff, stated explicitly: app instances stay in the
# public subnets (public_subnet_a/b) rather than moving to private subnets,
# to avoid the hourly+data cost of a NAT Gateway. Isolation is enforced via
# security-group chaining instead of network topology: the ALB's SG accepts
# 80 from the internet, the app's SG accepts 8080 ONLY from the ALB's SG (and
# from the monthly-report Lambda's SG), so instances are not directly
# reachable on the app port even though they have public IPs. A production
# deployment should move these instances into private subnets behind a NAT
# Gateway or VPC endpoints for defense in depth.
# -----------------------------------

resource "aws_security_group" "alb_sg" {
  name        = "bank-alb-sg"
  description = "ALB security group - public HTTP in, forwards to the app tier"
  vpc_id      = aws_vpc.bank_vpc.id
}

resource "aws_vpc_security_group_ingress_rule" "alb_http" {
  security_group_id = aws_security_group.alb_sg.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "alb_all" {
  security_group_id = aws_security_group.alb_sg.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

resource "aws_security_group_rule" "app_from_alb" {
  type                     = "ingress"
  security_group_id        = aws_security_group.bank_ec2_group.id
  source_security_group_id = aws_security_group.alb_sg.id
  from_port                = 8080
  to_port                  = 8080
  protocol                 = "tcp"
}

resource "aws_lb" "bank_alb" {
  name               = "bank-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_sg.id]
  subnets            = [aws_subnet.public_subnet_a.id, aws_subnet.public_subnet_b.id]

  tags = {
    Name = "bank-alb"
  }
}

resource "aws_lb_target_group" "bank_tg" {
  name        = "bank-app-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.bank_vpc.id
  target_type = "instance"

  health_check {
    path     = "/actuator/health"
    matcher  = "200"
    interval = 30
    # /actuator/health's DB indicator calls HikariCP, which occasionally takes
    # 8-10s to respond right after connection churn (observed live during an
    # RDS Multi-AZ failover: HikariCP had to detect and evict stale connections
    # before it could validate a fresh one). 5s was too tight and flapped a
    # perfectly healthy instance to "unhealthy," failing an ASG instance
    # refresh. 10s leaves headroom without approaching the 30s interval.
    timeout             = 10
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  # Short deregistration delay so `terraform apply`/instance-refresh cycles
  # during verification don't hang around waiting out the 300s default.
  deregistration_delay = 30

  tags = {
    Name = "bank-app-tg"
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.bank_alb.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.bank_tg.arn
  }
}

# -----------------------------------
# Launch template + Auto Scaling Group (replaces the single aws_instance)
# -----------------------------------

resource "aws_launch_template" "bank_server_lt" {
  name_prefix   = "bank-server-"
  image_id      = var.ami_id
  instance_type = var.ec2_instance_type
  key_name      = var.key_pair

  iam_instance_profile {
    name = aws_iam_instance_profile.ec2_profile.name
  }

  vpc_security_group_ids = [aws_security_group.bank_ec2_group.id]

  user_data = base64encode(templatefile("${path.module}/docker.sh", {
    aws_region           = var.aws_region
    ecr_repo_uri         = var.image_uri
    db_url               = "jdbc:postgresql://${local.db_address}:${local.db_port}/${local.db_name}"
    db_username          = local.db_username
    db_password          = local.db_password
    sqs_queue_url        = aws_sqs_queue.account_credit_events.id
    jwt_secret           = var.jwt_secret
    jwt_expiration       = var.jwt_expiration
    hibernate_ddl_auto   = var.hibernate_ddl_auto
    internal_api_key     = var.internal_api_key
    cloudwatch_log_group = aws_cloudwatch_log_group.app_logs.name
  }))

  tag_specifications {
    resource_type = "instance"
    tags = {
      Name = "bank-app-server"
    }
  }

  update_default_version = true

  depends_on = [aws_db_instance.banking_rds]
}

resource "aws_autoscaling_group" "bank_asg" {
  name                = "bank-app-asg"
  min_size            = var.asg_min_size
  max_size            = var.asg_max_size
  desired_capacity    = var.asg_desired_capacity
  vpc_zone_identifier = [aws_subnet.public_subnet_a.id, aws_subnet.public_subnet_b.id]
  target_group_arns   = [aws_lb_target_group.bank_tg.arn]

  health_check_type = "ELB"
  # 60s was too short — the full cold-start pipeline (dnf update, install
  # Docker, ECR login + pull a ~190MB image, THEN the ~20s Spring Boot
  # startup) regularly exceeds it on a t3.micro, so the ALB starts polling
  # and marking the instance unhealthy before the app is even listening,
  # which then makes the ASG kill and replace it — discovered live when two
  # separate instance-refresh attempts both failed on a freshly-launched
  # replacement that was, in fact, never actually broken.
  health_check_grace_period = 300

  launch_template {
    id      = aws_launch_template.bank_server_lt.id
    version = "$Latest"
  }

  # ASG-native equivalent of aws_instance's user_data_replace_on_change: when
  # the launch template gets a new version (new image_id/user_data), roll
  # running instances onto it instead of only applying to future launches.
  #
  # `triggers = ["launch_template"]` IS required here, despite `terraform
  # validate` warning it's redundant — that warning only holds when `version`
  # below references the launch template's computed `.latest_version`
  # attribute, so a new LT version shows up as a diff on this resource itself.
  # We use the literal string "$Latest" instead (so instances always launch
  # from whatever is newest without a second apply), which means Terraform
  # sees no diff on this resource when the LT gets a new default version —
  # discovered during live verification: pushing a new app image updated the
  # launch template's default version correctly, but the running ASG
  # instances were never replaced without this explicit trigger.
  instance_refresh {
    strategy = "Rolling"
    preferences {
      min_healthy_percentage = 50
    }
    triggers = ["launch_template"]
  }

  tag {
    key                 = "Name"
    value               = "bank-app-server"
    propagate_at_launch = true
  }
}
