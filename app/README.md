# Application Layer

This directory is reserved for local operational modifications to the OpenKoda application.

## Rules

- Do not imply that this repository authored the upstream OpenKoda application.
- Keep upstream source information in `upstream/` and `NOTICE.md`.
- Every local application modification must be tied to a GitHub Issue.
- Each modification must include verification evidence.
- Prefer configuration-based changes before source-code changes.

## Expected operational modifications

Potential future modifications include:

- liveness endpoint
- readiness endpoint
- actuator/prometheus metrics
- X-Request-ID propagation and logging
- filesystem storage configuration
- controlled fault-injection support for incident drills

The current scope and boundaries are defined in:

- `docs/01-architecture/openkoda-operability-scope.md`
- `docs/05-decisions/ADR-0002-openkoda-operability-hardening-scope.md`

## Current decision

No application modification has been implemented yet.

Issue #4 defines the operability hardening scope only. Follow-up issues should inspect the upstream source and implement the smallest necessary changes with evidence.
