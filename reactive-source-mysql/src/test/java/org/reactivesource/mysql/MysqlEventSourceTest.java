/*******************************************************************************
 * Copyright (c) 2013-2014 eBay Software Foundation
 *
 * See the file license.txt for copying permission.
 ******************************************************************************/

package org.reactivesource.mysql;

import org.reactivesource.ConnectionProvider;
import org.reactivesource.EventType;
import org.reactivesource.util.JdbcUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
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
import static org.reactivesource.mysql.ConnectionConstants.*;
import static org.reactivesource.mysql.ListenerRepo.TABLE_NAME;
import static org.reactivesource.mysql.ListenerRepo.extractListener;
import static org.reactivesource.mysql.MysqlEventRepoTest.insertEvent;
import static org.reactivesource.testing.TestConstants.INTEGRATION;
import static org.reactivesource.testing.TestConstants.SMALL;
import static org.testng.Assert.*;

public class MysqlEventSourceTest {

    ConnectionProvider provider = new MysqlConnectionProvider(URL, USERNAME, PASSWORD);

    @BeforeMethod(groups = INTEGRATION)
    public void setup() throws IOException, SQLException {
        new DbInitializer().setupDb();
    }

    @Test(groups = SMALL)
    public void testCanBeInitializedWithConnectionProvider() {
        assertNotNull(new MysqlEventSource(mock(ConnectionProvider.class), TEST_TABLE_NAME));
    }

    @Test(groups = SMALL)
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

    @Test(groups = SMALL, expectedExceptions = IllegalStateException.class)
    public void testCallingGetNewEventsWithoutCallingConnectThrowsException() {
        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME);
        eventSource.getNewEvents();
    }

    @Test(groups = SMALL)
    public void testAutoConfigurationDefaultsToFalseWhenNotSpecified() {
        assertFalse(new MysqlEventSource(provider, TEST_TABLE_NAME).isAutoConfigure());
        assertFalse(new MysqlEventSource(URL, USERNAME, PASSWORD, TABLE_NAME).isAutoConfigure());
    }

    @Test(groups = SMALL)
    public void testAutoConfigurationIsSetToTrueWhenSpecified() {
        assertTrue(new MysqlEventSource(provider, TEST_TABLE_NAME, true).isAutoConfigure());
        assertTrue(new MysqlEventSource(URL, USERNAME, PASSWORD, TABLE_NAME, true).isAutoConfigure());
    }

    @Test(groups = INTEGRATION)
    public void testConnectCreatesCorrectEntryInListenerTable() throws SQLException {
        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME);
        eventSource.connect();

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
        eventSource.connect();
        assertTrue(eventSource.isConnected());
    }

    @Test(groups = INTEGRATION)
    public void testIsConnectedReturnsFalseAfterDisconnect() {
        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME);
        eventSource.connect();
        eventSource.disconnect();
        assertFalse(eventSource.isConnected());
    }

    @Test(groups = INTEGRATION)
    public void testDisconnectRemovesListenerFromTable() throws SQLException {
        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME);
        eventSource.connect();
        eventSource.disconnect();

        assertEquals(listAllListeners().size(), 0);
    }

    @Test(groups = INTEGRATION)
    public void testGetNewEventsFetchesAllNewEvents() throws SQLException {
        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME);
        MysqlEvent event = new MysqlEvent(1L, TEST_TABLE_NAME, EventType.INSERT, "{}", "{}", new Date());

        eventSource.connect();

        Connection connection = provider.getConnection();
        insertEvent(event, connection);
        JdbcUtils.closeConnection(connection);

        List events = eventSource.getNewEvents();

        assertEquals(events.size(), 1);
    }

    @Test(groups = INTEGRATION)
    public void testIsConnectedReturnsFalseWhenConnectionGetsClosed() throws SQLException {
        Connection conn = provider.getConnection();

        ConnectionProvider mockedProvider = mock(ConnectionProvider.class);
        when(mockedProvider.getConnection()).thenReturn(conn);

        MysqlEventSource eventSource = new MysqlEventSource(mockedProvider, TEST_TABLE_NAME);
        assertFalse(eventSource.isConnected());

        eventSource.connect();
        assertTrue(eventSource.isConnected());

        JdbcUtils.closeConnection(conn);
        assertFalse(eventSource.isConnected());
    }

    @Test(groups = INTEGRATION)
    public void testIsConnectedReturnsFalseWhenSQLExceptionIsThrown() throws SQLException {
        Connection conn = provider.getConnection();

        ConnectionProvider mockedProvider = mock(ConnectionProvider.class);
        when(mockedProvider.getConnection()).thenReturn(conn);

        MysqlEventSource eventSource = new MysqlEventSource(mockedProvider, TEST_TABLE_NAME);
        assertFalse(eventSource.isConnected());

        eventSource.connect();
        assertTrue(eventSource.isConnected());

        JdbcUtils.closeConnection(conn);
        assertFalse(eventSource.isConnected());
    }

    @Test(groups = SMALL)
    public void testSetupCallsTheSetupMethodOfTheConfigurator() {
        MysqlConfigurator mockedConfigurator = mock(MysqlConfigurator.class);

        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME, mockedConfigurator, false);

        eventSource.setup();

        verify(mockedConfigurator).setup();
    }

    @Test(groups = SMALL)
    public void testSetupCallsTheCleanupMethodOfTheConfigurator() {
        MysqlConfigurator mockedConfigurator = mock(MysqlConfigurator.class);

        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME, mockedConfigurator, false);

        eventSource.cleanup();

        verify(mockedConfigurator).cleanup();
    }

    @Test(groups = SMALL)
    public void testSetupCallsTheInitReactiveTablesOfTheConfiguratorWhenAutoConfigureIsTrue() {
        MysqlConfigurator mockedConfigurator = mock(MysqlConfigurator.class);
        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME, mockedConfigurator, true);

        eventSource.setup();

        verify(mockedConfigurator).initReactiveTables();
        verify(mockedConfigurator).setup();
    }

    @Test(groups = SMALL)
    public void testSetupDoesNotCallTheInitReactiveTablesOfTheConfiguratorWhenAutoConfigureIsFalse() {
        MysqlConfigurator mockedConfigurator = mock(MysqlConfigurator.class);
        MysqlEventSource eventSource = new MysqlEventSource(provider, TEST_TABLE_NAME, mockedConfigurator, false);

        eventSource.setup();

        verify(mockedConfigurator, never()).initReactiveTables();
        verify(mockedConfigurator).setup();
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
