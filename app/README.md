# Application Layer

This directory is reserved for local operational modifications to the OpenKoda application.

## Rules

- Do not imply that this repository authored the upstream OpenKoda application.
- Keep upstream source information in `upstream/` and `NOTICE.md`.
- Every local application modification must be tied to a GitHub Issue.
- Each modification must include verification evidence.

## Expected operational modifications

Potential future modifications include:

- liveness endpoint
- readiness endpoint
- actuator/prometheus metrics
- X-Request-ID propagation and logging
- filesystem storage configuration
- controlled fault-injection endpoint for incident drills

## Status

No application modification has been implemented yet.
