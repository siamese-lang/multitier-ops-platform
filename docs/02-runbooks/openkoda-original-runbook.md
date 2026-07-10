# OpenKoda Upstream Execution Runbook

## 1. Purpose

Verify that the upstream OpenKoda application can be built and executed before any operational modifications are introduced by this project.

The result of this runbook becomes the baseline for later operational hardening tasks such as liveness/readiness endpoints, metrics, request ID propagation, filesystem storage, and controlled fault injection.

## 2. Scope

This runbook covers only upstream execution verification. It does not cover Terraform deployment, Ansible configuration, production hardening, or application code modification.

Two execution paths are recognized:

1. Docker Compose quick-start verification.
2. Source build and JVM execution verification.

For this project, the source-build path is more important because later EC2 lab environments will run OpenKoda as a service behind Nginx with an external PostgreSQL instance.

## 3. Upstream source information

| Item | Value |
|---|---|
| Repository URL | `https://github.com/openkoda/openkoda` |
| Repository full name | `openkoda/openkoda` |
| Clone URL | `https://github.com/openkoda/openkoda.git` |
| License | MIT License |
| Copyright notice | `Copyright (c) 2023 openkoda` |
| Checked reference | Upstream `main` branch documentation reviewed on 2026-07-10 KST |
| Product version shown in README badge | Openkoda 1.7.1 |
| Java version shown in README badge | Java 17.0.2 |
| Spring Boot version shown in README badge | Spring Boot 3.0.5 |

Before running the application, record the exact commit:

```bash
git clone https://github.com/openkoda/openkoda.git
cd openkoda
git rev-parse HEAD
```

## 4. Local prerequisites

| Requirement | Version / Value | Evidence command |
|---|---|---|
| OS | Linux recommended; upstream installation document references Ubuntu 18.04.1 LTS or higher | `cat /etc/os-release` |
| Java | Java 17 | `java -version` |
| Maven | Maven 3.8+ | `mvn -version` |
| PostgreSQL | PostgreSQL 14+ | `psql --version` or `docker exec postgres-db psql --version` |
| Docker Compose quick-start | Docker Compose installed | `docker version` and `docker compose version` |
| Source runtime port | Example: `8030` | `ss -lntp` |
| Docker Compose runtime port | `8080` mapped as `8080:8080` | `docker ps` and `ss -lntp` |

## 5. Docker Compose quick-start verification

This path verifies the upstream packaged image and compose defaults. It is useful as a baseline smoke test, but it is not the final EC2 operating model for this project.

```bash
mkdir -p /tmp/openkoda-compose-test
cd /tmp/openkoda-compose-test
curl -fsSL https://raw.githubusercontent.com/openkoda/openkoda/main/docker/docker-compose.yaml -o docker-compose.yaml
SECURE_COOKIE=false docker compose -f docker-compose.yaml up
```

Default upstream compose values include:

| Variable | Default |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/openkoda` |
| `SPRING_DATASOURCE_USERNAME` | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` |
| `BASE_URL` | `http://localhost:8080` |
| `INIT_ADMIN_USERNAME` | `admin` |
| `INIT_ADMIN_PASSWORD` | `admin123` |
| `FILE_STORAGE_FILESYSTEM_PATH` | `/data` |
| `SPRING_PROFILES_ACTIVE` | `openkoda` |
| `STORAGE_TYPE` | `database` |
| `SECURE_COOKIE` | `true` |

Verification commands:

```bash
docker ps
docker logs openkoda --tail 200
docker logs postgres-db --tail 100
curl -k -I https://localhost:8080 || true
curl -I http://localhost:8080 || true
```

Expected access information from upstream README:

- URL: `https://localhost:8080`
- Admin login: `admin`
- Admin password: `admin123`

Do not treat default credentials as acceptable for later EC2 environments. They are only for upstream baseline verification.

## 6. Source build procedure

```bash
git clone https://github.com/openkoda/openkoda.git
cd openkoda
git rev-parse HEAD
mvn -f openkoda/pom.xml clean install spring-boot:repackage -DskipTests
```

The upstream README shows the repackaged runtime artifact as `build/openkoda.jar`, while the installation guide examples use `openkoda.jar`. During verification, confirm the actual artifact path after the Maven build:

```bash
find . -name 'openkoda.jar' -o -name '*.jar' | sort | tail -50
```

Record the exact path used for execution in the evidence directory.

## 7. PostgreSQL initialization procedure

For a local source-build test, create a PostgreSQL database before first application startup.

Example using local PostgreSQL:

```bash
sudo -u postgres psql -c "CREATE DATABASE openkoda;"
sudo -u postgres psql -c "ALTER ROLE postgres WITH PASSWORD 'postgres';"
```

Example using Docker PostgreSQL:

```bash
docker run --name openkoda-postgres \
  -e POSTGRES_DB=openkoda \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgres:14.2

docker logs openkoda-postgres --tail 100
```

The first OpenKoda run should use the database initialization profile:

```bash
java \
  -Dloader.path=/BOOT-INF/classes \
  -Dspring.profiles.active=openkoda,drop_and_init_database \
  -jar <OPENKODA_JAR_PATH> \
  --server.port=8030
```

Stop the process after initialization completes and the application has started successfully.

## 8. Application startup procedure

After database initialization, run OpenKoda with the normal profile:

```bash
java \
  -Dloader.path=/BOOT-INF/classes \
  -Dsecure.cookie=false \
  -jar <OPENKODA_JAR_PATH> \
  --spring.profiles.active=openkoda \
  --server.port=8030
```

If the database connection is not using defaults, provide datasource properties explicitly using environment variables or JVM/application arguments. Record the exact method used.

## 9. Verification procedure

### Process check

```bash
ps -ef | grep '[j]ava'
```

### Port check

```bash
ss -lntp | grep -E '8030|8080|5432'
```

### HTTP access check

For source execution:

```bash
curl -I http://localhost:8030
curl -L http://localhost:8030 | head -50
```

For Docker Compose quick start:

```bash
curl -k -I https://localhost:8080
curl -k -L https://localhost:8080 | head -50
```

### Log check

```bash
# Source execution: save console output to a log file when running the java command.
# Docker execution:
docker logs openkoda --tail 200
```

### Database connection check

```bash
psql -h localhost -U postgres -d openkoda -c '\dt' || true
# or, for Docker PostgreSQL:
docker exec -it openkoda-postgres psql -U postgres -d openkoda -c '\dt'
```

## 10. Evidence to collect

Store evidence under:

```text
docs/03-evidence/openkoda-original-run/
```

Required evidence:

- `cat /etc/os-release`
- `git rev-parse HEAD`
- `java -version`
- `mvn -version`
- Docker/Docker Compose version, if using compose
- PostgreSQL version and status
- build command output summary
- database initialization log
- application startup log
- process check
- port check
- HTTP access result
- failure log if execution fails

Suggested files:

```text
docs/03-evidence/openkoda-original-run/
├── README.md
├── 01-environment.txt
├── 02-upstream-commit.txt
├── 03-build-output.txt
├── 04-db-init-log.txt
├── 05-app-startup-log.txt
├── 06-process-and-port-check.txt
├── 07-http-check.txt
└── 08-troubleshooting-notes.md
```

## 11. Known issues and watch points

- The upstream quick-start URL uses HTTPS on `localhost:8080`.
- OpenKoda uses secure cookies by default. For development/test runs without HTTPS, set `SECURE_COOKIE=false` or use `-Dsecure.cookie=false`.
- Docker Compose defaults are convenient but use default credentials; do not reuse them in AWS labs.
- The upstream README and installation guide differ slightly in jar path examples. Confirm the actual build artifact path during verification.
- The final project should not rely on Docker Compose as the production-like topology. It is only a baseline check.

## 12. Result summary

| Item | Result |
|---|---|
| Build | `<PASS/FAIL>` |
| DB initialization | `<PASS/FAIL>` |
| DB connection | `<PASS/FAIL>` |
| Application startup | `<PASS/FAIL>` |
| HTTP access | `<PASS/FAIL>` |
| Notes | `<TO_BE_FILLED_AFTER_MANUAL_RUN>` |