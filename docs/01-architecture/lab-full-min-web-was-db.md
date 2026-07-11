# lab-full-min WEB/WAS/DB minimum operating environment

## Purpose

`lab-full-min` is the first real implementation phase after the Phase 0 `lab-runtime` smoke test.

The goal is not to prove that a single workload can run on an EC2 instance. That was already covered by Phase 0. The goal of `lab-full-min` is to prove the original project thesis:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

In practical terms, this phase builds the smallest VM-based WEB/WAS/DB environment that can support operational checks and the first failure scenario.

## Phase classification

| Phase | Status | Meaning |
|---|---|---|
| Phase 0: `lab-runtime` | completed | Private EC2 workload smoke test, bastion path, NAT dependency pull, Ansible deployment evidence |
| Phase 1: `lab-full-min` | next | Minimum WEB/WAS/DB operating environment |
| Phase 2: `lab-full-ops` | later | Storage, observability, backup, logging, load generation |
| Phase 3: incident scenarios | later | Logs, metrics, recovery, root-cause evidence |
| Phase 4: `restore-lab` | later | Backup and restore verification |

## Node layout

`lab-full-min` contains only the nodes required to prove WEB/WAS/DB separation and the first failover-style scenario.

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

This is intentionally smaller than the final `lab-full` target. The final target may later add `nginx-02`, `app-03`, `db-standby-01`, `nfs-01`, `mon-01`, `log-01`, `backup-01`, and `loadgen-01`.

## Role responsibilities

| Node | Role | Responsibility |
|---|---|---|
| `bastion-01` | controlled entry point | SSH entry from operator network and ProxyCommand hop to private nodes |
| `nginx-01` | WEB tier | reverse proxy, upstream routing, access/error log generation |
| `app-01` | WAS tier | application runtime, health/readiness endpoint, DB-dependent endpoint |
| `app-02` | WAS tier | second application runtime for upstream failover evidence |
| `db-primary-01` | DB tier | PostgreSQL primary database for application state and DB connection evidence |

## Network placement

### Public subnet

`bastion-01` and `nginx-01` are placed in a public subnet.

`bastion-01` receives SSH from the operator CIDR only.

`nginx-01` may receive HTTP from the operator CIDR for validation. If broader access is ever needed for demonstration, it must be explicitly justified in an issue and evidence document.

### Private application subnet

`app-01` and `app-02` are private nodes.

They must not have public IP addresses. They receive:

- SSH only from `bastion-01`
- HTTP application traffic only from `nginx-01`

### Private database subnet

`db-primary-01` is private.

It must not have a public IP address. It receives:

- SSH only from `bastion-01`
- PostgreSQL traffic only from `app-01` and `app-02`

## Security group flow

```text
operator CIDR -> bastion-01:22
operator CIDR -> nginx-01:80
bastion-01 -> nginx-01:22
bastion-01 -> app-01/app-02:22
bastion-01 -> db-primary-01:22
nginx-01 -> app-01/app-02:8080
app-01/app-02 -> db-primary-01:5432
```

Denied by design:

```text
operator -> app-01/app-02 direct SSH/HTTP
operator -> db-primary-01 direct SSH/PostgreSQL
nginx-01 -> db-primary-01 direct PostgreSQL
app nodes -> each other by default
```

## Workload strategy

OpenKoda must not determine the architecture.

The architecture requires a workload that can provide clear operational evidence for WEB/WAS/DB behavior.

The workload must support:

- HTTP health endpoint that does not require DB access
- readiness or DB-dependent endpoint that proves PostgreSQL connectivity
- application logs that show request handling and DB access failures
- predictable process management through systemd or Docker
- simple deployment and restart behavior

### Option A: OpenKoda as the workload

OpenKoda may be used only if it can be configured in a way that clearly separates:

```text
nginx-01 -> app-01/app-02 -> db-primary-01
```

If OpenKoda requires excessive customization or hides the operational evidence, it should not be forced into this role.

### Option B: purpose-built operations workload

If OpenKoda is not suitable for clean WEB/WAS/DB evidence, add or implement a small Spring Boot operations workload.

This workload should be intentionally boring:

```text
GET /health          returns application process health
GET /ready           checks PostgreSQL connectivity
GET /api/items       reads from PostgreSQL
POST /api/items      writes to PostgreSQL
GET /api/slow        optional latency simulation for later performance tests
```

The purpose-built workload is not the portfolio subject either. It is a controllable workload for operating-system, network, database, log, and recovery evidence.

## Nginx design

`nginx-01` should define an upstream group:

```nginx
upstream app_backend {
    server app-01:8080 max_fails=2 fail_timeout=10s;
    server app-02:8080 max_fails=2 fail_timeout=10s;
}
```

The exact names may be implemented through private IPs or `/etc/hosts` depending on the Ansible design.

Required log evidence:

```text
/var/log/nginx/access.log
/var/log/nginx/error.log
```

Required checks:

```bash
curl -i http://nginx-01/health
curl -i http://nginx-01/ready
curl -i http://nginx-01/api/items
```

The final implementation should capture response codes, upstream response time, and failure behavior when one app node is stopped.

## App tier design

Each app node should expose the same service on port `8080`.

Required process evidence:

```bash
systemctl status <app-service>
ss -lntp | grep 8080
curl -i http://localhost:8080/health
curl -i http://localhost:8080/ready
```

Required log evidence depends on packaging, but must be documented. Examples:

```text
/var/log/<app-service>/application.log
journalctl -u <app-service>
```

## DB tier design

`db-primary-01` runs PostgreSQL.

For `lab-full-min`, standby replication is not yet required. The goal is to prove clean DB separation and DB-dependent application behavior.

Required evidence:

```bash
systemctl status postgresql
ss -lntp | grep 5432
sudo -u postgres psql -c "select version();"
```

From app nodes:

```bash
pg_isready -h <db-private-ip> -p 5432
```

The application readiness endpoint must fail if PostgreSQL is unavailable or the DB credentials are wrong.

## First incident scenario

The first incident scenario should validate WEB/WAS failover behavior.

```text
1. Confirm normal state through nginx-01.
2. Stop app-01 application service.
3. Send repeated requests to nginx-01.
4. Confirm traffic still reaches app-02.
5. Collect Nginx access/error logs.
6. Collect app-01 service status and app-02 access evidence.
7. Restart app-01 service.
8. Confirm both app nodes are usable again.
```

This is not a container restart demo. The point is to show that the operator can observe a WEB/WAS tier failure, narrow the failing component, and verify service continuity through the remaining app node.

## Evidence targets

The implementation should later produce evidence under:

```text
docs/03-evidence/YYYY-MM-DD-lab-full-min-web-was-db/
```

Minimum evidence fields:

```text
nginx_node=nginx-01
app_nodes=app-01,app-02
db_node=db-primary-01
app_01_private=true
app_02_private=true
db_private=true
nginx_to_app_health=success
db_dependent_readiness=success
app_01_stopped=true
nginx_continued_serving=true
app_01_recovered=true
```

## Implementation order

1. Terraform `lab-full-min` baseline
2. Ansible inventory template for `lab-full-min`
3. PostgreSQL install/configuration on `db-primary-01`
4. App workload deploy to `app-01` and `app-02`
5. Nginx reverse proxy configuration on `nginx-01`
6. Baseline evidence collection
7. App-node failure scenario
8. Recovery evidence collection

## Non-goals for this phase

- PostgreSQL standby promotion
- NFS or shared file storage
- Prometheus/Grafana/Loki
- Restic/pg_dump restore lab
- production high availability
- public load balancer design
- OpenKoda feature customization
