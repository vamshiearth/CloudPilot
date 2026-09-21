resource "aws_security_group" "alb" {
  name        = "${local.name_prefix}-alb-sg"
  description = "Security group for the CloudPilot public load balancer"
  vpc_id      = aws_vpc.cloudpilot.id

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-alb-sg"
    }
  )
}

resource "aws_vpc_security_group_ingress_rule" "alb_http" {
  security_group_id = aws_security_group.alb.id

  cidr_ipv4   = "0.0.0.0/0"
  from_port   = 80
  to_port     = 80
  ip_protocol = "tcp"

  description = "Allow public HTTP traffic"
}

resource "aws_vpc_security_group_egress_rule" "alb_egress" {
  security_group_id = aws_security_group.alb.id

  cidr_ipv4   = "0.0.0.0/0"
  ip_protocol = "-1"

  description = "Allow outbound traffic from the load balancer"
}

resource "aws_security_group" "eks_nodes" {
  name        = "${local.name_prefix}-eks-nodes-sg"
  description = "Security group for CloudPilot EKS worker nodes"
  vpc_id      = aws_vpc.cloudpilot.id

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-eks-nodes-sg"
    }
  )
}

resource "aws_vpc_security_group_ingress_rule" "eks_nodes_from_alb" {
  security_group_id = aws_security_group.eks_nodes.id

  referenced_security_group_id = aws_security_group.alb.id

  # NOTE:
  # This NodePort rule supports the current transitional/showcase model.
  # If AWS Load Balancer Controller uses target-type "ip",
  # traffic can target Pod IPs directly and this rule may be tightened/removed.
  from_port   = 30000
  to_port     = 32767
  ip_protocol = "tcp"

  description = "Allow NodePort traffic from the ALB"
}

resource "aws_vpc_security_group_ingress_rule" "eks_nodes_internal" {
  security_group_id = aws_security_group.eks_nodes.id

  referenced_security_group_id = aws_security_group.eks_nodes.id

  ip_protocol = "-1"

  description = "Allow communication between EKS worker nodes"
}

resource "aws_vpc_security_group_egress_rule" "eks_nodes_egress" {
  security_group_id = aws_security_group.eks_nodes.id

  cidr_ipv4   = "0.0.0.0/0"
  ip_protocol = "-1"

  description = "Allow worker nodes outbound access"
}