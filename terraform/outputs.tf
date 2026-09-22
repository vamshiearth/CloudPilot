output "project_name" {
  value = var.project_name
}

output "environment" {
  value = var.environment
}

output "aws_region" {
  value = var.aws_region
}

output "name_prefix" {
  value = local.name_prefix
}

output "github_actions_role_arn" {
  description = "IAM role ARN used by GitHub Actions OIDC authentication"
  value       = aws_iam_role.github_actions.arn
}

output "vpc_id" {
  description = "CloudPilot VPC ID"
  value       = aws_vpc.cloudpilot.id
}

output "public_subnet_ids" {
  description = "Public subnet IDs"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "Private subnet IDs"
  value       = aws_subnet.private[*].id
}

output "public_route_table_id" {
  description = "Public route table ID"
  value       = aws_route_table.public.id
}

output "private_route_table_id" {
  description = "Private route table ID"
  value       = aws_route_table.private.id
}

output "core_backend_ecr_repository_url" {
  description = "ECR repository URL for the Core backend"
  value       = aws_ecr_repository.core_backend.repository_url
}

output "audit_service_ecr_repository_url" {
  description = "ECR repository URL for the Audit service"
  value       = aws_ecr_repository.audit_service.repository_url
}

output "frontend_ecr_repository_url" {
  description = "ECR repository URL for the frontend"
  value       = aws_ecr_repository.frontend.repository_url
}

output "eks_cluster_role_arn" {
  description = "IAM role ARN used by the EKS control plane"
  value       = aws_iam_role.eks_cluster.arn
}

output "eks_node_role_arn" {
  description = "IAM role ARN used by the EKS worker nodes"
  value       = aws_iam_role.eks_node.arn
}

output "eks_cluster_name" {
  description = "CloudPilot EKS cluster name"
  value       = aws_eks_cluster.cloudpilot.name
}

output "eks_cluster_endpoint" {
  description = "CloudPilot EKS API endpoint"
  value       = aws_eks_cluster.cloudpilot.endpoint
}

output "eks_cluster_security_group_id" {
  description = "Security group created for the EKS cluster"
  value       = aws_eks_cluster.cloudpilot.vpc_config[0].cluster_security_group_id
}

output "eks_node_group_name" {
  description = "CloudPilot EKS managed node group name"
  value       = aws_eks_node_group.cloudpilot.node_group_name
}

output "ebs_storage_class_name" {
  description = "Kubernetes StorageClass used for CloudPilot persistent volumes"
  value       = kubernetes_storage_class_v1.cloudpilot_gp3.metadata[0].name
}

output "postgres_storage_size_gib" {
  value = var.postgres_storage_size_gib
}

output "kafka_storage_size_gib" {
  value = var.kafka_storage_size_gib
}

output "tempo_storage_size_gib" {
  value = var.tempo_storage_size_gib
}

output "alb_security_group_id" {
  description = "Security group ID for the CloudPilot load balancer"
  value       = aws_security_group.alb.id
}

output "eks_nodes_security_group_id" {
  description = "Security group ID for CloudPilot EKS worker nodes"
  value       = aws_security_group.eks_nodes.id
}

output "alb_ingress_scheme" {
  description = "Planned ALB ingress scheme"
  value       = var.alb_ingress_scheme
}

output "alb_target_type" {
  description = "Planned ALB target type"
  value       = var.alb_target_type
}

output "jwt_secret_arn" {
  description = "ARN of the CloudPilot JWT secret"
  value       = aws_secretsmanager_secret.jwt.arn
}

output "postgres_secret_arn" {
  description = "ARN of the CloudPilot PostgreSQL secret"
  value       = aws_secretsmanager_secret.postgres.arn
}

output "grafana_secret_arn" {
  description = "ARN of the CloudPilot Grafana secret"
  value       = aws_secretsmanager_secret.grafana.arn
}
