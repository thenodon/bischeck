create database if not exists bischecktest;
grant all on bischecktest.* to 'bischeck'@'localhost' IDENTIFIED BY 'bischeck';
drop table if exists bischecktest.test;
create table bischecktest.test (id int NOT NULL AUTO_INCREMENT, value int NOT NULL, createdate date, PRIMARY KEY (id));
insert into bischecktest.test (value, createdate) values (1000,now());
insert into bischecktest.test (value, createdate) values (2000,now());

