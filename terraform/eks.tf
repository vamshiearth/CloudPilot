resource "aws_eks_cluster" "cloudpilot" {
  name     = local.eks_cluster_name
  role_arn = aws_iam_role.eks_cluster.arn

  vpc_config {
    subnet_ids = concat(
      aws_subnet.public[*].id,
      aws_subnet.private[*].id
    )

    endpoint_public_access  = true
    endpoint_private_access = true
  }

  depends_on = [
    aws_iam_role_policy_attachment.eks_cluster_policy
  ]

  tags = merge(
    local.common_tags,
    {
      Name = local.eks_cluster_name
    }
  )
}

resource "aws_eks_node_group" "cloudpilot" {
  cluster_name    = aws_eks_cluster.cloudpilot.name
  node_group_name = "${local.name_prefix}-nodes"

  node_role_arn = aws_iam_role.eks_node.arn

  subnet_ids = aws_subnet.public[*].id

  instance_types = var.eks_node_instance_types

  disk_size = var.eks_node_disk_size

  scaling_config {
    desired_size = var.eks_node_desired_size
    min_size     = var.eks_node_min_size
    max_size     = var.eks_node_max_size
  }

  update_config {
    max_unavailable = 1
  }

  depends_on = [
    aws_iam_role_policy_attachment.eks_node_worker_policy,
    aws_iam_role_policy_attachment.eks_node_ecr_policy,
    aws_iam_role_policy_attachment.eks_node_cni_policy,
    aws_iam_role_policy_attachment.eks_node_ebs_csi_policy
  ]

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-nodes"
    }
  )
}