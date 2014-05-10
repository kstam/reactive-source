/*******************************************************************************
 * Copyright (c) 2013-2014 eBay Software Foundation
 *
 * See the file license.txt for copying permission.
 ******************************************************************************/

package org.reactivesource.mysql;

import org.reactivesource.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static java.lang.Thread.sleep;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.reactivesource.testing.TestConstants.*;
import static org.reactivesource.mysql.ConnectionConstants.*;
import static org.testng.Assert.*;

public class ReactiveSourceMysqlIntegrationTest {

    ConnectionProvider connectionProvider = new MysqlConnectionProvider(URL, USERNAME, PASSWORD);

    MyEventListener eventListener;

    @BeforeMethod(groups = INTEGRATION)
    public void setup() throws IOException, SQLException {
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
            insertNewRow(i+1, "someValue" + i);
        }

        // wait for database to be queried and verify all the insertion events arrived
        sleep(1000L);
        verify(eventListener, times(ENTITIES)).onEvent(any(Event.class));

        // stop the ReactiveDatasource
        cleanupDatabase();
        sleep(1000L);

        rds.stop();
        // cleanup the database and make sure that the delete events will arrive

        verify(eventListener, times(2 * ENTITIES)).onEvent(any(Event.class));
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
