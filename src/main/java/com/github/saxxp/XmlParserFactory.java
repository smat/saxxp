package com.github.saxxp;

import com.github.saxxp.annotation.ParseFromXmlEnumIdentifier;
import com.github.saxxp.annotation.ParseFromXmlWithXPath;
import com.github.saxxp.exception.XmlParserRuntimeException;
import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.io.IOUtils.toInputStream;

public class XmlParserFactory {
    public <T extends Object> XmlParser<T> createXmlParser(Class<T> clazz) {
        return XmlParserFactory._createXmlParser(clazz);
    }

    private static <T extends Object> XmlParser<T> _createXmlParser(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Could not create parser for null class");
        }
        final List<FieldParser> parseableElements = new ArrayList<FieldParser>();

        for (Field iteratorField : clazz.getDeclaredFields()) {
            if (iteratorField.isAnnotationPresent(ParseFromXmlWithXPath.class)) {
                try {
                    final Field field = iteratorField;
                    final ParseFromXmlWithXPath annotation = field.getAnnotation(ParseFromXmlWithXPath.class);
                    final XPath xPath = XPath.newInstance(annotation.value());
                    field.setAccessible(true);

                    if (field.getType() == int.class) {
                        parseableElements.add(new IntFieldParser(field, xPath));
                    }
                    else if (field.getType() == float.class) {
                        parseableElements.add(new FloatFieldParser(field, xPath));
                    }
                    else if (field.getType() == String.class) {
                        parseableElements.add(new StringFieldParser(field, xPath));
                    }
                    else if (field.getType() == boolean.class) {
                        parseableElements.add(new BooleanFieldParser(field, xPath));
                    }
                    else if (field.getType().isEnum()) {
                        Field identifierField = null;
                        for (Field enumField : field.getType().getDeclaredFields()) {
                            enumField.setAccessible(true);
                            if (enumField.isAnnotationPresent(ParseFromXmlEnumIdentifier.class)) {
                                identifierField = enumField;
                            }
                        }
                        if (identifierField == null) {
                            throw new IllegalArgumentException("Enum does not contains @ParseFromXmlEnumIdentifier annotation");
                        }
                        final Field enumIdentifier = identifierField;
                        parseableElements.add(new EnumFieldParser(field, xPath, enumIdentifier));
                    }
                    else if (List.class.isAssignableFrom(field.getType())) {
                        Type genericType = field.getGenericType();
                        if (genericType instanceof ParameterizedType) {
                            ParameterizedType type = (ParameterizedType) genericType;
                            Type[] generics = type.getActualTypeArguments();
                            if (generics.length >= 1) {
                                Class elementClazz = (Class) generics[0];
                                parseableElements.add(new ListFieldParser(field, xPath, elementClazz));
                            }
                        }
                        else {
                            throw new IllegalArgumentException("List does not have a defined generic type");
                        }
                    }
                } catch (JDOMException e) {
                    throw new XmlParserRuntimeException("Could not create XmlParser", e);
                }
            }
        }

        return new XmlParserImpl<T>(clazz, parseableElements);
    }

    private abstract static class FieldParser {
        protected final Field field;
        protected final XPath xPath;

        public abstract void parseElement(Object obj, Object context) throws JDOMException, IllegalAccessException;

        public FieldParser(Field field, XPath xPath) {
            this.field = field;
            this.xPath = xPath;
        }
    }

    private static class IntFieldParser extends FieldParser {
        public IntFieldParser(Field field, XPath xPath) {
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
    private static class FloatFieldParser extends FieldParser {
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
    private static class BooleanFieldParser extends FieldParser {
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
    private static class StringFieldParser extends FieldParser {
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
    private static class EnumFieldParser extends FieldParser {
        private final Field enumIdentifier;
        public EnumFieldParser(Field field, XPath xPath, Field enumIdentifier) {
            super(field, xPath);
            this.enumIdentifier = enumIdentifier;
        }
        @Override
        public void parseElement(Object obj, Object doc) throws JDOMException, IllegalAccessException {
            Element element = (Element) xPath.selectSingleNode(doc);
            if (element != null) {            
                for (Object enumElemnt : field.getType().getEnumConstants()) {
                    if (enumIdentifier.get(enumElemnt).equals(element.getText())) {
                        field.set(obj, enumElemnt);
                        break;
                    }
                }
            }
        }
    }
    private static class ListFieldParser extends FieldParser{
        private final Class elementClazz;
        private XmlParser xmlParser;

        public ListFieldParser(Field field, XPath xPath, Class elementClazz) {
            super(field, xPath);
            this.elementClazz = elementClazz;
            if (elementClazz == String.class) {
                xmlParser = null;
            }
            else {
                xmlParser = XmlParserFactory._createXmlParser(elementClazz);
            }
        }
        @Override
        public void parseElement(Object obj, Object doc) throws JDOMException, IllegalAccessException {
            List objList = (List) field.get(obj);
            List<Element> list = xPath.selectNodes(doc);
            for (Element element : list) {
                if (xmlParser != null) {
                    Object returnObj = xmlParser.parse(element);
                    objList.add(returnObj);
                }
                else if (elementClazz == String.class) {
                    objList.add(element.getText());
                }
            }
        }
    }

    private static class XmlParserImpl<T extends Object> implements XmlParser<T> {
        private final Class<T> clazz;
        private final List<FieldParser> parseableElements;
        private Constructor constructor;

        public XmlParserImpl(Class<T> clazz, List<FieldParser> parseableElements) {
            this.clazz = clazz;
            this.parseableElements = parseableElements;
            Constructor<?>[] constructors = clazz.getConstructors();
            for (Constructor constructor : constructors) {
                if (constructor.getParameterTypes().length == 0) {
                    this.constructor = constructor;
                }
            }
            if (this.constructor == null) {
                throw new IllegalArgumentException("Class " + clazz.toString() + " used in parser does not have a default constructor");
            }
        }

        private T _parse(Object context) {
            T returnObject = null;
            try {
                returnObject = clazz.newInstance();
                for (FieldParser action : parseableElements) {
                    action.parseElement(returnObject, context);
                }
            } catch (JDOMException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
            return returnObject;
        }

        public T parse(String xml) {
            return parse(toInputStream(xml));
        }

        public T parse(Element element) {
            return _parse(element);
        }

        public T parse(InputStream stream) {
            try {
                Document doc = new SAXBuilder().build(stream);
                return _parse(doc);
            } catch (JDOMException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}
