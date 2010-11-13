package com.github.smat.saxxp;

import org.jdom.Element;

import java.io.InputStream;

/**
 *
 * @param <T> Class to be parsed from XML
 */
public interface Parser<T> {
    /** Parses a XML document
     *
     * @param xml XML document
     * @throws com.github.smat.saxxp.exception.SaxxpException
     * @return
     */
    public T parse(String xml);
    /** Parses a XML document
     *
     * @param stream XML document as InputStream
     * @throws com.github.smat.saxxp.exception.SaxxpException
     * @return
     */
    public T parse(InputStream stream);
    /** Parses a XML document
     *
     * @param element Element in a XML document
     * @throws com.github.smat.saxxp.exception.SaxxpException
     * @return
     */
    public T parse(Element element);
}