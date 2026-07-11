# AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증

이 프로젝트는 AWS EC2를 VM 환경으로 사용해 WEB/WAS/DB/파일저장소/관측성/백업 계층을 분리 구성하고, 운영 중 발생할 수 있는 장애·성능·복구 시나리오를 로그·지표·명령 결과로 검증하는 운영 포트폴리오입니다.

OpenKoda는 이 프로젝트의 주제가 아닙니다. OpenKoda는 운영환경 검증을 위해 사용한 upstream 오픈소스 workload 중 하나이며, 본 저장소 작성자가 OpenKoda 원본을 개발한 것이 아닙니다. 이 저장소의 핵심 기여는 업무 애플리케이션 자체 개발이 아니라, EC2 VM 기반 운영환경 구성, 계층 분리, 설정 자동화, 상태 점검, 장애 분석, 복구 검증, runbook 및 evidence 문서화입니다.

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

## 프로젝트 범위

### 포함 범위

- AWS EC2 기반 VM-style 운영환경 구성
- Public/Private subnet 분리
- Security Group 계층별 접근 제한
- Bastion 경유 운영 접근
- Nginx WEB/reverse proxy 계층
- WAS 계층 다중화
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

## 기술별 역할

| 요소 | 이 프로젝트에서의 역할 |
|---|---|
| OpenKoda | Phase 0 smoke-test workload 및 필요 시 대표 업무 workload |
| AWS EC2 | VM 기반 운영환경 제공 |
| Terraform | 실험 환경 생성·삭제 자동화 |
| Ansible | 서버 설정과 운영 절차 재현 자동화 |
| Nginx | WEB/reverse proxy 계층, upstream 장애 분석 대상 |
| WAS | 애플리케이션 계층, readiness/thread/connection 분석 대상 |
| PostgreSQL | DB 계층, connection/slow query/backup/restore 분석 대상 |
| NFS/filesystem | 파일 저장소 계층, DB 메타데이터와 파일 일관성 검증 대상 |
| Prometheus/Grafana/Loki | 장애 분석을 위한 지표·로그 evidence 수집 도구 |
| Restic/pg_dump | 백업이 아니라 복구 검증을 위한 도구 |

## 단계별 로드맵

### Phase 0. `lab-runtime` smoke test

완료된 단계입니다.

목적은 OpenKoda 운영 자체가 아니라, 다음 운영 기반이 실제로 작동하는지 확인하는 것이었습니다.

- Terraform으로 private app node가 있는 실험 환경 생성
- Bastion 경유 Ansible 접속
- Private app node의 NAT 기반 dependency pull
- Docker 기반 workload 실행
- 실행 결과 evidence 문서화
- 검증 후 리소스 삭제

### Phase 1. `lab-full-min` WEB/WAS/DB 최소 구성

다음 구현 대상입니다.

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

검증 대상:

- Nginx reverse proxy
- WAS upstream 다중화
- DB 연결
- Nginx access/error log
- app log
- DB connection 상태
- app 장애 시 upstream 우회 가능성

### Phase 2. `lab-full-ops` 관측성·파일·백업 계층 확장

```text
[Private Storage Subnet]
- nfs-01

[Private Ops Subnet]
- mon-01
- log-01
- backup-01
- loadgen-01
```

검증 대상:

- Prometheus metrics
- Loki logs
- 파일 저장소 장애
- 백업 및 복구 준비
- 부하 발생과 지표 변화

### Phase 3. incident drill

최소 목표 시나리오:

1. app node 장애와 Nginx upstream 우회
2. rolling deploy 중 오류율과 응답시간 확인
3. WAS/DB connection pool 병목 분석
4. PostgreSQL 백업 및 restore-lab 복구 검증

추가 시나리오:

- PostgreSQL standby promote
- 파일 저장소 장애와 복구
- 로그·지표 기반 원인 후보 제거

### Phase 4. `restore-lab`

백업을 만들었다는 사실이 아니라, 별도 환경에서 실제 복구되는지 검증합니다.

```text
lab-full 데이터 생성
-> pg_dump + restic backup
-> lab-full destroy
-> restore-lab 생성
-> DB와 파일 복구
-> checksum, HTTP 다운로드, 데이터 조회로 복구 검증
```

## OpenKoda 사용 기준

OpenKoda는 대표 workload로 사용할 수 있지만, 프로젝트 목적에 맞지 않으면 보조 workload로 격하하거나 대체합니다.

판단 기준:

- Nginx 뒤에서 WAS 역할로 분리 가능한가
- 외부 PostgreSQL과 안정적으로 연결 가능한가
- 파일 저장소를 분리하거나 검증 대상으로 삼을 수 있는가
- 장애·성능·복구 시나리오를 로그와 지표로 설명할 수 있는가

위 기준을 충족하지 못하면, `lab-full`에서는 별도 Spring Boot 기반 업무 API와 PostgreSQL, 파일 업로드 구조를 사용합니다.

## Evidence-first 원칙

최종 산출물은 서비스 화면이 아니라 evidence입니다.

각 주요 작업은 다음 중 하나 이상의 증거를 남겨야 합니다.

- Terraform plan/apply/destroy 결과
- Ansible recap
- Nginx access/error log
- WAS application log
- PostgreSQL connection/query/backup/restore 결과
- Prometheus metric 변화
- Loki log query 결과
- 장애 전후 p95/p99 latency 비교
- incident report
- restore 검증 결과

## 현재 상태

현재까지 완료된 작업은 Phase 0 smoke test입니다.

```text
Terraform lab-runtime 생성
-> Bastion 경유 private app node 접근
-> NAT dependency pull
-> Ansible 기반 workload 실행
-> local health check
-> evidence 문서화
-> Terraform destroy
```

다음 구현 방향은 OpenKoda 단일 노드 고도화가 아니라, `lab-full-min`의 WEB/WAS/DB 분리 구성입니다.
