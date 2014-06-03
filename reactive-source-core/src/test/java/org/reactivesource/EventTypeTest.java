/*******************************************************************************
 * Copyright (c) 2013-2014 eBay Software Foundation
 *
 * See the file license.txt for copying permission.
 ******************************************************************************/

package org.reactivesource;

import com.google.common.collect.Lists;
import org.testng.annotations.Test;

import java.util.List;

import static org.reactivesource.testing.TestConstants.SMALL;
import static org.testng.Assert.*;

public class EventTypeTest {

    @Test(groups = SMALL)
    public void testForValueWorksCorrectlyForValidValues() {
        assertEquals(EventType.forValue("INSERT"), EventType.INSERT);
        assertEquals(EventType.forValue("UPDATE"), EventType.UPDATE);
        assertEquals(EventType.forValue("DELETE"), EventType.DELETE);
    }

    @Test(groups = SMALL, expectedExceptions = IllegalArgumentException.class)
    public void testForValueThrowsExceptionForInvalidValues() {
        EventType.forValue("WRONG");
    }

    @Test(groups = SMALL)
    public void testGetValue() {
        List<String> values = Lists.newArrayList("INSERT", "UPDATE", "DELETE");
        for (String value : values) {
            assertEquals(EventType.forValue(value).getValue(), value);
        }
    }
}
