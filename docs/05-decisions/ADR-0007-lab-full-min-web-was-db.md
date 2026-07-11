# ADR-0007: lab-full-min WEB/WAS/DB minimum operating environment

## Status

Accepted

## Context

The project scope has been redefined around the original portfolio topic:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The completed `lab-runtime` work proved that a private EC2 node can run a representative workload through Terraform, NAT, bastion access, and Ansible. However, that phase does not yet prove the main project thesis: operating a separated WEB/WAS/DB environment and validating failure and recovery using logs, metrics, and command evidence.

The next phase must therefore stop extending the single-node OpenKoda path and instead define a minimal multi-tier operating environment.

## Decision

Create a new implementation target named `lab-full-min`.

`lab-full-min` will contain:

```text
[Public Subnet]
- bastion-01
- nginx-01

[Private App Subnet]
- app-01
- app-02

[Private DB Subnet]
- db-primary-01
```

This design is the minimum environment that can show:

- WEB tier routing through Nginx
- WAS tier redundancy with two app nodes
- DB tier separation through PostgreSQL
- security group separation between public, app, and DB tiers
- operator access through bastion
- first app-node failure scenario and recovery evidence

## Workload boundary

OpenKoda is not the project subject.

OpenKoda may be used only if it supports clean WEB/WAS/DB operating evidence without distorting the architecture. If OpenKoda becomes an obstacle, the project will use a purpose-built Spring Boot operations workload instead.

The workload must provide:

```text
GET /health
GET /ready
GET /api/items
POST /api/items
```

The workload itself is not the deliverable. It exists to create reliable operating signals for Nginx, app nodes, DB connectivity, logs, and recovery procedures.

## Network and security decision

Use explicit tier-to-tier access:

```text
operator CIDR -> bastion-01:22
operator CIDR -> nginx-01:80
bastion-01 -> private nodes:22
nginx-01 -> app-01/app-02:8080
app-01/app-02 -> db-primary-01:5432
```

Deny direct public access to app and DB nodes.

This keeps the portfolio focused on server operations and troubleshooting rather than managed-service architecture.

## First incident scenario

The first incident scenario will be:

```text
app-01 service stop
-> Nginx upstream behavior observation
-> traffic continuation through app-02
-> log and HTTP evidence collection
-> app-01 recovery
-> post-recovery verification
```

The scenario is intentionally simple. It is the first evidence-producing scenario for WEB/WAS separation. Later scenarios can add database failure, connection pool bottlenecks, storage errors, and backup/restore.

## Rationale

### Why start with WEB/WAS/DB only

The original target includes storage, observability, backup, logging, and restore lab. Building all of them at once would increase cost and delay evidence production.

`lab-full-min` creates the smallest environment that can demonstrate the main operating pattern:

```text
request -> web tier -> app tier -> DB tier
```

### Why two app nodes

A single app node only proves deployment. Two app nodes allow upstream behavior, partial failure, and recovery evidence.

### Why one DB node first

PostgreSQL primary/standby promotion is important, but it should come after basic DB separation and DB-dependent app readiness are proven.

### Why a purpose-built workload is allowed

If OpenKoda does not expose the needed operational signals cleanly, forcing it into this architecture would make the project weaker. A small Spring Boot workload can better expose health, readiness, DB access, and later latency/connection-pool scenarios.

This does not change the project thesis. The thesis is operating the environment, not showcasing an application.

## Consequences

- The next Terraform work should create `lab-full-min`, not extend `lab-runtime`.
- The next Ansible work should configure Nginx, PostgreSQL, and two app nodes.
- Existing OpenKoda single-node work remains useful as Phase 0 smoke-test evidence.
- OpenKoda-specific recovery playbooks must not drive the main roadmap.
- Future evidence should be organized around tier behavior, not application branding.

## Follow-up implementation issues

1. Terraform `lab-full-min` baseline
2. Ansible `lab-full-min` inventory and node bootstrap
3. PostgreSQL primary install/configuration
4. App workload deployment to `app-01` and `app-02`
5. Nginx upstream configuration
6. Baseline WEB/WAS/DB evidence
7. App-node failure and recovery incident report
