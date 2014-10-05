<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:template name="Newline">
<xsl:text>
</xsl:text>
</xsl:template>

<xsl:template name="Tab">
<xsl:text>    </xsl:text>
</xsl:template>

<xsl:template name="Tab2">
<xsl:text>        </xsl:text>
</xsl:template>

<xsl:template match="/">

    <xsl:call-template name="Newline" />
    <xsl:text>bischeck configuration</xsl:text>
    <xsl:call-template name="Newline" />
    <xsl:text>======================</xsl:text>
    <xsl:call-template name="Newline" />
    <!-- Loop through each section -->
    
    <xsl:for-each select="bischeck/host">    
        <xsl:text>host: </xsl:text> <xsl:value-of select="name"/> <xsl:call-template name="Newline" /> 
        <xsl:text>desc: </xsl:text> <xsl:value-of select="desc"/> <xsl:call-template name="Newline" />
        <xsl:call-template name="Newline" />
    
        <xsl:for-each select="service">
    
            <xsl:call-template name="Tab" />
            <xsl:text>service: </xsl:text>
            <xsl:value-of select="name"/> 
            <xsl:call-template name="Newline" />
    
            <xsl:call-template name="Tab" />
            <xsl:text>desc: </xsl:text>
            <xsl:value-of select="desc"/>
            <xsl:call-template name="Newline" />
    
            <xsl:call-template name="Tab" />
            <xsl:text>Schedules:</xsl:text>
            <xsl:call-template name="Newline" />
            <xsl:apply-templates select="schedule"/>
            
            <xsl:call-template name="Tab" />
            <xsl:text>Url: </xsl:text>
            <xsl:value-of select="url" />
            <xsl:call-template name="Newline" />
            
            <xsl:call-template name="Tab" />
            <xsl:text>Driver: </xsl:text>
            <xsl:value-of select="driver" />
            <xsl:call-template name="Newline" />
            <xsl:call-template name="Newline" />
            <xsl:for-each select="serviceitem">
            
            <xsl:call-template name="Tab2" />
            <xsl:text>serviceitem: </xsl:text>
            <xsl:value-of select="name" />
            <xsl:call-template name="Newline" />
           
            <xsl:call-template name="Tab2" />
            <xsl:text>desc: </xsl:text>
            <xsl:value-of select="desc" />
            <xsl:call-template name="Newline" />
          
            <xsl:call-template name="Tab2" />
            <xsl:text>Exec: </xsl:text>
            <xsl:value-of select="execstatement" />
            <xsl:call-template name="Newline" />
          
            <xsl:call-template name="Tab2" />
            <xsl:text>Threshold class: </xsl:text> 
            <xsl:value-of select="thresholdclass" />
            <xsl:call-template name="Newline" />
            
            <xsl:call-template name="Tab2" />
            <xsl:text>Service Item class: </xsl:text>
            <xsl:value-of select="serviceitemclass" />
            <xsl:call-template name="Newline" />
            <xsl:call-template name="Newline" />
        </xsl:for-each>
        <xsl:call-template name="Newline" />
    </xsl:for-each>
    
</xsl:for-each>


</xsl:template>
    
<xsl:template match="schedule">
<xsl:text>    - </xsl:text><xsl:value-of select="." /><xsl:text>&#xa;</xsl:text>
</xsl:template>

</xsl:stylesheet>