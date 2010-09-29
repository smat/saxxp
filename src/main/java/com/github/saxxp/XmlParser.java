package com.github.saxxp;

import org.jdom.Element;

import java.io.InputStream;

public interface  XmlParser<T extends Object> {
    public T parse(String xml);
    public T parse(InputStream stream);
    public T parse(Element element);
};
