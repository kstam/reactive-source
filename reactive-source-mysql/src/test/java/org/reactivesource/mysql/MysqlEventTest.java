/*******************************************************************************
 * Copyright (c) 2013-2014 eBay Software Foundation
 *
 * See the file license.txt for copying permission.
 ******************************************************************************/

package org.reactivesource.mysql;

import org.reactivesource.EventType;
import org.testng.annotations.Test;

import static org.reactivesource.mysql.ConnectionConstants.TEST_TABLE_NAME;
import static org.reactivesource.testing.DateConstants.TODAY;
import static org.reactivesource.testing.DateConstants.YESTERDAY;
import static org.reactivesource.testing.TestConstants.SMALL;
import static org.testng.Assert.*;

public class MysqlEventTest {

    @Test(groups = SMALL, expectedExceptions = IllegalArgumentException.class)
    public void testCanNotBeInitializedWithNegativeEventId() {
        new MysqlEvent(-1, TEST_TABLE_NAME, EventType.DELETE, "{}", "{}", TODAY);
    }

    @Test(groups = SMALL, expectedExceptions = IllegalArgumentException.class)
    public void testCanNotBeInitializedWithNullDate() {
        new MysqlEvent(1, TEST_TABLE_NAME, EventType.INSERT, "{}", "{}", null);
    }

    @Test(groups = SMALL)
    public void testEqualsForSameObject() {
        MysqlEvent event1 = new MysqlEvent(1, TEST_TABLE_NAME, EventType.INSERT, "{}", "{}", TODAY);
        MysqlEvent event2 = new MysqlEvent(1, TEST_TABLE_NAME, EventType.INSERT, "{}", "{}", TODAY);
        MysqlEvent event3 = new MysqlEvent(1, TEST_TABLE_NAME, EventType.INSERT, "{}", "{}", YESTERDAY);

        assertEquals(event1, event1);
        assertEquals(event1, event2);
        assertNotEquals(event1, event3);
    }

}
