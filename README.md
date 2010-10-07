SAXXP (Simple Annotation XPath XML Parser for Java)
===================================================

Example
-------
        class DomainClass {
            @ParseFromXmlWithXPath("//tagname")
            private int variabel;
            public int getVariable() { return variabel; }
        }
        SAXXParser<DomainClass> parser = (new SAXXParserFactory()).createXmlParser(DomainClass.class);
        DomainClass domainClass = parser.parse("<tagname>2</tagname>");