# Workload strategy

This document clarifies the relationship between OpenKoda, `ops-sample-service`, and future workload choices.

## Fixed principle

The project is not about building or customizing the workload application.

The project is about operating a workload in a VM-based multi-tier environment and proving failure/recovery behavior with evidence.

```text
Workload = target of operation
Project theme = multi-tier operations, failure diagnosis, and recovery validation
```

## OpenKoda role

OpenKoda is treated as a candidate business-system workload.

It is useful because it represents the kind of internal business application this portfolio wants to operate. However, OpenKoda is not the project theme and this repository does not claim authorship of OpenKoda.

Correct framing:

```text
OpenKoda was evaluated as an upstream business workload that can be operated inside the lab environment.
```

Incorrect framing:

```text
This project develops OpenKoda.
This project improves OpenKoda UI.
This project is an OpenKoda deployment tutorial.
```

## Phase 0 OpenKoda usage

In Phase 0, OpenKoda or an OpenKoda-like containerized workload can be used for smoke testing.

The goal of Phase 0 is to prove:

```text
Terraform can create a temporary EC2 lab.
Ansible can reach private instances through a bastion.
Private instances can use NAT for dependency pull.
A workload can start and pass a basic health check.
The lab can be destroyed after evidence is collected.
```

This does not prove final WEB/WAS/DB/storage/observability/backup operations.

## ops-sample-service role

`ops-sample-service` is a controlled workload used for reproducible operations drills in `lab-full-min`.

It exists because the project needs endpoints and logs that are intentionally designed for operational validation:

```text
/healthz: process-level health
/readyz: DB-dependent readiness
/node: node identity for upstream routing checks
/api/work-orders/summary: DB-backed business-like endpoint
request ID logging: cross-layer correlation
systemd service: VM-style workload operation
```

Correct framing:

```text
ops-sample-service is a controlled operating workload for WEB/WAS/DB validation.
```

Incorrect framing:

```text
The project is a Spring Boot CRUD app.
The project is mainly backend feature development.
The sample API is the portfolio product.
```

## Why a controlled workload was valid for lab-full-min

The `lab-full-min` phase needed to verify operating behavior, not business UI behavior.

The controlled workload made the following evidence possible:

```text
Nginx upstream bypass when app-01 is stopped
rolling restart of app-01/app-02
DB-backed request tracing through Nginx and app journal
PostgreSQL stopped while app service remains active
DB-dependent endpoint failure and recovery
```

This evidence would be harder to obtain cleanly from an upstream workload that does not expose controlled health/readiness/logging behavior.

## Future workload decision rule

For `lab-full-ops`, workload choice should be decided by operating evidence value.

A workload is acceptable only if it supports these requirements:

```text
Can run behind Nginx as a WAS workload.
Can use an external PostgreSQL database.
Can expose or support file storage behavior.
Can generate meaningful application logs.
Can be checked by health/readiness or equivalent endpoints.
Can support failure and recovery validation without feature-heavy customization.
```

If OpenKoda can satisfy these requirements without turning the project into OpenKoda feature work, it can be used as the representative workload.

If not, continue with `ops-sample-service` or extend it minimally for file/storage/backup validation.

## Acceptable workload modifications

Allowed modifications are operationally motivated:

```text
health/readiness endpoint
request ID logging
node identity endpoint
DB-backed test endpoint
file upload/download test endpoint
configuration externalization
systemd-friendly runtime configuration
```

Avoid feature-heavy changes:

```text
business UI screens
domain-specific feature development
large application refactoring
OpenKoda product customization
non-operational user-facing enhancements
```

## Recommended next workload direction

For Phase 2, the likely minimal requirement is file/storage behavior.

Recommended approach:

```text
Add or use a minimal file metadata/upload/download flow.
Store metadata in PostgreSQL.
Store actual files on NFS or filesystem-backed storage.
Verify metadata/file consistency.
Run file storage failure and recovery drills.
Back up both DB and files.
Restore both into restore-lab and verify checksum/download.
```

This keeps the project centered on operations rather than application development.
