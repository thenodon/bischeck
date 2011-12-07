<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:template match="/">
<html>
<link rel="stylesheet" type="text/css" href="/home/andersh/development/bischeck/mystyle.css" />   

<head><title>bischeck configuration</title></head>
<body>

<xsl:for-each select="bischeck/host">
    <xsl:variable name="currentHost" select="name"/>
    <p id="headerhost">
    <a href="#{name}"> <xsl:value-of select="name"/> </a>
    </p>
    <xsl:for-each select="service">
        <p id="headerservice"> 
        <a href="#{$currentHost}-{name}"> <xsl:value-of select="$currentHost"/>-<xsl:value-of select="name"/> </a>
        </p>
    </xsl:for-each>
</xsl:for-each>

<xsl:for-each select="bischeck/host">

    
    <p id="headerhost">
    <a name="{name}"> <xsl:value-of select="name"/> </a>
    </p>
    <p><xsl:value-of select="desc"/> </p>
    

    

    
    
    <xsl:for-each select="service">
        <p id="headerservice"> 
        <xsl:value-of select="name" /></p>
        <p><xsl:value-of select="desc" /></p>
        <p><b>Schedules:</b></p>
        <ul>
        <xsl:apply-templates select="schedule"/>
        </ul>
        <p><b>Url:&#160;</b> <xsl:value-of select="url" /></p>   
        <p><b>Driver:&#160;</b> <xsl:value-of select="driver" /></p> 

        
        
        <xsl:for-each select="serviceitem">
        <p id="headerserviceitem">
        <xsl:value-of select="name" /></p>
            <p><xsl:value-of select="desc" /></p>
            <p><b>Exec:&#160;</b> <xsl:value-of select="execstatement" /></p>
            <p><b>Threshold class:&#160;</b> <xsl:value-of select="thresholdclass" /></p>
            <p><b>Service Item class:&#160;</b> <xsl:value-of select="serviceitemclass" /></p>
            
        </xsl:for-each>

    </xsl:for-each>

</xsl:for-each>

</body>
</html>
</xsl:template>
    
<xsl:template match="schedule">
<li><xsl:apply-templates/></li>
</xsl:template>

</xsl:stylesheet>