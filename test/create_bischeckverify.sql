create database bischeckverify;
create user 'bischeck'@'localhost' IDENTIFIED BY 'bischeck';
grant usage on bischeckverify.* to 'bischeck'@'localhost';
grant all on bischeckverify.* to 'bischeck'@'localhost';
create table bischeckverify.verval (value int);
create table bischeckverify.verdate (value1 int, value2 int, createdate date);

