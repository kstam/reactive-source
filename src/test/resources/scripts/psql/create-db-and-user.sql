DROP DATABASE IF EXISTS test_dbevents;

DROP ROLE IF EXISTS test;
CREATE ROLE test WITH CREATEDB LOGIN PASSWORD 'password';

CREATE DATABASE test_dbevents;
GRANT ALL PRIVILEGES ON DATABASE test_dbevents TO test;
