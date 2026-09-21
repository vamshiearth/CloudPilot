locals {
  name_prefix = "${var.project_name}-${var.environment}"

  eks_cluster_name = coalesce(
    var.eks_cluster_name,
    "${local.name_prefix}-eks"
  )

  storage_sizes = {
    postgres = var.postgres_storage_size_gib
    kafka    = var.kafka_storage_size_gib
    tempo    = var.tempo_storage_size_gib
  }

  core_service_account_name    = "cloudpilot-core"
  audit_service_account_name   = "cloudpilot-audit"
  grafana_service_account_name = "cloudpilot-grafana"
  cloudpilot_namespace         = "cloudpilot"

  common_tags = {
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}
