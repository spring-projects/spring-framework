/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.oxm.config;

import org.apache.xmlbeans.XmlOptions;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.oxm.castor.CastorMarshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.oxm.xmlbeans.XmlBeansMarshaller;

import static org.junit.Assert.*;

/**
 * Tests the {@link OxmNamespaceHandler} class.
 *
 * @author Arjen Poustma
 * @author Jakub Narloch
 * @author David Harrigan
 */
public class OxmNamespaceHandlerTests {

    private ApplicationContext applicationContext;

    @Before
    public void createAppContext() throws Exception {
        applicationContext = new ClassPathXmlApplicationContext("oxmNamespaceHandlerTest.xml", getClass());
    }

    @Test
    public void xmlBeansMarshaller() throws Exception {
        XmlBeansMarshaller marshaller = applicationContext.getBean("xmlBeansMarshaller", XmlBeansMarshaller.class);
        XmlOptions options = marshaller.getXmlOptions();
        assertNotNull("Options not set", options);
        assertTrue("option not set", options.hasOption("SAVE_PRETTY_PRINT"));
        assertEquals("option not set", "true", options.get("SAVE_PRETTY_PRINT"));
    }

    @Test
    public void jaxb2ContextPathMarshaller() throws Exception {
        Jaxb2Marshaller jaxb2Marshaller = applicationContext.getBean("jaxb2ContextPathMarshaller", Jaxb2Marshaller.class);
        assertNotNull(jaxb2Marshaller);
    }

    @Test
    public void jaxb2ClassesToBeBoundMarshaller() throws Exception {
        Jaxb2Marshaller jaxb2Marshaller = applicationContext.getBean("jaxb2ClassesMarshaller", Jaxb2Marshaller.class);
        assertNotNull(jaxb2Marshaller);
    }

    @Test
    public void jaxb2PackagesToScanMarshaller() throws Exception {
        Jaxb2Marshaller jaxb2Marshaller = applicationContext.getBean("jaxb2PackagesMarshaller", Jaxb2Marshaller.class);
        assertNotNull(jaxb2Marshaller);
    }

    @Test
    public void castorEncodingMarshaller() throws Exception {
        CastorMarshaller castorMarshaller = applicationContext.getBean("castorEncodingMarshaller", CastorMarshaller.class);
        assertNotNull(castorMarshaller);
    }

    @Test
    public void castorTargetClassMarshaller() throws Exception {
        CastorMarshaller castorMarshaller = applicationContext.getBean("castorTargetClassMarshaller", CastorMarshaller.class);
        assertNotNull(castorMarshaller);
    }

    @Test
    public void castorTargetPackageMarshaller() throws Exception {
        CastorMarshaller castorMarshaller = applicationContext.getBean("castorTargetPackageMarshaller", CastorMarshaller.class);
        assertNotNull(castorMarshaller);
    }

    @Test
    public void castorMappingLocationMarshaller() throws Exception {
        CastorMarshaller castorMarshaller = applicationContext.getBean("castorMappingLocationMarshaller", CastorMarshaller.class);
        assertNotNull(castorMarshaller);
    }
}
