# CloudPilot

CloudPilot is a multi-tenant SaaS operations platform for teams that need to plan work, manage access, understand usage, and operate services with better visibility. It combines project delivery, team administration, subscriptions, cost intelligence, audit history, and observability in one application.

## What CloudPilot provides

- **Work management:** Create projects, assign tasks, track status, and keep delivery visible.
- **Team operations:** Invite members and manage OWNER, ADMIN, and MEMBER permissions.
- **Subscriptions:** Track plans, usage, limits, feature access, and invitations.
- **Cost Intelligence:** Review budgets, projections, service breakdowns, anomaly signals, and advisory optimization insights.
- **Activity and audit:** Follow product activity in Core and persist audit events through the independent Audit Service.
- **Observability:** Inspect Prometheus metrics, Grafana dashboards, and OpenTelemetry traces through Tempo.
- **Noisy-neighbor signals:** Identify disproportionate tenant workload without throttling or changing tenant traffic.
- **Multi-tenant security:** Derive tenant context from the authenticated server-side principal and enforce permissions at the API boundary.
- **Deployment options:** Run locally with Docker Compose or use the included Kubernetes and Terraform foundations for a planned AWS deployment.

## How the application is used

1. An **OWNER** creates an organization, chooses a subscription, manages members, sets access rules, and reviews cost and platform health.
2. An **ADMIN** invites people, assigns roles, organizes projects, and keeps the operating flow ready for the team.
3. A **MEMBER** works on assigned projects and tasks, updates progress, and collaborates with the right context.
4. CloudPilot turns activity, workload, cost, and service-health data into signals that help the organization act before issues become surprises.

## Architecture

```text
React / Nginx -> Core Backend -> PostgreSQL
                         |      -> Redis
                         |      -> Kafka -> Audit Service -> Audit PostgreSQL
                         |
                         -> Micrometer -> Prometheus -> Grafana
                         -> OpenTelemetry Agent -> OTel Collector -> Tempo
```

The frontend sends `/api` requests to the Core Backend. Core owns business operations and publishes audit events to Kafka. The independent Audit Service consumes those events and stores them separately. Monitoring and tracing are kept outside the main business path so an observability outage does not stop normal work.

## Technology

| Area | Technology |
| --- | --- |
| Frontend | React, TypeScript, Vite, Nginx |
| Core API | Spring Boot, Java 21 |
| Data | PostgreSQL 16 |
| Cache | Redis 7 |
| Events | Apache Kafka 4 |
| Audit | Independent Spring Boot service and audit database |
| Metrics | Micrometer, Prometheus, Grafana |
| Tracing | OpenTelemetry Collector and Grafana Tempo |
| Infrastructure | Docker Compose, Kubernetes, Terraform, AWS foundations |

## Run locally with Docker Compose

### Prerequisites

- Docker Desktop with Docker Compose

Create a local `.env` file in the repository root:

```env
POSTGRES_PASSWORD=change-me
JWT_SECRET=change-me
```

Use strong local values for both variables. Never commit `.env`.

Start the full environment:

```bash
docker compose up -d --build
```

Open the application at [http://localhost:3000](http://localhost:3000/).

Useful commands:

```bash
docker compose ps
docker compose logs -f
docker compose logs -f core-backend
docker compose logs -f audit-service
docker compose down
```

To remove persistent PostgreSQL and Kafka volumes as well:

```bash
docker compose down -v
```

### Compose ports

| Service | Port |
| --- | ---: |
| Frontend | 3000 |
| Core Backend | 8081 |
| Audit Service | 8082 |
| PostgreSQL | 5433 |
| Redis | 6379 |
| Kafka | 9092 |

PostgreSQL and Kafka use named Docker volumes. Redis is an ephemeral cache. The frontend is served by Nginx and proxies `/api` requests over the Compose network.

## Kubernetes deployment

The `k8s/` directory contains a Docker Desktop Kubernetes deployment with PostgreSQL, Redis, Kafka, Core, Audit, Frontend, Prometheus, Grafana, Tempo, the OpenTelemetry Collector, Kafka Exporter, and Blackbox Exporter.

Prerequisites:

- Docker Desktop with Kubernetes enabled
- `kubectl`
- Local images for the Core Backend, Audit Service, and frontend

Apply the base resources:

```bash
kubectl config current-context
kubectl apply -f k8s/namespace.yaml
kubectl config set-context --current --namespace=cloudpilot
kubectl apply -f k8s/configmap.yaml
kubectl create secret generic cloudpilot-secrets --from-env-file=.env
```

Start infrastructure and initialize Kafka:

```bash
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/redis.yaml
kubectl apply -f k8s/kafka.yaml
kubectl get pods
kubectl apply -f k8s/kafka-topic-job.yaml
kubectl get jobs
```

Deploy the application:

```bash
kubectl apply -f k8s/audit-service.yaml
kubectl apply -f k8s/core-backend.yaml
kubectl apply -f k8s/frontend.yaml
```

Open the Kubernetes frontend at [http://localhost:30000](http://localhost:30000/).

## Observability

The Kubernetes configuration exposes these local endpoints:

| Component | URL |
| --- | --- |
| Frontend | `http://localhost:30000` |
| Prometheus | `http://localhost:30090` |
| Grafana | `http://localhost:30091` |

The provisioned **CloudPilot Overview** Grafana dashboard includes request rates and errors, latency, JVM and CPU data, database pools, audit processing, Kafka health and consumer lag, Redis fallback activity, and aggregate tenant workload indicators. Prometheus metrics intentionally do not contain tenant IDs as labels.

Core and Audit expose:

```text
/actuator/health
/actuator/metrics
/actuator/prometheus
```

OpenTelemetry traces are sent to the Collector and stored in Tempo. The main provisioning files are `k8s/grafana-cloudpilot-overview.json`, `k8s/grafana-dashboard-configmap.yaml`, `k8s/grafana-dashboard-provider.yaml`, `k8s/otel-collector.yaml`, and `k8s/tempo.yaml`.

For local Kubernetes use, PostgreSQL, Kafka, and Tempo have persistent storage. Redis, Prometheus, Grafana, the Collector, exporters, and application pods are disposable or configuration-reprovisioned workloads. Prometheus history is lost after pod replacement.

## Cost Intelligence

Cost Intelligence is tenant-aware and available through authenticated APIs and the React dashboard. It currently supports:

- Simulated daily cost data
- Budget tracking and monthly projections
- Cost by service
- Anomaly detection
- Advisory optimization insights
- Aggregate Prometheus cost metrics
- Grafana visualization

An AWS Cost Explorer adapter boundary and mapper are included for future integration. AWS billing integration is **disabled** in the current build and does not create an AWS client or make AWS API calls.

## Tenant isolation and workload signals

Tenant identity is derived from the authenticated server-side context. Client-provided tenant IDs are not used to override it. OWNER and ADMIN capabilities are enforced by role and permission checks, while MEMBER access is limited to operational project and task actions.

Noisy-neighbor detection is observation-only. It tracks request pressure, latency pressure, error rate, and recent activity to classify workload as `NORMAL`, `ELEVATED`, or `NOISY_CANDIDATE`. It does not throttle, reject, downgrade, or terminate tenant traffic.

An OWNER can inspect the current tenant assessment with:

```text
GET /api/tenant/workload/me/assessment
```

The detector state is pod-local and resets when Core restarts. A multi-replica production deployment would require shared or aggregated detector state.

## Reliability notes

- Redis failures fall back to PostgreSQL for subscription usage.
- Audit Service outages do not stop Core business operations; Kafka retains events for later consumption when available.
- Kafka publication and database writes have a documented dual-write gap. A transactional outbox remains a future reliability improvement.
- Current readiness checks are closer to liveness checks than full dependency-aware readiness checks.
- Local monitoring is not internet-facing by default.

## Development

Build the frontend:

```bash
cd frontend
npm install
npm run build
npm run lint
```

Build the Core Backend artifact:

```bash
cd backend
./mvnw clean package -Dmaven.test.skip=true
```

On Windows, use `mvnw.cmd`. The production Docker build skips test compilation because the current Core test sources contain unrelated compilation issues; the command above is not a passing test-suite result.

The main project directories are:

```text
backend/        Core Spring Boot service
audit-service/  Independent audit consumer and API
frontend/       React application
docker/         PostgreSQL initialization
k8s/            Kubernetes and observability manifests
scripts/        Local workload and validation scripts
terraform/      Planned AWS infrastructure
```

## Terraform AWS foundation

The `terraform/` directory describes a planned AWS deployment. It has been validated as infrastructure code, but `terraform apply` has not been run and no AWS resources are created by this repository automatically.

The plan includes:

- VPC networking with public and private subnets
- EKS and a managed node group
- ECR repositories for Core, Audit, and Frontend images
- Encrypted EBS gp3 storage through the CSI driver
- IAM roles and EKS Pod Identity
- Secrets Manager containers without committed secret values
- AWS Load Balancer Controller and ALB ingress foundations

The showcase configuration keeps NAT Gateway, MSK, RDS, and ElastiCache out of the current plan. Review and customize the Terraform variables before any production deployment.

## Status

CloudPilot is an active project prototype. Review the configuration, security controls, persistence choices, and observability retention before exposing it to production traffic.
