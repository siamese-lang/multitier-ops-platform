# ops-sample-service

`ops-sample-service` is a small Spring Boot workload for the `lab-full-min` WEB/WAS/DB operations lab.

It is not the subject of the portfolio. The service exists only to create deterministic operating evidence for:

- Nginx upstream routing
- WAS process health
- DB-dependent readiness
- app node identity
- later `app-01` failure and `app-02` continuation drills

## Runtime role

In the target architecture, this service runs on:

```text
app-01
app-02
```

Nginx routes traffic to both app nodes on port `8080`. PostgreSQL runs on `db-primary-01`.

## Build

```bash
cd apps/ops-sample-service
mvn clean package
```

The jar is created under:

```text
target/ops-sample-service-0.1.0.jar
```

## Run without DB

The service can start without DB environment variables. This is intentional because DB unavailability should be visible through readiness checks, not as an app startup failure.

```bash
java -jar target/ops-sample-service-0.1.0.jar
```

Expected behavior:

```text
GET /healthz  -> 200
GET /node     -> 200
GET /readyz   -> 503 when DB env is missing
GET /db/time  -> 503 when DB env is missing
```

## Run with DB

```bash
export OPS_DB_URL='jdbc:postgresql://<db-primary-private-ip>:5432/opsdb'
export OPS_DB_USERNAME='ops_user'
export OPS_DB_PASSWORD='<password>'
export OPS_NODE_ROLE='app'
export OPS_NODE_TIER='private-was'
export OPS_ENVIRONMENT='lab-full-min'
export APP_VERSION='0.1.0'

java -jar target/ops-sample-service-0.1.0.jar
```

## Endpoints

### `GET /healthz`

Process-level health check. It does not require DB connectivity.

Typical evidence use:

```bash
curl -s http://localhost:8080/healthz
```

### `GET /readyz`

Readiness check. It checks PostgreSQL connectivity with a simple query.

Expected behavior:

- returns `200` when the DB connection works
- returns `503` when DB configuration is missing or DB is unavailable

Typical evidence use:

```bash
curl -i http://localhost:8080/readyz
```

### `GET /node`

Node identity endpoint. It returns hostname, local address, role, tier, environment, and app version.

Typical evidence use:

```bash
curl -s http://localhost:8080/node
```

This endpoint is used to prove whether Nginx routed a request to `app-01` or `app-02`.

### `GET /db/time`

Runs `select now()` against PostgreSQL and returns the DB time.

Typical evidence use:

```bash
curl -i http://localhost:8080/db/time
```

## Environment variables

| Variable | Required | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | No | HTTP port. Defaults to `8080`. |
| `OPS_DB_URL` | For DB checks | PostgreSQL JDBC URL. |
| `OPS_DB_USERNAME` | For DB checks | PostgreSQL username. |
| `OPS_DB_PASSWORD` | For DB checks | PostgreSQL password. |
| `OPS_NODE_ROLE` | No | Logical role. Defaults to `app`. |
| `OPS_NODE_TIER` | No | Logical tier. Defaults to `private-was`. |
| `OPS_ENVIRONMENT` | No | Environment name. Defaults to `lab-full-min`. |
| `APP_VERSION` | No | Version string returned by responses. |

## Out of scope

This app does not include:

- business features
- authentication
- OpenKoda customization
- schema migration
- DB initialization
- deployment automation
- Nginx configuration
- incident drill automation

Those belong to later infrastructure and operations issues.
