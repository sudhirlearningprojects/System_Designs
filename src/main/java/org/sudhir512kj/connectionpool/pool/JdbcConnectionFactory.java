package org.sudhir512kj.connectionpool.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class JdbcConnectionFactory implements ConnectionFactory {

    private static final Logger log = LoggerFactory.getLogger(JdbcConnectionFactory.class);

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public JdbcConnectionFactory(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    @Override
    public Connection createConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
        conn.setAutoCommit(true);
        log.debug("Created new connection to {}", jdbcUrl);
        return conn;
    }

    @Override
    public boolean validateConnection(Connection connection, String validationQuery) {
        try {
            if (connection == null || connection.isClosed()) return false;
            try (Statement stmt = connection.createStatement()) {
                stmt.setQueryTimeout(5);
                stmt.execute(validationQuery);
            }
            return true;
        } catch (SQLException e) {
            log.debug("Connection validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void destroyConnection(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                log.debug("Destroyed connection");
            }
        } catch (SQLException e) {
            log.warn("Error closing connection: {}", e.getMessage());
        }
    }
}
