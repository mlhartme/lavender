<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:lavender="http://www.schlund.de/pustefix/lavender" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:template match="lavender:attribute">
    <ixsl:attribute name="data-lavender-{@name}">
      <ixsl:variable name="tmp"><xsl:apply-templates/></ixsl:variable>
      <ixsl:value-of select="$tmp"/>
    </ixsl:attribute>
  </xsl:template>

</xsl:stylesheet>
