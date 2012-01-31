<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:output method="xml" indent="yes" media-type="text/html"/>
	<xsl:param name="title"/>

	<xsl:template match="/">
		<html>
			<head>
				<title><xsl:value-of select="$title"/></title>
			</head>
			<body>
				<table>
					<xsl:apply-templates select="/products/product"/>
				</table>
			</body>
		</html>
	</xsl:template>

</xsl:stylesheet>
