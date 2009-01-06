/*
 * Copyright 2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.oxm.xstream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterMatcher;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import com.thoughtworks.xstream.io.xml.DomReader;
import com.thoughtworks.xstream.io.xml.DomWriter;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.SaxWriter;
import com.thoughtworks.xstream.io.xml.StaxReader;
import com.thoughtworks.xstream.io.xml.StaxWriter;
import com.thoughtworks.xstream.io.xml.XmlFriendlyReplacer;
import com.thoughtworks.xstream.io.xml.XppReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import org.springframework.beans.propertyeditors.ClassEditor;
import org.springframework.oxm.AbstractMarshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.xml.stream.StaxEventContentHandler;
import org.springframework.xml.stream.XmlEventStreamReader;

/**
 * Implementation of the <code>Marshaller</code> interface for XStream. By default, XStream does not require any further
 * configuration, though class aliases can be used to have more control over the behavior of XStream.
 * <p/>
 * Due to XStream's API, it is required to set the encoding used for writing to outputstreams. It defaults to
 * <code>UTF-8</code>.
 * <p/>
 * <b>Note</b> that XStream is an XML serialization library, not a data binding library. Therefore, it has limited
 * namespace support. As such, it is rather unsuitable for usage within Web services.
 *
 * @author Peter Meijer
 * @author Arjen Poutsma
 * @see #setEncoding(String)
 * @see #DEFAULT_ENCODING
 * @see #setAliases(Map)
 * @see #setConverters(ConverterMatcher[])
 * @since 1.0.0
 */
public class XStreamMarshaller extends AbstractMarshaller {

    /** The default encoding used for stream access. */
    public static final String DEFAULT_ENCODING = "UTF-8";

    private XStream xstream = new XStream();

    private String encoding;

    private Class[] supportedClasses;

    /** Specialized driver to be used with stream readers and writers */
    private HierarchicalStreamDriver streamDriver;

    /**
     * Returns the encoding to be used for stream access. If this property is not set, the default encoding is used.
     *
     * @see #DEFAULT_ENCODING
     */
    public String getEncoding() {
        return encoding != null ? encoding : DEFAULT_ENCODING;
    }

    /**
     * Sets the encoding to be used for stream access. If this property is not set, the default encoding is used.
     *
     * @see #DEFAULT_ENCODING
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /** Returns the XStream instance used by this marshaller. */
    public XStream getXStream() {
        return xstream;
    }

    /**
     * Sets the XStream mode.
     *
     * @see XStream#XPATH_REFERENCES
     * @see XStream#ID_REFERENCES
     * @see XStream#NO_REFERENCES
     */
    public void setMode(int mode) {
        getXStream().setMode(mode);
    }

    /**
     * Sets the classes supported by this marshaller. If this property is empty (the default), all classes are
     * supported.
     *
     * @see #supports(Class)
     */
    public void setSupportedClasses(Class[] supportedClasses) {
        this.supportedClasses = supportedClasses;
    }

    /**
     * Sets the <code>Converters</code> or <code>SingleValueConverters</code> to be registered with the
     * <code>XStream</code> instance.
     *
     * @see Converter
     * @see SingleValueConverter
     */
    public void setConverters(ConverterMatcher[] converters) {
        for (int i = 0; i < converters.length; i++) {
            if (converters[i] instanceof Converter) {
                getXStream().registerConverter((Converter) converters[i], i);
            }
            else if (converters[i] instanceof SingleValueConverter) {
                getXStream().registerConverter((SingleValueConverter) converters[i], i);
            }
            else {
                throw new IllegalArgumentException("Invalid ConverterMatcher [" + converters[i] + "]");
            }
        }
    }

    /** Sets the XStream hierarchical stream driver to be used with stream readers and writers */
    public void setStreamDriver(HierarchicalStreamDriver streamDriver) {
        this.streamDriver = streamDriver;
    }

    /**
     * Set a alias/type map, consisting of string aliases mapped to <code>Class</code> instances (or Strings to be
     * converted to <code>Class</code> instances).
     *
     * @see org.springframework.beans.propertyeditors.ClassEditor
     */
    public void setAliases(Map aliases) {
        for (Iterator iterator = aliases.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            // Check whether we need to convert from String to Class.
            Class type;
            if (entry.getValue() instanceof Class) {
                type = (Class) entry.getValue();
            }
            else {
                ClassEditor editor = new ClassEditor();
                editor.setAsText(String.valueOf(entry.getValue()));
                type = (Class) editor.getValue();
            }
            addAlias((String) entry.getKey(), type);
        }
    }

    /**
     * Adds an alias for the given type.
     *
     * @param name alias to be used for the type
     * @param type the type to be aliased
     */
    public void addAlias(String name, Class type) {
        getXStream().alias(name, type);
    }

    /**
     * Sets types to use XML attributes for.
     *
     * @see XStream#useAttributeFor(Class)
     */
    public void setUseAttributeForTypes(Class[] types) {
        for (int i = 0; i < types.length; i++) {
            getXStream().useAttributeFor(types[i]);
        }
    }

    /**
     * Sets the types to use XML attributes for. The given map can contain either <code>&lt;String, Class&gt;</code>
     * pairs, in which case {@link XStream#useAttributeFor(String,Class)} is called, or <code>&lt;Class,
     * String&gt;</code> pairs, which results in {@link XStream#useAttributeFor(Class,String)}.
     */
    public void setUseAttributeFor(Map attributes) {
        for (Iterator iterator = attributes.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            if (entry.getKey() instanceof String && entry.getValue() instanceof Class) {
                getXStream().useAttributeFor((String) entry.getKey(), (Class) entry.getValue());
            }
            else if (entry.getKey() instanceof Class && entry.getValue() instanceof String) {
                getXStream().useAttributeFor((Class) entry.getKey(), (String) entry.getValue());
            }
            else {
                throw new IllegalArgumentException("Invalid attribute key and value pair. " +
                        "'useAttributesFor' property takes either a <String, Class> map or a <Class, String> map");
            }
        }
    }

    /**
     * Adds an implicit Collection for the given type.
     *
     * @see XStream#addImplicitCollection(Class, String)
     */
    public void addImplicitCollection(String name, Class type) {
        getXStream().addImplicitCollection(type, name);
    }

    /**
     * Set a implicit colletion/type map, consisting of string implicit collection mapped to <code>Class</code>
     * instances (or Strings to be converted to <code>Class</code> instances).
     *
     * @see XStream#addImplicitCollection(Class, String)
     */
    public void setImplicitCollection(Map implicitCollection) {
        for (Iterator iterator = implicitCollection.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            // Check whether we need to convert from String to Class.
            Class type;
            if (entry.getValue() instanceof Class) {
                type = (Class) entry.getValue();
            }
            else {
                ClassEditor editor = new ClassEditor();
                editor.setAsText(String.valueOf(entry.getValue()));
                type = (Class) editor.getValue();
            }
            addImplicitCollection((String) entry.getKey(), type);
        }
    }

    /**
     * Adds an omitted field for the given type.
     *
     * @param type      the type to be containing the field
     * @param fieldName field to omitt
     * @see XStream#omitField(Class, String)
     */
    public void addOmittedField(Class type, String fieldName) {
        getXStream().omitField(type, fieldName);
    }

    /**
     * Sets a ommited field map, consisting of <code>Class</code> instances (or Strings to be converted to
     * <code>Class</code> instances) mapped to comma separated field names.
     *
     * @see XStream#omitField(Class, String)
     */
    public void setOmittedFields(Map omittedFields) {
        for (Iterator iterator = omittedFields.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            // Check whether we need to convert from String to Class.
            Class type;
            if (entry.getKey() instanceof Class) {
                type = (Class) entry.getKey();
            }
            else {
                ClassEditor editor = new ClassEditor();
                editor.setAsText(String.valueOf(entry.getKey()));
                type = (Class) editor.getValue();
            }
            // add each omitted field for the current type
            String fieldsString = (String) entry.getValue();
            String[] fields = StringUtils.commaDelimitedListToStringArray(fieldsString);
            for (int i = 0; i < fields.length; i++) {
                addOmittedField(type, fields[i]);
            }
        }
    }

    public boolean supports(Class clazz) {
        if (ObjectUtils.isEmpty(supportedClasses)) {
            return true;
        }
        else {
            for (int i = 0; i < supportedClasses.length; i++) {
                if (supportedClasses[i].isAssignableFrom(clazz)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Convert the given XStream exception to an appropriate exception from the <code>org.springframework.oxm</code>
     * hierarchy.
     * <p/>
     * The default implementation delegates to <code>XStreamUtils</code>. Can be overridden in subclasses.
     *
     * @param ex          exception that occured
     * @param marshalling indicates whether the exception occurs during marshalling (<code>true</code>), or
     *                    unmarshalling (<code>false</code>)
     * @return the corresponding <code>XmlMappingException</code> instance
     * @see XStreamUtils#convertXStreamException(Exception,boolean)
     */
    public XmlMappingException convertXStreamException(Exception ex, boolean marshalling) {
        return XStreamUtils.convertXStreamException(ex, marshalling);
    }

    //
    // Marshalling
    //

    /**
     * Marshals the given graph to the given XStream HierarchicalStreamWriter. Converts exceptions using
     * <code>convertXStreamException</code>.
     */
    private void marshal(Object graph, HierarchicalStreamWriter streamWriter) {
        try {
            getXStream().marshal(graph, streamWriter);
        }
        catch (Exception ex) {
            throw convertXStreamException(ex, true);
        }
    }

    protected void marshalDomNode(Object graph, Node node) throws XmlMappingException {
        HierarchicalStreamWriter streamWriter;
        if (node instanceof Document) {
            streamWriter = new DomWriter((Document) node);
        }
        else if (node instanceof Element) {
            streamWriter = new DomWriter((Element) node, node.getOwnerDocument(), new XmlFriendlyReplacer());
        }
        else {
            throw new IllegalArgumentException("DOMResult contains neither Document nor Element");
        }
        marshal(graph, streamWriter);
    }

    protected void marshalXmlEventWriter(Object graph, XMLEventWriter eventWriter) throws XmlMappingException {
        ContentHandler contentHandler = new StaxEventContentHandler(eventWriter);
        marshalSaxHandlers(graph, contentHandler, null);
    }

    protected void marshalXmlStreamWriter(Object graph, XMLStreamWriter streamWriter) throws XmlMappingException {
        try {
            marshal(graph, new StaxWriter(new QNameMap(), streamWriter));
        }
        catch (XMLStreamException ex) {
            throw convertXStreamException(ex, true);
        }
    }

    protected void marshalOutputStream(Object graph, OutputStream outputStream)
            throws XmlMappingException, IOException {
        marshalWriter(graph, new OutputStreamWriter(outputStream, getEncoding()));
    }

    protected void marshalSaxHandlers(Object graph, ContentHandler contentHandler, LexicalHandler lexicalHandler)
            throws XmlMappingException {
        SaxWriter saxWriter = new SaxWriter();
        saxWriter.setContentHandler(contentHandler);
        marshal(graph, saxWriter);
    }

    protected void marshalWriter(Object graph, Writer writer) throws XmlMappingException, IOException {
        if (streamDriver != null) {
            marshal(graph, streamDriver.createWriter(writer));
        }
        else {
            marshal(graph, new CompactWriter(writer));
        }
    }

    //
    // Unmarshalling
    //

    private Object unmarshal(HierarchicalStreamReader streamReader) {
        try {
            return getXStream().unmarshal(streamReader);
        }
        catch (Exception ex) {
            throw convertXStreamException(ex, false);
        }
    }

    protected Object unmarshalDomNode(Node node) throws XmlMappingException {
        HierarchicalStreamReader streamReader;
        if (node instanceof Document) {
            streamReader = new DomReader((Document) node);
        }
        else if (node instanceof Element) {
            streamReader = new DomReader((Element) node);
        }
        else {
            throw new IllegalArgumentException("DOMSource contains neither Document nor Element");
        }
        return unmarshal(streamReader);
    }

    protected Object unmarshalXmlEventReader(XMLEventReader eventReader) throws XmlMappingException {
        try {
            XMLStreamReader streamReader = new XmlEventStreamReader(eventReader);
            return unmarshalXmlStreamReader(streamReader);
        }
        catch (XMLStreamException ex) {
            throw convertXStreamException(ex, false);
        }
    }

    protected Object unmarshalXmlStreamReader(XMLStreamReader streamReader) throws XmlMappingException {
        return unmarshal(new StaxReader(new QNameMap(), streamReader));
    }

    protected Object unmarshalInputStream(InputStream inputStream) throws XmlMappingException, IOException {
        return unmarshalReader(new InputStreamReader(inputStream, getEncoding()));
    }

    protected Object unmarshalReader(Reader reader) throws XmlMappingException, IOException {
        if (streamDriver != null) {
            return unmarshal(streamDriver.createReader(reader));
        }
        else {
            return unmarshal(new XppReader(reader));
        }
    }

    protected Object unmarshalSaxReader(XMLReader xmlReader, InputSource inputSource)
            throws XmlMappingException, IOException {
        throw new UnsupportedOperationException(
                "XStreamMarshaller does not support unmarshalling using SAX XMLReaders");
    }


}
