<?php
#
# Copyright (c) 2011 Anders HÃ¥Ã¥l, based on check_load
# Plugin: check_bischeck
#
$opt[1] = "--title \"$hostname / $servicedesc\" ";
#
#
#
$def[1] = rrd::def("var1", $RRDFILE[1], $DS[1], "AVERAGE");
$def[1] .= rrd::def("var2", $RRDFILE[1], $DS[2], "AVERAGE");


#if ($WARN[1] != "") {
#    $def[1] .= "HRULE:$WARN[1]#FFFF00 ";
#}

#if ($CRIT[1] != "") {
#    $def[1] .= "HRULE:$CRIT[1]#FF0000 ";
#}
$def[1] .= rrd::area("var1", "#EACC00", "Measured  ");
$def[1] .= rrd::gprint("var1", array("LAST", "AVERAGE", "MAX"), "%6.0lf");
#$def[1] .= rrd::area("var2", "#EA8F00B0", "Threshold ") ;
$def[1] .= rrd::line2("var2", "#555210", "Threshold ") ;
$def[1] .= rrd::gprint("var2", array("LAST", "AVERAGE", "MAX"), "%6.0lf");

if($WARN[1] != ""){
        $def[1] .= rrd::hrule($WARN[1], "#FFFF00", "Warning   ".$WARN[1].$UNIT[1]."\\n");
}
if($CRIT[1] != ""){
        $def[1] .= rrd::hrule($CRIT[1], "#FF0000", "Critical  ".$CRIT[1].$UNIT[1]."\\n");
}
?>
