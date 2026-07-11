package io.github.siamese_lang.ops;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DbSettings {

    private static final String DB_URL_ENV = "OPS_DB_URL";
    private static final String DB_USERNAME_ENV = "OPS_DB_USERNAME";
    private static final String DB_PASSWORD_ENV = "OPS_DB_PASSWORD";

    public Connection openConnection() throws SQLException {
        String url = System.getenv(DB_URL_ENV);
        String username = System.getenv(DB_USERNAME_ENV);
        String password = System.getenv(DB_PASSWORD_ENV);

        if (isBlank(url) || isBlank(username)) {
            throw new IllegalStateException("database environment is incomplete");
        }

        return DriverManager.getConnection(url, username, password);
    }

    public Map<String, Object> requiredEnvironment() {
        Map<String, Object> env = new LinkedHashMap<>();
        env.put("requiredUrlEnv", DB_URL_ENV);
        env.put("requiredUsernameEnv", DB_USERNAME_ENV);
        env.put("passwordEnv", DB_PASSWORD_ENV);
        return env;
    }

    public String maskedUrl() {
        return maskUrl(System.getenv(DB_URL_ENV));
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
}
