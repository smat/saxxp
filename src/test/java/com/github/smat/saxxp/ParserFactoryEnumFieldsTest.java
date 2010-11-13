package com.github.smat.saxxp;

import com.github.smat.saxxp.annotation.XPath;
import com.github.smat.saxxp.annotation.XmlEnumIdentifier;
import com.github.smat.saxxp.exception.SaxxpException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ParserFactoryEnumFieldsTest {
    private ParserFactory factory;

    @Before
    public void setup() {
        factory = new ParserFactory();
    }

    @Test
    public void shouldParseEnum() throws SaxxpException {
        Parser<TestableObject> parser = factory.createXmlParser((Class) EnumTestObj.class);
        TestableObject response = parser.parse("<test>" + EnumImpl.VALID + "</test>");
        assertEquals(EnumImpl.VALID, response.getTest());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowRuntimeExceptionWhenEnumDoesNotHaveIdentifierAnnotation() {
        factory.createXmlParser(ExceptionEnumTestObj.class);
    }

    public static class EnumTestObj implements TestableObject<EnumImpl> {
        @XPath("/test")
        private EnumImpl test;
        public EnumImpl getTest() {
            return test;
        }
    }

    public static class ExceptionEnumTestObj implements TestableObject<EnumImplNoAnnotations> {
        @XPath("/test")
        private EnumImplNoAnnotations test;
        public EnumImplNoAnnotations getTest() {
            return test;
        }
    }

    public static enum EnumImpl {
        VALID("V"),
        INVALID("I");
        @XmlEnumIdentifier
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
