# CloudPilot

CloudPilot is a multi-tenant SaaS platform built with Spring Boot, React,
PostgreSQL, Redis, Kafka, and an independent audit service. It provides
tenant-scoped access control, project and task management, subscriptions,
audit events, monitoring, tracing, and observation-only noisy-neighbor
detection.

## Architecture

```text
React / Nginx -> Core Backend -> PostgreSQL
                         |      -> Redis
                         |      -> Kafka -> Audit Service -> Audit PostgreSQL
                         |
                         -> Micrometer -> Prometheus -> Grafana
                         -> OpenTelemetry Java Agent -> OTel Collector -> Tempo
```

The frontend sends `/api` requests to the Core Backend. Core owns business
operations and publishes audit events to Kafka. The Audit Service consumes
those events and stores them separately. Observability is separate from the
business path, so monitoring outages do not stop normal application work.

## Requirements

Docker Compose:

- Docker Desktop
- Docker Compose

Kubernetes:

- Docker Desktop with Kubernetes enabled
- `kubectl`
- Local images for the Core Backend, Audit Service, and frontend

## Configuration

Create a local `.env` file from `.env.example`:

```env
POSTGRES_PASSWORD=change-me
JWT_SECRET=change-me
```

Never commit `.env`; it is ignored by the root `.gitignore`.

## Run with Docker Compose

Start all services from the repository root:

```bash
docker compose up -d --build
```

Check status and logs:

```bash
docker compose ps
docker compose logs -f
```

Service-specific logs are available with commands such as:

```bash
docker compose logs -f core-backend
docker compose logs -f audit-service
docker compose logs -f kafka
```

Open the application at [http://localhost:3000](http://localhost:3000/).

Stop the environment while preserving persistent data:

```bash
docker compose down
```

Reset the environment and delete PostgreSQL and Kafka data:

```bash
docker compose down -v
```

### Docker Compose ports

| Service | Port |
| --- | ---: |
| Frontend | 3000 |
| Core Backend | 8081 |
| Audit Service | 8082 |
| PostgreSQL | 5433 |
| Redis | 6379 |
| Kafka | 9092 |

PostgreSQL and Kafka use named Docker volumes. Redis is an ephemeral cache.
The frontend is served by Nginx and proxies `/api` requests to Core over the
Compose network.

## Run on Kubernetes

Verify the Docker Desktop context:

```bash
kubectl config current-context
```

The expected context is `docker-desktop`.

Create the namespace, configuration, and secrets:

```bash
kubectl apply -f k8s/namespace.yaml
kubectl config set-context --current --namespace=cloudpilot
kubectl apply -f k8s/configmap.yaml
kubectl create secret generic cloudpilot-secrets --from-env-file=.env
```

Create infrastructure:

```bash
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/redis.yaml
kubectl apply -f k8s/kafka.yaml
kubectl get pods
kubectl get pvc
```

Initialize Kafka after the broker is ready:

```bash
kubectl apply -f k8s/kafka-topic-job.yaml
kubectl get jobs
kubectl logs job/kafka-topic-init
```

The `cloudpilot.events` topic uses three partitions and replication factor one
for local development.

Deploy the application:

```bash
kubectl apply -f k8s/audit-service.yaml
kubectl apply -f k8s/core-backend.yaml
kubectl apply -f k8s/frontend.yaml
```

Open the Kubernetes frontend at
[http://localhost:30000](http://localhost:30000/).

Useful status and log commands:

```bash
kubectl get all
kubectl get pvc
kubectl logs deployment/core-backend
kubectl logs deployment/audit-service
kubectl logs deployment/frontend
kubectl logs kafka-0
kubectl logs postgres-0
```

PostgreSQL, Kafka, and Tempo use persistent storage. Redis, Core, Audit, and
Frontend are disposable workloads. Application deployments recover deleted
pods, and the frontend preserves JWTs during temporary 502, 503, and network
failures.

## Monitoring and observability

The Kubernetes environment includes Prometheus, Grafana, Tempo, the
OpenTelemetry Collector, Kafka Exporter, and Blackbox Exporter.

| Component | Exposure | Local access |
| --- | --- | --- |
| Frontend | NodePort | `http://localhost:30000` |
| Prometheus | NodePort | `http://localhost:30090` |
| Grafana | NodePort | `http://localhost:30091` |
| Core Backend | ClusterIP | 8081 |
| Audit Service | ClusterIP | 8082 |
| OTel Collector | ClusterIP | 4317 gRPC, 4318 HTTP |
| Tempo | ClusterIP | 3200 HTTP |
| Kafka Exporter | ClusterIP | 9308 metrics |
| Blackbox Exporter | ClusterIP | 9115 probe endpoint |

Only the frontend, Prometheus, and Grafana are exposed through NodePorts.
Kafka is a protocol endpoint rather than a browser dashboard; use Grafana and
Prometheus for Kafka visibility.

Core and Audit expose `/actuator/health`, `/actuator/metrics`, and
`/actuator/prometheus`. Metrics include HTTP requests, JVM and process data,
HikariCP, Kafka consumers, and Redis fallback activity:

```text
cloudpilot_redis_fallback_total{cache="subscription-usage"}
```

Grafana is provisioned with Prometheus and Tempo data sources and the
**CloudPilot Overview** dashboard. It includes request rate and errors,
latency, JVM and CPU, database pools, audit processing, consumer lag, Kafka
health and throughput, Redis fallback rate, and tenant workload indicators.

Provisioning files:

```text
k8s/grafana.yaml
k8s/grafana-dashboard-configmap.yaml
k8s/grafana-dashboard-provider.yaml
k8s/grafana-cloudpilot-overview.json
```

The OpenTelemetry Java Agent is version `2.31.1`. Core and Audit export OTLP
HTTP traces to:

```text
http://otel-collector:4318/v1/traces
```

Tempo stores traces on the `tempo-data` persistent volume. Its metrics
generator produces service-graph and span metrics and remote-writes them to
Prometheus. Grafana connects traces and metrics through Tempo and Prometheus
configuration.

### Observability persistence and limitations

| Component | Persistence |
| --- | --- |
| PostgreSQL | PersistentVolumeClaim, 5Gi |
| Kafka | PersistentVolumeClaim, 5Gi |
| Tempo | PersistentVolumeClaim, 5Gi |
| Redis | Ephemeral cache |
| Prometheus | Ephemeral metrics storage |
| Grafana | Provisioned configuration, no PVC |
| Collector and exporters | Stateless |

Prometheus history is lost after pod replacement. Collector outage recovery
does not guarantee delivery of traces generated while it is down. Trace
sampling is configured as `always_on` for local validation and should be tuned
before production use.

## Tenant workload detection

CloudPilot includes observation-only noisy-neighbor detection for identifying
tenants that create disproportionate shared workload. It does not throttle,
reject, downgrade, or terminate tenant traffic.

Tenant identity comes from the authenticated server-side context. The frontend
cannot override it, and tenant IDs are intentionally excluded from Prometheus
labels. Core tracks request counts, status codes, duration, latency, requests
per minute, error rate, and recent activity in pod-local memory.

Detection uses request pressure, latency pressure, and a combined score weighted
70% toward request pressure and 30% toward latency. Default classifications are
`NORMAL`, `ELEVATED`, and `NOISY_CANDIDATE`. Detection requires at least two
active tenants and ten platform requests. Thresholds are development heuristics
and should be tuned for production.

Tenant OWNER users can inspect their own assessment:

```text
GET /api/tenant/workload/me/assessment
```

The API derives tenant identity from the authenticated principal and does not
accept a tenant ID path or query parameter.

Repeatable two-tenant load generation is available at:

```text
scripts/noisy-neighbor-load.ps1
```

JWTs are supplied at runtime and are not stored in the script. Detector state
is pod-local and ephemeral; restarting Core resets tracking while business data
and existing JWTs remain valid. A multi-replica deployment would require shared
or aggregated detector state.

## Reliability and security notes

- Redis failures fall back to PostgreSQL for subscription usage.
- Audit Service outages do not stop Core business operations; Kafka retains
  events for later consumption when available.
- Kafka publication and database writes have a dual-write gap. A transactional
  outbox is a future reliability improvement.
- Core and Audit readiness checks are currently closer to liveness checks than
  dependency-aware readiness checks.
- Secrets are supplied through `.env` or Kubernetes Secrets and are not stored
  in committed manifests.
- Monitoring services are configured for local development and are not
  internet-facing by default.

## Development notes

```text
backend/   Spring Boot services and tests
frontend/  React application and Vite configuration
k8s/       Kubernetes manifests and observability provisioning
scripts/   Local validation and workload simulation scripts
```

Build the frontend locally:

```bash
cd frontend
npm install
npm run build
```

Build the Core Backend artifact:

```bash
cd backend
./mvnw clean package -Dmaven.test.skip=true
```

The current Core test sources contain unrelated compilation issues, so the
production Docker build skips test compilation. That command is not a passing
test-suite result.

## Phase 13 - Terraform Infrastructure as Code

CloudPilot includes Terraform definitions for a planned AWS deployment.

### Current deployment mode

Terraform configuration has been validated against AWS, but infrastructure has
not been provisioned. `terraform apply` has not been run.

### Planned AWS architecture

Terraform defines:

- VPC with two public and two private subnets
- Internet Gateway and an optional NAT Gateway, disabled by default
- Security groups
- Three ECR repositories
- Amazon EKS cluster and one managed node group
- EBS CSI driver and an encrypted gp3 Kubernetes StorageClass
- Secrets Manager secret containers
- EKS Pod Identity roles and associations
- AWS Load Balancer Controller architecture
- AWS-specific Kubernetes ingress overlay

### Cost-conscious showcase configuration

Current dev defaults:

- EKS node count: 1
- Instance type: `t3.xlarge`
- Node disk: 40 GiB
- NAT Gateway: disabled
- PostgreSQL storage: 5 GiB
- Kafka storage: 5 GiB
- Tempo storage: 5 GiB

Managed MSK, RDS, and ElastiCache are intentionally not included in the
current showcase plan.

### Container registry

Separate ECR repositories are defined for the Core Backend, Audit Service, and
Frontend. Image scanning is enabled and lifecycle policies retain only the
latest 10 images.

### IAM

Terraform defines separate roles for the EKS control plane, EKS worker nodes,
Core workload, Audit workload, and Grafana workload. Application workloads use
EKS Pod Identity rather than embedded AWS access keys. Secrets access is
workload-specific and least-privilege.

### Secrets

Terraform creates Secrets Manager containers only. Secret values are not stored
in Terraform source, Terraform plan files, Kubernetes YAML, Git, or Docker
images.

### Storage

Kubernetes persistent workloads use an encrypted gp3 StorageClass backed by the
AWS EBS CSI driver. Planned PVC sizes are PostgreSQL 5 GiB, Kafka 5 GiB, and
Tempo 5 GiB.

### Ingress

The AWS deployment uses:

```text
Internet -> Application Load Balancer -> ALB Ingress -> Frontend ClusterIP
```

The local Docker Desktop Kubernetes NodePort configuration remains unchanged.

### Validation

Verified:

- `terraform fmt`
- `terraform validate`
- AWS provider initialization
- Kubernetes provider initialization
- AWS identity
- Static Terraform plan

Latest plan summary:

- 54 resources to add
- 0 to change
- 0 to destroy

The plan contains EKS, ECR, Secrets Manager, networking, storage, and IAM.
It does not contain NAT Gateway, MSK, RDS, or ElastiCache resources. No secret
values were present in the plan.

### Safety

`terraform apply` has not been run. No AWS resources were created during
Phase 13.