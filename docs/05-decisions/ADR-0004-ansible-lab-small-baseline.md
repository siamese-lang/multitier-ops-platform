# ADR-0004: Ansible lab-small baseline

## Status

Accepted

## Context

The Terraform `lab-small` baseline has been implemented and manually verified.

The verified environment had:

- `bastion-01` in a public subnet
- `app-01` in a private subnet
- no public IP on `app-01`
- SSH access to `app-01` through `bastion-01`
- no NAT Gateway in the private subnet
- successful Terraform teardown

The next project phase needs Ansible, but jumping directly to OpenKoda deployment would combine too many unknowns:

- inventory generation
- Windows/Git Bash SSH behavior
- bastion/jump access
- no-NAT package installation constraints
- application runtime setup

## Decision

The first Ansible milestone will be a connection and control-plane baseline, not an application deployment milestone.

The baseline will:

- use a static `lab-small` inventory example first
- use a local ignored inventory file for real IP addresses and private key paths
- represent app-node access through `bastion-01`
- use `ProxyCommand` as the documented jump strategy
- avoid copying private keys to bastion
- run `ansible ping` against bastion and app groups
- gather basic facts
- run a minimal no-package baseline playbook

## Rationale

### Why static inventory first

Terraform output-to-inventory automation is useful, but it is not the first problem to solve.

The first problem is verifying that Ansible can control the private app node through the bastion path.

A static inventory example keeps the implementation small and reviewable.

### Why ProxyCommand

Manual SSH verification succeeded with `ProxyCommand` in the Windows Git Bash environment.

`ProxyJump` is conceptually cleaner, but it failed in the user's local environment because key handling did not behave as expected for the jump host.

The baseline should encode the path that was actually verified.

### Why no private key on bastion

Copying the operator private key to bastion would make the lab easier but weaker from an operations/security perspective.

The operator workstation should initiate the SSH chain and keep the private key local.

### Why no package installation yet

`app-01` is intentionally in a private subnet without NAT.

Package installation from public repositories would fail or require changing the network design.

This should be handled in a later design decision rather than hidden inside the first Ansible implementation.

## Consequences

Positive consequences:

- Ansible work starts with a clear, testable baseline.
- The project preserves the private-node access model proven by Terraform.
- Evidence can show bastion-mediated configuration control.
- The no-NAT constraint is explicit instead of being discovered during package installation.

Tradeoffs:

- The first Ansible PR will not install OpenKoda or Docker.
- Inventory automation is deferred.
- A later issue must decide how private nodes install packages or receive artifacts.

## Rejected alternatives

### Deploy OpenKoda immediately

Rejected because it mixes inventory, SSH, package installation, Docker, application configuration, and runtime checks in one large change.

### Copy private key to bastion

Rejected because it weakens the access model and creates avoidable key-management risk.

### Add NAT Gateway immediately

Rejected for the first Ansible baseline because NAT changes the infrastructure scope. It may be introduced later with an explicit cost and operations rationale.

### Use dynamic inventory first

Rejected because dynamic inventory is not necessary to prove the first Ansible control path.

## Follow-up

Create an implementation issue for:

- `infra/ansible/inventories/lab-small/hosts.yml.example`
- local inventory ignore rules
- a minimal `lab-small-baseline.yml` playbook
- evidence commands for `ansible-inventory`, `ansible ping`, and playbook recap
