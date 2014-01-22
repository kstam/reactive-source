package com.ebay.epd.reactivedb.psql;

enum PsqlEventType {
    INSERT, UPDATE, DELETE;

    static boolean contains(String value) {
        for (PsqlEventType e : values()) {
            if (e.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
