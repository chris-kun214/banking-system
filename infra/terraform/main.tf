terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.4"
    }
  }
}

provider "aws" {
  region  = var.aws_region
  profile = var.profile
}

data "aws_caller_identity" "current" {}

data "aws_region" "current" {}

# -----------------------------------
# SQS
# -----------------------------------
resource "aws_sqs_queue" "account_credit_events" {
  name = "account-credit-events"
}

# -----------------------------------
# VPC
# -----------------------------------

resource "aws_vpc" "bank_vpc" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "banking_system"
  }
}

resource "aws_subnet" "public_subnet_a" {
  vpc_id                  = aws_vpc.bank_vpc.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "${var.aws_region}a"
  map_public_ip_on_launch = true

  tags = {
    Name = "banking-public-subnet-a"
  }
}

# Second public subnet in another AZ — required by the ALB (needs >=2 AZs) and
# by the ASG for genuine multi-AZ HA. No NAT Gateway is added: app instances
# stay in these public subnets (cost tradeoff, see infra/terraform/alb.tf),
# reachable on 8080 only from the ALB's security group, not the internet.
resource "aws_subnet" "public_subnet_b" {
  vpc_id                  = aws_vpc.bank_vpc.id
  cidr_block              = "10.0.3.0/24"
  availability_zone       = "${var.aws_region}b"
  map_public_ip_on_launch = true

  tags = {
    Name = "banking-public-subnet-b"
  }
}

resource "aws_subnet" "private_subnet_a" {
  vpc_id            = aws_vpc.bank_vpc.id
  cidr_block        = "10.0.4.0/24"
  availability_zone = "${var.aws_region}a"

  tags = {
    Name = "banking-private-subnet-a"
  }
}

resource "aws_subnet" "private_subnet_b" {
  vpc_id            = aws_vpc.bank_vpc.id
  cidr_block        = "10.0.2.0/24"
  availability_zone = "${var.aws_region}b"

  tags = {
    Name = "banking-private-subnet-b"
  }
}

resource "aws_internet_gateway" "bank_igw" {
  vpc_id = aws_vpc.bank_vpc.id

  tags = {
    Name = "bank-igw"
  }
}

resource "aws_route_table" "public_route" {
  vpc_id = aws_vpc.bank_vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.bank_igw.id
  }

  tags = {
    Name = "bank-public-rt"
  }
}

resource "aws_route_table_association" "public_a" {
  subnet_id      = aws_subnet.public_subnet_a.id
  route_table_id = aws_route_table.public_route.id
}

resource "aws_route_table_association" "public_b" {
  subnet_id      = aws_subnet.public_subnet_b.id
  route_table_id = aws_route_table.public_route.id
}

# -----------------------------------
# Security Group EC2
# -----------------------------------

resource "aws_security_group" "bank_ec2_group" {
  vpc_id      = aws_vpc.bank_vpc.id
  name        = "bank-ec2-sc"
  description = "Security group set up for ec2 instance"
}

resource "aws_vpc_security_group_ingress_rule" "ec2_ssh" {
  security_group_id = aws_security_group.bank_ec2_group.id
  cidr_ipv4         = var.allowed_ssh_cidr
  from_port         = 22
  to_port           = 22
  ip_protocol       = "tcp"
}

# Port 8080 is intentionally NOT opened here to allowed_ssh_cidr or the internet —
# traffic reaches the app only via the ALB (see aws_security_group_rule.app_from_alb
# in alb.tf) or from the monthly-report Lambda (aws_security_group_rule.app_from_lambda_report
# in lambda.tf).

resource "aws_vpc_security_group_egress_rule" "ec2_all" {
  security_group_id = aws_security_group.bank_ec2_group.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

# -----------------------------------
# Security Group RDS
# -----------------------------------

resource "aws_security_group" "bank_rds_group" {
  vpc_id      = aws_vpc.bank_vpc.id
  name        = "bank-ec2-rds"
  description = "Security group set up for rds"
}

resource "aws_vpc_security_group_ingress_rule" "rds_postgres" {
  security_group_id = aws_security_group.bank_rds_group.id

  referenced_security_group_id = aws_security_group.bank_ec2_group.id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "rds_all" {
  security_group_id = aws_security_group.bank_rds_group.id

  cidr_ipv4   = "0.0.0.0/0"
  ip_protocol = "-1"
}

# -----------------------------------
# RDS
# -----------------------------------
resource "aws_db_subnet_group" "db_subset" {
  name = "bank-rds-db-sng"
  subnet_ids = [
    aws_subnet.private_subnet_a.id,
    aws_subnet.private_subnet_b.id
  ]

  tags = {
    Name = "bank-rds-subnet-group"
  }
}

resource "aws_db_instance" "banking_rds" {
  identifier = "bank-rds"

  engine = "postgres"
  # RDS silently provisioned 17.9 for a requested "17.5" (maps to whatever
  # minor version is currently supported on that major track at creation
  # time) — declaring the version actually running avoids Terraform trying
  # to "downgrade" it back to 17.5 on every apply, which RDS rejects outright.
  engine_version = "17.9"
  instance_class = "db.t4g.micro"

  allocated_storage = 20
  storage_type      = "gp3"

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password
  port     = 5432

  db_subnet_group_name   = aws_db_subnet_group.db_subset.name
  vpc_security_group_ids = [aws_security_group.bank_rds_group.id]

  # Without this, modifications (e.g. backup_retention_period) are queued for
  # the next maintenance window instead of applying right away — surfaced
  # during live verification when a read-replica create raced ahead of a
  # still-pending backup-retention change. Fine for this project's use case
  # (immediate feedback matters more than avoiding a brief modification
  # window); a real production DB might prefer scheduled maintenance instead.
  apply_immediately = true

  # Off by default — a standby roughly doubles the instance-hour cost. Flip on
  # only to demonstrate/verify HA, then back off.
  multi_az = var.enable_multi_az

  # Automated backups (also required by RDS before it will let you create a
  # read replica off this instance — CreateDBInstanceReadReplica rejects a
  # source with backup_retention_period = 0).
  backup_retention_period = var.db_backup_retention_days

  skip_final_snapshot = true
}

# Same-region read replica for disaster recovery / read scaling. Off by
# default since it's a second full running instance. `engine`/`engine_version`/
# `db_subnet_group_name` are deliberately NOT set — same-region replicas
# inherit those from the source automatically, and setting them risks
# Terraform trying to force an unwanted separate upgrade/network path.
resource "aws_db_instance" "banking_rds_replica" {
  count = var.enable_read_replica ? 1 : 0

  identifier          = "bank-rds-replica"
  replicate_source_db = aws_db_instance.banking_rds.identifier
  instance_class      = var.replica_instance_class

  publicly_accessible = false
  skip_final_snapshot = true
}

# -----------------------------------
# CloudWatch Logs
# -----------------------------------

resource "aws_cloudwatch_log_group" "app_logs" {
  name              = "/banking-system/app"
  retention_in_days = var.cloudwatch_log_retention_days

  tags = {
    Name = "banking-system-app-logs"
  }
}

# -----------------------------------
# EC2 IAM
# -----------------------------------

resource "aws_iam_role" "ec2_role" {
  name = "bank-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      },
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ec2_pull" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

resource "aws_iam_role_policy" "ec2_sqs_send" {
  name = "bank-ec2-sqs-send-policy"
  role = aws_iam_role.ec2_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sqs:GetQueueUrl",
          "sqs:GetQueueAttributes",
          "sqs:SendMessage"
        ]
        Resource = aws_sqs_queue.account_credit_events.arn
      }
    ]
  })
}

resource "aws_iam_role_policy" "ec2_cloudwatch" {
  name = "bank-ec2-cloudwatch-policy"
  role = aws_iam_role.ec2_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["cloudwatch:PutMetricData"]
        Resource = "*" # PutMetricData does not support resource-level restriction
      },
      {
        Effect = "Allow"
        Action = [
          "logs:PutLogEvents",
          "logs:CreateLogStream",
          "logs:DescribeLogStreams"
        ]
        # Intentionally no logs:CreateLogGroup — the log group is pre-created by Terraform above.
        Resource = "${aws_cloudwatch_log_group.app_logs.arn}:*"
      }
    ]
  })
}

resource "aws_iam_instance_profile" "ec2_profile" {
  name = "bank-ec2-profile"
  role = aws_iam_role.ec2_role.name
}

# -----------------------------------
# Shared locals (app server templating — actual launch template/ASG live in alb.tf)
# -----------------------------------

locals {
  erc_registry_id = data.aws_caller_identity.current.account_id
  erc_repo        = "mybank"
  db_address      = aws_db_instance.banking_rds.address
  db_port         = aws_db_instance.banking_rds.port
  db_username     = var.db_username
  db_password     = var.db_password
  db_name         = var.db_name
}
