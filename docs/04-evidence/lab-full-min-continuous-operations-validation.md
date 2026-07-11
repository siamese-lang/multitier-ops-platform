# lab-full-min continuous operations validation

## 1. Purpose

This document records a continuous operations validation session for the `lab-full-min` WEB/WAS/DB environment.

The objective was to prove that the environment is not only deployable, but also diagnosable under common operating conditions:

```text
operator -> nginx-01:443 -> app-01/app-02:8080 -> db-primary-01:5432
```

The session reused one active AWS environment to avoid repeated Terraform create/destroy cycles. The final cleanup was performed after all validation scenarios were completed.

## 2. Environment under validation

The validated minimum operating topology was:

```text
Internet / operator
  |
  v
nginx-01
  - public WEB entrypoint
  - HTTPS reverse proxy
  - upstream routing to app-01/app-02
  |
  v
app-01 / app-02
  - private WAS nodes
  - ops-sample-service systemd service
  - DB-backed endpoints
  |
  v
db-primary-01
  - private PostgreSQL primary
  - opsdb database
```

The validation intentionally used VM-based tier separation rather than a managed-only architecture. The operating evidence was collected from Nginx logs, application journal logs, PostgreSQL service/activity checks, and Ansible-generated reports.

## 3. Execution sequence

The continuous session included the following validation sequence:

```text
1. Rebuild baseline WEB/WAS/DB stack
2. Run app-01 failure and Nginx upstream bypass drill
3. Run app-01/app-02 rolling restart drill
4. Run DB-backed concurrent request drill
5. Run PostgreSQL failure and recovery drill
6. Destroy Terraform resources
```

The first baseline had already been documented separately in:

```text
docs/04-evidence/lab-full-min-web-was-db-integrated-validation.md
```

This document focuses on the subsequent operations drills.

## 4. App failure and Nginx upstream bypass

### Scenario

```text
app-01 service stopped
-> app-02 remains active
-> Nginx continues serving traffic through app-02
-> app-01 restored before drill completion
```

### Evidence summary

The drill report recorded the following successful recovery path:

```text
target_stopped_state=inactive
survivor_during_failure_state=active
target_recovered_state=active
survivor_recovered_state=active
failure_window_requests_rc=0
post_recovery_summary_rc=0
```

Final app service checks showed:

```text
app-01 active
app-02 active
```

Nginx access logs showed the expected passive upstream bypass behavior. The first request attempted the stopped app node and then succeeded through the survivor node:

```text
request_id=lab-full-min-app01-failure-1-healthz
upstream_addr="10.40.11.203:8080, 10.40.11.40:8080"
upstream_status="502, 200"
status=200
```

Subsequent requests were served by the survivor:

```text
request_id=lab-full-min-app01-failure-2-summary
upstream_addr="10.40.11.40:8080"
upstream_status="200"
status=200
```

### Operational interpretation

This proves that the WEB tier can continue serving traffic when one WAS node is down. It also proves that the operator can correlate a client-facing 200 response with an upstream retry and a failed backend node by using Nginx access/error logs.

## 5. Rolling restart continuity

### Scenario

```text
stop app-01 -> verify traffic through app-02 -> restore app-01
stop app-02 -> verify traffic through app-01 -> restore app-02
```

### Evidence summary

The rolling restart report recorded:

```text
first_target=app-01
first_survivor=app-02
first_target_stopped_state=inactive
first_survivor_state=active
first_window_requests_rc=0
first_target_recovered_state=active

after_first_recovery_summary_rc=0

second_target=app-02
second_survivor=app-01
second_target_stopped_state=inactive
second_survivor_state=active
second_window_requests_rc=0
second_target_recovered_state=active

after_second_recovery_summary_rc=0
```

Final service state:

```text
app-01 active
app-02 active
```

Nginx logs showed the expected one-node-at-a-time maintenance behavior.

For the first window, app-01 was stopped and traffic was served through app-02:

```text
request_id=lab-full-min-rolling-restart-app01-1-summary
upstream_addr="10.40.11.203:8080, 10.40.11.40:8080"
upstream_status="502, 200"
status=200
```

For the second window, app-02 was stopped and traffic was served through app-01:

```text
request_id=lab-full-min-rolling-restart-app02-2-healthz
upstream_addr="10.40.11.40:8080, 10.40.11.203:8080"
upstream_status="502, 200"
status=200
```

### Operational interpretation

This proves that maintenance can be performed one app node at a time without full service interruption. It also provides a clear operator narrative: stop one WAS node, validate the survivor path through Nginx, restore the node, and repeat for the other node.

## 6. DB-backed concurrent request observation

### Scenario

Concurrent DB-backed requests were sent through Nginx:

```text
nginx-01 -> app-01/app-02 -> db-primary-01
```

The goal was not to produce a synthetic benchmark. The goal was to show cross-layer observability for DB-dependent traffic.

### Evidence summary

The drill report recorded:

```text
request_count=80
concurrency=16
success_count=80
failure_count=0
http_200=80
avg_time_sec=0.237403
min_time_sec=0.079599
max_time_sec=1.144421
post_load_summary_rc=0
```

PostgreSQL activity snapshots before and after the run recorded:

```text
opsdb_connections=1
active_opsdb_connections=1
max_conn_setting=100
```

Application journal logs showed request IDs and request durations from both app nodes.

Example from `app-01`:

```text
requestId=lab-full-min-db-bottleneck-3
path=/api/work-orders/summary
status=200
durationMs=40
node=ip-10-40-11-203
```

Example from `app-02`:

```text
requestId=lab-full-min-db-bottleneck-7
path=/api/work-orders/summary
status=200
durationMs=1036
node=ip-10-40-11-40
```

### Operational interpretation

This run demonstrated that 80 DB-backed requests at concurrency 16 were handled without failure. The important evidence is the cross-layer traceability:

```text
Nginx request_id
-> app journal requestId
-> DB-backed endpoint status/duration
-> PostgreSQL activity snapshot
```

The output also showed that some requests were noticeably slower than the average, which gives the operator a concrete reason to inspect app-side duration and Nginx upstream response timing together.

## 7. PostgreSQL failure and recovery isolation

### Scenario

```text
PostgreSQL stopped on db-primary-01
-> app process remains active
-> process-level health may remain healthy
-> DB-dependent endpoints fail or degrade
-> PostgreSQL restarted
-> DB-dependent endpoints recover
```

### Evidence summary

The drill report recorded:

```text
postgresql_pre_state=active
postgresql_stopped_state=inactive
postgresql_recovered_state=active

baseline_health_rc=0
baseline_ready_rc=0
baseline_summary_rc=0

failure_window_requests_rc=0
post_recovery_ready_rc=0
post_recovery_summary_rc=0
```

The failure window summary recorded:

```text
total_count=15
health_2xx=2
ready_2xx=0
ready_non_2xx=5
summary_2xx=0
summary_non_2xx=5
```

The failure result file showed the expected distinction between process-level and DB-dependent endpoints:

```text
lab-full-min-postgresql-failure-1-healthz   healthz 200
lab-full-min-postgresql-failure-1-readyz    readyz  503
lab-full-min-postgresql-failure-1-summary   summary 503
```

After recovery, direct checks through Nginx returned successful DB-backed responses:

```text
/readyz -> HTTP 200
/api/work-orders/summary -> HTTP 200
```

Application services and PostgreSQL were active after the drill:

```text
app-01 active
app-02 active
db-primary-01 postgresql active
```

### Operational interpretation

This is the clearest DB-layer failure isolation evidence in the session. It distinguishes three different states:

```text
app process state: active
process-level endpoint: can return 200
DB-dependent readiness/summary: fails while PostgreSQL is stopped
DB-dependent endpoints: recover after PostgreSQL restarts
```

This makes the failure mode diagnosable rather than ambiguous. An operator can explain why the app process was still running while user-facing DB-dependent functions failed.

## 8. Cleanup

After the final PostgreSQL recovery validation, the operator reported that Terraform resources were destroyed.

No `lab-full-min` resources are intentionally kept for this validation session.

The final `terraform state list` output was not pasted into the issue thread, so this document records cleanup as operator-reported completion rather than copied terminal evidence.

## 9. Portfolio value

This continuous validation session strengthens the project in four ways.

First, it shows a real multi-tier operating path rather than a single-container example.

Second, it proves failure handling at two different layers:

```text
WAS node failure
DB service failure
```

Third, it proves maintenance continuity through rolling restart.

Fourth, it demonstrates operational diagnosis using multiple evidence sources:

```text
Nginx access/error logs
application journal logs
PostgreSQL service/activity checks
Ansible reports
Terraform lifecycle control
```

The result is a portfolio narrative focused on operation, failure isolation, recovery, and evidence-based troubleshooting.