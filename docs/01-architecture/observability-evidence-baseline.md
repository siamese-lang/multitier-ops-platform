# Observability evidence baseline design

## Status

Proposed design for Phase 4.

This document does not implement Terraform, Ansible, Prometheus, Grafana, Loki, or AWS runtime validation. It defines the minimum observability evidence baseline that should be implemented and validated next.

## Purpose

The fixed project theme remains:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

Phase 4 starts after successful restore-lab DB/file/API recovery validation. Its purpose is not to build a polished monitoring dashboard. Its purpose is to define the minimum logs and metrics needed to narrow failure classes in an EC2-based WEB/WAS/DB/Storage/Backup operating environment.

The target operating question is:

```text
When the service fails or degrades, which tier should the operator inspect first, and what evidence proves that conclusion?
```

## Design boundary

This baseline may claim that the project has defined an observability evidence standard only after the required evidence classes, collection points, and incident-report structure are documented.

It must not claim any of the following from this design PR alone:

```text
production-grade monitoring
high availability
automated incident response
complete Prometheus/Loki platform coverage
Grafana dashboard maturity
CloudWatch-managed observability
new runtime validation
```

A later runtime validation may claim observability baseline success only when collected logs or metrics are used in an incident report to narrow the failure class.

## Initial topology

The first observability baseline should reuse the already validated reduced `lab-full-ops` style topology:

```text
[Public Subnet]
- bastion-01
- nginx-01

[Private App Subnet]
- app-01

[Private DB Subnet]
- db-primary-01

[Private Storage Subnet]
- nfs-01

[Private Ops Subnet]
- backup-01
```

The first baseline targets these operating nodes:

```text
nginx-01
app-01
db-primary-01
nfs-01
backup-01
```

Optional nodes such as `mon-01`, `log-01`, `app-02`, and `loadgen-01` may be introduced later, but they are not required for the first Phase 4 evidence baseline unless an implementation PR explicitly justifies them.

## Evidence model

The observability baseline should collect evidence in six classes:

```text
1. node health and resource pressure
2. service state
3. tier-specific logs
4. request-path visibility
5. backup/restore job visibility
6. one incident report that interprets the evidence
```

The baseline is not complete if it only stores raw logs. It must connect evidence to an operating conclusion such as:

```text
WEB path healthy but WAS cannot reach DB
WAS process running but readiness fails because DB is unavailable
DB host reachable but PostgreSQL service inactive
NFS host reachable but file path unavailable or full
backup job failed before artifact creation or during restore staging
```

## Minimum evidence by tier

| Tier | Node | Minimum metrics or state | Minimum logs | Operating question |
|---|---|---|---|---|
| WEB | `nginx-01` | host load, memory, disk, TCP listeners, Nginx service state | `/var/log/nginx/access.log`, `/var/log/nginx/error.log` | Did the request reach WEB, and did Nginx fail before or after upstream selection? |
| WAS | `app-01` | host load, memory, disk, app port listener, service state | `journalctl -u ops-sample-service` | Is the app process running, ready, and able to reach DB/NFS dependencies? |
| DB | `db-primary-01` | host load, memory, disk, port 5432 listener, PostgreSQL service state | PostgreSQL service journal and configured PostgreSQL logs | Is the DB host reachable, and is PostgreSQL accepting application connections? |
| Storage | `nfs-01` | disk usage, inode usage, NFS export state, NFS service state | NFS service journal and export output | Is the file tier mounted/exported and writable from the expected clients? |
| Backup/Ops | `backup-01` | host load, disk usage, artifact directory state, package/runtime state | backup/restore command output and system journal excerpts | Did the backup or restore job fail at artifact, DB, file, or control-path stage? |

## Request-path visibility

The baseline should preserve request correlation across the WEB and WAS path as much as the current workload supports.

Required HTTP-path evidence:

```text
Nginx access log line for /healthz
Nginx access log line for /readyz
Nginx access log line for one DB-backed API request
Nginx access log line for one DB/file consistency request
ops-sample-service journal excerpt near the same validation window
```

If a request ID is already produced by Nginx or the application, the evidence should preserve it. If request ID propagation is incomplete, the first Phase 4 implementation may document the gap instead of expanding application features prematurely.

## Node metrics baseline

The first baseline should prefer operating evidence over dashboard polish. Acceptable first-pass node metrics include command output from each target node:

```text
date -Is
hostname
uptime
free -m
df -h
df -i
ss -lntp
systemctl is-active <tier-service>
journalctl excerpts for tier services
```

Prometheus `node_exporter` can be added later if the implementation remains small and produces incident evidence. A Prometheus scrape target list alone is not sufficient unless the collected metrics are used to support an operating conclusion.

## Log visibility baseline

Minimum log sources:

```text
nginx-01: /var/log/nginx/access.log
nginx-01: /var/log/nginx/error.log
app-01: journalctl -u ops-sample-service
db-primary-01: journalctl -u postgresql or PostgreSQL log directory evidence
nfs-01: journalctl for nfs-server/nfs-kernel-server and exportfs -v
backup-01: backup/restore command logs from the validation window
```

Loki/Alloy may be added after the basic log evidence path is proven. The first implementation should not become a logging-platform build-out before an incident report exists.

## First incident-report target

The recommended first incident report is a controlled PostgreSQL service outage during a single runtime window.

Suggested failure class:

```text
DB service unavailable while host/network path remains reachable
```

Expected evidence chain:

```text
1. Nginx access log shows request reached WEB.
2. Nginx upstream result or HTTP status shows application path degraded.
3. ops-sample-service journal/readiness output shows DB dependency failure.
4. db-primary-01 host responds to SSH or node checks.
5. PostgreSQL service state is inactive or failed.
6. After PostgreSQL restart, readiness and DB-backed API path recover.
```

This scenario is intentionally narrower than HA or failover. It proves that logs and metrics help distinguish DB service failure from WEB, WAS, network, or storage failure.

## Runtime success criteria

A future Phase 4 runtime validation succeeds only when all of the following are true:

```text
node baseline evidence exists for nginx-01, app-01, db-primary-01, nfs-01, backup-01
Nginx access/error log evidence is preserved
ops-sample-service journal evidence is preserved
PostgreSQL service/log evidence is preserved
NFS/storage service and filesystem evidence is preserved
backup/restore job log visibility is preserved if a backup or restore step is part of the window
one incident report uses evidence to narrow the failure class
AWS resources are destroyed after evidence collection
```

A successful runtime evidence document should state the narrow claim:

```text
Observability baseline evidence validated for EC2 WEB/WAS/DB/Storage/Backup diagnosis.
```

It should not claim:

```text
full monitoring platform maturity
production observability
complete alerting coverage
SLO/SLA compliance
```

## Out of scope for the first Phase 4 PR

```text
Grafana dashboard polish
large Prometheus/Loki platform build-out
Alertmanager notification routing
Kubernetes/EKS/GitOps
OpenKoda feature work
Spring Boot business feature expansion
Terraform module refactoring as the primary goal
CloudWatch-only managed observability framing
multi-day AWS runtime experiments
```

## Recommended implementation order

```text
1. [DESIGN] Define observability evidence baseline
2. [ANSIBLE] Add observability evidence collection runbook/playbook
3. [VALIDATION] Run one batched observability baseline window
4. [DOCS] Document Phase 4 evidence and update roadmap
```

The first PR should stay documentation-only. Runtime validation should happen only after the baseline is reviewed and the evidence targets are fixed.

## Runtime policy

Do not open AWS for this design PR.

When implementation begins, use one batched runtime window:

```text
apply once -> configure/collect baseline -> run one controlled incident -> collect evidence -> recover -> destroy once
```

If NAT Gateway is enabled for package installation, keep the window short and destroy immediately after evidence collection.
