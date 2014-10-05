#!/bin/sh

jmxjar=~/Downloads/cmdline-jmxclient-0.10.3.jar
jmxpasswd=kalle:
while [ 1 ]
do 
  cp conf1/bischeck.xml .
  cp conf1/24thresholds.xml .
  java -jar $jmxjar $jmxpasswd localhost:3333 com.ingby.socbox.bischeck:name=Execute reload
  java -jar $jmxjar $jmxpasswd localhost:3333 com.ingby.socbox.bischeck:name=Execute ReloadCount
  java -jar $jmxjar $jmxpasswd localhost:3333 com.ingby.socbox.bischeck:name=Execute ReloadTime
  sleep 60
  cp conf2/bischeck.xml .
  cp conf2/24thresholds.xml .
  java -jar $jmxjar $jmxpasswd localhost:3333 com.ingby.socbox.bischeck:name=Execute reload
  java -jar $jmxjar $jmxpasswd localhost:3333 com.ingby.socbox.bischeck:name=Execute ReloadCount
  java -jar $jmxjar $jmxpasswd localhost:3333 com.ingby.socbox.bischeck:name=Execute ReloadTime
  sleep 60

done
