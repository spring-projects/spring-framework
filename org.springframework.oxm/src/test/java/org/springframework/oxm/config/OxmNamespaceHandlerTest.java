/*
 * Copyright ${YEAR} the original author or authors.
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

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.oxm.jaxb.Jaxb1Marshaller;
import org.springframework.oxm.jibx.JibxMarshaller;
import org.springframework.oxm.xmlbeans.XmlBeansMarshaller;

import junit.framework.TestCase;
import org.apache.xmlbeans.XmlOptions;

public class OxmNamespaceHandlerTest extends TestCase {

    private ApplicationContext applicationContext;

    protected void setUp() throws Exception {
        applicationContext = new ClassPathXmlApplicationContext("oxmNamespaceHandlerTest.xml", getClass());
    }

    public void testJaxb1Marshaller() throws Exception {
        applicationContext.getBean("jaxb1Marshaller", Jaxb1Marshaller.class);
    }

    public void testJibxMarshaller() throws Exception {
        applicationContext.getBean("jibxMarshaller", JibxMarshaller.class);
    }

    public void testXmlBeansMarshaller() throws Exception {
        XmlBeansMarshaller marshaller =
                (XmlBeansMarshaller) applicationContext.getBean("xmlBeansMarshaller", XmlBeansMarshaller.class);
        XmlOptions options = marshaller.getXmlOptions();
        assertNotNull("Options not set", options);
        assertTrue("option not set", options.hasOption("SAVE_PRETTY_PRINT"));
        assertEquals("option not set", "true", options.get("SAVE_PRETTY_PRINT"));
    }
}