/*******************************************************************************
 * Copyright (c) 2013-2014 eBay Software Foundation
 *
 * See the file license.txt for copying permission.
 ******************************************************************************/
package org.reactivesource.psql;

import org.reactivesource.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static java.lang.Thread.sleep;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.reactivesource.testing.TestConstants.*;
import static org.reactivesource.psql.ConnectionConstants.*;
import static org.testng.Assert.*;

public class ReactiveSourcePsqlIntegrationTest {

    private static final String TEST_TABLE = TEST_TABLE_NAME;

    ConnectionProvider connectionProvider = new PsqlConnectionProvider(PSQL_URL, USERNAME, PASSWORD);

    MyEventListener eventListener;

    @BeforeMethod(groups = INTEGRATION)
    public void setup() {
        eventListener = spy(new MyEventListener(new MyEntityExtractor()));
        cleanupDatabase();
    }

    @SuppressWarnings("unchecked")
    @Test(groups = INTEGRATION)
    public void testReactiveDatasourceBehaviorForPsqlEventSource() throws InterruptedException {
        int ENTITIES = 10;
        // create new ReactiveEventSource
        PsqlEventSource eventSource = new PsqlEventSource(connectionProvider, TEST_TABLE);
        ReactiveSource<String> rds = new ReactiveSource<>(eventSource);

        // add new eventListener
        rds.addEventListener(eventListener);
        rds.start();
        sleep(200L); //wait for the thread to be started

        // insert new entities
        for (int i = 0; i < ENTITIES; i++) {
            insertNewRow(i, "someValue" + i);
        }

        // wait for database to be queried and verify all the insertion events arrived
        sleep(1000L);
        verify(eventListener, times(ENTITIES)).onEvent(any(Event.class));

        // stop the ReactiveDatasource
        rds.stop();

        // cleanup the database and make sure that none of the delete events will arrive
        cleanupDatabase();
        sleep(1000L);
        verify(eventListener, times(ENTITIES)).onEvent(any(Event.class));
    }

    @Test(groups = INTEGRATION, enabled = false)
    public void testManually() throws InterruptedException {
        PsqlEventSource eventSource = new PsqlEventSource(connectionProvider, TEST_TABLE);
        ReactiveSource<String> rds = new ReactiveSource<>(eventSource);

        // add new eventListener
        rds.addEventListener(new MyEventListener(new MyEntityExtractor()));
        rds.start();

        // sleep enough to see it working
        sleep(600000L);

        rds.stop();
    }

    private void insertNewRow(int id, String value) {
        try {
            Connection connection = connectionProvider.getConnection();
            PreparedStatement stmt = connection.prepareStatement("INSERT INTO " + TEST_TABLE + " VALUES (?, ?)");
            stmt.setInt(1, id);
            stmt.setString(2, value);
            stmt.executeUpdate();

            stmt.close();
            connection.close();
        } catch (SQLException sqle) {
            fail("Could not insert new row (" + id + "," + value + ")", sqle);
        }
    }

    private void cleanupDatabase() {
        try {
            Connection connection = connectionProvider.getConnection();
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("DELETE FROM " + TEST_TABLE);
            stmt.close();
            connection.close();
        } catch (SQLException sqle) {
            fail("Failed to cleanup database", sqle);
        }
    }

    class MyEventListener extends EventListener<String> {

        MyEventListener(MyEntityExtractor extractor) {
            super(extractor);
        }

        @Override
        public void onEvent(Event<String> event) {
            System.out.println(event);
        }
    }

    class MyEntityExtractor implements EntityExtractor<String> {
        @Override
        public String extractEntity(Map<String, Object> entityRow) {
            return entityRow.toString();
        }
    }
}
