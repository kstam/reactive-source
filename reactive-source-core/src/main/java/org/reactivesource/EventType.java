/*******************************************************************************
 * Copyright (c) 2013-2014 eBay Software Foundation
 *
 * See the file license.txt for copying permission.
 ******************************************************************************/

package org.reactivesource;

public enum EventType {
    INSERT("INSERT"),
    UPDATE("UPDATE"),
    DELETE("DELETE");

    private String value;

    EventType(String value) {
        this.value = value;
    }

    public static EventType forValue(String value) {
        for (EventType eventType : values()) {
            if (eventType.value.equals(value)) {
                return eventType;
            }
        }
        throw new IllegalArgumentException(String.format("%s is not a valid EventType value", value));
    }

    public String getValue() {
        return value;
    }
}
