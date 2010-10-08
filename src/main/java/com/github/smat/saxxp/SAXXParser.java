package com.github.smat.saxxp;

import org.jdom.Element;

import java.io.InputStream;

public interface SAXXParser<T> {
    public T parse(String xml);
    public T parse(InputStream stream);
    public T parse(Element element);
}