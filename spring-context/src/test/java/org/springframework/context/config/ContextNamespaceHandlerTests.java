/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.config;

import java.util.Calendar;
import java.util.Date;

import org.junit.After;
import org.junit.Test;

import org.springframework.beans.FatalBeanException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Arjen Poutsma
 * @author Dave Syer
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 2.5.6
 */
public class ContextNamespaceHandlerTests {

	@After
	public void tearDown() {
		System.getProperties().remove("foo");
	}


	@Test
	public void propertyPlaceholder() {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"contextNamespaceHandlerTests-replace.xml", getClass());
		assertEquals("bar", applicationContext.getBean("string"));
		assertEquals("null", applicationContext.getBean("nullString"));
	}

	@Test
	public void propertyPlaceholderSystemProperties() {
		String value = System.setProperty("foo", "spam");
		try {
			ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
					"contextNamespaceHandlerTests-system.xml", getClass());
			assertEquals("spam", applicationContext.getBean("string"));
			assertEquals("none", applicationContext.getBean("fallback"));
		}
		finally {
			if (value != null) {
				System.setProperty("foo", value);
			}
		}
	}

	@Test
	public void propertyPlaceholderEnvironmentProperties() {
		MockEnvironment env = new MockEnvironment().withProperty("foo", "spam");
		GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
		applicationContext.setEnvironment(env);
		applicationContext.load(new ClassPathResource("contextNamespaceHandlerTests-simple.xml", getClass()));
		applicationContext.refresh();
		assertEquals("spam", applicationContext.getBean("string"));
		assertEquals("none", applicationContext.getBean("fallback"));
	}

	@Test
	public void propertyPlaceholderLocation() {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"contextNamespaceHandlerTests-location.xml", getClass());
		assertEquals("bar", applicationContext.getBean("foo"));
		assertEquals("foo", applicationContext.getBean("bar"));
		assertEquals("maps", applicationContext.getBean("spam"));
	}

	@Test
	public void propertyPlaceholderLocationWithSystemPropertyForOneLocation() {
		System.setProperty("properties",
				"classpath*:/org/springframework/context/config/test-*.properties");
		try {
			ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
					"contextNamespaceHandlerTests-location-placeholder.xml", getClass());
			assertEquals("bar", applicationContext.getBean("foo"));
			assertEquals("foo", applicationContext.getBean("bar"));
			assertEquals("maps", applicationContext.getBean("spam"));
		}
		finally {
			System.clearProperty("properties");
		}
	}

	@Test
	public void propertyPlaceholderLocationWithSystemPropertyForMultipleLocations() {
		System.setProperty("properties",
				"classpath*:/org/springframework/context/config/test-*.properties," +
				"classpath*:/org/springframework/context/config/empty-*.properties," +
				"classpath*:/org/springframework/context/config/missing-*.properties");
		try {
			ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
					"contextNamespaceHandlerTests-location-placeholder.xml", getClass());
			assertEquals("bar", applicationContext.getBean("foo"));
			assertEquals("foo", applicationContext.getBean("bar"));
			assertEquals("maps", applicationContext.getBean("spam"));
		}
		finally {
			System.clearProperty("properties");
		}
	}

	@Test
	public void propertyPlaceholderLocationWithSystemPropertyMissing() {
		try {
			new ClassPathXmlApplicationContext(
					"contextNamespaceHandlerTests-location-placeholder.xml", getClass());
			fail("Should have thrown FatalBeanException");
		}
		catch (FatalBeanException ex) {
			Throwable cause = ex.getRootCause();
			assertTrue(cause instanceof IllegalArgumentException);
			assertEquals("Could not resolve placeholder 'foo' in value \"${foo}\"", cause.getMessage());
		}
	}

	@Test
	public void propertyPlaceholderIgnored() {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"contextNamespaceHandlerTests-replace-ignore.xml", getClass());
		assertEquals("${bar}", applicationContext.getBean("string"));
		assertEquals("null", applicationContext.getBean("nullString"));
	}

	@Test
	public void propertyOverride() {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"contextNamespaceHandlerTests-override.xml", getClass());
		Date date = (Date) applicationContext.getBean("date");
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		assertEquals(42, calendar.get(Calendar.MINUTE));
	}

}
