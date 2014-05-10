/*******************************************************************************
 * Copyright (c) 2013-2014 eBay Software Foundation
 *
 * See the file license.txt for copying permission.
 ******************************************************************************/

package org.reactivesource;

import java.util.Map;

/**
 * The {@link org.reactivesource.EntityExtractor} is used to translate a {@link java.util.Map} containing the data of
 * the entity into a meaningful model object.
 * <p/>
 * <u>Example</u>
 * <p/>
 * Suppose you want to listen to events from a source that contains entries with the following format STUDENT(long id, String name)
 * <p/>
 * You will need to create an {@link org.reactivesource.EventListener} and pass it an implementation of the {@link org.reactivesource.EntityExtractor}
 * that will convert the map to a Student object
 * <pre>
 * {@code
 * public class StudentEntityExtractor implements EntityExtractor<Student> {
 *      public Student extractEntity(Map<String, Object> entityRow) {
 *          long id = (Long) entityRow.get("id");
 *          String name = (String) entityRow.get("name");
 *          return new Student(id, name);
 *      }
 * }
 * }
 * </pre>
 *
 * @param <T> the class of the model object of the entity
 */
public interface EntityExtractor<T> {
    T extractEntity(Map<String, Object> entityRow);
}
