/*
 * Copyright 2005 the original author or authors.
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
package org.springframework.oxm.castor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.Properties;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.ResolverException;
import org.exolab.castor.xml.UnmarshalHandler;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.XMLContext;
import org.exolab.castor.xml.XMLException;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.oxm.AbstractMarshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.xml.dom.DomContentHandler;
import org.springframework.xml.sax.SaxUtils;
import org.springframework.xml.stream.StaxEventContentHandler;
import org.springframework.xml.stream.StaxEventXmlReader;
import org.springframework.xml.stream.StaxStreamContentHandler;
import org.springframework.xml.stream.StaxStreamXmlReader;

/**
 * Implementation of the <code>Marshaller</code> interface for Castor. By default, Castor does not require any further
 * configuration, though setting a target class or providing a mapping file can be used to have more control over the
 * behavior of Castor.
 * <p/>
 * If a target class is specified using <code>setTargetClass</code>, the <code>CastorMarshaller</code> can only be used
 * to unmarshall XML that represents that specific class. If you want to unmarshall multiple classes, you have to
 * provide a mapping file using <code>setMappingLocations</code>.
 * <p/>
 * Due to Castor's API, it is required to set the encoding used for writing to output streams. It defaults to
 * <code>UTF-8</code>.
 *
 * @author Arjen Poutsma
 * @see #setEncoding(String)
 * @see #setTargetClass(Class)
 * @see #setMappingLocation(org.springframework.core.io.Resource)
 * @see #setMappingLocations(org.springframework.core.io.Resource[])
 * @since 1.0.0
 */
public class CastorMarshaller extends AbstractMarshaller implements InitializingBean {

    /** The default encoding used for stream access. */
    public static final String DEFAULT_ENCODING = "UTF-8";

    private Resource[] mappingLocations;

    private String encoding = DEFAULT_ENCODING;

    private Class targetClass;

    private XMLContext xmlContext;

    private boolean validating = false;

    private boolean whitespacePreserve = false;

    private boolean ignoreExtraAttributes = true;

    private boolean ignoreExtraElements = false;

    private Properties namespaceMappings;

    /** Returns whether the Castor  {@link Unmarshaller} should ignore attributes that do not match a specific field. */
    public boolean getIgnoreExtraAttributes() {
        return ignoreExtraAttributes;
    }

    /**
     * Sets whether the Castor  {@link Unmarshaller} should ignore attributes that do not match a specific field.
     * Default is <code>true</code>: extra attributes are ignored.
     *
     * @see org.exolab.castor.xml.Unmarshaller#setIgnoreExtraAttributes(boolean)
     */
    public void setIgnoreExtraAttributes(boolean ignoreExtraAttributes) {
        this.ignoreExtraAttributes = ignoreExtraAttributes;
    }

    /** Returns whether the Castor  {@link Unmarshaller} should ignore elements that do not match a specific field. */
    public boolean getIgnoreExtraElements() {
        return ignoreExtraElements;
    }

    /**
     * Sets whether the Castor  {@link Unmarshaller} should ignore elements that do not match a specific field. Default
     * is <code>false</code>, extra attributes are flagged as an error.
     *
     * @see org.exolab.castor.xml.Unmarshaller#setIgnoreExtraElements(boolean)
     */
    public void setIgnoreExtraElements(boolean ignoreExtraElements) {
        this.ignoreExtraElements = ignoreExtraElements;
    }

    /** Returns whether the Castor {@link Unmarshaller} should preserve "ignorable" whitespace. */
    public boolean getWhitespacePreserve() {
        return whitespacePreserve;
    }

    /**
     * Sets whether the Castor {@link Unmarshaller} should preserve "ignorable" whitespace. Default is
     * <code>false</code>.
     *
     * @see org.exolab.castor.xml.Unmarshaller#setWhitespacePreserve(boolean)
     */
    public void setWhitespacePreserve(boolean whitespacePreserve) {
        this.whitespacePreserve = whitespacePreserve;
    }

    /** Returns whether this marshaller should validate in- and outgoing documents. */
    public boolean isValidating() {
        return validating;
    }

    /**
     * Sets whether this marshaller should validate in- and outgoing documents. Default is <code>false</code>.
     *
     * @see Marshaller#setValidation(boolean)
     */
    public void setValidating(boolean validating) {
        this.validating = validating;
    }

    /** Returns the namespace mappings. Property names are interpreted as namespace prefixes; values are namespace URIs. */
    public Properties getNamespaceMappings() {
        return namespaceMappings;
    }

    /**
     * Sets the namespace mappings. Property names are interpreted as namespace prefixes; values are namespace URIs.
     *
     * @see org.exolab.castor.xml.Marshaller#setNamespaceMapping(String, String)
     */
    public void setNamespaceMappings(Properties namespaceMappings) {
        this.namespaceMappings = namespaceMappings;
    }

    /**
     * Sets the encoding to be used for stream access. If this property is not set, the default encoding is used.
     *
     * @see #DEFAULT_ENCODING
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /** Sets the locations of the Castor XML Mapping files. */
    public void setMappingLocation(Resource mappingLocation) {
        mappingLocations = new Resource[]{mappingLocation};
    }

    /** Sets the locations of the Castor XML Mapping files. */
    public void setMappingLocations(Resource[] mappingLocations) {
        this.mappingLocations = mappingLocations;
    }

    /**
     * Sets the Castor target class. If this property is set, this <code>CastorMarshaller</code> is tied to this one
     * specific class. Use a mapping file for unmarshalling multiple classes.
     * <p/>
     * You cannot set both this property and the mapping (location).
     */
    public void setTargetClass(Class targetClass) {
        this.targetClass = targetClass;
    }

    public final void afterPropertiesSet() throws IOException {
        if (mappingLocations != null && targetClass != null) {
            throw new IllegalArgumentException("Cannot set both the 'mappingLocations' and 'targetClass' property. " +
                    "Set targetClass for unmarshalling a single class, and 'mappingLocations' for multiple classes'");
        }
        if (logger.isInfoEnabled()) {
            if (mappingLocations != null) {
                logger.info("Configured using " + StringUtils.arrayToCommaDelimitedString(mappingLocations));
            }
            else if (targetClass != null) {
                logger.info("Configured for target class [" + targetClass.getName() + "]");
            }
            else {
                logger.info("Using default configuration");
            }
        }
        try {
            xmlContext = createXMLContext(mappingLocations, targetClass);
        }
        catch (MappingException ex) {
            throw new CastorSystemException("Could not load Castor mapping: " + ex.getMessage(), ex);
        }
        catch (ResolverException rex) {
            throw new CastorSystemException("Could not load Castor mapping: " + rex.getMessage(), rex);
        }
    }

    /** Returns <code>true</code> for all classes, i.e. Castor supports arbitrary classes. */
    public boolean supports(Class clazz) {
        return true;
    }

    /**
     * Creates the Castor <code>XMLContext</code>. Subclasses can override this to create a custom context.
     * <p/>
     * The default implementation loads mapping files if defined, and the target class if not defined.
     *
     * @return the created resolver
     * @throws MappingException when the mapping file cannot be loaded
     * @throws IOException      in case of I/O errors
     * @see XMLContext#addMapping(org.exolab.castor.mapping.Mapping)
     * @see XMLContext#addClass(Class)
     */
    protected XMLContext createXMLContext(Resource[] mappingLocations, Class targetClass)
            throws MappingException, IOException, ResolverException {
        XMLContext context = new XMLContext();
        if (!ObjectUtils.isEmpty(mappingLocations)) {
            Mapping mapping = new Mapping();
            for (int i = 0; i < mappingLocations.length; i++) {
                mapping.loadMapping(SaxUtils.createInputSource(mappingLocations[i]));
            }
            context.addMapping(mapping);
        }
        if (targetClass != null) {
            context.addClass(targetClass);
        }
        return context;
    }

    //
    // Marshalling
    //

    protected final void marshalDomNode(Object graph, Node node) throws XmlMappingException {
        marshalSaxHandlers(graph, new DomContentHandler(node), null);
    }

    protected final void marshalSaxHandlers(Object graph, ContentHandler contentHandler, LexicalHandler lexicalHandler)
            throws XmlMappingException {
        Marshaller marshaller = xmlContext.createMarshaller();
        marshaller.setContentHandler(contentHandler);
        marshal(graph, marshaller);
    }

    protected final void marshalOutputStream(Object graph, OutputStream outputStream)
            throws XmlMappingException, IOException {
        marshalWriter(graph, new OutputStreamWriter(outputStream, encoding));
    }

    protected final void marshalWriter(Object graph, Writer writer) throws XmlMappingException, IOException {
        Marshaller marshaller = xmlContext.createMarshaller();
        marshaller.setWriter(writer);
        marshal(graph, marshaller);
    }

    protected final void marshalXmlEventWriter(Object graph, XMLEventWriter eventWriter) throws XmlMappingException {
        marshalSaxHandlers(graph, new StaxEventContentHandler(eventWriter), null);
    }

    protected final void marshalXmlStreamWriter(Object graph, XMLStreamWriter streamWriter) throws XmlMappingException {
        marshalSaxHandlers(graph, new StaxStreamContentHandler(streamWriter), null);
    }

    private void marshal(Object graph, Marshaller marshaller) {
        try {
            customizeMarshaller(marshaller);
            marshaller.marshal(graph);
        }
        catch (XMLException ex) {
            throw convertCastorException(ex, true);
        }
    }

    /**
     * Template method that allows for customizing of the given Castor {@link Marshaller}.
     * <p/>
     * Default implementation invokes {@link Marshaller#setValidation(boolean)} with the property set on this
     * marshaller, and calls {@link Marshaller#setNamespaceMapping(String, String)} with the {@linkplain
     * #setNamespaceMappings(java.util.Properties) namespace mappings}.
     */
    protected void customizeMarshaller(Marshaller marshaller) {
        marshaller.setValidation(isValidating());
        Properties namespaceMappings = getNamespaceMappings();
        if (namespaceMappings != null) {
            for (Iterator iterator = namespaceMappings.keySet().iterator(); iterator.hasNext();) {
                String prefix = (String) iterator.next();
                String uri = namespaceMappings.getProperty(prefix);
                marshaller.setNamespaceMapping(prefix, uri);
            }
        }
    }

    //
    // Unmarshalling
    //

    protected final Object unmarshalDomNode(Node node) throws XmlMappingException {
        try {
            return createUnmarshaller().unmarshal(node);
        }
        catch (XMLException ex) {
            throw convertCastorException(ex, false);
        }
    }

    protected final Object unmarshalInputStream(InputStream inputStream) throws XmlMappingException, IOException {
        try {
            return createUnmarshaller().unmarshal(new InputSource(inputStream));
        }
        catch (XMLException ex) {
            throw convertCastorException(ex, false);
        }
    }

    protected final Object unmarshalReader(Reader reader) throws XmlMappingException, IOException {
        try {
            return createUnmarshaller().unmarshal(new InputSource(reader));
        }
        catch (XMLException ex) {
            throw convertCastorException(ex, false);
        }
    }

    protected final Object unmarshalXmlEventReader(XMLEventReader eventReader) {
        XMLReader reader = new StaxEventXmlReader(eventReader);
        try {
            return unmarshalSaxReader(reader, new InputSource());
        }
        catch (IOException ex) {
            throw new CastorUnmarshallingFailureException(new MarshalException(ex));
        }
    }

    protected final Object unmarshalSaxReader(XMLReader xmlReader, InputSource inputSource)
            throws XmlMappingException, IOException {
        UnmarshalHandler unmarshalHandler = createUnmarshaller().createHandler();
        try {
            ContentHandler contentHandler = Unmarshaller.getContentHandler(unmarshalHandler);
            xmlReader.setContentHandler(contentHandler);
            xmlReader.parse(inputSource);
            return unmarshalHandler.getObject();
        }
        catch (SAXException ex) {
            throw new CastorUnmarshallingFailureException(ex);
        }
    }

    protected final Object unmarshalXmlStreamReader(XMLStreamReader streamReader) {
        XMLReader reader = new StaxStreamXmlReader(streamReader);
        try {
            return unmarshalSaxReader(reader, new InputSource());
        }
        catch (IOException ex) {
            throw new CastorUnmarshallingFailureException(new MarshalException(ex));
        }
    }

    private Unmarshaller createUnmarshaller() {
        Unmarshaller unmarshaller = xmlContext.createUnmarshaller();
        if (targetClass != null) {
            unmarshaller.setClass(targetClass);
            unmarshaller.setClassLoader(targetClass.getClassLoader());
        }
        customizeUnmarshaller(unmarshaller);
        return unmarshaller;
    }

    /**
     * Template method that allows for customizing of the given Castor {@link Unmarshaller}.
     * <p/>
     * Default implementation invokes {@link Unmarshaller#setValidation(boolean)}, {@link
     * Unmarshaller#setWhitespacePreserve(boolean)}, {@link Unmarshaller#setIgnoreExtraAttributes(boolean)}, and {@link
     * Unmarshaller#setIgnoreExtraElements(boolean)} with the properties set on this marshaller.
     */
    protected void customizeUnmarshaller(Unmarshaller unmarshaller) {
        unmarshaller.setValidation(isValidating());
        unmarshaller.setWhitespacePreserve(getWhitespacePreserve());
        unmarshaller.setIgnoreExtraAttributes(getIgnoreExtraAttributes());
        unmarshaller.setIgnoreExtraElements(getIgnoreExtraElements());
    }

    /**
     * Converts the given <code>CastorException</code> to an appropriate exception from the
     * <code>org.springframework.oxm</code> hierarchy.
     * <p/>
     * The default implementation delegates to <code>CastorUtils</code>. Can be overridden in subclasses.
     * <p/>
     * A boolean flag is used to indicate whether this exception occurs during marshalling or unmarshalling, since
     * Castor itself does not make this distinction in its exception hierarchy.
     *
     * @param ex          Castor <code>XMLException</code> that occured
     * @param marshalling indicates whether the exception occurs during marshalling (<code>true</code>), or
     *                    unmarshalling (<code>false</code>)
     * @return the corresponding <code>XmlMappingException</code>
     * @see CastorUtils#convertXmlException
     */
    public XmlMappingException convertCastorException(XMLException ex, boolean marshalling) {
        return CastorUtils.convertXmlException(ex, marshalling);
    }
}
