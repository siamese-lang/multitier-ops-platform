package io.github.siamese_lang.ops;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.stereotype.Repository;

@Repository
public class WorkOrderRepository {

    private static final List<String> ALLOWED_STATUSES = List.of("OPEN", "IN_PROGRESS", "DONE", "FAILED", "CANCELLED");
    private static final List<String> ALLOWED_PRIORITIES = List.of("LOW", "NORMAL", "HIGH", "CRITICAL");

    private final DbSettings dbSettings;

    public WorkOrderRepository(DbSettings dbSettings) {
        this.dbSettings = dbSettings;
    }

    public List<Map<String, Object>> findAll(String status) throws SQLException {
        try (Connection connection = dbSettings.openConnection()) {
            ensureSchemaAndSeed(connection);
            if (status == null || status.isBlank()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                    select id, title, status, priority, requester, assignee, description, created_at, updated_at
                    from ops_work_orders
                    order by id
                    """)) {
                    return readWorkOrderRows(statement);
                }
            }

            try (PreparedStatement statement = connection.prepareStatement("""
                select id, title, status, priority, requester, assignee, description, created_at, updated_at
                from ops_work_orders
                where status = ?
                order by id
                """)) {
                statement.setString(1, normalizeStatus(status));
                return readWorkOrderRows(statement);
            }
        }
    }

    public Map<String, Object> findById(long id) throws SQLException {
        try (Connection connection = dbSettings.openConnection()) {
            ensureSchemaAndSeed(connection);
            return findById(connection, id);
        }
    }

    public Map<String, Object> create(CreateWorkOrderRequest request) throws SQLException {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }

        String title = requiredText(request.title(), "title");
        String priority = normalizePriority(request.priority() == null ? "NORMAL" : request.priority());
        String requester = blankToDefault(request.requester(), "operator");
        String assignee = blankToNull(request.assignee());
        String description = blankToNull(request.description());
        String actor = requester;

        try (Connection connection = dbSettings.openConnection()) {
            ensureSchemaAndSeed(connection);
            try (PreparedStatement statement = connection.prepareStatement("""
                insert into ops_work_orders (title, status, priority, requester, assignee, description)
                values (?, 'OPEN', ?, ?, ?, ?)
                returning id
                """)) {
                statement.setString(1, title);
                statement.setString(2, priority);
                statement.setString(3, requester);
                setNullableString(statement, 4, assignee);
                setNullableString(statement, 5, description);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    long workOrderId = resultSet.getLong(1);
                    insertEvent(
                        connection,
                        workOrderId,
                        "CREATED",
                        null,
                        "OPEN",
                        defaultMessage(description, "work order created"),
                        actor
                    );
                    insertAuditLog(
                        connection,
                        actor,
                        "work_order.create",
                        "work_order",
                        workOrderId,
                        "created",
                        "work order created"
                    );
                    return findById(connection, workOrderId);
                }
            }
        }
    }

    public Map<String, Object> updateStatus(long id, UpdateStatusRequest request) throws SQLException {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }

        String status = normalizeStatus(request.status());
        String actor = blankToDefault(request.actor(), "operator");
        String message = blankToDefault(request.message(), "status changed to " + status);

        try (Connection connection = dbSettings.openConnection()) {
            ensureSchemaAndSeed(connection);
            Map<String, Object> current = findById(connection, id);
            if (current == null) {
                return null;
            }

            String fromStatus = (String) current.get("status");
            try (PreparedStatement statement = connection.prepareStatement("""
                update ops_work_orders
                set status = ?, updated_at = now()
                where id = ?
                """)) {
                statement.setString(1, status);
                statement.setLong(2, id);
                statement.executeUpdate();
            }

            insertEvent(
                connection,
                id,
                fromStatus.equals(status) ? "STATUS_CONFIRMED" : "STATUS_CHANGED",
                fromStatus,
                status,
                message,
                actor
            );
            insertAuditLog(
                connection,
                actor,
                "work_order.update_status",
                "work_order",
                id,
                "updated",
                fromStatus + " -> " + status
            );

            return findById(connection, id);
        }
    }

    public Map<String, Object> summary() throws SQLException {
        try (Connection connection = dbSettings.openConnection()) {
            ensureSchemaAndSeed(connection);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("total", count(connection, null));
            for (String status : ALLOWED_STATUSES) {
                response.put(status.toLowerCase(Locale.ROOT), count(connection, status));
            }
            return response;
        }
    }

    public List<Map<String, Object>> findEventsByWorkOrderId(long workOrderId) throws SQLException {
        try (Connection connection = dbSettings.openConnection()) {
            ensureSchemaAndSeed(connection);
            try (PreparedStatement statement = connection.prepareStatement("""
                select id, work_order_id, event_type, from_status, to_status, message, actor, created_at
                from ops_work_order_events
                where work_order_id = ?
                order by id
                """)) {
                statement.setLong(1, workOrderId);
                List<Map<String, Object>> rows = new ArrayList<>();
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        rows.add(mapEventRow(resultSet));
                    }
                }
                return rows;
            }
        }
    }

    public List<Map<String, Object>> findRecentAuditLogs(int limit) throws SQLException {
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        try (Connection connection = dbSettings.openConnection()) {
            ensureSchemaAndSeed(connection);
            try (PreparedStatement statement = connection.prepareStatement("""
                select id, actor, action, target_type, target_id, result, request_id, message, created_at
                from ops_operation_audit_logs
                order by id desc
                limit ?
                """)) {
                statement.setInt(1, boundedLimit);
                List<Map<String, Object>> rows = new ArrayList<>();
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        rows.add(mapAuditLogRow(resultSet));
                    }
                }
                return rows;
            }
        }
    }

    private Map<String, Object> findById(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            select id, title, status, priority, requester, assignee, description, created_at, updated_at
            from ops_work_orders
            where id = ?
            """)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapWorkOrderRow(resultSet);
            }
        }
    }

    private void ensureSchemaAndSeed(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                create table if not exists ops_work_orders (
                    id bigserial primary key,
                    title varchar(200) not null,
                    status varchar(30) not null,
                    priority varchar(30) not null,
                    requester varchar(100),
                    assignee varchar(100),
                    description text,
                    created_at timestamptz not null default now(),
                    updated_at timestamptz not null default now()
                )
                """);
            statement.execute("alter table ops_work_orders add column if not exists requester varchar(100)");
            statement.execute("""
                create table if not exists ops_work_order_events (
                    id bigserial primary key,
                    work_order_id bigint not null references ops_work_orders(id) on delete cascade,
                    event_type varchar(50) not null,
                    from_status varchar(30),
                    to_status varchar(30),
                    message text,
                    actor varchar(100),
                    created_at timestamptz not null default now()
                )
                """);
            statement.execute("""
                create index if not exists idx_ops_work_order_events_work_order_id
                on ops_work_order_events(work_order_id, id)
                """);
            statement.execute("""
                create table if not exists ops_operation_audit_logs (
                    id bigserial primary key,
                    actor varchar(100),
                    action varchar(100) not null,
                    target_type varchar(100) not null,
                    target_id bigint,
                    result varchar(50) not null,
                    request_id varchar(100),
                    message text,
                    created_at timestamptz not null default now()
                )
                """);
            statement.execute("""
                create index if not exists idx_ops_operation_audit_logs_created_at
                on ops_operation_audit_logs(created_at desc, id desc)
                """);
        }

        if (count(connection, null) > 0) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("""
            insert into ops_work_orders (title, status, priority, requester, assignee, description)
            values
              ('Confirm Nginx upstream reaches app nodes', 'OPEN', 'HIGH', 'operator', 'ops-admin', 'Use this record while validating nginx-01 to app-01/app-02 routing.'),
              ('Verify DB readiness endpoint', 'IN_PROGRESS', 'HIGH', 'operator', 'ops-admin', 'Use /readyz and /db/time to confirm PostgreSQL connectivity.'),
              ('Collect baseline node identity evidence', 'DONE', 'NORMAL', 'operator', 'ops-admin', 'Use /node to prove which app instance served the request.'),
              ('Prepare app-01 failure drill', 'OPEN', 'CRITICAL', 'operator', 'ops-admin', 'Stop app-01 later and verify app-02 continues to serve the same data.'),
              ('Check recovery after PostgreSQL restart', 'OPEN', 'NORMAL', 'operator', 'ops-admin', 'Use this record for DB restart and readiness recovery evidence.')
            returning id, status, assignee, description
            """)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    long workOrderId = resultSet.getLong("id");
                    String status = resultSet.getString("status");
                    insertEvent(connection, workOrderId, "SEEDED", null, status, resultSet.getString("description"), "system-seed");
                    insertAuditLog(connection, "system-seed", "work_order.seed", "work_order", workOrderId, "seeded", "seed work order created");
                }
            }
        }
    }

    private long count(Connection connection, String status) throws SQLException {
        if (status == null) {
            try (PreparedStatement statement = connection.prepareStatement("select count(*) from ops_work_orders")) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getLong(1);
                }
            }
        }

        try (PreparedStatement statement = connection.prepareStatement("select count(*) from ops_work_orders where status = ?")) {
            statement.setString(1, status);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private void insertEvent(
        Connection connection,
        long workOrderId,
        String eventType,
        String fromStatus,
        String toStatus,
        String message,
        String actor
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            insert into ops_work_order_events (work_order_id, event_type, from_status, to_status, message, actor)
            values (?, ?, ?, ?, ?, ?)
            """)) {
            statement.setLong(1, workOrderId);
            statement.setString(2, eventType);
            setNullableString(statement, 3, fromStatus);
            setNullableString(statement, 4, toStatus);
            setNullableString(statement, 5, message);
            setNullableString(statement, 6, actor);
            statement.executeUpdate();
        }
    }

    private void insertAuditLog(
        Connection connection,
        String actor,
        String action,
        String targetType,
        long targetId,
        String result,
        String message
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            insert into ops_operation_audit_logs (actor, action, target_type, target_id, result, request_id, message)
            values (?, ?, ?, ?, ?, ?, ?)
            """)) {
            setNullableString(statement, 1, actor);
            statement.setString(2, action);
            statement.setString(3, targetType);
            statement.setLong(4, targetId);
            statement.setString(5, result);
            setNullableString(statement, 6, currentRequestId());
            setNullableString(statement, 7, message);
            statement.executeUpdate();
        }
    }

    private static List<Map<String, Object>> readWorkOrderRows(PreparedStatement statement) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                rows.add(mapWorkOrderRow(resultSet));
            }
        }
        return rows;
    }

    private static Map<String, Object> mapWorkOrderRow(ResultSet resultSet) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", resultSet.getLong("id"));
        row.put("title", resultSet.getString("title"));
        row.put("status", resultSet.getString("status"));
        row.put("priority", resultSet.getString("priority"));
        row.put("requester", resultSet.getString("requester"));
        row.put("assignee", resultSet.getString("assignee"));
        row.put("description", resultSet.getString("description"));
        row.put("createdAt", resultSet.getString("created_at"));
        row.put("updatedAt", resultSet.getString("updated_at"));
        return row;
    }

    private static Map<String, Object> mapEventRow(ResultSet resultSet) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", resultSet.getLong("id"));
        row.put("workOrderId", resultSet.getLong("work_order_id"));
        row.put("eventType", resultSet.getString("event_type"));
        row.put("fromStatus", resultSet.getString("from_status"));
        row.put("toStatus", resultSet.getString("to_status"));
        row.put("message", resultSet.getString("message"));
        row.put("actor", resultSet.getString("actor"));
        row.put("createdAt", resultSet.getString("created_at"));
        return row;
    }

    private static Map<String, Object> mapAuditLogRow(ResultSet resultSet) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", resultSet.getLong("id"));
        row.put("actor", resultSet.getString("actor"));
        row.put("action", resultSet.getString("action"));
        row.put("targetType", resultSet.getString("target_type"));
        row.put("targetId", resultSet.getLong("target_id"));
        row.put("result", resultSet.getString("result"));
        row.put("requestId", resultSet.getString("request_id"));
        row.put("message", resultSet.getString("message"));
        row.put("createdAt", resultSet.getString("created_at"));
        return row;
    }

    private static String normalizeStatus(String status) {
        String normalized = requiredText(status, "status").toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("unsupported status: " + status);
        }
        return normalized;
    }

    private static String normalizePriority(String priority) {
        String normalized = requiredText(priority, "priority").toUpperCase(Locale.ROOT);
        if (!ALLOWED_PRIORITIES.contains(normalized)) {
            throw new IllegalArgumentException("unsupported priority: " + priority);
        }
        return normalized;
    }

    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String blankToDefault(String value, String defaultValue) {
        String normalized = blankToNull(value);
        return normalized == null ? defaultValue : normalized;
    }

    private static String defaultMessage(String value, String defaultValue) {
        String normalized = blankToNull(value);
        return normalized == null ? defaultValue : normalized;
    }

    private static String currentRequestId() {
        return blankToNull(MDC.get("requestId"));
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    public record CreateWorkOrderRequest(String title, String priority, String assignee, String requester, String description) {
    }

    public record UpdateStatusRequest(String status, String message, String actor) {
    }
}
