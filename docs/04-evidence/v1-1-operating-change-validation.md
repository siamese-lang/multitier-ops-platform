# v1.1 operating-change validation evidence

## Scope

This evidence note records the v1.1 runtime validation for the project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The validated scenario is:

```text
운영 변경 요청 기반 WAS DB 환경변수 오설정 감지, 롤백, 변경 요청 추적, 종료 판단 검증
```

This is not a production DR, HA, SLA, or zero-downtime validation. It is a controlled EC2 lab scenario that verifies whether an operator can detect a bad WAS configuration change, separate process health from dependency readiness, restore the previous environment file, verify post-change service behavior, represent the change as work-order data, and close the change only after DB, NFS, API, and log evidence are consistent.

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

Terraform validation profile during package installation and the initial v1.1 validation window:

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

NAT Gateway was needed only while the private EC2 nodes required outbound access for package installation. The follow-up change-request trace, closeout acceptance check, and evidence bundle collection used already-installed services and internal VPC communication.

## Validation flow

The v1.1 operating procedure now represents this closed flow:

```text
preflight
-> controlled bad DB environment change
-> dependency failure isolation
-> environment rollback
-> postflight business workflow validation
-> CHG-style work-order trace validation
-> change closeout acceptance validation
-> evidence bundle archive/fetch
```

The trace and closeout acceptance checks were validated against the already-running parked runtime after NAT removal. They do not install packages, create AWS resources, or require private subnet internet egress.

## Evidence archives

Local evidence archive directory:

```text
/mnt/c/Project/test/multitier-ops-platform/evidence/v1-1
```

Final evidence archives after adding CHG-style trace and closeout acceptance:

```text
lab-full-ops-v1-1-app-01-20260714T074339.tar.gz
lab-full-ops-v1-1-app-01-20260714T074339.tar.gz.sha256
lab-full-ops-v1-1-nginx-01-20260714T074339.tar.gz
lab-full-ops-v1-1-nginx-01-20260714T074339.tar.gz.sha256
```

Checksum result, verified locally with the fetched `.sha256` files:

```text
lab-full-ops-v1-1-app-01-20260714T074339.tar.gz: OK
lab-full-ops-v1-1-nginx-01-20260714T074339.tar.gz: OK
```

The final evidence bundle manifests recorded:

```text
scenario=preflight_bad_db_env_rollback_postflight_change_trace_closeout_acceptance
trace.scenario=chg_001_was_db_env_rollback_trace
closeout.scenario=chg_001_closeout_acceptance
validation=lab-full-ops-v1-1-evidence-bundle
```

## Key validation results

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

The closeout acceptance report recorded:

```text
validation=lab-full-ops-change-closeout-acceptance
scenario=chg_001_closeout_acceptance
work_order_id=7
api_consistent=true
api_checksum_matches=true
acceptance_status=validated
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

Nginx access logs and application journal logs included request-id evidence for both the change trace and closeout acceptance checks.

```text
lab-full-ops-change-trace-*
lab-full-ops-closeout-acceptance-*
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
10. DB rows, NFS object state, API consistency, and request-id logs were tied to the same trace.
11. Closeout acceptance confirmed that the change could be closed based on service readiness and evidence consistency.
12. Evidence was archived from both WEB and WAS tiers.
```

The important operational point is that `/healthz` and `/readyz` were intentionally interpreted differently:

```text
/healthz 200       -> process is alive
/readyz 503        -> business dependency is not ready
/dependencies 503  -> dependency-level failure details are observable
```

After rollback and closeout acceptance, the expected state changed to:

```text
/healthz 200
/readyz 200
/dependencies 200
work_order_status=DONE
acceptance_status=validated
```

This supports the portfolio claim that the lab validates operating changes through status, dependency, rollback, work-order history, evidence consistency, and closeout acceptance rather than by relying on a single service-active check.

## Runtime status after validation

The runtime was not destroyed immediately after this validation because follow-up low-risk checks were still useful. Instead, NAT Gateway was removed and the configured EC2 runtime was parked.

```text
NAT Gateway: removed
NAT EIP: released
Private subnet egress: disabled
Runtime: kept temporarily
```

This should be presented as a lab operating decision, not as production cost optimization.

## Cleanup decision

At this point, the v1.1 evidence path is complete. If no additional runtime validation is planned, destroy the lab runtime from Git Bash:

```bash
cd /c/Project/test/multitier-ops-platform/infra/terraform/envs/lab-full-ops
terraform destroy
```

If additional low-risk validation is still planned, keep the parked runtime but do not re-enable NAT unless package installation or external downloads are required.
