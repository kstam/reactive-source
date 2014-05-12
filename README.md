reactive-source
===============

Open source framework to turn your database into a reactive stream of information.

Instead of you querying the database for new events, let the "database" (aka the reactive-source framework) notify you
that there is new or updated data available.

Currently supported database systems:

* **Postgres**
* **MySQL**

Upcoming database systems:

* **MongoDB**

Usage and Examples
--------

Using the framework is pretty straight forward.

In the following example you can see how you can start getting (next to real-time) events for a Mysql table.

    //Define an event listener that receives events with meaningful objects
    class PurchaseEventListener extends EventListener<Purchase> {
        PurchaseEventListener(PurchaseEntityExtractor extractor) {
            super(extractor);
        }

        public void onEvent(Event<Purchase> event) {
            //apply your business logic for the new event here
        }
    }

    //Define an extractor that converts data in maps to your business object
    class PurchaseEntityExtractor implements EntityExtractor<Purchase> {
        @Override public Purchase extractEntity(Map<String, Object> entityData) {
            Purchase purchase = new Purchase();
            // create the purchase object from a map of data
            return purchase;
        }
    }

    //Define your event source
    MysqlEventSource eventSource = new MysqlEventSource(URL, USERNAME, PASSWORD, TABLE_NAME);
    //Create a reactiveSource
    ReactiveSource<Purchase> reactiveSource = new ReactiveSource<>(eventSource);
    reactiveSource.addEventListener(new PurchaseEventListener(new PurchaseEntityExtractor()));

    //start the reactive source
    reactiveSource.start();

    // YEYY!! you are getting events

    //stop the reactive source when you don't need it any more
    reactiveSource.stop();


This example was for a MySQL database.

All you would need to change to use another database (ie PostgreSQL) would be creating a PsqlEventSource instead of
a MysqlEventSource and passing it to the ReactiveSource.

Provided the same classes exist here is :

    //Define your event source
    PsqlEventSource eventSource = new PsqlEventSource(URL, USERNAME, PASSWORD, TABLE_NAME); //only line that changed
    //Create a reactiveSource
    ReactiveSource<Purchase> reactiveSource = new ReactiveSource<>(eventSource);
    reactiveSource.addEventListener(new PurchaseEventListener(new PurchaseEntityExtractor()));

    //start the reactive source
    reactiveSource.start();

    // YEYY!! you are getting events

    //stop the reactive source when you don't need it any more
    reactiveSource.stop();

What if I want a source that is not supported?
-------

If you have ideas about what other types of sources we could support feel free to contact me at kstamatoukos@ebay.com

Even better, you could implement the add in for the new source type yourself and thus contribute to our framework.

We will be happy to hear from you in case you want to contribute.


