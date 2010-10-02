package com.github.saxxp;

import com.github.saxxp.exception.SAXXParserException;
import org.jdom.Element;

import java.io.InputStream;

public interface SAXXParser<T extends Object> {
    public T parse(String xml) throws SAXXParserException;
    public T parse(InputStream stream) throws SAXXParserException;
    public T parse(Element element) throws SAXXParserException;
};
