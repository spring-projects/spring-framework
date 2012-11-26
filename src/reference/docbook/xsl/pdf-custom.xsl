<?xml version="1.0" encoding="UTF-8"?>

<!--
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                xmlns:xslthl="http://xslthl.sf.net"
                exclude-result-prefixes="xslthl"
                version='1.0'>

    <xsl:import href="http://docbook.sourceforge.net/release/xsl/current/fo/docbook.xsl"/>
    <xsl:import href="http://docbook.sourceforge.net/release/xsl/current/fo/highlight.xsl"/>
    <xsl:import href="titlepage/spring-pdf.xsl"/>

<!-- Use nice graphics for admonitions -->
    <xsl:param name="admon.graphics">'1'</xsl:param>
    <xsl:param name="admon.graphics.path">images/admon/</xsl:param>
    <xsl:param name="admon.graphics.extension">.png</xsl:param>

<!-- resize the admon graphics. they're width 36pt by default
     even though the graphics that ship with docbook are 24x24 -->
    <xsl:template match="*" mode="admon.graphic.width">
      <xsl:param name="node" select="."/>
      <xsl:text>24pt</xsl:text>
    </xsl:template>

    <xsl:param name="paper.type" select="'A4'"/>
    <xsl:param name="page.margin.top" select="'1cm'"/>
    <xsl:param name="region.before.extent" select="'1cm'"/>
    <xsl:param name="body.margin.top" select="'1.5cm'"/>

    <xsl:param name="body.margin.bottom" select="'1.5cm'"/>
    <xsl:param name="region.after.extent" select="'1cm'"/>
    <xsl:param name="page.margin.bottom" select="'1cm'"/>
    <xsl:param name="title.margin.left" select="'0cm'"/>

<!--###################################################
        Header
    ################################################### -->

<!-- More space in the center header for long text -->
    <xsl:attribute-set name="header.content.properties">
        <xsl:attribute name="font-family">
            <xsl:value-of select="$body.font.family"/>
        </xsl:attribute>
        <xsl:attribute name="margin-left">-5em</xsl:attribute>
        <xsl:attribute name="margin-right">-5em</xsl:attribute>
    </xsl:attribute-set>

<!--###################################################
        Table of Contents
    ################################################### -->

    <xsl:param name="generate.toc">
        book      toc,title
    </xsl:param>

<!--###################################################
        Custom Header
    ################################################### -->

    <xsl:template name="header.content">
        <xsl:param name="pageclass" select="''"/>
        <xsl:param name="sequence" select="''"/>
        <xsl:param name="position" select="''"/>
        <xsl:param name="gentext-key" select="''"/>

        <xsl:variable name="Version">
            <xsl:choose>
                <xsl:when test="//productname">
                    <xsl:value-of select="//productname"/><xsl:text> </xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>please define productname in your docbook file!</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:choose>
            <xsl:when test="$sequence='blank'">
                <xsl:choose>
                    <xsl:when test="$position='center'">
                        <xsl:value-of select="$Version"/>
                    </xsl:when>

                    <xsl:otherwise>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>

            <xsl:when test="$pageclass='titlepage'">
            </xsl:when>

            <xsl:when test="$position='center'">
                <xsl:value-of select="$Version"/>
            </xsl:when>

            <xsl:otherwise>
            </xsl:otherwise>
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
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:variable name="Title">
            <xsl:value-of select="//title"/>
        </xsl:variable>

        <xsl:choose>
            <xsl:when test="$sequence='blank'">
                <xsl:choose>
                    <xsl:when test="$double.sided != 0 and $position = 'left'">
                        <xsl:value-of select="$Version"/>
                    </xsl:when>

                    <xsl:when test="$double.sided = 0 and $position = 'center'">
                    </xsl:when>

                    <xsl:otherwise>
                        <fo:page-number/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>

            <xsl:when test="$pageclass='titlepage'">
            </xsl:when>

            <xsl:when test="$double.sided != 0 and $sequence = 'even' and $position='left'">
                <fo:page-number/>
            </xsl:when>

            <xsl:when test="$double.sided != 0 and $sequence = 'odd' and $position='right'">
                <fo:page-number/>
            </xsl:when>

            <xsl:when test="$double.sided = 0 and $position='right'">
                <fo:page-number/>
            </xsl:when>

            <xsl:when test="$double.sided != 0 and $sequence = 'odd' and $position='left'">
                <xsl:value-of select="$Version"/>
            </xsl:when>

            <xsl:when test="$double.sided != 0 and $sequence = 'even' and $position='right'">
                <xsl:value-of select="$Version"/>
            </xsl:when>

            <xsl:when test="$double.sided = 0 and $position='left'">
                <xsl:value-of select="$Version"/>
            </xsl:when>

            <xsl:when test="$position='center'">
                <xsl:value-of select="$Title"/>
            </xsl:when>

            <xsl:otherwise>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="processing-instruction('hard-pagebreak')">
        <fo:block break-before='page'/>
    </xsl:template>

<!--###################################################
        Extensions
    ################################################### -->

    <!-- These extensions are required for table printing and other stuff -->
    <xsl:param name="fop1.extensions" select="1"></xsl:param>    <!-- INT-2346 Acrobat ToC Pane -->
    <xsl:param name="use.extensions">1</xsl:param>
    <xsl:param name="tablecolumns.extension">0</xsl:param>
    <xsl:param name="callout.extensions">1</xsl:param>

<!--###################################################
        Paper & Page Size
    ################################################### -->

    <!-- Paper type, no headers on blank pages, no double sided printing -->
    <xsl:param name="double.sided">0</xsl:param>
    <xsl:param name="headers.on.blank.pages">0</xsl:param>
    <xsl:param name="footers.on.blank.pages">0</xsl:param>

<!--###################################################
        Fonts & Styles
    ################################################### -->

    <xsl:param name="hyphenate">false</xsl:param>

<!-- Default Font size -->
    <xsl:param name="body.font.master">11</xsl:param>
    <xsl:param name="body.font.small">8</xsl:param>

    <!-- Line height in body text -->
    <xsl:param name="line-height">1.4</xsl:param>

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
    <xsl:attribute-set name="section.title.level4.properties">
        <xsl:attribute name="space-before.optimum">0.3em</xsl:attribute>
        <xsl:attribute name="space-before.minimum">0.3em</xsl:attribute>
        <xsl:attribute name="space-before.maximum">0.3em</xsl:attribute>
        <xsl:attribute name="font-size">
            <xsl:value-of select="$body.font.master * 0.9"/>
            <xsl:text>pt</xsl:text>
        </xsl:attribute>
        <xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
        <xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
        <xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>
    </xsl:attribute-set>


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
            <xsl:value-of select="$body.font.small * 1.0"/>
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
        <xsl:attribute name="margin-left">0.5em</xsl:attribute>
        <xsl:attribute name="margin-right">0.5em</xsl:attribute>
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

<!-- Show only Sections up to level 3 in the TOCs -->
    <xsl:param name="toc.section.depth">3</xsl:param>

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

    <xsl:param name="ulink.footnotes">true</xsl:param>
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
