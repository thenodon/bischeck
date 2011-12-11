<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:template match="/">
<html>
<link rel="stylesheet" type="text/css" href="bischeck.css" />   

<head><title>bischeck configuration</title></head>
<body>

<!-- Create header host and services toc -->
<div class="toc">
<xsl:for-each select="bischeck/host">
    <xsl:variable name="currentHost" select="name"/>
    <h1>
    <a href="#{name}"> <xsl:value-of select="name"/> </a>
    </h1>
    <xsl:for-each select="service">
        <h2> 
        <a href="#{$currentHost}-{name}"> <xsl:value-of select="name"/> </a>
        </h2>
    </xsl:for-each>
</xsl:for-each>
</div>

<!-- Loop through each section -->
<div class="hostSection">
<xsl:for-each select="bischeck/host">
    <xsl:variable name="currentHost" select="name"/>
    <p class="host">
    <a name="{name}"> <xsl:value-of select="name"/> </a>
    </p>
    <p class="desc"><xsl:value-of select="desc"/> </p>
    
    
    <div class="serviceSection">
    <xsl:for-each select="service">
        <p class="service"> 
        <a name="{$currentHost}-{name}"> <xsl:value-of select="name"/> </a>
        </p>
        <p class="desc"><xsl:value-of select="desc"/> </p>
        <p class="keyof">Schedules:</p>
        <ul>
        <span class="valueof"><xsl:apply-templates select="schedule"/></span>
        </ul>
        <p><span class="keyof">Url:&#160;</span> <span class="valueof"><xsl:value-of select="url" /></span></p>   
        <p><span class="keyof">Driver:&#160;</span> <span class="valueof"><xsl:value-of select="driver" /></span></p> 

        
        <div class="serviceitemSection">    
        <xsl:for-each select="serviceitem">
        <p class="serviceitem">
        <xsl:value-of select="name" /></p>
            <p class="desc"><xsl:value-of select="desc" /></p>
            <p><span class="keyof">Exec:&#160;</span> <span class="valueof"><xsl:value-of select="execstatement" /></span></p>
            <p><span class="keyof">Threshold class:&#160;</span> <span class="valueof"><xsl:value-of select="thresholdclass" /></span></p>
            <p><span class="keyof">Service Item class:&#160;</span> <span class="valueof"><xsl:value-of select="serviceitemclass" /></span></p>
            
        </xsl:for-each>
        </div>
    </xsl:for-each>
    </div>
</xsl:for-each>
</div>

</body>
</html>
</xsl:template>
    
<xsl:template match="schedule">
<li><xsl:apply-templates/></li>
</xsl:template>

</xsl:stylesheet>