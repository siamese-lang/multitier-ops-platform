# AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증

이 프로젝트는 AWS EC2를 VM 환경으로 사용해 WEB/WAS/DB/파일저장소/백업/관측성 계층을 분리 구성하고, 운영 중 발생할 수 있는 장애·복구 상황을 로그·지표·명령 결과로 검증하는 운영 포트폴리오입니다.

운영 대상 서비스는 `ops-sample-service`입니다. 현재 이 서비스는 단순 샘플 API가 아니라 **운영 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스**로 보강되었습니다. 작업 요청 목록·상세·등록·상태 변경, 증빙 파일 업로드·다운로드, 상태 이력, 감사 로그, DB/file consistency, WEB/WAS failure-lab 경로를 포함합니다.

이 저장소의 주제는 OpenKoda 설치가 아닙니다. Terraform showcase도 아니고, AWS 관리형 아키텍처 자체도 아닙니다. 핵심은 **VM 기반 WEB/WAS/DB/Storage/Backup/Observability 운영환경을 직접 구성하고, 서비스 장애 상황에서 어느 계층을 확인해야 하는지 evidence로 설명하는 것**입니다.

## 한 줄 정의

```text
작업 요청·증빙 파일 관리형 경량 웹 서비스를 EC2 VM 기반 WEB/WAS/DB/파일저장소/백업/관측성 계층 위에 구성하고, 장애·복구 문제를 로그와 지표로 분석하는 프로젝트
```

## 운영 대상 서비스

`ops-sample-service`는 다음 한 문장으로 설명합니다.

```text
운영 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스
```

현재 구현된 서비스 기능:

```text
1. 작업 요청 목록/상세 화면
2. 작업 요청 등록 화면
3. 상태 변경과 조치 메모
4. 상태 이력과 감사 로그
5. 증빙 파일 업로드/다운로드
6. PostgreSQL 파일 metadata 저장
7. NFS/file storage object 저장
8. DB metadata와 file object consistency 확인
9. /healthz와 /readyz 분리
10. request ID 기반 로그 추적
11. WEB/WAS failure-lab: sleep, DB sleep, file storage check, upload-limit inspection
```

면접에서 이 서비스는 아래처럼 설명합니다.

```text
운영 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스를 운영 대상으로 구성했습니다. 사용자는 작업 요청을 등록하고, 운영자는 상태를 변경하며, 조치 메모와 증빙 파일을 남길 수 있습니다. PostgreSQL에는 작업 요청과 파일 메타데이터를 저장하고, NFS에는 실제 첨부파일을 저장하도록 분리했습니다.
```

주의할 점:

```text
이 서비스는 production service, commercial ITSM, full asset management system, groupware/ERP가 아닙니다.
WEB/WAS 운영 시나리오를 설명 가능하게 만들기 위한 경량 업무 서비스입니다.
```

## 포트폴리오 메시지

이 프로젝트가 보여주려는 역량은 다음입니다.

```text
운영 대상 서비스를 계층별 인프라 위에 배치하고,
설정 기준을 자동화하며,
장애를 재현하고,
로그·지표·명령 결과로 원인을 좁히고,
백업과 복구 절차를 실제 runtime evidence로 검증할 수 있는 운영 역량
```

채용담당자와 실무담당자에게 전달하려는 메시지는 다음입니다.

```text
실제 상용 운영 경험은 제한적이지만,
WEB/WAS/DB/Storage/Backup/Observability 구조를 나누어 이해하고,
서비스 장애 상황에서 확인해야 할 지점과 복구 절차를 근거 기반으로 설명할 수 있다.
```

## 현재 완료된 운영 evidence

```text
Phase 0. lab-runtime smoke test: completed
Phase 1. lab-full-min WEB/WAS/DB minimum environment: completed
Phase 2A. lab-full-ops storage validation: completed
Phase 2B. backup artifact creation: completed as backup-artifact evidence
Phase 3. restore-lab DB/file/API recovery validation: completed
Phase 4A. logs/service/request-path observability evidence: completed
Phase 4B. node_exporter + Prometheus scrape metrics evidence: completed
Phase 4C. metric-based DB service incident diagnosis: completed
Phase 4D. Prometheus DB service alert-rule evaluation evidence: completed
```

## 현재 완료된 서비스 구현 baseline

```text
Phase 5A. work-order domain/schema: implemented
Phase 5B. server-rendered work-order web workflow: implemented
Phase 5C. evidence upload/download workflow: implemented
Phase 5D. WEB/WAS failure-lab endpoints: implemented
```

Phase 5 구현은 repository code/static 수준에서 완료되었습니다. 다만 아래 항목은 아직 **새 runtime evidence로 갱신해야 하는 범위**입니다.

```text
작업 요청 웹 화면 smoke test
작업 요청 생성/상태 변경/조치 메모 runtime 검증
증빙 파일 업로드/다운로드 runtime 검증
request ID 기반 WEB/WAS 로그 추적
Nginx timeout, WAS slow request, DB slow path 검증
보강된 서비스 모델 기준 backup/restore evidence refresh
```

Phase 4 이후에는 Grafana dashboard, Alertmanager, Loki, Kubernetes, HA 구성 실습으로 확장하지 않습니다. 다음 작업은 관측성 기능 추가가 아니라, 보강된 서비스 기준의 validation playbook과 runtime evidence 갱신입니다.

## 검증한 운영 시나리오

이미 검증한 runtime evidence:

```text
1. WEB/WAS/DB 정상 경로 검증
2. app-01 장애 시 Nginx upstream 우회 검증
3. app-01/app-02 rolling restart 중 서비스 연속성 검증
4. DB-backed 동시 요청 관측
5. PostgreSQL 장애와 복구 검증
6. NFS mount 및 write probe 검증
7. PostgreSQL metadata와 NFS file object consistency 검증
8. backup-01 기반 pg_dump/restic backup artifact 생성 검증
9. 별도 restore-lab에서 DB/file/API 복구 검증
10. 로그·서비스 상태·request path 기반 장애 분석 evidence 수집
11. Prometheus/node_exporter 기반 DB host reachability와 DB service failure 구분
12. Prometheus alert rule evaluation으로 PostgreSQL service inactivity 감지 검증
```

서비스 보강 이후 추가 검증해야 하는 runtime evidence:

```text
13. 작업 요청 웹 화면 smoke test
14. 작업 요청 생성/상태 변경/조치 메모 검증
15. 증빙 파일 업로드/다운로드 검증
16. request ID 기반 WEB/WAS 로그 추적
17. Nginx timeout, WAS slow request, DB slow query 등 WEB/WAS 운영 문제 검증
18. 보강된 서비스 모델 기준 restore-lab 복구 검증 갱신
```

## 대표 토폴로지

### lab-full-min

```text
operator -> nginx-01:443 -> app-01/app-02:8080 -> db-primary-01:5432
```

### lab-full-ops

```text
operator -> nginx-01:443 -> app-01:8080 -> db-primary-01:5432
                                 |
                                 -> nfs-01:/srv/ops-sample/files

backup-01 -> db-primary-01:5432
backup-01 -> nfs-01:/srv/ops-sample/files
mon-01    -> nginx/app/db/nfs/backup node_exporter:9100
```

### restore-lab

```text
preserved backup artifact
-> backup-01
-> pg_restore to db-primary-01
-> restic restore to nfs-01
-> app-01 reads restored DB metadata and NFS file object
-> nginx-01 reverse proxy verifies HTTP/API consistency
```

## 포트폴리오 리뷰 순서

저장소를 처음 보는 사람은 아래 순서로 보면 됩니다.

```text
1. README.md
   - project identity, completed evidence, current service implementation/validation boundary
2. apps/ops-sample-service/README.md
   - operated service behavior and endpoints
3. apps/ops-sample-service/FAILURE_LAB.md
   - WEB/WAS failure-lab scenarios
4. docs/00-project/portfolio-summary.md
   - recruiter/interviewer-facing summary
5. docs/04-evidence/evidence-index.md
   - claim-to-evidence map
6. docs/00-project/interview-explanation-notes.md
   - 30-second/2-minute explanation and interview Q&A
7. docs/00-project/submission-description-notes.md
   - application-form and portfolio URL wording
```

## 핵심 evidence 문서

전체 evidence map은 아래 문서를 기준으로 봅니다.

```text
docs/04-evidence/evidence-index.md
```

대표 evidence 문서:

```text
docs/04-evidence/lab-full-min-web-was-db-integrated-validation.md
docs/04-evidence/lab-full-min-continuous-operations-validation.md
docs/04-evidence/lab-full-ops-storage-validation-2026-07-12.md
docs/04-evidence/lab-full-ops-backup-validation-2026-07-12.md
docs/04-evidence/restore-lab-recovery-validation-2026-07-12.md
docs/04-evidence/observability-baseline-validation-2026-07-12.md
docs/04-evidence/observability-metrics-validation-2026-07-12.md
docs/04-evidence/observability-alert-validation-2026-07-12.md
```

## 포트폴리오 요약 및 면접 설명 문서

채용담당자나 실무 면접관에게 보여줄 요약은 아래 문서를 기준으로 합니다.

```text
docs/00-project/portfolio-summary.md
docs/00-project/interview-explanation-notes.md
docs/00-project/submission-description-notes.md
```

새 대화에서 이어갈 때는 아래 문서를 먼저 읽습니다.

```text
docs/00-project/project-scope.md
docs/00-project/roadmap.md
docs/00-project/workload-strategy.md
docs/00-project/ops-sample-service-completion-scope.md
docs/00-project/next-chat-handoff.md
docs/00-project/portfolio-summary.md
docs/00-project/interview-explanation-notes.md
docs/00-project/submission-description-notes.md
docs/04-evidence/evidence-index.md
```

## 기술별 역할

| 요소 | 이 프로젝트에서의 역할 | 주의점 |
|---|---|---|
| AWS EC2 | VM 기반 운영환경 제공 | 관리형 서비스 중심 프로젝트가 아님 |
| Terraform | 실험 환경 생성·삭제 자동화 | 프로젝트 주제가 아니라 supporting tool |
| Ansible | 서버 설정과 운영 절차 재현 자동화 | role 자체보다 운영 설정 일관성이 중요 |
| Nginx | WEB/reverse proxy 계층 | upstream, timeout, access/error log 분석 대상 |
| Spring Boot/Tomcat | WAS 계층, 경량 업무 서비스 실행 | Spring Boot 기능 개발 포트폴리오가 아님 |
| PostgreSQL | 작업 요청·이력·파일 metadata 저장 | connection, metadata, backup, restore, service-state 분석 대상 |
| NFS/filesystem | 증빙 파일 object 저장 | DB metadata와 file object consistency 검증 대상 |
| pg_dump/restic | 백업·복구 검증 도구 | backup creation과 restore validation을 구분 |
| node_exporter/Prometheus | 장애 분석용 지표 evidence | dashboard 자체가 목적이 아님 |
| OpenKoda | 초기 workload 후보 및 Phase 0 smoke-test 맥락 | 프로젝트 주제가 아님 |
| ops-sample-service | 작업 요청·증빙 파일 관리형 경량 웹 업무 서비스 | 구현 baseline은 추가됨, runtime evidence 갱신은 남아 있음 |

## 현재 주장할 수 있는 것

Runtime evidence로 주장할 수 있는 것:

```text
EC2 기반 WEB/WAS/DB/Storage/Backup 계층을 분리 구성했다.
정상 경로, 장애, 백업 artifact 생성, 별도 restore-lab 복구를 evidence로 검증했다.
DB metadata와 NFS file object consistency를 검증했다.
로그·서비스 상태·request path를 통해 장애 원인을 계층별로 좁혔다.
Prometheus metrics로 DB host reachability와 PostgreSQL service failure를 구분했다.
Prometheus rule evaluation으로 PostgreSQL service inactivity를 감지했다.
```

Repository implementation 기준으로 주장할 수 있는 것:

```text
ops-sample-service는 작업 요청·증빙 파일 관리형 경량 웹 업무 서비스로 보강되었다.
웹 화면 기반 작업 요청 등록/상세/상태 변경 흐름이 구현되어 있다.
증빙 파일 업로드/다운로드 흐름이 구현되어 있다.
WEB/WAS failure-lab endpoint가 구현되어 있다.
```

아직 runtime evidence 없이 주장하지 않는 것:

```text
보강된 웹 서비스 흐름을 AWS runtime에서 검증 완료했다.
증빙 파일 업로드/다운로드 전체 흐름을 Nginx/WAS/NFS/PostgreSQL 계층에서 evidence로 검증 완료했다.
WEB/WAS timeout/thread/slow query 문제를 서비스 기능과 연결해 검증 완료했다.
보강된 서비스 모델 기준 restore-lab 복구를 갱신 완료했다.
```

## 주장하지 않는 것

```text
production-grade monitoring maturity
Grafana dashboard readiness
Alertmanager notification maturity
paging or on-call workflow
PostgreSQL HA
automatic failover
SLO/SLA compliance
Kubernetes/EKS/GitOps 운영 경험
AWS managed architecture 운영 경험
production service 운영 경험
commercial ITSM 구현 경험
```

## Runtime validation policy

AWS runtime validation은 필요할 때만 수행합니다. 작은 PR마다 Terraform apply/destroy를 반복하지 않습니다.

```text
문서/Ansible syntax/follow-up 수정
-> static check 중심

서비스 기능 구현 PR
-> local/static validation 중심

보강된 서비스 기준 restore/observability/incident scenario
-> 명시적 runtime validation window
-> evidence 수집
-> destroy once
```

NAT Gateway를 켠 검증 창은 비용 리스크가 있으므로 evidence 수집 후 즉시 destroy합니다. 현재 Phase 4까지의 핵심 운영 evidence는 완료되었지만, 서비스 실체성 보강 후 일부 evidence는 갱신해야 합니다.

## 현재 다음 작업

```text
No new AWS runtime by default.
No more observability feature expansion by default.
Focus on enhanced-service validation: Ansible validation prep -> one planned AWS runtime window -> evidence refresh -> destroy once.
```
