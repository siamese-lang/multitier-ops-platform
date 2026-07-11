# lab-full-ops storage, backup, and observability expansion design

## Status

Proposed design for Phase 2.

Related issue: #80

This document is a design artifact. It does not claim that `lab-full-ops` has already been implemented or validated.

## Purpose

`lab-full-min` already proved the minimum WEB/WAS/DB operating path:

```text
operator -> nginx-01:443 -> app-01/app-02:8080 -> db-primary-01:5432
```

`lab-full-ops` extends that baseline into a fuller VM-based operating environment with file storage, backup, observability, logging, and load generation tiers.

The goal remains fixed:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

This phase must strengthen the operations story. It must not become an OpenKoda installation task, Terraform showcase, Spring Boot sample app project, or Grafana dashboard project.

## Design boundary

The Phase 2 design is allowed to add operating surfaces only when they produce evidence for one of the following:

```text
storage consistency
backup creation
restore verification
failure diagnosis
log/metric based root-cause narrowing
repeatable operating procedure
```

## Baseline inherited from lab-full-min

`lab-full-ops` starts from the completed `lab-full-min` tier separation.

| Tier | Nodes | Existing responsibility |
|---|---|---|
| Bastion | `bastion-01` | Controlled SSH entry point |
| WEB | `nginx-01` | HTTPS reverse proxy, upstream routing, access/error logs |
| WAS | `app-01`, `app-02` | Application runtime, health/readiness, DB-backed endpoint, request logs |
| DB | `db-primary-01` | PostgreSQL primary, DB state, connection and failure evidence |

Do not add more `lab-full-min` drills unless they directly unlock Phase 2 storage, backup, observability, or restore evidence.

## Target topology

```text
[Public Subnet]
- bastion-01
- nginx-01

[Private App Subnet]
- app-01
- app-02

[Private DB Subnet]
- db-primary-01

[Private Storage Subnet]
- nfs-01

[Private Ops Subnet]
- mon-01
- log-01
- backup-01
- loadgen-01
```

Optional later additions such as `nginx-02`, `app-03`, or `db-standby-01` are explicitly outside the first Phase 2 implementation pass unless a later issue ties them to a concrete incident or recovery scenario.

## Tier responsibilities

| Node | Tier | Responsibility | Evidence target |
|---|---|---|---|
| `nfs-01` | File storage | Shared file path for application-owned files | mount state, file presence, checksum, NFS service status |
| `backup-01` | Backup | PostgreSQL dump collection and file backup with restic | `pg_dump` output, restic snapshot, manifest, restore input |
| `mon-01` | Metrics | Prometheus, Grafana, and later Alertmanager | scrape targets, metric changes during incidents |
| `log-01` | Logs | Loki and Alloy-based log collection | queryable Nginx/app/PostgreSQL/systemd logs |
| `loadgen-01` | Load generation | Controlled traffic source for upload/download and failure drills | request count, status distribution, latency/error evidence |

## Network and security group direction

```text
operator CIDR -> bastion-01:22
operator CIDR -> nginx-01:443
bastion-01 -> private nodes:22
nginx-01 -> app-01/app-02:8080
app-01/app-02 -> db-primary-01:5432
app-01/app-02 -> nfs-01:2049
backup-01 -> db-primary-01:5432
backup-01 -> nfs-01:2049
loadgen-01 -> nginx-01:443
mon-01 -> scrape targets as required
log-01 <- log shipping from Alloy agents as required
```

Denied by design:

```text
operator -> app nodes direct HTTP/SSH
operator -> db-primary-01 direct PostgreSQL/SSH
operator -> nfs-01 direct NFS/SSH
nginx-01 -> db-primary-01 direct PostgreSQL
nginx-01 -> nfs-01 direct NFS
public internet -> private ops nodes
```

The exact Prometheus and Loki flows may be adjusted during implementation, but every allowed flow must map to a specific evidence target.

## File storage design

### Primary choice

Use `nfs-01` as the Phase 2 file storage tier.

Rationale:

- The roadmap already identifies `nfs-01` as the storage node.
- A separate storage node makes file failure visible to the operator.
- It creates a clear boundary between DB metadata and file objects.
- It supports restore validation because DB rows and file contents can be restored and checked separately.

### Filesystem abstraction

The application should use a mounted path, for example:

```text
/srv/ops-sample/files
```

The application must not rely on local ephemeral instance paths for the final Phase 2 storage scenario. If local filesystem behavior is used as a temporary harness during development, it must be documented as a pre-NFS step and not treated as final evidence.

## Workload file behavior

The workload may be extended minimally only to support operating evidence.

Acceptable endpoints or equivalent harness behavior:

```text
POST /api/files
GET  /api/files/{id}
GET  /api/files/{id}/download
GET  /readyz/storage
```

Expected behavior:

1. Store file metadata in PostgreSQL.
2. Store file bytes on the NFS-backed file path.
3. Store or calculate a checksum such as SHA-256.
4. Return a file identifier that can be used for later consistency checks.
5. Include request ID and file ID in application logs.

This is not product feature development. It is a controlled operating workload for validating DB/file consistency, storage failure, backup, and restore.

## DB metadata and file consistency model

A minimal metadata table or equivalent schema should represent:

```text
file_id
original_filename
storage_path
size_bytes
sha256
created_at
created_by_node
```

Consistency checks should detect at least these states:

| State | DB row | File object | Checksum | Interpretation |
|---|---:|---:|---:|---|
| Normal | exists | exists | matches | DB/file pair is recoverable |
| Missing file | exists | missing | unavailable | DB metadata points to lost file |
| Orphan file | missing | exists | calculable | file exists without DB metadata |
| Corrupt file | exists | exists | mismatch | file content changed after metadata write |
| Storage unavailable | maybe exists | inaccessible | unavailable | storage tier or mount failure |

Required command or API evidence:

```text
psql query for metadata row
ls/stat on storage path
sha256sum on file object
HTTP download response
application log with request_id and file_id
```

## First storage failure drill

The first Phase 2 incident should prove that the operator can distinguish application failure, DB failure, and storage failure.

Proposed sequence:

```text
1. Upload a file through nginx-01.
2. Verify metadata row in PostgreSQL.
3. Verify file object and checksum on nfs-01.
4. Stop NFS service or unmount the storage path from app nodes.
5. Run upload/download requests from loadgen-01.
6. Observe HTTP status and application errors.
7. Check app journal logs, Nginx logs, and nfs-01 systemd state.
8. Recover NFS service or remount storage.
9. Re-run download and checksum verification.
10. Document incident evidence and recovery gap.
```

Expected diagnosis narrative:

```text
WEB tier is reachable.
WAS process is alive.
DB readiness may remain healthy.
File-dependent operation fails.
Storage tier state or mount state explains the failure.
Recovery restores file operation without treating it as an app or DB outage.
```

## Backup design

### Backup boundaries

`lab-full-ops` backup must cover both data planes:

```text
PostgreSQL metadata/state -> pg_dump
NFS-backed file objects  -> restic snapshot
```

A DB-only backup is incomplete because file metadata can restore without file bytes. A file-only backup is incomplete because restored files may not have corresponding metadata.

### Backup node responsibility

`backup-01` should run or coordinate backup operations.

Minimum artifacts:

```text
pg_dump file
restic snapshot
backup manifest
checksum list
backup command output
```

The manifest should map the two backup sides:

```text
db_dump_name
restic_snapshot_id
file_count
metadata_row_count
sample_file_id
sample_file_sha256
backup_started_at
backup_finished_at
```

### Backup artifact persistence

For Phase 2, it is acceptable to prove backup creation on `backup-01`.

For Phase 3 `restore-lab`, backup artifacts must survive the destruction of the source lab. That can be handled by one of these explicitly documented approaches:

1. Copy backup artifacts to the operator workspace before destroy.
2. Preserve a dedicated backup volume outside the transient source lab.
3. Use an object-store artifact bucket only as a backup artifact store, while keeping the operating workload VM-based.

The selected approach must be stated in the restore issue before validation. The project must not use managed services to hide the WEB/WAS/DB/storage operating behavior.

## Restore-lab target flow

The Phase 2 design must prepare for this Phase 3 flow:

```text
1. Create lab-full data with DB rows and file objects.
2. Capture pg_dump and restic snapshot.
3. Record manifest and checksums.
4. Destroy or isolate the source lab.
5. Create restore-lab nodes.
6. Restore PostgreSQL dump.
7. Restore files from restic.
8. Verify metadata row count.
9. Verify sample file checksum.
10. Verify HTTP download through restored application path.
11. Document recovery time, missing data, and recovery gaps.
```

Restore validation is stronger than backup creation. Phase 3 must not be skipped after Phase 2 backup work.

## Observability minimum

Observability should be added to support incident analysis, not to create decorative dashboards.

### Metrics

Minimum Prometheus targets should eventually include:

| Target | Example evidence |
|---|---|
| all nodes | CPU, memory, disk, network, filesystem fullness |
| `nginx-01` | upstream errors, response status, latency if exported or derived |
| app nodes | JVM/process health, HTTP counts, latency, readiness, DB pool if exposed |
| `db-primary-01` | PostgreSQL up/down, connections, query activity where available |
| `nfs-01` | node and filesystem state, disk usage, NFS service state where available |
| `backup-01` | backup job success/failure and storage usage |

Implementation may start with node-level metrics and application health metrics, then expand. The evidence document must state exactly which metrics were available at the time of each incident.

### Logs

Minimum Loki log sources should include:

```text
/var/log/nginx/access.log
/var/log/nginx/error.log
application journal or application log file
PostgreSQL log
nfs-01 systemd/journal logs
backup command logs
```

Required correlation fields where available:

```text
request_id
upstream_addr
app_node
file_id
backup_id
incident_timestamp
```

`Alloy` may be used as the log collection agent. The exact agent layout can be decided during implementation, but the log evidence must support root-cause narrowing.

## Load generation design

`loadgen-01` exists to make incidents observable and repeatable.

Minimum load patterns:

```text
normal health/readiness checks
repeated DB-backed reads
file upload/download loop
concurrent file metadata + download checks
failure-window traffic during NFS or DB interruption
```

The initial implementation can use shell scripts and `curl`. Later issues may introduce `wrk`, `hey`, `k6`, or another tool only if it improves evidence quality.

## Evidence targets

Phase 2 work should eventually produce evidence documents for:

```text
lab-full-ops topology validation
file upload/download and DB metadata consistency
file storage failure and recovery drill
pg_dump + restic backup creation
restore-lab DB/file restore verification
Prometheus/Loki incident analysis
```

Minimum evidence fields for the first file storage drill:

```text
nginx_node=nginx-01
app_nodes=app-01,app-02
db_node=db-primary-01
storage_node=nfs-01
backup_node=backup-01
load_node=loadgen-01
file_metadata_created=true
file_object_created=true
checksum_recorded=true
storage_failure_injected=true
web_tier_reachable_during_storage_failure=true
app_process_alive_during_storage_failure=true
file_operation_failed_as_expected=true
storage_recovered=true
file_download_after_recovery=true
checksum_after_recovery_matches=true
```

## Implementation sequence after this design

Create fewer, larger roadmap-aligned issues rather than many small drifting issues.

Recommended next implementation sequence:

```text
1. [TF] lab-full-ops node/subnet/security-group skeleton
2. [ANSIBLE] lab-full-ops inventory/control path
3. [ANSIBLE] nfs-01 file storage baseline
4. [APP] minimal file metadata/upload/download operational endpoint or harness
5. [INCIDENT] file storage failure and recovery drill
6. [ANSIBLE] pg_dump + restic backup baseline
7. [VALIDATION] restore-lab DB/file restore verification
8. [OBS] Prometheus/Loki minimum observability
9. [INCIDENT] metric/log-based incident report
```

The immediate next issue after this design should be:

```text
[TF] lab-full-ops node/subnet/security-group skeleton
```

## Non-goals for the first Phase 2 implementation pass

- OpenKoda UI or feature development
- Large application feature expansion
- Kubernetes, EKS, or GitOps migration
- RDS, ALB, CloudWatch-only managed replacement of VM tiers
- Grafana dashboard-first work
- PostgreSQL standby promotion
- Nginx active/active entrypoint
- Performance tuning before storage/backup evidence exists
- More `lab-full-min` validations with the same already-proven pattern

## Definition of done for this design issue

This design issue is done when:

```text
A lab-full-ops design document exists.
The target storage, backup, observability, logging, and loadgen tiers are defined.
The file metadata/object consistency model is defined.
The backup and restore boundaries are defined.
The next implementation issue is clear.
The project remains centered on VM-based operations evidence.
```
