# Interview explanation notes

Use this document to explain the project in interviews without drifting into a Terraform, Prometheus, or dashboard-first narrative.

## One-line answer

```text
AWS EC2 기반으로 WEB/WAS/DB/파일저장소/백업/관측성 계층을 직접 구성하고, 장애와 복구 상황을 로그·지표·명령 결과로 검증한 운영 포트폴리오입니다.
```

Shorter version:

```text
VM 기반 다계층 업무시스템 운영환경을 만들고, 장애가 났을 때 어느 계층을 확인해야 하는지 evidence로 설명하는 프로젝트입니다.
```

## 2-minute interview script

```text
이 프로젝트는 AWS EC2를 VM 환경처럼 사용해 WEB, WAS, DB, 파일저장소, 백업, 관측성 계층을 분리 구성하고 운영 상황을 검증한 프로젝트입니다.

단순히 서버를 띄우는 데서 끝내지 않고, Nginx에서 Spring Boot WAS로 요청이 전달되고, WAS가 PostgreSQL과 NFS 파일저장소를 함께 사용하는 구조를 만들었습니다. 이후 정상 요청, WAS 장애, DB 장애, 파일 메타데이터와 실제 파일의 일관성, 백업 artifact 생성, 별도 restore-lab 복구까지 단계적으로 검증했습니다.

가장 중요하게 본 부분은 장애 상황에서 추측하지 않고 evidence로 원인을 좁히는 것이었습니다. 예를 들어 DB 장애 상황에서는 /healthz는 200이지만 /readyz와 DB-dependent API는 503이 되었고, Prometheus에서는 DB host의 node_exporter는 up=1로 살아 있는데 PostgreSQL service active metric은 0으로 떨어지는 것을 확인했습니다. 이를 통해 DB 서버 자체가 죽은 것이 아니라 PostgreSQL 서비스 의존성이 실패했다는 결론을 낼 수 있었습니다.

또한 백업은 단순히 pg_dump와 restic snapshot을 만든 것으로 끝내지 않고, 별도 restore-lab 환경에서 DB metadata와 NFS 파일을 복구한 뒤 API consistency와 SHA-256 checksum으로 검증했습니다.

이 프로젝트의 목적은 화려한 대시보드나 관리형 서비스를 보여주는 것이 아니라, VM 기반 운영환경에서 계층을 분리하고, 장애·복구 상황을 로그와 지표로 설명하는 운영 역량을 보여주는 것입니다.
```

## 30-second version

```text
EC2 기반으로 Nginx, Spring Boot WAS, PostgreSQL, NFS, backup node, Prometheus monitoring node를 분리 구성했습니다. 정상 경로뿐 아니라 WAS 장애, DB 서비스 장애, DB metadata와 NFS 파일 일관성, 백업 artifact 생성, 별도 restore-lab 복구를 검증했습니다. 특히 DB 장애에서는 DB host는 Prometheus에서 up=1이지만 PostgreSQL service metric은 active=0이고, /readyz와 API는 503이 되는 evidence를 통해 DB host 장애가 아니라 DB service dependency failure라고 판단했습니다.
```

## What I built

| Area | Explanation |
|---|---|
| WEB | Nginx reverse proxy and request-path validation |
| WAS | Spring Boot controlled workload with health/readiness/API paths |
| DB | PostgreSQL primary service and DB-backed work-order data |
| Storage | NFS-backed file object storage separated from DB metadata |
| Backup | pg_dump and restic-based backup artifact creation |
| Restore | separate restore-lab validation of DB/file/API consistency |
| Observability | logs, service state, node_exporter metrics, Prometheus scrape, DB service alert-rule evaluation |
| IaC/config | Terraform for lab lifecycle, Ansible for server configuration and evidence collection |

## Main evidence chain

```text
1. WEB/WAS/DB normal path works.
2. WAS failure can be isolated through Nginx upstream behavior.
3. DB-dependent readiness/API paths fail when PostgreSQL is stopped.
4. DB metadata and NFS file object consistency can be checked together.
5. Backup artifacts are created with inventory and checksum evidence.
6. Restore-lab proves the backup artifacts can restore DB/file/API consistency in a separate environment.
7. Logs and metrics narrow the DB incident from host failure to PostgreSQL service failure.
8. Prometheus rule evaluation detects PostgreSQL service inactivity while DB host remains reachable.
```

## Strongest interview points

### 1. The project is operations-focused, not app-feature-focused

Good answer:

```text
서비스 기능 자체보다 운영자가 장애 상황에서 어떤 계층을 먼저 확인해야 하는지를 검증하는 데 초점을 두었습니다. 그래서 /healthz와 /readyz를 분리하고, DB metadata와 NFS file consistency를 확인할 수 있는 workload를 사용했습니다.
```

Avoid:

```text
Spring Boot 서비스를 만들었습니다.
```

The Spring Boot app is only a controlled workload.

### 2. The restore validation is stronger than backup creation

Good answer:

```text
백업 파일을 만들었다는 것만으로는 복구 가능성을 증명할 수 없다고 봤습니다. 그래서 lab-full-ops에서 만든 backup artifact를 별도 restore-lab에 옮기고, pg_restore와 restic restore 후 API consistency와 SHA-256 checksum으로 복구 결과를 검증했습니다.
```

Avoid:

```text
pg_dump와 restic으로 백업했습니다.
```

Backup creation alone is not the core claim.

### 3. The observability work supports diagnosis, not dashboard polish

Good answer:

```text
Prometheus를 도입한 목적은 대시보드를 꾸미기 위해서가 아니라 장애 원인을 좁히기 위해서였습니다. DB 장애 때 DB host의 node_exporter는 up=1이었고 PostgreSQL service active metric은 0이었기 때문에, DB host 장애가 아니라 PostgreSQL service dependency failure라고 판단할 수 있었습니다.
```

Avoid:

```text
Prometheus 모니터링을 구축했습니다.
```

That is too generic.

## Likely interview questions and answers

### Q1. 왜 EC2로 직접 구성했나요?

```text
관리형 서비스만 사용하면 운영자가 직접 확인해야 하는 계층별 설정, 로그, 포트, systemd 상태, 파일 권한, 백업 artifact의 경계를 보기 어렵다고 판단했습니다. 이 프로젝트에서는 EC2를 VM 환경처럼 사용해서 WEB/WAS/DB/Storage/Backup/Monitoring 계층을 직접 나누고, 각 계층에서 장애가 났을 때 어떤 evidence를 확인해야 하는지 검증했습니다.
```

### Q2. Terraform이 핵심인가요?

```text
아닙니다. Terraform은 실험 환경을 반복 가능하게 만들고 비용 리스크 때문에 검증 후 destroy하기 위한 도구입니다. 프로젝트의 핵심은 Terraform 코드 자체가 아니라, 생성된 VM 기반 운영환경에서 장애·복구를 어떻게 검증했는지입니다.
```

### Q3. Ansible이 핵심인가요?

```text
Ansible도 보조 도구입니다. 다만 서버 설정과 evidence 수집 절차를 사람이 임의로 수행하지 않고 재현 가능하게 만들기 위해 사용했습니다. 예를 들어 PostgreSQL, NFS, application service, Nginx, node_exporter, Prometheus 설정과 evidence 수집을 playbook으로 남겼습니다.
```

### Q4. 가장 의미 있었던 장애 검증은 무엇인가요?

```text
DB service incident 검증입니다. PostgreSQL을 중지했을 때 /healthz는 200으로 살아 있었지만 /readyz와 DB-dependent API는 503을 반환했습니다. 동시에 Prometheus에서는 DB host의 node_exporter up metric은 1이었고 PostgreSQL service active metric은 0이었습니다. 이 조합을 통해 DB host outage가 아니라 DB service dependency failure라고 판단할 수 있었습니다.
```

### Q5. 백업과 복구는 어떻게 검증했나요?

```text
lab-full-ops에서 PostgreSQL metadata와 NFS file object를 가진 데이터를 만든 뒤, backup-01에서 pg_dump와 restic snapshot을 생성했습니다. 이후 별도의 restore-lab 환경을 만들고, 해당 artifact를 복구한 뒤 work-order row count, evidence metadata, NFS file size, SHA-256 checksum, API consistency endpoint를 통해 복구 결과가 일치하는지 검증했습니다.
```

### Q6. Prometheus alert까지 했으면 운영 모니터링이 완성된 건가요?

```text
그렇게 주장하지 않습니다. 이 프로젝트에서 검증한 것은 Alertmanager나 온콜 알림 체계가 아니라 Prometheus rule evaluation입니다. 즉 DB host는 reachable한데 PostgreSQL service가 inactive인 상태를 rule이 firing으로 평가하는지까지 검증했습니다. production monitoring maturity나 paging workflow는 별도 범위입니다.
```

### Q7. 왜 OpenKoda가 아니라 ops-sample-service를 사용했나요?

```text
이 프로젝트의 주제는 특정 오픈소스 서비스를 운영하는 것이 아니라 WEB/WAS/DB/Storage/Backup/Observability 계층을 나누고 장애·복구를 검증하는 것입니다. 그래서 운영 시나리오를 명확히 재현할 수 있는 controlled workload를 사용했습니다. OpenKoda는 Phase 0에서 workload 후보로 검토했지만, 최종 주제는 OpenKoda가 아닙니다.
```

### Q8. 이 프로젝트에서 가장 많이 배운 운영 포인트는 무엇인가요?

```text
장애 상황에서 하나의 증거만 보고 판단하면 안 된다는 점입니다. 예를 들어 Prometheus /-/ready는 Prometheus 서버가 준비됐다는 뜻이지 scrape target이 정상이라는 뜻은 아니었습니다. 그래서 /api/v1/targets와 job-specific up query까지 확인하도록 playbook을 수정했습니다. 이런 식으로 service state, port, HTTP response, application log, Prometheus metric을 함께 봐야 원인을 좁힐 수 있다는 점을 배웠습니다.
```

## Claims that are safe to make

```text
EC2 기반 WEB/WAS/DB/Storage/Backup/Monitoring 계층을 분리 구성했다.
Nginx -> WAS -> PostgreSQL 정상 경로를 검증했다.
WAS 장애와 DB 장애를 구분해 검증했다.
DB metadata와 NFS file object consistency를 검증했다.
pg_dump/restic backup artifact를 만들고 별도 restore-lab에서 복구 검증했다.
node_exporter와 Prometheus metric으로 DB host reachability와 PostgreSQL service failure를 구분했다.
Prometheus rule evaluation으로 PostgreSQL service inactivity while host reachable 조건을 검증했다.
```

## Claims to avoid

```text
운영 환경을 production 수준으로 구축했다.
고가용성 또는 자동 failover를 구현했다.
Alertmanager 기반 알림 체계를 완성했다.
Grafana dashboard를 구축했다.
SLO/SLA를 검증했다.
RDS, ALB, CloudWatch 중심의 AWS managed architecture를 운영했다.
Kubernetes/EKS/GitOps 운영 프로젝트다.
OpenKoda 운영 프로젝트다.
```

## Project explanation structure for portfolio review

When someone opens the repository, guide them in this order:

```text
1. README.md
   - project identity and current completed scope
2. docs/00-project/portfolio-summary.md
   - portfolio-facing summary
3. docs/04-evidence/evidence-index.md
   - evidence map
4. docs/04-evidence/restore-lab-recovery-validation-2026-07-12.md
   - backup/restore proof
5. docs/04-evidence/observability-alert-validation-2026-07-12.md
   - metric/rule-based DB service incident proof
```

## Final positioning

```text
이 프로젝트는 서비스를 예쁘게 배포한 결과물이 아니라, 장애 상황에서 운영자가 어떤 계층을 확인하고 어떤 evidence로 판단해야 하는지를 직접 검증한 운영 포트폴리오입니다.
```
