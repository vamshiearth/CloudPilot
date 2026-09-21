# CloudPilot AWS Kubernetes Overlay

This directory contains AWS-specific Kubernetes resources.

The local Docker Desktop Kubernetes manifests remain unchanged.

AWS differences:

- Frontend Service uses ClusterIP instead of NodePort
- Public traffic enters through an AWS Application Load Balancer
- Ingress class is `alb`
- ALB target type is `ip`
- Persistent storage uses the `cloudpilot-gp3` StorageClass

The AWS Load Balancer Controller must be installed before applying the Ingress.

No AWS resources are created by these YAML files alone.

## Pod Identity

AWS workloads use EKS Pod Identity.

Service account mappings:

- Core: `cloudpilot-core`
- Audit: `cloudpilot-audit`
- Grafana: `cloudpilot-grafana`

No AWS access keys are stored in Kubernetes.

IAM permissions are assigned through EKS Pod Identity associations.