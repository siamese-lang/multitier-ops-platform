variable "aws_region" {
  description = "AWS region for lab-small resources."
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "Project name used in resource names and tags."
  type        = string
  default     = "multitier-ops-platform"
}

variable "environment" {
  description = "Environment name used in resource names and tags."
  type        = string
  default     = "lab-small"
}

variable "availability_zone" {
  description = "Optional availability zone. If null, Terraform uses the first available AZ in the selected region."
  type        = string
  default     = null
}

variable "vpc_cidr" {
  description = "CIDR block for the lab-small VPC."
  type        = string
  default     = "10.20.0.0/16"

  validation {
    condition     = can(cidrhost(var.vpc_cidr, 0))
    error_message = "vpc_cidr must be a valid CIDR block."
  }
}

variable "public_subnet_cidr" {
  description = "CIDR block for the public subnet that hosts bastion-01."
  type        = string
  default     = "10.20.1.0/24"

  validation {
    condition     = can(cidrhost(var.public_subnet_cidr, 0))
    error_message = "public_subnet_cidr must be a valid CIDR block."
  }
}

variable "private_subnet_cidr" {
  description = "CIDR block for the private subnet that hosts app-01."
  type        = string
  default     = "10.20.11.0/24"

  validation {
    condition     = can(cidrhost(var.private_subnet_cidr, 0))
    error_message = "private_subnet_cidr must be a valid CIDR block."
  }
}

variable "operator_cidr" {
  description = "Public CIDR allowed to SSH into bastion-01, for example 203.0.113.10/32. Do not use 0.0.0.0/0 except for a short troubleshooting window."
  type        = string

  validation {
    condition     = can(cidrhost(var.operator_cidr, 0))
    error_message = "operator_cidr must be a valid CIDR block."
  }
}

variable "key_name" {
  description = "Existing EC2 key pair name. Terraform references it but does not create or store private keys."
  type        = string
}

variable "bastion_instance_type" {
  description = "EC2 instance type for bastion-01."
  type        = string
  default     = "t3.micro"
}

variable "app_instance_type" {
  description = "EC2 instance type for app-01."
  type        = string
  default     = "t3.small"
}

variable "ubuntu_ami_owner" {
  description = "Canonical AWS account ID used to find Ubuntu AMIs."
  type        = string
  default     = "099720109477"
}

variable "ubuntu_ami_name_pattern" {
  description = "AMI name pattern for Ubuntu Server."
  type        = string
  default     = "ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"
}

variable "root_volume_size_gib" {
  description = "Root EBS volume size for both baseline instances."
  type        = number
  default     = 20

  validation {
    condition     = var.root_volume_size_gib >= 8
    error_message = "root_volume_size_gib must be at least 8."
  }
}

variable "enable_detailed_monitoring" {
  description = "Enable EC2 detailed monitoring. Disabled by default for the first lab baseline."
  type        = bool
  default     = false
}

variable "extra_tags" {
  description = "Additional tags merged into all resources."
  type        = map(string)
  default     = {}
}
