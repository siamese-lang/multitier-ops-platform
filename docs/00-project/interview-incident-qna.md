# Interview incident Q&A

Use this document to explain the project in infrastructure / WEB-WAS operations interviews.

Fixed project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

This project should be explained as a bounded operations portfolio, not as production operations experience.

## Source documents

```text
docs/04-evidence/final-runtime-validation-2026-07-13.md
docs/04-evidence/evidence-index.md
docs/04-evidence/connection-pressure-validation-2026-07-13.md
docs/05-incident-reports/enhanced-service-workflow-baseline-report.md
docs/05-incident-reports/upload-limit-incident-report.md
docs/05-incident-reports/latency-diagnosis-incident-report.md
docs/05-incident-reports/db-web-impact-incident-report.md
docs/05-incident-reports/restore-lab-recovery-incident-report.md
```

## Q1. 이 프로젝트에서 실제로 운영한 서비스는 무엇인가요?

```text
운영 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스를 운영 대상으로 구성했습니다. 사용자는 작업 요청을 등록하고, 운영자는 상태를 변경하며 조치 메모를 남길 수 있습니다. 증빙 파일은 웹 화면에서 업로드·다운로드할 수 있고, PostgreSQL에는 작업 요청·상태 이력·감사 로그·파일 메타데이터를 저장하며 NFS에는 실제 파일 object를 저장하도록 분리했습니다.
```

주의해서 말할 점:

```text
상용 ITSM이나 ERP를 만든 것은 아닙니다. WEB/WAS/DB/File 운영 문제를 설명하기 위한 경량 업무 서비스입니다.
```

## Q2. 이 프로젝트가 단순 클라우드 실습과 다른 점은 무엇인가요?

```text
단순히 EC2를 만들거나 Terraform으로 리소스를 띄운 것이 아니라, 그 위에 업무 흐름이 있는 서비스를 올리고 운영 시나리오를 검증했습니다. 작업 요청, 상태 변경, 증빙 파일 업로드, DB metadata, NFS file object, backup, restore-lab recovery까지 하나의 운영 흐름으로 연결했습니다.
```

핵심 포인트:

```text
인프라 생성 자체가 목적이 아니라, 운영 대상 workload와 장애·복구 evidence를 함께 구성했다는 점이 차이입니다.
```

## Q3. 정상 업무 흐름은 어떻게 검증했나요?

```text
Nginx를 통해 작업 요청 목록과 등록 화면에 접근하고, 작업 요청을 생성한 뒤 상태 변경과 조치 메모가 DB에 반영되는지 확인했습니다. 이후 증빙 파일을 업로드하고 다운로드하면서 DB metadata와 NFS file object가 일치하는지 확인했습니다. 이 흐름을 통해 단순 API smoke test가 아니라 실제 업무 요청 흐름이 WEB/WAS/DB/NFS 경로를 거친다는 점을 검증했습니다.
```

핵심 포인트:

```text
정상 업무 흐름 baseline이 있어야 장애 후 복구 여부도 판단할 수 있습니다. 그래서 health check만 보지 않고 업무 데이터, 파일 object, consistency endpoint까지 확인했습니다.
```

## Q4. 대표 운영 시나리오는 무엇인가요?

```text
대표 시나리오는 다섯 가지입니다. 첫째, Tomcat request-thread pressure와 HikariCP connection-pool pressure를 구분했습니다. 둘째, 잘못된 WAS artifact 배포를 systemd와 health check로 확인하고 이전 jar/env로 rollback했습니다. 셋째, app-side NFS mount 장애 중 DB-backed 업무 생성과 file-storage 의존 기능 실패를 분리했습니다. 넷째, 잘못된 Nginx config candidate를 nginx -t로 차단하고 원본 config를 복구했습니다. 다섯째, backup artifact 생성과 restore-lab 복구 검증을 구분했습니다.
```

면접에서 강조할 점:

```text
장애를 많이 만들었다는 점보다, 장애별로 먼저 봐야 할 계층과 근거를 분리했다는 점을 강조합니다.
```

## Q5. WAS thread pressure와 DB connection-pool pressure는 어떻게 구분했나요?

```text
Spring Boot embedded Tomcat의 request thread와 HikariCP DB connection pool을 작은 값으로 제한한 뒤 bounded lab에서 비교했습니다. WAS request-thread pressure에서는 DB-backed summary가 실패하지 않고 지연됐습니다. 반면 HikariCP connection-pool pressure에서는 PostgreSQL 자체는 active였지만 DB-backed summary API가 503으로 실패했습니다.
```

확인한 evidence:

```text
Nginx access log
app journald
HTTP status and timing
HikariCP active/idle connection state
PostgreSQL pg_stat_activity
```

면접에서 강조할 점:

```text
서버가 느리거나 실패한다는 증상을 하나로 보지 않고, WAS request 처리량 문제인지 WAS-side DB connection pool 고갈인지 나누어 보았습니다. 다만 이는 bounded lab evidence이며 production load test나 capacity sizing은 아닙니다.
```

## Q6. 잘못된 WAS 배포는 어떻게 rollback했나요?

```text
잘못된 jar artifact를 배포해 서비스와 health check 실패를 관찰했습니다. 이후 이전 jar와 systemd environment file을 복구하고 서비스를 재시작한 뒤 health, version, readiness, DB-backed summary, systemd active 상태를 순서대로 확인했습니다.
```

확인한 evidence:

```text
systemd service state
health check result
version endpoint result
readiness result
DB-backed summary result
rollback final status
```

면접에서 강조할 점:

```text
배포가 실패했을 때 단순히 프로세스만 다시 띄운 것이 아니라, 이전 artifact와 환경값을 복구한 뒤 업무 의존 경로까지 확인했습니다. 다만 blue/green, canary, zero-downtime deployment를 구현한 것은 아닙니다.
```

## Q7. NFS mount 장애는 어떻게 접근했나요?

```text
app-01에서 NFS mount를 의도적으로 해제해 app-side storage 장애를 만들었습니다. 이때 DB-backed work-order 생성은 201 Created로 성공했지만, evidence-file 생성은 storage-not-ready 상태로 503 실패했습니다. 이후 NFS를 remount하고 evidence-file 생성과 consistency endpoint를 다시 확인했습니다.
```

확인한 evidence:

```text
mountpoint state
file-storage readiness
HTTP status
DB metadata row
NFS file object existence
file size
SHA-256 checksum
application consistency endpoint
```

면접에서 강조할 점:

```text
같은 서비스 장애처럼 보여도 DB-backed 업무 경로와 file-storage 의존 경로는 다르게 실패할 수 있습니다. 이 프로젝트에서는 mount 상태, readiness, DB row, NFS object, checksum을 함께 확인해 복구 여부를 판단했습니다.
```

## Q8. Nginx 설정 오류는 어떻게 막고 복구했나요?

```text
잘못된 Nginx config candidate를 site file에 반영한 뒤 바로 reload하지 않고 nginx -t로 먼저 검증했습니다. nginx -t가 invalid directive를 차단했고, unsafe reload 전 running Nginx는 active 상태를 유지했습니다. 이후 원본 config를 복구하고 다시 nginx -t와 reload를 수행한 뒤 health, version, summary proxy path가 정상화됐는지 확인했습니다.
```

확인한 evidence:

```text
nginx -t failure
running Nginx active state
known-good config restore
nginx -t success after restore
nginx reload success
post-rollback proxied health/version/summary checks
```

면접에서 강조할 점:

```text
WEB tier 설정 변경은 reload 전에 문법 검증으로 차단해야 하며, 잘못된 후보 설정이 있을 때는 known-good config로 되돌린 뒤 실제 proxy path까지 확인해야 합니다.
```

## Q9. 파일 업로드 장애는 어떻게 접근했나요?

```text
업로드 실패를 단순히 애플리케이션 오류로 보지 않고 Nginx 제한, WAS multipart 처리, PostgreSQL metadata 저장, NFS file object 생성 여부로 나누어 확인했습니다. 업로드 요청의 HTTP status, Nginx log, app log, DB metadata row, NFS 파일 존재 여부, consistency check를 함께 보면서 어느 계층에서 실패했는지 분리했습니다.
```

면접에서 강조할 점:

```text
파일 업로드는 WEB/WAS/DB/NFS가 모두 관여하는 흐름이므로, HTTP 응답만 보고 판단하지 않고 metadata와 실제 파일 object를 함께 확인했습니다.
```

## Q10. 응답 지연은 어떻게 구분했나요?

```text
WAS sleep과 DB sleep 경로를 나누어 검증했습니다. WAS sleep은 애플리케이션 내부 처리 지연을 만들고, DB sleep은 DB 의존 경로의 지연을 만듭니다. 두 요청의 curl time_total, Nginx upstream response time, app request duration, DB 의존 endpoint 상태를 비교해 지연이 WAS 처리 문제인지 DB-backed path 문제인지 구분했습니다.
```

면접에서 강조할 점:

```text
서버가 느리다는 증상을 하나로 보지 않고 WEB/WAS/DB 요청 경로 중 어느 구간에서 시간이 증가하는지 확인했습니다.
```

## Q11. DB 장애가 웹 서비스에 미치는 영향은 어떻게 검증했나요?

```text
PostgreSQL 장애 상황에서 /healthz와 /readyz를 분리해 보았습니다. 프로세스 자체가 살아 있으면 /healthz는 정상일 수 있지만, DB 의존성이 실패하면 /readyz와 작업 요청 API 또는 화면은 실패할 수 있습니다. 이 차이를 통해 애플리케이션 프로세스 상태와 업무 처리 가능 상태를 구분했습니다.
```

면접에서 강조할 점:

```text
프로세스 health와 업무 readiness는 다릅니다. DB host가 살아 있어도 PostgreSQL service나 DB dependency가 실패하면 업무 요청은 실패할 수 있습니다.
```

## Q12. 백업과 복구는 어떻게 구분했나요?

```text
pg_dump와 restic snapshot을 생성한 것은 backup artifact 생성입니다. 이것만으로는 복구 가능성을 증명할 수 없다고 판단했습니다. 그래서 별도 restore-lab 환경에서 PostgreSQL metadata를 pg_restore로 복구하고, NFS file object를 restic으로 복구한 뒤, 앱이 복구된 DB metadata와 파일 object를 읽는지 HTTP/API consistency와 SHA-256 checksum으로 검증했습니다.
```

면접에서 강조할 점:

```text
백업 파일이 있다는 것과 복구가 검증됐다는 것은 다릅니다. 복구 검증은 별도 환경에서 DB row, file size, checksum, API consistency가 맞아야 주장할 수 있습니다.
```

## Q13. 이 프로젝트에서 가장 강한 운영 포인트는 무엇인가요?

```text
장애를 단순히 재현한 것이 아니라, 계층별로 확인 지점을 나누고 evidence로 판단한 점입니다. 예를 들어 connection pressure는 Tomcat thread와 HikariCP pool로 나누고, 업로드 문제는 WEB/WAS/DB/NFS로 나누며, DB 장애는 health와 readiness를 분리해서 판단했습니다. restore-lab에서는 백업과 복구를 구분해 API와 checksum까지 확인했습니다.
```

## Q14. 운영 경험이 없는데 이 프로젝트를 어떻게 설명해야 하나요?

```text
실제 상용 운영 경험이라고 말하지는 않습니다. 대신 운영 직무에서 필요한 사고방식과 확인 절차를 EC2 기반 lab에서 재현했습니다. 즉 WEB/WAS/DB/Storage/Backup/Observability 계층을 직접 나누고, 장애가 생겼을 때 어떤 로그와 상태, 지표, DB row, 파일 checksum을 봐야 하는지 검증한 포트폴리오라고 설명합니다.
```

## Q15. 면접에서 30초로 설명한다면 어떻게 말하나요?

```text
EC2 VM 기반으로 Nginx, Spring Boot embedded Tomcat, PostgreSQL, NFS, backup, Prometheus 계층을 구성하고, 운영 작업 요청과 증빙 파일을 관리하는 경량 서비스를 올렸습니다. 이후 Nginx 설정 오류, WAS 배포 실패, Tomcat/HikariCP pressure, NFS mount 장애, 백업·복구를 각각 재현하고 HTTP 상태, 로그, systemd 상태, DB row, pg_stat_activity, 파일 checksum으로 원인과 복구 여부를 확인했습니다. 상용 운영 경험은 아니지만 운영 상황에서 어떤 계층을 봐야 하는지 evidence로 정리한 포트폴리오입니다.
```

## Q16. 다음으로 보강한다면 무엇을 할 건가요?

```text
현재 v1.0 기준으로는 새 runtime을 여는 것보다 제출·면접용 정리를 우선하겠습니다. 이미 주요 장애·복구 시나리오는 검증했기 때문에, 추가 기능을 계속 늘리기보다는 대표 시나리오별 claim, evidence, boundary를 더 짧고 명확하게 정리하는 것이 포트폴리오 완성도에 더 도움이 된다고 봅니다.
```

## Claims to avoid in interviews

Do not say:

```text
운영 실무 경험이 있습니다.
프로덕션 장애 대응 경험이 있습니다.
프로덕션 수준 DR을 구현했습니다.
HA와 자동 failover를 구현했습니다.
RPO/RTO를 보장했습니다.
SLO/SLA를 검증했습니다.
상용 ITSM을 만들었습니다.
대규모 부하 테스트를 수행했습니다.
capacity sizing을 완료했습니다.
Kubernetes/GitOps 운영을 했습니다.
외부 Tomcat 서버를 설치·운영했습니다.
```

Use instead:

```text
상용 운영 경험은 아니지만, 운영 직무에서 필요한 계층별 확인 절차와 장애·복구 검증 방식을 EC2 기반 lab에서 evidence로 정리했습니다.
```

## Safe wording for embedded Tomcat

Use this wording:

```text
Spring Boot embedded Tomcat 기반 WAS process를 systemd 서비스로 운영하고, Nginx reverse proxy, readiness, 로그, 배포, rollback, connection pool, DB dependency를 검증했습니다.
```

Avoid this wording:

```text
외부 Tomcat 서버 운영 경험이 있습니다.
Tomcat 서버를 별도로 설치·운영했습니다.
```