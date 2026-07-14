# v1.1 operating-change validation evidence

## Scope

This evidence note records the v1.1 runtime validation for the project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The validation scenario is:

```text
운영 변경 요청 기반 WAS DB 환경변수 오설정 감지, 롤백, 변경 요청 추적 검증
```

This is not a production DR, HA, or zero-downtime validation. It is a controlled EC2 lab scenario that verifies whether an operator can detect a bad WAS configuration change, separate process health from dependency readiness, restore the previous environment file, verify post-change service behavior, and link the change to work-order, DB, NFS, and log evidence.

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

Terraform validation profile during package installation and the main v1.1 validation window:

```text
app_02=false
backup_node=true
loadgen_node=false
logging_node=false
monitoring_node=false
storage_node=true
nat_gateway=true during package installation and initial validation
```

After evidence confirmation, the lab was moved to a parked runtime state:

```text
nat_gateway=false
NAT Gateway removed
NAT Elastic IP released
EC2 runtime kept for follow-up low-risk validation
```

NAT Gateway was needed only while the private EC2 nodes required outbound access for package installation. The follow-up change-request trace and evidence bundle collection used already-installed services and internal VPC communication.

## Validation flow

The initial orchestrated validation command was:

```bash
V1_1_EVIDENCE_DIR=/mnt/c/Project/test/multitier-ops-platform/evidence/v1-1 \
  scripts/run-v1-1-operating-change-validation.sh --execute
```

The v1.1 operating procedure now represents this flow:

```text
preflight
-> controlled bad DB environment change
-> dependency failure isolation
-> environment rollback
-> postflight business workflow validation
-> CHG-style work-order trace validation
-> evidence bundle archive/fetch
```

The CHG-style trace was validated separately against the already-running parked runtime after NAT removal.

## Evidence archives

Local evidence archive directory:

```text
/mnt/c/Project/test/multitier-ops-platform/evidence/v1-1
```

Final evidence archives after adding the CHG-style trace:

```text
lab-full-ops-v1-1-app-01-20260714T073113.tar.gz
lab-full-ops-v1-1-app-01-20260714T073113.tar.gz.sha256
lab-full-ops-v1-1-nginx-01-20260714T073113.tar.gz
lab-full-ops-v1-1-nginx-01-20260714T073113.tar.gz.sha256
```

Checksum result, verified locally with the fetched `.sha256` files:

```text
lab-full-ops-v1-1-app-01-20260714T073113.tar.gz: OK
lab-full-ops-v1-1-nginx-01-20260714T073113.tar.gz: OK
```

## Key validation results

The final evidence bundle manifests recorded:

```text
scenario=preflight_bad_db_env_rollback_postflight_change_trace
trace.scenario=chg_001_was_db_env_rollback_trace
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

The web-side postflight report recorded:

```text
validation=lab-full-ops-postflight-check
work_order_id=6
api_consistent=true
checksum_matches=true
```

The CHG-style change-request trace report recorded:

```text
validation=lab-full-ops-change-request-trace-validation
scenario=chg_001_was_db_env_rollback_trace
work_order_id=7
final_status=DONE
event_count=3
in_progress_event_found=true
done_event_found=true
evidence_id=2
api_consistent=true
file_exists=true
size_matches=true
checksum_matches=true
trace_status=validated
```

The direct DB row evidence showed the change request and status history:

```text
7  CHG-001 WAS DB connection setting rollback validation  DONE  HIGH  ops-change-manager  was-operator
CREATED         -> OPEN         actor=ops-change-manager
STATUS_CHANGED OPEN -> IN_PROGRESS actor=was-operator
STATUS_CHANGED IN_PROGRESS -> DONE actor=ops-change-manager
```

The direct NFS object check showed that the evidence object existed and matched API metadata:

```text
file=/srv/ops-sample/files/work-order-7/evidence-a82e0829-dc67-4069-9b97-f410f5014a1c.txt
size=215
sha256=ac941cc42e7521598652546074f5f6305e85f61c7a8269d29e03524d199aa3f7
```

Nginx access logs and application journal logs both included the same request-id prefix:

```text
lab-full-ops-change-trace-dependencies-before
lab-full-ops-change-trace-work-order-create
lab-full-ops-change-trace-work-order-in-progress
lab-full-ops-change-trace-evidence-create
lab-full-ops-change-trace-evidence-consistency
lab-full-ops-change-trace-work-order-done
lab-full-ops-change-trace-events
lab-full-ops-change-trace-audit-logs
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
8. The change was represented as CHG-001 work-order data.
9. The change request moved through OPEN -> IN_PROGRESS -> DONE.
10. DB rows, NFS object state, API consistency, and request-id logs were all tied to the same trace.
11. Evidence was archived from both WEB and WAS tiers.
```

The important operational point is that `/healthz` and `/readyz` were intentionally interpreted differently:

```text
/healthz 200       -> process is alive
/readyz 503        -> business dependency is not ready
/dependencies 503  -> dependency-level failure details are observable
```

This supports the portfolio claim that the lab validates changes through request state, dependency status, rollback, DB records, file evidence, checksums, and logs rather than relying on a single service-active check.

## Known follow-up fixed after the first v1.1 run

During the first local checksum verification, the fetched `.sha256` files referenced the remote absolute archive path under `/tmp/multitier-ops-platform`. The archive hashes were valid when checked by basename, but the checksum files were inconvenient for local `sha256sum -c` usage.

The evidence bundle playbook was updated so later `.sha256` files record the archive basename instead of the remote absolute path. The final 20260714T073113 archives were verified directly with `sha256sum -c`.

## Current cleanup state

The lab runtime was not destroyed after v1.1 because additional low-risk validation was still useful. Instead, NAT was removed and the configured EC2 runtime was kept.

Current intended parked state:

```text
EC2 runtime: kept
NAT Gateway: removed
NAT Elastic IP: released
private subnet default egress: disabled
Ansible ping: successful
preflight check: successful
```

Destroy the lab from Git Bash when no further runtime validation is planned:

```bash
cd /c/Project/test/multitier-ops-platform/infra/terraform/envs/lab-full-ops
terraform destroy
```
