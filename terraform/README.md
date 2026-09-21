# CloudPilot Terraform

Terraform definitions for the planned AWS deployment of CloudPilot.

## Current mode

Infrastructure definition and local validation only.

Do not run:

```text
terraform apply
```

until the AWS deployment phase is intentionally resumed.

## Planned infrastructure

- VPC
- public/private subnets
- route tables
- security groups
- ECR
- EKS
- worker nodes
- storage
- ingress/load balancing
- IAM roles and policies

## Cost safety

The current Terraform phase is for configuration and validation only.
AWS resources must not be created yet.

## Development environment

Example non-secret configuration:

`environments/dev/dev.tfvars.example`

A real local override file may later be created as:

`environments/dev/dev.tfvars`

That file is intentionally ignored by Git.

Example validation command:

```text
terraform plan -var-file="environments/dev/dev.tfvars"
```

Do not run `terraform apply` until the AWS deployment phase is intentionally resumed.
