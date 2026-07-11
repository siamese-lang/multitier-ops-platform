package io.github.siamese_lang.ops;

import java.sql.Connection;
import java.sql.DriverManager;
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

    private static final String DB_URL_ENV = "OPS_DB_URL";
    private static final String DB_USERNAME_ENV = "OPS_DB_USERNAME";
    private static final String DB_PASSWORD_ENV = "OPS_DB_PASSWORD";

    public DbCheckResult check() {
        String url = System.getenv(DB_URL_ENV);
        String username = System.getenv(DB_USERNAME_ENV);
        String password = System.getenv(DB_PASSWORD_ENV);

        if (isBlank(url) || isBlank(username)) {
            return DbCheckResult.failure(
                "database environment is incomplete",
                Map.of(
                    "requiredUrlEnv", DB_URL_ENV,
                    "requiredUsernameEnv", DB_USERNAME_ENV,
                    "passwordEnv", DB_PASSWORD_ENV
                )
            );
        }

        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement("select now()")) {
            statement.setQueryTimeout(3);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                String databaseTime = resultSet.getString(1);
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("databaseTime", databaseTime);
                details.put("jdbcUrl", maskUrl(url));
                details.put("checkedAt", OffsetDateTime.now(ZoneOffset.UTC).toString());
                return DbCheckResult.success(details);
            }
        } catch (SQLException ex) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("jdbcUrl", maskUrl(url));
            details.put("sqlState", ex.getSQLState());
            details.put("errorCode", ex.getErrorCode());
            return DbCheckResult.failure(ex.getMessage(), details);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String maskUrl(String url) {
        if (url == null) {
            return "not-set";
        }
        int credentialsMarker = url.indexOf("//");
        int atMarker = url.indexOf('@');
        if (credentialsMarker >= 0 && atMarker > credentialsMarker) {
            return url.substring(0, credentialsMarker + 2) + "***@" + url.substring(atMarker + 1);
        }
        return url;
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
