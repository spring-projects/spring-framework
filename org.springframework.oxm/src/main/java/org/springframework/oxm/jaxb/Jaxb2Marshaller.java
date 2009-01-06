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

package org.springframework.oxm.jaxb;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.oxm.GenericMarshaller;
import org.springframework.oxm.GenericUnmarshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.mime.MimeContainer;
import org.springframework.oxm.mime.MimeMarshaller;
import org.springframework.oxm.mime.MimeUnmarshaller;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.xml.transform.TraxUtils;
import org.springframework.xml.validation.SchemaLoaderUtils;

/**
 * Implementation of the <code>Marshaller</code> interface for JAXB 2.0.
 * <p/>
 * The typical usage will be to set either the <code>contextPath</code> or the <code>classesToBeBound</code> property on
 * this bean, possibly customize the marshaller and unmarshaller by setting properties, schemas, adapters, and
 * listeners, and to refer to it.
 *
 * @author Arjen Poutsma
 * @see #setContextPath(String)
 * @see #setClassesToBeBound(Class[])
 * @see #setJaxbContextProperties(java.util.Map)
 * @see #setMarshallerProperties(java.util.Map)
 * @see #setUnmarshallerProperties(java.util.Map)
 * @see #setSchema(org.springframework.core.io.Resource)
 * @see #setSchemas(org.springframework.core.io.Resource[])
 * @see #setMarshallerListener(javax.xml.bind.Marshaller.Listener)
 * @see #setUnmarshallerListener(javax.xml.bind.Unmarshaller.Listener)
 * @see #setAdapters(javax.xml.bind.annotation.adapters.XmlAdapter[])
 * @since 1.0.0
 */
public class Jaxb2Marshaller extends AbstractJaxbMarshaller
        implements MimeMarshaller, MimeUnmarshaller, GenericMarshaller, GenericUnmarshaller, BeanClassLoaderAware {

    private ClassLoader classLoader;

    private Resource[] schemaResources;

    private String schemaLanguage = XMLConstants.W3C_XML_SCHEMA_NS_URI;

    private Marshaller.Listener marshallerListener;

    private Unmarshaller.Listener unmarshallerListener;

    private XmlAdapter[] adapters;

    private Schema schema;

    private Class[] classesToBeBound;

    private Map<String, ?> jaxbContextProperties;

    private boolean mtomEnabled = false;

    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Sets the <code>XmlAdapter</code>s to be registered with the JAXB <code>Marshaller</code> and
     * <code>Unmarshaller</code>
     */
    public void setAdapters(XmlAdapter[] adapters) {
        this.adapters = adapters;
    }

    /**
     * Sets the list of java classes to be recognized by a newly created JAXBContext. Setting this property or
     * <code>contextPath</code> is required.
     *
     * @see #setContextPath(String)
     */
    public void setClassesToBeBound(Class[] classesToBeBound) {
        this.classesToBeBound = classesToBeBound;
    }

    /**
     * Sets the <code>JAXBContext</code> properties. These implementation-specific properties will be set on the
     * <code>JAXBContext</code>.
     */
    public void setJaxbContextProperties(Map<String, ?> jaxbContextProperties) {
        this.jaxbContextProperties = jaxbContextProperties;
    }

    /** Sets the <code>Marshaller.Listener</code> to be registered with the JAXB <code>Marshaller</code>. */
    public void setMarshallerListener(Marshaller.Listener marshallerListener) {
        this.marshallerListener = marshallerListener;
    }

    /**
     * Indicates whether MTOM support should be enabled or not. Default is <code>false</code>, marshalling using
     * XOP/MTOM is not enabled.
     */
    public void setMtomEnabled(boolean mtomEnabled) {
        this.mtomEnabled = mtomEnabled;
    }

    /**
     * Sets the schema language. Default is the W3C XML Schema: <code>http://www.w3.org/2001/XMLSchema"</code>.
     *
     * @see XMLConstants#W3C_XML_SCHEMA_NS_URI
     * @see XMLConstants#RELAXNG_NS_URI
     */
    public void setSchemaLanguage(String schemaLanguage) {
        this.schemaLanguage = schemaLanguage;
    }

    /** Sets the schema resource to use for validation. */
    public void setSchema(Resource schemaResource) {
        schemaResources = new Resource[]{schemaResource};
    }

    /** Sets the schema resources to use for validation. */
    public void setSchemas(Resource[] schemaResources) {
        this.schemaResources = schemaResources;
    }

    /** Sets the <code>Unmarshaller.Listener</code> to be registered with the JAXB <code>Unmarshaller</code>. */
    public void setUnmarshallerListener(Unmarshaller.Listener unmarshallerListener) {
        this.unmarshallerListener = unmarshallerListener;
    }

    public boolean supports(Type type) {
        if (type instanceof Class) {
            return supportsInternal((Class) type, true);
        }
        else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (JAXBElement.class.equals(parameterizedType.getRawType())) {
                Assert.isTrue(parameterizedType.getActualTypeArguments().length == 1,
                        "Invalid amount of parameterized types in JAXBElement");
                Type typeArgument = parameterizedType.getActualTypeArguments()[0];
                if (typeArgument instanceof Class) {
                    Class clazz = (Class) typeArgument;
                    if (!isPrimitiveType(clazz) && !isStandardType(clazz) && !supportsInternal(clazz, false)) {
                        return false;
                    }
                }
                else if (typeArgument instanceof GenericArrayType) {
                    GenericArrayType genericArrayType = (GenericArrayType) typeArgument;
                    return genericArrayType.getGenericComponentType().equals(Byte.TYPE);
                }
                else if (!supports(typeArgument)) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    private boolean isPrimitiveType(Class clazz) {
        return (Boolean.class.equals(clazz) || Byte.class.equals(clazz) || Short.class.equals(clazz) ||
                Integer.class.equals(clazz) || Long.class.equals(clazz) || Float.class.equals(clazz) ||
                Double.class.equals(clazz) || byte[].class.equals(clazz));
    }

    private boolean isStandardType(Class clazz) {
        return (String.class.equals(clazz) || BigInteger.class.equals(clazz) || BigDecimal.class.equals(clazz) ||
                Calendar.class.isAssignableFrom(clazz) || Date.class.isAssignableFrom(clazz) ||
                QName.class.equals(clazz) || URI.class.equals(clazz) ||
                XMLGregorianCalendar.class.isAssignableFrom(clazz) || Duration.class.isAssignableFrom(clazz) ||
                Object.class.equals(clazz) || Image.class.isAssignableFrom(clazz) || DataHandler.class.equals(clazz) ||
                Source.class.isAssignableFrom(clazz) || UUID.class.equals(clazz));
    }

    public boolean supports(Class clazz) {
        return supportsInternal(clazz, true);
    }

    private boolean supportsInternal(Class<?> clazz, boolean checkForXmlRootElement) {
        if (checkForXmlRootElement && clazz.getAnnotation(XmlRootElement.class) == null) {
            return false;
        }
        if (clazz.getAnnotation(XmlType.class) == null) {
            return false;
        }
        if (StringUtils.hasLength(getContextPath())) {
            String className = ClassUtils.getQualifiedName(clazz);
            int lastDotIndex = className.lastIndexOf('.');
            if (lastDotIndex == -1) {
                return false;
            }
            String packageName = className.substring(0, lastDotIndex);
            String[] contextPaths = StringUtils.tokenizeToStringArray(getContextPath(), ":");
            for (int i = 0; i < contextPaths.length; i++) {
                if (contextPaths[i].equals(packageName)) {
                    return true;
                }
            }
            return false;
        }
        else if (!ObjectUtils.isEmpty(classesToBeBound)) {
            return Arrays.asList(classesToBeBound).contains(clazz);
        }
        return false;
    }

    /*
     * JAXBContext
     */

    protected JAXBContext createJaxbContext() throws Exception {
        if (JaxbUtils.getJaxbVersion() < JaxbUtils.JAXB_2) {
            throw new IllegalStateException(
                    "Cannot use Jaxb2Marshaller in combination with JAXB 1.0. Use Jaxb1Marshaller instead.");
        }
        if (StringUtils.hasLength(getContextPath()) && !ObjectUtils.isEmpty(classesToBeBound)) {
            throw new IllegalArgumentException("specify either contextPath or classesToBeBound property; not both");
        }
        if (!ObjectUtils.isEmpty(schemaResources)) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                        "Setting validation schema to " + StringUtils.arrayToCommaDelimitedString(schemaResources));
            }
            schema = SchemaLoaderUtils.loadSchema(schemaResources, schemaLanguage);
        }
        if (StringUtils.hasLength(getContextPath())) {
            return createJaxbContextFromContextPath();
        }
        else if (!ObjectUtils.isEmpty(classesToBeBound)) {
            return createJaxbContextFromClasses();
        }
        else {
            throw new IllegalArgumentException("setting either contextPath or classesToBeBound is required");
        }
    }

    private JAXBContext createJaxbContextFromContextPath() throws JAXBException {
        if (logger.isInfoEnabled()) {
            logger.info("Creating JAXBContext with context path [" + getContextPath() + "]");
        }
        if (jaxbContextProperties != null) {
            if (classLoader != null) {
                return JAXBContext
                        .newInstance(getContextPath(), classLoader, jaxbContextProperties);
            }
            else {
                return JAXBContext
                        .newInstance(getContextPath(), ClassUtils.getDefaultClassLoader(), jaxbContextProperties);
            }
        }
        else {
            return classLoader != null ? JAXBContext.newInstance(getContextPath(), classLoader) :
                    JAXBContext.newInstance(getContextPath());
        }
    }

    private JAXBContext createJaxbContextFromClasses() throws JAXBException {
        if (logger.isInfoEnabled()) {
            logger.info("Creating JAXBContext with classes to be bound [" +
                    StringUtils.arrayToCommaDelimitedString(classesToBeBound) + "]");
        }
        if (jaxbContextProperties != null) {
            return JAXBContext.newInstance(classesToBeBound, jaxbContextProperties);
        }
        else {
            return JAXBContext.newInstance(classesToBeBound);
        }
    }

    /*
     * Marshaller/Unmarshaller
     */

    protected void initJaxbMarshaller(Marshaller marshaller) throws JAXBException {
        if (schema != null) {
            marshaller.setSchema(schema);
        }
        if (marshallerListener != null) {
            marshaller.setListener(marshallerListener);
        }
        if (adapters != null) {
            for (int i = 0; i < adapters.length; i++) {
                marshaller.setAdapter(adapters[i]);
            }
        }
    }

    protected void initJaxbUnmarshaller(Unmarshaller unmarshaller) throws JAXBException {
        if (schema != null) {
            unmarshaller.setSchema(schema);
        }
        if (unmarshallerListener != null) {
            unmarshaller.setListener(unmarshallerListener);
        }
        if (adapters != null) {
            for (int i = 0; i < adapters.length; i++) {
                unmarshaller.setAdapter(adapters[i]);
            }
        }
    }

    /*
     * Marshalling
     */

    public void marshal(Object graph, Result result) throws XmlMappingException {
        marshal(graph, result, null);
    }

    public void marshal(Object graph, Result result, MimeContainer mimeContainer) throws XmlMappingException {
        try {
            Marshaller marshaller = createMarshaller();
            if (mtomEnabled && mimeContainer != null) {
                marshaller.setAttachmentMarshaller(new Jaxb2AttachmentMarshaller(mimeContainer));
            }
            if (TraxUtils.isStaxResult(result)) {
                marshalStaxResult(marshaller, graph, result);
            }
            else {
                marshaller.marshal(graph, result);
            }
        }
        catch (JAXBException ex) {
            throw convertJaxbException(ex);
        }
    }

    private void marshalStaxResult(Marshaller jaxbMarshaller, Object graph, Result staxResult) throws JAXBException {
        XMLStreamWriter streamWriter = TraxUtils.getXMLStreamWriter(staxResult);
        if (streamWriter != null) {
            jaxbMarshaller.marshal(graph, streamWriter);
        }
        else {
            XMLEventWriter eventWriter = TraxUtils.getXMLEventWriter(staxResult);
            if (eventWriter != null) {
                jaxbMarshaller.marshal(graph, eventWriter);
            }
            else {
                throw new IllegalArgumentException("StAX Result contains neither XMLStreamWriter nor XMLEventConsumer");
            }
        }
    }

    /*
     * Unmarshalling
     */

    public Object unmarshal(Source source) throws XmlMappingException {
        return unmarshal(source, null);
    }

    public Object unmarshal(Source source, MimeContainer mimeContainer) throws XmlMappingException {
        try {
            Unmarshaller unmarshaller = createUnmarshaller();
            if (mtomEnabled && mimeContainer != null) {
                unmarshaller.setAttachmentUnmarshaller(new Jaxb2AttachmentUnmarshaller(mimeContainer));
            }
            if (TraxUtils.isStaxSource(source)) {
                return unmarshalStaxSource(unmarshaller, source);
            }
            else {
                return unmarshaller.unmarshal(source);
            }
        }
        catch (JAXBException ex) {
            throw convertJaxbException(ex);
        }
    }

    private Object unmarshalStaxSource(Unmarshaller jaxbUnmarshaller, Source staxSource) throws JAXBException {
        XMLStreamReader streamReader = TraxUtils.getXMLStreamReader(staxSource);
        if (streamReader != null) {
            return jaxbUnmarshaller.unmarshal(streamReader);
        }
        else {
            XMLEventReader eventReader = TraxUtils.getXMLEventReader(staxSource);
            if (eventReader != null) {
                return jaxbUnmarshaller.unmarshal(eventReader);
            }
            else {
                throw new IllegalArgumentException("StaxSource contains neither XMLStreamReader nor XMLEventReader");
            }
        }
    }

    /*
    * Inner classes
    */

    private static class Jaxb2AttachmentMarshaller extends AttachmentMarshaller {

        private final MimeContainer mimeContainer;

        public Jaxb2AttachmentMarshaller(MimeContainer mimeContainer) {
            this.mimeContainer = mimeContainer;
        }

        public String addMtomAttachment(byte[] data,
                                        int offset,
                                        int length,
                                        String mimeType,
                                        String elementNamespace,
                                        String elementLocalName) {
            ByteArrayDataSource dataSource = new ByteArrayDataSource(mimeType, data, offset, length);
            return addMtomAttachment(new DataHandler(dataSource), elementNamespace, elementLocalName);
        }

        public String addMtomAttachment(DataHandler dataHandler, String elementNamespace, String elementLocalName) {
            String host = getHost(elementNamespace, dataHandler);
            String contentId = UUID.randomUUID() + "@" + host;
            mimeContainer.addAttachment("<" + contentId + ">", dataHandler);
            try {
                contentId = URLEncoder.encode(contentId, "UTF-8");
            }
            catch (UnsupportedEncodingException e) {
                // ignore
            }
            return "cid:" + contentId;
        }

        private String getHost(String elementNamespace, DataHandler dataHandler) {
            try {
                URI uri = new URI(elementNamespace);
                return uri.getHost();
            }
            catch (URISyntaxException e) {
                // ignore
            }
            return dataHandler.getName();
        }

        public String addSwaRefAttachment(DataHandler dataHandler) {
            String contentId = UUID.randomUUID() + "@" + dataHandler.getName();
            mimeContainer.addAttachment(contentId, dataHandler);
            return contentId;
        }

        @Override
        public boolean isXOPPackage() {
            return mimeContainer.convertToXopPackage();
        }
    }

    private static class Jaxb2AttachmentUnmarshaller extends AttachmentUnmarshaller {

        private final MimeContainer mimeContainer;

        public Jaxb2AttachmentUnmarshaller(MimeContainer mimeContainer) {
            this.mimeContainer = mimeContainer;
        }

        public byte[] getAttachmentAsByteArray(String cid) {
            try {
                DataHandler dataHandler = getAttachmentAsDataHandler(cid);
                return FileCopyUtils.copyToByteArray(dataHandler.getInputStream());
            }
            catch (IOException ex) {
                throw new JaxbUnmarshallingFailureException(ex);
            }
        }

        public DataHandler getAttachmentAsDataHandler(String contentId) {
            if (contentId.startsWith("cid:")) {
                contentId = contentId.substring("cid:".length());
                try {
                    contentId = URLDecoder.decode(contentId, "UTF-8");
                }
                catch (UnsupportedEncodingException e) {
                    // ignore
                }
                contentId = '<' + contentId + '>';
            }
            return mimeContainer.getAttachment(contentId);
        }

        @Override
        public boolean isXOPPackage() {
            return mimeContainer.isXopPackage();
        }
    }

    /*
     * DataSource that wraps around a byte array
     */
    private static class ByteArrayDataSource implements DataSource {

        private byte[] data;

        private String contentType;

        private int offset;

        private int length;

        public ByteArrayDataSource(String contentType, byte[] data, int offset, int length) {
            this.contentType = contentType;
            this.data = data;
            this.offset = offset;
            this.length = length;
        }

        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(data, offset, length);
        }

        public OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException();
        }

        public String getContentType() {
            return contentType;
        }

        public String getName() {
            return "ByteArrayDataSource";
        }
    }

}

