package com.github.smat.saxxp;

import com.github.smat.saxxp.annotation.XPath;
import com.github.smat.saxxp.annotation.XmlEnumIdentifier;
import com.github.smat.saxxp.exception.SaxxpException;
import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

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
public class ParserFactory {
    private final PrimitiveFieldParserFactory primitiveFieldParserFactory;

    public ParserFactory() {
        primitiveFieldParserFactory = new PrimitiveFieldParserFactory();
    }

    /**
     * Creates a new parser
     * @param clazz The class to be parsed
     * @param <T> The class to be parsed
     * @return SAXXParser
     */
    public <T> Parser<T> createXmlParser(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Could not create parser for null class");
        }
        final List<FieldParser> parseableElements = new ArrayList<FieldParser>();

        for (Field iteratorField : clazz.getDeclaredFields()) {
            if (iteratorField.isAnnotationPresent(XPath.class)) {
                try {
                    final XPath annotation = iteratorField.getAnnotation(XPath.class);
                    final org.jdom.xpath.XPath xPath = org.jdom.xpath.XPath.newInstance(annotation.value());
                    iteratorField.setAccessible(true);

                    FieldParser fieldParser = primitiveFieldParserFactory.createFieldParser(iteratorField, xPath);
                    if (fieldParser != null) {
                        parseableElements.add(fieldParser);
                    }
                    else if (iteratorField.getType().isEnum()) {
                        Field identifierField = null;
                        for (Field enumField : iteratorField.getType().getDeclaredFields()) {
                            enumField.setAccessible(true);
                            if (enumField.isAnnotationPresent(XmlEnumIdentifier.class)) {
                                identifierField = enumField;
                            }
                        }
                        if (identifierField == null) {
                            throw new IllegalArgumentException("Enum does not contains @XmlEnumIdentifier annotation");
                        }
                        final Field enumIdentifier = identifierField;
                        parseableElements.add(new EnumFieldParser(iteratorField, xPath, enumIdentifier));
                    }
                    else if (iteratorField.getType().isArray()) {
                        parseableElements.add(new ArrayFieldParser(iteratorField, xPath, iteratorField.getType().getComponentType()));
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

        return new ParserImpl<T>(clazz, parseableElements);
    }

    public <T> Parser<List<T>> createXmlListParser(Class<T> clazz, String XPathForList) {
        return null;
    }

    private abstract class FieldParser<T> {
        final Field field;
        final org.jdom.xpath.XPath xPath;

        public abstract void parseElement(T obj, Object context) throws JDOMException, IllegalAccessException, SaxxpException;

        public FieldParser(Field field, org.jdom.xpath.XPath xPath) {
            this.field = field;
            this.xPath = xPath;
        }
    }

    private class EnumFieldParser extends FieldParser {
        private final Field enumIdentifier;

        public EnumFieldParser(Field field, org.jdom.xpath.XPath xPath, Field enumIdentifier) {
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

    private abstract class CollectionsFieldParser<T> extends FieldParser {
        protected Parser<T> parser;
        protected FieldParser fieldParser;
        protected final Class<T> elementClazz;

        protected class ObjectWrapper<T> {
            public T object;
        }

        public CollectionsFieldParser(Field field, org.jdom.xpath.XPath xPath, Class<T> elementClazz) {
            super(field, xPath);
            this.elementClazz = elementClazz;
            try {
                fieldParser = primitiveFieldParserFactory.createFieldParser(ObjectWrapper.class.getField("object"), org.jdom.xpath.XPath.newInstance("."), elementClazz);
            } catch (NoSuchFieldException e) {
                throw new SaxxpException("Could not create List parser", e);
            } catch (JDOMException e) {
                throw new SaxxpException("Could not create new XPath for List parser", e);
            }
            if (fieldParser == null) {
                parser = ParserFactory.this.createXmlParser(elementClazz);
            }
        }
    }

    private class ListFieldParser<T> extends CollectionsFieldParser<T> {
        public ListFieldParser(Field field, org.jdom.xpath.XPath xPath, Class<T> elementClazz) {
            super(field, xPath, elementClazz);
        }

        @Override
        public void parseElement(Object obj, Object doc) throws JDOMException, IllegalAccessException, SaxxpException {
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

    private class ArrayFieldParser<T> extends CollectionsFieldParser<T> {
        public ArrayFieldParser(Field field, org.jdom.xpath.XPath xPath, Class elementClazz) {
            super(field, xPath, elementClazz);
        }

        @Override
        public void parseElement(Object obj, Object doc) throws JDOMException, IllegalAccessException, SaxxpException {
            ObjectWrapper<T> wrapper = new ObjectWrapper<T>();
            List<Element> list = xPath.selectNodes(doc);
            T[] objArray = (T[]) Array.newInstance(elementClazz, list.size());
            for (int i = 0; i <objArray.length; i++) {
                if (fieldParser != null) {
                    fieldParser.parseElement(wrapper, list.get(i));
                    objArray[i] = wrapper.object;
                }
                else {
                    T returnObj = parser.parse(list.get(i));
                    objArray[i] = returnObj;
                }
            }
            field.set(obj, objArray);
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

        public FieldParser createFieldParser(Field field, org.jdom.xpath.XPath xPath) {
            return createFieldParser(field, xPath, field.getType());
        }

        public FieldParser createFieldParser(Field field, org.jdom.xpath.XPath xPath, Class clazz) {
            Class<FieldParser> fieldParserClass = parserMap.get(clazz);
            if (fieldParserClass == null) {
                return null;
            }
            try {
                Constructor<FieldParser> parserConstructor = fieldParserClass.getDeclaredConstructor(PrimitiveFieldParserFactory.class, Field.class, org.jdom.xpath.XPath.class);
                return parserConstructor.newInstance(this, field, xPath);
            } catch (NoSuchMethodException e) {
                throw new SaxxpException("Could not find constructor for " + fieldParserClass, e);
            } catch (Exception e) {
                throw new SaxxpException("Could not create new parser for " + field.getType(), e);
            }
        }

        private abstract class PrimitiveFieldParser<T> extends FieldParser {
            PrimitiveFieldParser(Field field, org.jdom.xpath.XPath xPath) {
                super(field, xPath);
            }
        }

        private class IntegerFieldParser extends PrimitiveFieldParser<Integer> {
            public IntegerFieldParser(Field field, org.jdom.xpath.XPath xPath) {
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
            public FloatFieldParser(Field field, org.jdom.xpath.XPath xPath) {
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
            public DoubleFieldParser(Field field, org.jdom.xpath.XPath xPath) {
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
            public ByteFieldParser(Field field, org.jdom.xpath.XPath xPath) {
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
            public ShortFieldParser(Field field, org.jdom.xpath.XPath xPath) {
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
            public LongFieldParser(Field field, org.jdom.xpath.XPath xPath) {
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
            public CharFieldParser(Field field, org.jdom.xpath.XPath xPath) {
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
            public BooleanFieldParser(Field field, org.jdom.xpath.XPath xPath) {
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
            public StringFieldParser(Field field, org.jdom.xpath.XPath xPath) {
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

    private static class ParserImpl<T> implements Parser<T> {
        private final Class<T> clazz;
        private final List<FieldParser> parseableElements;
        private Constructor constructor;

        public ParserImpl(Class<T> clazz, List<FieldParser> parseableElements) {
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
                throw new SaxxpException("Could not parse XML using XPath", e);
            } catch (IllegalAccessException e) {
                throw new SaxxpException("Could not access field in object", e);
            } catch (InstantiationException e) {
                throw new SaxxpException("Could not create new instance of object", e);
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
                throw new SaxxpException("Could not parse input XML", e);
            } catch (IOException e) {
                throw new SaxxpException("Could not read XML stream", e);
            }
        }
    }
}
