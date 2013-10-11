#!/bin/bash

function usage {
    echo "Usage: $0: [-u] -i interval -m services"
    echo "-m the number of services to create, default 100"
    echo "-i interval to run services, default every minute (1M)"
    echo "-u show usage"
    echo ""
    echo "Generate the bischeck.xml and 24thresholds.xml files used for benchmark"
    echo "testing"
}

schedule=1M
max=100

while getopts ui:m: name
do
    case $name in
        i) schedule="$OPTARG";;
        m) max="$OPTARG";;
        u) usage;exit 0;;
    esac
done

shift $(($OPTIND - 1))

count=0
tmp24file="24.tmp"
tmpbischeckfile="bis.tmp"

> $tmp24file
> $tmpbischeckfile

while [ $count -lt $max ] 
do
  
  echo "        <member>" >> $tmp24file
  echo "           <hostname>hostXX</hostname>" | sed -e "s/XX/$count/" >> $tmp24file
  echo "            <servicename>avgrand</servicename>" >> $tmp24file
  echo "            <serviceitemname>avg</serviceitemname>" >> $tmp24file
  echo "        </member>" >> $tmp24file

  echo "    <host>" >> $tmpbischeckfile 
  echo "        <name>hostXX</name>" | sed -e "s/XX/$count/" >> $tmpbischeckfile 
  echo "        <alias>127.0.0.1</alias>" >> $tmpbischeckfile 
  echo "        <desc></desc>" >> $tmpbischeckfile 
  echo "        <service>" >> $tmpbischeckfile 
  echo "          <template>randtemplate</template>" >> $tmpbischeckfile 
  echo "        </service>" >> $tmpbischeckfile 
  echo "        <service>" >> $tmpbischeckfile 
  echo "          <template>avgrandtemplate</template>" >> $tmpbischeckfile 
  echo "        </service>" >> $tmpbischeckfile 
  echo "    </host>" >> $tmpbischeckfile
    
  count=`expr $count + 1 `
done

cat template/24threshold_head.temp > 24thresholds.xml
cat $tmp24file >> 24thresholds.xml
cat template/24threshold_foot.temp >> 24thresholds.xml


cat template/bischeck_head.temp > bischeck.xml
cat $tmpbischeckfile >> bischeck.xml
cat template/bischeck_foot.temp | sed -e "s/SCHEDULE/$schedule/" >> bischeck.xml

rm $tmp24file
rm $tmpbischeckfile

