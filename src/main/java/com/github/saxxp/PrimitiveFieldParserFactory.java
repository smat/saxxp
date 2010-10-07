package com.github.saxxp;

import com.github.saxxp.exception.SAXXParserException;
import org.apache.commons.lang.StringUtils;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import java.lang.reflect.*;
import java.util.HashMap;

class PrimitiveFieldParserFactory {
    private HashMap<Class, Class<FieldParser>> parserMap;

    PrimitiveFieldParserFactory() {
        parserMap = new HashMap<Class, Class<FieldParser>>();
        for (Class clazz : this.getClass().getDeclaredClasses()) {
            if (FieldParser.class.isAssignableFrom(clazz)) {
                Type type = clazz.getGenericSuperclass();
                if (type instanceof ParameterizedType) {
                    Type[] generics = ((ParameterizedType) type).getActualTypeArguments();
                    parserMap.put((Class) generics[0], (Class<FieldParser>) clazz);
                    try {
                        Field primitive = ((Class) generics[0]).getDeclaredField("TYPE");
                        parserMap.put((Class) primitive.get(null), (Class<FieldParser>) clazz);
                    } catch (NoSuchFieldException ignore) {
                    } catch (IllegalAccessException ignore) {
                    }
                }
            }
        }
    }

    public FieldParser createFieldParser(Field field, XPath xPath) {
        return createFieldParser(field, xPath, field.getType());
    }

    public FieldParser createFieldParser(Field field, XPath xPath, Class clazz) {
        Class<FieldParser> fieldParserClass = parserMap.get(clazz);
        if (fieldParserClass == null) {
            return null;
        }
        try {
            Constructor<FieldParser> parserConstructor = fieldParserClass.getDeclaredConstructor(PrimitiveFieldParserFactory.class, Field.class, XPath.class);
            return parserConstructor.newInstance(this, field, xPath);
        } catch (NoSuchMethodException e) {
            throw new SAXXParserException("Could not find constructor for " + fieldParserClass, e);
        } catch (Exception e) {
            throw new SAXXParserException("Could not create new parser for " + field.getType(), e);
        }
    }

    private abstract class PrimitiveFieldParser<T extends Object> extends FieldParser {
        protected PrimitiveFieldParser(Field field, XPath xPath) {
            super(field, xPath);
        }
    }

    private class IntegerFieldParser extends PrimitiveFieldParser<Integer> {
        public IntegerFieldParser(Field field, XPath xPath) {
            super(field, xPath);
        }
        @Override
        public void parseElement(Object obj, Object doc) throws JDOMException, IllegalAccessException {
            Element element = (Element) xPath.selectSingleNode(doc);
            if (element != null) {
                if (StringUtils.isBlank(element.getTextTrim())) {
                    field.set(obj, 0);
                }
                else {
                    field.set(obj, Integer.parseInt(element.getText()));
                }
            }
        }
    }
    private class FloatFieldParser extends PrimitiveFieldParser<Float> {
        public FloatFieldParser(Field field, XPath xPath) {
            super(field, xPath);
        }
        @Override
        public void parseElement(Object obj, Object doc) throws JDOMException, IllegalAccessException {
            Element element = (Element) xPath.selectSingleNode(doc);
            if (element != null) {
                if (StringUtils.isBlank(element.getTextTrim())) {
                    field.set(obj, 0.0F);
                }
                else {
                    field.set(obj, Float.parseFloat(element.getText()));
                }
            }
        }
    }
    private class DoubleFieldParser extends PrimitiveFieldParser<Double> {
        public DoubleFieldParser(Field field, XPath xPath) {
            super(field, xPath);
        }
        @Override
        public void parseElement(Object obj, Object doc) throws JDOMException, IllegalAccessException {
            Element element = (Element) xPath.selectSingleNode(doc);
            if (element != null) {
                if (StringUtils.isBlank(element.getTextTrim())) {
                    field.set(obj, 0.0);
                }
                else {
                    field.set(obj, Double.parseDouble(element.getText()));
                }
            }
        }
    }
    private class ByteFieldParser extends PrimitiveFieldParser<Byte> {
        public ByteFieldParser(Field field, XPath xPath) {
            super(field, xPath);
        }
        @Override
        public void parseElement(Object obj, Object doc) throws JDOMException, IllegalAccessException {
            Element element = (Element) xPath.selectSingleNode(doc);
            if (element != null) {
                if (StringUtils.isBlank(element.getTextTrim())) {
                    field.set(obj, (byte) 0);
                }
                else {
                    field.set(obj, Byte.parseByte(element.getText()));
                }
            }
        }
    }
    private class ShortFieldParser extends PrimitiveFieldParser<Short> {
        public ShortFieldParser(Field field, XPath xPath) {
            super(field, xPath);
        }
        @Override
        public void parseElement(Object obj, Object doc) throws JDOMException, IllegalAccessException {
            Element element = (Element) xPath.selectSingleNode(doc);
            if (element != null) {
                if (StringUtils.isBlank(element.getTextTrim())) {
                    field.set(obj, (short) 0);
                }
                else {
                    field.set(obj, Short.parseShort(element.getText()));
                }
            }
        }
    }
    private class LongFieldParser extends PrimitiveFieldParser<Long> {
        public LongFieldParser(Field field, XPath xPath) {
            super(field, xPath);
        }
        @Override
        public void parseElement(Object obj, Object doc) throws JDOMException, IllegalAccessException {
            Element element = (Element) xPath.selectSingleNode(doc);
            if (element != null) {
                if (StringUtils.isBlank(element.getTextTrim())) {
                    field.set(obj, 0L);
                }
                else {
                    field.set(obj, Long.parseLong(element.getText()));
                }
            }
        }
    }
    private class CharFieldParser extends PrimitiveFieldParser<Character> {
        public CharFieldParser(Field field, XPath xPath) {
            super(field, xPath);
        }
        @Override
        public void parseElement(Object obj, Object doc) throws JDOMException, IllegalAccessException {
            Element element = (Element) xPath.selectSingleNode(doc);
            if (element != null) {
                if (StringUtils.isBlank(element.getTextTrim())) {
                    field.set(obj, (char) 0);
                }
                else {
                    field.set(obj, element.getTextTrim().charAt(0));
                }
            }
        }
    }
    private class BooleanFieldParser extends PrimitiveFieldParser<Boolean> {
        public BooleanFieldParser(Field field, XPath xPath) {
            super(field, xPath);
        }
        @Override
        public void parseElement(Object obj, Object doc) throws JDOMException, IllegalAccessException {
            Element element = (Element) xPath.selectSingleNode(doc);
            if (element != null) {
                if (element.getText().compareToIgnoreCase("true") == 0 || element.getText().compareToIgnoreCase("1") == 0) {
                    field.set(obj, true);
                }
                else {
                    field.set(obj, false);
                }
            }
        }
    }
    private class StringFieldParser extends PrimitiveFieldParser<String> {
        public StringFieldParser(Field field, XPath xPath) {
            super(field, xPath);
        }
        @Override
        public void parseElement(Object obj, Object doc) throws JDOMException, IllegalAccessException {
            Element element = (Element) xPath.selectSingleNode(doc);
            if (element != null)
                field.set(obj, element.getText());
        }
    }
}
