## Phase 15 Final Implementation

Implemented:

- daily tenant cost persistence
- monthly tenant budgets
- simulated development cost data
- cost aggregation
- month-to-date totals
- projected monthly spending
- service-category breakdown
- budget health classification
- anomaly detection
- cost insight generation
- authenticated tenant-scoped REST APIs
- React Cost Intelligence dashboard
- aggregate Prometheus metrics
- Grafana Cost Intelligence panels
- AWS Cost Explorer adapter boundary

### Security

Tenant identity is derived from authenticated server-side context.

Client-supplied tenant IDs are not trusted.

OWNER may modify the monthly budget.

MEMBER may view cost intelligence but may not modify the budget.

Prometheus metrics contain no tenant-identifying labels.

### Cost Anomaly Detection

Baseline:

previous 7 completed days

Minimum history:

3 days

Thresholds:

- NORMAL: < 1.25
- ELEVATED: >= 1.25 and < 1.75
- ANOMALY_CANDIDATE: >= 1.75

Detection is advisory only.

### Budget Health

- HEALTHY: < 75%
- WATCH: >= 75% and < 90%
- HIGH: >= 90% and < 100%
- OVER_BUDGET: >= 100%
- NO_BUDGET: no configured budget

### Observability

Aggregate Prometheus metrics:

- cloudpilot_cost_tenants_with_budget
- cloudpilot_cost_over_budget_tenants
- cloudpilot_cost_anomaly_candidates
- cloudpilot_cost_max_pressure
- cloudpilot_cost_max_budget_usage_percent

No tenant IDs are used as Prometheus labels.

Grafana dashboard:

`CloudPilot Overview`

UID:

`cloudpilot-overview`

Current panel count:

30

### AWS Integration

The AWS Cost Explorer adapter is designed but disabled.

Configuration:

`cloudpilot.cost.aws.enabled=false`

No AWS Cost Explorer calls are made in the current environment.

Before enabling AWS ingestion, the cost-record deduplication key must be
refined because multiple AWS services can map to the same normalized service
category on the same day.

### Known Follow-Up

Malformed or overlong daily cost date ranges currently pass through an
existing error-mapping path that may return HTTP 401 instead of the intended
HTTP 400.

Normal authenticated cost requests are unaffected.

### Safety

CloudPilot Cost Intelligence does not automatically:

- delete infrastructure
- resize workloads
- terminate Pods
- modify AWS resources
- alter subscription plans
- enforce budget limits

Recommendations remain advisory.
# CloudPilot Cost Intelligence

## Purpose

CloudPilot Cost Intelligence helps each tenant understand:

- how much cloud infrastructure is costing
- which services are responsible for the cost
- how spending changes over time
- whether spending is above expected levels
- whether the tenant is approaching its monthly budget
- where potential optimization opportunities exist

Cost Intelligence is observation and recommendation oriented.

It does not automatically delete infrastructure, resize workloads,
or modify cloud resources.

---

## Multi-Tenant Security

All cost information belongs to a tenant.

The authenticated server-side tenant context is the source of truth.

The frontend must never provide or control `tenant_id`.

Every cost query must be scoped using the authenticated tenant context.

A user from Tenant A must never see cost information belonging to Tenant B.

---

## Initial Data Source

The first implementation uses simulated/local cost records.

Initial source:

`SIMULATED`

Future source:

`AWS_COST_EXPLORER`

The application must keep the cost model independent of the cloud provider
so additional providers can be added later.

### AWS ingestion deduplication limitation

The current simulated-data uniqueness model uses tenant, date, category, and source.

Before enabling AWS Cost Explorer ingestion, this must be refined because
multiple provider services may map to the same normalized category on the
same day.

---

## Currency

Initial currency:

`USD`

All monetary calculations use decimal values.

Floating-point values must not be used for persisted monetary amounts.

Java implementation should use:

`BigDecimal`

---

## Cost Granularity

Initial cost granularity is daily.

A cost record represents:

- tenant
- cloud provider
- cloud service
- usage date
- cost amount
- currency
- source
- estimated/final status
- observation timestamp

Example:

Tenant:

`Acme Corporation`

Provider:

`AWS`

Service:

`Amazon EKS`

Usage date:

`2026-09-21`

Cost:

`4.82 USD`

---

## Supported Cloud Services

Initial normalized service categories:

- COMPUTE
- KUBERNETES
- DATABASE
- STORAGE
- NETWORK
- CACHE
- MESSAGING
- OBSERVABILITY
- CONTAINER_REGISTRY
- OTHER

These are CloudPilot categories rather than AWS-specific product names.

Provider-specific names can later be mapped into these categories.

Examples:

`Amazon EC2` -> `COMPUTE`

`Amazon EKS` -> `KUBERNETES`

`Amazon EBS` -> `STORAGE`

`Amazon ECR` -> `CONTAINER_REGISTRY`

---

## Cost Metrics

CloudPilot will calculate:

### Today Cost

Sum of the tenant's costs for the current day.

### Month-to-Date Cost

Sum from the first day of the current month through today.

### Previous Month Cost

Total cost for the previous calendar month.

### Projected Monthly Cost

Initial projection:

`average daily cost for current month × number of days in month`

Projection is informational only.

### Daily Average

`month-to-date cost / elapsed days`

### Cost Change

Percentage change compared with the previous comparable period.

### Cost by Service

Cost grouped by CloudPilot service category.

### Highest Cost Service

The service category with the largest cost during the requested period.

---

## Budget Model

Each tenant may configure one monthly budget.

Example:

Monthly budget:

`$300`

Alert thresholds:

- 50%
- 75%
- 90%
- 100%

Budget usage percentage:

`month-to-date cost / monthly budget × 100`

Example:

Month-to-date cost:

`$225`

Budget:

`$300`

Usage:

`75%`

---

## Cost Health

CloudPilot classifies tenant cost status as:

### HEALTHY

Budget usage is below 75%.

### WATCH

Budget usage is at least 75% but below 90%.

### HIGH

Budget usage is at least 90% but below 100%.

### OVER_BUDGET

Budget usage is at least 100%.

A tenant without a configured budget has:

`NO_BUDGET`

These classifications are informational.

CloudPilot does not automatically stop workloads.

---

## Cost Anomaly Detection

Initial anomaly detection compares today's cost with the tenant's recent
daily average.

Definitions:

`baseline = average cost of previous 7 completed days`

`cost pressure = today's cost / baseline`

Initial classification:

### NORMAL

Cost pressure below `1.25`

### ELEVATED

Cost pressure at least `1.25` and below `1.75`

### ANOMALY_CANDIDATE

Cost pressure at least `1.75`

Minimum sample requirement:

At least 3 previous daily cost records are required.

If there is insufficient historical data:

`INSUFFICIENT_DATA`

Anomaly classification is observation-only.

---

## Optimization Insights

Initial CloudPilot insights may include:

### HIGH_KUBERNETES_COST

Kubernetes represents a large share of tenant spending.

### STORAGE_GROWTH

Storage spending is increasing significantly.

### NETWORK_COST_SPIKE

Network spending increased substantially compared with its recent baseline.

### BUDGET_RISK

Projected monthly spending is expected to exceed the configured budget.

### COST_ANOMALY

Current daily spending is significantly above historical baseline.

Insights must include supporting measurements rather than only a label.

Example:

`Kubernetes represents 63% of month-to-date spending.`

---

## APIs Planned

Initial tenant-facing APIs:

`GET /api/costs/summary`

Returns:

- today cost
- month-to-date cost
- projected monthly cost
- monthly budget
- budget usage
- cost health

---

`GET /api/costs/daily`

Returns daily cost history for the authenticated tenant.

---

`GET /api/costs/services`

Returns cost grouped by service category.

---

`GET /api/costs/insights`

Returns tenant-specific cost observations and optimization insights.

---

`GET /api/costs/anomaly`

Returns current cost anomaly assessment.

---

`GET /api/costs/budget`

Returns the authenticated tenant's budget.

---

`PUT /api/costs/budget`

Creates or updates the authenticated tenant's monthly budget.

The tenant ID is never accepted from the request.

---

## Roles

Initial permissions:

### OWNER

- view all tenant cost intelligence
- configure monthly budget
- view cost insights

### MEMBER

- view tenant cost summary
- view cost history
- view cost insights

Members cannot change the monthly budget.

---

## Observability

Prometheus metrics must remain low-cardinality.

Do not expose tenant IDs as Prometheus labels.

Safe aggregate metrics may later include:

- number of tenants over budget
- number of anomaly candidates
- maximum cost pressure
- number of tenants with configured budgets

Tenant-specific cost context may exist in authenticated APIs and traces,
but not Prometheus labels.

---

## Safety

Cost Intelligence is advisory.

CloudPilot will not automatically:

- terminate instances
- delete Pods
- delete storage
- resize infrastructure
- modify AWS resources
- change subscription plans
- enforce budgets

Any future resource-changing optimization must require explicit user action.

---

## Phase 15 Initial Data Flow

SIMULATED COST DATA

        |
        v

CloudPilot Cost Records

        |
        +----------------------+
        |                      |
        v                      v

Cost Aggregation         Anomaly Detector

        |                      |
        +-----------+----------+
                    |
                    v

              Cost Insights

                    |
                    v

              REST APIs

                    |
                    v

             React Dashboard

Later:

AWS Cost Explorer
        |
        v
Cost Ingestion
        |
        v
same CloudPilot cost model

---

## Phase 15 Implementation Order

1. Cost model and rules
2. Cost persistence
3. Simulated cost data
4. Cost aggregation service
5. Budget management
6. Anomaly detection
7. Cost insight engine
8. REST APIs
9. React Cost Intelligence dashboard
10. Prometheus aggregate metrics
11. Security and tenant-isolation testing
12. AWS Cost Explorer adapter design
13. Failure testing
14. Documentation and final acceptance
