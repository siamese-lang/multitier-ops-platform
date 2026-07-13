# DB web-impact incident report

## Scenario

PostgreSQL service availability affects the enhanced work-order web workflow and DB-backed APIs. The validation goal is to show how a DB service incident appears from the WEB/WAS layer and how the operator separates process health from business readiness.

## User-visible symptom

Potential user-visible symptoms:

```text
- work-order list page cannot load DB-backed records
- DB-backed API returns 503
- readiness check fails
- process health endpoint can still respond
```

The key distinction is:

```text
/healthz can remain healthy while /readyz and DB-backed workflows fail.
```

## Impact scope

Impact scope:

```text
WEB tier: Nginx can still route requests to the WAS upstream.
WAS tier: Spring Boot process can remain alive.
DB tier: PostgreSQL dependency failure breaks readiness and DB-backed work-order operations.
Monitoring tier: host reachability and service dependency health must be interpreted separately.
```

## Initial hypotheses

Initial hypotheses:

```text
1. Nginx is down or cannot reach app-01.
2. App process is down.
3. App process is up, but DB dependency is unavailable.
4. DB host is reachable, but PostgreSQL service is inactive or port 5432 is unavailable.
5. The issue is limited to DB-backed paths, not static process health.
```

## Layer-by-layer checks

### 1. WEB / Nginx

Check:

```text
- Nginx service state
- access log status
- upstream_addr
- upstream status
- request ID for failed paths
```

Question:

```text
Is the WEB tier still routing requests to the WAS tier?
```

### 2. WAS / process health

Check:

```text
GET /healthz
application systemd status
application journal log
```

Question:

```text
Is the app process alive even while DB-backed behavior fails?
```

### 3. WAS / readiness and DB-backed behavior

Check:

```text
GET /readyz
GET /api/work-orders/summary
GET /work-orders
application exception or DB dependency log
```

Question:

```text
Does readiness fail because the DB dependency is unavailable?
```

### 4. DB / PostgreSQL service

Check:

```text
PostgreSQL systemd state
port 5432 listen state
PostgreSQL log
pg_hba.conf scope if the symptom is access-related
```

Question:

```text
Is PostgreSQL actually accepting app connections?
```

### 5. Monitoring / host vs service distinction

Check:

```text
node_exporter up for db-primary-01
PostgreSQL service state metric if available
application readiness result
DB-backed API status
```

Question:

```text
Is the DB host reachable while the PostgreSQL service dependency is unhealthy?
```

## Observed evidence

The enhanced runtime validation completed this scenario:

```text
S4 DB web-impact incident validation: completed
```

The broader project also validated this diagnostic distinction:

```text
Prometheus metrics distinguished DB host reachability from PostgreSQL service failure.
Prometheus rule evaluation detected PostgreSQL service inactivity while the DB host remained reachable.
```

## Root-cause judgment

The root-cause judgment pattern:

```text
If /healthz returns normally but /readyz and DB-backed endpoints fail, the app process is not the primary failed component.
If Nginx still reaches the app upstream, the WEB tier is not the primary failed component.
If DB service or port state is inactive/unavailable, classify the incident as DB dependency failure affecting WEB/WAS readiness and DB-backed workflow.
```

This is the main interview value of the scenario: health and readiness are not the same thing.

## Action taken

The validation flow checks the WEB/WAS/DB path before and during the DB impact condition:

```text
1. Confirm baseline web/API behavior.
2. Apply controlled DB service incident condition.
3. Compare /healthz, /readyz, and DB-backed API behavior.
4. Inspect Nginx and app logs by request path.
5. Verify DB service state.
6. Restore DB service.
7. Confirm readiness and DB-backed workflow recovery.
```

## Recovery validation

Recovery validation requires more than restarting PostgreSQL. The application path must also recover.

Recovery checks:

```text
- PostgreSQL service active
- port 5432 accepting connections
- /readyz returns success
- /api/work-orders/summary returns success
- /work-orders page returns DB-backed content or normal page behavior
- Nginx access log shows successful upstream response
- application request logs show successful DB-backed requests
```

## Remaining limits

This scenario does not prove:

```text
- PostgreSQL HA
- automatic failover
- replication recovery
- RPO/RTO guarantee
- production incident handling
- long-term DB performance tuning
```

## Interview explanation points

Use this scenario to explain:

```text
DB 장애가 발생했을 때 애플리케이션 프로세스가 살아 있다고 해서 서비스가 정상이라고 판단하지 않았습니다. /healthz는 프로세스 생존 확인이고, /readyz와 작업 요청 API는 DB 의존성을 확인합니다. Nginx가 app upstream에는 도달하지만 DB-backed API와 readiness가 실패하는 것을 보고 WEB/WAS 자체 장애가 아니라 DB dependency 장애로 원인을 좁혔습니다.
```

Short version:

```text
/healthz와 /readyz를 분리해 DB 장애가 WEB/WAS 업무 흐름에 미치는 영향을 확인했고, Nginx·app·DB 상태를 순서대로 보며 원인을 DB dependency로 좁혔습니다.
```
