package io.github.siamese_lang.ops;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class WorkOrderEvidenceRepository {

    private final DbSettings dbSettings;

    public WorkOrderEvidenceRepository(DbSettings dbSettings) {
        this.dbSettings = dbSettings;
    }

    public Map<String, Object> createMetadata(CreateEvidenceMetadataRequest request) throws SQLException {
        try (Connection connection = dbSettings.openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement("""
                insert into ops_work_order_evidence_files (
                    work_order_id,
                    file_name,
                    storage_path,
                    size_bytes,
                    sha256,
                    created_by_node
                )
                values (?, ?, ?, ?, ?, ?)
                returning id, work_order_id, file_name, storage_path, size_bytes, sha256, created_by_node, created_at
                """)) {
                statement.setLong(1, request.workOrderId());
                statement.setString(2, request.fileName());
                statement.setString(3, request.storagePath());
                statement.setLong(4, request.sizeBytes());
                statement.setString(5, request.sha256());
                statement.setString(6, request.createdByNode());

                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return mapRow(resultSet);
                }
            }
        }
    }

    public List<Map<String, Object>> findByWorkOrderId(long workOrderId) throws SQLException {
        try (Connection connection = dbSettings.openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement("""
                select id, work_order_id, file_name, storage_path, size_bytes, sha256, created_by_node, created_at
                from ops_work_order_evidence_files
                where work_order_id = ?
                order by id
                """)) {
                statement.setLong(1, workOrderId);
                List<Map<String, Object>> rows = new ArrayList<>();
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        rows.add(mapRow(resultSet));
                    }
                }
                return rows;
            }
        }
    }

    public Map<String, Object> findByWorkOrderIdAndEvidenceId(long workOrderId, long evidenceId) throws SQLException {
        try (Connection connection = dbSettings.openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement("""
                select id, work_order_id, file_name, storage_path, size_bytes, sha256, created_by_node, created_at
                from ops_work_order_evidence_files
                where work_order_id = ? and id = ?
                """)) {
                statement.setLong(1, workOrderId);
                statement.setLong(2, evidenceId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    return mapRow(resultSet);
                }
            }
        }
    }

    private void ensureSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                create table if not exists ops_work_order_evidence_files (
                    id bigserial primary key,
                    work_order_id bigint not null references ops_work_orders(id) on delete cascade,
                    file_name varchar(255) not null,
                    storage_path text not null,
                    size_bytes bigint not null,
                    sha256 char(64) not null,
                    created_by_node varchar(200) not null,
                    created_at timestamptz not null default now(),
                    unique (storage_path)
                )
                """);
        }
    }

    private static Map<String, Object> mapRow(ResultSet resultSet) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", resultSet.getLong("id"));
        row.put("workOrderId", resultSet.getLong("work_order_id"));
        row.put("fileName", resultSet.getString("file_name"));
        row.put("storagePath", resultSet.getString("storage_path"));
        row.put("sizeBytes", resultSet.getLong("size_bytes"));
        row.put("sha256", resultSet.getString("sha256"));
        row.put("createdByNode", resultSet.getString("created_by_node"));
        row.put("createdAt", resultSet.getString("created_at"));
        return row;
    }

    public record CreateEvidenceMetadataRequest(
        long workOrderId,
        String fileName,
        String storagePath,
        long sizeBytes,
        String sha256,
        String createdByNode
    ) {
    }
}
