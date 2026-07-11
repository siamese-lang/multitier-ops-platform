# ADR-0002: OpenKoda Operability Hardening Scope

## Status

Accepted

## Context

The upstream OpenKoda application was verified on a temporary EC2 instance using Docker Compose. The baseline test proved that the application can start, connect to PostgreSQL, listen on HTTP `8080`, and serve the OpenKoda home page from inside the EC2 instance and through an SSH tunnel.

The next project step is to define what application-level operability improvements are appropriate before implementing Terraform `lab-small` or Ansible roles.

This project must avoid two mistakes:

1. Presenting OpenKoda business functionality as work authored by this repository owner.
2. Jumping directly into infrastructure automation before defining what the application needs for operation, monitoring, and incident evidence.

## Decision

This project will define a narrow set of operational hardening targets around OpenKoda:

1. liveness check
2. readiness check
3. private metrics endpoint for Prometheus
4. request ID propagation through Nginx and application logs
5. filesystem storage boundary for mutable application data
6. logging baseline for incident reconstruction
7. controlled lab-only fault-injection approach

Issue #4 will not implement those changes. It will define the scope, boundaries, expected behavior, evidence requirements, and follow-up implementation issues.

## Rationale

### Why define operability before Terraform

Terraform can create servers, networks, and security groups, but it cannot decide what service checks mean. Without clear application health and readiness criteria, later Nginx, Prometheus, Alertmanager, and incident drills would be arbitrary.

### Why separate liveness and readiness

A process can be alive while the service is not ready to handle real requests. For example, the JVM can respond while PostgreSQL or filesystem storage is unavailable. Separating liveness and readiness allows more precise troubleshooting evidence.

### Why metrics must be private

Metrics often expose internal service names, JVM details, endpoints, and operational state. They are useful for Prometheus but should not be exposed to the public internet.

### Why request ID matters

This project emphasizes operations evidence. A request ID makes it possible to connect Nginx access logs, application logs, and incident reports without relying on guesswork.

### Why filesystem storage is included

OpenKoda exposes storage-related configuration. In a multi-tier VM environment, mutable files must have an explicit location, ownership model, backup scope, and restore verification path.

### Why fault injection is constrained

Fault injection is useful for incident drills only when it is controlled, documented, disabled by default, and limited to lab environments. Public or always-on failure endpoints would create unnecessary risk and weaken the project.

## Consequences

- Application modifications remain narrow and defensible.
- Follow-up issues can be small and evidence-driven.
- Terraform `lab-small` can use defined ports, checks, log paths, and storage assumptions.
- Some changes may require source inspection before implementation.
- The project will avoid claiming business feature development that was not performed.

## Follow-up issues

Recommended follow-up issues after this ADR:

1. Inspect OpenKoda source for existing actuator, health, and metrics support.
2. Implement or configure liveness/readiness endpoints only if needed.
3. Define Nginx request ID forwarding and access log format.
4. Define Prometheus scrape target after monitoring node exists.
5. Define filesystem storage path and backup scope.
6. Define first controlled incident drill after service management is stable.
