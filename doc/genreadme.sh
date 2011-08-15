#!/bin/bash

# Remove unicode info
awk '{if(NR==1)sub(/^\xef\xbb\xbf/,"");print}' README.txt > readme_tmp

while read line; do 
# if line is chapter
echo $line | grep '^$$.' >/dev/null
if [ $? = 0 ] ; then 
    n=$((++n))
    echo "$line"|sed -e "s/\$\$./$n\./" 
else
    echo "$line"
fi
done < readme_tmp > readme.tmp

cat readme.tmp | grep '^[0-9]*\.' > content_tmp

awk '/\%\%SPLIT\%\%/{close("readme.tmp_"f);f++}{print $0 > "readme.tmp_"f}' readme.tmp

cat readme.tmp_1 > readme_tmp
cat content_tmp >> readme_tmp
cat readme.tmp_2 >> readme_tmp
cat readme_tmp | sed -e 's/\%\%SPLIT\%\%//' | fold -s | sed ':a; s/^\(_*\)_\([^_]\)/\1\ \2/; t a'> README
rm readme.tmp readme.tmp_1 readme.tmp_2 content_tmp 
rm readme_tmp
