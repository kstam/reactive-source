/*******************************************************************************
 * Copyright (c) 2013-2014 eBay Software Foundation
 *
 * See the file license.txt for copying permission.
 ******************************************************************************/

package org.reactivesource.mysql;

import org.apache.commons.io.IOUtils;
import org.reactivesource.ConnectionProvider;
import org.reactivesource.util.JdbcUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.reactivesource.mysql.ConnectionConstants.*;

public class DbInitializer {

    public void setupDb() {
        try {
            ConnectionProvider provider = new MysqlConnectionProvider(URL, USERNAME, PASSWORD);
            createTestTable(provider);
            createReactiveSchema(provider);
        } catch (SQLException | IOException e) {
            throw new RuntimeException("Failed to setup db.", e);
        }
    }

    private void createReactiveSchema(ConnectionProvider provider) throws IOException, SQLException {
        try (Connection connection = provider.getConnection()) {
            String query = IOUtils.toString(getClass().getResourceAsStream("create-reactive-tables.sql"));
            JdbcUtils.sql(connection, query);
        }
    }

    private void createTestTable(ConnectionProvider provider) throws IOException, SQLException {
        try (Connection connection = provider.getConnection()) {
            String query = IOUtils.toString(getClass().getResourceAsStream("create-test-schema.sql"));
            JdbcUtils.sql(connection, query);
        }
    }
}
