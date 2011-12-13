<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:variable name="cur" select="0" />

<xsl:template match="/">
<html>
<link rel="stylesheet" type="text/css" href="bischeck.css" />   

<head><title>twenty4threshold configuration</title></head>
<body>
<!-- Create toc -->
<div class="toc">
<xsl:for-each select="twenty4threshold/servicedef">
         
    <h1> 
    <a href="#{hostname}-{servicename}-{serviceitemname}"> <xsl:value-of select="hostname"/>-<xsl:value-of select="servicename"/>-<xsl:value-of select="serviceitemname"/>  </a>
    </h1>
        
</xsl:for-each>
</div>

<!-- Loop through each section -->
<div class="servicedefHeadSection">
<xsl:for-each select="twenty4threshold/servicedef">
         
    <h1> 
    <a name="{hostname}-{servicename}-{serviceitemname}"> <xsl:value-of select="hostname"/>-<xsl:value-of select="servicename"/>-<xsl:value-of select="serviceitemname"/>  </a>
    </h1>

    <div class="servicedefSection">
 
    <xsl:for-each select="period">
        <p><span class="keyof">Calculation:&#160;</span> <span class="valueof"><xsl:value-of select="calcmethod" /></span></p>
        <p><span class="keyof">Warning:&#160;</span> <span class="valueof"><xsl:value-of select="warning" /></span></p>
        <p><span class="keyof">Critical:&#160;</span> <span class="valueof"><xsl:value-of select="critical" /></span></p>
        <p><span class="keyof">Periods:&#160;</span></p>
        
        <!-- Check for default -->
        <div class="period">       
        <xsl:choose>
            <xsl:when test="not(weeks or months)">
            <p><span class="keyof">Default&#160;</span></p>
            </xsl:when>
            <xsl:otherwise>
                <!-- Check for months -->
                <xsl:for-each select="months">
                    <p>
                    <xsl:choose>
                        <xsl:when test="not(month)">
                            <span class="periodkeyof">Month:&#160;</span> <span class="periodvalueof">*</span>
                        </xsl:when>
                        <xsl:otherwise>
                            <span class="periodkeyof">Month:&#160;</span> <span class="periodvalueof"><xsl:value-of select="month" /></span>
                        </xsl:otherwise>
                    </xsl:choose>    
                    <xsl:choose>
                        <xsl:when test="not(dayofmonth)">
                            <span class="periodkeyof"> - Day of month:&#160;</span> <span class="periodvalueof">*</span>
                        </xsl:when>
                        <xsl:otherwise>    
                            <span class="periodkeyof"> - Day of month:&#160;</span> <span class="periodvalueof"><xsl:value-of select="dayofmonth" /></span>
                        </xsl:otherwise>
                    </xsl:choose>
                    </p> 
                </xsl:for-each>
        
        
                <!-- Check for weeks -->
                <xsl:for-each select="weeks">
                    <p>
                    <xsl:choose>
                        <xsl:when test="not(week)">
                            <span class="periodkeyof">Week:&#160;</span> <span class="periodvalueof">*</span>
                        </xsl:when>
                        <xsl:otherwise>
                            <span class="periodkeyof">Week:&#160;</span> <span class="periodvalueof"><xsl:value-of select="week" /></span>
                        </xsl:otherwise>
                    </xsl:choose>    
                    <xsl:choose>
                        <xsl:when test="not(dayofweek)">
                            <span class="periodkeyof"> - Day of week:&#160;</span> <span class="periodvalueof">*</span>
                        </xsl:when>
                        <xsl:otherwise>    
                            <span class="periodkeyof"> - Day of week:&#160;</span> <span class="periodvalueof"><xsl:value-of select="dayofweek" /></span>
                        </xsl:otherwise>
                    </xsl:choose>
                    </p> 
                </xsl:for-each>
            </xsl:otherwise>
        </xsl:choose>
        </div>
        

        <xsl:variable name="myid" select="hoursIDREF" />
        <xsl:variable name="cur" select="0" />
        <table class="hours">
            
            <CAPTION class="hours">
            hoursIDREF:&#160; <xsl:value-of select="hoursIDREF" />
            </CAPTION>
            
            <thead>
                <tr>
                    <th>Hour</th>
                    <th>Value</th>
                </tr>
            </thead>
            
            <xsl:for-each select="//twenty4threshold/hours[@hoursID=$myid]" >
                <xsl:apply-templates select="hour"/> 
            </xsl:for-each>
                
        </table>
        
    </xsl:for-each>
    </div>
</xsl:for-each>
</div>



</body>
</html>
</xsl:template>

<xsl:template match="hour">
<xsl:variable name="cur" select="$cur + position() - 1" />
<tr>
    <xsl:choose>
        <xsl:when test="$cur &lt; '10'">
            <td>
            <xsl:text>0</xsl:text>
            <xsl:text><xsl:value-of select="$cur" /></xsl:text>
            </td>
        </xsl:when>
        <xsl:otherwise>
            <td>
            <xsl:text><xsl:value-of select="$cur" /></xsl:text>
            </td>
        </xsl:otherwise>
    </xsl:choose>    
    
    <td><xsl:value-of select="." /> </td>
</tr>
</xsl:template>

</xsl:stylesheet>