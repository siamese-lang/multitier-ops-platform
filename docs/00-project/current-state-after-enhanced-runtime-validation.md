# Current State After Enhanced Runtime Validation

## Fixed project theme

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

This project remains a VM-based WEB/WAS/DB operations portfolio. It is not an OpenKoda project, Terraform showcase, Kubernetes project, or Spring Boot feature-development project.

The operating target is still `ops-sample-service`, but the service is not frozen. It may be extended when a service behavior is required to reproduce realistic WEB/WAS/DB/Storage operating scenarios. Application changes must remain subordinate to operations validation and evidence collection.

## Runtime validation status

Completed validation scope:

```text
S1 enhanced service workflow validation: completed
S2 upload-limit incident validation: completed
S3 latency scenario validation: completed
S4 DB web-impact incident validation: completed
Backup baseline: completed
Restore-lab DB/file restore baseline: completed
Restore-lab HTTP/API consistency validation: completed
Source lab destroy: completed
Restore lab destroy: completed
```

These completed validations are preserved as existing runtime evidence. They do not mean the project should be closed immediately, and they do not make the next phase documentation-only.

## Validated recovery proof

The restore-lab recovery proof is valid because both conditions were met:

```text
1. DB/file restore baseline succeeded.
2. HTTP/API consistency check through nginx-01 succeeded against restored data.
```

Validated path:

```text
backup archive
-> PostgreSQL metadata restore via pg_restore
-> NFS file object restore via restic
-> ops-sample-service on app-01
-> nginx-01 HTTPS reverse proxy
-> restored work order/evidence consistency API
-> direct DB row and NFS SHA-256 checks
```

## Key evidence documents

```text
docs/04-evidence/restore-lab-recovery-validation-2026-07-13.md
```

## Supported claims

Supported:

```text
A preserved lab-full-ops backup archive was restored into an isolated restore-lab runtime, and the restored PostgreSQL metadata plus NFS file objects were verified through both direct DB/NFS checks and WEB/WAS HTTP/API consistency checks.
```

Not supported:

```text
- Production disaster recovery
- Automated failover
- RPO/RTO guarantee
- Multi-AZ high availability
- Continuous backup policy
- Managed database recovery
```

## Current operating direction

The next phase is service-linked operating scenario hardening, not documentation-only cleanup.

Current direction:

```text
1. Keep AWS EC2 as the runtime environment.
2. Do not introduce OCI for v1.0.
3. Keep ops-sample-service as the operated workload, but extend it when required by a concrete operating scenario.
4. Prefer scenarios where service behavior exposes WEB/WAS/DB/Storage operating problems.
5. Introduce new open-source tools only when they support diagnosis, comparison, or recovery evidence for a concrete scenario.
6. Update documentation after runtime scope changes, but avoid making documentation work the project center before v1.0 scope is closed.
```

## Next work before v1.0 closure

Next work is limited to the scenario-hardening path below:

```text
M1. Reframe roadmap/current-state language so the project is not read as documentation-only or tool-freeze work.
M2. Extend ops-sample-service as an operations workload where needed.
M3. Implement and validate a WAS thread pool / HikariCP / PostgreSQL connection pressure scenario.
M4. Runtime-validate release metadata and deployment rollback behavior against real service smoke checks.
M5. Finalize README, evidence index, incident reports, and interview Q&A after runtime evidence is complete.
```

## Guardrails

Allowed:

```text
- Service changes that make WEB/WAS/DB/Storage operating scenarios more realistic
- HikariCP, request handling, DB hold, query pressure, release metadata, and rollback behavior when tied to evidence
- Additional observability/exporter/logging tools when they are required by a concrete incident diagnosis path
```

Not allowed for v1.0:

```text
- OCI migration or dual-cloud rewrite
- OpenKoda adoption as the main workload
- Kubernetes/EKS/GitOps migration
- AWS managed architecture redesign around ALB/RDS
- Dashboard-first Grafana work
- Tool-first Loki/Alertmanager expansion
- Application feature development that is not tied to an operating scenario
- Documentation-only closure before the remaining service-linked runtime scenarios are evaluated
```
