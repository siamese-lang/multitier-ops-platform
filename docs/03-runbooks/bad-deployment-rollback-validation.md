# Bad deployment rollback validation

## Purpose

This runbook validates a controlled VM/systemd deployment failure and rollback path for `ops-sample-service`.

Fixed project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

This scenario is intentionally narrow. It is not CI/CD automation, Kubernetes, blue/green, canary, or production release management. It tests whether an operator can identify a bad WAS artifact deployment, preserve evidence, and restore the previous service artifact.

## What this scenario proves

Supported after runtime validation:

```text
A bad application artifact can be distinguished from DB, NFS, and Nginx failures by checking systemd state, health endpoint behavior, service logs, and rollback validation results.
```

Not supported:

```text
production deployment experience
zero-downtime deployment
blue/green deployment
canary deployment
enterprise release management
RPO/RTO guarantee
```

## Preconditions

Run this only after the normal app deployment baseline is already healthy.

Required state:

```text
ops-sample-service is deployed as a systemd service
/healthz returns 200
/version returns deployment metadata
/readyz returns 200 when DB is expected to be ready
/api/work-orders/summary works when DB is expected to be ready
current jar exists at ops_app_jar_dest
current env file exists at /etc/ops-sample-service/ops-sample-service.env
```

Do not run this before the service deployment baseline.

## Static validation

From WSL:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible

ansible-playbook \
  -i inventories/ci/hosts.yml \
  playbooks/lab-full-ops-bad-deployment-rollback-validation.yml \
  --syntax-check
```

This syntax check does not touch AWS. It only validates the playbook can be parsed.

## Runtime command

Run only during a planned AWS runtime validation window.

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-bad-deployment-rollback-validation.yml \
  -e 'ops_db_password=<strong-local-password>'
```

## Scenario flow

The playbook performs this sequence:

```text
1. Capture baseline service state and /version metadata.
2. Verify the current app jar exists.
3. Back up the current app jar.
4. Verify the current env file exists.
5. Back up the current env file.
6. Replace the service jar with an intentionally invalid jar file.
7. Mark APP_VERSION and deployment metadata as a bad candidate.
8. Restart ops-sample-service.
9. Confirm the bad artifact causes service startup or health failure.
10. Capture systemd status and journal excerpt.
11. Restore the previous jar.
12. Restore the previous env file.
13. Restart ops-sample-service.
14. Validate rollback health, /version, readiness, and DB-backed summary.
15. Write a report file.
```

## Expected evidence

Report path:

```text
/tmp/multitier-ops-platform/lab-full-ops-bad-deployment-rollback-<inventory_hostname>.txt
```

Important fields:

```text
baseline_service_state
baseline_version_rc
previous_jar_sha256
invalid_jar_sha256
bad_deployment_version
bad_deployment_id
bad_deploy_service_state
bad_deploy_health_rc
rollback_health_rc
rollback_version_rc
rollback_ready_rc
rollback_summary_rc
final_service_state
failure_layer
rollback_status
```

## Success criteria

The scenario is successful when:

```text
1. The intentionally invalid jar creates an observable WAS service failure.
2. systemctl or /healthz shows the bad deployment is not serving normally.
3. The previous jar and env file are restored.
4. /healthz succeeds after rollback.
5. /version succeeds after rollback.
6. /readyz succeeds when DB readiness is expected.
7. /api/work-orders/summary succeeds when DB readiness is expected.
8. final_service_state is active.
9. rollback_status is validated.
```

## Failure interpretation

If the bad deployment does not fail:

```text
Check whether systemd kept the previous process alive.
Check whether the playbook targeted the correct app node.
Check whether ExecStart is actually using ops_app_jar_dest.
```

If rollback fails:

```text
Check the jar backup path.
Check the env backup path.
Check journalctl -u ops-sample-service.
Check /version and /readyz separately.
Do not assume DB or NFS failure until service startup is confirmed.
```

## Interview explanation

Use this explanation only after runtime validation:

```text
배포 실패 시나리오는 실제 장애를 과장하지 않고, VM/systemd 기반 WAS 운영 관점에서 검증했습니다. 정상 artifact와 env 파일을 백업한 뒤 의도적으로 잘못된 jar를 배포해 서비스 기동 실패를 만들었습니다. 이때 systemctl, journalctl, /healthz를 통해 DB나 NFS가 아니라 WAS artifact startup 계층 문제라고 판단했습니다. 이후 이전 jar와 env 파일로 rollback하고 /healthz, /version, /readyz, DB-backed summary까지 확인해 rollback이 완료됐음을 검증했습니다.
```

## Runtime policy

Do not run this repeatedly.

Run it only once in a controlled AWS validation window, collect evidence, and destroy the runtime environment when no follow-up check is planned.
