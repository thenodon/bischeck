<?php
#
# Copyright (c) 2012 Anders Håål Ingenjörsbyn, based on check_load
# Plugin: check_bischeck
#
#
#
#

$index=1;
foreach($DS as $i => $VAL){
    if(!isset($def[$index])){
        $def[$index] = "";
    }
    $def[$index] .= "DEF:var$i=$RRDFILE[$i]:$DS[$i]:AVERAGE " ;
    if(!preg_match('/^threshold$/',$NAME[$i], $matches)){
        $opt[$index] = "--title \"$hostname / $servicedesc / $NAME[$i] \" ";
        $def[$index] .= rrd::gradient("var$i", "ff5c00", "ffdc00", "$NAME[$i]", 20 ) ;
        $def[$index] .= rrd::line1("var$i", "#000000");
        $def[$index] .= rrd::gprint("var$i", "LAST", "%6.0lf \\n");  
	} else {
        $def[$index] .= rrd::line2("var$i", "#555210", "$NAME[$i]") ;
        $def[$index] .= rrd::gprint("var$i", "LAST", "%6.0lf \\n");
        $val=$i-1;
        if($WARN[$val] != ""){
            $def[$index] .= rrd::hrule($WARN[$val], "#FFFF00", "Warning   ".$WARN[$val].$UNIT[$val]."\\n");
        }
        if($CRIT[$val] != ""){
            $def[$index] .= rrd::hrule($CRIT[$val], "#FF0000", "Critical  ".$CRIT[$val].$UNIT[$val]."\\n");
        }
        $index=$index+1;
    } 
}
 
?>

