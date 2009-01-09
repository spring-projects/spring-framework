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

import org.apache.xmlbeans.XmlOptions;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.oxm.jibx.JibxMarshaller;
import org.springframework.oxm.xmlbeans.XmlBeansMarshaller;

public class OxmNamespaceHandlerTest {

	private ApplicationContext applicationContext;

	@Before
	public void createAppContext() throws Exception {
		applicationContext = new ClassPathXmlApplicationContext("oxmNamespaceHandlerTest.xml", getClass());
	}

	@Test
	@Ignore
	public void jibxMarshaller() throws Exception {
		applicationContext.getBean("jibxMarshaller", JibxMarshaller.class);
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
		applicationContext.getBean("contextPathMarshaller", Jaxb2Marshaller.class);
	}

	@Test
	public void jaxb2ClassesToBeBoundMarshaller() throws Exception {
		applicationContext.getBean("classesMarshaller", Jaxb2Marshaller.class);
	}

}