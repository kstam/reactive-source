/*******************************************************************************
 * Copyright (c) 2013-2014 eBay Software Foundation
 *
 * See the file license.txt for copying permission.
 ******************************************************************************/

package org.reactivesource.mysql;

import org.reactivesource.ConnectionProvider;
import org.reactivesource.EntityExtractor;
import org.reactivesource.Event;
import org.reactivesource.EventListener;
import org.reactivesource.ReactiveSource;
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
import static org.reactivesource.mysql.ConnectionConstants.PASSWORD;
import static org.reactivesource.mysql.ConnectionConstants.TEST_TABLE_NAME;
import static org.reactivesource.mysql.ConnectionConstants.URL;
import static org.reactivesource.mysql.ConnectionConstants.USERNAME;
import static org.reactivesource.testing.TestConstants.INTEGRATION;
import static org.testng.Assert.*;

public class ReactiveSourceMysqlIntegrationTest {

    ConnectionProvider connectionProvider = new MysqlConnectionProvider(URL, USERNAME, PASSWORD);

    MyEventListener eventListener;

    @BeforeMethod(groups = INTEGRATION)
    public void setup() {
        new DbInitializer().setupDb();
        eventListener = spy(new MyEventListener(new MyEntityExtractor()));
        cleanupDatabase();
    }

    @SuppressWarnings("unchecked")
    @Test(groups = INTEGRATION)
    public void testReactiveDatasourceBehaviorForMysqlEventSource() throws InterruptedException {
        int ENTITIES = 10;
        // create new ReactiveEventSource
        MysqlEventSource eventSource = new MysqlEventSource(connectionProvider, TEST_TABLE_NAME);
        ReactiveSource<String> rds = new ReactiveSource<>(eventSource);

        // add new eventListener
        rds.addEventListener(eventListener);
        rds.start();
        sleep(200L); //wait for the poller to start

        // insert new entities
        for (int i = 0; i < ENTITIES; i++) {
            insertNewRow(i + 1, "someValue" + i);
        }

        // wait for database to be queried and verify all the insertion events arrived
        sleep(1000L);
        verify(eventListener, times(ENTITIES)).onEvent(any(Event.class));

        // cleanup the database and make sure that the delete events will arrive
        cleanupDatabase();
        sleep(1000L);

        // stop the ReactiveDatasource
        rds.stop();
        sleep(1000L);

        verify(eventListener, times(2 * ENTITIES)).onEvent(any(Event.class));
    }

    @SuppressWarnings("unchecked")
    @Test(groups = INTEGRATION)
    public void testReactiveDatasourceBehaviorForMysqlEventSourceWithTwoReactiveSourcesForSameTable()
            throws InterruptedException {
        int ENTITIES = 10;
        // create new ReactiveEventSource
        MysqlEventSource eventSource1 = new MysqlEventSource(connectionProvider, TEST_TABLE_NAME);
        MysqlEventSource eventSource2 = new MysqlEventSource(connectionProvider, TEST_TABLE_NAME);

        ReactiveSource<String> rds1 = new ReactiveSource<>(eventSource1);
        ReactiveSource<String> rds2 = new ReactiveSource<>(eventSource2);

        // add new eventListener
        rds1.addEventListener(eventListener);
        rds2.addEventListener(eventListener);

        rds1.start();
        rds2.start();

        sleep(200L); //wait for the pollers to start

        // insert new entities
        for (int i = 0; i < ENTITIES; i++) {
            insertNewRow(i + 1, "someValue" + i);
        }

        // wait for database to be queried and verify all the insertion events arrived
        sleep(1000L);
        verify(eventListener, times(2 * ENTITIES)).onEvent(any(Event.class));

        // cleanup the database and make sure that the delete events will arrive
        cleanupDatabase();
        sleep(1000L);

        // stop the ReactiveDatasource
        rds1.stop();
        rds2.stop();
        sleep(1000L);

        verify(eventListener, times(4 * ENTITIES)).onEvent(any(Event.class));
    }

    private void insertNewRow(int id, String value) {
        try {
            Connection connection = connectionProvider.getConnection();
            PreparedStatement stmt = connection.prepareStatement("INSERT INTO " + TEST_TABLE_NAME + " VALUES (?, ?)");
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
            stmt.executeUpdate("DELETE FROM " + TEST_TABLE_NAME);
            stmt.close();
            connection.close();
        } catch (SQLException sqle) {
            fail("Failed to cleanup database", sqle);
        }
    }

    class MyEventListener extends EventListener<String> {
        public MyEventListener(MyEntityExtractor extractor) {
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
