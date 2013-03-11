<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:variable name="cur" select="0" />


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
    <xsl:text>twenty4threshold configuration</xsl:text>
    <xsl:call-template name="Newline" />
    <xsl:text>==============================</xsl:text>
    <xsl:call-template name="Newline" />
    
         
    <!-- Loop through each section -->
    <xsl:for-each select="twenty4threshold/servicedef">
        <xsl:call-template name="Newline" />
        
        <xsl:text>---- </xsl:text>
         
        <xsl:text><xsl:value-of select="hostname"/></xsl:text>
        <xsl:text>-</xsl:text>
        <xsl:text><xsl:value-of select="servicename"/></xsl:text>
        <xsl:text>-</xsl:text>
        <xsl:text><xsl:value-of select="serviceitemname"/></xsl:text>
        <xsl:text> ----</xsl:text>
        
        <xsl:call-template name="Newline" />
    
        <xsl:for-each select="period">
            <xsl:text>Calculation: <xsl:value-of select="calcmethod" /></xsl:text>
            <xsl:call-template name="Newline" />
            <xsl:text>Warning:<xsl:value-of select="warning" /></xsl:text>
            <xsl:call-template name="Newline" />
            <xsl:text>Critical:<xsl:value-of select="critical" /></xsl:text>
            <xsl:call-template name="Newline" />
            <xsl:text>Periods:</xsl:text>
            <xsl:call-template name="Newline" />
        
            <!-- Check for default -->
            
            <xsl:choose>
                <xsl:when test="not(weeks or months)">
                    <xsl:text>    </xsl:text>
                    <xsl:text>Default</xsl:text>
                    <xsl:call-template name="Newline" />
                </xsl:when>
                <xsl:otherwise>
                <!-- Check for months -->
                    <xsl:for-each select="months">
                        <xsl:text>    </xsl:text>
                        <xsl:choose>
                            <xsl:when test="not(month)">
                                <xsl:text>Month: *</xsl:text>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:text>Month: <xsl:value-of select="month" /></xsl:text>
                            </xsl:otherwise>
                        </xsl:choose>    
                        <xsl:choose>
                            <xsl:when test="not(dayofmonth)">
                                <xsl:text> - Day of month: *</xsl:text>
                            </xsl:when>
                            <xsl:otherwise>    
                                <xsl:text> - Day of month: <xsl:value-of select="dayofmonth" /></xsl:text>
                            </xsl:otherwise>
                        </xsl:choose> 
                        <xsl:call-template name="Newline" />
                    </xsl:for-each>
        
        
                    <!-- Check for weeks -->
                    <xsl:for-each select="weeks">
                        <xsl:text>    </xsl:text>
                        <xsl:choose>
                            <xsl:when test="not(week)">
                                <xsl:text>Week: *</xsl:text>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:text>Week: <xsl:value-of select="week" /></xsl:text>
                            </xsl:otherwise>
                        </xsl:choose>    
                        <xsl:choose>
                            <xsl:when test="not(dayofweek)">
                                <xsl:text> - Day of week: *</xsl:text>
                            </xsl:when>
                            <xsl:otherwise>    
                                <xsl:text> - Day of week: <xsl:value-of select="dayofweek" /></xsl:text>
                            </xsl:otherwise>
                        </xsl:choose> 
                        <xsl:call-template name="Newline" />
                    </xsl:for-each>
                </xsl:otherwise>
            </xsl:choose>
        

            <xsl:variable name="myid" select="hoursIDREF" />
            <xsl:variable name="cur" select="0" />
            
            <xsl:text>hoursIDREF: <xsl:value-of select="hoursIDREF" /></xsl:text>
            <xsl:call-template name="Newline" />
            
            <xsl:for-each select="//twenty4threshold/hours[@hoursID=$myid]" >
                <xsl:apply-templates select="hour"/> 
            </xsl:for-each>
            
            <xsl:for-each select="//twenty4threshold/hours[@hoursID=$myid]" >
                <xsl:apply-templates select="hourinterval"/> 
            </xsl:for-each>
            
            <xsl:call-template name="Newline" />
        </xsl:for-each>
    </xsl:for-each>
</xsl:template>

<xsl:template match="hour">
    <xsl:variable name="cur" select="$cur + position() - 1" />

     <xsl:choose>
        <xsl:when test="$cur &lt; '10'">
           <xsl:text>0</xsl:text>
           <xsl:text><xsl:value-of select="$cur" /></xsl:text>
        </xsl:when>
        <xsl:otherwise>
            <xsl:text><xsl:value-of select="$cur" /></xsl:text>
        </xsl:otherwise>
    </xsl:choose>    
    
    <xsl:text> : </xsl:text>
    <xsl:text><xsl:value-of select="." /></xsl:text>
    <xsl:call-template name="Newline" />
</xsl:template>

<xsl:template match="hourinterval">

    <xsl:value-of select="from" /> - <xsl:value-of select="to" />
    <xsl:text> : </xsl:text>
    <xsl:value-of select="threshold" />
    <xsl:call-template name="Newline" />

</xsl:template>

</xsl:stylesheet>