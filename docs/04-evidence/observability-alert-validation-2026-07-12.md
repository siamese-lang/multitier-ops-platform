# Observability alert validation evidence - 2026-07-12

## Project scope

Fixed project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

This validation covers the Prometheus DB service alert-rule runtime window after the completed observability metrics validation.

The goal was not to prove production alerting, Alertmanager notification maturity, paging workflow, or Grafana readiness. The goal was narrower: validate that Prometheus rule evaluation can detect PostgreSQL service inactivity while the DB host remains reachable through node_exporter metrics.

## Validation claim

Narrow claim supported by this runtime window:

```text
Prometheus rule evaluation detected PostgreSQL service inactivity while the DB host remained reachable.
```

This validation does not claim:

```text
Alertmanager notification maturity
paging or on-call workflow
production monitoring maturity
Grafana dashboard readiness
PostgreSQL HA
automatic failover
SLO/SLA compliance
```

## Runtime window

Runtime evidence directory:

```text
.tmp/observability-alert-20260712T131524Z
```

Local raw evidence archive:

```text
.tmp/observability-alert-20260712T131524Z.tar.gz
archive_size=25K
```

Raw runtime evidence remains local and is not committed.

## Terraform lifecycle evidence

Terraform destroy completed after evidence collection:

```text
Destroy complete! Resources: 35 destroyed.
```

A post-destroy state-list evidence file was preserved:

```text
terraform-state-list-after-destroy.txt
```

The post-destroy state-list output was empty, indicating that Terraform-managed resources for this runtime window were removed.

## Runtime topology

The runtime used the monitoring-enabled reduced `lab-full-ops` profile:

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

Validation profile:

```text
app_02=false
backup_node=true
loadgen_node=false
logging_node=false
monitoring_node=true
nat_gateway=true
storage_node=true
```

The observed DB node exporter instance during this validation was:

```text
db_node_exporter_instance=10.50.21.30:9100
```

## Baseline configuration evidence

The runtime executed the normal operating baseline before alert validation:

```text
lab-full-ops-postgresql-primary.txt
lab-full-ops-nfs-storage-baseline.txt
lab-full-ops-app-nfs-mount-baseline.txt
lab-full-ops-ops-sample-service.txt
lab-full-ops-nginx-reverse-proxy.txt
```

Normal WEB/WAS/DB path was checked before alert validation:

```text
/healthz 200
/readyz 200
/api/work-orders/summary 200
```

## Metrics baseline evidence

The node_exporter and Prometheus scrape baselines were executed before the alert rule validation:

```text
observability-node-exporter-baseline.txt
observability-prometheus-scrape-baseline.txt
```

The Prometheus scrape baseline output contained:

```text
active_targets=5 healthy_targets=1 expected=5
up_results=5 healthy_up_results=5 expected=5
```

This means the job-specific `up` query showed all five expected node_exporter targets as healthy. The `targets` API-derived `healthy_targets=1` observation was kept as a runtime finding rather than overstated as a full target-health success.

The alert validation itself did not depend on the global target health counter. It depended on the DB-specific Prometheus queries that follow:

```text
up{job="node-exporter-operating-nodes",instance="10.50.21.30:9100"} = 1
node_systemd_unit_state{job="node-exporter-operating-nodes",instance="10.50.21.30:9100",name="postgresql.service",state="active"} = 0 during incident
ALERTS{alertname="DbPostgresqlServiceInactiveWhileHostReachable",alertstate="firing"} = 1 during incident
```

## DB systemd metric evidence

The DB node's node_exporter systemd collector exposed PostgreSQL service state metrics.

Normal DB service state:

```text
postgresql_service_state active
```

PostgreSQL systemd metrics in the normal state:

```text
node_systemd_unit_state{name="postgresql.service",state="activating",type="oneshot"} 0
node_systemd_unit_state{name="postgresql.service",state="active",type="oneshot"} 1
node_systemd_unit_state{name="postgresql.service",state="deactivating",type="oneshot"} 0
node_systemd_unit_state{name="postgresql.service",state="failed",type="oneshot"} 0
node_systemd_unit_state{name="postgresql.service",state="inactive",type="oneshot"} 0
```

## Prometheus rule evidence

The alert rule name was:

```text
alert_name=DbPostgresqlServiceInactiveWhileHostReachable
```

The rule expression used node_exporter `up` plus the PostgreSQL systemd active-state metric:

```promql
up{job="node-exporter-operating-nodes",instance="10.50.21.30:9100"} == 1
and on(instance, job)
node_systemd_unit_state{job="node-exporter-operating-nodes",instance="10.50.21.30:9100",name="postgresql.service",state="active"} == 0
```

The `on(instance, job)` vector matching is intentional because `up` and `node_systemd_unit_state` have different label sets.

Prometheus loaded the rule successfully through `/api/v1/rules`:

```text
name=DbPostgresqlServiceInactiveWhileHostReachable
state=inactive
health=ok
```

Normal state alert query returned no firing alert:

```text
ALERTS{alertname="DbPostgresqlServiceInactiveWhileHostReachable"} -> result=[]
```

Normal state DB service metric through Prometheus:

```text
node_systemd_unit_state{instance="10.50.21.30:9100",name="postgresql.service",state="active"} = 1
```

## Alert incident validation

Scenario:

```text
PostgreSQL service was stopped on db-primary-01 while node_exporter and Prometheus remained active.
```

The incident evidence file identified the scenario and target:

```text
scenario=postgresql_service_stopped_alert_evaluation
db_target=db-primary-01
db_node_exporter_instance=10.50.21.30:9100
alert_name=DbPostgresqlServiceInactiveWhileHostReachable
```

During the incident, the alert fired:

```text
ALERTS{alertname="DbPostgresqlServiceInactiveWhileHostReachable",alertstate="firing"} = 1
```

The DB service active metric was zero:

```text
node_systemd_unit_state{instance="10.50.21.30:9100",name="postgresql.service",state="active"} = 0
```

The DB host remained reachable from Prometheus through node_exporter:

```text
up{job="node-exporter-operating-nodes",instance="10.50.21.30:9100"} = 1
```

This is the core diagnostic result: the rule fired for PostgreSQL service inactivity while the DB host remained reachable.

## Recovery evidence

After the alert incident, PostgreSQL was restarted and the service path recovered.

DB recovery check:

```text
postgresql service: active
port 5432 LISTEN on 0.0.0.0:5432 and [::]:5432
```

WEB/WAS recovery check:

```text
/readyz HTTP/1.1 200
/api/work-orders/summary HTTP/1.1 200
```

Application readiness response confirmed database connectivity:

```text
status=ready
check=database
db.ok=true
message=database connection ok
```

Application summary response recovered:

```text
status=ok
operation=work_orders.summary
data.total=5
```

## Evidence files preserved locally

Representative local evidence files:

```text
ansible-ping.txt
terraform-plan.txt
terraform-apply.txt
terraform-output.txt
terraform-output.json
terraform-state-list-after-apply.txt
lab-full-ops-postgresql-primary.txt
lab-full-ops-nfs-storage-baseline.txt
lab-full-ops-app-nfs-mount-baseline.txt
lab-full-ops-ops-sample-service.txt
lab-full-ops-nginx-reverse-proxy.txt
web-ready-summary-before-alert-validation.txt
observability-node-exporter-baseline.txt
observability-prometheus-scrape-baseline.txt
db-node-exporter-systemd-db-primary-01.txt
prometheus-db-service-alert-baseline-mon-01.txt
observability-prometheus-db-service-alert-baseline-no-incident.txt
observability-prometheus-db-service-alert-baseline-with-incident.txt
prometheus-db-service-alert-incident-mon-01.txt
post-alert-db-recovery-check.txt
post-alert-web-recovery-check.txt
observability-alert-validation-summary.md
evidence-file-list-final.txt
terraform-destroy.txt
terraform-state-list-after-destroy.txt
```

## Supported conclusion

The alert rule validation succeeded.

The evidence supports the following narrow claim:

```text
Prometheus rule evaluation detected PostgreSQL service inactivity while the DB host remained reachable.
```

## Boundaries

This validation does not prove:

```text
Alertmanager notification maturity
paging or on-call workflow
production monitoring maturity
Grafana dashboard readiness
PostgreSQL HA
automatic failover
SLO/SLA compliance
```

## Follow-up options

Reasonable next steps are documentation-oriented or one additional tightly scoped observability task:

```text
1. Update roadmap and current-state handoff after alert validation.
2. Add a minimal log-correlation evidence baseline for Nginx/app DB dependency incidents.
3. Stop Phase 4 expansion and prepare portfolio-facing summary material.
```

Avoid expanding into Grafana or Alertmanager unless those are separately scoped with narrow validation evidence.
