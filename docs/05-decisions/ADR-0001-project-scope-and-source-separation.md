# ADR-0001: Project Scope and Upstream Source Separation

## Status

Accepted

## Context

This project is intended to demonstrate operations engineering capability through a realistic AWS EC2-based multi-tier environment.

The project topic is fixed as:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The project must not appear as if the repository owner authored OpenKoda. It must also avoid becoming an OpenKoda deployment project, a Terraform showcase, or an AWS managed-service architecture showcase.

The intended contribution is VM-based service operations:

- WEB/WAS/DB tier separation
- process and service management
- server configuration repeatability
- network path and security group reasoning
- logs and metrics for troubleshooting
- backup and restore verification
- incident reports and evidence

## Decision

1. Use AWS EC2 instances as VM-style infrastructure.
2. Minimize managed application platforms and managed runtime abstractions.
3. Use Terraform only for infrastructure lifecycle automation.
4. Use Ansible for server configuration and operational procedures.
5. Treat OpenKoda as an upstream workload, not the project subject.
6. Keep upstream application source information, local operational modifications, and infrastructure code clearly separated.
7. Treat `lab-runtime` OpenKoda work as Phase 0 smoke-test evidence.
8. Make `lab-full-min` WEB/WAS/DB separation the next implementation direction.

## Rationale

### Why EC2 as VM infrastructure

The goal is to show that the operator can understand server-level responsibilities: process management, network paths, logs, service health, storage, backup, and recovery. EC2 used as VM infrastructure makes those responsibilities visible.

### Why managed services are minimized

Managed services are valuable in production, but this portfolio project intentionally emphasizes OS, process, service, and failure analysis. Excessive use of managed services would hide the operational surface that this project is designed to demonstrate.

### Why upstream and local modifications are separated

OpenKoda is not authored by this repository owner. Separating upstream source and local operational modifications prevents misrepresentation and makes the engineering contribution clearer.

### Why OpenKoda is not the center

A business-system workload is necessary, but the project is evaluated on operations evidence rather than application features. If OpenKoda cannot support WEB/WAS/DB/storage separation in a clean way, a small purpose-built Spring Boot workload may be used for the `lab-full` phase.

### Why `lab-runtime` comes before `lab-full`

A smaller baseline made it possible to verify Terraform provisioning, Ansible access, private-node egress, workload execution, and evidence collection before introducing WEB/WAS/DB and observability complexity.

## Consequences

- The project will require more explicit runbooks and evidence.
- Some architecture choices may be less production-optimized than managed-service alternatives.
- OpenKoda-related documents must be interpreted as workload or smoke-test documents, not as the project thesis.
- Future work should prioritize tier separation, logs, metrics, incident reports, and restore validation.
- The portfolio will better demonstrate VM-based operations, troubleshooting, and recovery thinking.
