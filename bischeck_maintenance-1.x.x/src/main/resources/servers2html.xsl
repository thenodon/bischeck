<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:template match="/">
<html>
<link rel="stylesheet" type="text/css" href="bischeck.css" />   

<head><title>bischeck servers configuration</title></head>
<body>


<!-- Create server toc -->
<div class="toc">
<xsl:for-each select="servers/server">
    <h1>
    <a href="#{@name}"> <xsl:value-of select="@name"/> </a>
    </h1>
</xsl:for-each>
</div>


<xsl:for-each select="servers/server">

    <table class="property">
    <CAPTION CLASS="property">
        <a name="{@name}"> <xsl:value-of select="@name"/> </a>
        <p class="classname">
            <xsl:value-of select="class"/> 
        </p>
    </CAPTION>

    <thead>
        <tr>
            <th>Key</th>
            <th>Value</th>
        </tr>
    </thead>

    <xsl:for-each select="property">
    
        <tr>
            <td><xsl:value-of select="key"/> </td>
            <td><xsl:value-of select="value"/> </td>
        </tr>
    

    </xsl:for-each>
    </table>
</xsl:for-each>

</body>
</html>
</xsl:template>
    

</xsl:stylesheet>