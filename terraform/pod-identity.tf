resource "aws_eks_addon" "pod_identity_agent" {
  cluster_name = aws_eks_cluster.cloudpilot.name
  addon_name   = "eks-pod-identity-agent"

  depends_on = [
    aws_eks_node_group.cloudpilot
  ]

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-pod-identity-agent"
    }
  )
}

data "aws_iam_policy_document" "core_pod_identity_trust" {
  statement {
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["pods.eks.amazonaws.com"]
    }

    actions = ["sts:AssumeRole", "sts:TagSession"]

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/kubernetes-namespace"
      values   = [local.cloudpilot_namespace]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/kubernetes-service-account"
      values   = [local.core_service_account_name]
    }
  }
}

resource "aws_iam_role" "core_pod" {
  name               = "${local.name_prefix}-core-pod-role"
  assume_role_policy = data.aws_iam_policy_document.core_pod_identity_trust.json

  tags = merge(
    local.common_tags,
    {
      Name    = "${local.name_prefix}-core-pod-role"
      Service = "core-backend"
    }
  )
}

data "aws_iam_policy_document" "core_secrets_read" {
  statement {
    effect = "Allow"
    actions = [
      "secretsmanager:GetSecretValue",
      "secretsmanager:DescribeSecret"
    ]
    resources = [
      aws_secretsmanager_secret.jwt.arn,
      aws_secretsmanager_secret.postgres.arn
    ]
  }
}

resource "aws_iam_policy" "core_secrets_read" {
  name   = "${local.name_prefix}-core-secrets-read"
  policy = data.aws_iam_policy_document.core_secrets_read.json
}

resource "aws_iam_role_policy_attachment" "core_secrets_read" {
  role       = aws_iam_role.core_pod.name
  policy_arn = aws_iam_policy.core_secrets_read.arn
}

data "aws_iam_policy_document" "audit_pod_identity_trust" {
  statement {
    effect = "Allow"
    principals {
      type        = "Service"
      identifiers = ["pods.eks.amazonaws.com"]
    }
    actions = ["sts:AssumeRole", "sts:TagSession"]
    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/kubernetes-namespace"
      values   = [local.cloudpilot_namespace]
    }
    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/kubernetes-service-account"
      values   = [local.audit_service_account_name]
    }
  }
}

resource "aws_iam_role" "audit_pod" {
  name               = "${local.name_prefix}-audit-pod-role"
  assume_role_policy = data.aws_iam_policy_document.audit_pod_identity_trust.json

  tags = merge(
    local.common_tags,
    {
      Name    = "${local.name_prefix}-audit-pod-role"
      Service = "audit-service"
    }
  )
}

data "aws_iam_policy_document" "audit_secrets_read" {
  statement {
    effect = "Allow"
    actions = [
      "secretsmanager:GetSecretValue",
      "secretsmanager:DescribeSecret"
    ]
    resources = [aws_secretsmanager_secret.postgres.arn]
  }
}

resource "aws_iam_policy" "audit_secrets_read" {
  name   = "${local.name_prefix}-audit-secrets-read"
  policy = data.aws_iam_policy_document.audit_secrets_read.json
}

resource "aws_iam_role_policy_attachment" "audit_secrets_read" {
  role       = aws_iam_role.audit_pod.name
  policy_arn = aws_iam_policy.audit_secrets_read.arn
}

data "aws_iam_policy_document" "grafana_pod_identity_trust" {
  statement {
    effect = "Allow"
    principals {
      type        = "Service"
      identifiers = ["pods.eks.amazonaws.com"]
    }
    actions = ["sts:AssumeRole", "sts:TagSession"]
    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/kubernetes-namespace"
      values   = [local.cloudpilot_namespace]
    }
    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/kubernetes-service-account"
      values   = [local.grafana_service_account_name]
    }
  }
}

resource "aws_iam_role" "grafana_pod" {
  name               = "${local.name_prefix}-grafana-pod-role"
  assume_role_policy = data.aws_iam_policy_document.grafana_pod_identity_trust.json

  tags = merge(
    local.common_tags,
    {
      Name    = "${local.name_prefix}-grafana-pod-role"
      Service = "grafana"
    }
  )
}

data "aws_iam_policy_document" "grafana_secrets_read" {
  statement {
    effect = "Allow"
    actions = [
      "secretsmanager:GetSecretValue",
      "secretsmanager:DescribeSecret"
    ]
    resources = [aws_secretsmanager_secret.grafana.arn]
  }
}

resource "aws_iam_policy" "grafana_secrets_read" {
  name   = "${local.name_prefix}-grafana-secrets-read"
  policy = data.aws_iam_policy_document.grafana_secrets_read.json
}

resource "aws_iam_role_policy_attachment" "grafana_secrets_read" {
  role       = aws_iam_role.grafana_pod.name
  policy_arn = aws_iam_policy.grafana_secrets_read.arn
}

resource "aws_eks_pod_identity_association" "core" {
  cluster_name    = aws_eks_cluster.cloudpilot.name
  namespace       = local.cloudpilot_namespace
  service_account = local.core_service_account_name
  role_arn        = aws_iam_role.core_pod.arn

  depends_on = [aws_eks_addon.pod_identity_agent]
}

resource "aws_eks_pod_identity_association" "audit" {
  cluster_name    = aws_eks_cluster.cloudpilot.name
  namespace       = local.cloudpilot_namespace
  service_account = local.audit_service_account_name
  role_arn        = aws_iam_role.audit_pod.arn

  depends_on = [aws_eks_addon.pod_identity_agent]
}

resource "aws_eks_pod_identity_association" "grafana" {
  cluster_name    = aws_eks_cluster.cloudpilot.name
  namespace       = local.cloudpilot_namespace
  service_account = local.grafana_service_account_name
  role_arn        = aws_iam_role.grafana_pod.arn

  depends_on = [aws_eks_addon.pod_identity_agent]
}