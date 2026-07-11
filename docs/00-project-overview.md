# Project Overview

## Project title

AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증

## Purpose

This project is an operations portfolio for a VM-based multi-tier business system on AWS EC2.

The goal is not to install a specific open-source product. The goal is to show that the operator can build a service environment, separate responsibilities by tier, define operating checks, reproduce failures, narrow down causes with logs and metrics, and verify recovery procedures.

The project message is:

```text
AWS EC2를 VM 환경으로 사용해 WEB/WAS/DB/파일저장소/관측성/백업 계층을 분리 구성하고,
장애·성능·복구 시나리오를 로그·지표·명령 결과로 검증하는 운영 포트폴리오.
```

## What this project is not

This repository is not:

- an OpenKoda product development project
- an OpenKoda UI customization project
- a Docker Compose deployment demo
- a Terraform module showcase
- a managed AWS architecture showcase
- a Kubernetes/EKS/GitOps project
- a dashboard-only observability project

## Workload strategy

The project needs a representative business application workload. OpenKoda is one candidate workload because it resembles an internal business system and already provides an upstream open-source application to operate.

However, OpenKoda is not the subject of this project.

OpenKoda is used only when it helps demonstrate the operating environment. If it cannot support the required WEB/WAS/DB/storage separation without excessive product-specific work, it will remain a Phase 0 smoke-test workload and the `lab-full` environment may use a purpose-built Spring Boot workload for clearer operations evidence.

## Main technical scope

- AWS EC2 as VM-style infrastructure
- Terraform for reproducible lab lifecycle
- Ansible for server configuration and operational procedures
- Nginx for WEB/reverse proxy tier
- WAS nodes for application runtime and readiness checks
- PostgreSQL for database tier
- NFS or filesystem storage for file consistency checks
- Prometheus, Grafana, Alertmanager for metrics and alerting
- Loki and Grafana Alloy for log collection
- Restic and pg_dump for backup and restore verification
- Load generation for traffic and incident drills

## Environment progression

### Phase 0. `lab-runtime` smoke test

Status: completed.

Purpose:

- prove private EC2 workload execution
- verify bastion-to-private-app Ansible path
- verify NAT-based dependency retrieval
- run a representative workload
- collect evidence
- destroy resources after validation

This phase used OpenKoda as the smoke-test workload. It must not be interpreted as the final project scope.

### Phase 1. `lab-full-min`

Status: next implementation target.

Target nodes:

```text
bastion-01
nginx-01
app-01
app-02
db-primary-01
```

Primary goal:

```text
WEB/WAS/DB 분리 구성과 기본 운영 점검 기준 수립
```

Evidence target:

- Nginx upstream configuration
- app readiness endpoint
- DB connection check
- Nginx access/error logs
- app logs
- PostgreSQL connection state
- app node failure and upstream behavior

### Phase 2. `lab-full-ops`

Target nodes:

```text
nfs-01
mon-01
log-01
backup-01
loadgen-01
```

Primary goal:

```text
파일 저장소, 관측성, 로그 수집, 백업·복구 실험 기반 구성
```

### Phase 3. incident scenarios

Minimum scenarios:

1. App node failure and Nginx upstream bypass
2. Rolling deploy with readiness checks
3. WAS thread / DB connection pool bottleneck
4. Backup and restore verification in a separate restore lab

Additional scenarios:

- PostgreSQL primary failure and standby promotion
- file storage failure and consistency verification
- log and metric based root-cause narrowing

### Phase 4. `restore-lab`

Primary goal:

```text
백업 파일이 존재한다는 사실이 아니라, 실제 별도 환경에서 복구되는지 검증
```

Evidence target:

- pg_dump restore
- file restore
- checksum comparison
- HTTP/file download verification
- application-level data verification

## Evidence-first rule

Each implementation issue must produce evidence. Evidence may include command output, service status, logs, HTTP response checks, Terraform plan/apply output, Ansible recap, dashboards, alerts, backup manifests, restore logs, and incident reports.

Evidence should answer four questions:

1. Was the failure or condition actually created?
2. What logs, metrics, or command outputs changed?
3. How were causes narrowed down?
4. Was recovery actually verified?

## Current implementation direction

The next implementation direction is not further OpenKoda single-node work.

The next direction is:

```text
lab-full-min WEB/WAS/DB 최소 구성 설계 및 구현
```
