# Current state after observability metrics validation

Use this addendum when continuing the project after the 2026-07-12 observability metrics validation window.

## Fixed project theme

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

Short version:

```text
VM 기반 WEB/WAS/DB 운영환경을 직접 구성하고, 장애·성능·복구 문제를 로그와 지표로 분석하는 운영 포트폴리오
```

Do not reinterpret the project as:

```text
OpenKoda installation
Terraform showcase
AWS managed architecture showcase
Spring Boot sample app project
Kubernetes/EKS/GitOps work
Grafana dashboard-first project
```

Terraform, Ansible, Spring Boot, Nginx, PostgreSQL, NFS, restic, node_exporter, and Prometheus are supporting tools only.

## Completed state

```text
Phase 0. lab-runtime smoke test: completed
Phase 1. lab-full-min WEB/WAS/DB minimum environment: completed
Phase 2A. lab-full-ops storage validation: completed
Phase 2B. lab-full-ops backup artifact creation: completed as backup-artifact evidence
Phase 3. restore-lab DB/file/API recovery validation: completed
Phase 4A. observability logs/service/request-path baseline evidence: completed
Phase 4B. node_exporter + Prometheus scrape metrics evidence: completed
Phase 4C. metric-based DB service incident diagnosis: completed
```

## Recently merged PRs

```text
PR #125 [VALIDATION] Document observability baseline evidence
PR #126 [FIX] Define observability DB incident target default
PR #127 [ANSIBLE] Add node exporter observability baseline
PR #128 [ANSIBLE] Add Prometheus scrape observability baseline
PR #129 [DOCS] Add observability metrics runtime validation plan
PR #130 [FIX] Wait for Prometheus scrape targets before evidence
PR #131 [VALIDATION] Document observability metrics evidence
```

## Metrics runtime evidence window

Runtime evidence directory:

```text
.tmp/observability-metrics-20260712T121325Z
```

Local raw evidence archive:

```text
.tmp/observability-metrics-20260712T121325Z.tar.gz
archive_size=24K
```

Terraform destroy result:

```text
Destroy complete! Resources: 35 destroyed.
```

Raw evidence is local only and is not committed.

## Validated monitoring-enabled topology

```text
[Public Subnet]
- bastion-01
- nginx-01

[Private App Subnet]
- app-01

[Private DB Subnet]
- db-primary-01

[Private Storage Subnet]
- nfs-01

[Private Ops Subnet]
- backup-01
- mon-01
```

Terraform profile used:

```text
app_02=false
backup_node=true
loadgen_node=false
logging_node=false
monitoring_node=true
nat_gateway=true
storage_node=true
```

## Validated Prometheus scrape result

Prometheus on `mon-01` scraped node_exporter targets from:

```text
nginx-01:9100
app-01:9100
db-primary-01:9100
nfs-01:9100
backup-01:9100
```

Final post-fix playbook evidence matched:

```text
active_targets=5
healthy_targets=5
up_results=5
healthy_up_results=5
```

Supported claim:

```text
Prometheus scrape evidence validated host-level node_exporter targets for EC2 WEB/WAS/DB/Storage/Backup diagnosis.
```

## Metric-based DB service incident

Scenario:

```text
PostgreSQL service was stopped on db-primary-01 while node_exporter and Prometheus remained active.
```

Incident evidence:

```text
Before incident:
- /healthz 200
- /readyz 200
- /api/work-orders/summary 200
- up{instance="10.50.21.31:9100"}=1

During incident:
- PostgreSQL service inactive
- port 5432 closed
- /healthz 200
- /readyz 503
- /api/work-orders/summary 503
- up{instance="10.50.21.31:9100"}=1
- node_cpu_seconds_total for db-primary-01 still queryable

After recovery:
- PostgreSQL service active
- port 5432 LISTEN
- /readyz 200
- /api/work-orders/summary 200
- up{instance="10.50.21.31:9100"}=1
```

Supported claim:

```text
Prometheus metrics helped distinguish DB host reachability from DB service dependency failure.
```

## Runtime finding fixed during the same window

The first Prometheus evidence pass showed:

```text
/-/ready succeeded
activeTargets=[]
up query result=[]
```

The environment was not recreated. The same runtime window was used to:

```text
1. Confirm mon-01 could reach all node_exporter endpoints.
2. Confirm manual Prometheus config/restart produced activeTargets=5.
3. Merge PR #130 to wait for target registration and job-specific up results.
4. Pull main and rerun only observability-prometheus-scrape-baseline.yml.
5. Confirm the playbook produced active_targets=5 and healthy_up_results=5.
```

This is an important operating lesson: Prometheus `/-/ready` only proves that the server is ready, not that scrape targets have been registered and are healthy.

## Do not claim

```text
production monitoring maturity
Grafana dashboard readiness
Alertmanager notification maturity
PostgreSQL HA
automatic failover
SLO/SLA compliance
```

## Recommended next task

Do not jump to Grafana polish first.

Recommended next direction:

```text
[ALERT] Add minimal Prometheus alert rule for DB service dependency evidence
```

The purpose should be narrow:

```text
Add one or two Prometheus alert rules and validate the rule evaluation path, not Alertmanager notification maturity.
```

Alternative next direction:

```text
[LOGS] Add minimal log query evidence for Nginx/app incident correlation
```

Avoid opening another AWS runtime window until the next validation scenario is prepared.