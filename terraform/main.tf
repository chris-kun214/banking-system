terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

data "aws_caller_identity" "current" {}

data "aws_region" "current" {}

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
    Name = "banking-public-subnet"
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

resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public_subnet_a.id
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
  cidr_ipv4         = "202.171.178.8/32"
  from_port         = 22
  to_port           = 22
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "ec2_http" {
  security_group_id = aws_security_group.bank_ec2_group.id
  cidr_ipv4         = "202.171.178.8/32"
  from_port         = 8080
  to_port           = 8080
  ip_protocol       = "tcp"
}

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

  engine         = "postgres"
  engine_version = "17.5"
  instance_class = "db.t4g.micro"

  allocated_storage = 20
  storage_type      = "gp3"

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password
  port     = 5432

  db_subnet_group_name   = aws_db_subnet_group.db_subset.name
  vpc_security_group_ids = [aws_security_group.bank_rds_group.id]

  skip_final_snapshot = true
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

resource "aws_iam_instance_profile" "ec2_profile" {
  name = "bank-ec2-profile"
  role = aws_iam_role.ec2_role.name
}

# -----------------------------------
# EC2 instance
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

resource "aws_instance" "bank_server" {
  ami                         = var.ami_id
  instance_type               = var.ec2_instance_type
  key_name                    = var.key_pair
  subnet_id                   = aws_subnet.public_subnet_a.id
  vpc_security_group_ids      = [aws_security_group.bank_ec2_group.id]
  associate_public_ip_address = true
  iam_instance_profile        = aws_iam_instance_profile.ec2_profile.name

  user_data = templatefile("${path.module}/docker.sh", {
    aws_region   = var.aws_region
    ecr_repo_uri = "541032290058.dkr.ecr.ap-southeast-2.amazonaws.com/mybank:prod-1.0.0"
    db_url       = "jdbc:postgresql://${local.db_address}:${local.db_port}/${local.db_name}"
    db_username  = local.db_username
    db_password  = local.db_password
  })

  tags = {
    Name = "bank-app-server"
  }

  depends_on = [aws_db_instance.banking_rds]
}
