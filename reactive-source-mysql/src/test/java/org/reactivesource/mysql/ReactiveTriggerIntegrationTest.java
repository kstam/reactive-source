/*******************************************************************************
 * Copyright (c) 2013-2014 eBay Software Foundation
 *
 * See the file license.txt for copying permission.
 ******************************************************************************/

package org.reactivesource.mysql;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.reactivesource.mysql.ConnectionConstants.PASSWORD;
import static org.reactivesource.mysql.ConnectionConstants.TEST_TABLE_NAME;
import static org.reactivesource.mysql.ConnectionConstants.URL;
import static org.reactivesource.mysql.ConnectionConstants.USERNAME;
import static org.reactivesource.mysql.MysqlEventRepoUtils.getEventsForTable;
import static org.reactivesource.testing.TestConstants.INTEGRATION;

public class ReactiveTriggerIntegrationTest {

    MysqlConnectionProvider provider = new MysqlConnectionProvider(URL, USERNAME, PASSWORD);
    List<String> columnNames;

    @BeforeClass(groups = INTEGRATION)
    public void setup() throws SQLException {
        columnNames = new TableMetadata(provider).getColumnNames(TEST_TABLE_NAME);
    }

    @BeforeMethod(groups = INTEGRATION)
    public void setupDb() {
        new DbInitializer().setupDb();
    }

    @Test(groups = INTEGRATION)
    public void testAfterInsertTriggerWorksCorrectly() throws SQLException {
        ReactiveTrigger trigger = ReactiveTriggerFactory.afterInsert(TEST_TABLE_NAME, columnNames);
        try (
                Connection connection = provider.getConnection();
                Statement stmt = connection.createStatement()) {
            dropTrigger(connection, trigger);
            createTrigger(connection, trigger);

            stmt.execute("INSERT INTO " + TEST_TABLE_NAME + " VALUES (1, 'ABC')");

            List<MysqlEvent> result = getEventsForTable(TEST_TABLE_NAME, connection);

            Assert.assertEquals(result.size(), 1);
        }
    }

    @Test(groups = INTEGRATION)
    public void testAfterUpdateTriggerWorksCorrectly() throws SQLException {
        ReactiveTrigger trigger = ReactiveTriggerFactory.afterUpdate(TEST_TABLE_NAME, columnNames);
        try (
                Connection connection = provider.getConnection();
                Statement stmt = connection.createStatement()) {
            dropTrigger(connection, trigger);
            createTrigger(connection, trigger);

            stmt.execute("INSERT INTO " + TEST_TABLE_NAME + " VALUES (1, 'ABC')");
            stmt.execute("UPDATE " + TEST_TABLE_NAME + " SET TXT='CDE' WHERE ID=1");

            List<MysqlEvent> result = getEventsForTable(TEST_TABLE_NAME, connection);

            Assert.assertEquals(result.size(), 1);
        }
    }

    @Test(groups = INTEGRATION)
    public void testAfterDeleteTriggerWorksCorrectly() throws SQLException {
        ReactiveTrigger trigger = ReactiveTriggerFactory.afterDelete(TEST_TABLE_NAME, columnNames);
        try (
                Connection connection = provider.getConnection();
                Statement stmt = connection.createStatement()) {
            dropTrigger(connection, trigger);
            createTrigger(connection, trigger);

            stmt.execute("INSERT INTO " + TEST_TABLE_NAME + " VALUES (1, 'ABC')");
            stmt.execute("DELETE FROM " + TEST_TABLE_NAME + " WHERE ID=1");

            List<MysqlEvent> result = getEventsForTable(TEST_TABLE_NAME, connection);

            Assert.assertEquals(result.size(), 1);
        }
    }

    private void createTrigger(Connection connection, ReactiveTrigger trigger) throws SQLException {
        connection.createStatement().execute(trigger.getCreateSql());
    }

    private void dropTrigger(Connection connection, ReactiveTrigger trigger) throws SQLException {
        connection.createStatement().execute(trigger.getDropSql());
    }

}
