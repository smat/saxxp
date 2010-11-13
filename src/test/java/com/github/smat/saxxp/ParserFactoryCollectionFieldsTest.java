package com.github.smat.saxxp;

import com.github.smat.saxxp.annotation.XPath;
import com.github.smat.saxxp.exception.SaxxpException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ParserFactoryCollectionFieldsTest {
    private ParserFactory factory;

    @Before
    public void setup() {
        factory = new ParserFactory();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenGenericInListIsUndefined() {
        factory.createXmlParser(UndefinedListTestObj.class);
    }

    @Test
    public void shouldParseListOfStrings() throws SaxxpException {
        Parser<ListOfStringsTestObj> parser = factory.createXmlParser(ListOfStringsTestObj.class);
        String xml = "<test><array>A</array><array>B</array></test>";
        ListOfStringsTestObj response = parser.parse(xml);
        assertEquals("A", response.getTest().get(0));
        assertEquals("B", response.getTest().get(1));
    }

    @Test
    public void shouldParseListOfInts() throws SaxxpException {
        Parser<ListOfIntsTestObj> parser = factory.createXmlParser(ListOfIntsTestObj.class);
        String xml = "<test><array>1</array><array>2</array></test>";
        ListOfIntsTestObj response = parser.parse(xml);
        assertEquals(1, (int) response.getTest().get(0));
        assertEquals(2, (int) response.getTest().get(1));
    }

    @Test
    public void shouldParseListOfXmlAnnotatedClass() throws SaxxpException {
        Parser<ListOfAnnotatedClassTestObj> parser = factory.createXmlParser(ListOfAnnotatedClassTestObj.class);
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
        Parser<NullListTestObject> parser = factory.createXmlParser(NullListTestObject.class);
        String xml = "<test><array>1</array><array>2</array></test>";
        NullListTestObject response = parser.parse(xml);
        assertEquals(1d, response.getTest().get(0), 0.1);
        assertEquals(2d, response.getTest().get(1), 0.1);
    }

    @Test
    public void shouldParseArrayOfString() {
        Parser<ArrayOfStringTestObj> parser = factory.createXmlParser(ArrayOfStringTestObj.class);
        String xml = "<test><array>A</array><array>B</array></test>";
        ArrayOfStringTestObj response = parser.parse(xml);
        assertEquals("A", response.getTest()[0]);
        assertEquals("B", response.getTest()[1]);
    }

    @Test
    public void shouldParseArrayOfInts() throws SaxxpException {
        Parser<ArrayOfIntsTestObj> parser = factory.createXmlParser(ArrayOfIntsTestObj.class);
        String xml = "<test><array>1</array><array>2</array></test>";
        ArrayOfIntsTestObj response = parser.parse(xml);
        assertEquals(1, (int) response.getTest()[0]);
        assertEquals(2, (int) response.getTest()[1]);
    }

    @Test
    public void shouldParseArrayOfXmlAnnotatedClass() throws SaxxpException {
        Parser<ArrayOfAnnotatedClassTestObj> parser = factory.createXmlParser(ArrayOfAnnotatedClassTestObj.class);
        String xml = "<test><array><string>A</string><int>1</int></array><array><string>B</string><int>2</int></array></test>";
        ArrayOfAnnotatedClassTestObj response = parser.parse(xml);
        Assert.assertEquals(SeveralAnnotatedFieldsTestObject.class, response.getTest()[0].getClass());
        assertEquals("A", response.getTest()[0].getTest());
        assertEquals(1, response.getTest()[0].getTestInt());
        assertEquals("B", response.getTest()[1].getTest());
        assertEquals(2, response.getTest()[1].getTestInt());
    }

    public static class ListOfStringsTestObj implements TestableObject<List> {
        @XPath("/test/array")
        private ArrayList<String> test = new ArrayList<String>();

        public ArrayList<String> getTest() {
            return test;
        }
    }
    public static class ListOfIntsTestObj implements TestableObject<List> {
        @XPath("/test/array")
        private ArrayList<Integer> test = new ArrayList<Integer>();

        public ArrayList<Integer> getTest() {
            return test;
        }
    }

    public static class UndefinedListTestObj implements TestableObject<List> {
        @XPath("/test")
        private List test = new ArrayList();
        public List getTest() {
            return test;
        }
    }

    public static class ListOfAnnotatedClassTestObj implements TestableObject<List<SeveralAnnotatedFieldsTestObject>> {
        @XPath("/test/array")
        private List<SeveralAnnotatedFieldsTestObject> list = new ArrayList<SeveralAnnotatedFieldsTestObject>();
        public List<SeveralAnnotatedFieldsTestObject> getTest() {
            return list;
        }
    }

    public static class NullListTestObject implements TestableObject<List<Double>> {
        @XPath("/test/array")
        private List<Double> test;
        public List<Double> getTest() {
            return test;
        }
    }

    public static class SeveralAnnotatedFieldsTestObject {
        @XPath("string")
        private String test;
        @XPath("int")
        private int testInt;

        public String getTest() {
            return test;
        }

        public int getTestInt() {
            return testInt;
        }
    }

    public static class ArrayOfStringTestObj implements TestableObject<String[]>{
        @XPath("/test/array")
        String[] test;
        public String[] getTest() {
            return test;
        }
    }
    public static class ArrayOfIntsTestObj implements TestableObject<Integer[]>{
        @XPath("/test/array")
        Integer[] test;
        public Integer[] getTest() {
            return test;
        }
    }
    public static class ArrayOfAnnotatedClassTestObj implements TestableObject<SeveralAnnotatedFieldsTestObject[]>{
        @XPath("/test/array")
        SeveralAnnotatedFieldsTestObject[] test;
        public SeveralAnnotatedFieldsTestObject[] getTest() {
            return test;
        }
    }
}
