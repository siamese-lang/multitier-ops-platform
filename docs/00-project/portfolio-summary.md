# Portfolio summary

## Project title

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

## One-line summary

```text
작업 요청·증빙 파일 관리형 경량 웹 서비스를 EC2 VM 기반 WEB/WAS/DB/Storage/Backup/Observability 계층 위에 구성하고, 장애·성능·복구 상황을 HTTP 응답·로그·systemd 상태·DB row·pg_stat_activity·파일 checksum으로 검증한 운영 포트폴리오
```

## What this project is

This project is an operations portfolio, not a feature-development showcase.

It demonstrates that a small but realistic operated workload can be deployed on separated VM-style tiers, and that failures can be diagnosed by checking the right layer with evidence.

```text
운영 대상 서비스
-> Nginx WEB tier
-> Spring Boot embedded Tomcat WAS process
-> HikariCP DB connection pool
-> PostgreSQL metadata DB
-> NFS file storage
-> pg_dump/restic backup and restore-lab validation
-> node_exporter/Prometheus observability evidence
```

## Operated service

The operated workload is `ops-sample-service`.

Use this description:

```text
운영 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스
```

What the service does:

```text
- 사용자는 작업 요청을 등록한다.
- 운영자는 작업 요청의 상태를 변경하고 조치 메모를 남긴다.
- 증빙 파일을 업로드하고 다운로드할 수 있다.
- PostgreSQL은 작업 요청, 상태 이력, 감사 로그, 파일 metadata를 저장한다.
- NFS/file storage는 실제 증빙 파일 object를 저장한다.
- Consistency API는 DB metadata와 file object의 크기·SHA-256 일치를 확인한다.
- Failure-lab endpoints는 latency, DB dependency, DB connection pool, file storage 상태를 관찰하게 한다.
```

Boundary:

```text
This is a lightweight operations service.
It is not a production service, commercial ITSM, ERP, groupware, or full asset-management system.
```

## Runtime topology

```text
operator -> nginx-01:443 -> app-01 embedded Tomcat -> HikariCP -> db-primary-01:5432
                                 |
                                 -> nfs-01:/srv/ops-sample/files

backup-01 -> db-primary-01:5432
backup-01 -> nfs-01:/srv/ops-sample/files
mon-01    -> node_exporter on WEB/WAS/DB/Storage/Backup nodes
```

## Final validation state

2026-07-13 기준 AWS runtime validation은 완료했고, evidence 수집 후 lab 리소스는 삭제했습니다.

```text
lab-full-ops AWS runtime validation: completed
connection pressure validation: completed
bad deployment rollback validation: completed
NFS mount failure/recovery validation: completed
Nginx config rollback validation: completed
AWS lab cleanup/destroy: completed
```

Final summary document:

```text
docs/04-evidence/final-runtime-validation-2026-07-13.md
```

Evidence map:

```text
docs/04-evidence/evidence-index.md
```

## Representative validation scenarios

Use these five scenarios as the main interview and portfolio storyline.

| Scenario | What was validated | Evidence boundary |
|---|---|---|
| Nginx bad config detection and rollback | Invalid Nginx config candidate was rejected by `nginx -t`; known-good config was restored and reloaded; proxied health/version/summary checks succeeded | Not production change-management maturity, traffic safety guarantee, blue/green, or canary deployment |
| Bad WAS artifact deployment and rollback | Bad jar deployment caused observable systemd/health failure; previous jar/env was restored; health, version, readiness, DB-backed summary, and systemd active state were verified | Not production release management, zero-downtime deployment, blue/green, or canary deployment |
| Tomcat request-thread pressure vs HikariCP pool pressure | WAS request-thread pressure caused delayed but successful DB-backed response; HikariCP pool pressure caused DB-backed API failure while PostgreSQL remained active | Not production load testing, capacity sizing, SLO/SLA validation, or autoscaling evidence |
| App-side NFS mount failure and recovery | DB-backed work-order creation remained available while file-storage-dependent evidence-file creation failed; remount restored DB metadata/NFS object/size/SHA-256 consistency | Not storage HA, automatic failover, NFS performance tuning, or chaos engineering |
| Backup artifact vs restore-lab recovery proof | pg_dump/restic backup artifacts were created, then recovery was proven separately in restore-lab through DB/file/API consistency and checksum checks | Not production DR, RPO/RTO guarantee, continuous backup policy, or managed database recovery |

## What this project proves

The project supports these claims:

```text
- EC2 VM 기반으로 WEB/WAS/DB/Storage/Backup/Observability 계층을 분리 구성했다.
- 운영 대상 workload를 단순 hello world가 아니라 작업 요청·증빙 파일 관리형 서비스로 만들었다.
- 정상 업무 흐름과 장애·복구 흐름을 HTTP 응답, 로그, systemd 상태, DB row, pg_stat_activity, file object, checksum으로 검증했다.
- WAS request-thread pressure와 WAS-side DB connection-pool exhaustion을 구분했다.
- 잘못된 WAS artifact 배포와 rollback을 systemd, health, version, readiness, DB-backed summary로 검증했다.
- app-side NFS mount 장애가 DB-backed path와 file-storage-dependent path에 다르게 영향을 주는 것을 확인했다.
- Nginx config candidate를 reload 전에 검증하고 known-good config 복구 후 proxy path 정상화를 확인했다.
- backup artifact 생성과 restore-lab recovery proof를 구분했다.
- 검증 후 AWS lab 리소스를 삭제했다.
```

## Safe interview framing

Use this concise explanation:

```text
EC2를 VM 환경처럼 사용해 WEB/WAS/DB/Storage/Backup/Observability 계층을 나누고, 작업 요청·증빙 파일 관리형 경량 서비스를 운영 대상으로 구성했습니다. 이후 Nginx 설정 오류, WAS artifact 배포 실패, Tomcat/HikariCP pressure, NFS mount 장애, 백업·복구를 각각 검증하면서 HTTP 상태, 로그, systemd 상태, DB row, pg_stat_activity, 파일 checksum으로 원인과 복구 여부를 확인했습니다. 실제 상용 운영 경험이라고 주장하지는 않고, 운영 직무에서 필요한 계층별 확인 절차와 근거 기반 판단을 EC2 lab에서 정리한 포트폴리오로 설명합니다.
```

## Tools and their roles

| Tool | Role in this project | Not the project theme |
|---|---|---|
| Terraform | Create and destroy temporary AWS lab environments | Not a Terraform showcase |
| Ansible | Configure hosts and reproduce operating procedures | Not an Ansible role showcase |
| AWS EC2 | VM-style infrastructure substrate | Not an AWS managed architecture project |
| Nginx | WEB/reverse proxy tier; config test and rollback target | Not a web tuning-only project |
| Spring Boot/embedded Tomcat | Runs the lightweight operated service in the WAS tier; exposes request-thread pressure evidence | Not external Tomcat/WAR administration |
| HikariCP | Exposes WAS-side DB connection-pool pressure evidence | Not production capacity sizing |
| PostgreSQL | DB tier for work-order/event/audit/file metadata and pg_stat_activity checks | Not HA database engineering |
| NFS | File storage tier for evidence file objects; mount failure and recovery target | Not storage product evaluation |
| pg_dump/restic | Backup artifact creation and restore-lab recovery validation tooling | Not backup product comparison |
| node_exporter/Prometheus | Metrics evidence for diagnosis | Not a monitoring platform project |

## Claims that must not be made

```text
production operations experience
production incident response
production-grade monitoring maturity
production disaster recovery
production load testing
capacity sizing
SLO/SLA compliance
RPO/RTO guarantee
PostgreSQL HA
automatic failover
storage HA
blue-green/canary deployment
zero-downtime release guarantee
external Tomcat/WAR operation
Kubernetes/EKS/GitOps operation
AWS managed architecture operation
commercial ITSM implementation
```

## Where to read next

```text
1. docs/00-project/portfolio-review-guide.md
   - recommended review order and representative five-scenario sequence
2. docs/04-evidence/evidence-index.md
   - scenario-to-evidence and claim-to-evidence map
3. docs/04-evidence/final-runtime-validation-2026-07-13.md
   - final runtime validation and cleanup summary
4. docs/00-project/interview-incident-qna.md
   - spoken interview explanation layer
5. docs/00-project/submission-description-notes.md
   - application-form and portfolio URL wording
```

## Current hardening focus

Runtime validation is now closed by default.

Further work should focus on:

```text
- README, portfolio-summary, evidence-index, and interview Q&A alignment
- interview explanation notes
- application-form wording
- final repository polish
```

Further work should not focus on:

```text
- more Prometheus features
- Grafana dashboards
- Alertmanager routing
- Loki expansion
- unplanned AWS runtime windows
- unrelated architecture expansion
```
