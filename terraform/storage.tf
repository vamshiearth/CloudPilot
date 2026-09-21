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

resource "kubernetes_storage_class_v1" "cloudpilot_gp3" {
  metadata {
    name = var.ebs_storage_class_name
  }

  storage_provisioner    = "ebs.csi.aws.com"
  reclaim_policy         = "Delete"
  volume_binding_mode    = "WaitForFirstConsumer"
  allow_volume_expansion = true

  parameters = {
    type      = "gp3"
    encrypted = "true"
  }

  depends_on = [
    aws_eks_addon.ebs_csi
  ]
}