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
    <xsl:text>bischeck servers configuration</xsl:text>
    <xsl:call-template name="Newline" />
    <xsl:text>==============================</xsl:text>
    <xsl:call-template name="Newline" />
    
    

    <!-- Create server toc -->

<xsl:strip-space elements="class" />
    <xsl:for-each select="servers/server">
        <xsl:call-template name="Newline" />
        <xsl:text><xsl:value-of select="@name"/></xsl:text>
        <xsl:call-template name="Newline" />
        <xsl:apply-templates select="class"/>
        <xsl:call-template name="Newline" /> 
    
        <xsl:for-each select="property">
            <xsl:text><xsl:value-of select="key"/></xsl:text> 
            <xsl:text>: </xsl:text> 
            <xsl:text><xsl:value-of select="value"/></xsl:text> 
            <xsl:call-template name="Newline" /> 
        </xsl:for-each>
    </xsl:for-each>

</xsl:template>

<xsl:template match="class | text()">
   
    <xsl:value-of select=
     "concat(normalize-space(.), '&#xA;')"/>
     
 
  <xsl:apply-templates />
</xsl:template>    
</xsl:stylesheet>