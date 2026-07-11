# lab-full-min 운영 설정·관측성·백업 복구 계획

## 1. 고정 프로젝트 주제

이 문서는 `lab-full-min` 이후 구현이 프로젝트 주제에서 벗어나지 않도록 운영 기준을 고정한다.

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

이 프로젝트는 OpenKoda, Terraform, AWS 아키텍처, Spring Boot 앱 개발 자체가 중심이 아니다. 중심은 EC2를 VM 환경으로 사용해 WEB/WAS/DB 계층을 직접 구성하고, 장애·성능·복구 상황에서 로그·지표·명령 결과로 원인을 좁히고 대응 절차를 검증하는 것이다.

## 2. lab-full-min의 역할

`lab-full-min`은 최종 `lab-full`의 축소판이다. 이 단계에서는 다음을 먼저 검증한다.

```text
Internet / operator
  -> nginx-01
  -> app-01 / app-02
  -> db-primary-01
```

`lab-full-min`에서 중요한 것은 화면이나 복잡한 기능이 아니라 다음 운영 흐름이다.

- Nginx reverse proxy
- HTTPS/TLS termination
- upstream load balancing
- proxy header forwarding
- request ID propagation
- Nginx access/error log
- Spring Boot 내장 Tomcat 운영
- DB-dependent readiness
- PostgreSQL read/write traffic
- app node 장애 시 우회
- DB 장애 시 readiness 실패
- 명령 결과와 로그 기반 evidence

## 3. Nginx 운영 기준

### 3.1 역할

`nginx-01`은 WEB 계층이다. 단순 포트 포워딩이 아니라 다음 운영 기능을 검증하는 대상이다.

- HTTPS termination
- HTTP to HTTPS redirect
- reverse proxy
- app upstream load balancing
- upstream 장애 우회
- proxy timeout 조정
- access log 기반 원인 분석
- error log 기반 upstream 장애 확인

### 3.2 HTTPS/TLS 정책

단기 실험 환경에서는 운영 도메인과 공인 인증서를 전제로 하지 않는다. 따라서 단계별로 나눈다.

| 단계 | 정책 | 목적 |
| --- | --- | --- |
| `lab-full-min` | self-signed certificate 또는 내부 테스트 인증서 | TLS termination, redirect, certificate path, proxy header 검증 |
| `lab-full` | 도메인이 준비된 경우 ACME/Let's Encrypt 검토 | 실제 HTTPS 운영에 가까운 형태 검증 |
| 포트폴리오 제출 | 인증서 방식보다 TLS termination과 evidence를 문서화 | 실습이 아니라 운영 설정 근거를 보여줌 |

`lab-full-min`에서는 self-signed 인증서를 사용해도 된다. 중요한 것은 브라우저 신뢰 표시가 아니라 다음을 증명하는 것이다.

- Nginx가 443을 수신한다.
- 80 요청을 443으로 redirect한다.
- upstream에는 내부 HTTP로 전달한다.
- app에는 `X-Forwarded-Proto: https`가 전달된다.
- access log에 TLS/proxy/upstream 근거가 남는다.

### 3.3 포트 정책

기본 정책은 다음과 같다.

| 경로 | 허용 | 비고 |
| --- | --- | --- |
| operator CIDR -> bastion:22 | 허용 | 운영자 SSH 진입점 |
| operator 또는 web ingress CIDR -> nginx:80 | 허용 | 443 redirect 검증용 |
| operator 또는 web ingress CIDR -> nginx:443 | 허용 | HTTPS 진입점 |
| bastion -> nginx/app/db:22 | 허용 | Ansible 제어 경로 |
| nginx SG -> app SG:8080 | 허용 | WAS upstream |
| app SG -> db SG:5432 | 허용 | PostgreSQL |
| internet -> app/db | 차단 | private node 직접 노출 금지 |

`lab-full-min`은 포트폴리오 검증 환경이므로 web ingress CIDR은 기본적으로 운영자 공인 IP `/32`를 사용한다. 공개 시연이 필요한 경우에만 80/443을 넓히고, 그 근거를 문서화한다.

### 3.4 Upstream load balancing

초기 정책은 Nginx OSS 기준으로 구현한다.

```nginx
upstream ops_app_backend {
    least_conn;
    server app-01:8080 max_fails=2 fail_timeout=10s;
    server app-02:8080 max_fails=2 fail_timeout=10s;
    keepalive 32;
}
```

선택 기준은 다음과 같다.

- `least_conn`: 단순 round-robin보다 요청 처리 시간이 다른 상황을 설명하기 좋다.
- `max_fails`, `fail_timeout`: app 장애 시 upstream 제외 근거를 남기기 좋다.
- `keepalive`: upstream connection 재사용을 명시해 reverse proxy 운영성을 보여준다.

Nginx OSS에는 상용 Nginx Plus의 active health check가 없다. 따라서 `lab-full-min`에서는 passive failure handling을 사용한다. active health check가 없다는 점을 숨기지 않고, access/error log와 반복 요청 결과로 장애 우회를 증명한다.

### 3.5 Proxy timeout/retry 정책

초기값은 다음 기준으로 둔다.

```nginx
proxy_connect_timeout 3s;
proxy_send_timeout 10s;
proxy_read_timeout 10s;
proxy_next_upstream error timeout http_502 http_503 http_504;
proxy_next_upstream_tries 2;
```

의도는 다음과 같다.

- app process down: 빠르게 다음 upstream으로 우회
- DB 장애: app은 살아 있으나 `/readyz`와 DB API가 503 반환
- timeout 장애: Nginx `upstream_response_time`, `request_time`으로 병목 확인

### 3.6 Proxy headers

앱 로그, Nginx 로그, curl 결과를 연결하기 위해 아래 header를 전달한다.

```nginx
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Request-Id $request_id;
```

향후 incident report에서는 `X-Request-Id`를 기준으로 다음을 연결한다.

```text
curl output
-> Nginx access log
-> app request log
-> DB readiness / DB API result
```

### 3.7 Nginx log format

Nginx access log에는 최소한 다음 필드가 필요하다.

```text
time
request_id
remote_addr
request
status
body_bytes_sent
request_time
upstream_addr
upstream_status
upstream_response_time
http_x_forwarded_for
user_agent
```

장애 분석에서 보는 기준은 다음이다.

| 증상 | 확인 지표/로그 |
| --- | --- |
| app down | `upstream_status`, error log의 connection refused |
| app latency | `upstream_response_time` 증가 |
| proxy timeout | 504, timeout error log |
| DB 장애 | app은 503 반환, `/healthz`는 200 가능 |
| upstream 우회 | 반복 요청에서 `upstream_addr` 변화 |

## 4. Spring Boot / Tomcat / Middleware 기준

### 4.1 서비스 역할

`ops-sample-service`는 프로젝트 주제가 아니다. 역할은 운영 검증을 위해 통제 가능한 업무 데이터 흐름을 제공하는 것이다.

필수 동작은 다음이다.

- `/healthz`: process-level health
- `/readyz`: DB-dependent readiness
- `/node`: app node identity
- `/db/time`: DB query 확인
- `/api/work-orders`: DB-backed 업무 데이터 조회
- `POST /api/work-orders`: DB write
- `PATCH /api/work-orders/{id}/status`: DB-backed state change

### 4.2 Systemd 운영 기준

앱은 수동 `java -jar`가 아니라 systemd service로 운영한다.

예정 기준:

```text
/etc/systemd/system/ops-sample-service.service
/opt/ops-sample-service/ops-sample-service.jar
/etc/ops-sample-service/ops-sample-service.env
/var/log/ops-sample-service/
```

systemd에서 확인할 evidence:

```bash
systemctl status ops-sample-service
journalctl -u ops-sample-service --since '10 minutes ago'
ss -lntp | grep 8080
curl -i http://localhost:8080/healthz
curl -i http://localhost:8080/readyz
```

### 4.3 Tomcat / HikariCP 병목 계획

초기 `lab-full-min`에서는 과도한 튜닝보다 관찰 가능한 기준을 먼저 둔다.

향후 추가할 설정:

```text
SERVER_TOMCAT_THREADS_MAX
SERVER_TOMCAT_ACCEPT_COUNT
HIKARI_MAXIMUM_POOL_SIZE
HIKARI_CONNECTION_TIMEOUT
```

성능 장애 시나리오는 다음과 같이 진행한다.

```text
1. 정상 기준 latency 측정
2. HikariCP pool size를 작게 제한
3. loadgen으로 DB-backed API 반복 호출
4. app log, Nginx upstream_response_time, DB connection 수 확인
5. pool size 조정 전후 p95/p99 또는 request_time 비교
```

이 시나리오는 `lab-full-min` 문서화 후 별도 issue로 진행한다.

## 5. 네트워크 운영 기준

### 5.1 계층 분리 원칙

`lab-full-min` 네트워크는 다음 원칙을 지킨다.

- SSH 진입은 bastion으로 제한한다.
- web 계층만 외부 HTTP/HTTPS를 받는다.
- app 계층은 web SG에서만 8080을 받는다.
- DB 계층은 app SG에서만 5432를 받는다.
- app/db에는 public IP를 부여하지 않는다.
- NAT는 private node의 패키지 설치와 의존성 다운로드용이다.

### 5.2 운영 evidence

네트워크 검증 evidence는 다음을 남긴다.

```bash
terraform output
ansible -i inventories/lab-full-min/hosts.yml lab_full_min -m ping
ss -lntp
curl -I http://<nginx-public-ip>
curl -k -I https://<nginx-public-ip>
curl -s https://<nginx-public-ip>/node
```

추가로 security group 흐름은 issue 또는 evidence 문서에 다음 형식으로 기록한다.

```text
operator -> bastion:22
operator -> nginx:80/443
bastion -> nginx/app/db:22
nginx -> app:8080
app -> db:5432
```

## 6. 모니터링 / 로그 계획

### 6.1 단계별 계획

| 단계 | 범위 | 목적 |
| --- | --- | --- |
| `lab-full-min` | Nginx log, app log, systemd/journal, PostgreSQL log | 수동 evidence 기반 장애 분석 |
| `lab-full-obs` | Prometheus, Grafana, Alertmanager | 지표 기반 분석 |
| `lab-full-logs` | Loki, Alloy 또는 Promtail 계열 로그 수집 | requestId 기반 로그 추적 |
| `incident` | 로그·지표·명령 결과를 incident report에 연결 | 포트폴리오 핵심 산출물 |

`lab-full-min`에서 바로 풀 모니터링 스택을 설치하지 않는다. 먼저 Nginx/app/DB 로그가 충분히 의미 있게 남는지 검증한 뒤 관측성 계층을 추가한다.

### 6.2 수집 대상

향후 모니터링 대상은 다음과 같다.

| 대상 | 수집 항목 |
| --- | --- |
| node | CPU, memory, disk, network, systemd 상태 |
| Nginx | access log, error log, request_time, upstream_response_time, upstream_status |
| app | request log, readiness result, durationMs, JVM/Tomcat/HikariCP metrics |
| PostgreSQL | connection 수, slow query, lock, replication 상태 |
| backup | backup 성공/실패, backup size, restore result |

### 6.3 Alert 기준 초안

초기 alert 후보는 다음이다.

- app `/readyz` 실패
- Nginx 5xx 비율 증가
- upstream_response_time 증가
- DB connection 사용률 높음
- disk 사용률 높음
- PostgreSQL down
- backup 실패

Alertmanager 구성은 `lab-full-obs` 단계에서 별도 issue로 구현한다.

## 7. 백업·복구 계획

### 7.1 백업 대상

최종 운영 검증에서 백업 대상은 다음이다.

| 대상 | 도구 | 비고 |
| --- | --- | --- |
| PostgreSQL DB | `pg_dump` | 논리 백업, 복구 검증 필수 |
| 첨부파일 저장소 | `restic` | NFS/filesystem 데이터 백업 |
| 설정 파일 | Git + evidence copy | Nginx/app/PostgreSQL 설정 |
| 로그 evidence | 압축 또는 restic | incident report 근거 보존 |

### 7.2 복구 검증 원칙

백업은 실행 자체가 아니라 복구 검증이 핵심이다.

최종 목표는 다음이다.

```text
lab-full에서 데이터 생성
-> pg_dump + restic backup
-> lab-full destroy
-> restore-lab 생성
-> DB와 파일 복구
-> API 조회, 파일 checksum, 다운로드 검증
-> 복구 소요 시간과 실패 지점 기록
```

### 7.3 lab-full-min에서의 위치

`lab-full-min`에서는 아직 NFS와 standby가 없으므로 백업 구현을 바로 넣지 않는다. 대신 다음을 먼저 준비한다.

- DB-backed workload 데이터 생성
- 데이터 변경 API 확보
- DB 장애 시 readiness 실패 확인
- 복구 후 같은 데이터 조회 확인

실제 `pg_dump`와 `restic`은 `lab-full` 또는 `restore-lab` 단계에서 구현한다.

## 8. 향후 작업 issue 분해

이 문서 이후 작업은 다음 순서로 나눈다.

### 8.1 PR #50 마무리

```text
[APP] ops-sample-service CI 통과 및 병합
```

- GitHub Actions Maven build 확인
- 실패 시 컴파일 오류 수정
- 병합 후 다음 Ansible 배포 작업으로 이동

### 8.2 Nginx 운영 설정 구현

```text
[ANSIBLE] lab-full-min Nginx HTTPS reverse proxy 구현
```

범위:

- Nginx 설치
- self-signed TLS 인증서 생성
- 80 -> 443 redirect
- upstream `app-01/app-02:8080`
- proxy headers
- timeout/retry 설정
- custom access log
- configtest/reload

### 8.3 App systemd 배포

```text
[ANSIBLE] ops-sample-service systemd 배포 구현
```

범위:

- jar 배포
- env file 배포
- systemd unit
- health/readiness check
- app log 확인

### 8.4 PostgreSQL primary 구성

```text
[ANSIBLE] lab-full-min PostgreSQL primary 구성
```

범위:

- PostgreSQL 설치
- DB/user 생성
- app 접속 허용
- PostgreSQL log 설정
- `/readyz`, `/api/work-orders` 검증

### 8.5 통합 검증

```text
[VALIDATION] lab-full-min Nginx/App/PostgreSQL 통합 검증
```

범위:

- Terraform apply
- Ansible Nginx/App/PostgreSQL 적용
- HTTPS endpoint 검증
- work order read/write/status update
- Nginx/app/DB log evidence
- destroy

### 8.6 관측성 계층

```text
[OBS] lab-full-min Prometheus/Grafana/Loki 계획 및 최소 구현
```

범위:

- node exporter
- Nginx log/metrics
- app actuator/prometheus endpoint
- PostgreSQL exporter 또는 DB-level checks
- Loki/Alloy 로그 수집

### 8.7 백업·복구

```text
[BACKUP] pg_dump/restic 백업 및 restore-lab 복구 검증
```

범위:

- DB 데이터 생성
- 파일 데이터 생성
- backup 실행
- destroy 이후 restore-lab 복구
- checksum/API 검증
- 복구 시간과 실패 지점 기록

## 9. incident report 기준

모든 장애 시나리오는 다음 형식으로 남긴다.

```text
1. 장애 목적
2. 정상 기준 상태
3. 장애 주입 방법
4. 사용자 영향
5. 관찰한 로그
6. 관찰한 지표
7. 원인 후보와 제거 근거
8. 조치 내용
9. 조치 전후 비교
10. 재발 방지 설정
11. 남은 한계
```

이 형식이 없으면 단순 실습으로 보인다. 이후 모든 validation issue는 incident report와 연결되어야 한다.
