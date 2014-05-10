/*******************************************************************************
 * Copyright (c) 2013-2014 eBay Software Foundation
 *
 * See the file license.txt for copying permission.
 ******************************************************************************/
package org.reactivesource;

import java.util.Map;

import static org.reactivesource.util.Assert.notNull;

/**
 * The {@link org.reactivesource.EventListener} is passed to a {@link org.reactivesource.ReactiveSource}. This way, for
 * every new event that happens on the {@link org.reactivesource.ReactiveSource} the {@link #onEvent(Event)} method of
 * the EventListener will be called.
 * <p/>
 * You only have to implement the {@link #onEvent(Event)} method with the functionality you desire for each event.
 *
 * @param <T> the class of the entities monitored from the {@link org.reactivesource.ReactiveSource}
 * @see  org.reactivesource.ReactiveSource
 */
public abstract class EventListener<T> {

    private EntityExtractor<T> entityExtractor;

    public EventListener(EntityExtractor<T> entityExtractor) {
        notNull(entityExtractor, "entityExtractor can not be null");
        this.entityExtractor = entityExtractor;
    }

    /**
     * This method is called every time there is a new event for the listener.
     * <p/>
     * Implement with the logic you want per event.
     *
     * @param event the {@link org.reactivesource.Event} that occurred.
     */
    public abstract void onEvent(Event<T> event);

    /**
     * Internal method that handles the event transformation and forwarding.
     *
     * @param event
     */
    void notifyEvent(Event<Map<String, Object>> event) {
        Event<T> parsedEvt = new Event<>(event.getEventType(),
                event.getEntityName(),
                entityExtractor.extractEntity(event.getNewEntity()),
                entityExtractor.extractEntity(event.getOldEntity()));
        onEvent(parsedEvt);
    }
}
