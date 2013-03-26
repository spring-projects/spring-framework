
                           JDiff Doclet
                           ------------

                           Matthew Doar
                          mdoar@pobox.com


The JDiff doclet is used to generate a report describing the
difference between two public Java APIs. 

The file jdiff.html contains the reference page for JDiff.  The latest
version of JDiff can be downloaded at:
http://sourceforge.net/projects/javadiff

To use the Ant task on your own project, see example.xml. More examples
of using JDiff to compare the public APIs of J2SE1.3 and J2SE1.4 can
be seen at http://www.jdiff.org

For an example with the source distribution, run "ant" and
look at the HTML output in ./build/reports/example/changes.html 
The page at ./build/reports/example/changes/com.acme.sp.SPImpl.html
shows what a typical page of changes looks like. 

System Requirements
-------------------

JDiff has been tested with all releases of Java since J2SE1.2 but
releases of JDiff after 1.10.0 focus on JDK1.5.

License
-------

JDiff is licensed under the Lesser GNU General Public License (LGPL).
See the file LICENSE.txt.

Acknowledgements
----------------

JDiff uses Stuart D. Gathman's Java translation of Gene Myers' O(ND) 
difference algorithm.

JDiff uses Xerces 1.4.2 from http://www.apache.org.

JDiff also includes a script to use the classdoc application from
http://classdoc.sourceforge.net or http://www.jensgulden.de, by Jens
Gulden, (mail@jensgulden.de), to call a doclet such as jdiff on a .jar
file rather than on source.

Many thanks to the reviewers at Sun and Vitria who gave feedback on early
versions of JDiff output, and also to the distillers of Laphroaig, and to
Arturo Fuente for his consistently fine cigars which helped inspire
much of this work.


Footnote:

If you are looking for a generalized diff tool for XML, try diffmk from
http://wwws.sun.com/software/xml/developers/diffmk/
