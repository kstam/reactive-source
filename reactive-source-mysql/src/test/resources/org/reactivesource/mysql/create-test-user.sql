GRANT USAGE ON *.* TO 'reactive_test'@'localhost';
DROP USER 'reactive_test'@'localhost';

CREATE USER 'reactive_test'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON reactive_test.* TO 'reactive_test'@'localhost' WITH GRANT OPTION;
