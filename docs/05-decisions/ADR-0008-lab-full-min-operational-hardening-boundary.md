# ADR-0008: lab-full-min 운영 설정·관측성·백업 복구 경계

## Status

Accepted

## Context

프로젝트의 고정 주제는 다음이다.

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

최근 작업에서 OpenKoda, Terraform, Spring Boot 앱, Nginx, 모니터링, 백업 복구 중 어느 하나가 프로젝트의 중심처럼 보일 위험이 있었다. 특히 Spring Boot workload를 새로 추가하면서 애플리케이션 개발 프로젝트처럼 보일 수 있고, Nginx 설정을 추가하면서 단순 reverse proxy 실습처럼 보일 수 있다.

따라서 `lab-full-min` 이후 작업은 다음을 기준으로 제한해야 한다.

- EC2를 VM처럼 사용한다.
- WEB/WAS/DB 계층을 분리한다.
- Nginx, Spring Boot, PostgreSQL은 운영 검증 대상이다.
- Terraform은 실험 환경 생성·삭제 수단이다.
- Ansible은 서버 설정 재현 수단이다.
- 모니터링은 대시보드가 아니라 장애 분석 evidence 수집 수단이다.
- 백업은 백업 실행이 아니라 복구 검증이 핵심이다.

## Decision

`lab-full-min`은 다음 운영 경계를 따른다.

### 1. Nginx

Nginx는 WEB 계층으로 두고 다음 운영 설정을 검증한다.

- HTTPS termination
- HTTP to HTTPS redirect
- reverse proxy
- upstream load balancing
- passive failover
- proxy timeout and retry
- proxy header forwarding
- request ID propagation
- access/error log evidence

`lab-full-min`에서는 Nginx OSS의 passive health behavior를 사용한다. Nginx Plus active health check를 흉내 내지 않는다.

### 2. HTTPS

운영 도메인이 없는 단기 실험 환경에서는 self-signed certificate를 허용한다. 이것은 공인 서비스 제공이 아니라 TLS termination, redirect, proxy header, log evidence를 검증하기 위한 선택이다.

공인 인증서 자동화는 도메인이 준비된 `lab-full` 이후 선택 사항으로 둔다.

### 3. Spring Boot / Tomcat

Spring Boot workload는 프로젝트 주제가 아니다. 역할은 DB-backed workload를 제공하여 다음 운영 evidence를 만들기 위한 것이다.

- app node identity
- process health
- DB readiness
- DB read/write traffic
- state change
- request log
- latency evidence

WAS 운영 검증은 systemd, JVM/Tomcat, readiness, HikariCP 병목 분석으로 확장한다.

### 4. Network

네트워크는 다음 원칙을 따른다.

- SSH는 bastion으로 제한한다.
- Nginx만 외부 HTTP/HTTPS를 받는다.
- app은 Nginx SG에서만 8080을 받는다.
- DB는 app SG에서만 5432를 받는다.
- app/db는 private node로 유지한다.
- NAT는 private node의 패키지 설치와 의존성 다운로드를 위한 보조 수단이다.

### 5. Monitoring / logging

`lab-full-min`에서는 우선 수동 evidence 기반 로그를 남긴다.

- Nginx access log
- Nginx error log
- app request log
- app systemd journal
- PostgreSQL log
- command output

Prometheus/Grafana/Loki/Alertmanager는 이후 관측성 단계에서 추가한다. 대시보드 생성 자체가 목표가 아니며, incident report에서 지표와 로그를 근거로 사용해야 한다.

### 6. Backup / restore

`lab-full-min`에서는 backup 구현을 바로 넣지 않는다. 먼저 DB-backed workload와 장애·복구 검증 가능성을 확보한다.

최종 backup/restore는 다음 단계에서 구현한다.

- `pg_dump` for PostgreSQL
- `restic` for filesystem/NFS data
- restore-lab에서 실제 복구
- checksum/API/download 검증
- 복구 소요 시간과 실패 지점 기록

## Consequences

### Positive

- 프로젝트가 OpenKoda 설치, Terraform 실습, Spring Boot 앱 개발로 변질되는 것을 막는다.
- Nginx, WAS, DB, monitoring, backup이 모두 운영 evidence 중심으로 정렬된다.
- 이후 issue를 작은 구현 단위로 나눌 수 있다.
- incident report와 직접 연결되는 설정 기준을 확보한다.

### Negative

- 초기 구현 속도는 느려진다.
- self-signed HTTPS는 실제 공인 서비스 운영과 다르므로 문서에서 한계를 명시해야 한다.
- `lab-full-min`만으로는 primary/standby, NFS, full observability, restore-lab까지 완성되지 않는다.

## Follow-up issues

1. `[APP] ops-sample-service CI 통과 및 병합`
2. `[ANSIBLE] lab-full-min Nginx HTTPS reverse proxy 구현`
3. `[ANSIBLE] ops-sample-service systemd 배포 구현`
4. `[ANSIBLE] lab-full-min PostgreSQL primary 구성`
5. `[VALIDATION] lab-full-min Nginx/App/PostgreSQL 통합 검증`
6. `[OBS] lab-full-min Prometheus/Grafana/Loki 계획 및 최소 구현`
7. `[BACKUP] pg_dump/restic 백업 및 restore-lab 복구 검증`

## Notes

이 ADR은 애플리케이션 기능 범위를 넓히기 위한 결정이 아니다. 운영 검증에 필요한 minimum workload, Nginx 설정, 로그, 지표, 백업·복구 evidence를 연결하기 위한 경계 결정이다.
