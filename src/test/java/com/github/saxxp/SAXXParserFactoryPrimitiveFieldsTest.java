package com.github.saxxp;

import com.github.saxxp.annotation.ParseFromXmlWithXPath;
import com.github.saxxp.exception.SAXXParserException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SAXXParserFactoryPrimitiveFieldsTest {
    private SAXXParserFactory factory;

    @Before
    public void setup() {
        factory = new SAXXParserFactory();
    }

    @Test
    public void shouldParseInteger() throws SAXXParserException {
        testIfParsable(IntegerTestObj.class, 1);
    }

    @Test
    public void shouldSetIntegerToZeroIfFieldIsEmpty() throws SAXXParserException {
        testIfParsable(IntegerTestObj.class, 0, "");
    }

    @Test
    public void shouldParseFloat() throws SAXXParserException {
        testIfParsable(FloatTestObj.class, 1.0F);
    }

    @Test
    public void shouldSetFloatToZeroIfFieldIsEmpty() throws SAXXParserException {
        testIfParsable(FloatTestObj.class, 0.0F, "");
    }

    @Test
    public void shouldParseDouble() throws SAXXParserException {
        testIfParsable(DoubleTestObj.class, 1.0);
    }

    @Test
    public void shouldSetDoubleToZeroIfFieldIsEmpty() throws SAXXParserException {
        testIfParsable(DoubleTestObj.class, 0.0, "");
    }

    @Test
    public void shouldParseByte() {
        testIfParsable(ByteTestObj.class, (byte) 1);
    }

    @Test
    public void shouldSetByteToZeroIfFieldIsEmpty() {
        testIfParsable(ByteTestObj.class, (byte) 0, "");
    }

    @Test
    public void shouldParseShort() {
        testIfParsable(ShortTestObj.class, (short) 1);
    }

    @Test
    public void shouldSetShortToZeroIfFieldIsEmpty() {
        testIfParsable(ShortTestObj.class, (short) 0, "");
    }

    @Test
    public void shouldParseLong() {
        testIfParsable(LongTestObj.class, 1L);
    }

    @Test
    public void shouldSetLongToZeroIfFieldIsEmpty() {
        testIfParsable(LongTestObj.class, 0L, "");
    }

    @Test
    public void shouldCharLong() {
        testIfParsable(CharTestObj.class, 'a');
    }

    @Test
    public void shouldSetCharToZeroIfFieldIsEmpty() {
        testIfParsable(CharTestObj.class, (char) 0, "");
    }

    @Test
    public void shouldParseString() throws SAXXParserException {
        testIfParsable(StringTestObj.class, "test");
    }

    @Test
    public void shouldParseBoolean() throws SAXXParserException {
        BooleanTestObj response;
        SAXXParser<BooleanTestObj> parser = factory.createXmlParser(BooleanTestObj.class);
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

    private void testIfParsable(Class clazz, Object expected) throws SAXXParserException {
        testIfParsable(clazz, expected, expected.toString());
    }

    private void testIfParsable(Class clazz, Object expected, String xmlString) throws SAXXParserException {
        SAXXParser<TestableObject> parser = factory.createXmlParser(clazz);
        TestableObject response = parser.parse("<test>" + xmlString + "</test>");
        assertEquals(expected, response.getTest());
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

    public static class DoubleTestObj implements TestableObject<Double> {
        @ParseFromXmlWithXPath("/test")
        private double testFloat;

        public Double getTest() {
            return testFloat;
        }
    }

    public static class ByteTestObj implements TestableObject<Byte> {
        @ParseFromXmlWithXPath("/test")
        private byte testByte;

        public Byte getTest() {
            return testByte;
        }
    }

    public static class ShortTestObj implements TestableObject<Short> {
        @ParseFromXmlWithXPath("/test")
        private short testShort;

        public Short getTest() {
            return testShort;
        }
    }

    public static class LongTestObj implements TestableObject<Long> {
        @ParseFromXmlWithXPath("/test")
        private long testShort;

        public Long getTest() {
            return testShort;
        }
    }

    public static class CharTestObj implements TestableObject<Character> {
        @ParseFromXmlWithXPath("/test")
        private char testShort;

        public Character getTest() {
            return testShort;
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

}
