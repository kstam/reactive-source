ReactiveSource Core Module
=========

The core module contains the definition of the abstraction and the implementation of the core functionality of the framework.

It also defines the APIs of what needs to be implemented in order to extend the framework by adding support for more sources.

Using ReactiveSources for your project
--------

Using the framework is pretty straight forward.

In the following snippet you can see how you can start getting (next to realtime) events for a Mysql table.

To simplify the example we assume that there is already a connectionProvider defined.

    //Define an event listener that receives events with meaningful objects
    class PurchaseEventListener implements EventListener<Purchase> {
        PurchaseEventListener(PurchaseEntityExtractor extractor) {
            super(extractor);
        }

        public void onEvent(Event<Purchase> event) {
            Purcahse newPurchase = event.getNewEntity();
            //do your meaningful stuff
        }
    }

    //Define an extractor that converts data in maps to your business object
    class PurchaseEntityExtractor implements EntityExtractor<Purchase> {
        @Override public Purchase extractEntity(Map<String, Object> entityData) {
            // create the purchase object
        }
    }

    //Define your event source
    MysqlEventSource eventSource = new MysqlEventSource(connectionProvider, "table");
    //Create a reactiveSource
    ReactiveSource reactiveSource = new ReactiveSource(eventSource);
    reactiveSource.addListener(new PurchaseEventListener(new PurchaseEntityExtractor());

    //start the reactive source
    reactiveSource.start();

    // YEYY!! you are getting events

    //stop the reactive source when you don't need it any more
    reactiveSource.stop();

That is all the code you neeed to start getting (almost realtime) events from any Mysql table.

There is already support also for PostgreSQL.

Provided the API, all you would need to change to use Psql instead would be creating a PsqlEventSource instead of a MysqlEventSource

The rest of your code can remain exactly the same.

What if I want a source that is not supported?
-------

If you have ideas about what other types of sources we could support feel free to contact me at kstamatoukos@ebay.com

Even better, you could implement the add in for the new source type yourself and thus contribute to our framework.

We will be happy to hear from you in case you want to contribute.


