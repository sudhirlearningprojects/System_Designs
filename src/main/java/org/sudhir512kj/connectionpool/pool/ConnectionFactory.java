package org.sudhir512kj.connectionpool.pool;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionFactory {

    Connection createConnection() throws SQLException;

    boolean validateConnection(Connection connection, String validationQuery);

    void destroyConnection(Connection connection);
}
