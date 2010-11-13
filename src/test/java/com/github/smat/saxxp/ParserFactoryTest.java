package com.github.smat.saxxp;

import com.github.smat.saxxp.annotation.XPath;
import org.junit.Before;
import org.junit.Test;

public class ParserFactoryTest {
    private ParserFactory factory;

    @Before
    public void setup() {
        factory = new ParserFactory();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNullClassReference() {
        factory.createXmlParser(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenErrorsInXPath() {
        factory.createXmlParser(ErrornousXPathTestObj.class);
    }

    public static class ErrornousXPathTestObj implements TestableObject<Integer> {
        @XPath("////asdf")
        private int test;
        public Integer getTest() {
            return test;
        }
    }
}
