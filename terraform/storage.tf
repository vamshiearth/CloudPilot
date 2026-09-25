resource "aws_eks_addon" "ebs_csi" {
  cluster_name = aws_eks_cluster.cloudpilot.name
  addon_name   = "aws-ebs-csi-driver"

  depends_on = [
    aws_eks_node_group.cloudpilot
  ]

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-ebs-csi"
    }
  )
}

