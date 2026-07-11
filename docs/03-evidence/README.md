# Evidence

Evidence is the core output of this project. Each implementation issue must produce evidence that shows what was changed, how it was verified, and what result was observed.

## Evidence principles

- Record the command and the output together.
- Distinguish expected results from actual results.
- Keep enough context to reproduce the verification.
- Summarize large logs and store full logs only when safe.
- Do not store secrets, private keys, AWS access keys, database passwords, session tokens, or personal information.

## Evidence index

- [`2026-07-11-lab-runtime-openkoda`](./2026-07-11-lab-runtime-openkoda/) - Terraform `lab-runtime` apply, private app Ansible control path, Docker/OpenKoda single-node deployment, local health check, and teardown evidence.

## Recommended directory naming

Use one of the following patterns:

```text
YYYY-MM-DD-short-scenario-name
issue-N-short-scenario-name
```

Examples:

```text
docs/03-evidence/2026-07-10-openkoda-original-run/
docs/03-evidence/issue-3-openkoda-original-run/
```

## Recommended evidence README structure

Each evidence directory should include a `README.md` with:

1. Scenario
2. Date and timezone
3. Environment
4. Related issue and PR
5. Commands executed
6. Expected result
7. Actual result
8. Relevant logs or screenshots
9. Conclusion
10. Follow-up actions

## Common evidence types

### Terraform

- `terraform fmt -check`
- `terraform validate`
- `terraform plan`
- `terraform apply`
- `terraform output`
- `terraform destroy` when relevant

### Ansible

- inventory used
- playbook command
- task changes
- recap
- failed task logs

### Service verification

- `systemctl status`
- `journalctl`
- `ss -lntp`
- `curl -i`
- application log excerpts

### Incident drills

- trigger command
- start time and end time
- symptom
- detection signal
- mitigation action
- recovery evidence
- incident report link
