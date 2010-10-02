package com.github.saxxp;

import com.github.saxxp.annotation.ParseFromXmlWithXPath;

public class SAXXParserTestClass {
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
