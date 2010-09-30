package com.github.saxxp;

import com.github.saxxp.annotation.ParseFromXmlEnumIdentifier;
import com.github.saxxp.annotation.ParseFromXmlWithXPath;
import com.github.saxxp.exception.XmlParserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class XmlParserFactoryTest {
    private XmlParserFactory xmlParserFactory;

    @Before
    public void setup() {
        xmlParserFactory = new XmlParserFactory();

    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNullClassReference() {
        xmlParserFactory.createXmlParser(null);
    }

    @Test
    public void shouldParseInteger() throws XmlParserException {
        testIfParsable(IntegerTestObj.class, 1);
    }

    @Test
    public void shouldSetIntegerToZeroIfFieldIsEmpty() throws XmlParserException {
        testIfParsable(IntegerTestObj.class, 0, "");
    }

    @Test
    public void shouldParseFloat() throws XmlParserException {
        testIfParsable(FloatTestObj.class, 1.0F);
    }

    @Test
    public void shouldSetFloatToZeroIfFieldIsEmpty() throws XmlParserException {
        testIfParsable(FloatTestObj.class, 0.0F, "");
    }

    @Test
    public void shouldParseString() throws XmlParserException {
        testIfParsable(StringTestObj.class, "test");
    }

    @Test
    public void shouldParseBoolean() throws XmlParserException {
        BooleanTestObj response = new BooleanTestObj();
        XmlParser<BooleanTestObj> parser = xmlParserFactory.createXmlParser(BooleanTestObj.class);
        response = parser.parse("<test>true</test>");
        assertEquals(true, response.getTest());
        response = parser.parse("<test>false</test>");
        assertEquals(false, response.getTest());
        response = parser.parse("<test>1</test>");
        assertEquals(true, response.getTest());
        response = parser.parse("<test>0</test>");
        assertEquals(false, response.getTest());
        response = parser.parse("<test>true</test>");
        assertEquals(true, response.getTest());
        response = parser.parse("<test></test>");
        assertEquals(false, response.getTest());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowRuntimeExceptionWhenEnumDoesNotHaveIdentifierAnnotation() {
        xmlParserFactory.createXmlParser(ExceptionEnumTestObj.class);
    }

    @Test
    public void shouldParseEnum() throws XmlParserException {
        testIfParsable(EnumTestObj.class, EnumImpl.VALID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenGenericInListIsUndefined() {
        XmlParser parser = xmlParserFactory.createXmlParser(UndefinedListTestObj.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenErrorsInXPath() {
        XmlParser parser = xmlParserFactory.createXmlParser(ErrornousXPathTestObj.class);
    }

    @Test
    public void shouldParseListOfStrings() throws XmlParserException {
        XmlParser<ListOfStringsTestObj> parser = xmlParserFactory.createXmlParser(ListOfStringsTestObj.class);
        String xml = "<test><array>A</array><array>B</array></test>";
        ListOfStringsTestObj response = parser.parse(xml);
        assertEquals("A", response.getTest().get(0));
        assertEquals("B", response.getTest().get(1));
    }

    @Test
    public void shouldParseListOfXmlAnnotatedClass() throws XmlParserException {
        XmlParser<ListOfAnnotatedClassTestObj> parser = xmlParserFactory.createXmlParser(ListOfAnnotatedClassTestObj.class);
        String xml = "<test><array><string>A</string><int>1</int></array><array><string>B</string><int>2</int></array></test>";
        ListOfAnnotatedClassTestObj response = parser.parse(xml);
        Assert.assertEquals(XmlParserTestClass.class, response.getTest().get(0).getClass());
        assertEquals("A", response.getTest().get(0).getTest());
        assertEquals(1, response.getTest().get(0).getTestInt());
        assertEquals("B", response.getTest().get(1).getTest());
        assertEquals(2, response.getTest().get(1).getTestInt());
    }

    private void testIfParsable(Class clazz, Object expected) throws XmlParserException {
        testIfParsable(clazz, expected, expected.toString());
    }

    private void testIfParsable(Class clazz, Object expected, String xmlString) throws XmlParserException {
        XmlParser<TestableObject> parser = xmlParserFactory.createXmlParser(clazz);
        TestableObject response = parser.parse("<test>" + xmlString + "</test>");
        assertEquals(expected, response.getTest());
    }

    public static interface TestableObject<T> {
        public T getTest();
    }

    public static class IntegerTestObj implements TestableObject<Integer> {
        @ParseFromXmlWithXPath("/test")
        private int test;

        public Integer getTest() {
            return test;
        }
    }

    public static class FloatTestObj implements TestableObject<Float> {
        @ParseFromXmlWithXPath("/test")
        private float testFloat;

        public Float getTest() {
            return testFloat;
        }
    }

    public static class StringTestObj implements TestableObject<String> {
        @ParseFromXmlWithXPath("/test")
        private String test;
        public String getTest() {
            return test;
        }
    }

    public static class BooleanTestObj implements TestableObject<Boolean> {
        @ParseFromXmlWithXPath("/test")
        private boolean test;
        public Boolean getTest() {
            return test;
        }
    }

    public static class ListOfStringsTestObj implements TestableObject<List> {
        @ParseFromXmlWithXPath("/test/array")
        private ArrayList<String> test = new ArrayList<String>();

        public ArrayList<String> getTest() {
            return test;
        }
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

    public static class ErrornousXPathTestObj implements TestableObject<Integer> {
        @ParseFromXmlWithXPath("////asdf")
        private int test;
        public Integer getTest() {
            return test;
        }
    }

    public static class UndefinedListTestObj implements TestableObject<List> {
        @ParseFromXmlWithXPath("/test")
        private List test = new ArrayList();
        public List getTest() {
            return test;
        }
    }

    public static class ListOfAnnotatedClassTestObj implements TestableObject<List<XmlParserTestClass>> {
        @ParseFromXmlWithXPath("/test/array")
        private List<XmlParserTestClass> list = new ArrayList<XmlParserTestClass>();
        public List<XmlParserTestClass> getTest() {
            return list;
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
