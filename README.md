SAXXP (Simple Annotation XPath XML Parser for Java)
===================================================

Example
-------
<code>
    public class DomainClass {
        @ParseXmlWithXPath("//tagname")
        private int variabel;
        public int getVariable() { return variabel; }
    }
    XmlParser parser = XmlParserFactory.createNewParser(DomainClass.class);
    DomainClass domainClass = parser.parse("<tagname>2</tagname>");
</code>