@'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "pods.eks.amazonaws.com"
      },
      "Action": [
        "sts:AssumeRole",
        "sts:TagSession"
      ]
    }
  ]
}
'@ | Set-Content "$env:TEMP\alb-controller-trust-policy.json"resource "aws_eks_addon" "ebs_csi" {
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

