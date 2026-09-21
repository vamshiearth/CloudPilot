# AWS Storage Notes

CloudPilot uses the Terraform-managed StorageClass:

`cloudpilot-gp3`

Persistent workloads:

- PostgreSQL: 5 GiB
- Kafka: 5 GiB
- Tempo: 5 GiB

Provisioner:

`ebs.csi.aws.com`

Properties:

- gp3
- encrypted
- expandable
- WaitForFirstConsumer
- Delete reclaim policy for showcase/dev use