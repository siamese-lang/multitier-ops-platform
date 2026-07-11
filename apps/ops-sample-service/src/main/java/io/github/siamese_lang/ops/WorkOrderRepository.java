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
import org.springframework.stereotype.Repository;

@Repository
public class WorkOrderRepository {

    private static final List<String> ALLOWED_STATUSES = List.of("OPEN", "IN_PROGRESS", "DONE", "CANCELLED");
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
                    select id, title, status, priority, assignee, description, created_at, updated_at
                    from ops_work_orders
                    order by id
                    """)) {
                    return readRows(statement);
                }
            }

            try (PreparedStatement statement = connection.prepareStatement("""
                select id, title, status, priority, assignee, description, created_at, updated_at
                from ops_work_orders
                where status = ?
                order by id
                """)) {
                statement.setString(1, normalizeStatus(status));
                return readRows(statement);
            }
        }
    }

    public Map<String, Object> findById(long id) throws SQLException {
        try (Connection connection = dbSettings.openConnection()) {
            ensureSchemaAndSeed(connection);
            try (PreparedStatement statement = connection.prepareStatement("""
                select id, title, status, priority, assignee, description, created_at, updated_at
                from ops_work_orders
                where id = ?
                """)) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    return mapRow(resultSet);
                }
            }
        }
    }

    public Map<String, Object> create(CreateWorkOrderRequest request) throws SQLException {
        String title = requiredText(request.title(), "title");
        String priority = normalizePriority(request.priority() == null ? "NORMAL" : request.priority());
        String assignee = blankToNull(request.assignee());
        String description = blankToNull(request.description());

        try (Connection connection = dbSettings.openConnection()) {
            ensureSchemaAndSeed(connection);
            try (PreparedStatement statement = connection.prepareStatement("""
                insert into ops_work_orders (title, status, priority, assignee, description)
                values (?, 'OPEN', ?, ?, ?)
                returning id
                """)) {
                statement.setString(1, title);
                statement.setString(2, priority);
                setNullableString(statement, 3, assignee);
                setNullableString(statement, 4, description);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return findById(resultSet.getLong(1));
                }
            }
        }
    }

    public Map<String, Object> updateStatus(long id, UpdateStatusRequest request) throws SQLException {
        String status = normalizeStatus(request.status());
        try (Connection connection = dbSettings.openConnection()) {
            ensureSchemaAndSeed(connection);
            try (PreparedStatement statement = connection.prepareStatement("""
                update ops_work_orders
                set status = ?, updated_at = now()
                where id = ?
                """)) {
                statement.setString(1, status);
                statement.setLong(2, id);
                int updated = statement.executeUpdate();
                if (updated == 0) {
                    return null;
                }
                return findById(id);
            }
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

    private void ensureSchemaAndSeed(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                create table if not exists ops_work_orders (
                    id bigserial primary key,
                    title varchar(200) not null,
                    status varchar(30) not null,
                    priority varchar(30) not null,
                    assignee varchar(100),
                    description text,
                    created_at timestamptz not null default now(),
                    updated_at timestamptz not null default now()
                )
                """);
        }

        if (count(connection, null) > 0) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("""
            insert into ops_work_orders (title, status, priority, assignee, description)
            values
              ('Confirm Nginx upstream reaches app nodes', 'OPEN', 'HIGH', 'ops-admin', 'Use this record while validating nginx-01 to app-01/app-02 routing.'),
              ('Verify DB readiness endpoint', 'IN_PROGRESS', 'HIGH', 'ops-admin', 'Use /readyz and /db/time to confirm PostgreSQL connectivity.'),
              ('Collect baseline node identity evidence', 'DONE', 'NORMAL', 'ops-admin', 'Use /node to prove which app instance served the request.'),
              ('Prepare app-01 failure drill', 'OPEN', 'CRITICAL', 'ops-admin', 'Stop app-01 later and verify app-02 continues to serve the same data.'),
              ('Check recovery after PostgreSQL restart', 'OPEN', 'NORMAL', 'ops-admin', 'Use this record for DB restart and readiness recovery evidence.')
            """)) {
            statement.executeUpdate();
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

    private static List<Map<String, Object>> readRows(PreparedStatement statement) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                rows.add(mapRow(resultSet));
            }
        }
        return rows;
    }

    private static Map<String, Object> mapRow(ResultSet resultSet) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", resultSet.getLong("id"));
        row.put("title", resultSet.getString("title"));
        row.put("status", resultSet.getString("status"));
        row.put("priority", resultSet.getString("priority"));
        row.put("assignee", resultSet.getString("assignee"));
        row.put("description", resultSet.getString("description"));
        row.put("createdAt", resultSet.getString("created_at"));
        row.put("updatedAt", resultSet.getString("updated_at"));
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

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    public record CreateWorkOrderRequest(String title, String priority, String assignee, String description) {
    }

    public record UpdateStatusRequest(String status) {
    }
}
