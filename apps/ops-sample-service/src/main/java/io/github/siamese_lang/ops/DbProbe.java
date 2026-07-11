package io.github.siamese_lang.ops;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DbProbe {

    private final DbSettings dbSettings;

    public DbProbe(DbSettings dbSettings) {
        this.dbSettings = dbSettings;
    }

    public DbCheckResult check() {
        try (Connection connection = dbSettings.openConnection();
             PreparedStatement statement = connection.prepareStatement("select now()")) {
            statement.setQueryTimeout(3);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                String databaseTime = resultSet.getString(1);
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("databaseTime", databaseTime);
                details.put("jdbcUrl", dbSettings.maskedUrl());
                details.put("checkedAt", OffsetDateTime.now(ZoneOffset.UTC).toString());
                return DbCheckResult.success(details);
            }
        } catch (IllegalStateException ex) {
            return DbCheckResult.failure(ex.getMessage(), dbSettings.requiredEnvironment());
        } catch (SQLException ex) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("jdbcUrl", dbSettings.maskedUrl());
            details.put("sqlState", ex.getSQLState());
            details.put("errorCode", ex.getErrorCode());
            return DbCheckResult.failure(ex.getMessage(), details);
        }
    }

    public record DbCheckResult(boolean ok, String message, Map<String, Object> details) {
        public static DbCheckResult success(Map<String, Object> details) {
            return new DbCheckResult(true, "database connection ok", details);
        }

        public static DbCheckResult failure(String message, Map<String, Object> details) {
            return new DbCheckResult(false, message, details);
        }
    }
}
