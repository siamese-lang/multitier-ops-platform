# Roadmap

This roadmap separates completed validation work from future operating-system expansion work.

The project theme is fixed:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

## Status summary

```text
Phase 0. lab-runtime smoke test: completed
Phase 1. lab-full-min WEB/WAS/DB minimum environment: completed
Phase 2. lab-full-ops storage/backup/observability expansion: next
Phase 3. restore-lab recovery validation: future, required
Phase 4. advanced incident reports and failover: future, optional after core recovery
```

## Phase 0. lab-runtime smoke test — completed

Purpose:

```text
Verify that the repository can create a temporary EC2 lab,
reach private nodes through a bastion,
pull dependencies through NAT,
run a workload,
collect evidence,
and destroy the environment.
```

Completed evidence:

```text
Terraform lab-runtime creation
Bastion-based Ansible control path
private app node NAT egress
Docker-based workload smoke test
health check evidence
Terraform destroy
```

Scope boundary:

```text
This phase does not prove the final operating architecture.
It only proves the lab execution mechanism and initial workload feasibility.
```

## Phase 1. lab-full-min WEB/WAS/DB — completed

Validated topology:

```text
[Public Subnet]
- bastion-01
- nginx-01

[Private App Subnet]
- app-01
- app-02

[Private DB Subnet]
- db-primary-01
```

Validated operating path:

```text
operator -> nginx-01:443 -> app-01/app-02:8080 -> db-primary-01:5432
```

Completed build items:

```text
Terraform lab-full-min baseline
Ansible inventory/control path
PostgreSQL primary configuration
ops-sample-service systemd deployment
Nginx HTTPS reverse proxy
GitHub Actions jar artifact workflow
Nginx HTTPS ingress rule
```

Completed validation scenarios:

```text
WEB/WAS/DB integrated normal path
app-01 failure and Nginx upstream bypass
app-01/app-02 rolling restart continuity
DB-backed concurrent request observation
PostgreSQL failure and recovery isolation
Terraform cleanup after validation
```

Key evidence documents:

```text
docs/04-evidence/lab-full-min-web-was-db-integrated-validation.md
docs/04-evidence/lab-full-min-continuous-operations-validation.md
```

Important boundary:

```text
Do not keep adding similar lab-full-min drills unless they clearly unlock Phase 2.
The minimum WEB/WAS/DB operating story is already strong enough.
```

## Phase 2. lab-full-ops — next

Purpose:

```text
Extend the validated WEB/WAS/DB environment into a fuller operating environment
with file storage, backup, observability, and load generation tiers.
```

Target topology:

```text
[Public Subnet]
- bastion-01
- nginx-01
- optionally nginx-02 later

[Private App Subnet]
- app-01
- app-02
- optionally app-03 later

[Private DB Subnet]
- db-primary-01
- optionally db-standby-01 later

[Private Storage Subnet]
- nfs-01

[Private Ops Subnet]
- mon-01
- log-01
- backup-01
- loadgen-01
```

Recommended implementation order:

```text
1. Design lab-full-ops topology and tier responsibilities
2. Add Terraform support for nfs-01, backup-01, mon-01, log-01, loadgen-01
3. Add Ansible baseline control path for new nodes
4. Configure nfs-01 or filesystem storage
5. Add application/file endpoint or test harness for file metadata consistency
6. Run file storage failure drill
7. Configure pg_dump and restic backup workflow
8. Validate restore into restore-lab
9. Add Prometheus/Grafana/Loki for evidence collection
10. Write incident reports using logs and metrics
```

First recommended next issue:

```text
[DESIGN] lab-full-ops 파일저장소·백업·관측성 확장 설계
```

## Phase 3. restore-lab — required future milestone

Purpose:

```text
Prove that backup artifacts can restore a working system in a separate environment.
```

Target flow:

```text
Create lab-full data
Create PostgreSQL dump
Create file backup with restic
Destroy lab-full resources
Create restore-lab resources
Restore DB and file backup
Validate DB rows, file checksum, and HTTP download
Document recovery time and recovery gaps
```

Required evidence:

```text
pg_dump command output
restic snapshot output
Terraform destroy and new apply
restore command output
checksum comparison
HTTP endpoint verification
incident/recovery report
```

This phase is important because it proves recovery rather than merely backup creation.

## Phase 4. Advanced operations — optional after core recovery

Possible items:

```text
PostgreSQL standby and promote
Nginx active/active or failover entrypoint
HikariCP and DB connection pool bottleneck analysis
Tomcat thread saturation
Prometheus alert rule and Alertmanager notification
Loki log query-based incident diagnosis
p95/p99 latency comparison before/after tuning
```

These should be added only after Phase 2 and Phase 3 produce evidence.

## Work that should stop now

Avoid the following unless a clear evidence gap is identified:

```text
more lab-full-min drills with the same pattern
OpenKoda UI or feature work
Terraform-only refactoring without an operating scenario
GitHub issue/PR churn without validation value
dashboard-first monitoring work
managed AWS replacement of VM-based tiers
```

## Definition of done for the portfolio

The project reaches a strong portfolio state when the repository includes:

```text
architecture documents
Terraform environment lifecycle
Ansible configuration/runbooks
WEB/WAS/DB/file/observability/backup tier descriptions
at least four incident or recovery scenarios
logs/metrics/command evidence
restore-lab recovery proof
portfolio summary for reviewers
```
