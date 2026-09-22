variable "project_name" {
  description = "CloudPilot project name"
  type        = string
  default     = "cloudpilot"
}

variable "github_owner" {
  description = "GitHub repository owner"
  type        = string
  default     = "vamshiearth"
}

variable "github_repository" {
  description = "GitHub repository name"
  type        = string
  default     = "CloudPilot"
}

variable "environment" {
  description = "Deployment environment"
  type        = string
  default     = "dev"

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment must be dev, staging, or prod."
  }
}

variable "aws_region" {
  description = "AWS region for CloudPilot infrastructure"
  type        = string
  default     = "us-east-1"
}

variable "vpc_cidr" {
  description = "CIDR block for the CloudPilot VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "Availability zones used by CloudPilot"
  type        = list(string)

  default = [
    "us-east-1a",
    "us-east-1b"
  ]
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets"
  type        = list(string)

  default = [
    "10.0.0.0/20",
    "10.0.16.0/20"
  ]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets"
  type        = list(string)

  default = [
    "10.0.32.0/20",
    "10.0.48.0/20"
  ]
}

variable "enable_nat_gateway" {
  description = "Whether to create a NAT Gateway for private subnet internet access"
  type        = bool
  default     = false
}

variable "eks_cluster_name" {
  description = "Name of the CloudPilot EKS cluster"
  type        = string
  default     = null
}

variable "eks_node_instance_types" {
  description = "EC2 instance types used by the EKS managed node group"
  type        = list(string)

  default = [
    "t3.xlarge"
  ]
}

variable "eks_node_desired_size" {
  description = "Desired number of EKS worker nodes"
  type        = number
  default     = 1

  validation {
    condition     = var.eks_node_desired_size >= 1
    error_message = "eks_node_desired_size must be at least 1."
  }

  validation {
    condition = (
      var.eks_node_desired_size >= var.eks_node_min_size &&
      var.eks_node_desired_size <= var.eks_node_max_size
    )
    error_message = "eks_node_desired_size must be between min and max node sizes."
  }
}

variable "eks_node_min_size" {
  description = "Minimum number of EKS worker nodes"
  type        = number
  default     = 1

  validation {
    condition     = var.eks_node_min_size >= 1
    error_message = "eks_node_min_size must be at least 1."
  }
}

variable "eks_node_max_size" {
  description = "Maximum number of EKS worker nodes"
  type        = number
  default     = 1

  validation {
    condition     = var.eks_node_max_size >= 1
    error_message = "eks_node_max_size must be at least 1."
  }
}

variable "eks_node_disk_size" {
  description = "Root EBS volume size in GiB for each EKS worker node"
  type        = number
  default     = 40
}

variable "ebs_storage_class_name" {
  description = "Kubernetes StorageClass name for CloudPilot EBS volumes"
  type        = string
  default     = "cloudpilot-gp3"
}

variable "postgres_storage_size_gib" {
  description = "PostgreSQL persistent storage size in GiB"
  type        = number
  default     = 5

  validation {
    condition     = var.postgres_storage_size_gib >= 1
    error_message = "PostgreSQL storage must be at least 1 GiB."
  }
}

variable "kafka_storage_size_gib" {
  description = "Kafka persistent storage size in GiB"
  type        = number
  default     = 5

  validation {
    condition     = var.kafka_storage_size_gib >= 1
    error_message = "Kafka storage must be at least 1 GiB."
  }
}

variable "tempo_storage_size_gib" {
  description = "Tempo persistent storage size in GiB"
  type        = number
  default     = 5

  validation {
    condition     = var.tempo_storage_size_gib >= 1
    error_message = "Tempo storage must be at least 1 GiB."
  }
}

variable "alb_ingress_scheme" {
  description = "Scheme used by the CloudPilot Application Load Balancer"
  type        = string
  default     = "internet-facing"

  validation {
    condition     = contains(["internet-facing", "internal"], var.alb_ingress_scheme)
    error_message = "alb_ingress_scheme must be internet-facing or internal."
  }
}

variable "alb_target_type" {
  description = "AWS Load Balancer Controller target type"
  type        = string
  default     = "ip"

  validation {
    condition     = contains(["ip", "instance"], var.alb_target_type)
    error_message = "alb_target_type must be ip or instance."
  }
}

variable "postgres_database" {
  description = "Primary CloudPilot PostgreSQL database name"
  type        = string
  default     = "cloudpilot"
}

variable "audit_database" {
  description = "CloudPilot Audit PostgreSQL database name"
  type        = string
  default     = "cloudpilot_audit_db"
}
