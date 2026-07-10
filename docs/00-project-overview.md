# Project Overview

## Project title

AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증

## Purpose

This project builds an operations-oriented AWS EC2 environment for a multi-tier business application and verifies operational scenarios such as deployment, health checks, observability, backup, restore, and incident response.

The goal is not to produce a tutorial-style demo. The goal is to produce evidence that an operator can design, configure, verify, troubleshoot, and document a VM-based service environment.

## Target application

The target application is OpenKoda, an upstream open-source internal business application platform.

This repository does not claim authorship of OpenKoda. Application changes must be documented as operational modifications on top of the upstream source.

## Main technical scope

- AWS EC2 as VM-style infrastructure
- Terraform for infrastructure provisioning
- Ansible for server configuration
- Nginx for web/reverse proxy tier
- Spring Boot embedded Tomcat for application tier
- PostgreSQL for database tier
- NFS for shared filesystem storage
- Prometheus, Grafana, Alertmanager for metrics and alerting
- Loki and Grafana Alloy for log collection
- Restic for backup and restore verification
- Load generator for traffic and incident drills

## Environment progression

1. OpenKoda upstream execution verification
2. OpenKoda operational hardening
3. Repository structure and documentation baseline
4. Terraform `lab-small`
5. Ansible base roles
6. `lab-small` deployment verification
7. `lab-full` expansion
8. Observability configuration
9. Backup and restore configuration
10. Incident scenario execution
11. Restore-lab verification
12. Portfolio documentation

## Evidence-first rule

Each implementation issue must produce evidence. Evidence may include command output, service status, logs, HTTP response checks, Terraform plan/apply output, Ansible recap, dashboards, alerts, backup manifests, restore logs, and incident reports.
