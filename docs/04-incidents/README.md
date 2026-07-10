# Incident Reports

Incident reports document controlled failure scenarios and recovery verification.

This project uses incident reports to show operational reasoning, not just infrastructure construction.

## Required structure

Each incident report should include:

1. Summary
2. Environment
3. Impact
4. Timeline
5. Detection signal
6. Root cause or fault trigger
7. Mitigation
8. Recovery verification
9. Evidence links
10. Prevention or follow-up actions

## Timeline format

Use absolute timestamps with timezone.

```text
2026-07-10 22:00 KST - Fault injected
2026-07-10 22:01 KST - Alert fired
2026-07-10 22:03 KST - Mitigation started
2026-07-10 22:05 KST - Service recovered
```

## Rules

- Every incident must be reproducible or clearly marked as non-reproducible.
- Recovery must be verified with commands, HTTP checks, metrics, logs, or user-visible behavior.
- Do not call a scenario successful without evidence of both failure and recovery.
- Do not include secrets or private infrastructure credentials.

## Future incident report candidates

- Nginx node failure
- Application node failure
- PostgreSQL primary failure
- NFS outage
- Disk full condition
- High latency or 5xx spike
- Backup restore failure
