package com.github.saxxp;

import com.github.saxxp.annotation.ParseFromXmlWithXPath;
import org.junit.Before;
import org.junit.Test;

public class SAXXParserFactoryTest {
    private SAXXParserFactory factory;

    @Before
    public void setup() {
        factory = new SAXXParserFactory();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNullClassReference() {
        factory.createXmlParser(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenErrorsInXPath() {
        SAXXParser parser = factory.createXmlParser(ErrornousXPathTestObj.class);
    }

    public static class ErrornousXPathTestObj implements TestableObject<Integer> {
        @ParseFromXmlWithXPath("////asdf")
        private int test;
        public Integer getTest() {
            return test;
        }
    }
}
