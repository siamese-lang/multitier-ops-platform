# Target Architecture

## Goal

The final target is a two-AZ `lab-full` environment that separates web, application, database, file storage, observability, logging, backup, bastion, and load generation responsibilities.

## Node layout

| Role | Nodes | Purpose |
|---|---:|---|
| Bastion | bastion-01 | Controlled SSH entry point |
| Web | nginx-01, nginx-02 | Reverse proxy and web tier |
| Application | app-01, app-02, app-03 | OpenKoda application nodes |
| Database | db-primary-01, db-standby-01 | PostgreSQL primary/standby |
| File storage | nfs-01 | Shared filesystem storage |
| Monitoring | mon-01 | Prometheus, Grafana, Alertmanager |
| Logging | log-01 | Loki and log ingestion components |
| Backup | backup-01 | Restic repository and restore operations |
| Load generation | loadgen-01 | Controlled traffic and incident drills |

## Design principles

- Use EC2 instances as VM-like servers.
- Minimize managed application platforms to avoid overlap with previous EKS-based work.
- Keep provisioning and configuration separated: Terraform creates infrastructure, Ansible configures servers.
- Treat every service as an operational component with health checks, logs, metrics, and recovery procedures.
- Prefer explicit runbooks and evidence over informal screenshots.

## Network direction

The detailed VPC, subnet, route table, and security group design will be implemented in later Terraform issues. At a high level:

- Public subnet: bastion and external-facing web entry points where required.
- Private application subnets: app nodes.
- Private data subnets: database, NFS, monitoring, logging, backup.
- Load generation node may be placed in a controlled subnet depending on the test scenario.

## Status

This document is a target design baseline. It is not yet evidence of a deployed environment.
