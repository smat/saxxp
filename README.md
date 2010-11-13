SAXXP (Simple Annotation XPath XML Parser for Java)
===================================================
SAXXP is an annotation framework for Java built on top of the [JDOM XML Parser](http://jdom.org/).

It works by annotating member variables with an XPath expression, and then creates and instance
of the class derived from a XML document. See the example below. 

Example
-------
        class DomainClass {
            @XPath("//tagname")
            private int variabel;
            public int getVariable() { return variabel; }
        }
        Parser<DomainClass> parser = (new ParserFactory()).createXmlParser(DomainClass.class);
        DomainClass domainClass = parser.parse("<tagname>2</tagname>");

Motivation
----------
A very common task whan writing Java applications is to parse XML from various sources. There are several
good libraries out there, but most of them are very complex. This library tries to make it easier to parse
XML documents by using annotions to seperate the POJO from the XML.
