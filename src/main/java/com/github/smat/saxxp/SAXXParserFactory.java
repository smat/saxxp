package com.github.smat.saxxp;

import com.github.smat.saxxp.annotation.ParseFromXmlEnumIdentifier;
import com.github.smat.saxxp.annotation.ParseFromXmlWithXPath;
import com.github.smat.saxxp.exception.SAXXParserException;
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
import java.util.HashMap;
import java.util.List;

import static org.apache.commons.io.IOUtils.toInputStream;

/**
 * Factory to create XML Parsers
 */
public class SAXXParserFactory {
    private final PrimitiveFieldParserFactory primitiveFieldParserFactory;

    public SAXXParserFactory() {
        primitiveFieldParserFactory = new PrimitiveFieldParserFactory();
    }

    /**
     * Creates a new parser
     * @param clazz The class to be parsed
     * @param <T> The class to be parsed
     * @return SAXXParser
     */
    public <T> SAXXParser<T> createXmlParser(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Could not create parser for null class");
        }
        final List<FieldParser> parseableElements = new ArrayList<FieldParser>();

        for (Field iteratorField : clazz.getDeclaredFields()) {
            if (iteratorField.isAnnotationPresent(ParseFromXmlWithXPath.class)) {
                try {
                    final ParseFromXmlWithXPath annotation = iteratorField.getAnnotation(ParseFromXmlWithXPath.class);
                    final XPath xPath = XPath.newInstance(annotation.value());
                    iteratorField.setAccessible(true);

                    FieldParser fieldParser = primitiveFieldParserFactory.createFieldParser(iteratorField, xPath);
                    if (fieldParser != null) {
                        parseableElements.add(fieldParser);
                    }
                    else if (iteratorField.getType().isEnum()) {
                        Field identifierField = null;
                        for (Field enumField : iteratorField.getType().getDeclaredFields()) {
                            enumField.setAccessible(true);
                            if (enumField.isAnnotationPresent(ParseFromXmlEnumIdentifier.class)) {
                                identifierField = enumField;
                            }
                        }
                        if (identifierField == null) {
                            throw new IllegalArgumentException("Enum does not contains @ParseFromXmlEnumIdentifier annotation");
                        }
                        final Field enumIdentifier = identifierField;
                        parseableElements.add(new EnumFieldParser(iteratorField, xPath, enumIdentifier));
                    }
                    else if (List.class.isAssignableFrom(iteratorField.getType())) {
                        Type genericType = iteratorField.getGenericType();
                        if (genericType instanceof ParameterizedType) {
                            ParameterizedType type = (ParameterizedType) genericType;
                            Type[] generics = type.getActualTypeArguments();
                            if (generics.length >= 1) {
                                Class elementClazz = (Class) generics[0];
                                parseableElements.add(new ListFieldParser(iteratorField, xPath, elementClazz));
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

    private abstract class FieldParser<T> {
        final Field field;
        final XPath xPath;

        public abstract void parseElement(T obj, Object context) throws JDOMException, IllegalAccessException, SAXXParserException;

        public FieldParser(Field field, XPath xPath) {
            this.field = field;
            this.xPath = xPath;
        }
    }

    private class EnumFieldParser extends FieldParser {
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

    private class ListFieldParser<T> extends FieldParser {
        private SAXXParser<T> parser;
        private FieldParser fieldParser;

        private class ObjectWrapper<T> {
            public T object;
        }

        public ListFieldParser(Field field, XPath xPath, Class<T> elementClazz) {
            super(field, xPath);
            try {
                fieldParser = primitiveFieldParserFactory.createFieldParser(ObjectWrapper.class.getField("object"), XPath.newInstance("."), elementClazz);
            } catch (NoSuchFieldException e) {
                throw new SAXXParserException("Could not create List parser", e);
            } catch (JDOMException e) {
                throw new SAXXParserException("Could not create new XPath for List parser", e);
            }
            if (fieldParser == null) {
                parser = SAXXParserFactory.this.createXmlParser(elementClazz);
            }
        }

        @Override
        public void parseElement(Object obj, Object doc) throws JDOMException, IllegalAccessException, SAXXParserException {
            ObjectWrapper<T> wrapper = new ObjectWrapper<T>();
            List<T> objList = (List<T>) field.get(obj);
            if (objList == null) {
                objList = new ArrayList<T>();
                field.set(obj, objList);
            }
            List<Element> list = xPath.selectNodes(doc);
            for (Element element : list) {
                if (fieldParser != null) {
                    fieldParser.parseElement(wrapper, element);
                    objList.add(wrapper.object);
                } else if (parser != null) {
                    T returnObj = parser.parse(element);
                    objList.add(returnObj);
                }
            }
        }
    }

    private class PrimitiveFieldParserFactory {
        private final HashMap<Class, Class<FieldParser>> parserMap;

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

        private abstract class PrimitiveFieldParser<T> extends FieldParser {
            PrimitiveFieldParser(Field field, XPath xPath) {
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
                    } else {
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
                    } else {
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
                    } else {
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
                    } else {
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
                    } else {
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
                    } else {
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
                    } else {
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
                    } else {
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

    private static class SAXXParserImpl<T> implements SAXXParser<T> {
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
            T returnObject;
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
