# Project scope

## Fixed project theme

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

Short definition:

```text
VM 기반 WEB/WAS/DB 운영환경을 직접 구성하고, 장애·성능·복구 문제를 로그와 지표로 분석하는 운영 포트폴리오
```

This is the source-of-truth scope document for the repository. When a future task becomes ambiguous, return to this document before creating another issue or pull request.

## What this project is

This project demonstrates the ability to operate a multi-tier business system on VM-style infrastructure.

The project should prove that the operator can:

```text
understand the target workload,
place components into separate tiers,
define repeatable operating configuration,
reproduce failures,
narrow causes using logs, metrics, and command output,
validate recovery procedures,
and document the evidence clearly.
```

The target portfolio message is:

```text
The candidate may not have production operations experience yet,
but understands WEB/WAS/DB/storage/observability/backup separation,
knows which layer to inspect during failures,
and can document configuration changes and recovery procedures using evidence.
```

## What this project is not

This repository must not be interpreted as any of the following:

```text
OpenKoda feature development project
OpenKoda UI customization project
Terraform module showcase
AWS managed-service architecture demo
Kubernetes/EKS/GitOps project
Docker Compose deployment exercise
Grafana dashboard gallery
sample Spring Boot CRUD project
```

Those tools or workloads can appear in the repository, but they are not the project theme.

## Role of each technology

| Element | Role in this project | Boundary |
|---|---|---|
| OpenKoda | Candidate business workload and Phase 0 smoke-test workload | Not the project theme |
| ops-sample-service | Controlled workload for reproducible WEB/WAS/DB drills | Not the final product |
| AWS EC2 | VM-style infrastructure substrate | Avoid managed-service-first framing |
| Terraform | Repeatable lab lifecycle control | Not a Terraform portfolio by itself |
| Ansible | Repeatable host configuration and runbook execution | Focus on operating consistency |
| Nginx | WEB/reverse proxy tier | Analyze upstream, timeout, access/error logs |
| Spring Boot/Tomcat | WAS tier | Analyze readiness, request logs, thread/connection behavior |
| PostgreSQL | DB tier | Analyze connections, failures, backup, restore, failover |
| NFS/filesystem | File storage tier | Analyze file/metadata consistency and recovery |
| Prometheus/Grafana/Loki | Evidence collection for metrics/logs | Do not stop at dashboard creation |
| Restic/pg_dump | Backup and restore validation tools | Restore verification is the core |

## Included scope

The intended final scope includes:

```text
EC2-based VM multi-tier environment
public/private subnet separation
security-group-based tier control
bastion access
Nginx reverse proxy
multi-node WAS tier
PostgreSQL primary and optionally standby
filesystem or NFS-backed file storage
Prometheus/Grafana/Alertmanager
Loki/Alloy
Restic and pg_dump backup/restore
Terraform apply/destroy lifecycle
Ansible configuration automation
failure drills and incident reports
logs/metrics/command evidence
```

## Excluded scope

The following should not become the center of work:

```text
OpenKoda business-feature implementation
OpenKoda UI improvement
Kubernetes/EKS
ArgoCD/GitOps
RDS-centered DB operation
ALB/RDS/CloudWatch-only managed architecture
simple Docker Compose deployment
simple monitoring dashboard construction
work without incident scenario or evidence
```

ALB can be used only if it supports the VM-based operating story, for example as an optional entry point in front of Nginx. It must not replace the Nginx/WAS/DB operating analysis.

## Evidence-first rule

A task is valuable only when it leaves evidence that supports an operations narrative.

Every major task should include at least one of the following:

```text
Terraform plan/apply/destroy evidence
Ansible recap
systemd state
Nginx access/error log
application journal log
PostgreSQL activity/query/backup/restore output
Prometheus metric change
Loki query result
latency/error-rate comparison
incident report
restore validation result
```

## Minimum portfolio criteria

The repository should eventually prove at least four of the following six scenarios:

```text
1. App node failure and Nginx upstream bypass
2. Rolling deploy or rolling restart with service continuity
3. WAS thread/HikariCP/DB connection bottleneck analysis
4. PostgreSQL failure and standby or recovery procedure
5. File storage failure and recovery
6. Backup followed by restore into a separate environment
```

Current status: scenarios 1, 2, and DB primary failure/recovery have already been validated in `lab-full-min`. DB-backed concurrent request observation has also been validated, but a deeper pool-bottleneck analysis remains future work.

## Decision rule for future work

Before adding a new issue or PR, ask:

```text
Does this work strengthen multi-tier operations, failure diagnosis, or recovery validation?
Will it produce logs, metrics, command output, or incident evidence?
Does it avoid turning the project into OpenKoda/Terraform/AWS-dashboard work?
```

If the answer is no, do not proceed.
