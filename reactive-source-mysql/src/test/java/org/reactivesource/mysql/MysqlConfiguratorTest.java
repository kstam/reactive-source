/*******************************************************************************
 * Copyright (c) 2013-2014 eBay Software Foundation
 *
 * See the file license.txt for copying permission.
 ******************************************************************************/

package org.reactivesource.mysql;

import org.mockito.Mock;
import org.reactivesource.ConnectionProvider;
import org.reactivesource.EventType;
import org.reactivesource.exceptions.ConfigurationException;
import org.reactivesource.util.JdbcUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.reactivesource.mysql.ConnectionConstants.PASSWORD;
import static org.reactivesource.mysql.ConnectionConstants.TEST_TABLE_NAME;
import static org.reactivesource.mysql.ConnectionConstants.URL;
import static org.reactivesource.mysql.ConnectionConstants.USERNAME;
import static org.reactivesource.testing.DateConstants.TODAY;
import static org.reactivesource.testing.TestConstants.INTEGRATION;
import static org.reactivesource.testing.TestConstants.SMALL;
import static org.testng.Assert.*;

public class MysqlConfiguratorTest {

    private static final String INSERT_QUERY = "INSERT INTO " + TEST_TABLE_NAME + " VALUES (?, ?)";
    private static final String COUNT_EVENTS_QUERY = "SELECT count(1) FROM " + MysqlEventRepo.TABLE_NAME;
    private static final String UPDATE_QUERY = "UPDATE " + TEST_TABLE_NAME + " SET TXT=? WHERE ID=?";
    private static final String DELETE_QUERY = "DELETE FROM " + TEST_TABLE_NAME + " WHERE ID=?";

    private ConnectionProvider provider;

    @Mock
    TableMetadata mockedTableMetadata;
    @Mock
    ConnectionProvider mockedErroneousProvider;

    @BeforeMethod(groups = SMALL)
    public void setupSmall() throws SQLException {
        initMocks(this);
        prepareMocks();
    }

    @BeforeMethod(groups = INTEGRATION)
    public void setupIntegration() {
        new DbInitializer().setupDb();
        provider = new MysqlConnectionProvider(URL, USERNAME, PASSWORD);
    }

    @Test(groups = SMALL, expectedExceptions = IllegalArgumentException.class)
    public void testCanNotBeInitializedWithNullConnectionProvider() {
        new MysqlConfigurator(null, "asd");

    }

    @Test(groups = SMALL, expectedExceptions = IllegalArgumentException.class)
    public void testCanNotBeInitializedWithEmptyTableName() {
        new MysqlConfigurator(mock(ConnectionProvider.class), "");
    }

    @Test(groups = INTEGRATION)
    public void testCreateTriggersCreatesInsertTrigger() throws SQLException {
        MysqlConfigurator configurator = new MysqlConfigurator(provider, TEST_TABLE_NAME);
        configurator.createTriggers();

        int initialEventsCount = getEventsCount();
        insertToTestTable(1, "value");

        assertEquals(getEventsCount(), initialEventsCount + 1);
    }

    @Test(groups = INTEGRATION)
    public void testCreateTriggersCreatesUpdateTrigger() throws SQLException {
        MysqlConfigurator configurator = new MysqlConfigurator(provider, TEST_TABLE_NAME);

        insertToTestTable(1, "value");

        configurator.createTriggers();

        int initialEventsCount = getEventsCount();
        updateTestValue(1, "value2");

        assertEquals(getEventsCount(), initialEventsCount + 1);
    }

    @Test(groups = INTEGRATION)
    public void testCreateTriggersCreatesDeleteTrigger() throws SQLException {
        MysqlConfigurator configurator = new MysqlConfigurator(provider, TEST_TABLE_NAME);

        insertToTestTable(1, "value");

        configurator.createTriggers();

        int initialEventsCount = getEventsCount();
        deleteTestValue(1);

        assertEquals(getEventsCount(), initialEventsCount + 1);
    }

    @Test(groups = SMALL, expectedExceptions = ConfigurationException.class)
    public void testCreateTriggersThrowsConfigurationExceptionWhenSqlExceptionOccures() throws SQLException {
        MysqlConfigurator configurator = new MysqlConfigurator(mockedErroneousProvider, TEST_TABLE_NAME,
                mockedTableMetadata);

        configurator.createTriggers();
    }

    @Test(groups = INTEGRATION)
    public void testCleanupTriggersRemovesTheTriggersIfThereIsNoOtherListenerForThisTable() throws SQLException {
        MysqlConfigurator configurator = new MysqlConfigurator(provider, TEST_TABLE_NAME);
        configurator.createTriggers();
        configurator.cleanupTriggers();

        int initialEventsCount = getEventsCount();
        insertToTestTable(1, "value");

        assertEquals(getEventsCount(), initialEventsCount);
    }

    @Test(groups = INTEGRATION)
    public void testCreateTriggersDoesntFailIfTriggersAlreadyExistForTheGivenTable() throws SQLException {
        MysqlConfigurator configurator = new MysqlConfigurator(provider, TEST_TABLE_NAME);
        configurator.createTriggers();
        configurator.createTriggers();

        int initialEventsCount = getEventsCount();
        insertToTestTable(1, "value");

        assertEquals(getEventsCount(), initialEventsCount + 1);
    }

    @Test(groups = SMALL, expectedExceptions = ConfigurationException.class)
    public void testCleanupTriggersThrowsConfigurationExceptionWhenSqlExceptionOccures() throws SQLException {
        MysqlConfigurator configurator = new MysqlConfigurator(mockedErroneousProvider, TEST_TABLE_NAME,
                mockedTableMetadata);

        configurator.cleanupTriggers();
    }

    @Test(groups = INTEGRATION)
    public void testCleanupTriggersTriggersDoesNotRemoveTheTriggersIfThereAreOtherListenersForThisTable()
            throws SQLException {
        Listener listener = new Listener(TEST_TABLE_NAME);
        ListenerRepo listenerRepo = new ListenerRepo();

        Connection connection = provider.getConnection();
        listenerRepo.insert(listener, connection);
        JdbcUtils.closeConnection(connection);

        MysqlConfigurator configurator = new MysqlConfigurator(provider, TEST_TABLE_NAME);
        configurator.createTriggers();
        configurator.cleanupTriggers();

        int initialEventsCount = getEventsCount();
        insertToTestTable(1, "value");

        assertEquals(getEventsCount(), initialEventsCount + 1);
    }

    @Test(groups = INTEGRATION)
    public void testInitReactiveTablesCreatesTheReactiveSchemaTablesIfTheyDoNotExist() throws SQLException {
        cleanupSchema();
        MysqlConfigurator configurator = new MysqlConfigurator(provider, TEST_TABLE_NAME);

        configurator.initReactiveTables();

        Connection connection = provider.getConnection();
        JdbcUtils.sql(connection, "SELECT * FROM " + MysqlEventRepo.TABLE_NAME);
        JdbcUtils.sql(connection, "SELECT * FROM " + ListenerRepo.TABLE_NAME);
        JdbcUtils.closeConnection(connection);
    }

    @Test(groups = INTEGRATION)
    public void testInitReactiveTablesDoesNotOverrideTablesIfTheyAlreadyExist() throws SQLException {
        Connection connection = provider.getConnection();
        ListenerRepo listenerRepo = new ListenerRepo();

        //tables are there from BeforeMethod. Insert some values.
        listenerRepo.insert(new Listener(TEST_TABLE_NAME), connection);
        MysqlEventRepoUtils
                .insertEvent(new MysqlEvent(1, TEST_TABLE_NAME, EventType.INSERT, "{}", "{}", TODAY), connection);

        MysqlConfigurator configurator = new MysqlConfigurator(provider, TEST_TABLE_NAME);

        configurator.initReactiveTables();

        //verify the tables where not overwritten
        assertEquals(listenerRepo.findByTableName(TEST_TABLE_NAME, connection).size(), 1);
        assertEquals(getEventsCount(), 1);

        JdbcUtils.closeConnection(connection);
    }

    @Test(groups = SMALL, expectedExceptions = ConfigurationException.class)
    public void testInitReactiveTablesThrowsConfigurationExceptionIfSQLExceptionOccurs() throws SQLException {
        MysqlConfigurator configurator = new MysqlConfigurator(mockedErroneousProvider, TEST_TABLE_NAME,
                mockedTableMetadata);

        configurator.initReactiveTables();
    }

    private void prepareMocks() throws SQLException {
        Connection mockedConnection = mock(Connection.class);

        when(mockedErroneousProvider.getConnection()).thenReturn(mockedConnection);
        when(mockedConnection.createStatement()).thenThrow(new SQLException());
        when(mockedConnection.prepareStatement(anyString())).thenThrow(new SQLException());

        when(mockedTableMetadata.getColumnNames(anyString())).thenReturn(newArrayList("testCol"));
    }

    static List<String> getTriggers(String tableName, Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            List<String> triggerNames = newArrayList();
            ResultSet rs = stmt.executeQuery("SHOW TRIGGERS LIKE '" + tableName + "'");

            while (rs.next()) {
                String triggerName = rs.getString("Trigger");
                triggerNames.add(triggerName);
            }
            return triggerNames;
        }
    }

    private void cleanupSchema() throws SQLException {
        try (
                Connection connection = provider.getConnection();
                Statement stmt = connection.createStatement()
        ) {
            stmt.execute("DROP TABLE IF EXISTS " + MysqlEventRepo.TABLE_NAME);
            stmt.execute("DROP TABLE IF EXISTS " + ListenerRepo.TABLE_NAME);
        }
    }

    private void deleteTestValue(int id) throws SQLException {
        try (
                Connection connection = provider.getConnection();
                PreparedStatement stmt = connection.prepareStatement(DELETE_QUERY)
        ) {
            stmt.setObject(1, id);
            stmt.executeUpdate();
        }
    }

    private void updateTestValue(int id, String newValue) throws SQLException {
        try (
                Connection connection = provider.getConnection();
                PreparedStatement stmt = connection.prepareStatement(UPDATE_QUERY)
        ) {
            stmt.setObject(1, newValue);
            stmt.setObject(2, id);
            stmt.executeUpdate();
        }
    }

    private void insertToTestTable(int id, String value) throws SQLException {
        try (
                Connection connection = provider.getConnection();
                PreparedStatement stmt = connection.prepareStatement(INSERT_QUERY)
        ) {
            stmt.setObject(1, id);
            stmt.setObject(2, value);
            stmt.executeUpdate();
        }
    }

    private int getEventsCount() throws SQLException {
        try (
                Connection connection = provider.getConnection();
                Statement stmt = connection.createStatement()
        ) {
            ResultSet rs = stmt.executeQuery(COUNT_EVENTS_QUERY);
            rs.next();
            return rs.getInt(1);
        }
    }

}
