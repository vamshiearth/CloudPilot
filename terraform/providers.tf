provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

data "aws_eks_cluster_auth" "cloudpilot" {
  name = aws_eks_cluster.cloudpilot.name
}

provider "kubernetes" {
  host                   = aws_eks_cluster.cloudpilot.endpoint
  cluster_ca_certificate = base64decode(aws_eks_cluster.cloudpilot.certificate_authority[0].data)
  token                  = data.aws_eks_cluster_auth.cloudpilot.token
}
