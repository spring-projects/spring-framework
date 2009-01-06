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
package org.springframework.oxm.jaxb;

import javax.xml.bind.Element;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.xml.transform.StaxResult;
import org.springframework.xml.transform.StaxSource;
import org.springframework.xml.transform.TraxUtils;

/**
 * Implementation of the <code>Marshaller</code> interface for JAXB 1.0.
 * <p/>
 * The typical usage will be to set the <code>contextPath</code> property on this bean, possibly customize the
 * marshaller and unmarshaller by setting properties, and validations, and to refer to it.
 *
 * @author Arjen Poutsma
 * @see #setContextPath(String)
 * @see #setMarshallerProperties(java.util.Map)
 * @see #setUnmarshallerProperties(java.util.Map)
 * @see #setValidating(boolean)
 * @since 1.0.0
 */
public class Jaxb1Marshaller extends AbstractJaxbMarshaller implements BeanClassLoaderAware {

    private boolean validating = false;

    private ClassLoader classLoader;

    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /** Set if the JAXB <code>Unmarshaller</code> should validate the incoming document. Default is <code>false</code>. */
    public void setValidating(boolean validating) {
        this.validating = validating;
    }

    public boolean supports(Class clazz) {
        if (!Element.class.isAssignableFrom(clazz)) {
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
        return false;

    }

    protected final JAXBContext createJaxbContext() throws JAXBException {
        if (!StringUtils.hasLength(getContextPath())) {
            throw new IllegalArgumentException("contextPath is required");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Creating JAXBContext with context path [" + getContextPath() + "]");
        }
        return classLoader != null ? JAXBContext.newInstance(getContextPath(), classLoader) :
                JAXBContext.newInstance(getContextPath());
    }

    protected void initJaxbUnmarshaller(Unmarshaller unmarshaller) throws JAXBException {
        unmarshaller.setValidating(validating);
    }

    public void marshal(Object graph, Result result) {
        if (TraxUtils.isStaxResult(result)) {
            XMLStreamWriter streamWriter = TraxUtils.getXMLStreamWriter(result);
            if (streamWriter != null) {
                result = new StaxResult(streamWriter);
            }
            else {
                XMLEventWriter eventWriter = TraxUtils.getXMLEventWriter(result);
                if (eventWriter != null) {
                    result = new StaxResult(eventWriter);
                }
                else {
                    throw new IllegalArgumentException(
                            "StAXResult contains neither XMLStreamWriter nor XMLEventWriter");
                }
            }
        }
        try {
            createMarshaller().marshal(graph, result);
        }
        catch (JAXBException ex) {
            throw convertJaxbException(ex);
        }
    }

    public Object unmarshal(Source source) {
        if (TraxUtils.isStaxSource(source)) {
            XMLStreamReader streamReader = TraxUtils.getXMLStreamReader(source);
            if (streamReader != null) {
                source = new StaxSource(streamReader);
            }
            else {
                XMLEventReader eventReader = TraxUtils.getXMLEventReader(source);
                if (eventReader != null) {
                    source = new StaxSource(eventReader);
                }
                else {
                    throw new IllegalArgumentException(
                            "StAXSource contains neither XMLStreamReader nor XMLEventReader");
                }
            }
        }
        try {
            return createUnmarshaller().unmarshal(source);
        }
        catch (JAXBException ex) {
            throw convertJaxbException(ex);
        }
    }

}
