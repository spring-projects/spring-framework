/*
 * Copyright 2008 the original author or authors.
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

package org.springframework.context.config;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.config.PropertyOverrideConfigurer;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Arjen Poutsma
 * @author Dave Syer
 * @since 2.5.6
 */
public class ContextNamespaceHandlerTests {

	@Test
	public void propertyPlaceholder() throws Exception {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"contextNamespaceHandlerTests-replace.xml", getClass());
		Map<String, PropertyPlaceholderConfigurer> beans = applicationContext
				.getBeansOfType(PropertyPlaceholderConfigurer.class);
		assertFalse("No PropertyPlaceHolderConfigurer found", beans.isEmpty());
		String s = (String) applicationContext.getBean("string");
		assertEquals("No properties replaced", "bar", s);
	}

	@Test
	public void propertyPlaceholderSystemProperties() throws Exception {
		String value = System.setProperty("foo", "spam");
		try {
			ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
					"contextNamespaceHandlerTests-system.xml", getClass());
			Map<String, PropertyPlaceholderConfigurer> beans = applicationContext
					.getBeansOfType(PropertyPlaceholderConfigurer.class);
			assertFalse("No PropertyPlaceHolderConfigurer found", beans.isEmpty());
			String s = (String) applicationContext.getBean("string");
			assertEquals("No properties replaced", "spam", s);
		} finally {
			if (value!=null) {
				System.setProperty("foo", value);
			}
		}
	}

	@Test
	public void propertyPlaceholderLocation() throws Exception {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"contextNamespaceHandlerTests-location.xml", getClass());
		Map<String, PropertyPlaceholderConfigurer> beans = applicationContext
				.getBeansOfType(PropertyPlaceholderConfigurer.class);
		assertFalse("No PropertyPlaceHolderConfigurer found", beans.isEmpty());
		String s = (String) applicationContext.getBean("foo");
		assertEquals("No properties replaced", "bar", s);
		s = (String) applicationContext.getBean("bar");
		assertEquals("No properties replaced", "foo", s);
		s = (String) applicationContext.getBean("spam");
		assertEquals("No properties replaced", "maps", s);
	}

	@Test
	public void propertyPlaceholderIgnored() throws Exception {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"contextNamespaceHandlerTests-replace-ignore.xml", getClass());
		Map<String, PropertyPlaceholderConfigurer> beans = applicationContext
				.getBeansOfType(PropertyPlaceholderConfigurer.class);
		assertFalse("No PropertyPlaceHolderConfigurer found", beans.isEmpty());
		String s = (String) applicationContext.getBean("string");
		assertEquals("Properties replaced", "${bar}", s);
	}

	@Test
	public void propertyOverride() throws Exception {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"contextNamespaceHandlerTests-override.xml", getClass());
		Map<String, PropertyOverrideConfigurer> beans = applicationContext
				.getBeansOfType(PropertyOverrideConfigurer.class);
		assertFalse("No PropertyOverrideConfigurer found", beans.isEmpty());
		Date date = (Date) applicationContext.getBean("date");
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		assertEquals("No properties overriden", 42, calendar.get(Calendar.MINUTE));
	}
}
