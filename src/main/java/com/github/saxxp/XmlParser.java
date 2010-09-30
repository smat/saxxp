package com.github.saxxp;

import com.github.saxxp.exception.XmlParserException;
import org.jdom.Element;

import java.io.InputStream;

public interface  XmlParser<T extends Object> {
    public T parse(String xml) throws XmlParserException;
    public T parse(InputStream stream) throws XmlParserException;
    public T parse(Element element) throws XmlParserException;
};
