# lab-full-ops connection pressure validation

## Purpose

Validate a bounded WEB/WAS/DB operating scenario through the real service path:

```text
operator -> nginx-01:443 -> Spring Boot embedded Tomcat on app nodes -> HikariCP -> PostgreSQL
```

This runbook is not a production load test, capacity sizing exercise, or external Tomcat operation claim. Its purpose is to collect evidence that distinguishes:

```text
1. WAS request-thread pressure
2. HikariCP connection-pool pressure
3. PostgreSQL activity during DB-backed pressure
4. User-facing impact on normal work-order APIs
```

## Guardrails

Do not use this scenario to expand the project into JMeter/k6 load testing, SLO/SLA work, autoscaling, Kubernetes, external Tomcat migration, or production tuning.

Use it only during a planned `lab-full-ops` runtime validation window. Do not run Terraform apply/destroy only for this playbook.

## Precondition

`ops-sample-service` must already include these endpoints:

```text
GET /api/failure-lab/was-runtime
GET /api/failure-lab/sleep?millis=10000
GET /api/failure-lab/db-pool
GET /api/failure-lab/db-hold?seconds=10
GET /api/work-orders/summary
```

The app deployment must pass:

```text
GET /healthz
GET /readyz
GET /version
GET /api/work-orders/summary
```

## Suggested pressure deployment values

For a bounded validation, deploy the app with intentionally small Tomcat and HikariCP settings.

Example:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible

ansible-playbook -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-min-ops-sample-service.yml \
  -e 'ops_db_password=<supply-from-ignored-source>' \
  -e 'ops_tomcat_threads_max=4' \
  -e 'ops_tomcat_threads_min_spare=2' \
  -e 'ops_tomcat_accept_count=8' \
  -e 'ops_tomcat_connection_timeout=20s' \
  -e 'ops_hikari_max_pool_size=2' \
  -e 'ops_hikari_min_idle=1' \
  -e 'ops_hikari_connection_timeout_ms=3000'
```

These values are intentionally small. They are not recommended production settings.

## Validation command

Run the connection pressure playbook from the same Ansible environment:

```bash
ansible-playbook -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-connection-pressure-validation.yml
```

Optional overrides:

```bash
ansible-playbook -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-connection-pressure-validation.yml \
  -e 'connection_pressure_was_concurrency=4' \
  -e 'connection_pressure_db_concurrency=4' \
  -e 'connection_pressure_was_sleep_millis=10000' \
  -e 'connection_pressure_db_hold_seconds=10'
```

## Evidence collected

The playbook writes files under:

```text
/tmp/multitier-ops-platform/lab-full-ops-connection-pressure/
```

Main report:

```text
/tmp/multitier-ops-platform/lab-full-ops-connection-pressure-nginx-01.txt
```

Evidence categories:

```text
baseline:
- /ops/failure-lab
- /api/failure-lab/was-runtime
- /api/failure-lab/db-pool
- /api/work-orders/summary

WAS thread pressure:
- concurrent /api/failure-lab/sleep requests
- /api/work-orders/summary during pressure
- /api/failure-lab/was-runtime during pressure

HikariCP pool pressure:
- concurrent /api/failure-lab/db-hold requests
- /api/work-orders/summary during pressure
- /api/failure-lab/db-pool during pressure

Cross-tier evidence:
- Nginx access log lines with request IDs
- app journald lines with request IDs
- PostgreSQL pg_stat_activity sample
```

## Supported claim

If validation succeeds and evidence is archived, the project may claim:

```text
A bounded lab scenario showed how WEB/WAS request-thread pressure and HikariCP connection-pool pressure affect DB-backed work-order requests through Nginx, with evidence from HTTP responses, embedded Tomcat runtime settings, HikariCP pool state, Nginx logs, app journald, and PostgreSQL pg_stat_activity.
```

## Not supported

Do not claim:

```text
- production load testing
- production capacity sizing
- production Tomcat administration
- external Tomcat/WAR operation
- SLO/SLA validation
- autoscaling behavior
- PostgreSQL HA or failover
```

## Exit criteria

This scenario is complete when the report contains:

```text
1. baseline Tomcat and HikariCP settings
2. baseline work-order summary response
3. WAS sleep concurrency results
4. normal work-order API behavior during WAS pressure
5. DB hold concurrency results
6. normal work-order API behavior during DB pool pressure
7. HikariCP active/idle/awaiting state
8. PostgreSQL pg_stat_activity sample
9. Nginx request-id log sample
10. app journald request-id log sample
```

After these are collected, do not keep increasing concurrency or adding load-test tooling for v1.0.
