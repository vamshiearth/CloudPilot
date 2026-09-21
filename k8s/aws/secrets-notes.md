# AWS Secrets

CloudPilot sensitive configuration is designed to use AWS Secrets Manager.

Planned secrets:

- `cloudpilot-dev/jwt`
- `cloudpilot-dev/postgres`
- `cloudpilot-dev/grafana`

Actual secret values must not be committed to:

- Terraform `.tf` files
- Terraform `.tfvars`
- Kubernetes YAML
- Dockerfiles
- `.env.example`
- README files
- Git

Terraform defines the secret containers only.

Secret values will be populated securely during the AWS deployment phase.

The integration from AWS Secrets Manager into Kubernetes will use a dedicated
workload identity mechanism rather than embedding AWS credentials inside Pods.