package com.seekora.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Loads db.properties and exposes a pooled {@link DataSource}.
 * Values may be overridden with the environment variables
 * SEEKORA_DB_URL, SEEKORA_DB_USER and SEEKORA_DB_PASSWORD.
 */
public final class DatabaseConfig {

    private static final String PROPERTIES_FILE = "db.properties";
    private static volatile HikariDataSource dataSource;

    private DatabaseConfig() {
    }

    public static synchronized void init() {
        if (dataSource != null) {
            return;
        }
        Properties props = loadProperties();

        String url = env("SEEKORA_DB_URL", props.getProperty("db.url"));
        String user = env("SEEKORA_DB_USER", props.getProperty("db.username"));
        String password = env("SEEKORA_DB_PASSWORD", props.getProperty("db.password"));

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName(props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
        config.setMaximumPoolSize(intProp(props, "db.pool.maxSize", 10));
        config.setMinimumIdle(intProp(props, "db.pool.minIdle", 2));
        config.setConnectionTimeout(intProp(props, "db.pool.connectionTimeoutMs", 10000));
        config.setPoolName("seekora-pool");
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        if (dataSource == null) {
            init();
        }
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    public static synchronized void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = DatabaseConfig.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (in == null) {
                throw new IllegalStateException(PROPERTIES_FILE + " not found on the classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + PROPERTIES_FILE, e);
        }
        return props;
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static int intProp(Properties props, String key, int fallback) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
