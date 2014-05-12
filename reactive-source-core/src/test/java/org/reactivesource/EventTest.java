/*******************************************************************************
 * Copyright (c) 2013-2014 eBay Software Foundation
 *
 * See the file license.txt for copying permission.
 ******************************************************************************/
package org.reactivesource;

import org.testng.annotations.Test;

import static org.reactivesource.testing.TestConstants.*;
import static org.testng.Assert.*;

public class EventTest {
    private static final String DATA_NEW = "data";
    private static final String DATA_OLD = "dataOld";
    private static final String TABLE_NAME = "tableName";

    @Test(groups = SMALL, expectedExceptions = IllegalArgumentException.class)
    public void testCanNotInitializeWithNullEventType() {
        new Event<>(null, TABLE_NAME, DATA_NEW, DATA_OLD);
    }

    @Test(groups = SMALL, expectedExceptions = IllegalArgumentException.class)
    public void testCanNotInitializeWithNullData() {
        new Event<>(EventType.DELETE, TABLE_NAME, null, DATA_OLD);
    }

    @Test(groups = SMALL, expectedExceptions = IllegalArgumentException.class)
    public void testCanNotInitializeWithNullTableName() {
        new Event<>(EventType.DELETE, null, DATA_NEW, DATA_OLD);
    }

    @Test(groups = SMALL, expectedExceptions = IllegalArgumentException.class)
    public void testCanNotInitializeWithEmptyTableName() {
        new Event<>(EventType.DELETE, "", DATA_NEW, DATA_OLD);
    }

    @Test(groups = SMALL)
    public void testCanInitializeWithCorrectValues() {
        Event<String> event = new Event<>(EventType.DELETE, TABLE_NAME, DATA_NEW, DATA_OLD);

        assertNotNull(event);
        assertEquals(event.getEventType(), EventType.DELETE);
        assertEquals(event.getEntityName(), TABLE_NAME);
        assertEquals(event.getNewEntity(), DATA_NEW);
        assertEquals(event.getOldEntity(), DATA_OLD);

    }

}
