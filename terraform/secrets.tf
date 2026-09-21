resource "aws_secretsmanager_secret" "jwt" {
  name                    = "${local.name_prefix}/jwt"
  description             = "JWT signing secret for CloudPilot"
  recovery_window_in_days = 0

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-jwt-secret"
    }
  )
}

resource "aws_secretsmanager_secret" "postgres" {
  name                    = "${local.name_prefix}/postgres"
  description             = "PostgreSQL credentials for CloudPilot"
  recovery_window_in_days = 0

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-postgres-secret"
    }
  )
}

resource "aws_secretsmanager_secret" "grafana" {
  name                    = "${local.name_prefix}/grafana"
  description             = "Grafana administrator credentials"
  recovery_window_in_days = 0

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-grafana-secret"
    }
  )
}