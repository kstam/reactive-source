/*******************************************************************************
 * Copyright (c) 2013-2014 eBay Software Foundation
 *
 * See the file license.txt for copying permission.
 ******************************************************************************/

package org.reactivesource.mysql;

import com.google.common.collect.Lists;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class MysqlEventRepoUtils {

    public static void insertEvent(MysqlEvent event, Connection connection) throws SQLException {
        try (
                PreparedStatement stmt = connection.prepareStatement(INSERT_QUERY)
        ) {
            stmt.setLong(1, event.getEventId());
            stmt.setString(2, event.getEntityName());
            stmt.setObject(3, event.getEventType().getValue());
            stmt.setObject(4, event.getOldEntity());
            stmt.setObject(5, event.getNewEntity());

            stmt.executeUpdate();
        }
    }

    public static void insertEventWithDate(MysqlEvent event, Connection connection) throws SQLException {
        try (
                PreparedStatement stmt = connection.prepareStatement(INSERT_WITH_DATE_QUERY)
        ) {
            stmt.setLong(1, event.getEventId());
            stmt.setString(2, event.getEntityName());
            stmt.setObject(3, event.getEventType().getValue());
            stmt.setObject(4, event.getOldEntity());
            stmt.setObject(5, event.getNewEntity());
            stmt.setObject(6, event.getCreatedDt());

            stmt.executeUpdate();
        }
    }

    public static List<MysqlEvent> getEventsForTable(String testTableName, Connection connection) throws SQLException {
        try (
                PreparedStatement stmt = connection.prepareStatement(GET_EVENTS_FOR_TABLE_QUERY);
        ) {

            stmt.setObject(1, testTableName);
            ResultSet rs = stmt.executeQuery();

            List<MysqlEvent> result = Lists.newArrayList();
            while (rs.next()) {
                result.add(MysqlEventRepo.extractEvent(rs));
            }

            return result;
        }
    }

    public static final String INSERT_QUERY = "INSERT INTO REACTIVE_EVENT VALUES (?, ?, ?, ?, ?, NOW())";
    public static final String INSERT_WITH_DATE_QUERY = "INSERT INTO REACTIVE_EVENT VALUES (?, ?, ?, ?, ?, ?)";
    private static final String GET_EVENTS_FOR_TABLE_QUERY = "SELECT * FROM REACTIVE_EVENT WHERE TABLE_NAME=?";

}
