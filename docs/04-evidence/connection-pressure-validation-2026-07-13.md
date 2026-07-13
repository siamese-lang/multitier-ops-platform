# Connection pressure validation - 2026-07-13

## Purpose

Validate a service-linked WEB/WAS/DB operating scenario in the `lab-full-ops` EC2 environment.

The scenario intentionally used bounded runtime settings to distinguish two different symptoms:

```text
1. Embedded Tomcat WAS request-thread pressure
2. HikariCP DB connection-pool pressure
```

The purpose is not production load testing, capacity sizing, external Tomcat administration, or SLO/SLA validation.

## Environment

```text
project=multitier-ops-platform
environment=lab-full-ops
inventory_hostname=nginx-01
ansible_host=10.50.1.90
validation=connection-pressure-validation
request_prefix=lab-full-ops-connection-pressure
base_url=https://127.0.0.1
```

Runtime path:

```text
operator -> nginx-01:443 -> app-01 Spring Boot embedded Tomcat -> HikariCP -> PostgreSQL on db-primary-01
```

Validation was run after the lab-full-ops foundation had been prepared:

```text
PostgreSQL primary active
NFS storage baseline active
app NFS mount active
ops-sample-service deployed
Nginx reverse proxy active
```

## Raw evidence location

Raw runtime evidence was archived locally and is intentionally not committed to the repository.

Local archive directory used for this validation window:

```text
/mnt/c/Project/test/multitier-ops-platform-evidence/connection-pressure
```

Extracted evidence files included:

```text
lab-full-ops-connection-pressure-nginx-01.txt
lab-full-ops-connection-pressure/archive-manifest.txt
lab-full-ops-connection-pressure/archive-sha256sums.txt
lab-full-ops-connection-pressure/baseline-was-runtime.json
lab-full-ops-connection-pressure/baseline-db-pool.json
lab-full-ops-connection-pressure/baseline-work-orders-summary.json
lab-full-ops-connection-pressure/was-thread-pressure/*.json
lab-full-ops-connection-pressure/was-thread-pressure/*.metrics
lab-full-ops-connection-pressure/hikari-pool-pressure/*.json
lab-full-ops-connection-pressure/hikari-pool-pressure/*.metrics
```

## Playbooks used

```text
infra/ansible/playbooks/lab-full-ops-connection-pressure-bounded-app-deploy.yml
infra/ansible/playbooks/lab-full-ops-connection-pressure-validation.yml
infra/ansible/playbooks/lab-full-ops-connection-pressure-fetch-evidence.yml
infra/ansible/playbooks/lab-full-ops-connection-pressure-restore-app-defaults.yml
```

Runtime sequence:

```text
1. Deploy bounded pressure profile
2. Run connection pressure validation
3. Fetch and archive evidence
4. Restore normal app runtime profile
```

## Bounded runtime profile

The app was intentionally redeployed with small WAS and DB pool settings:

```text
baseline_tomcat_max_threads=4
baseline_tomcat_min_spare_threads=2
baseline_tomcat_accept_count=8
baseline_tomcat_connection_timeout=20s
baseline_hikari_max_pool_size=2
baseline_hikari_min_idle=1
baseline_hikari_connection_timeout_ms=3000
```

Baseline service checks succeeded:

```text
baseline_was_runtime_http_code=200
baseline_db_pool_http_code=200
baseline_summary_http_code=200
baseline_summary_time_total=0.026994
```

The validation preflight confirmed the bounded profile:

```text
enforce_bounded_settings=True
max_allowed_tomcat_threads=4
max_allowed_hikari_pool_size=2
```

## Scenario 1. WAS request-thread pressure

Input:

```text
GET /api/failure-lab/sleep?millis=10000
was_sleep_millis=10000
was_concurrency=4
```

Observed result:

```text
was_pressure_expected_concurrency=4
was_pressure_completed=4
was_pressure_curl_success=4
was_pressure_http_2xx=4
was_pressure_http_non_2xx=0
was_pressure_max_time_total=10.050939
was_pressure_avg_time_total=10.049166
was_pressure_request_threads=http-nio-8080-exec-1,http-nio-8080-exec-2,http-nio-8080-exec-3,http-nio-8080-exec-4
```

Normal DB-backed summary request during WAS pressure:

```text
was_summary_during_curl_rc=0
was_summary_during_http_code=200
was_summary_during_time_total=9.071659
```

Interpretation:

```text
WAS request threads were occupied by long-running requests.
The normal DB-backed summary endpoint eventually succeeded but was delayed to about 9 seconds.
This indicates request-thread pressure rather than a PostgreSQL outage.
```

Supporting log evidence:

```text
Nginx access log recorded four /api/failure-lab/sleep requests with about 10 second request_time.
Nginx access log recorded /api/work-orders/summary status=200 with about 9 second request_time during WAS pressure.
App journald recorded matching request IDs and about 10 second durations for the sleep requests.
```

## Scenario 2. HikariCP connection-pool pressure

Input:

```text
GET /api/failure-lab/db-hold?seconds=10
db_hold_seconds=10
db_concurrency=4
baseline_hikari_max_pool_size=2
```

Observed result:

```text
db_pressure_expected_concurrency=4
db_pressure_completed=4
db_pressure_curl_success=4
db_pressure_http_2xx=2
db_pressure_http_503=2
db_pressure_http_other=0
db_pressure_max_time_total=10.035459
db_pressure_avg_time_total=6.524307
```

Normal DB-backed summary request during DB pool pressure:

```text
db_summary_during_curl_rc=0
db_summary_during_http_code=503
db_summary_during_time_total=3.011622
```

HikariCP pool state during pressure:

```text
db_pool_maximum_pool_size=2
db_pool_active_connections=2
db_pool_idle_connections=0
db_pool_total_connections=2
db_pool_threads_awaiting_connection=0
```

Connection timeout messages were captured:

```text
Connection is not available, request timed out after 3001ms (total=2, active=2, idle=0, waiting=1)
Connection is not available, request timed out after 3000ms (total=2, active=2, idle=0, waiting=0)
```

Interpretation:

```text
The PostgreSQL service was not down.
Two requests held the available HikariCP connections, while the remaining DB-backed requests timed out waiting for a connection.
The normal summary endpoint returned 503 during pool exhaustion.
This distinguishes application-side DB connection-pool pressure from DB host or PostgreSQL service failure.
```

## PostgreSQL activity during pressure

`pg_stat_activity` was sampled while DB pressure was active:

```text
db_host=db-primary-01
db_name=opsdb
state=active
wait_event_type=Timeout
wait_event=PgSleep
query=select pg_sleep($1)
```

Two active PostgreSQL sessions were observed for the `ops-sample-service` application:

```text
ops-sample-service active Timeout PgSleep select pg_sleep($1)
ops-sample-service active Timeout PgSleep select pg_sleep($1)
```

Interpretation:

```text
PostgreSQL was reachable and executing the held queries.
The user-facing 503 responses were caused by HikariCP pool exhaustion, not by PostgreSQL process failure.
```

## Cross-tier correlation

The report included cross-tier request ID evidence from:

```text
Nginx access log
app journald request log
PostgreSQL pg_stat_activity
HikariCP pool state endpoint
HTTP response code and timing metrics
```

Examples of correlated request IDs:

```text
lab-full-ops-connection-pressure-was-sleep-1
lab-full-ops-connection-pressure-was-summary-during
lab-full-ops-connection-pressure-db-hold-1
lab-full-ops-connection-pressure-db-hold-3
lab-full-ops-connection-pressure-db-summary-during
lab-full-ops-connection-pressure-db-pool-during
```

## Supported claims

This evidence supports the following claims:

```text
A bounded EC2 lab scenario validated request-path behavior through Nginx, Spring Boot embedded Tomcat, HikariCP, and PostgreSQL.
WAS request-thread pressure caused delayed but successful DB-backed API response.
HikariCP connection-pool pressure caused DB-backed API failure while PostgreSQL remained active.
Nginx access logs, app journald logs, HikariCP pool state, PostgreSQL pg_stat_activity, and HTTP timing/status metrics were used together to distinguish failure modes.
```

## Unsupported claims

Do not claim:

```text
production load testing
production capacity sizing
production Tomcat administration
external Tomcat/WAR deployment operation
SLO/SLA validation
autoscaling behavior
PostgreSQL HA or failover
production incident response experience
```

## Cleanup and runtime status

After evidence collection:

```text
app runtime profile was restored to the default/stable profile
NAT Gateway was disabled after package installation and validation to reduce cost
lab-full-ops EC2 environment was intentionally kept for follow-up AWS validation work
terraform plan later reported no infrastructure drift
```

The environment was not destroyed because additional AWS runtime work is planned and repeatedly destroying/recreating the lab would cause unnecessary setup churn.

## Portfolio wording

Safe portfolio wording:

```text
Spring Boot embedded Tomcat 기반 WAS 프로세스와 HikariCP/PostgreSQL 연결 경로에서 bounded connection pressure를 재현하고, WEB/WAS/DB 계층의 응답 코드, 처리 시간, pool 상태, PostgreSQL 세션, Nginx/app 로그를 연결해 장애 유형을 구분했습니다.
```

Unsafe wording:

```text
대규모 부하 테스트를 수행했습니다.
상용 Tomcat 서버를 운영했습니다.
운영 환경의 SLO/SLA를 검증했습니다.
PostgreSQL HA/failover를 검증했습니다.
프로덕션 장애를 대응했습니다.
```
