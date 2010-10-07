package com.github.saxxp;

import com.github.saxxp.exception.SAXXParserException;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import java.lang.reflect.Field;

abstract class FieldParser<T extends Object> {
    protected final Field field;
    protected final XPath xPath;

    public abstract void parseElement(T obj, Object context) throws JDOMException, IllegalAccessException, SAXXParserException;

    public FieldParser(Field field, XPath xPath) {
        this.field = field;
        this.xPath = xPath;
    }
}
