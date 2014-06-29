/*******************************************************************************
 * Copyright (c) 2013-2014 eBay Software Foundation
 *
 * See the file license.txt for copying permission.
 ******************************************************************************/

package org.reactivesource.mysql;

import com.google.common.annotations.VisibleForTesting;
import org.reactivesource.ConnectionProvider;
import org.reactivesource.Event;
import org.reactivesource.EventSource;
import org.reactivesource.exceptions.DataAccessException;
import org.reactivesource.exceptions.ReactiveException;
import org.reactivesource.util.JdbcUtils;

import javax.annotation.concurrent.NotThreadSafe;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.reactivesource.util.Assert.hasText;
import static org.reactivesource.util.Assert.notNull;
import static org.reactivesource.util.Assert.state;

/**
 * Implementation of EventSource for Mysql database tables
 * <p/>
 * By default MysqlEventSource autConfigure option defaults to <code>false</code>. You can set it to true by using
 * the appropriate constructors
 */
@NotThreadSafe
public class MysqlEventSource implements EventSource {

    private final ConnectionProvider connectionProvider;
    private final String tableName;

    private final MysqlEventMapper eventMapper;

    private MysqlEventRepo eventRepo;
    private ListenerRepo listenerRepo;

    private Listener listener;
    private Connection connection;
    private MysqlConfigurator configurator;
    private final boolean autoConfigure;

    public MysqlEventSource(ConnectionProvider connectionProvider, String tableName) {
        this(connectionProvider, tableName, false);
    }

    public MysqlEventSource(ConnectionProvider connectionProvider, String tableName, boolean autoConfigure) {
        this(connectionProvider, tableName, new MysqlConfigurator(connectionProvider, tableName), autoConfigure);
    }

    public MysqlEventSource(String url, String username, String password, String tableName) {
        this(url, username, password, tableName, false);
    }

    public MysqlEventSource(String url, String username, String password, String tableName, boolean autoConfigure) {
        this(new MysqlConnectionProvider(url, username, password), tableName, autoConfigure);
    }

    @VisibleForTesting MysqlEventSource(ConnectionProvider connectionProvider, String tableName,
                                        MysqlConfigurator configurator, boolean autoConfigure) {

        notNull(connectionProvider, "Connection Provider can not be null");
        hasText(tableName, "Table Name can not be null or empty");
        verifyConfiguration(connectionProvider, tableName);
        this.tableName = tableName;
        this.connectionProvider = connectionProvider;
        this.eventMapper = new MysqlEventMapper();
        this.eventRepo = new MysqlEventRepo();
        this.listenerRepo = new ListenerRepo();
        this.configurator = configurator;
        this.autoConfigure = autoConfigure;
    }

    @Override
    public List<Event<Map<String, Object>>> getNewEvents() throws DataAccessException {
        state(isConnected(), "Attempted to call 'getNewEvents' without first calling 'connect'");
        return mapMysqlEventsToGenericEvents(eventRepo.getNewEventsForListener(listener, connection));
    }

    @Override
    public void connect() throws DataAccessException {
        if (!isConnected()) {
            connection = connectionProvider.getConnection();
            updateListener(listener);
        }
    }

    @Override
    public void disconnect() throws DataAccessException {
        if (isConnected()) {
            JdbcUtils.closeConnection(connection);
            connection = null;
        }
    }

    @Override
    public boolean isConnected() {
        try {
            return (connection != null && !connection.isClosed());
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public void setup() {
        if (autoConfigure) {
            configurator.initReactiveTables();
        }
        registerListener();
        configurator.createTriggers();
    }

    @Override
    public void cleanup() {
        configurator.cleanupTriggers();
        cleanupListener();
    }

    @VisibleForTesting boolean isAutoConfigure() {
        return autoConfigure;
    }

    private void registerListener() {
        Connection connection = connectionProvider.getConnection();
        try {
            if (listener == null) {
                listener = listenerRepo.insert(new Listener(tableName), connection);
            }
        } finally {
            JdbcUtils.closeConnection(connection);
        }
    }

    private void updateListener(Listener listener) {
        state(listener != null, "Attempted to call 'connect' before calling 'setup'");
        Connection connection = connectionProvider.getConnection();
        try {
            listenerRepo.refreshLastCheck(listener, connection);
        } finally {
            JdbcUtils.closeConnection(connection);
        }
    }

    private void cleanupListener() {
        state(listener != null, "Attempted to call 'cleanup' before calling 'setup'");
        Connection connection = connectionProvider.getConnection();
        try {
            listenerRepo.remove(listener, connection);
            listener = null;
        } finally {
            JdbcUtils.closeConnection(connection);
        }
    }

    private List<Event<Map<String, Object>>> mapMysqlEventsToGenericEvents(List<MysqlEvent> mysqlEvents) {
        List<Event<Map<String, Object>>> result = newArrayList();
        for (MysqlEvent event : mysqlEvents) {
            result.add(eventMapper.mapToGenericEvent(event));
        }
        return result;
    }

    private void verifyConfiguration(ConnectionProvider connectionProvider, String tableName) {
        try (Connection connection = connectionProvider.getConnection()) {
            verifyTableExists(connection, tableName);
        } catch (SQLException sqle) {
            throw new ReactiveException(
                    "Could not initialize EventSource. Verify that the connection parameters are correct.", sqle);
        }
    }

    private void verifyTableExists(Connection connection, String tableName) throws SQLException {
        connection.createStatement().executeQuery("SELECT * FROM " + tableName + " LIMIT 1");
    }
}
