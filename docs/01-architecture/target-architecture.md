# Target Architecture

## Goal

The final target is an AWS EC2-based VM operations environment that separates WEB, WAS, DB, file storage, observability, logging, backup, bastion, and load generation responsibilities.

The architecture is not designed to showcase AWS managed services. It intentionally keeps the operating surface visible by using EC2 instances as VM-like servers.

## Project boundary

This architecture is centered on operations, not on a specific product.

OpenKoda may be used as a representative internal-business workload, but it is not the architecture center. If OpenKoda cannot support the required tier separation without excessive product-specific work, the `lab-full` workload may be replaced or supplemented by a purpose-built Spring Boot service with PostgreSQL and file upload behavior.

## Final node layout

| Tier | Nodes | Purpose |
|---|---:|---|
| Bastion | bastion-01 | Controlled SSH entry point |
| WEB | nginx-01, nginx-02 | Reverse proxy, upstream routing, access/error log evidence |
| WAS | app-01, app-02, app-03 | Application runtime, readiness, thread/connection behavior |
| DB | db-primary-01, db-standby-01 | PostgreSQL primary/standby, query and backup evidence |
| File storage | nfs-01 | Shared filesystem or attachment storage consistency checks |
| Metrics | mon-01 | Prometheus, Grafana, Alertmanager |
| Logs | log-01 | Loki and log ingestion components |
| Backup | backup-01 | Restic repository, pg_dump storage, restore operations |
| Load generation | loadgen-01 | Controlled traffic and incident drills |

## Phase architecture

### Phase 0. `lab-runtime`

Completed.

Purpose:

```text
private EC2 workload smoke test
```

Components:

```text
bastion-01
app-01
NAT Gateway for dependency pull
```

What it proves:

- Terraform can create and destroy a private runtime lab.
- Ansible can reach a private app node through bastion.
- The private node can retrieve runtime dependencies through NAT.
- A representative workload can run and be checked locally.
- Evidence can be collected and committed without secrets.

What it does not prove:

- WEB/WAS/DB separation
- Nginx upstream behavior
- PostgreSQL operations
- file storage consistency
- observability-based root-cause analysis
- backup/restore recovery

### Phase 1. `lab-full-min`

Completed minimum WEB/WAS/DB operating baseline.

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

Primary design questions already validated in Phase 1:

- How does Nginx route to multiple app nodes?
- What health/readiness signal is used for app nodes?
- How are Nginx access/error logs used during app failure?
- How do app nodes connect to PostgreSQL?
- What command proves DB connectivity and connection count?
- What changes when one app node is stopped?

Completed incident evidence:

```text
app-01 stop
-> Nginx upstream behavior observed
-> app-02 continued serving requests
-> access/error logs and HTTP results recorded
-> app-01 recovery verified
```

### Phase 2. `lab-full-ops`

Next target.

Detailed design:

```text
docs/01-architecture/lab-full-ops-storage-backup-observability.md
```

```text
[Private Storage Subnet]
- nfs-01

[Private Ops Subnet]
- mon-01
- log-01
- backup-01
- loadgen-01
```

Primary design questions:

- What file storage behavior proves DB metadata and file object consistency?
- What metrics change during app/WAS/DB/storage failure?
- Which logs are needed to narrow down the cause?
- What backup artifacts are produced?
- How is restore verified in a separate environment?

### Phase 3. `restore-lab`

Separate recovery validation environment.

```text
backup artifacts
-> new restore lab
-> restore DB
-> restore files
-> verify data and file consistency
```

## Network direction

High-level segmentation:

- Public subnet: bastion and WEB entry point only.
- Private app subnet: WAS nodes.
- Private DB subnet: PostgreSQL nodes.
- Private storage subnet: NFS or filesystem storage.
- Private ops subnet: monitoring, logging, backup, load generation.

Security group direction:

```text
operator -> bastion: SSH
bastion -> private nodes: SSH
client/loadgen -> nginx: HTTP
nginx -> app nodes: application port
app nodes -> db-primary/db-standby: PostgreSQL
app nodes -> nfs: filesystem protocol
mon/log -> scrape or receive telemetry as needed
backup -> db/storage: backup and restore operations
```

## Design principles

- Use EC2 instances as VM-like servers.
- Keep provisioning and configuration separated: Terraform creates infrastructure, Ansible configures servers.
- Treat OpenKoda as a workload, not the project subject.
- Prefer WEB/WAS/DB/Storage/Ops/Backup evidence over product-specific customization.
- Treat every service as an operational component with health checks, logs, metrics, and recovery procedures.
- Avoid managed-service shortcuts that hide the operational behavior the project is meant to demonstrate.
- Prefer explicit runbooks and evidence over screenshots.

## Status

This document is the corrected target architecture baseline.

The currently completed work includes Phase 0 `lab-runtime` and Phase 1 `lab-full-min`. The next architecture implementation must target Phase 2 `lab-full-ops` storage, backup, observability, logging, and load generation expansion.
