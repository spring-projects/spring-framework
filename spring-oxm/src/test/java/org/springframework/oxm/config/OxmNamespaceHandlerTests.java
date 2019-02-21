/*
 * Copyright 2002-2018 the original author or authors.
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

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import static org.junit.Assert.*;

/**
 * Tests the {@link OxmNamespaceHandler} class.
 *
 * @author Arjen Poustma
 * @author Jakub Narloch
 * @author Sam Brannen
 */
public class OxmNamespaceHandlerTests {

	private final ApplicationContext applicationContext =
			new ClassPathXmlApplicationContext("oxmNamespaceHandlerTest.xml", getClass());


	@Test
	public void jaxb2ContextPathMarshaller() {
		Jaxb2Marshaller jaxb2Marshaller = applicationContext.getBean("jaxb2ContextPathMarshaller", Jaxb2Marshaller.class);
		assertNotNull(jaxb2Marshaller);
	}

	@Test
	public void jaxb2ClassesToBeBoundMarshaller() {
		Jaxb2Marshaller jaxb2Marshaller = applicationContext.getBean("jaxb2ClassesMarshaller", Jaxb2Marshaller.class);
		assertNotNull(jaxb2Marshaller);
	}

}
