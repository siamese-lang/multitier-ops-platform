# ADR-0001: Project Scope and Upstream Source Separation

## Status

Accepted

## Context

This project is intended to demonstrate operations engineering capability through a realistic AWS EC2-based multi-tier environment. The target application is OpenKoda, an upstream open-source application.

The project must not appear as if the repository owner authored OpenKoda. It must also avoid overlapping too heavily with prior EKS/Terraformers work. Therefore, the project focuses on VM-based service operations, infrastructure provisioning, configuration automation, observability, backup, restore, and incident response.

## Decision

1. Use AWS EC2 instances as VM-style infrastructure.
2. Minimize managed application platforms and managed runtime abstractions.
3. Use Terraform only for infrastructure provisioning.
4. Use Ansible for server configuration and service installation.
5. Treat OpenKoda as an upstream application target.
6. Keep upstream application source information, local operational modifications, and infrastructure code clearly separated.
7. Start with `lab-small` before expanding to `lab-full`.

## Rationale

### Why EC2 as VM infrastructure

The goal is to show that the operator can understand server-level responsibilities: process management, network paths, logs, service health, storage, backup, and recovery. EC2 used as VM infrastructure makes those responsibilities visible.

### Why managed services are minimized

Managed services are valuable in production, but this portfolio project intentionally emphasizes OS, process, service, and failure analysis. Excessive use of managed services would hide the operational surface that this project is designed to demonstrate.

### Why upstream and local modifications are separated

OpenKoda is not authored by this repository owner. Separating upstream source and local operational modifications prevents misrepresentation and makes the engineering contribution clearer.

### Why `lab-small` comes first

A smaller baseline makes it easier to verify application execution, Terraform provisioning, Ansible configuration, SSH access, service checks, and initial evidence before introducing multi-AZ complexity.

## Consequences

- The project will require more explicit runbooks and evidence.
- Some architecture choices may be less production-optimized than managed-service alternatives.
- The portfolio will better demonstrate VM-based operations, troubleshooting, and recovery thinking.
