/*******************************************************************************
 * Copyright (c) 2013-2014 eBay Software Foundation
 *
 * See the file license.txt for copying permission.
 ******************************************************************************/
package org.reactivesource;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.reactivesource.util.Assert.notNull;

public class ReactiveSource<T> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private EventChannel<T> eventChannel;
    private EventPoller<T> eventPoller;
    private Thread pollerDaemon;

    public ReactiveSource(EventSource eventSource) {
        this(eventSource, new EventChannel<T>());
    }

    @VisibleForTesting ReactiveSource(EventSource eventSource, EventChannel<T> eventChannel) {
        this(eventChannel, new EventPoller<>(eventSource, eventChannel));
    }

    @VisibleForTesting ReactiveSource(EventChannel<T> eventChannel, EventPoller<T> eventPoller) {
        super();
        logger.info("Initializing ReactiveDatasource");
        notNull(eventChannel, "eventChannel can not be null");
        notNull(eventPoller, "eventPoller can not be null");
        this.eventChannel = eventChannel;
        this.eventPoller = eventPoller;
        this.pollerDaemon = null;
    }

    public void addEventListener(EventListener<T> listener) {
        logger.info("Adding listener to ReactiveDatasource.");
        notNull(listener, "Can not add null eventListener");
        eventChannel.addEventListener(listener);
    }

    /**
     * @return true if the {@link ReactiveSource} is started. Returns false if stopped.
     */
    public boolean isStarted() {
        return pollerDaemon != null;
    }

    /**
     * Starts monitoring the {@link EventSource} associated with this {@link ReactiveSource}
     */
    public void start() {
        if (!isStarted()) {
            logger.info("Starting ReactiveDatasource");
            pollerDaemon = new Thread(eventPoller);
            pollerDaemon.setDaemon(true);
            pollerDaemon.start();
        }
    }

    /**
     * <p>
     * Stops monitoring the {@link EventSource} associated with this {@link ReactiveSource}.
     * </p>
     *
     * <p>
     * If you start the {@link ReactiveSource} again, any events that occurred in eventSource the between stopping
     * the {@link ReactiveSource} and starting it again will be lost.
     * </p>
     */
    public void stop() {
        if (isStarted()) {
            logger.info("Stopping ReactiveDatasource");
            eventPoller.stop();
            pollerDaemon = null;
        }
    }
}
