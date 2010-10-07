package com.github.saxxp;

import com.github.saxxp.annotation.ParseFromXmlWithXPath;
import com.github.saxxp.exception.SAXXParserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SAXXParserFactoryCollectionFieldsTest {
    private SAXXParserFactory factory;

    @Before
    public void setup() {
        factory = new SAXXParserFactory();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenGenericInListIsUndefined() {
        SAXXParser parser = factory.createXmlParser(UndefinedListTestObj.class);
    }

    @Test
    public void shouldParseListOfStrings() throws SAXXParserException {
        SAXXParser<ListOfStringsTestObj> parser = factory.createXmlParser(ListOfStringsTestObj.class);
        String xml = "<test><array>A</array><array>B</array></test>";
        ListOfStringsTestObj response = parser.parse(xml);
        assertEquals("A", response.getTest().get(0));
        assertEquals("B", response.getTest().get(1));
    }

    @Test
    public void shouldParseListOfInts() throws SAXXParserException {
        SAXXParser<ListOfIntsTestObj> parser = factory.createXmlParser(ListOfIntsTestObj.class);
        String xml = "<test><array>1</array><array>2</array></test>";
        ListOfIntsTestObj response = parser.parse(xml);
        assertEquals(1, (int) response.getTest().get(0));
        assertEquals(2, (int) response.getTest().get(1));
    }

    @Test
    public void shouldParseListOfXmlAnnotatedClass() throws SAXXParserException {
        SAXXParser<ListOfAnnotatedClassTestObj> parser = factory.createXmlParser(ListOfAnnotatedClassTestObj.class);
        String xml = "<test><array><string>A</string><int>1</int></array><array><string>B</string><int>2</int></array></test>";
        ListOfAnnotatedClassTestObj response = parser.parse(xml);
        Assert.assertEquals(SeveralAnnotatedFieldsTestObject.class, response.getTest().get(0).getClass());
        assertEquals("A", response.getTest().get(0).getTest());
        assertEquals(1, response.getTest().get(0).getTestInt());
        assertEquals("B", response.getTest().get(1).getTest());
        assertEquals(2, response.getTest().get(1).getTestInt());
    }

    @Test
    public void shouldCreateNewArrayListIfListIsNullObject() {
        SAXXParser<NullListTestObject> parser = factory.createXmlParser(NullListTestObject.class);
        String xml = "<test><array>1</array><array>2</array></test>";
        NullListTestObject response = parser.parse(xml);
        assertEquals(1d, response.getTest().get(0), 0.1);
        assertEquals(2d, response.getTest().get(1), 0.1);
    }

    public static class ListOfStringsTestObj implements TestableObject<List> {
        @ParseFromXmlWithXPath("/test/array")
        private ArrayList<String> test = new ArrayList<String>();

        public ArrayList<String> getTest() {
            return test;
        }
    }
    public static class ListOfIntsTestObj implements TestableObject<List> {
        @ParseFromXmlWithXPath("/test/array")
        private ArrayList<Integer> test = new ArrayList<Integer>();

        public ArrayList<Integer> getTest() {
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

    public static class ListOfAnnotatedClassTestObj implements TestableObject<List<SeveralAnnotatedFieldsTestObject>> {
        @ParseFromXmlWithXPath("/test/array")
        private List<SeveralAnnotatedFieldsTestObject> list = new ArrayList<SeveralAnnotatedFieldsTestObject>();
        public List<SeveralAnnotatedFieldsTestObject> getTest() {
            return list;
        }
    }

    public static class NullListTestObject implements TestableObject<List<Double>> {
        @ParseFromXmlWithXPath("/test/array")
        private List<Double> test;
        public List<Double> getTest() {
            return test;
        }
    }

    public static class SeveralAnnotatedFieldsTestObject {
        @ParseFromXmlWithXPath("string")
        private String test;
        @ParseFromXmlWithXPath("int")
        private int testInt;

        public String getTest() {
            return test;
        }

        public int getTestInt() {
            return testInt;
        }
    }
}
