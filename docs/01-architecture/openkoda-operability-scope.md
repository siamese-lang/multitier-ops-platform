# OpenKoda Operability Hardening Scope

## Purpose

This document defines the application-level operability improvements that may be added around upstream OpenKoda for this portfolio project.

The goal is not to change OpenKoda business behavior. The goal is to make the application easier to operate in an AWS EC2-based multi-tier environment by defining health checks, metrics, request tracing, storage boundaries, logging, and controlled incident-drill hooks.

## Current baseline

Issue #3 verified that upstream OpenKoda can run on a temporary EC2 instance using the upstream Docker Compose path.

Observed baseline:

- `openkoda/openkoda:latest` container started successfully.
- `postgres:14.2` container reached healthy state.
- OpenKoda started with Java 17 and Spring Boot.
- Tomcat listened on HTTP port `8080`.
- `curl http://127.0.0.1:8080` returned `HTTP/1.1 200` and OpenKoda home page HTML.
- SSH tunnel access from a local browser to `http://localhost:8080` succeeded.
- Direct Public IP access to `13.125.14.138:8080` failed and is treated as an AWS external network path issue, not an application runtime issue.

## Design principles

1. Keep upstream OpenKoda attribution clear.
2. Avoid business feature changes.
3. Prefer configuration-based changes before source-code changes.
4. Make each change observable and testable through commands, logs, or HTTP responses.
5. Keep operational endpoints private to the internal VPC or monitoring node.
6. Tie every implementation change to a GitHub issue and evidence directory.

## Candidate operability improvements

### 1. Liveness check

Purpose:

- Confirm that the application process is alive and the embedded web server can respond.
- Allow Nginx, systemd, or monitoring checks to distinguish a dead process from a running process.

Preferred target:

```text
GET /ops/health/live
```

Expected behavior:

- Returns `200` when the JVM process and web server are responsive.
- Does not check PostgreSQL, NFS, or optional integrations.
- Must be cheap and safe to call frequently.

Alternative:

- Use Spring Boot Actuator liveness endpoint if already available and suitable after source inspection.

Evidence to collect when implemented:

```bash
curl -i http://127.0.0.1:8080/ops/health/live
```

### 2. Readiness check

Purpose:

- Confirm that OpenKoda is ready to serve real requests.
- Detect database or required storage dependency failure before routing traffic to an app node.

Preferred target:

```text
GET /ops/health/ready
```

Expected behavior:

- Returns `200` only when required dependencies are usable.
- Checks at minimum:
  - PostgreSQL connectivity
  - required filesystem storage path availability when filesystem storage is enabled
- Returns non-2xx when the app should not receive traffic.

Readiness should not depend on optional external integrations such as Slack, GitHub, Jira, OpenAI, or SMTP unless those are explicitly required for the chosen scenario.

Evidence to collect when implemented:

```bash
curl -i http://127.0.0.1:8080/ops/health/ready
```

### 3. Metrics exposure

Purpose:

- Provide Prometheus-compatible metrics for `mon-01`.
- Support dashboards and alerting without scraping arbitrary logs.

Preferred target:

```text
GET /actuator/prometheus
```

Expected behavior:

- Exposed only on private network paths.
- Scraped by Prometheus from `mon-01`.
- Not exposed directly to the public internet.

Minimum metrics to verify:

- JVM memory and GC metrics
- HTTP request count and latency metrics
- process uptime
- application health-related metrics if available

Evidence to collect when implemented:

```bash
curl -i http://127.0.0.1:8080/actuator/prometheus | head
```

### 4. Request ID propagation

Purpose:

- Make it possible to trace one request across Nginx access logs, app logs, and incident evidence.

Header:

```text
X-Request-ID
```

Expected behavior:

- Nginx should forward `X-Request-ID` to app nodes.
- If a request does not include an ID, the edge layer or app layer should generate one.
- Application logs should include the request ID for request-scoped log lines.
- The response should include the same request ID when feasible.

Evidence to collect when implemented:

```bash
curl -i -H 'X-Request-ID: evidence-001' http://127.0.0.1:8080/
```

Then verify that `evidence-001` appears in the relevant log path.

### 5. Filesystem storage boundary

Purpose:

- Make uploaded/generated files operationally visible and recoverable.
- Separate mutable application data from application binaries and container images.

OpenKoda Docker Compose already exposes a storage-related environment surface through `FILE_STORAGE_FILESYSTEM_PATH` and `STORAGE_TYPE`.

Target operating model:

```text
FILE_STORAGE_FILESYSTEM_PATH=/srv/openkoda/files
STORAGE_TYPE=filesystem
```

In `lab-small` this may be a local directory on the app node.

In `lab-full` this should be backed by NFS from `nfs-01` and included in backup/restore verification.

Evidence to collect when implemented:

```bash
mount | grep openkoda || true
ls -ld /srv/openkoda/files
sudo -u <service-user> test -r /srv/openkoda/files
sudo -u <service-user> test -w /srv/openkoda/files
```

### 6. Logging baseline

Purpose:

- Ensure incident analysis does not rely only on screenshots or terminal memory.
- Preserve enough context to reconstruct request flow and failure timing.

Minimum log fields:

- timestamp
- host or container name
- service name
- log level
- request ID when request-scoped
- HTTP method/path/status/latency where available
- exception class and message for errors

Rules:

- Do not log secrets, session tokens, database passwords, AWS keys, or personal information.
- Prefer standard output for container-based execution.
- In VM-based execution, define a predictable log path and log rotation policy.

### 7. Controlled fault-injection hooks

Purpose:

- Support repeatable incident drills such as app failure, dependency failure, or latency simulation.
- Avoid pretending that uncontrolled outages are test design.

Rules:

- Disabled by default.
- Enabled only in lab environments through explicit configuration.
- Not exposed publicly.
- Must be documented in incident runbooks before use.

Candidate scenarios:

- app process stop/restart through systemd or Docker
- DB connectivity interruption through security group or local firewall rule
- NFS unmount or path permission failure
- synthetic app endpoint delay if implemented later

Application source changes for fault injection should be a last resort. Prefer infrastructure-level controlled failure first.

## Out of scope

The following are not part of Issue #4:

- implementing Java code changes
- changing OpenKoda business features
- changing authentication or authorization behavior
- adding public debug endpoints
- exposing metrics or health endpoints to the internet
- Terraform `lab-small` implementation
- Ansible role implementation
- production-grade TLS and DNS setup

## Decision for next implementation issues

Issue #4 should close with design scope only.

Implementation should be split into later issues:

1. Application source inspection for existing actuator and health support.
2. Minimal local app configuration or code changes for liveness/readiness if needed.
3. Nginx reverse proxy and request ID forwarding in `lab-small`.
4. Prometheus scrape configuration after monitoring node exists.
5. Filesystem storage verification after app node storage layout is defined.
6. Incident drill scripts after baseline service management is stable.

## Acceptance criteria for Issue #4

- Operability targets are defined without claiming they have been implemented.
- Liveness and readiness responsibilities are clearly separated.
- Metrics exposure rules are private-network only.
- Request ID propagation is defined as a cross-layer evidence requirement.
- Filesystem storage is treated as operational data, not application code.
- Fault injection is constrained to lab-only controlled scenarios.
- Follow-up implementation issues can be created from this scope.
