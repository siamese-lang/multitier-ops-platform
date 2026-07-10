# OpenKoda Upstream Execution Runbook

## 1. Purpose

Verify that the upstream OpenKoda application can be built and executed before any operational modifications are introduced by this project.

## 2. Scope

This runbook covers only upstream execution verification. It does not cover Terraform deployment, Ansible configuration, production hardening, or application code modification.

## 3. Upstream source information

| Item | Value |
|---|---|
| Repository URL | `<TO_BE_FILLED>` |
| License | `<TO_BE_FILLED>` |
| Checked commit or release | `<TO_BE_FILLED>` |
| Verification date | `<TO_BE_FILLED>` |

## 4. Local prerequisites

| Requirement | Version / Value | Evidence |
|---|---|---|
| OS | `<TO_BE_FILLED>` | `<TO_BE_FILLED>` |
| Java | `<TO_BE_FILLED>` | `java -version` |
| Build tool | `<TO_BE_FILLED>` | `mvn -version` or equivalent |
| Database | `<TO_BE_FILLED>` | DB status/log |
| Required ports | `<TO_BE_FILLED>` | `ss -lntp` |

## 5. Build procedure

```bash
git clone <UPSTREAM_REPOSITORY_URL>
cd <OPENKODA_SOURCE_DIR>
git rev-parse HEAD
<BUILD_COMMAND_TO_BE_FILLED>
```

## 6. Database initialization procedure

```bash
<DB_INIT_COMMAND_TO_BE_FILLED>
```

## 7. Application startup procedure

```bash
<APP_START_COMMAND_TO_BE_FILLED>
```

## 8. Verification procedure

### Process check

```bash
ps -ef | grep java
```

### Port check

```bash
ss -lntp
```

### HTTP access check

```bash
curl -I http://localhost:<PORT>
curl http://localhost:<PORT>
```

### Log check

```bash
<LOG_COMMAND_TO_BE_FILLED>
```

### Database connection check

```bash
<DB_CONNECTION_CHECK_TO_BE_FILLED>
```

## 9. Evidence to collect

Store evidence under:

```text
docs/03-evidence/openkoda-original-run/
```

Required evidence:

- source commit hash
- Java version
- build command output
- database status/log
- application startup log
- process check
- port check
- HTTP access result
- failure log if execution fails

## 10. Known issues

- `<TO_BE_FILLED>`

## 11. Result summary

| Item | Result |
|---|---|
| Build | `<PASS/FAIL>` |
| DB connection | `<PASS/FAIL>` |
| Application startup | `<PASS/FAIL>` |
| HTTP access | `<PASS/FAIL>` |
| Notes | `<TO_BE_FILLED>` |
