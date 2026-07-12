# Current state after DB service alert baseline prep

Use this addendum when continuing after PR #133.

## Fixed project theme

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

Short version:

```text
VM 기반 WEB/WAS/DB 운영환경을 직접 구성하고, 장애·성능·복구 문제를 로그와 지표로 분석하는 운영 포트폴리오
```

This is not a Grafana dashboard project, Terraform showcase, OpenKoda project, Kubernetes project, or AWS managed-architecture showcase.

## Current phase status

```text
Phase 0. lab-runtime smoke test: completed
Phase 1. lab-full-min WEB/WAS/DB minimum environment: completed
Phase 2A. lab-full-ops storage validation: completed
Phase 2B. lab-full-ops backup artifact creation: completed as backup-artifact evidence
Phase 3. restore-lab DB/file/API recovery validation: completed
Phase 4A. observability logs/service/request-path evidence: completed
Phase 4B. node_exporter + Prometheus scrape metrics evidence: completed
Phase 4C. metric-based DB service incident diagnosis: completed
Phase 4D. Prometheus DB service alert-rule baseline: prepared, runtime validation pending
```

## Most recent completed work

PR #133 added static preparation for Prometheus DB service alert rule validation.

Added:

```text
infra/ansible/playbooks/observability-prometheus-db-service-alert-baseline.yml
docs/03-runbooks/observability-prometheus-db-service-alert-baseline.md
```

No AWS resources were created by PR #133.

## Technical boundary

The current Prometheus baseline scrapes node_exporter only.

Therefore Prometheus does not directly evaluate:

```text
/readyz HTTP 503
/api/work-orders/summary HTTP 503
application DB dependency response body
```

Those remain request-path evidence collected through Nginx/app probes.

The alert-rule baseline instead evaluates a node/systemd condition:

```text
DB host is reachable from Prometheus, but postgresql.service is not active.
```

## PromQL rule design

```promql
up{job="node-exporter-operating-nodes",instance="<db-primary-01>:9100"} == 1
and on(instance, job)
node_systemd_unit_state{job="node-exporter-operating-nodes",instance="<db-primary-01>:9100",name="postgresql.service",state="active"} == 0
```

The `on(instance, job)` vector matching is required because `up` and `node_systemd_unit_state` do not have identical label sets.

## Playbook safety default

The alert validation incident is disabled by default:

```text
observability_run_db_alert_incident=false
```

Default execution should only:

```text
enable DB node_exporter systemd collector
write Prometheus rule file
write Prometheus config with rule_files
run promtool check rules
run promtool check config
verify the rule is loaded
verify DB systemd service metric is queryable
collect evidence
```

## Next runtime validation target

Recommended next PR/runtime sequence:

```text
[VALIDATION] Run Prometheus DB service alert rule evidence
```

Expected runtime sequence:

```text
1. Git Bash: create lab-full-ops with monitoring_node=true.
2. WSL: populate Ansible inventory including mon-01.
3. WSL: configure WEB/WAS/DB/Storage/Backup baseline.
4. WSL: run node_exporter baseline.
5. WSL: run Prometheus scrape baseline.
6. WSL: run observability-prometheus-db-service-alert-baseline.yml with incident disabled.
7. WSL: rerun the same playbook with observability_run_db_alert_incident=true.
8. WSL: confirm rule evaluation enters firing/pending state as designed.
9. WSL: confirm PostgreSQL is recovered after the incident.
10. Git Bash: destroy once.
11. GitHub: document evidence in docs/04-evidence.
```

## Future supported claim after runtime validation

Only after runtime evidence succeeds, claim:

```text
Prometheus rule evaluation detected PostgreSQL service inactivity while the DB host remained reachable.
```

## Do not claim

```text
Alertmanager notification maturity
paging or on-call workflow
production monitoring maturity
Grafana dashboard readiness
PostgreSQL HA
automatic failover
SLO/SLA compliance
```

## Cost and lifecycle reminder

Do not repeatedly create and destroy AWS resources.

Use this policy:

```text
prepare statically first -> apply once -> configure -> validate -> collect evidence -> destroy once
```

If an AWS runtime is already open and matches the needed topology, continue within that runtime instead of recreating it.
