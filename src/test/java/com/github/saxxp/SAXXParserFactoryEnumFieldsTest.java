package com.github.saxxp;

import com.github.saxxp.annotation.ParseFromXmlEnumIdentifier;
import com.github.saxxp.annotation.ParseFromXmlWithXPath;
import com.github.saxxp.exception.SAXXParserException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SAXXParserFactoryEnumFieldsTest {
    private SAXXParserFactory factory;

    @Before
    public void setup() {
        factory = new SAXXParserFactory();
    }

    @Test
    public void shouldParseEnum() throws SAXXParserException {
        SAXXParser<TestableObject> parser = factory.createXmlParser((Class) EnumTestObj.class);
        TestableObject response = parser.parse("<test>" + EnumImpl.VALID + "</test>");
        assertEquals(EnumImpl.VALID, response.getTest());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowRuntimeExceptionWhenEnumDoesNotHaveIdentifierAnnotation() {
        factory.createXmlParser(ExceptionEnumTestObj.class);
    }

    public static class EnumTestObj implements TestableObject<EnumImpl> {
        @ParseFromXmlWithXPath("/test")
        private EnumImpl test;
        public EnumImpl getTest() {
            return test;
        }
    }

    public static class ExceptionEnumTestObj implements TestableObject<EnumImplNoAnnotations> {
        @ParseFromXmlWithXPath("/test")
        private EnumImplNoAnnotations test;
        public EnumImplNoAnnotations getTest() {
            return test;
        }
    }

    public static enum EnumImpl {
        VALID("V"),
        INVALID("I");
        @ParseFromXmlEnumIdentifier
        private final String ident;
        EnumImpl(String ident) {
            this.ident = ident;
        }
        public String toString() { return ident; }
    }

    public static enum EnumImplNoAnnotations {
        VALID("V"),
        INVALID("I");
        private final String ident;
        EnumImplNoAnnotations(String ident) {
            this.ident = ident;
        }
    }
}
