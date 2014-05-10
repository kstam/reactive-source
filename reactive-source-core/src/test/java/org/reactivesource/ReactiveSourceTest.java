/*******************************************************************************
 * Copyright (c) 2013-2014 eBay Software Foundation
 *
 * See the file license.txt for copying permission.
 ******************************************************************************/
package org.reactivesource;

import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.reactivesource.testing.TestConstants.*;
import static org.testng.Assert.*;

public class ReactiveSourceTest {

    @Mock
    private EventChannel<Integer> channel;
    @Mock
    private EventListener<Integer> listener;
    @Mock
    private EventPoller<Integer> poller;

    private ReactiveSource<Integer> reactiveSource;

    @BeforeMethod(groups = SMALL)
    public void setUp() {
        initMocks(this);
        reactiveSource = new ReactiveSource<Integer>(channel, poller);
    }

    @Test(groups = SMALL, expectedExceptions = IllegalArgumentException.class)
    public void testCanNotBeInitializedWithNullEventSource() {
        new ReactiveSource<Integer>(null);
    }

    @Test(groups = SMALL, expectedExceptions = IllegalArgumentException.class)
    public void testCanNotAddANullEventListener() {
        reactiveSource.addEventListener(null);
    }

    @Test(groups = SMALL)
    public void testAddingEventListenerAddsTheListenerToTheChannel() {
        reactiveSource.addEventListener(listener);
        verify(channel).addEventListener(listener);
    }

    @Test(groups = SMALL)
    public void testReactiveSourceIsNotStartedWhenInstantiated() {
        assertFalse(reactiveSource.isStarted());
    }

    @Test(groups = SMALL)
    public void testCanStartTheReactiveDatasource() {
        reactiveSource.start();
        assertTrue(reactiveSource.isStarted());
    }

    @Test(groups = SMALL)
    public void testCanStopTheReactiveDatasource() throws InterruptedException {
        reactiveSource.start();
        assertTrue(reactiveSource.isStarted());

        reactiveSource.stop();
        verify(poller).stop();
        Thread.sleep(100L);
        assertFalse(reactiveSource.isStarted());
    }
}
