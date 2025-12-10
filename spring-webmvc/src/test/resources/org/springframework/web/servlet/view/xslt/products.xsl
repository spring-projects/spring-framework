<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:import href="productsImport.xsl"/>

	<xsl:output method="xml" indent="yes" media-type="text/html"/>
	<xsl:param name="title"/>

	<xsl:template match="/products/product">
		<tr>
			<td>
				<xsl:value-of select="@id"/>
			</td>
			<td>
				<xsl:value-of select="@name"/>
			</td>
			<td>
				<xsl:value-of select="@price"/>
			</td>
		</tr>
	</xsl:template>

</xsl:stylesheet>
