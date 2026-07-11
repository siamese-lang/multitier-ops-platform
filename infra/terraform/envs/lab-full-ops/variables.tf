variable "aws_region" {
  description = "AWS region for lab-full-ops resources."
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
  default     = "lab-full-ops"
}

variable "availability_zone" {
  description = "Optional availability zone. If null, Terraform uses the first available AZ in the selected region."
  type        = string
  default     = null
}

variable "vpc_cidr" {
  description = "CIDR block for the lab-full-ops VPC."
  type        = string
  default     = "10.50.0.0/16"

  validation {
    condition     = can(cidrhost(var.vpc_cidr, 0))
    error_message = "vpc_cidr must be a valid CIDR block."
  }
}

variable "public_subnet_cidr" {
  description = "CIDR block for the public subnet that hosts bastion-01 and nginx-01."
  type        = string
  default     = "10.50.1.0/24"

  validation {
    condition     = can(cidrhost(var.public_subnet_cidr, 0))
    error_message = "public_subnet_cidr must be a valid CIDR block."
  }
}

variable "private_app_subnet_cidr" {
  description = "CIDR block for the private app subnet that hosts app nodes."
  type        = string
  default     = "10.50.11.0/24"

  validation {
    condition     = can(cidrhost(var.private_app_subnet_cidr, 0))
    error_message = "private_app_subnet_cidr must be a valid CIDR block."
  }
}

variable "private_db_subnet_cidr" {
  description = "CIDR block for the private DB subnet that hosts db-primary-01."
  type        = string
  default     = "10.50.21.0/24"

  validation {
    condition     = can(cidrhost(var.private_db_subnet_cidr, 0))
    error_message = "private_db_subnet_cidr must be a valid CIDR block."
  }
}

variable "private_storage_subnet_cidr" {
  description = "CIDR block for the private storage subnet that hosts nfs-01 when enabled."
  type        = string
  default     = "10.50.31.0/24"

  validation {
    condition     = can(cidrhost(var.private_storage_subnet_cidr, 0))
    error_message = "private_storage_subnet_cidr must be a valid CIDR block."
  }
}

variable "private_ops_subnet_cidr" {
  description = "CIDR block for the private ops subnet that hosts backup, monitoring, logging, and load generation nodes when enabled."
  type        = string
  default     = "10.50.41.0/24"

  validation {
    condition     = can(cidrhost(var.private_ops_subnet_cidr, 0))
    error_message = "private_ops_subnet_cidr must be a valid CIDR block."
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

variable "web_ingress_cidr" {
  description = "CIDR allowed to access nginx-01 HTTP/HTTPS during lab validation. Use the operator public IP CIDR unless public access is intentionally required."
  type        = string

  validation {
    condition     = can(cidrhost(var.web_ingress_cidr, 0))
    error_message = "web_ingress_cidr must be a valid CIDR block."
  }
}

variable "key_name" {
  description = "Existing EC2 key pair name. Terraform references it but does not create or store private keys."
  type        = string
}

variable "enable_nat_gateway" {
  description = "Create a NAT Gateway and private default routes. Disabled by default to avoid NAT Gateway hourly charges in Free Tier validation."
  type        = bool
  default     = false
}

variable "enable_app_02" {
  description = "Create app-02. Disabled by default for reduced Free Tier validation."
  type        = bool
  default     = false
}

variable "enable_storage_node" {
  description = "Create nfs-01 for storage-tier validation."
  type        = bool
  default     = true
}

variable "enable_backup_node" {
  description = "Create backup-01 for backup-tier validation."
  type        = bool
  default     = true
}

variable "enable_monitoring_node" {
  description = "Create mon-01. Disabled by default for reduced Free Tier validation."
  type        = bool
  default     = false
}

variable "enable_logging_node" {
  description = "Create log-01. Disabled by default for reduced Free Tier validation."
  type        = bool
  default     = false
}

variable "enable_loadgen_node" {
  description = "Create loadgen-01. Disabled by default for reduced Free Tier validation."
  type        = bool
  default     = false
}

variable "bastion_instance_type" {
  description = "EC2 instance type for bastion-01. Default uses t3.micro because the account rejected t2.micro as not Free Tier eligible."
  type        = string
  default     = "t3.micro"
}

variable "nginx_instance_type" {
  description = "EC2 instance type for nginx-01."
  type        = string
  default     = "t3.micro"
}

variable "app_instance_type" {
  description = "EC2 instance type for app nodes."
  type        = string
  default     = "t3.micro"
}

variable "db_instance_type" {
  description = "EC2 instance type for db-primary-01."
  type        = string
  default     = "t3.micro"
}

variable "storage_instance_type" {
  description = "EC2 instance type for nfs-01."
  type        = string
  default     = "t3.micro"
}

variable "backup_instance_type" {
  description = "EC2 instance type for backup-01."
  type        = string
  default     = "t3.micro"
}

variable "monitoring_instance_type" {
  description = "EC2 instance type for mon-01."
  type        = string
  default     = "t3.micro"
}

variable "logging_instance_type" {
  description = "EC2 instance type for log-01."
  type        = string
  default     = "t3.micro"
}

variable "loadgen_instance_type" {
  description = "EC2 instance type for loadgen-01."
  type        = string
  default     = "t3.micro"
}

variable "web_port" {
  description = "Public HTTP port exposed by nginx-01."
  type        = number
  default     = 80
}

variable "web_https_port" {
  description = "Public HTTPS port exposed by nginx-01 for lab validation."
  type        = number
  default     = 443
}

variable "app_port" {
  description = "Application HTTP port used by app nodes."
  type        = number
  default     = 8080
}

variable "db_port" {
  description = "PostgreSQL port exposed by db-primary-01 to app and backup nodes."
  type        = number
  default     = 5432
}

variable "nfs_port" {
  description = "NFS port allowed from app and backup nodes to nfs-01."
  type        = number
  default     = 2049
}

variable "node_exporter_port" {
  description = "Future node exporter port allowed from mon-01 to lab nodes. This skeleton only opens the flow."
  type        = number
  default     = 9100
}

variable "loki_push_port" {
  description = "Future Loki push/API port allowed from lab nodes to log-01. This skeleton only opens the flow."
  type        = number
  default     = 3100
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
  description = "Default root EBS volume size for lab-full-ops EC2 instances."
  type        = number
  default     = 8

  validation {
    condition     = var.root_volume_size_gib >= 8
    error_message = "root_volume_size_gib must be at least 8."
  }
}

variable "storage_root_volume_size_gib" {
  description = "Root EBS volume size for nfs-01. Increase only for a specific storage validation run."
  type        = number
  default     = 8

  validation {
    condition     = var.storage_root_volume_size_gib >= 8
    error_message = "storage_root_volume_size_gib must be at least 8."
  }
}

variable "backup_root_volume_size_gib" {
  description = "Root EBS volume size for backup-01. Increase only for a specific backup validation run."
  type        = number
  default     = 8

  validation {
    condition     = var.backup_root_volume_size_gib >= 8
    error_message = "backup_root_volume_size_gib must be at least 8."
  }
}

variable "enable_detailed_monitoring" {
  description = "Enable EC2 detailed monitoring. Disabled by default for the lab-full-ops skeleton."
  type        = bool
  default     = false
}

variable "extra_tags" {
  description = "Additional tags merged into all resources."
  type        = map(string)
  default     = {}
}
