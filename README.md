# AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증

이 프로젝트는 AWS EC2를 VM 환경으로 사용해 업무시스템의 WEB/WAS/DB/파일저장소/관측성/백업 계층을 분리 구성하고, 운영 중 발생할 수 있는 장애·성능·복구 시나리오를 로그·지표·명령 결과로 검증하는 운영 포트폴리오입니다.

이 저장소의 주제는 OpenKoda가 아닙니다. Terraform도 아니고, AWS 관리형 아키텍처 자체도 아닙니다. 핵심은 **VM 기반 다계층 운영환경을 직접 구성하고, 장애 상황에서 어느 계층을 확인해야 하는지 evidence로 설명하는 것**입니다.

## 한 줄 정의

```text
EC2 VM 기반 WEB/WAS/DB/파일저장소 운영환경을 구성하고, 장애·성능·복구를 로그와 지표로 검증하는 프로젝트
```

## 핵심 메시지

이 프로젝트가 보여주려는 역량은 다음입니다.

```text
운영 대상 시스템을 이해하고,
계층별로 배치하고,
설정 기준을 잡고,
장애를 재현하고,
로그·지표·명령 결과로 원인을 좁히고,
복구 절차를 검증할 수 있는 운영 역량
```

채용담당자와 실무담당자에게 전달하려는 메시지는 다음입니다.

```text
실제 운영 경험은 제한적이지만,
WEB/WAS/DB/파일저장소/관측성/백업 구조를 나누어 이해하고,
장애 상황에서 확인해야 할 지점과 복구 절차를 근거 기반으로 정리할 수 있다.
```

## 현재 상태 요약

현재까지 완료된 핵심 단계는 다음입니다.

```text
Phase 0. lab-runtime smoke test 완료
Phase 1. lab-full-min WEB/WAS/DB 최소 운영환경 구현 및 검증 완료
Phase 2A. lab-full-ops 파일저장소 검증 완료
```

`lab-full-min`에서는 다음 최소 운영 경로를 구성하고 검증했습니다.

```text
operator -> nginx-01:443 -> app-01/app-02:8080 -> db-primary-01:5432
```

`lab-full-ops` storage validation에서는 다음 확장 경로를 구성하고 검증했습니다.

```text
operator -> nginx-01:443 -> app-01:8080 -> db-primary-01:5432
                                 |
                                 -> nfs-01:/srv/ops-sample/files
```

완료된 주요 검증은 다음입니다.

```text
1. WEB/WAS/DB 통합 정상 경로 검증
2. app-01 장애 시 Nginx upstream 우회 검증
3. app-01/app-02 rolling restart 중 서비스 연속성 검증
4. DB-backed 동시 요청 80건, concurrency 16 관측
5. PostgreSQL 중지 시 DB-dependent endpoint 실패와 복구 검증
6. NFS mount 및 write probe 검증
7. work-order evidence file 생성 검증
8. PostgreSQL metadata row와 NFS file object consistency 검증
9. stale artifact, NFS 권한, playbook idempotency 문제 진단 및 후속 수정
10. Terraform destroy 기반 실험 환경 정리
```

대표 evidence 문서:

```text
docs/04-evidence/lab-full-min-web-was-db-integrated-validation.md
docs/04-evidence/lab-full-min-continuous-operations-validation.md
docs/04-evidence/lab-full-ops-storage-validation-2026-07-12.md
```

## 프로젝트 기준 문서

새 작업을 시작하기 전에는 아래 문서를 먼저 확인합니다.

```text
docs/00-project/project-scope.md
docs/00-project/roadmap.md
docs/00-project/workload-strategy.md
docs/00-project/next-chat-handoff.md
```

이 문서들은 프로젝트가 OpenKoda 설치, Terraform showcase, 샘플앱 개발, 단순 대시보드 작업으로 흐르지 않게 하기 위한 기준 문서입니다.

## 기술별 역할

| 요소 | 이 프로젝트에서의 역할 | 주의점 |
|---|---|---|
| OpenKoda | 운영 대상 업무시스템 후보 및 Phase 0 smoke-test workload | 프로젝트 주제가 아님 |
| ops-sample-service | 장애·복구와 DB/file consistency를 재현하기 위한 controlled workload | 최종 서비스 주제가 아님 |
| AWS EC2 | VM 기반 운영환경 제공 | 관리형 서비스 중심 프로젝트가 아님 |
| Terraform | 실험 환경 생성·삭제 자동화 | 기존 Terraformers와 겹치지 않게 보조 도구로 사용 |
| Ansible | 서버 설정과 운영 절차 재현 자동화 | role 자체보다 운영 설정의 일관성이 중요 |
| Nginx | WEB/reverse proxy 계층 | upstream, timeout, access/error log 분석 대상 |
| Spring Boot/Tomcat | WAS 계층 | readiness, request log, artifact/version 검증 대상 |
| PostgreSQL | DB 계층 | connection, metadata, backup, restore 분석 대상 |
| NFS/filesystem | 파일 저장소 계층 | DB 메타데이터와 파일 일관성·복구 검증 대상 |
| Prometheus/Grafana/Loki | 장애 분석을 위한 지표·로그 evidence 수집 도구 | 대시보드 자체가 목적이 아님 |
| Restic/pg_dump | 백업·복구 검증 도구 | 백업 생성보다 restore 검증이 핵심 |

## 프로젝트 범위

### 포함 범위

- AWS EC2 기반 VM-style 운영환경 구성
- Public/Private subnet 분리
- Security Group 계층별 접근 제한
- Bastion 경유 운영 접근
- Nginx WEB/reverse proxy 계층
- WAS 계층
- PostgreSQL DB 계층
- NFS 또는 filesystem 기반 파일 저장소 계층
- Prometheus/Grafana/Alertmanager 기반 관측성
- Loki/Alloy 기반 로그 수집
- Restic 및 pg_dump 기반 백업·복구 검증
- Terraform 기반 실험 환경 생성·삭제
- Ansible 기반 서버 설정 재현 자동화
- 장애 시나리오 및 incident report
- 로그·지표·명령 결과 기반 evidence

### 제외 범위

- OpenKoda 기능 개발 중심 작업
- OpenKoda UI 개선 중심 작업
- 단순 Docker Compose 배포 프로젝트
- Terraform 모듈 showcase
- Kubernetes/EKS/GitOps 중심 프로젝트
- ALB/RDS/CloudWatch 같은 관리형 서비스 중심 아키텍처
- 예쁜 Grafana dashboard만 만드는 모니터링 프로젝트
- 시나리오 없는 Issue/PR 누적

## 단계별 로드맵

### Phase 0. `lab-runtime` smoke test — 완료

목적은 OpenKoda 자체 운영이 아니라, private subnet workload를 EC2 환경에서 실행하고 Bastion/Ansible/NAT/cleanup 흐름이 작동하는지 확인하는 것이었습니다.

완료 항목:

```text
Terraform lab-runtime 생성
-> Bastion 경유 private app node 접근
-> NAT dependency pull
-> Ansible 기반 workload 실행
-> local health check
-> evidence 문서화
-> Terraform destroy
```

### Phase 1. `lab-full-min` WEB/WAS/DB 최소 운영환경 — 완료

검증된 최소 운영 토폴로지는 다음입니다.

```text
[Public Subnet]
- bastion-01
- nginx-01

[Private App Subnet]
- app-01
- app-02

[Private DB Subnet]
- db-primary-01
```

검증한 운영 시나리오:

```text
- WEB/WAS/DB 정상 경로
- Nginx reverse proxy
- WAS upstream 다중화
- PostgreSQL DB 연결
- app node 장애와 Nginx upstream 우회
- app node rolling restart
- DB-backed 동시 요청 관측
- PostgreSQL 장애와 복구
```

### Phase 2A. `lab-full-ops` 파일저장소 검증 — 완료

검증된 확장 토폴로지는 다음입니다.

```text
[Public Subnet]
- bastion-01
- nginx-01

[Private App Subnet]
- app-01

[Private DB Subnet]
- db-primary-01

[Private Storage Subnet]
- nfs-01

[Private Ops Subnet]
- backup-01
```

검증한 운영 시나리오:

```text
- NFS server export baseline
- app-side NFS mount baseline
- root_squash 환경에서 NFS write permission failure 진단
- app NFS mount playbook idempotency failure 진단
- stale jar artifact로 인한 evidence endpoint 404 진단
- work-order DB metadata + NFS file object consistency 검증
- Nginx request-id 기반 접근 로그 확인
- Terraform destroy 후 비용 리스크 정리
```

### Phase 2B. 백업 baseline — 다음 구현 대상

다음 단계는 파일저장소 검증을 백업·복구 검증으로 연결하는 것입니다.

우선순위:

```text
1. PostgreSQL metadata backup boundary 정의
2. NFS file object backup boundary 정의
3. pg_dump baseline 추가
4. restic 또는 명시적 file backup baseline 추가
5. backup-01 또는 명시적 backup target에 artifact 보관
6. backup artifact inventory와 checksum evidence 수집
7. restore-lab 검증으로 연결
```

### Phase 3. restore-lab 백업·복구 검증 — 필수 목표

백업을 만들었다는 사실이 아니라, 별도 환경에서 실제로 복구되는지 검증합니다.

```text
lab-full-ops 데이터 생성
-> pg_dump + file backup
-> lab-full-ops destroy
-> restore-lab 생성
-> DB와 파일 복구
-> checksum, API consistency, 데이터 조회로 복구 검증
```

### Phase 4. 고도화 후보

기본 운영 evidence가 정리된 뒤 다음을 선택적으로 수행합니다.

```text
- PostgreSQL standby promote
- WAS/HikariCP/DB connection pool 병목 분석
- p95/p99 latency 비교
- Alertmanager 알림 흐름
- 장애별 incident report 정리
```

## workload 사용 기준

OpenKoda는 대표 업무시스템 workload 후보로 사용할 수 있지만, 프로젝트 목적에 맞지 않으면 보조 workload로 둡니다.

`ops-sample-service`는 최종 서비스 주제가 아니라 다음을 검증하기 위한 controlled workload입니다.

```text
- Nginx -> WAS -> DB 경로
- /healthz 와 /readyz 분리
- DB-backed work-order endpoint
- request ID 기반 로그 추적
- PostgreSQL metadata와 NFS file object consistency
- app 장애, DB 장애, storage 장애의 차이
```

향후 OpenKoda를 다시 사용할지는 아래 기준으로 판단합니다.

```text
- Nginx 뒤에서 WAS 역할로 분리 가능한가
- 외부 PostgreSQL과 안정적으로 연결 가능한가
- 파일 저장소를 분리하거나 검증 대상으로 삼을 수 있는가
- 장애·성능·복구 시나리오를 로그와 지표로 설명할 수 있는가
```

위 기준을 충족하지 못하면, OpenKoda는 Phase 0 smoke-test workload로 남기고 `lab-full-ops`는 controlled workload 중심으로 확장합니다.

## Evidence-first 원칙

최종 산출물은 서비스 화면이 아니라 evidence입니다.

각 주요 작업은 다음 중 하나 이상의 증거를 남겨야 합니다.

- Terraform plan/apply/destroy 결과
- Ansible recap
- systemd 상태
- Nginx access/error log
- 애플리케이션 request log
- PostgreSQL query 결과
- NFS/file checksum 결과
- 백업 artifact inventory
- 복구 검증 결과
- incident report

## Runtime validation policy

AWS runtime validation은 필요할 때 수행합니다. 다만 작은 PR마다 Terraform apply/destroy를 반복하지 않습니다.

```text
문서/Ansible syntax/follow-up 수정
-> static check 중심

새 backup/restore/observability/incident scenario
-> 명시적 runtime validation window
-> evidence 수집
-> 즉시 destroy
```

NAT Gateway를 켠 검증 창은 비용 리스크가 있으므로, evidence 수집 후 즉시 destroy합니다.

## 현재 다음 작업

다음 작업은 파일저장소 검증을 백업·복구 검증으로 연결하는 것입니다.

```text
[ANSIBLE] Add lab-full-ops pg_dump and file backup baseline
```

이 작업의 목표는 백업을 만들었다고 끝내는 것이 아니라, 이후 `restore-lab`에서 실제 복구 검증으로 이어질 수 있는 backup artifact와 evidence를 만드는 것입니다.
