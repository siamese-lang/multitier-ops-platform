# lab-full-ops enhanced DB web-impact incident validation

This runbook prepares scenario S4 from the enhanced service operations matrix.

```text
Scenario S4. DB service incident with web impact
```

The goal is not to prove PostgreSQL HA or automatic failover. The goal is to show that when PostgreSQL is stopped, the enhanced `ops-sample-service` fails in a way that can be separated from a WAS process outage.

## Validation playbook

```text
infra/ansible/playbooks/lab-full-ops-enhanced-db-web-impact-incident.yml
```

Run from WSL only, after the `lab-full-ops` runtime has already been created and configured.

```bash
ansible-playbook \
  -i infra/ansible/inventories/lab-full-ops/hosts.yml \
  infra/ansible/playbooks/lab-full-ops-enhanced-db-web-impact-incident.yml
```

## Runtime boundary

This playbook intentionally stops PostgreSQL on `db-primary-01`.

Run it only inside one planned validation window:

```text
apply once -> configure -> validate -> collect evidence -> destroy once
```

Do not run this playbook against a shared, persistent, or production database.

## Scenario intent

Business situation:

```text
The work-order list page or API fails even though the application process is still running.
```

Operating question:

```text
Is this an app process outage, a DB host outage, or PostgreSQL service dependency failure?
```

Expected distinction:

```text
/healthz remains 200
/readyz becomes 503
/api/work-orders/summary becomes 503
/work-orders returns the page shell with a DB-unavailable error marker
PostgreSQL service becomes inactive and port 5432 stops listening
PostgreSQL restart restores readiness and work-order visibility
```

## What the playbook does

### 1. Baseline checks before incident

Through `nginx-01`:

```text
GET /healthz
GET /readyz
GET /work-orders
GET /api/work-orders/summary
```

Expected baseline pattern:

```text
baseline_health_code=200
baseline_ready_code=200
baseline_work_orders_code=200
baseline_summary_code=200
baseline_summary_status=ok
```

Directly on `db-primary-01`:

```text
systemctl is-active postgresql
ss -ltnp | grep :5432
optional node_exporter postgresql.service systemd metrics
```

### 2. DB incident

The playbook stops PostgreSQL:

```text
systemctl stop postgresql
```

It then confirms:

```text
postgresql_service_state != active
postgresql_port_listening=false
app service remains active on app nodes
```

### 3. Web/API impact checks during incident

Through `nginx-01`:

```text
GET /healthz
GET /readyz
GET /work-orders
GET /api/work-orders/summary
```

Expected incident pattern:

```text
incident_health_code=200
incident_ready_code=503
incident_summary_code=503
incident_work_orders_error_marker=true
```

Important nuance:

```text
/work-orders may return HTTP 200 because the server-rendered page catches the DB exception and displays an error banner.
Therefore the playbook checks for the page-level marker:
DB-backed work-order view is unavailable
```

This is intentional and closer to a real web operations situation. A page may render, but the business function is degraded.

### 4. Evidence collection during incident

The playbook collects:

```text
Nginx access log request-id samples
Nginx error log request-id samples
app journald request-id samples
PostgreSQL service state
PostgreSQL port state
node_exporter PostgreSQL systemd metrics when available
```

### 5. Recovery

The `always` block restarts PostgreSQL even if the incident checks fail:

```text
systemctl start postgresql
wait for port 5432
```

After recovery, the playbook checks:

```text
GET /readyz
GET /work-orders
GET /api/work-orders/summary
```

Expected recovery pattern:

```text
recovery_ready_code=200
recovery_work_orders_code=200
recovery_summary_code=200
recovery_summary_status=ok
```

## Evidence files on nginx-01

The playbook writes files under:

```text
/tmp/multitier-ops-platform/lab-full-ops-db-web-impact/
```

Important files:

```text
baseline-probes.tsv
baseline-work-orders.html
baseline-summary.json
incident-probes.tsv
incident-work-orders.html
incident-readyz.json
incident-summary.json
recovery-probes.tsv
recovery-work-orders.html
recovery-summary.json
```

Report file:

```text
/tmp/multitier-ops-platform/lab-full-ops-db-web-impact-nginx-01.txt
```

## Success criteria

The validation succeeds when all of the following are true:

```text
baseline /healthz = 200
baseline /readyz = 200
baseline /work-orders = 200
baseline /api/work-orders/summary = 200
incident /healthz = 200
incident /readyz = 503
incident /api/work-orders/summary = 503
incident /work-orders contains DB-backed work-order view is unavailable
recovery /readyz = 200
recovery /work-orders = 200
recovery /api/work-orders/summary = 200
```

## Safe claim after runtime validation

After successful runtime evidence collection, it is safe to say:

```text
DB 장애 시 작업 요청 화면과 API가 어떤 영향을 받는지 확인했고, /healthz와 /readyz, PostgreSQL systemd 상태, 포트, 로그를 함께 봐서 WAS 프로세스 장애가 아니라 DB service dependency failure로 분리했습니다.
```

If node_exporter systemd metrics are present, this can be extended to:

```text
DB host는 reachable 상태였지만 PostgreSQL service inactive 상태임을 metric으로도 구분했습니다.
```

## Claims to avoid

Do not claim:

```text
PostgreSQL HA validated
automatic failover validated
zero-downtime DB recovery validated
connection-pool tuning completed
production-grade incident response validated
```

This is a controlled incident validation for operational diagnosis, not HA or capacity validation.

## Relationship to older evidence

Older Phase 4 evidence already distinguished DB host reachability from PostgreSQL service inactivity. This enhanced scenario adds user-facing business impact:

```text
/work-orders page impact
/api/work-orders/summary impact
readyz vs healthz distinction
recovery of enhanced web workflow
```

## Recommended execution order in the runtime window

Run after the normal enhanced workflow validation and upload/latency prep validations:

```text
1. lab-full-ops-enhanced-service-workflow-validation.yml
2. lab-full-ops-enhanced-upload-limit-incident.yml
3. lab-full-ops-enhanced-latency-scenario.yml
4. lab-full-ops-enhanced-db-web-impact-incident.yml
```

Run enhanced restore-lab refresh after backup artifacts are collected from the enhanced service data.
