# v1.1 operating-change validation evidence

## Scope

This evidence note records the v1.1 runtime validation for the project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The validation scenario is:

```text
운영 변경 요청 기반 WAS DB 환경변수 오설정 감지 및 롤백 검증
```

This is not a production DR, HA, or zero-downtime validation. It is a controlled EC2 lab scenario that verifies whether an operator can detect a bad WAS configuration change, separate process health from dependency readiness, restore the previous environment file, and collect evidence.

## Runtime context

Validation date:

```text
2026-07-14 Asia/Seoul
```

Validated environment:

```text
lab-full-ops
```

Validated nodes:

```text
nginx-01
app-01
db-primary-01
nfs-01
backup-01
```

Terraform validation profile used during the runtime window:

```text
app_02=false
backup_node=true
loadgen_node=false
logging_node=false
monitoring_node=false
storage_node=true
nat_gateway=true during package installation and validation window
```

NAT Gateway was enabled for the runtime window because the private EC2 nodes needed outbound access for package installation. It should be removed by destroying the lab after evidence confirmation.

## Validation flow

The orchestrated validation command was:

```bash
V1_1_EVIDENCE_DIR=/mnt/c/Project/test/multitier-ops-platform/evidence/v1-1 \
  scripts/run-v1-1-operating-change-validation.sh --execute
```

The orchestrator ran this flow:

```text
preflight
-> bad DB env
-> dependency failure isolation
-> rollback
-> postflight
-> evidence fetch
```

## Evidence archives

Local evidence archive directory:

```text
/mnt/c/Project/test/multitier-ops-platform/evidence/v1-1
```

Generated archives:

```text
lab-full-ops-v1-1-app-01-20260714T064348.tar.gz        12158 bytes
lab-full-ops-v1-1-app-01-20260714T064348.tar.gz.sha256 142 bytes
lab-full-ops-v1-1-nginx-01-20260714T064348.tar.gz      6936 bytes
lab-full-ops-v1-1-nginx-01-20260714T064348.tar.gz.sha256 144 bytes
```

Checksum result, verified locally with basename-normalized `.sha256` paths:

```text
lab-full-ops-v1-1-app-01-20260714T064348.tar.gz: OK
lab-full-ops-v1-1-nginx-01-20260714T064348.tar.gz: OK
```

## Key validation results

The evidence bundle manifests recorded:

```text
scenario=preflight_bad_db_env_rollback_postflight
validation=lab-full-ops-v1-1-evidence-bundle
```

The app-side bad DB environment rollback report recorded:

```text
validation=lab-full-ops-bad-db-env-rollback-validation
bad_health_code=200
bad_ready_code=503
bad_dependencies_code=503
bad_env_failure_observed=True
rollback_attempted=True
rollback_health_code=200
rollback_ready_code=200
rollback_dependencies_code=200
rollback_status=validated
```

The web-side evidence bundle included preflight and postflight reports:

```text
validation=lab-full-ops-preflight-check
validation=lab-full-ops-postflight-check
```

## Interpretation

The v1.1 scenario validated the intended operating-change behavior:

```text
1. The service process stayed reachable during the bad DB environment change.
2. Business readiness failed as expected.
3. The dependency endpoint returned a DB dependency failure.
4. Storage remained outside the failure scope.
5. The previous systemd environment file was restored.
6. The service recovered after rollback.
7. Postflight business workflow validation completed after rollback.
8. Evidence was archived from both WEB and WAS tiers.
```

The important operational point is that `/healthz` and `/readyz` were intentionally interpreted differently:

```text
/healthz 200  -> process is alive
/readyz 503   -> business dependency is not ready
/dependencies 503 -> dependency-level failure details are observable
```

This supports the portfolio claim that the lab validates changes through status, dependency, rollback, and evidence rather than by relying on a single service-active check.

## Known follow-up fixed after this run

During local checksum verification, the fetched `.sha256` files initially referenced the remote absolute archive path under `/tmp/multitier-ops-platform`. The archive hashes were valid when checked by basename, but the checksum files were inconvenient for local `sha256sum -c` usage.

The evidence bundle playbook was updated after this run so future `.sha256` files record the archive basename instead of the remote absolute path.

## Cleanup requirement

After confirming this evidence, destroy the AWS lab runtime to remove the NAT Gateway and EC2 resources.

Recommended cleanup location:

```bash
cd /c/Project/test/multitier-ops-platform/infra/terraform/envs/lab-full-ops
terraform destroy
```
