# Notice

This project uses OpenKoda as an upstream open-source application target for an operations portfolio project.

OpenKoda is not authored by the owner of this repository. This repository focuses on infrastructure, configuration automation, operational hardening, observability, backup/restore verification, and incident documentation around the upstream application.

## Upstream application

- Name: OpenKoda
- Upstream repository: `<TO_BE_FILLED>`
- License: `<TO_BE_FILLED>`
- Checked commit or release: `<TO_BE_FILLED>`
- Verification date: `<TO_BE_FILLED>`

## Scope of local modifications

Any application-level changes in this repository must be limited to operational hardening, such as:

- liveness/readiness endpoints
- actuator/prometheus metrics exposure
- request ID propagation and logging
- filesystem storage configuration
- fault-injection endpoints for controlled incident drills

Functional business logic changes are out of scope unless they are explicitly documented in a separate issue and decision record.

## Sensitive information

Do not commit AWS credentials, private keys, database passwords, session tokens, personal information, or production-like secrets to this repository.
