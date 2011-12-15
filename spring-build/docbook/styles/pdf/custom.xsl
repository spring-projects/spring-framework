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
				xmlns:fo="http://www.w3.org/1999/XSL/Format"
				xmlns:xslthl="http://xslthl.sf.net"
				exclude-result-prefixes="xslthl"
				version='1.0'>
				
	<!-- Use nice graphics for admonitions -->
	<xsl:param name="admon.graphics">'1'</xsl:param>
	<xsl:param name="admon.graphics.path">@file.prefix@@dbf.xsl@/images/</xsl:param>
	
	<xsl:param name="draft.watermark.image" select="'@file.prefix@@dbf.xsl@/images/draft.png'"/>
	<xsl:param name="paper.type" select="'@paper.type@'"/>

	<xsl:param name="page.margin.top" select="'1cm'"/>
	<xsl:param name="region.before.extent" select="'1cm'"/>
	<xsl:param name="body.margin.top" select="'1.5cm'"/>

	<xsl:param name="body.margin.bottom" select="'1.5cm'"/>
	<xsl:param name="region.after.extent" select="'1cm'"/>
	<xsl:param name="page.margin.bottom" select="'1cm'"/>
	<xsl:param name="title.margin.left" select="'0cm'"/>
	
	<!-- Leave image paths as relative when navigating XInclude -->
	<xsl:param name="keep.relative.image.uris" select="1"/>

<!--###################################################
		Header and Footer control
	################################################### -->
	
	<!-- Number of levels of sections to include in markers (for running headings/footings). Default is 2. -->
	<xsl:param name="marker.section.level">1</xsl:param>
	
	<!-- Remove rules from top and bottom of the page -->
	<xsl:param name="header.rule" select="0"></xsl:param>
  	<xsl:param name="footer.rule" select="0"></xsl:param>

<!-- More space in the center header for long text 
	<xsl:attribute-set name="header.content.properties">
		<xsl:attribute name="font-family">
			<xsl:value-of select="$body.font.family"/>
		</xsl:attribute>
		<xsl:attribute name="margin-left">-5em</xsl:attribute>
		<xsl:attribute name="margin-right">-5em</xsl:attribute>
	</xsl:attribute-set>
-->
<!--###################################################
		Table of Contents
	################################################### -->

	<xsl:param name="generate.toc">
		book      toc,title
	</xsl:param>
	
	<!-- Show only Sections up to level 1 in the TOCs -->
	<xsl:param name="toc.section.depth">1</xsl:param>

<!--###################################################
		Custom Header and Footer
	################################################### -->
<!--
	pageclass
	
	There is a specific pageclass value for each type of page design that might be needed. For example, an index might 
	be two-column layout while the rest of the book is single column. Each pageclass has a set of FO simple-page-masters 
	defined for it. The following pageclass values are available by default, but this list could be extended by adding 
	custom page masters.

	titlepage  Division title page, including set, book, part.
	lot        Page with a list of titles, including book table of contents, list of figures, etc.
	front      Front matter pages, including preface, dedication
	body       Main content pages
	back       Back matter pages, including appendix, glossary, etc.
	index      Alphabetical book-style index
	
	sequence
	
	Within a pageclass, the sequence of pages can have different page designs. For example, the first page of sequence 
	might omit the running header so it will not detract from the main title. The enumerated sequence values are:

	first      First page of a page class.
	odd        Odd-numbered pages in the page class.
	even       Even-numbered pages.
	blank      Blank page at end of sequence, to even out page count.
	
	If the output format is single-sided, then odd and even pages should have the same design, and the blank page is 
	not called upon.
-->
	<xsl:template name="header.content">
		<xsl:param name="pageclass" select="''"/>
		<xsl:param name="sequence" select="''"/>
		<xsl:param name="position" select="''"/>
		<xsl:param name="gentext-key" select="''"/>

		<xsl:variable name="ProductName">
			<xsl:choose>
				<xsl:when test="//productname">
					<xsl:value-of select="//productname"/><xsl:text> </xsl:text>
				</xsl:when>
				<xsl:otherwise>
					<xsl:text>please define productname in your docbook file!</xsl:text>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:variable name="TitleAbbrev">
			<xsl:choose>
				<xsl:when test="//titleabbrev">
					<xsl:value-of select="//titleabbrev"/><xsl:text> </xsl:text>
				</xsl:when>
				<xsl:otherwise>
					<xsl:text>please define titleabbrev in your docbook file!</xsl:text>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
		<!-- $double.sided can be 0 or 1, meaning false or true respectively -->
		<!-- pageclass can be titlepage, lot, front, body, back, index -->    
		<!-- sequence can be first, odd, even, blank -->
	    <!-- position can be left, center, right -->
		<xsl:choose>
			<xsl:when test="$double.sided != 0">
				<xsl:choose>
					<xsl:when test="$pageclass='titlepage'"><!-- nop --></xsl:when>
					<xsl:when test="$pageclass='lot'"> 		<!-- nop --></xsl:when>
            		
					<xsl:when test="$pageclass='front' or $pageclass='body'">
						<xsl:choose>
							<xsl:when test="$sequence='first' or $sequence='odd'">
								<xsl:choose>
									<xsl:when test="$position='left'">
										<xsl:apply-templates select="." mode="titleabbrev.markup"/>  <!-- chapter header -->
									</xsl:when>
									<xsl:when test="$position='center'">
										<!--
										<xsl:value-of select="$TitleAbbrev"/>
									    -->
									</xsl:when>
									<xsl:when test="$position='right'">
										<fo:page-number/>
									</xsl:when>
								</xsl:choose>
							</xsl:when>
            		
							<xsl:when test="$sequence='even' or $sequence='blank'">
								<xsl:choose>
									<xsl:when test="$position='left'">
										<fo:page-number/>
									</xsl:when>
									<xsl:when test="$position='center'">
										<!--
										<fo:retrieve-marker retrieve-class-name="section.head.marker"  
							                                retrieve-position="first-including-carryover"
							                                retrieve-boundary="page-sequence"/>
									    -->
									</xsl:when>
									<xsl:when test="$position='right'">
										<xsl:value-of select="$TitleAbbrev"/>
									</xsl:when>
								</xsl:choose>
							</xsl:when>
						</xsl:choose>
					</xsl:when>

					<xsl:when test="$pageclass='back'">		<!-- nop --></xsl:when>
					<xsl:when test="$pageclass='index'">	<!-- nop --></xsl:when>
					<xsl:otherwise> 						<!-- nop --></xsl:otherwise>
				</xsl:choose>
			</xsl:when>	
			<xsl:when test="$double.sided = 0">
				<xsl:choose>
					<xsl:when test="$pageclass='titlepage'"><!-- nop --></xsl:when>
					<xsl:when test="$pageclass='lot'"> 		<!-- nop --></xsl:when>
            		
					<xsl:when test="$pageclass='front' or $pageclass='body'">
						<xsl:choose>
							<xsl:when test="$sequence='first' or $sequence='odd' or $sequence='even' or $sequence='blank'">
								<xsl:choose>
									<xsl:when test="$position='left'">
										<xsl:apply-templates select="." mode="titleabbrev.markup"/>  <!-- chapter header -->
									</xsl:when>
									<xsl:when test="$position='center'">
										<!--
										<xsl:value-of select="$TitleAbbrev"/>
										-->
									</xsl:when>
									<xsl:when test="$position='right'">
										<fo:page-number/>
									</xsl:when>
								</xsl:choose>
							</xsl:when>
						</xsl:choose>
					</xsl:when>

					<xsl:when test="$pageclass='back'">		<!-- nop --></xsl:when>
					<xsl:when test="$pageclass='index'">	<!-- nop --></xsl:when>
					<xsl:otherwise> 						<!-- nop --></xsl:otherwise>
				</xsl:choose>
			</xsl:when>	
		</xsl:choose>
					
		
	</xsl:template>

<!--###################################################
		Custom Footer
	################################################### -->

	<xsl:template name="footer.content">
		<xsl:param name="pageclass" select="''"/>
		<xsl:param name="sequence" select="''"/>
		<xsl:param name="position" select="''"/>
		<xsl:param name="gentext-key" select="''"/>

		<xsl:variable name="Version">
			<xsl:choose>
				<xsl:when test="//releaseinfo">
					<xsl:value-of select="//releaseinfo"/>
				</xsl:when>
				<xsl:otherwise>
					<!-- nop -->
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:variable name="TitleAbbrev">
			<xsl:choose>
				<xsl:when test="//titleabbrev">
					<xsl:value-of select="//titleabbrev"/><xsl:text> </xsl:text>
				</xsl:when>
				<xsl:otherwise>
					<xsl:text>please define titleabbrev in your docbook file!</xsl:text>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<!-- $double.sided can be 0 or 1, meaning false or true respectively -->
		<!-- pageclass can be titlepage, lot, front, body, back, index -->    
		<!-- sequence can be first, odd, even, blank -->
	    <!-- position can be left, center, right -->
		<xsl:choose>
			<xsl:when test="$double.sided != 0">
				<xsl:choose>
					<xsl:when test="$pageclass='titlepage'"> 		<!-- nop --></xsl:when>
            		
					<xsl:when test="$pageclass='lot' or $pageclass='front' or $pageclass='body'">
						<xsl:choose>
							<xsl:when test="$pageclass!='titlepage' and ($sequence='first' or $sequence='odd')">
								<xsl:choose>
									<xsl:when test="$position='left'">
										<xsl:value-of select="$Version"/>
									</xsl:when>
									<xsl:when test="$position='center'">
										<!--
										<xsl:value-of select="$TitleAbbrev"/>
										<fo:retrieve-marker retrieve-class-name="section.head.marker"  
							                                retrieve-position="first-including-carryover"
							                                retrieve-boundary="page-sequence"/>
										-->
									</xsl:when>
									<xsl:when test="$position='right'">
										<fo:page-number/>
									</xsl:when>
								</xsl:choose>
							</xsl:when>
            		
							<xsl:when test="$sequence='even' or $sequence='blank'">
								<xsl:choose>
									<xsl:when test="$position='left'">
										<fo:page-number/>
									</xsl:when>
									<xsl:when test="$position='center'">
										<!--
										<xsl:value-of select="$TitleAbbrev"/>
										-->
									</xsl:when>
									<xsl:when test="$position='right'">
										<xsl:apply-templates select="." mode="titleabbrev.markup"/>  <!-- chapter header -->
									</xsl:when>
								</xsl:choose>
							</xsl:when>
						</xsl:choose>
					</xsl:when>

					<xsl:when test="$pageclass='back'">		<!-- nop --></xsl:when>
					<xsl:when test="$pageclass='index'">	<!-- nop --></xsl:when>
					<xsl:otherwise> 						<!-- nop --></xsl:otherwise>
				</xsl:choose>
			</xsl:when>	
			<xsl:when test="$double.sided = 0">
				<xsl:choose>
					<xsl:when test="$pageclass='titlepage'"><!-- nop --></xsl:when>
					<xsl:when test="$pageclass='lot'"> 		<!-- nop --></xsl:when>
            		
					<xsl:when test="$pageclass='front' or $pageclass='body'">
						<xsl:choose>
							<xsl:when test="$sequence='first' or $sequence='odd' or $sequence='even' or $sequence='blank'">
								<xsl:choose>
									<xsl:when test="$position='left'">
										<xsl:value-of select="$Version"/>
									</xsl:when>
									<xsl:when test="$position='center'">
										<fo:retrieve-marker retrieve-class-name="section.head.marker"  
							                                retrieve-position="first-including-carryover"
							                                retrieve-boundary="page-sequence"/>
									</xsl:when>
									<xsl:when test="$position='right'">
										<fo:page-number/>
									</xsl:when>
								</xsl:choose>
							</xsl:when>
						</xsl:choose>
					</xsl:when>

					<xsl:when test="$pageclass='back'">		<!-- nop --></xsl:when>
					<xsl:when test="$pageclass='index'">	<!-- nop --></xsl:when>
					<xsl:otherwise> 						<!-- nop --></xsl:otherwise>
				</xsl:choose>
			</xsl:when>	
		</xsl:choose>
		
	</xsl:template>

	<xsl:template match="processing-instruction('hard-pagebreak')">
		<fo:block break-before='page'/>
	</xsl:template>
	
<!--###################################################
		Extensions
	################################################### -->

<!-- These extensions are required for table printing and other stuff -->
	<xsl:param name="use.extensions">1</xsl:param>
	<xsl:param name="tablecolumns.extension">0</xsl:param>
	<xsl:param name="callout.extensions">1</xsl:param>
	<xsl:param name="fop.extensions">1</xsl:param>

<!--###################################################
		Paper & Page Size
	################################################### -->

<!-- Paper type, no headers on blank pages, no double sided printing -->
	<xsl:param name="double.sided">1</xsl:param>
	<xsl:param name="headers.on.blank.pages">1</xsl:param>
	<xsl:param name="footers.on.blank.pages">1</xsl:param>

<!--###################################################
		Fonts & Styles
	################################################### -->
	<xsl:param name="alignment">left</xsl:param>
	
	<xsl:param name="hyphenate">false</xsl:param>

	<xsl:attribute-set name="footer.content.properties">
	    <xsl:attribute name="font-family">
	        <xsl:value-of select="$body.fontset"/>
	    </xsl:attribute>
	</xsl:attribute-set>

<!-- Default Font size -->
	<xsl:param name="body.font.master">12</xsl:param>
	<xsl:param name="body.font.small">8</xsl:param>

<!-- Line height in body text -->
	<xsl:param name="line-height">1.2</xsl:param>

<!-- Chapter title size -->
	<xsl:attribute-set name="chapter.titlepage.recto.style">
		<xsl:attribute name="text-align">left</xsl:attribute>
		<xsl:attribute name="font-weight">bold</xsl:attribute>
		<xsl:attribute name="font-size">
			<xsl:value-of select="$body.font.master * 1.8"/>
			<xsl:text>pt</xsl:text>
		</xsl:attribute>
	</xsl:attribute-set>

<!-- Why is the font-size for chapters hardcoded in the XSL FO templates?
	 Let's remove it, so this sucker can use our attribute-set only... -->
	<xsl:template match="title" mode="chapter.titlepage.recto.auto.mode">
		<fo:block xmlns:fo="http://www.w3.org/1999/XSL/Format"
				xsl:use-attribute-sets="chapter.titlepage.recto.style">
			<xsl:call-template name="component.title">
			<xsl:with-param name="node" select="ancestor-or-self::chapter[1]"/>
			</xsl:call-template>
		</fo:block>
	</xsl:template>

<!-- Sections 1, 2 and 3 titles have a small bump factor and padding -->
	<xsl:attribute-set name="section.title.level1.properties">
		<xsl:attribute name="space-before.optimum">0.8em</xsl:attribute>
		<xsl:attribute name="space-before.minimum">0.8em</xsl:attribute>
		<xsl:attribute name="space-before.maximum">0.8em</xsl:attribute>
		<xsl:attribute name="font-size">
			<xsl:value-of select="$body.font.master * 1.5"/>
			<xsl:text>pt</xsl:text>
		</xsl:attribute>
		<xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>
	</xsl:attribute-set>
	<xsl:attribute-set name="section.title.level2.properties">
		<xsl:attribute name="space-before.optimum">0.6em</xsl:attribute>
		<xsl:attribute name="space-before.minimum">0.6em</xsl:attribute>
		<xsl:attribute name="space-before.maximum">0.6em</xsl:attribute>
		<xsl:attribute name="font-size">
			<xsl:value-of select="$body.font.master * 1.25"/>
			<xsl:text>pt</xsl:text>
		</xsl:attribute>
		<xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>
	</xsl:attribute-set>
	<xsl:attribute-set name="section.title.level3.properties">
		<xsl:attribute name="space-before.optimum">0.4em</xsl:attribute>
		<xsl:attribute name="space-before.minimum">0.4em</xsl:attribute>
		<xsl:attribute name="space-before.maximum">0.4em</xsl:attribute>
		<xsl:attribute name="font-size">
			<xsl:value-of select="$body.font.master * 1.0"/>
			<xsl:text>pt</xsl:text>
		</xsl:attribute>
		<xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>
	</xsl:attribute-set>

<!-- Use code syntax highlighting -->
	<xsl:param name="highlight.source" select="1"/>
	<xsl:param name="highlight.default.language" select="xml" />

	<xsl:template match='xslthl:keyword'>
		<fo:inline font-weight="bold" color="#7F0055"><xsl:apply-templates/></fo:inline>
	</xsl:template>
	
	<xsl:template match='xslthl:comment'>
		<fo:inline font-style="italic" color="#3F5F5F"><xsl:apply-templates/></fo:inline>
	</xsl:template>

	<xsl:template match='xslthl:oneline-comment'>
		<fo:inline font-style="italic" color="#3F5F5F"><xsl:apply-templates/></fo:inline>
	</xsl:template>

	<xsl:template match='xslthl:multiline-comment'>
		<fo:inline font-style="italic" color="#3F5FBF"><xsl:apply-templates/></fo:inline>
	</xsl:template>

	<xsl:template match='xslthl:tag'>
		<fo:inline  color="#3F7F7F"><xsl:apply-templates/></fo:inline>
	</xsl:template>

	<xsl:template match='xslthl:attribute'>
		<fo:inline color="#7F007F"><xsl:apply-templates/></fo:inline>
	</xsl:template>
	
	<xsl:template match='xslthl:value'>
		<fo:inline color="#2A00FF"><xsl:apply-templates/></fo:inline>
	</xsl:template>

	<xsl:template match='xslthl:string'>
		<fo:inline color="#2A00FF"><xsl:apply-templates/></fo:inline>
	</xsl:template>

<!--###################################################
		Tables
	################################################### -->

	<!-- Some padding inside tables -->
	<xsl:attribute-set name="table.cell.padding">
		<xsl:attribute name="padding-left">4pt</xsl:attribute>
		<xsl:attribute name="padding-right">4pt</xsl:attribute>
		<xsl:attribute name="padding-top">4pt</xsl:attribute>
		<xsl:attribute name="padding-bottom">4pt</xsl:attribute>
	</xsl:attribute-set>

<!-- Only hairlines as frame and cell borders in tables -->
	<xsl:param name="table.frame.border.thickness">0.1pt</xsl:param>
	<xsl:param name="table.cell.border.thickness">0.1pt</xsl:param>

<!--###################################################
		Labels
	################################################### -->

<!-- Label Chapters and Sections (numbering) -->
	<xsl:param name="chapter.autolabel" select="1"/>
	<xsl:param name="section.autolabel" select="1"/>
	<xsl:param name="section.autolabel.max.depth" select="1"/>

	<xsl:param name="section.label.includes.component.label" select="1"/>
	<xsl:param name="table.footnote.number.format" select="'1'"/>

<!--###################################################
		Programlistings
	################################################### -->

<!-- Verbatim text formatting (programlistings) -->
	<xsl:attribute-set name="monospace.verbatim.properties">
		<xsl:attribute name="font-size">
			<xsl:value-of select="$body.font.small * 0.90"/>
			<xsl:text>pt</xsl:text>
		</xsl:attribute>
	</xsl:attribute-set>

	<xsl:attribute-set name="verbatim.properties">
		<xsl:attribute name="space-before.minimum">1em</xsl:attribute>
		<xsl:attribute name="space-before.optimum">1em</xsl:attribute>
		<xsl:attribute name="space-before.maximum">1em</xsl:attribute>
		<xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>

		<xsl:attribute name="border-color">#444444</xsl:attribute>
		<xsl:attribute name="border-style">solid</xsl:attribute>
		<xsl:attribute name="border-width">0.1pt</xsl:attribute>
		<xsl:attribute name="padding-top">0.5em</xsl:attribute>
		<xsl:attribute name="padding-left">0.5em</xsl:attribute>
		<xsl:attribute name="padding-right">0.5em</xsl:attribute>
		<xsl:attribute name="padding-bottom">0.5em</xsl:attribute>
		<xsl:attribute name="margin-left">0em</xsl:attribute>
		<xsl:attribute name="margin-right">0em</xsl:attribute>
	</xsl:attribute-set>

    <!-- Shade (background) programlistings -->
	<xsl:param name="shade.verbatim">1</xsl:param>
	<xsl:attribute-set name="shade.verbatim.style">
		<xsl:attribute name="background-color">#F0F0F0</xsl:attribute>
	</xsl:attribute-set>

	<xsl:attribute-set name="list.block.spacing">
		<xsl:attribute name="space-before.optimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-before.minimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-before.maximum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>
	</xsl:attribute-set>
	
    <xsl:attribute-set name="abstract.properties">
	  <xsl:attribute name="font-weight">normal</xsl:attribute>
	</xsl:attribute-set>

	<xsl:attribute-set name="example.properties">
		<xsl:attribute name="space-before.minimum">0.5em</xsl:attribute>
		<xsl:attribute name="space-before.optimum">0.5em</xsl:attribute>
		<xsl:attribute name="space-before.maximum">0.5em</xsl:attribute>
		<xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>
		<xsl:attribute name="keep-together.within-column">always</xsl:attribute>
	</xsl:attribute-set>

<!--###################################################
		Title information for Figures, Examples etc.
	################################################### -->

	<xsl:attribute-set name="formal.title.properties" use-attribute-sets="normal.para.spacing">
		<xsl:attribute name="font-weight">normal</xsl:attribute>
		<xsl:attribute name="font-style">italic</xsl:attribute>
		<xsl:attribute name="font-size">
			<xsl:value-of select="$body.font.master"/>
			<xsl:text>pt</xsl:text>
		</xsl:attribute>
		<xsl:attribute name="hyphenate">false</xsl:attribute>
		<xsl:attribute name="space-before.minimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-before.optimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-before.maximum">0.1em</xsl:attribute>
	</xsl:attribute-set>

<!--###################################################
		Callouts
	################################################### -->

<!-- don't use images for callouts -->
	<xsl:param name="callout.graphics">0</xsl:param>
	<xsl:param name="callout.unicode">1</xsl:param>

<!-- Place callout marks at this column in annotated areas -->
	<xsl:param name="callout.defaultcolumn">90</xsl:param>

<!--###################################################
		Misc
	################################################### -->

<!-- Placement of titles -->
	<xsl:param name="formal.title.placement">
		figure after
		example after
		equation before
		table before
		procedure before
	</xsl:param>

<!-- Format Variable Lists as Blocks (prevents horizontal overflow) -->
	<xsl:param name="variablelist.as.blocks">1</xsl:param>

	<xsl:param name="body.start.indent">0pt</xsl:param>

<!-- Remove "Chapter" from the Chapter titles... -->
	<xsl:param name="local.l10n.xml" select="document('')"/>
	<l:i18n xmlns:l="http://docbook.sourceforge.net/xmlns/l10n/1.0">
		<l:l10n language="en">
			<l:context name="title-numbered">
				<l:template name="chapter" text="%n.&#160;%t"/>
				<l:template name="section" text="%n&#160;%t"/>
			</l:context>
			<l:context name="title">
				<l:template name="example" text="Example&#160;%n&#160;%t"/>
			</l:context>
		</l:l10n>
	</l:i18n>

<!--###################################################
		colored and hyphenated links 
	################################################### --> 

	<xsl:template match="ulink">
		<fo:basic-link external-destination="{@url}"
				xsl:use-attribute-sets="xref.properties"
				text-decoration="underline"
				color="blue">
			<xsl:choose>
				<xsl:when test="count(child::node())=0">
					<xsl:value-of select="@url"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:apply-templates/>
				</xsl:otherwise>
			</xsl:choose>
		</fo:basic-link>
	</xsl:template>

	<xsl:template match="link">
		<fo:basic-link internal-destination="{@linkend}"
				xsl:use-attribute-sets="xref.properties"
				text-decoration="underline"
				color="blue">
			<xsl:choose>
				<xsl:when test="count(child::node())=0">
					<xsl:value-of select="@linkend"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:apply-templates/>
				</xsl:otherwise>
			</xsl:choose>
		</fo:basic-link>
	</xsl:template>

</xsl:stylesheet>