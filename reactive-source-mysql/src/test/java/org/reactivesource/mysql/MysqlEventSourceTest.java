/*******************************************************************************
 * Copyright (c) 2013-2014 eBay Software Foundation
 *
 * See the file license.txt for copying permission.
 ******************************************************************************/

package org.reactivesource.mysql;

import org.reactivesource.ConnectionProvider;
import org.reactivesource.exceptions.ReactiveException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.reactivesource.EventType.INSERT;
import static org.reactivesource.mysql.ConnectionConstants.PASSWORD;
import static org.reactivesource.mysql.ConnectionConstants.TEST_TABLE_NAME;
import static org.reactivesource.mysql.ConnectionConstants.URL;
import static org.reactivesource.mysql.ConnectionConstants.USERNAME;
import static org.reactivesource.mysql.ListenerRepo.TABLE_NAME;
import static org.reactivesource.mysql.ListenerRepo.extractListener;
import static org.reactivesource.mysql.MysqlEventRepoUtils.insertEvent;
import static org.reactivesource.testing.TestConstants.INTEGRATION;
import static org.reactivesource.testing.TestConstants.SMALL;
import static org.reactivesource.util.JdbcUtils.closeConnection;
import static org.testng.Assert.*;

public class MysqlEventSourceTest {

    ConnectionProvider provider = new MysqlConnectionProvider(URL, USERNAME, PASSWORD);

    @BeforeMethod(groups = INTEGRATION)
    public void setup() {
        new DbInitializer().setupDb();
    }

    @Test(groups = INTEGRATION)
    public void testCanBeInitializedWithConnectionProvider() {
        assertNotNull(new MysqlEventSource(provider, TEST_TABLE_NAME));
    }

    @Test(groups = INTEGRATION)
    public void testCanBeInitializedWithConnectionParams() {
        assertNotNull(new MysqlEventSource(URL, USERNAME, PASSWORD, TEST_TABLE_NAME));
    }

    @Test(groups = SMALL, expectedExceptions = IllegalArgumentException.class)
    public void testCanNotBeInitializedWithNullConnectionProvider() {
        new MysqlEventSource(null, TEST_TABLE_NAME);
    }

    @Test(groups = SMALL, expectedExceptions = IllegalArgumentException.class)
    public void testCanNotBeInitializedWithEmptyTableName() {
        new MysqlEventSource(mock(ConnectionProvider.class), "");
    }

    @Test(groups = INTEGRATION, expectedExceptions = IllegalStateException.class)
    public void testCallingGetNewEventsWithoutCallingConnectThrowsException() {
        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME);
        eventSource.getNewEvents();
    }

    @Test(groups = INTEGRATION, expectedExceptions = IllegalStateException.class)
    public void testCallingConnectWithoutCallingSetupThrowsException() {
        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME);
        eventSource.connect();
    }

    @Test(groups = INTEGRATION, expectedExceptions = IllegalStateException.class)
    public void testCallingCleanupWithoutCallingSetupThrowsException() {
        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME);
        eventSource.connect();
    }

    @Test(groups = INTEGRATION)
    public void testAutoConfigurationDefaultsToFalseWhenNotSpecified() {
        assertFalse(new MysqlEventSource(provider, TEST_TABLE_NAME).isAutoConfigure());
        assertFalse(new MysqlEventSource(URL, USERNAME, PASSWORD, TABLE_NAME).isAutoConfigure());
    }

    @Test(groups = INTEGRATION)
    public void testAutoConfigurationIsSetToTrueWhenSpecified() {
        assertTrue(new MysqlEventSource(provider, TEST_TABLE_NAME, true).isAutoConfigure());
        assertTrue(new MysqlEventSource(URL, USERNAME, PASSWORD, TABLE_NAME, true).isAutoConfigure());
    }

    @Test(groups = INTEGRATION)
    public void testVerifiesParametersOnInitialization() {
        assertNotNull(new MysqlEventSource(provider, TEST_TABLE_NAME));
    }

    @Test(groups = INTEGRATION, expectedExceptions = ReactiveException.class)
    public void tesThrowsExceptionIfGivenTableDoenstExist() {
        new MysqlEventSource(provider, "wrongTableName");
    }

    @Test(groups = INTEGRATION)
    public void testSetupCreatesCorrectEntryInListenerTable() throws SQLException {
        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME);
        eventSource.setup();

        assertEquals(listAllListeners().size(), 1);
    }

    @Test(groups = INTEGRATION)
    public void testIsConnectedReturnsFalseBeforeConnection() {
        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME);
        assertFalse(eventSource.isConnected());
    }

    @Test(groups = INTEGRATION)
    public void testAfterConnectingIsConnectedReturnsTrue() {
        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME);
        eventSource.setup();
        eventSource.connect();
        assertTrue(eventSource.isConnected());
    }

    @Test(groups = INTEGRATION)
    public void testIsConnectedReturnsFalseAfterDisconnect() {
        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME);
        eventSource.setup();
        eventSource.connect();
        eventSource.disconnect();
        assertFalse(eventSource.isConnected());
    }

    @Test(groups = INTEGRATION)
    public void testCanConnectAgainAfterDisconnect() {
        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME);
        eventSource.setup();
        eventSource.connect();
        eventSource.disconnect();
        eventSource.connect();

        assertTrue(eventSource.isConnected());
    }

    @Test(groups = INTEGRATION)
    public void testCleanupRemovesListenerFromTable() throws SQLException {
        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME);
        eventSource.setup();
        eventSource.cleanup();

        assertEquals(listAllListeners().size(), 0);
    }

    @Test(groups = INTEGRATION)
    public void testGetNewEventsFetchesAllNewEvents() throws SQLException {
        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME);
        MysqlEvent event = new MysqlEvent(1L, TEST_TABLE_NAME, INSERT, "{}", "{}", new Date());

        eventSource.setup();
        eventSource.connect();

        Connection connection = provider.getConnection();
        insertEvent(event, connection);
        closeConnection(connection);

        List events = eventSource.getNewEvents();

        eventSource.disconnect();
        eventSource.cleanup();
        assertEquals(events.size(), 1);
    }

    @Test(groups = INTEGRATION)
    public void testIsConnectedReturnsFalseWhenConnectionGetsClosed() throws SQLException {
        MysqlConfigurator mockedConfigurator = mock(MysqlConfigurator.class);
        ConnectionProvider mockedProvider = mock(ConnectionProvider.class);
        Connection conn = provider.getConnection();
        when(mockedProvider.getConnection())
                .thenReturn(provider.getConnection())
                .thenReturn(provider.getConnection())
                .thenReturn(conn)
                .thenReturn(provider.getConnection());

        MysqlEventSource eventSource = new MysqlEventSource(mockedProvider, TEST_TABLE_NAME, mockedConfigurator, false);
        assertFalse(eventSource.isConnected());

        eventSource.setup();
        eventSource.connect();

        assertTrue(eventSource.isConnected());

        closeConnection(conn);
        assertFalse(eventSource.isConnected());
    }

    @Test(groups = INTEGRATION)
    public void testIsConnectedReturnsFalseWhenSQLExceptionIsThrown() throws SQLException {
        MysqlConfigurator mockedConfigurator = mock(MysqlConfigurator.class);
        ConnectionProvider mockedProvider = mock(ConnectionProvider.class);
        Connection mockedConn = mock(Connection.class);
        when(mockedConn.isClosed()).thenReturn(false);

        when(mockedProvider.getConnection())
                .thenReturn(provider.getConnection())
                .thenReturn(provider.getConnection())
                .thenReturn(mockedConn)
                .thenReturn(provider.getConnection());

        MysqlEventSource eventSource = new MysqlEventSource(mockedProvider, TEST_TABLE_NAME, mockedConfigurator, false);
        assertFalse(eventSource.isConnected());

        eventSource.setup();
        eventSource.connect();
        assertTrue(eventSource.isConnected());

        when(mockedConn.isClosed()).thenThrow(new SQLException(""));
        assertFalse(eventSource.isConnected());
    }

    @Test(groups = INTEGRATION)
    public void testMultipleEventSourcesAreNotAffectedByTheOrderOfExecution() throws SQLException {
        final MysqlEventSource es1 = new MysqlEventSource(provider, TEST_TABLE_NAME);
        final MysqlEventSource es2 = new MysqlEventSource(provider, TEST_TABLE_NAME);

        es1.setup();
        es1.connect();
        es2.setup();
        es1.disconnect();
        es1.cleanup();
        es2.connect();

        Connection conn = provider.getConnection();
        List<String> triggerNames = MysqlConfiguratorTest.getTriggers(TEST_TABLE_NAME, conn);
        closeConnection(conn);

        assertEquals(triggerNames.size(), 3);
    }

    @Test(groups = INTEGRATION)
    public void testSetupCallsTheCreateTriggersMethodOfTheConfigurator() {
        MysqlConfigurator mockedConfigurator = mock(MysqlConfigurator.class);

        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME, mockedConfigurator, false);

        eventSource.setup();

        verify(mockedConfigurator).createTriggers();
    }

    @Test(groups = INTEGRATION)
    public void testCleanupCallsTheCleanupTriggersMethodOfTheConfigurator() {
        MysqlConfigurator mockedConfigurator = mock(MysqlConfigurator.class);

        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME, mockedConfigurator, false);

        eventSource.setup();
        eventSource.cleanup();

        verify(mockedConfigurator).cleanupTriggers();
    }

    @Test(groups = INTEGRATION)
    public void testSetupCallsTheInitReactiveTablesOfTheConfiguratorWhenAutoConfigureIsTrue() {
        MysqlConfigurator mockedConfigurator = mock(MysqlConfigurator.class);
        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME, mockedConfigurator, true);

        eventSource.setup();

        verify(mockedConfigurator).initReactiveTables();
        verify(mockedConfigurator).createTriggers();
    }

    @Test(groups = INTEGRATION)
    public void testSetupDoesNotCallTheInitReactiveTablesOfTheConfiguratorWhenAutoConfigureIsFalse() {
        MysqlConfigurator mockedConfigurator = mock(MysqlConfigurator.class);
        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME, mockedConfigurator, false);

        eventSource.setup();

        verify(mockedConfigurator, never()).initReactiveTables();
        verify(mockedConfigurator).createTriggers();
    }

    private List<Listener> listAllListeners() throws SQLException {
        try (
                Connection connection = provider.getConnection();
                PreparedStatement stmt = connection.prepareStatement(LIST_ALL_QUERY)
        ) {
            List<Listener> result = newArrayList();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(extractListener(rs));
            }
            return result;
        }
    }

    private static final String LIST_ALL_QUERY =
            "SELECT * FROM " + ListenerRepo.TABLE_NAME;

}
