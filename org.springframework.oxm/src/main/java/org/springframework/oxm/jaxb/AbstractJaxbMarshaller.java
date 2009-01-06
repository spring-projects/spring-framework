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

import java.util.Iterator;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.oxm.XmlMappingException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for implementations of the <code>Marshaller</code> and <code>Unmarshaller</code> interfaces that
 * use JAXB. This base class is responsible for creating JAXB marshallers from a <code>JAXBContext</code>.
 * <p/>
 * JAXB 2.0 added  breaking API changes, so specific subclasses must be used for JAXB 1.0 and 2.0
 * (<code>Jaxb1Marshaller</code> and <code>Jaxb2Marshaller</code> respectivaly).
 *
 * @author Arjen Poutsma
 * @see Jaxb1Marshaller
 * @see Jaxb2Marshaller
 * @since 1.0.0
 */
public abstract class AbstractJaxbMarshaller
        implements org.springframework.oxm.Marshaller, org.springframework.oxm.Unmarshaller, InitializingBean {

    /** Logger available to subclasses. */
    protected final Log logger = LogFactory.getLog(getClass());

    private String contextPath;

    private Map marshallerProperties;

    private Map unmarshallerProperties;

    private JAXBContext jaxbContext;

    private ValidationEventHandler validationEventHandler;

    /** Returns the JAXB Context path. */
    protected String getContextPath() {
        return contextPath;
    }

    /** Sets the JAXB Context path. */
    public void setContextPath(String contextPath) {
        Assert.notNull(contextPath, "'contextPath' must not be null");
        this.contextPath = contextPath;
    }

    /**
     * Sets multiple JAXB Context paths. The given array of context paths is converted to a colon-delimited string, as
     * supported by JAXB.
     */
    public void setContextPaths(String[] contextPaths) {
        Assert.notEmpty(contextPaths, "'contextPaths' must not be empty");
        this.contextPath = StringUtils.arrayToDelimitedString(contextPaths, ":");
    }

    /**
     * Sets the JAXB <code>Marshaller</code> properties. These properties will be set on the underlying JAXB
     * <code>Marshaller</code>, and allow for features such as indentation.
     *
     * @param properties the properties
     * @see javax.xml.bind.Marshaller#setProperty(String,Object)
     * @see javax.xml.bind.Marshaller#JAXB_ENCODING
     * @see javax.xml.bind.Marshaller#JAXB_FORMATTED_OUTPUT
     * @see javax.xml.bind.Marshaller#JAXB_NO_NAMESPACE_SCHEMA_LOCATION
     * @see javax.xml.bind.Marshaller#JAXB_SCHEMA_LOCATION
     */
    public void setMarshallerProperties(Map properties) {
        this.marshallerProperties = properties;
    }

    /**
     * Sets the JAXB <code>Unmarshaller</code> properties. These properties will be set on the underlying JAXB
     * <code>Unmarshaller</code>.
     *
     * @param properties the properties
     * @see javax.xml.bind.Unmarshaller#setProperty(String,Object)
     */
    public void setUnmarshallerProperties(Map properties) {
        this.unmarshallerProperties = properties;
    }

    /**
     * Sets the JAXB validation event handler. This event handler will be called by JAXB if any validation errors are
     * encountered during calls to any of the marshal API's.
     *
     * @param validationEventHandler the event handler
     */
    public void setValidationEventHandler(ValidationEventHandler validationEventHandler) {
        this.validationEventHandler = validationEventHandler;
    }

    /** Returns the {@link JAXBContext} created in {@link #afterPropertiesSet()}. */
    public JAXBContext getJaxbContext() {
        return jaxbContext;
    }

    public final void afterPropertiesSet() throws Exception {
        try {
            jaxbContext = createJaxbContext();
        }
        catch (JAXBException ex) {
            throw convertJaxbException(ex);
        }
    }

    /**
     * Convert the given <code>JAXBException</code> to an appropriate exception from the
     * <code>org.springframework.oxm</code> hierarchy.
     * <p/>
     * The default implementation delegates to <code>JaxbUtils</code>. Can be overridden in subclasses.
     *
     * @param ex <code>JAXBException</code> that occured
     * @return the corresponding <code>XmlMappingException</code> instance
     * @see JaxbUtils#convertJaxbException
     */
    protected XmlMappingException convertJaxbException(JAXBException ex) {
        return JaxbUtils.convertJaxbException(ex);
    }

    /** Returns a newly created JAXB marshaller. JAXB marshallers are not necessarily thread safe. */
    protected Marshaller createMarshaller() {
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            if (marshallerProperties != null) {
                for (Iterator iterator = marshallerProperties.keySet().iterator(); iterator.hasNext();) {
                    String name = (String) iterator.next();
                    marshaller.setProperty(name, marshallerProperties.get(name));
                }
            }
            if (validationEventHandler != null) {
                marshaller.setEventHandler(validationEventHandler);
            }
            initJaxbMarshaller(marshaller);
            return marshaller;
        }
        catch (JAXBException ex) {
            throw convertJaxbException(ex);
        }
    }

    /** Returns a newly created JAXB unmarshaller. JAXB unmarshallers are not necessarily thread safe. */
    protected Unmarshaller createUnmarshaller() {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            if (unmarshallerProperties != null) {
                for (Iterator iterator = unmarshallerProperties.keySet().iterator(); iterator.hasNext();) {
                    String name = (String) iterator.next();
                    unmarshaller.setProperty(name, unmarshallerProperties.get(name));
                }
            }
            if (validationEventHandler != null) {
                unmarshaller.setEventHandler(validationEventHandler);
            }
            initJaxbUnmarshaller(unmarshaller);
            return unmarshaller;
        }
        catch (JAXBException ex) {
            throw convertJaxbException(ex);
        }
    }

    /**
     * Template method that can be overridden by concrete JAXB marshallers for custom initialization behavior. Gets
     * called after creation of JAXB <code>Marshaller</code>, and after the respective properties have been set.
     * <p/>
     * Default implementation does nothing.
     */
    protected void initJaxbMarshaller(Marshaller marshaller) throws JAXBException {
    }

    /**
     * Template method that can overridden by concrete JAXB marshallers for custom initialization behavior. Gets called
     * after creation of JAXB <code>Unmarshaller</code>, and after the respective properties have been set.
     * <p/>
     * Default implementation does nothing.
     */
    protected void initJaxbUnmarshaller(Unmarshaller unmarshaller) throws JAXBException {
    }

    /** Template method that returns a newly created JAXB context. Called from <code>afterPropertiesSet()</code>. */
    protected abstract JAXBContext createJaxbContext() throws Exception;
}
