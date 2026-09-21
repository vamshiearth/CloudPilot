data "aws_iam_policy_document" "alb_controller_assume_role" {
  statement {
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }

    actions = ["sts:AssumeRole"]
  }
}

resource "aws_iam_role" "alb_controller" {
  name = "${local.name_prefix}-alb-controller-role"

  assume_role_policy = data.aws_iam_policy_document.alb_controller_assume_role.json

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-alb-controller-role"
    }
  )
}