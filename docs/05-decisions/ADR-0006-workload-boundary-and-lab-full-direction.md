# ADR-0006: Workload Boundary and lab-full Direction

## Status

Accepted

## Context

The project topic is:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

Recent work used OpenKoda to prove that a private EC2 app node can run a representative workload through Terraform and Ansible. That was useful as a smoke test, but it also introduced a risk: the repository could appear to be an OpenKoda deployment project.

That interpretation would be incorrect.

The project is intended to demonstrate VM-based operations capability:

- WEB/WAS/DB tier separation
- server configuration and repeatability
- service health checks
- logs and metrics for root-cause narrowing
- failure reproduction
- recovery procedure verification
- backup and restore validation

## Decision

1. Treat OpenKoda as a workload, not the project subject.
2. Reclassify current `lab-runtime` OpenKoda work as Phase 0 smoke-test evidence.
3. Stop expanding single-node OpenKoda operations as the main path.
4. Make `lab-full-min` WEB/WAS/DB separation the next implementation target.
5. Evaluate whether OpenKoda can support the required operating scenarios without excessive product-specific work.
6. If OpenKoda blocks clear WEB/WAS/DB/storage operations evidence, replace or supplement it with a purpose-built Spring Boot workload.

## Revised phase model

### Phase 0. lab-runtime smoke test

Purpose:

```text
Prove the infrastructure/control/evidence loop.
```

Validated capabilities:

- Terraform lab lifecycle
- bastion-to-private-app Ansible path
- private node NAT egress
- representative workload execution
- health check evidence
- teardown evidence

This phase does not prove the final project goal.

### Phase 1. lab-full-min

Purpose:

```text
Build the minimum WEB/WAS/DB operating environment.
```

Target nodes:

```text
bastion-01
nginx-01
app-01
app-02
db-primary-01
```

Required evidence:

- Nginx upstream configuration
- app readiness check
- PostgreSQL connection check
- Nginx access/error logs
- app logs
- DB connection state
- app node failure and upstream behavior

### Phase 2. lab-full-ops

Purpose:

```text
Add file storage, observability, logging, backup, and load generation.
```

Target additions:

```text
nfs-01
mon-01
log-01
backup-01
loadgen-01
```

### Phase 3. restore-lab

Purpose:

```text
Verify that backup artifacts can restore service data in a separate environment.
```

## Workload selection criteria

A workload is suitable only if it supports operations evidence for:

- WEB/reverse proxy behavior
- WAS health/readiness behavior
- DB connectivity and failure behavior
- logs useful for narrowing down failure causes
- backup and restore verification
- repeatable deployment and recovery

OpenKoda remains acceptable only if it helps those goals.

If OpenKoda becomes the center of work, the project has drifted.

## Consequences

- Some existing OpenKoda documents remain useful, but they must be read as Phase 0 evidence.
- Future issue titles should avoid making OpenKoda the main subject unless the task is explicitly workload-related.
- The next design work should focus on `lab-full-min`, not on OpenKoda single-node hardening.
- A small custom Spring Boot workload is allowed if it better demonstrates WEB/WAS/DB/storage operations.
- Terraform and Ansible remain implementation tools, not the portfolio thesis.

## Non-goals

- OpenKoda feature development
- OpenKoda UI customization
- Docker Compose as the final architecture
- managed-service-heavy AWS architecture
- EKS or GitOps migration
- dashboard-only observability work
