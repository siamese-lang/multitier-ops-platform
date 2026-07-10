# Runbooks

Runbooks describe repeatable operational procedures. They must be written so that another operator can reproduce the same check or recovery action without relying on memory.

## Required structure

Each runbook should include:

1. Purpose
2. Scope
3. Preconditions
4. Commands
5. Expected results
6. Actual evidence location
7. Rollback or recovery steps
8. Known risks
9. Related issues and PRs

## Rules

- Commands must be copyable where possible.
- Destructive commands must be clearly marked.
- Do not include secrets, private keys, tokens, or passwords.
- When using AWS, identify the environment, region, and Terraform workspace or directory.
- When using Ansible, include the inventory and playbook path.

## Initial runbooks

- `openkoda-original-runbook.md`: upstream OpenKoda execution verification before local modifications.
