# ADR-0003: Terraform lab-small Baseline

## Status

Accepted

## Context

The project has completed the early application-discovery phase:

- OpenKoda was identified as the upstream open-source application target.
- OpenKoda was verified on a temporary EC2 instance through Docker Compose.
- Initial source inspection did not confirm existing Spring Boot Actuator or Prometheus endpoint support.
- Application-level operability targets were defined, but source modification is intentionally deferred.

The next project phase is Terraform `lab-small`.

It would be possible to start by building the full target architecture, but that would make troubleshooting harder and would blur the evidence trail. The project needs a small, repeatable infrastructure baseline first.

## Decision

The first Terraform `lab-small` implementation will create only the minimum AWS infrastructure required to prove repeatable provisioning, controlled SSH access, and private app-node placement.

The baseline will include:

1. VPC
2. public subnet
3. private subnet
4. Internet Gateway
5. public route table
6. private route table
7. security group for `bastion-01`
8. security group for `app-01`
9. EC2 instance `bastion-01`
10. EC2 instance `app-01`
11. outputs for bastion public IP and app private IP

The first baseline will not deploy OpenKoda, install Docker, configure Nginx, split PostgreSQL, add NFS, or create monitoring/logging/backup nodes.

## Rationale

### Why only bastion and app first

The project needs to prove the basic AWS access path before adding services. A bastion plus private app node demonstrates a realistic operations pattern without prematurely introducing database, reverse proxy, NFS, or monitoring complexity.

### Why app stays private

A core goal of this portfolio is to show operational judgment, not simply expose an EC2 port to the internet. Keeping `app-01` private forces the project to use a controlled access path and prepares for later Nginx-based routing.

### Why no NAT Gateway initially

NAT Gateway can be useful for private subnet package installation, but it adds cost and another troubleshooting variable. The first baseline should avoid it unless a later implementation issue explicitly needs it and captures the cost/benefit decision.

### Why no OpenKoda deployment yet

OpenKoda has already been manually verified. The next question is whether Terraform can build the infrastructure safely and repeatedly. Application deployment belongs to a later Ansible issue.

### Why local state is acceptable initially

This is currently a single-operator portfolio lab. Local state keeps the first implementation simple. `.tfstate` files must not be committed. Remote state can be introduced later if the project needs locking, collaboration, or longer-lived environments.

## Consequences

- The first Terraform PR stays small and reviewable.
- Evidence can focus on `fmt`, `validate`, `plan`, `apply`, `output`, SSH path, and `destroy`.
- The project avoids creating a costly or hard-to-debug environment too early.
- Later work must add deployment and service configuration through Ansible.
- Some private-subnet package installation decisions remain unresolved until the Ansible phase.

## Follow-up work

1. Implement Terraform `lab-small` baseline resources.
2. Verify SSH to `bastion-01` and jump access to `app-01`.
3. Produce Terraform apply/output/destroy evidence.
4. Add Ansible inventory generation or inventory documentation.
5. Configure Docker and OpenKoda on `app-01` through Ansible.

## Related issue

Issue #14.