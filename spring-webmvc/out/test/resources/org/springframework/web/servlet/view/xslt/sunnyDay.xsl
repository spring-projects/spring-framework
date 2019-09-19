<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:output method="xml" omit-xml-declaration="yes"/>

	<xsl:param name="sex">Female</xsl:param>

	<xsl:template match="*">
		<xsl:element name="hero">
			<xsl:attribute name="name">
				<xsl:value-of select="@name"/>
			</xsl:attribute>
			<xsl:attribute name="age">
				<xsl:value-of select="@age"/>
			</xsl:attribute>
			<xsl:attribute name="catchphrase">
				<xsl:value-of select="@catchphrase"/>
			</xsl:attribute>
			<xsl:attribute name="sex">
				<xsl:value-of select="$sex"/>
			</xsl:attribute>
		</xsl:element>
	</xsl:template>

</xsl:stylesheet>
