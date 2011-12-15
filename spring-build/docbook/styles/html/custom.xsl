<?xml version="1.0" encoding="UTF-8"?>
<!--
   Copyright 2010 SpringSource
   
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
   
       http://www.apache.org/licenses/LICENSE-2.0
   
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:xslthl="http://xslthl.sf.net"
				exclude-result-prefixes="xslthl"
				version='1.0'>

<!-- Extensions -->
	<xsl:param name="use.extensions">1</xsl:param>
	<xsl:param name="tablecolumns.extension">0</xsl:param>
	<xsl:param name="callout.extensions">1</xsl:param>

<!-- Activate Graphics -->
	<xsl:param name="admon.graphics" select="1"/>
	<xsl:param name="admon.graphics.path">images/</xsl:param>
	<xsl:param name="admon.graphics.extension">.gif</xsl:param>
	<xsl:param name="callout.graphics" select="1" />
	<xsl:param name="callout.defaultcolumn">120</xsl:param>
	<xsl:param name="callout.graphics.path">images/callouts/</xsl:param>
	<xsl:param name="callout.graphics.extension">.gif</xsl:param>

	<xsl:param name="table.borders.with.css" select="1"/>
	<xsl:param name="html.stylesheet">css/stylesheet.css</xsl:param>
	<xsl:param name="html.stylesheet.type">text/css</xsl:param>
	<xsl:param name="generate.toc">book toc,title</xsl:param>

	<xsl:param name="admonition.title.properties">text-align: left</xsl:param>

	<!-- Leave image paths as relative when navigating XInclude -->
	<xsl:param name="keep.relative.image.uris" select="1"/>

<!-- Label Chapters and Sections (numbering) -->
	<xsl:param name="chapter.autolabel" select="1"/>
	<xsl:param name="section.autolabel" select="1"/>
	<xsl:param name="section.autolabel.max.depth" select="1"/>

	<xsl:param name="section.label.includes.component.label" select="1"/>
	<xsl:param name="table.footnote.number.format" select="'1'"/>

<!-- Show only Sections up to level 1 in the TOCs -->
	<xsl:param name="toc.section.depth">1</xsl:param>

<!-- Remove "Chapter" from the Chapter titles... -->
	<xsl:param name="local.l10n.xml" select="document('')"/>
	<l:i18n xmlns:l="http://docbook.sourceforge.net/xmlns/l10n/1.0">
		<l:l10n language="en">
			<l:context name="title-numbered">
				<l:template name="chapter" text="%n.&#160;%t"/>
				<l:template name="section" text="%n&#160;%t"/>
			</l:context>
		</l:l10n>
	</l:i18n>

<!-- Use code syntax highlighting -->
	<xsl:param name="highlight.source" select="1"/>

	<xsl:template match='xslthl:keyword'>
		<span class="hl-keyword"><xsl:value-of select='.'/></span>
	</xsl:template>

	<xsl:template match='xslthl:comment'>
		<span class="hl-comment"><xsl:value-of select='.'/></span>
	</xsl:template>

	<xsl:template match='xslthl:oneline-comment'>
		<span class="hl-comment"><xsl:value-of select='.'/></span>
	</xsl:template>

	<xsl:template match='xslthl:multiline-comment'>
		<span class="hl-multiline-comment"><xsl:value-of select='.'/></span>
	</xsl:template>

	<xsl:template match='xslthl:tag'>
		<span class="hl-tag"><xsl:value-of select='.'/></span>
	</xsl:template>

	<xsl:template match='xslthl:attribute'>
		<span class="hl-attribute"><xsl:value-of select='.'/></span>
	</xsl:template>

	<xsl:template match='xslthl:value'>
		<span class="hl-value"><xsl:value-of select='.'/></span>
	</xsl:template>
	
	<xsl:template match='xslthl:string'>
		<span class="hl-string"><xsl:value-of select='.'/></span>
	</xsl:template>

	<!-- Google Analytics -->
	<xsl:template name="user.head.content">
		<xsl:comment>Begin Google Analytics code</xsl:comment>
		<script type="text/javascript">
			var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");
			document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));
		</script>
		<script type="text/javascript">
			var pageTracker = _gat._getTracker("UA-2728886-3");
			pageTracker._setDomainName("none");
			pageTracker._setAllowLinker(true);
			pageTracker._trackPageview();
		</script>
		<xsl:comment>End Google Analytics code</xsl:comment>
	</xsl:template>

</xsl:stylesheet>