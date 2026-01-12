variable "aws_region" {
  description = "AWS region"
  type = string
  default = "ap-southeast-2"
}

variable "db_name" {
  type = string
  default = "bankdb"
}

variable "db_username" {
  type = string
  default = "***REMOVED_DB_USERNAME***"
}

variable "db_password" {
  type = string
  default = "***REMOVED_DB_PASSWORD***"
}

variable "ami_id" {
  type = string
  default = "ami-0b3c832b6b7289e44"
}

variable "key_pair" {
  type = string
  default = "aws_rsa"
}

variable "ec2_instance_type" {
  type = string
  default = "t2.micro"
}

