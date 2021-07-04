<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:output method="html" omit-xml-declaration="yes"/>

	<xsl:template match="/">
		<html>
			<head><title>Hello!</title></head>
			<body>
				<h1>My First Words</h1>
				<xsl:apply-templates/>
			</body>
		</html>
	</xsl:template>

	<xsl:template match="word">
		<xsl:value-of select="."/><br/>
	</xsl:template>

</xsl:stylesheet>
