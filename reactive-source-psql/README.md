
Postgres Reactive Module
=====

This module adds support of PostgreSQL database system.

Creating A PsqlEventSource
------

You can create a PsqlEventSource in any of the following ways:


Auto Configuration mode
-------

There are two modes for the framework to run.

- Auto configuration mode
- Manual configuration mode

By default the auto-configuration is enabled which means that the framework will attempt to setup the database.

##### Auto Configuration Mode

The framework will try to configure the database in your behalf.

In that case, the user you are connecting to the database with, needs to have the following priviledges:

- USAGE (In order to set up a stored procedure)
- TRIGGER (In order to setup the needed triggers)

##### Manual Configuration Mode

It is your responsibility to configure the database

Enabling manual configuration

    PsqlEventSource eventSource = new PsqlEventSource(connectionProvider, "tableName", false);

In that case, it is your responsibility to setup the database accordingly.

Specifically, you will need to:
- install the notify_with_json() stored procedure in your psql server.
-


Want to contribute? Checking out and building the project
--------

You will need to setup a local instance of a PostgreSQL database for the integration test to get executed.

Once you have installed a local instance of PostgreSQL, you will need to create the test database and user that are needed for the tests to run.

Checkout the code. Under the directory {CODE_DIR}/src/test/resources/scipts/psql you will find 2 sql. You only need the one named **create-db-and-user.sql**.

So the steps are:

1. Run the **create-db-and-user.sql** file with a user that has the privilege to create Databases and Roles.

    `
    psql -h localhost -U <USERNAME> -d <EXISTING_DB_NAME> -f create-db-and-user.sql
    `

2. Build the project. While on {CODE_DIR} run

    `
    mvn clean package
    `

Thats all!