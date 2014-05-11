
MySQL Reactive Module
=====

This module adds support of MySQL database system.

Creating A MysqlEventSource
------

You can create a MysqlEventSource in the following way:

    MysqlEventSource eventSource = new MysqlEventSource(connectionProvider, TABLE_NAME);

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
