import javax.xml.parsers.SAXParserFactorya
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.*


String EOL = "---EOL---"
File doc = new File("/home/rwinch/git/spring-framework/src/reference/docbook/index.adoc")

class Docbook5Handler extends DefaultHandler {
    File doc
    def ignored = ['book','title','subtitle', 'authorgroup', 'author','personname','productname','releaseinfo','toc','partintro','itemizedlist','tgroup','colspec','thead','tbody','mediaobject','imageobject','info']
    def sections = []
    def currentSection = new Section()
    def qName
    def attrs
    def sectionIndent = 0
    def EOL = "\n"

    Docbook5Handler(File doc) {
        this.doc = doc
    }

    void startElement(String ns, String localName, String qName, Attributes attrs) {
        if(isSection(qName)) {
            /*def space = (" " * sectionIndent)
            println space + "<$qName>"*/
            def removeLast = currentSection.title.contains("TBD")
            if(removeLast) {
                def lastSection = currentSection
                sections.remove(lastSection)
                currentSection = new Section(id: lastSection.id)
            } else {
                currentSection = new Section()
            }

            sectionIndent++
            currentSection.id = attrs.getValue('xml:id') ?: 'TBD'
            currentSection.indent = sectionIndent
            sections.add(currentSection)
        } else if(isCode(qName)) {
            currentSection.content += "`"
        } else if(qName == "quote") {
            currentSection.content += '"'
        } else if(qName == "note") {
            currentSection.content += """${EOL}[NOTE]${EOL}====${EOL}"""
        } else if(qName == "para") {
            if(!currentSection.content.endsWith("*  ") && !currentSection.content.endsWith("* ")) {
                currentSection.content += """${EOL}"""
            }
        } else if(qName == "tip") {
            currentSection.content += """${EOL}[TIP]${EOL}====${EOL}"""
        } else if(qName == "literallayout") {
            currentSection.content += """${EOL}----${EOL}"""
        } else if(qName == "sidebar") {
            currentSection.content += """${EOL}****${EOL}"""
        } else if(qName == "footnote") {
            currentSection.content += " footnote:["
        } else if(qName == "emphasis") {
            currentSection.content += "__"
        } else if(qName == "programlisting") {
            def language = attrs.getValue("language")
            def codeStyle = language ? "," + language : ""
            currentSection.content += """${EOL}${EOL}[source${codeStyle}]${EOL}----${EOL}"""
        } else if(["itemizedlist","orderedlist"].contains(qName)) {
            currentSection.content += "${EOL}${EOL}"
        } else if(qName == "listitem") {
            currentSection.content += "* "
        } else if(qName == "figure") {
            currentSection.content += "${EOL}"
        } else if(qName == "imagedata") {
            def href = attrs.getValue("fileref")
            currentSection.content += "image::$href[]${EOL}${EOL}"
        }else if(qName == "table") {
            currentSection.content += "${EOL}${EOL}|==="
        } else if(qName == "row") {
//            currentSection.content += "${EOL}"
        } else if(qName == "entry") {
            currentSection.content += "${EOL}| "
        } else if(qName == "xi:include") {
            def name = attrs.getValue("href").replaceAll(".xml",".adoc")
            //def handler = new Docbook5Handler(new File(doc.parent, name))
            //handler.sectionIndent = sectionIndent
            //sections += handler.parseSections()
            currentSection.content += "${EOL}include::"+name+"[]"
        } else if(isLink(qName)) {
            def crossRef = attrs.getValue("linkend")
            if(crossRef) {
                currentSection.content += "<<${crossRef},"
            } else {
                def href = attrs.getValue("xlink:href")
                currentSection.content += "$href["
            }
        } else if (ignored.contains(qName)) {
            // ignore
        } else {
            throw new RuntimeException("Unhandled in "+doc+ " at "+qName)
        }
        this.attrs = attrs
        this.qName = qName
    }

    void characters(char[] chars, int offset, int length) {
       def content = new String(chars, offset, length)
       if(!" ".equals(content) && content.trim().equals("")) {
           return
       }
       if(qName != 'programlisting' && qName != 'literallayout') {
           content = content.replaceAll("\\n\\s+", " ")
       }
       if (qName == "title") {
           currentSection.title += content
       } else {
           currentSection.content += content
       }
    }
    void endElement(String ns, String localName, String qName) {
        if(isSection(qName)) {
            /*def space = (" " * sectionIndent)
            println space + "</$qName>"*/
            /*sections.add(currentSection)
            currentSection = new Section()
            currentSection.indent = sectionIndent*/

            sectionIndent--
        } else if(isCode(qName)) {
            currentSection.content += "`"
        } else if(qName == "para") {
            currentSection.content += """${EOL}"""
        } else if(qName == "quote") {
            currentSection.content += '"'
        } else if(qName == "note") {
            currentSection.content += """${EOL}====${EOL}"""
        } else if(qName == "tip") {
            currentSection.content += """${EOL}====${EOL}"""
        } else if(qName == "literallayout") {
            currentSection.content += """${EOL}----${EOL}"""
        } else if(qName == "sidebar") {
            currentSection.content += """${EOL}****${EOL}"""
        } else if(qName == "footnote") {
            currentSection.content += "]"
        } else if(qName == "emphasis") {
            currentSection.content += "__"
        } else if(["itemizedlist","orderedlist"].contains(qName)) {
            currentSection.content += "${EOL}"
        } else if(qName == "listitem") {
            currentSection.content += "${EOL}"
        } else if(qName == "row") {
            currentSection.content += "${EOL}"
        } else if(qName == "table") {
            currentSection.content += "|===${EOL}${EOL}"
        } else if(qName == "programlisting") {
            currentSection.content += "${EOL}----${EOL}${EOL}"
            qName = "dummy"
        } else if(qName == "figure") {
            currentSection.content += "${EOL}"
        } else if(isLink(qName)) {
            def crossRef = attrs.getValue("linkend")
            if(crossRef) {
                if(currentSection.content.endsWith(",")) {
                    currentSection.content = currentSection.content.substring(0, currentSection.content.length() - 1)
                }
                currentSection.content += ">>"
            } else {
                def href = attrs.getValue("xlink:href")
                if(!href) {
                    href = attrs.getValue("url")
                }
                currentSection.content += "]"
            }
        }
    }

    def isLink(qName) {
        ["link","xref","uri",'ulink'].contains(qName)
    }
    def isCode(qName) {
        ["classname","interfacename",'filename','literal','methodname','code','exceptionname','package','property','parameter'].contains(qName)
    }

    def isSection(qName) {
        return ['part','preface','chapter', 'section','appendix'].contains(qName)
    }

    def isTitle(qName) {
        return ['title'].contains(qName)
    }

    def parseSections() {
        def reader = SAXParserFactory.newInstance().newSAXParser().XMLReader
        reader.setContentHandler(this)
        reader.parse(new InputSource(new FileInputStream(doc)))
        sections
    }
}



class Section {

    def id = ""

    def indent

    def title = ""

    def content = ""

    def getFormattedTitle() {
        def t = title ?: "TBD"
        ("=" * indent) + " " + t.replaceAll("\\n","")
    }

    def getFormattedContent() {
        content.replaceFirst("^\\s+", "")
    }
}


def handler = new Docbook5Handler(doc)

println """= Spring Security Reference
:author: Ben Alex, Luke Taylor, Rob Winch

Spring Security is a powerful and highly customizable authentication and access-control framework. It is the de-facto standard for securing Spring-based applications.
"""
handler.parseSections().each { section->
    if(section.id != "TBD") {
        println "[[$section.id]]"
    }
    println section.formattedTitle
    println section.formattedContent
    println ""

}
