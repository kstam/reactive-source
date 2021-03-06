/*******************************************************************************
 * Copyright (c) 2013-2014 eBay Software Foundation
 *
 * See the file license.txt for copying permission.
 ******************************************************************************/

package org.reactivesource.psql;

import org.reactivesource.ConnectionProvider;
import org.reactivesource.exceptions.ConfigurationException;
import org.reactivesource.exceptions.DataAccessException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.*;

import static org.reactivesource.util.JdbcUtils.closeResultset;
import static org.reactivesource.util.JdbcUtils.closeStatement;
import static org.reactivesource.testing.TestConstants.*;
import static org.reactivesource.psql.ConnectionConstants.*;
import static org.reactivesource.psql.PsqlConfigurator.FUNCTION_NAME;
import static org.reactivesource.psql.PsqlConfigurator.TRIGGER_NAME_SUFFIX;
import static org.testng.Assert.*;

public class PsqlConfiguratorIntegrationTest {

    private ConnectionProvider connectionProvider;
    private PsqlConfigurator configurator;
    private static final String STREAM_NAME = "newStreamName";
    private static final String TABLE_NAME = TEST_TABLE_NAME;

    private static final String EXPECTED_TRIGGER_NAME = TABLE_NAME + TRIGGER_NAME_SUFFIX;

    @BeforeMethod(groups = INTEGRATION)
    public void setup() {
        connectionProvider = new PsqlConnectionProvider(PSQL_URL, USERNAME, PASSWORD);
        configurator = new PsqlConfigurator(connectionProvider, TABLE_NAME, STREAM_NAME);

        dropTrigger(EXPECTED_TRIGGER_NAME, TABLE_NAME);
        dropProc(FUNCTION_NAME);
    }

    @AfterMethod(groups = INTEGRATION)
    public void tearDown() {
        dropTrigger(EXPECTED_TRIGGER_NAME, TABLE_NAME);
        dropProc(FUNCTION_NAME);
    }

    @Test(groups = INTEGRATION)
    public void testSetupCreatesNotifyMethod() {
        configurator.setup();
        assertTrue(isProcCreated(FUNCTION_NAME));
    }

    @Test(groups = INTEGRATION)
    public void testSetupCreatesTriggersOnGivenTable() {
        configurator.setup();
        assertTrue(isTriggerCreated(EXPECTED_TRIGGER_NAME));
    }

    @Test(groups = INTEGRATION)
    public void testCallingSetupTwiceShouldntFail() {
        configurator.setup();
        configurator.setup();
        assertTrue(isTriggerCreated(EXPECTED_TRIGGER_NAME));
        assertTrue(isProcCreated(FUNCTION_NAME));
    }

    @Test(groups = INTEGRATION)
    public void testTwoConfiguratorsForTheSameTableCreateOnlyOneTrigger() {
        new PsqlConfigurator(connectionProvider, TABLE_NAME, "whatever").setup();
        configurator.setup();
    }

    private boolean isProcCreated(String procName) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try (Connection connection = connectionProvider.getConnection()) {
            stmt = connection.prepareStatement(GET_PROC_QUERY);
            stmt.setString(1, procName);

            rs = stmt.executeQuery();

            return rs.next();
        } catch (SQLException e) {
            throw new DataAccessException("", e);
        } finally {
            closeResultset(rs);
            closeStatement(stmt);
        }
    }

    private boolean isTriggerCreated(String triggerName) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try (Connection connection = connectionProvider.getConnection()) {
            stmt = connection.prepareStatement(GET_TRIGGER_QUERY);
            stmt.setString(1, triggerName);
            stmt.setString(2, TABLE_NAME);

            rs = stmt.executeQuery();

            return rs.next();
        } catch (SQLException e) {
            throw new DataAccessException("", e);
        } finally {
            closeResultset(rs);
            closeStatement(stmt);
        }
    }

    private void dropTrigger(String triggerName, String tableName) {
        Statement stmt = null;
        try (Connection connection = connectionProvider.getConnection()) {
            stmt = connection.createStatement();
            stmt.executeUpdate(PsqlQueryGenerator.generateDropTriggerQuery(triggerName, tableName));
        } catch (SQLException e) {
            throw new ConfigurationException("Couldn't drop trigger", e);
        } finally {
            closeStatement(stmt);
        }
    }

    private void dropProc(String procName) {
        Statement stmt = null;
        try (Connection connection = connectionProvider.getConnection()) {
            stmt = connection.createStatement();
            stmt.executeUpdate(PsqlQueryGenerator.generateDropProcQuery(procName));
        } catch (SQLException e) {
            throw new ConfigurationException("Couldn't drop notifyFunction", e);
        } finally {
            closeStatement(stmt);
        }
    }

    private static final String GET_PROC_QUERY = "SELECT proname "
            + "FROM pg_catalog.pg_namespace n "
            + "JOIN pg_catalog.pg_proc p ON pronamespace = n.oid"
            + " WHERE nspname = current_schema() and proname = ?";

    private static final String GET_TRIGGER_QUERY = "SELECT t.tgname, c.relname AS table_name "
            + "FROM pg_trigger t "
            + "JOIN pg_class c ON t.tgrelid = c.oid "
            + "WHERE t.tgname=? and c.relname=?";

}
