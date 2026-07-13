package io.github.siamese_lang.ops;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DbSettings {

    private static final String DB_URL_ENV = "OPS_DB_URL";
    private static final String DB_USERNAME_ENV = "OPS_DB_USERNAME";
    private static final String DB_PASSWORD_ENV = "OPS_DB_PASSWORD";
    private static final String HIKARI_MAX_POOL_SIZE_ENV = "OPS_HIKARI_MAX_POOL_SIZE";
    private static final String HIKARI_MIN_IDLE_ENV = "OPS_HIKARI_MIN_IDLE";
    private static final String HIKARI_CONNECTION_TIMEOUT_MS_ENV = "OPS_HIKARI_CONNECTION_TIMEOUT_MS";
    private static final String HIKARI_IDLE_TIMEOUT_MS_ENV = "OPS_HIKARI_IDLE_TIMEOUT_MS";
    private static final String HIKARI_MAX_LIFETIME_MS_ENV = "OPS_HIKARI_MAX_LIFETIME_MS";

    private static final String POOL_NAME = "ops-sample-service-db-pool";
    private static final int DEFAULT_MAX_POOL_SIZE = 10;
    private static final int DEFAULT_MIN_IDLE = 1;
    private static final long DEFAULT_CONNECTION_TIMEOUT_MS = 30_000L;
    private static final long DEFAULT_IDLE_TIMEOUT_MS = 600_000L;
    private static final long DEFAULT_MAX_LIFETIME_MS = 1_800_000L;

    private final Object dataSourceLock = new Object();
    private volatile HikariDataSource dataSource;

    public Connection openConnection() throws SQLException {
        return dataSource().getConnection();
    }

    public Map<String, Object> requiredEnvironment() {
        Map<String, Object> env = new LinkedHashMap<>();
        env.put("requiredUrlEnv", DB_URL_ENV);
        env.put("requiredUsernameEnv", DB_USERNAME_ENV);
        env.put("passwordEnv", DB_PASSWORD_ENV);
        env.put("hikariMaxPoolSizeEnv", HIKARI_MAX_POOL_SIZE_ENV);
        env.put("hikariMinIdleEnv", HIKARI_MIN_IDLE_ENV);
        env.put("hikariConnectionTimeoutMsEnv", HIKARI_CONNECTION_TIMEOUT_MS_ENV);
        env.put("hikariIdleTimeoutMsEnv", HIKARI_IDLE_TIMEOUT_MS_ENV);
        env.put("hikariMaxLifetimeMsEnv", HIKARI_MAX_LIFETIME_MS_ENV);
        return env;
    }

    public String maskedUrl() {
        return maskUrl(System.getenv(DB_URL_ENV));
    }

    public Map<String, Object> poolSettings() {
        int maximumPoolSize = hikariMaximumPoolSize();
        int minimumIdle = Math.min(hikariMinimumIdle(), maximumPoolSize);

        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("poolName", POOL_NAME);
        settings.put("jdbcUrl", maskedUrl());
        settings.put("maximumPoolSize", maximumPoolSize);
        settings.put("minimumIdle", minimumIdle);
        settings.put("connectionTimeoutMs", hikariConnectionTimeoutMs());
        settings.put("idleTimeoutMs", hikariIdleTimeoutMs());
        settings.put("maxLifetimeMs", hikariMaxLifetimeMs());
        settings.put("source", Map.of(
            "maximumPoolSize", HIKARI_MAX_POOL_SIZE_ENV,
            "minimumIdle", HIKARI_MIN_IDLE_ENV,
            "connectionTimeoutMs", HIKARI_CONNECTION_TIMEOUT_MS_ENV,
            "idleTimeoutMs", HIKARI_IDLE_TIMEOUT_MS_ENV,
            "maxLifetimeMs", HIKARI_MAX_LIFETIME_MS_ENV
        ));
        return settings;
    }

    public Map<String, Object> poolState() {
        HikariDataSource current = dataSource;
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("initialized", current != null);
        state.put("closed", current != null && current.isClosed());
        state.put("settings", poolSettings());

        if (current != null && !current.isClosed()) {
            HikariPoolMXBean poolBean = current.getHikariPoolMXBean();
            if (poolBean != null) {
                state.put("activeConnections", poolBean.getActiveConnections());
                state.put("idleConnections", poolBean.getIdleConnections());
                state.put("totalConnections", poolBean.getTotalConnections());
                state.put("threadsAwaitingConnection", poolBean.getThreadsAwaitingConnection());
            }
        }

        return state;
    }

    @PreDestroy
    public void close() {
        HikariDataSource current = dataSource;
        if (current != null) {
            current.close();
        }
    }

    private HikariDataSource dataSource() {
        HikariDataSource current = dataSource;
        if (current != null && !current.isClosed()) {
            return current;
        }

        synchronized (dataSourceLock) {
            current = dataSource;
            if (current == null || current.isClosed()) {
                dataSource = new HikariDataSource(hikariConfig());
            }
            return dataSource;
        }
    }

    private HikariConfig hikariConfig() {
        String url = System.getenv(DB_URL_ENV);
        String username = System.getenv(DB_USERNAME_ENV);
        String password = System.getenv(DB_PASSWORD_ENV);

        if (isBlank(url) || isBlank(username)) {
            throw new IllegalStateException("database environment is incomplete");
        }

        int maximumPoolSize = hikariMaximumPoolSize();
        int minimumIdle = Math.min(hikariMinimumIdle(), maximumPoolSize);

        HikariConfig config = new HikariConfig();
        config.setPoolName(POOL_NAME);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password == null ? "" : password);
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(hikariConnectionTimeoutMs());
        config.setIdleTimeout(hikariIdleTimeoutMs());
        config.setMaxLifetime(hikariMaxLifetimeMs());
        config.setInitializationFailTimeout(-1L);
        config.addDataSourceProperty("ApplicationName", "ops-sample-service");
        return config;
    }

    private int hikariMaximumPoolSize() {
        return intEnv(HIKARI_MAX_POOL_SIZE_ENV, DEFAULT_MAX_POOL_SIZE, 1, 100);
    }

    private int hikariMinimumIdle() {
        return intEnv(HIKARI_MIN_IDLE_ENV, DEFAULT_MIN_IDLE, 0, 100);
    }

    private long hikariConnectionTimeoutMs() {
        return longEnv(HIKARI_CONNECTION_TIMEOUT_MS_ENV, DEFAULT_CONNECTION_TIMEOUT_MS, 250L, 300_000L);
    }

    private long hikariIdleTimeoutMs() {
        return longEnv(HIKARI_IDLE_TIMEOUT_MS_ENV, DEFAULT_IDLE_TIMEOUT_MS, 10_000L, 3_600_000L);
    }

    private long hikariMaxLifetimeMs() {
        return longEnv(HIKARI_MAX_LIFETIME_MS_ENV, DEFAULT_MAX_LIFETIME_MS, 30_000L, 7_200_000L);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static int intEnv(String name, int defaultValue, int min, int max) {
        String value = System.getenv(name);
        if (isBlank(value)) {
            return defaultValue;
        }
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(value.trim())));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static long longEnv(String name, long defaultValue, long min, long max) {
        String value = System.getenv(name);
        if (isBlank(value)) {
            return defaultValue;
        }
        try {
            return Math.max(min, Math.min(max, Long.parseLong(value.trim())));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
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
