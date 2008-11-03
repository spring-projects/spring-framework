/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.beans.factory.annotation;

import junit.framework.TestCase;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Rob Harrop
 * @since 2.0
 */
public class RequiredAnnotationBeanPostProcessorTests extends TestCase {

	public void testWithRequiredPropertyOmitted() throws Exception {
		try {
			new ClassPathXmlApplicationContext("requiredWithOneRequiredPropertyOmitted.xml", getClass());
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			String message = ex.getCause().getMessage();
			assertTrue(message.indexOf("Property") > -1);
			assertTrue(message.indexOf("age") > -1);
			assertTrue(message.indexOf("testBean") > -1);
		}
	}

	public void testWithThreeRequiredPropertiesOmitted() throws Exception {
		try {
			new ClassPathXmlApplicationContext("requiredWithThreeRequiredPropertiesOmitted.xml", getClass());
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			String message = ex.getCause().getMessage();
			assertTrue(message.indexOf("Properties") > -1);
			assertTrue(message.indexOf("age") > -1);
			assertTrue(message.indexOf("favouriteColour") > -1);
			assertTrue(message.indexOf("jobTitle") > -1);
			assertTrue(message.indexOf("testBean") > -1);
		}
	}

	public void testWithOnlyRequiredPropertiesSpecified() throws Exception {
		ApplicationContext context =
				new ClassPathXmlApplicationContext("requiredWithAllRequiredPropertiesProvided.xml", getClass());
		RequiredTestBean bean = (RequiredTestBean) context.getBean("testBean");
		assertEquals(24, bean.getAge());
		assertEquals("Blue", bean.getFavouriteColour());
	}

	public void testWithCustomAnnotation() throws Exception {
		try {
			new ClassPathXmlApplicationContext("requiredWithCustomAnnotation.xml", getClass());
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			String message = ex.getCause().getMessage();
			assertTrue(message.indexOf("Property") > -1);
			assertTrue(message.indexOf("name") > -1);
			assertTrue(message.indexOf("testBean") > -1);
		}
	}

}
