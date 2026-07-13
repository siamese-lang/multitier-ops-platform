# Connection pressure incident report

## Incident type

```text
Type: bounded lab incident / operating scenario validation
Environment: lab-full-ops
Date: 2026-07-13
Primary path: nginx-01 -> app-01 embedded Tomcat -> HikariCP -> PostgreSQL
Evidence summary: docs/04-evidence/connection-pressure-validation-2026-07-13.md
Raw evidence archive: local only, not committed
```

This document describes a controlled lab incident. It must not be represented as production operations experience, production load testing, capacity sizing, or external Tomcat administration.

## Executive summary

A bounded `lab-full-ops` runtime validation reproduced two different WEB/WAS/DB pressure symptoms through the same Nginx entry point.

```text
1. WAS request-thread pressure caused delayed but successful DB-backed API behavior.
2. HikariCP DB connection-pool pressure caused DB-backed API failure while PostgreSQL remained active.
```

The incident was useful because the symptoms were similar from the user-facing path, but the evidence pointed to different operating causes:

```text
WAS thread pressure:
- four long-running WAS sleep requests occupied the small embedded Tomcat worker pool
- normal work-order summary still returned 200
- the normal API response was delayed to about 9 seconds

HikariCP pool pressure:
- DB hold requests occupied the small HikariCP pool
- only two hold requests succeeded because maximumPoolSize was 2
- two hold requests returned 503 after connection timeout
- normal DB-backed summary also returned 503 during pool exhaustion
- PostgreSQL stayed reachable and showed active pg_sleep sessions
```

## Scope and guardrails

### In scope

```text
- EC2-based lab-full-ops runtime
- Nginx reverse proxy request path
- Spring Boot embedded Tomcat WAS process
- HikariCP connection pool behavior
- PostgreSQL activity visibility through pg_stat_activity
- HTTP status and timing evidence
- Nginx access log and app journald request-id correlation
```

### Out of scope

```text
- production incident response
- production load testing
- capacity sizing
- SLO/SLA validation
- autoscaling behavior
- external Tomcat/WAR operation
- PostgreSQL HA or failover
- Alertmanager or paging workflow
```

## Precondition

The `lab-full-ops` foundation had already been prepared:

```text
PostgreSQL primary: active
NFS storage baseline: active
app NFS mount: active
ops-sample-service: deployed
Nginx reverse proxy: active
```

The validation used a deliberately small bounded profile:

```text
baseline_tomcat_max_threads=4
baseline_tomcat_min_spare_threads=2
baseline_tomcat_accept_count=8
baseline_tomcat_connection_timeout=20s
baseline_hikari_max_pool_size=2
baseline_hikari_min_idle=1
baseline_hikari_connection_timeout_ms=3000
```

These values are not recommended production settings. They were selected only to make pressure behavior observable in a small lab.

## Detection and evidence sources

The evidence was collected through:

```text
- HTTP status and timing metrics from curl
- Nginx access logs with request IDs
- app journald logs with propagated request IDs
- HikariCP pool state endpoint
- PostgreSQL pg_stat_activity sampled during DB pressure
```

Primary evidence files included:

```text
lab-full-ops-connection-pressure-nginx-01.txt
baseline-was-runtime.json
baseline-db-pool.json
baseline-work-orders-summary.json
was-thread-pressure/*.json
was-thread-pressure/*.metrics
hikari-pool-pressure/*.json
hikari-pool-pressure/*.metrics
archive-manifest.txt
archive-sha256sums.txt
```

## Timeline

### T0. Baseline

Baseline checks were healthy.

```text
baseline_was_runtime_http_code=200
baseline_db_pool_http_code=200
baseline_summary_http_code=200
baseline_summary_time_total=0.026994
```

Interpretation:

```text
The normal Nginx -> app -> DB-backed summary path was healthy before pressure was introduced.
```

### T1. WAS request-thread pressure

The scenario started four concurrent long-running WAS requests:

```text
GET /api/failure-lab/sleep?millis=10000
was_concurrency=4
was_pressure_completed=4
was_pressure_curl_success=4
was_pressure_http_2xx=4
was_pressure_http_non_2xx=0
was_pressure_max_time_total=10.050939
was_pressure_avg_time_total=10.049166
```

The request threads used by the sleep requests were:

```text
http-nio-8080-exec-1
http-nio-8080-exec-2
http-nio-8080-exec-3
http-nio-8080-exec-4
```

The normal DB-backed summary request during WAS pressure showed:

```text
was_summary_during_http_code=200
was_summary_during_time_total=9.071659
```

Interpretation:

```text
The normal API still succeeded, but response time increased sharply because the small embedded Tomcat worker pool was occupied by long-running requests.
```

### T2. HikariCP connection-pool pressure

The scenario started four concurrent DB hold requests:

```text
GET /api/failure-lab/db-hold?seconds=10
db_concurrency=4
db_pressure_completed=4
db_pressure_curl_success=4
db_pressure_http_2xx=2
db_pressure_http_503=2
db_pressure_http_other=0
```

The HikariCP pool state during pressure was:

```text
db_pool_maximum_pool_size=2
db_pool_active_connections=2
db_pool_idle_connections=0
db_pool_total_connections=2
db_pool_threads_awaiting_connection=0
```

The normal DB-backed summary request during DB pool pressure showed:

```text
db_summary_during_http_code=503
db_summary_during_time_total=3.011622
```

The failure messages showed connection pool exhaustion:

```text
Connection is not available, request timed out after 3001ms (total=2, active=2, idle=0, waiting=1)
Connection is not available, request timed out after 3000ms (total=2, active=2, idle=0, waiting=0)
```

Interpretation:

```text
The application process was reachable, and PostgreSQL was not down. The DB-backed API failed because the WAS process could not obtain a DB connection from the bounded HikariCP pool before the connection timeout.
```

### T3. PostgreSQL activity during DB pressure

PostgreSQL was sampled while DB pressure was active:

```text
db_host=db-primary-01
db_name=opsdb
state=active
wait_event_type=Timeout
wait_event=PgSleep
query=select pg_sleep($1)
```

Interpretation:

```text
PostgreSQL was active and serving the two successful DB hold sessions. The 503 responses were therefore not explained by DB host loss or PostgreSQL process shutdown.
```

## Diagnosis

### Symptom 1: delayed but successful response

```text
Observed symptom:
- /api/work-orders/summary returned 200
- response time increased to about 9 seconds

Relevant evidence:
- Tomcat maxThreads was 4
- four concurrent sleep requests used http-nio-8080-exec-1..4
- Nginx and app logs showed long-running sleep requests

Diagnosis:
- embedded Tomcat request-thread pressure
```

### Symptom 2: failed DB-backed response

```text
Observed symptom:
- /api/work-orders/summary returned 503 during DB hold pressure
- HikariCP active connections reached 2/2
- two DB hold requests succeeded and two returned 503
- PostgreSQL still showed active pg_sleep sessions

Diagnosis:
- HikariCP connection-pool exhaustion affecting DB-backed application requests
```

## Resolution and cleanup

The bounded profile was intended only for the validation window. After collecting evidence, the app runtime profile was restored to the normal lab defaults:

```text
ops_tomcat_threads_max=200
ops_tomcat_threads_min_spare=10
ops_tomcat_accept_count=100
ops_hikari_max_pool_size=10
ops_hikari_connection_timeout_ms=30000
```

NAT Gateway was disabled after package installation and validation to reduce ongoing cost while retaining the EC2 lab for follow-up validation work.

## Supported claims

This incident report supports the following portfolio claims:

```text
- I reproduced bounded WEB/WAS/DB pressure through an EC2-based Nginx -> Spring Boot -> PostgreSQL path.
- I distinguished WAS request-thread pressure from DB connection-pool pressure using HTTP status/timing, Nginx logs, app logs, HikariCP state, and PostgreSQL activity.
- I confirmed that DB-backed API failure can occur even when PostgreSQL itself remains active, if the WAS-side DB connection pool is exhausted.
- I treated the result as lab evidence and documented the boundary instead of overstating it as production operations experience.
```

## Unsupported claims

Do not claim:

```text
- production incident handling experience
- production load testing
- production capacity sizing
- SLO/SLA validation
- external Tomcat administration
- commercial APM operation
- PostgreSQL HA or failover validation
- autoscaling or Kubernetes operation
```

## Interview explanation

A concise interview-safe explanation:

```text
In the lab-full-ops EC2 environment, I reproduced two similar-looking WEB/WAS/DB symptoms through the same Nginx entry point. When I occupied the embedded Tomcat worker threads with long-running requests, the normal DB-backed summary API still returned 200 but was delayed by about 9 seconds. When I occupied the HikariCP pool with DB hold requests, the same DB-backed API returned 503 because the pool had only two active connections and no idle connections. PostgreSQL was still active and pg_stat_activity showed pg_sleep sessions, so I could separate DB server failure from WAS-side connection-pool exhaustion. I documented this as bounded lab evidence, not production capacity testing.
```

A shorter resume or portfolio bullet:

```text
Validated bounded WEB/WAS/DB connection-pressure scenarios on EC2, distinguishing embedded Tomcat thread pressure from HikariCP pool exhaustion using HTTP timing/status, Nginx logs, app journald, HikariCP state, and PostgreSQL pg_stat_activity.
```
