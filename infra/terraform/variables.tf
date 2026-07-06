variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-southeast-2"
}

variable "profile" {
  type    = string
  default = "work-account"
}

variable "db_name" {
  type    = string
  default = "bankdb"
}

variable "db_username" {
  description = "RDS master username. Must be supplied via terraform.tfvars (gitignored) or TF_VAR_db_username."
  type        = string
  sensitive   = true
}

variable "db_password" {
  description = "RDS master password. Must be supplied via terraform.tfvars (gitignored) or TF_VAR_db_password."
  type        = string
  sensitive   = true
}

variable "ami_id" {
  type    = string
  default = "ami-0b3c832b6b7289e44"
}

variable "key_pair" {
  type    = string
  default = "aws_key"
}

variable "ec2_instance_type" {
  type    = string
  default = "t3.micro"
}

variable "image_uri" {
  description = "Full ECR image URI with tag"
  type        = string
  default     = "187740755858.dkr.ecr.ap-southeast-2.amazonaws.com/myproject/banking:1.1.3"
}

variable "jwt_secret" {
  description = "Base64 encoded JWT signing secret for production. Must be supplied via terraform.tfvars (gitignored) or TF_VAR_jwt_secret."
  type        = string
  sensitive   = true
}

variable "jwt_expiration" {
  description = "JWT expiration in milliseconds"
  type        = string
  default     = "86400000"
}

variable "hibernate_ddl_auto" {
  description = "Hibernate ddl-auto strategy for prod profile"
  type        = string
  default     = "update"
}

variable "allowed_ssh_cidr" {
  description = "CIDR allowed to reach the EC2 instance over SSH (22) and app port (8080). Supply your own IP/32 via terraform.tfvars — do not default to a real address."
  type        = string
}

