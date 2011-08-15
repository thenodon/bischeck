delete from properties;
delete from  hosts;
delete from services;
delete from items;

PRAGMA foreign_keys=OFF;
BEGIN TRANSACTION;
INSERT INTO "properties" VALUES('nscaserver','localhost');
INSERT INTO "properties" VALUES('nscaencryption','XOR');
INSERT INTO "properties" VALUES('nscapassword','28016183115911');
INSERT INTO "properties" VALUES('nscaport','5667');
INSERT INTO "properties" VALUES('checkinterval','30');
INSERT INTO "properties" VALUES('pidfile','/var/tmp/bischeck.pid');
INSERT INTO "properties" VALUES('bischeckserver','bischeck');
INSERT INTO "properties" VALUES('JDBCSerivce.querytimeout','5');

INSERT INTO "hosts" VALUES(1,'verify1','Verify server 1',1);


INSERT INTO "services" VALUES(1,1,'verifydbdate','verify database with date','jdbc:mysql://localhost/bischeckverify?user=bischeck&password=bischeck','com.mysql.jdbc.Driver',1);
INSERT INTO "services" VALUES(2,1,'verifydbvalue','verify database with value','jdbc:mysql://localhost/bischeckverify?user=bischeck&password=bischeck','com.mysql.jdbc.Driver',1);
INSERT INTO "services" VALUES(3,1,'verifycache','verify last status cache','bischeck:cache://',null,1);

-- For testing - create table verdate (value1 int, value2 int, createdate date);
INSERT INTO "items" VALUES(1,1,'verdatecurrent','verify date current','select value1 from verdate where createdate=''%%yyyy-MM-dd%%''','com.ingby.socbox.bischeck.threshold.Twenty4HourThreshold',1);
INSERT INTO "items" VALUES(2,1,'verdateyesterday','verify date yesterday','select value1 from verdate where createdate=''%%yyyy-MM-dd%[D-1]%%''','com.ingby.socbox.bischeck.threshold.Twenty4HourThreshold',1);

-- For testing - create table verval (value int);
INSERT INTO "items" VALUES(1,2,'vervalue','verify value','select value from verval','com.ingby.socbox.bischeck.threshold.Twenty4HourThreshold',1);

INSERT INTO "items" VALUES(1,3,'diffifdate','diffif','if((verify1-verifydbdate-verdatecurrent[0] - verify1-verifydbdate-verdateyesterday[1]) < 0,0,verify1-verifydbdate-verdatecurrent[0] - verify1-verifydbdate-verdateyesterday[1])','com.ingby.socbox.bischeck.threshold.Twenty4HourThreshold',1);

INSERT INTO "items" VALUES(2,3,'diffvalue','diff','verify1-verifydbvalue-vervalue[0] - verify1-verifydbvalue-vervalue[1]','com.ingby.socbox.bischeck.threshold.Twenty4HourThreshold',1);
COMMIT;
