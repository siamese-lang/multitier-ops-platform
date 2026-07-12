# AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증

이 프로젝트는 AWS EC2를 VM 환경으로 사용해 WEB/WAS/DB/파일저장소/백업/관측성 계층을 분리 구성하고, 운영 중 발생할 수 있는 장애·복구 상황을 로그·지표·명령 결과로 검증한 운영 포트폴리오입니다.

이 저장소의 주제는 OpenKoda 설치가 아닙니다. Terraform showcase도 아니고, AWS 관리형 아키텍처 자체도 아닙니다. 핵심은 **VM 기반 다계층 운영환경을 직접 구성하고, 장애 상황에서 어느 계층을 확인해야 하는지 evidence로 설명하는 것**입니다.

## 한 줄 정의

```text
EC2 VM 기반 WEB/WAS/DB/파일저장소 운영환경을 구성하고, 장애·복구 문제를 로그와 지표로 분석하는 프로젝트
```

## 포트폴리오 메시지

이 프로젝트가 보여주려는 역량은 다음입니다.

```text
운영 대상 시스템을 계층별로 배치하고,
설정 기준을 자동화하며,
장애를 재현하고,
로그·지표·명령 결과로 원인을 좁히고,
백업과 복구 절차를 실제 runtime evidence로 검증할 수 있는 운영 역량
```

채용담당자와 실무담당자에게 전달하려는 메시지는 다음입니다.

```text
실제 상용 운영 경험은 제한적이지만,
WEB/WAS/DB/Storage/Backup/Observability 구조를 나누어 이해하고,
장애 상황에서 확인해야 할 지점과 복구 절차를 근거 기반으로 설명할 수 있다.
```

## 완료 상태

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

Phase 4는 여기서 기능 확장을 멈춥니다. 이 프로젝트를 Grafana dashboard, Alertmanager, Loki, Kubernetes, HA 구성 실습으로 확장하지 않습니다. 이후 작업은 포트폴리오 설명력, evidence 연결성, 문서 구조 정리에 집중합니다.

## 검증한 운영 시나리오

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
   - project identity, completed scope, safe claims
2. docs/00-project/portfolio-summary.md
   - recruiter/interviewer-facing summary
3. docs/04-evidence/evidence-index.md
   - claim-to-evidence map
4. docs/00-project/interview-explanation-notes.md
   - 30-second/2-minute explanation and interview Q&A
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
```

새 대화에서 이어갈 때는 아래 문서를 먼저 읽습니다.

```text
docs/00-project/project-scope.md
docs/00-project/roadmap.md
docs/00-project/workload-strategy.md
docs/00-project/next-chat-handoff.md
docs/00-project/portfolio-summary.md
docs/00-project/interview-explanation-notes.md
docs/04-evidence/evidence-index.md
```

## 기술별 역할

| 요소 | 이 프로젝트에서의 역할 | 주의점 |
|---|---|---|
| AWS EC2 | VM 기반 운영환경 제공 | 관리형 서비스 중심 프로젝트가 아님 |
| Terraform | 실험 환경 생성·삭제 자동화 | 프로젝트 주제가 아니라 supporting tool |
| Ansible | 서버 설정과 운영 절차 재현 자동화 | role 자체보다 운영 설정 일관성이 중요 |
| Nginx | WEB/reverse proxy 계층 | upstream, timeout, access/error log 분석 대상 |
| Spring Boot/Tomcat | WAS 계층 | readiness, request log, artifact/version 검증 대상 |
| PostgreSQL | DB 계층 | connection, metadata, backup, restore, service-state 분석 대상 |
| NFS/filesystem | 파일 저장소 계층 | DB metadata와 file object consistency 검증 대상 |
| pg_dump/restic | 백업·복구 검증 도구 | backup creation과 restore validation을 구분 |
| node_exporter/Prometheus | 장애 분석용 지표 evidence | dashboard 자체가 목적이 아님 |
| OpenKoda | 초기 workload 후보 및 Phase 0 smoke-test 맥락 | 프로젝트 주제가 아님 |
| ops-sample-service | controlled workload | 최종 서비스 주제가 아니라 운영 시나리오 재현 도구 |

## 현재 주장할 수 있는 것

```text
EC2 기반 WEB/WAS/DB/Storage/Backup 계층을 분리 구성했다.
정상 경로, 장애, 백업 artifact 생성, 별도 restore-lab 복구를 evidence로 검증했다.
DB metadata와 NFS file object consistency를 검증했다.
로그·서비스 상태·request path를 통해 장애 원인을 계층별로 좁혔다.
Prometheus metrics로 DB host reachability와 PostgreSQL service failure를 구분했다.
Prometheus rule evaluation으로 PostgreSQL service inactivity를 감지했다.
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
```

## Runtime validation policy

AWS runtime validation은 필요할 때만 수행합니다. 작은 PR마다 Terraform apply/destroy를 반복하지 않습니다.

```text
문서/Ansible syntax/follow-up 수정
-> static check 중심

새 restore/observability/incident scenario
-> 명시적 runtime validation window
-> evidence 수집
-> destroy once
```

NAT Gateway를 켠 검증 창은 비용 리스크가 있으므로 evidence 수집 후 즉시 destroy합니다. 현재 Phase 4까지의 핵심 운영 evidence는 완료되었으므로, 이후 작업은 기본적으로 AWS runtime 없이 문서 정리와 포트폴리오 설명 구조 개선으로 진행합니다.

## 현재 다음 작업

```text
No new AWS runtime by default.
No more observability feature expansion by default.
Focus on final portfolio readability, evidence links, and interview explanation polish.
```
