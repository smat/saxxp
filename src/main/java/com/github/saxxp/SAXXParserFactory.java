package com.github.saxxp;

import com.github.saxxp.annotation.ParseFromXmlEnumIdentifier;
import com.github.saxxp.annotation.ParseFromXmlWithXPath;
import com.github.saxxp.exception.SAXXParserException;
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

public class SAXXParserFactory {
    private PrimitiveFieldParserFactory primitiveFieldParserFactory;

    public SAXXParserFactory() {
        primitiveFieldParserFactory = new PrimitiveFieldParserFactory();
    }

    public <T extends Object> SAXXParser<T> createXmlParser(Class<T> clazz) {
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

                    FieldParser fieldParser = primitiveFieldParserFactory.createFieldParser(field, xPath);
                    if (fieldParser != null) {
                        parseableElements.add(fieldParser);
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
                    throw new IllegalArgumentException("Error createing XPath, invalid expression", e);
                }
            }
        }

        return new SAXXParserImpl<T>(clazz, parseableElements);
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
    private class ListFieldParser extends FieldParser{
        private final Class elementClazz;
        private SAXXParser SAXXParser;
        private FieldParser fieldParser;

        private class ObjectWrapper {
            public Object object;
        }

        public ListFieldParser(Field field, XPath xPath, Class elementClazz) {
            super(field, xPath);
            this.elementClazz = elementClazz;
            try {
                fieldParser = primitiveFieldParserFactory.createFieldParser(ObjectWrapper.class.getField("object"), XPath.newInstance("."), elementClazz);
            } catch (NoSuchFieldException e) {
                throw new SAXXParserException("Could not create List parser", e);
            } catch (JDOMException e) {
                throw new SAXXParserException("Could not create new XPath for List parser", e);
            }
            if (fieldParser == null) {
                SAXXParser = SAXXParserFactory.this.createXmlParser(elementClazz);
            }
        }
        @Override
        public void parseElement(Object obj, Object doc) throws JDOMException, IllegalAccessException, SAXXParserException {
            ObjectWrapper wrapper = new ObjectWrapper();
            List objList = (List) field.get(obj);
            if (objList == null) {
                objList = new ArrayList();
                field.set(obj, objList);
            }
            List<Element> list = xPath.selectNodes(doc);
            for (Element element : list) {
                if (fieldParser != null) {
                    fieldParser.parseElement(wrapper, element);
                    objList.add(wrapper.object);
                }
                else if (SAXXParser != null) {
                    Object returnObj = SAXXParser.parse(element);
                    objList.add(returnObj);
                }
            }
        }
    }

    private static class SAXXParserImpl<T extends Object> implements SAXXParser<T> {
        private final Class<T> clazz;
        private final List<FieldParser> parseableElements;
        private Constructor constructor;

        public SAXXParserImpl(Class<T> clazz, List<FieldParser> parseableElements) {
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
                throw new SAXXParserException("Could not parse XML using XPath", e);
            } catch (IllegalAccessException e) {
                throw new SAXXParserException("Could not access field in object", e);
            } catch (InstantiationException e) {
                throw new SAXXParserException("Could not create new instance of object", e);
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
                throw new SAXXParserException("Could not parse input XML", e);
            } catch (IOException e) {
                throw new SAXXParserException("Could not read XML stream", e);
            }
        }
    }

}
