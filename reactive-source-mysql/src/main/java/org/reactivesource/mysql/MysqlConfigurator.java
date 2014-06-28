/*******************************************************************************
 * Copyright (c) 2013-2014 eBay Software Foundation
 *
 * See the file license.txt for copying permission.
 ******************************************************************************/

package org.reactivesource.mysql;

import com.google.common.collect.Lists;
import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;
import org.apache.commons.io.IOUtils;
import org.reactivesource.ConnectionProvider;
import org.reactivesource.exceptions.ConfigurationException;
import org.reactivesource.util.JdbcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.reactivesource.util.Assert.hasText;

/**
 * Used for properly configuring the database ({@link #initReactiveTables()}) or the table for which the configurator
 * is defined ({@link #createTriggers()}, {@link #cleanupTriggers()}
 */
class MysqlConfigurator {

    public static final String TABLE_NAME_NULL = "tableName can not be null or empty";
    private final TableMetadata tableMetadata;
    private final ConnectionProvider connectionProvider;
    private final String tableName;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public MysqlConfigurator(ConnectionProvider connectionProvider, String tableName) {
        hasText(tableName, TABLE_NAME_NULL);

        this.connectionProvider = connectionProvider;
        this.tableName = tableName;
        this.tableMetadata = new TableMetadata(connectionProvider);
    }

    /**
     * Will create the triggers that are required for the ReactiveSource framework to function.
     */
    public void createTriggers() {
        try (Connection connection = connectionProvider.getConnection()) {
            List<String> tableColumnNames = tableMetadata.getColumnNames(tableName);
            List<ReactiveTrigger> triggersToCreate = Lists.newArrayList(
                    ReactiveTriggerFactory.afterInsert(tableName, tableColumnNames),
                    ReactiveTriggerFactory.afterUpdate(tableName, tableColumnNames),
                    ReactiveTriggerFactory.afterDelete(tableName, tableColumnNames));

            createTriggers(triggersToCreate, connection);

        } catch (MySQLSyntaxErrorException msee) {
            logger.error("Could not create triggers for the table [{}]. If you are using a MySQL version < 5.7 " +
                    "make sure there is no other trigger with the same trigger_time and trigger_event. Otherwise " +
                    "consider upgrading to the latest MySQL version.", tableName);
            throw new ConfigurationException("Couldn't setup triggers for ReactiveSource table [" + tableName + "]",
                    msee);
        } catch (SQLException sqle) {
            logger.error("Couldn't setup triggers for ReactiveSource table [{}]", tableName);
            throw new ConfigurationException("Couldn't setup triggers for ReactiveSource table [" + tableName + "]",
                    sqle);
        }
    }

    /**
     * Will cleanup the triggers associated to the given tableName only if there are no listeners for this table
     */
    public void cleanupTriggers() {
        try (
                Connection connection = connectionProvider.getConnection();
                Statement stmt = connection.createStatement()
        ) {

            ListenerRepo repo = new ListenerRepo();
            List<Listener> listeners = repo.findByTableName(tableName, connection);

            if (listeners.isEmpty()) {
                List<String> tableColumnNames = tableMetadata.getColumnNames(tableName);
                ReactiveTrigger insertTrigger = ReactiveTriggerFactory.afterInsert(tableName, tableColumnNames);
                ReactiveTrigger updateTrigger = ReactiveTriggerFactory.afterUpdate(tableName, tableColumnNames);
                ReactiveTrigger deleteTrigger = ReactiveTriggerFactory.afterDelete(tableName, tableColumnNames);

                stmt.execute(insertTrigger.getDropSql());
                stmt.execute(updateTrigger.getDropSql());
                stmt.execute(deleteTrigger.getDropSql());
            }

        } catch (SQLException sqle) {
            throw new ConfigurationException("Couldn't cleanup [" + tableName + "] reactive source triggers", sqle);
        }
    }

    /**
     * Will create the tables needed for the framework to work.
     * <p/>
     * It will create new tables only if the tables are not already there.
     */
    public void initReactiveTables() {
        try (
                Connection connection = connectionProvider.getConnection();
        ) {
            if (!reactiveTablesExist(connection)) {
                String query = IOUtils.toString(getClass().getResourceAsStream("create-reactive-tables.sql"));
                JdbcUtils.sql(connection, query);
            }
        } catch (SQLException | IOException e) {
            throw new ConfigurationException("Couldn't cleanup " + tableName + " reactive source triggers", e);
        }
    }

    private void createTriggers(List<ReactiveTrigger> triggersToCreate, Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            for (ReactiveTrigger trigger : triggersToCreate) {
                if (!triggerExists(trigger, connection)) {
                    logSkipCreationMessage(trigger);
                    stmt.execute(trigger.getCreateSql());
                }
            }
        }
    }

    private boolean triggerExists(ReactiveTrigger trigger, Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SHOW TRIGGERS LIKE '" + trigger.getTriggerTable() + "'");

            while(rs.next()) {
                String triggerName = rs.getString("Trigger");
                if (triggerName.equals(trigger.getTriggerName())) {
                    return true;
                }
            }
            return false;
        }
    }

    private void logSkipCreationMessage(ReactiveTrigger trigger) {
        logger.info("An {} {} trigger for table [{}] with the same name exists. Skipping creation.",
                trigger.getTriggerTime(), trigger.getTriggerEvent(), trigger.getTriggerTable());
    }

    private boolean reactiveTablesExist(Connection connection) {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SELECT * FROM " + ListenerRepo.TABLE_NAME + " LIMIT 1");
            stmt.execute("SELECT * FROM " + MysqlEventRepo.TABLE_NAME + " LIMIT 1");
            return true;
        } catch (SQLException sqle) {
            return false;
        }
    }
}
