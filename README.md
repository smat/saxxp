SAXXP (Simple Annotation XPath XML Parser for Java)
===================================================

Example
-------
    public class DomainClass {
        @ParseXmlWithXPath("//tagname")
        private int variabel;
        public int getVariable() { return variabel; }
    }
    SAXXParser parser = SAXXParserFactory.createNewParser(DomainClass.class);
    DomainClass domainClass = parser.parse("<tagname>2</tagname>");