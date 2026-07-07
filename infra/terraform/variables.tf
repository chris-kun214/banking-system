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
  description = "CIDR allowed to reach the EC2 instance over SSH (22). Supply your own IP/32 via terraform.tfvars — do not default to a real address."
  type        = string
}

variable "internal_api_key" {
  description = "Shared-secret header value for /api/internal/** (called by the reconciliation/report Lambdas). Must be supplied via terraform.tfvars (gitignored) or TF_VAR_internal_api_key."
  type        = string
  sensitive   = true
}

variable "cloudwatch_log_retention_days" {
  description = "Retention for the app's CloudWatch Logs log group"
  type        = number
  default     = 14
}

variable "daily_reconciliation_image_tag" {
  description = "Tag of the daily-reconciliation Lambda image, already pushed to its ECR repo before apply. See docs/backend/LAMBDA_DEPLOY_GUIDE.md."
  type        = string
  default     = "latest"
}

variable "asg_min_size" {
  description = "Minimum app-tier instances. Kept small by default to control cost."
  type        = number
  default     = 1
}

variable "asg_max_size" {
  description = "Maximum app-tier instances."
  type        = number
  default     = 2
}

variable "asg_desired_capacity" {
  description = "Desired app-tier instances. Set to 2 only when demonstrating multi-AZ HA; scale back to 1 afterward."
  type        = number
  default     = 1
}

variable "enable_multi_az" {
  description = "Enable RDS Multi-AZ standby. Roughly doubles instance-hour cost — leave off unless demonstrating HA."
  type        = bool
  default     = false
}

variable "enable_read_replica" {
  description = "Create a same-region RDS read replica for DR/read scaling. A second full running instance — leave off unless demonstrating it."
  type        = bool
  default     = false
}

variable "replica_instance_class" {
  description = "Instance class for the optional read replica"
  type        = string
  default     = "db.t4g.micro"
}

variable "db_backup_retention_days" {
  description = "RDS automated backup retention (days). Required to be >0 before a read replica can be created; also just good DR practice on its own."
  type        = number
  default     = 1
}

