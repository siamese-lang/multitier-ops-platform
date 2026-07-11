# OpenKoda Actuator, Health, and Metrics Inspection

## Purpose

This document records the initial inspection for existing OpenKoda support for actuator, health, and metrics endpoints.

The goal is to decide whether the project can rely on upstream OpenKoda behavior, add configuration only, or needs a local operational modification later.

This is a documentation-only result for Issue #10. It does not implement Java source changes, custom Docker image builds, Terraform, Ansible, Nginx, or Prometheus configuration.

## Context

Issue #4 defined the operability hardening scope. It intentionally separated liveness, readiness, metrics, request ID propagation, filesystem storage, logging baseline, and controlled fault-injection boundaries before moving into infrastructure automation.

The next question is whether OpenKoda already provides enough health and metrics support for this project.

## Inspection method

Initial repository code search was performed against upstream repository:

```text
openkoda/openkoda
```

Search terms:

```text
spring-boot-starter-actuator
actuator
micrometer
management.endpoints
```

## Findings

### 1. Actuator dependency

Initial code search did not find `spring-boot-starter-actuator`.

Interpretation:

- Existing actuator support was not confirmed in the initial inspection.
- A configuration-only actuator enablement path should not be assumed.
- If actuator endpoints are required later, the project will likely need either a custom OpenKoda build or a separate sidecar/proxy-level approach.

### 2. Prometheus metrics

Initial code search did not find `micrometer` or `management.endpoints`.

Interpretation:

- Existing Prometheus metrics exposure was not confirmed.
- `GET /actuator/prometheus` should not be treated as available until verified by runtime evidence.
- Prometheus integration should be deferred until the monitoring node exists and the app packaging strategy is clear.

### 3. Dedicated liveness/readiness endpoints

Initial inspection did not confirm dedicated OpenKoda liveness or readiness endpoints.

Observed runtime baseline from Issue #3:

- OpenKoda container started successfully.
- PostgreSQL container reached healthy state.
- Tomcat listened on HTTP `8080`.
- `curl http://127.0.0.1:8080` returned `HTTP/1.1 200` and OpenKoda HTML.
- SSH tunnel access to the web UI succeeded.

Interpretation:

- `/` can be used as a coarse process/web response check in the earliest lab stage.
- `/` is not a true readiness check because it does not intentionally distinguish database, filesystem, and optional integration status.
- Dedicated readiness should be implemented only after the application packaging strategy is decided.

## Decision for lab-small

For the first Terraform `lab-small` phase, do not modify OpenKoda source code yet.

Use infrastructure-level checks:

```text
liveness approximation: HTTP GET / on app node port 8080
container/process check: docker compose ps or systemd status, depending on deployment mode
readiness approximation: HTTP GET / plus PostgreSQL connectivity check from the app host
metrics: not implemented yet
```

This keeps the next infrastructure step small and verifiable while avoiding premature custom application builds.

## Recommended follow-up issues

### Follow-up 1: lab-small infrastructure baseline

Create Terraform `lab-small` with minimal EC2-based layout:

```text
bastion-01
app-01
```

Optional early addition:

```text
db-01
```

The first objective is to prove SSH, security groups, instance provisioning, and repeatable teardown through Terraform.

### Follow-up 2: Ansible single-node OpenKoda deployment

Use Ansible to install Docker and run OpenKoda Docker Compose on `app-01`.

Evidence:

```bash
docker compose ps
curl -i http://127.0.0.1:8080
```

### Follow-up 3: health check design after deployment mode is stable

After lab-small deployment is reproducible, decide whether to:

1. keep external checks only,
2. add a small operational wrapper endpoint outside OpenKoda,
3. or build a local OpenKoda image with actuator support.

## Boundaries

Do not do the following in Issue #10:

- modify OpenKoda source code,
- add actuator dependencies,
- create a custom Docker image,
- expose metrics publicly,
- implement Prometheus,
- implement Nginx,
- implement Terraform.

## Conclusion

Initial source inspection did not confirm existing actuator, Micrometer, Prometheus, or management endpoint support.

The safest next step is to move to Terraform `lab-small` and Ansible deployment using coarse runtime checks first, while leaving application-level health and metrics implementation for a later, evidence-driven issue.
