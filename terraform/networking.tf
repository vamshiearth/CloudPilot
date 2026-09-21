resource "aws_vpc" "cloudpilot" {
  cidr_block = var.vpc_cidr

  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-vpc"
    }
  )
}

resource "aws_internet_gateway" "cloudpilot" {
  vpc_id = aws_vpc.cloudpilot.id

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-igw"
    }
  )
}

resource "aws_subnet" "public" {
  count = length(var.public_subnet_cidrs)

  vpc_id            = aws_vpc.cloudpilot.id
  cidr_block        = var.public_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]

  map_public_ip_on_launch = true

  tags = merge(
    local.common_tags,
    {
      Name                                              = "${local.name_prefix}-public-${count.index + 1}"
      "kubernetes.io/role/elb"                          = "1"
      "kubernetes.io/cluster/${local.eks_cluster_name}" = "shared"
    }
  )
}

resource "aws_subnet" "private" {
  count = length(var.private_subnet_cidrs)

  vpc_id            = aws_vpc.cloudpilot.id
  cidr_block        = var.private_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]

  map_public_ip_on_launch = false

  tags = merge(
    local.common_tags,
    {
      Name                                              = "${local.name_prefix}-private-${count.index + 1}"
      "kubernetes.io/role/internal-elb"                 = "1"
      "kubernetes.io/cluster/${local.eks_cluster_name}" = "shared"
    }
  )
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.cloudpilot.id

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-public-rt"
    }
  )
}

resource "aws_route" "public_internet" {
  route_table_id         = aws_route_table.public.id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.cloudpilot.id
}

resource "aws_route_table_association" "public" {
  count = length(aws_subnet.public)

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.cloudpilot.id

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-private-rt"
    }
  )
}

resource "aws_route_table_association" "private" {
  count = length(aws_subnet.private)

  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}

resource "aws_eip" "nat" {
  count = var.enable_nat_gateway ? 1 : 0

  domain = "vpc"

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-nat-eip"
    }
  )
}

resource "aws_nat_gateway" "cloudpilot" {
  count = var.enable_nat_gateway ? 1 : 0

  allocation_id = aws_eip.nat[0].id
  subnet_id     = aws_subnet.public[0].id

  depends_on = [
    aws_internet_gateway.cloudpilot
  ]

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-nat"
    }
  )
}

resource "aws_route" "private_nat" {
  count = var.enable_nat_gateway ? 1 : 0

  route_table_id         = aws_route_table.private.id
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id         = aws_nat_gateway.cloudpilot[0].id
}