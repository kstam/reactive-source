
MySQL Reactive Module
=====

This module adds support of MySQL database system.

Creating A MysqlEventSource
------

You can create a MysqlEventSource in the following way:

    MysqlEventSource eventSource1 = new MysqlEventSource(connectionProvider, TABLE_NAME);
    MysqlEventSource eventSource2 = new MysqlEventSource(connectionProvider, TABLE_NAME, AUTO_CONFIGURE);

    MysqlEventSource eventSource3 = new MysqlEventSource(URL, USERNAME, PASSWORD, TEST_TABLE_NAME);
    MysqlEventSource eventSource4 = new MysqlEventSource(URL, USERNAME, PASSWORD, TEST_TABLE_NAME, AUTO_CONFIGURE);

_Note_: AUTO_CONFIGURE defaults to **false** when not specified.

User Privileges for MysqlEventSource
--------
In order the MysqlEventSource to work properly you must connect with a user that has the following privileges on the
monitored tables:

- TRIGGER

The user you connect to the MysqlEventSource with should also have the following privileges on the REACTIVE tables
(REACTIVE_LISTENER and REACTIVE_EVENT):

- INSERT
- UPDATE
- SELECT
- DELETE

### Only when auto-config is activated
If you **enable auto-confire** you need to make sure the user also has the following privileges on the REACTIVE tables:

- CREATE (used for creating the reactive tables if they are not there)
- EVENT (used for scheduled cleanup event)

### Why do I need these privileges?

MySQL doesn't natively support LISTEN/NOTIFY functionality. This framework is taking care of that for you, but in order
to do so, it needs to create triggers on the monitored tables.

Checking out and building the project
--------

You will need to setup a local instance of a MySQL database for the integration test to get executed.

##### Setup database for the tests

To help you configure your database for running the tests we have created a small SQL script that does it for you.

You can find the script at src/test/resources/org/reactivesource/mysql/init-test-db.sql

You will need to run the script as a superuser(root) in order to setup everything correctly

_Example_

    $ mysql -u root -p password < init-test-db.sql

this will setup your db for running the tests.

##### Build the project

The project is built with maven

under the parent directory run

    mvn clean install

And thats all :)
