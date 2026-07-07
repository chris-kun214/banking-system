# -----------------------------------
# Lambda: daily reconciliation + monthly report trigger
# -----------------------------------
# Both functions only need VPC access (ENI create/describe/delete) + basic
# CloudWatch Logs — daily_reconciliation talks to RDS directly over the VPC,
# monthly_report calls the app's internal HTTP endpoint over the VPC, and
# neither calls any other AWS API, so one shared execution role covers both
# and no NAT Gateway / VPC endpoint is required.
#
# (During live verification, monthly_report was briefly changed to look up a
# healthy instance's IP via elbv2/ec2 describe calls instead of hitting the
# ALB directly — reverted, because those describe calls need the AWS control
# plane, which is unreachable from this NAT-less private subnet. Calling the
# ALB is the only approach here that needs zero AWS API access.)

resource "aws_iam_role" "lambda_execution_role" {
  name = "bank-lambda-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action    = "sts:AssumeRole"
        Effect    = "Allow"
        Principal = { Service = "lambda.amazonaws.com" }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_vpc_access" {
  role       = aws_iam_role.lambda_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
}

# -----------------------------------
# Security groups
# -----------------------------------

resource "aws_security_group" "lambda_recon_sg" {
  name        = "bank-lambda-recon-sg"
  description = "Daily reconciliation Lambda ENIs - egress only, needs to reach RDS on 5432"
  vpc_id      = aws_vpc.bank_vpc.id
}

resource "aws_vpc_security_group_egress_rule" "lambda_recon_all" {
  security_group_id = aws_security_group.lambda_recon_sg.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

resource "aws_security_group" "lambda_report_sg" {
  name        = "bank-lambda-report-sg"
  description = "Monthly report Lambda ENIs - egress only, needs to reach the app on 8080"
  vpc_id      = aws_vpc.bank_vpc.id
}

resource "aws_vpc_security_group_egress_rule" "lambda_report_all" {
  security_group_id = aws_security_group.lambda_report_sg.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

# Separate aws_security_group_rule resources (not inline blocks on the two SGs
# above) so the RDS/app security groups can reference the Lambda SGs without a
# circular dependency between them.
resource "aws_security_group_rule" "rds_from_lambda_recon" {
  type                     = "ingress"
  security_group_id        = aws_security_group.bank_rds_group.id
  source_security_group_id = aws_security_group.lambda_recon_sg.id
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
}

resource "aws_security_group_rule" "app_from_lambda_report" {
  type                     = "ingress"
  security_group_id        = aws_security_group.bank_ec2_group.id
  source_security_group_id = aws_security_group.lambda_report_sg.id
  from_port                = 8080
  to_port                  = 8080
  protocol                 = "tcp"
}

# -----------------------------------
# daily_reconciliation — container image (psycopg2 needs its C extension,
# so this is packaged as a Docker image, not a zip). Build & push manually
# (or from CI) before `terraform apply` — see docs/backend/LAMBDA_DEPLOY_GUIDE.md.
# -----------------------------------

resource "aws_ecr_repository" "daily_reconciliation" {
  name                 = "banking-system/daily-reconciliation"
  image_tag_mutability = "MUTABLE"
}

resource "aws_lambda_function" "daily_reconciliation" {
  function_name = "daily-reconciliation"
  role          = aws_iam_role.lambda_execution_role.arn
  package_type  = "Image"
  image_uri     = "${aws_ecr_repository.daily_reconciliation.repository_url}:${var.daily_reconciliation_image_tag}"
  timeout       = 30
  memory_size   = 256

  vpc_config {
    subnet_ids         = [aws_subnet.private_subnet_a.id, aws_subnet.private_subnet_b.id]
    security_group_ids = [aws_security_group.lambda_recon_sg.id]
  }

  environment {
    variables = {
      DB_HOST     = aws_db_instance.banking_rds.address
      DB_PORT     = tostring(aws_db_instance.banking_rds.port)
      DB_NAME     = var.db_name
      DB_USER     = var.db_username
      DB_PASSWORD = var.db_password
    }
  }
}

# -----------------------------------
# monthly_report — stdlib only, plain zip package
# -----------------------------------

data "archive_file" "monthly_report_zip" {
  type        = "zip"
  source_file = "${path.module}/../lambda/monthly_report/handler.py"
  output_path = "${path.module}/../lambda/build/monthly_report.zip"
}

# Resolved by Terraform at apply time (not by the Lambda at invoke time) so
# the function needs zero AWS API access — sidesteps both (a) whatever is
# blocking this Lambda from reaching the internet-facing ALB's DNS name, a
# live-verification finding not yet root-caused, and (b) the NAT-Gateway
# requirement that a runtime elbv2/ec2 describe call would introduce. Tradeoff:
# this IP goes stale if the ASG replaces its instances until the next apply —
# acceptable for this project's scale; a production setup would use Cloud Map
# service discovery or pay for a NAT Gateway + real-time target lookup instead.
data "aws_instances" "app_instances" {
  instance_tags = {
    Name = "bank-app-server"
  }
  instance_state_names = ["running"]

  depends_on = [aws_autoscaling_group.bank_asg]
}

resource "aws_lambda_function" "monthly_report" {
  function_name    = "monthly-report"
  role             = aws_iam_role.lambda_execution_role.arn
  runtime          = "python3.12"
  handler          = "handler.lambda_handler"
  filename         = data.archive_file.monthly_report_zip.output_path
  source_code_hash = data.archive_file.monthly_report_zip.output_base64sha256
  timeout          = 30
  memory_size      = 128

  vpc_config {
    subnet_ids         = [aws_subnet.private_subnet_a.id, aws_subnet.private_subnet_b.id]
    security_group_ids = [aws_security_group.lambda_report_sg.id]
  }

  environment {
    variables = {
      INTERNAL_API_BASE_URL = "http://${tolist(data.aws_instances.app_instances.private_ips)[0]}:8080"
      INTERNAL_API_KEY      = var.internal_api_key
    }
  }
}

# -----------------------------------
# EventBridge Scheduler — cron triggers for both functions
# -----------------------------------

resource "aws_iam_role" "scheduler_invoke_role" {
  name = "bank-scheduler-invoke-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action    = "sts:AssumeRole"
        Effect    = "Allow"
        Principal = { Service = "scheduler.amazonaws.com" }
      }
    ]
  })
}

resource "aws_iam_role_policy" "scheduler_invoke_lambda" {
  name = "bank-scheduler-invoke-lambda-policy"
  role = aws_iam_role.scheduler_invoke_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = ["lambda:InvokeFunction"]
        Resource = [
          aws_lambda_function.daily_reconciliation.arn,
          aws_lambda_function.monthly_report.arn
        ]
      }
    ]
  })
}

resource "aws_scheduler_schedule" "daily_reconciliation" {
  name                = "daily-reconciliation"
  schedule_expression = "cron(0 1 * * ? *)" # 01:00 UTC daily

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = aws_lambda_function.daily_reconciliation.arn
    role_arn = aws_iam_role.scheduler_invoke_role.arn
  }
}

resource "aws_scheduler_schedule" "monthly_report" {
  name                = "monthly-report"
  schedule_expression = "cron(0 2 1 * ? *)" # 02:00 UTC on the 1st of each month

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = aws_lambda_function.monthly_report.arn
    role_arn = aws_iam_role.scheduler_invoke_role.arn
  }
}
