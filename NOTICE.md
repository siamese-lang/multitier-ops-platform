# Notice

This project uses OpenKoda as an upstream open-source application target for an operations portfolio project.

OpenKoda is not authored by the owner of this repository. This repository focuses on infrastructure, configuration automation, operational hardening, observability, backup/restore verification, and incident documentation around the upstream application.

## Upstream application

- Name: OpenKoda
- Upstream repository: `https://github.com/openkoda/openkoda`
- Repository full name: `openkoda/openkoda`
- License: MIT License
- Copyright notice: `Copyright (c) 2023 openkoda`
- Checked reference: upstream `main` branch documentation reviewed on 2026-07-10 KST
- Verification date: 2026-07-10 KST

## MIT license attribution requirement

The upstream OpenKoda LICENSE grants permission to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the software, provided that the copyright notice and permission notice are included in all copies or substantial portions of the software.

If this project copies OpenKoda source code or substantial portions of it into this repository, the upstream MIT copyright and permission notice must be preserved.

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