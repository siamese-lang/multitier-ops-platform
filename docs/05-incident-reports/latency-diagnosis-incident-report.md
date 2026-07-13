# Latency diagnosis incident report

## Scenario

A user reports that the web service is slow. The validation goal is to distinguish between a WAS-side slow request and a DB-backed slow path.

This scenario uses the enhanced `ops-sample-service` failure-lab endpoints and runtime evidence from the first enhanced validation pass.

## User-visible symptom

The user-visible symptom is broad:

```text
The page or API responds slowly.
```

This symptom is intentionally ambiguous. In a real WEB/WAS operations context, slow response reports often do not immediately reveal whether the issue is in Nginx, WAS processing, DB dependency, network path, or storage.

## Impact scope

Potential impact scope:

```text
WEB tier: Nginx may show increased request_time or upstream_response_time.
WAS tier: application thread may spend time in controlled sleep or internal processing.
DB tier: DB-backed endpoint may spend time waiting on PostgreSQL query execution.
Monitoring tier: metrics/logs help separate host reachability from service dependency issues.
```

## Initial hypotheses

Initial hypotheses:

```text
1. Nginx is slow to reach upstream.
2. WAS process is reachable but the application handler is slow.
3. PostgreSQL query path is slow.
4. The DB host is reachable but DB service behavior affects application readiness or DB-backed APIs.
```

## Layer-by-layer checks

### 1. Client-side response timing

Check:

```text
- curl total time
- HTTP status
- response body operation/duration fields if present
```

Question:

```text
Is the request slow while still returning a successful response, or does it fail?
```

### 2. WEB / Nginx

Check:

```text
- Nginx access log status
- request_time
- upstream_response_time
- upstream_addr
- request ID
```

Question:

```text
Did Nginx connect to the upstream, and how long did the upstream take to respond?
```

### 3. WAS / application log

Check:

```text
- application request log
- request ID correlation
- endpoint path
- application durationMs
- node identity
```

Question:

```text
Was the delay inside the application handler, and which app node handled the request?
```

### 4. DB / PostgreSQL path

Check:

```text
- DB-backed endpoint behavior
- DB sleep endpoint behavior
- readiness result
- PostgreSQL service status if needed
```

Question:

```text
Is the delay caused by the DB-backed path rather than a generic WAS delay?
```

### 5. Monitoring / metrics

Check:

```text
- node_exporter up result
- DB host reachability
- PostgreSQL service state metrics when available
```

Question:

```text
Is the DB host reachable even when DB-backed application behavior is degraded?
```

## Observed evidence

The first enhanced validation pass completed the latency scenario:

```text
S3 latency scenario validation: completed
```

The intended distinction is:

```text
WAS sleep path: slow response caused by application-side controlled delay.
DB sleep path: slow response caused by DB-backed controlled delay.
```

The project also separately validated that Prometheus metrics can distinguish DB host reachability from PostgreSQL service failure in the DB incident scenario.

## Root-cause judgment

The root-cause judgment is made by comparing evidence from the request path:

```text
- If Nginx upstream_response_time rises and the application endpoint is a WAS sleep path, classify as WAS-side slow handler behavior.
- If the slow path is DB-backed and readiness/DB query behavior is affected, classify as DB dependency latency.
- If host metrics remain reachable while DB-backed paths fail, avoid misclassifying host availability as DB service health.
```

The key operating habit is to avoid saying “the server is slow” without separating request path layers.

## Action taken

The validation uses controlled latency endpoints and compares:

```text
- curl timing
- HTTP status
- Nginx upstream timing
- application request duration
- DB-backed behavior
- node identity and request ID correlation
```

## Recovery validation

For latency scenarios, recovery validation means returning to baseline response behavior after the controlled slow path or incident condition ends.

Minimum recovery checks:

```text
- /healthz returns normally
- /readyz returns normally when DB is available
- work-order list or summary returns normally
- Nginx access logs show expected status and upstream
- application logs show successful request completion
```

## Remaining limits

This scenario does not prove:

```text
- full JVM thread dump analysis
- HikariCP pool exhaustion diagnosis
- production APM-level tracing
- long-term performance baselining
- capacity test results
```

Those may be future hardening work only if they directly support the WEB/WAS operations story.

## Interview explanation points

Use this scenario to explain:

```text
느린 요청을 단순히 서버 성능 문제라고 보지 않고, Nginx의 upstream 응답 시간, 애플리케이션 request duration, DB-backed endpoint 동작을 비교했습니다. WAS 내부 sleep과 DB sleep을 분리해서 보면서 지연이 WAS 처리 구간에서 생기는지 DB 의존 경로에서 생기는지 구분했습니다.
```

Short version:

```text
느린 요청을 Nginx, WAS, DB 경로로 나누어 보고, upstream 시간과 app duration, DB-backed 응답을 비교해 병목 위치를 좁혔습니다.
```
