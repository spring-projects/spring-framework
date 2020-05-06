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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.FatalBeanException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Arjen Poutsma
 * @author Dave Syer
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 2.5.6
 */
public class ContextNamespaceHandlerTests {

	@AfterEach
	public void tearDown() {
		System.getProperties().remove("foo");
	}


	@Test
	public void propertyPlaceholder() {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"contextNamespaceHandlerTests-replace.xml", getClass());
		assertThat(applicationContext.getBean("string")).isEqualTo("bar");
		assertThat(applicationContext.getBean("nullString")).isEqualTo("null");
	}

	@Test
	public void propertyPlaceholderSystemProperties() {
		String value = System.setProperty("foo", "spam");
		try {
			ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
					"contextNamespaceHandlerTests-system.xml", getClass());
			assertThat(applicationContext.getBean("string")).isEqualTo("spam");
			assertThat(applicationContext.getBean("fallback")).isEqualTo("none");
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
		assertThat(applicationContext.getBean("string")).isEqualTo("spam");
		assertThat(applicationContext.getBean("fallback")).isEqualTo("none");
	}

	@Test
	public void propertyPlaceholderLocation() {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"contextNamespaceHandlerTests-location.xml", getClass());
		assertThat(applicationContext.getBean("foo")).isEqualTo("bar");
		assertThat(applicationContext.getBean("bar")).isEqualTo("foo");
		assertThat(applicationContext.getBean("spam")).isEqualTo("maps");
	}

	@Test
	public void propertyPlaceholderLocationWithSystemPropertyForOneLocation() {
		System.setProperty("properties",
				"classpath*:/org/springframework/context/config/test-*.properties");
		try {
			ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
					"contextNamespaceHandlerTests-location-placeholder.xml", getClass());
			assertThat(applicationContext.getBean("foo")).isEqualTo("bar");
			assertThat(applicationContext.getBean("bar")).isEqualTo("foo");
			assertThat(applicationContext.getBean("spam")).isEqualTo("maps");
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
			assertThat(applicationContext.getBean("foo")).isEqualTo("bar");
			assertThat(applicationContext.getBean("bar")).isEqualTo("foo");
			assertThat(applicationContext.getBean("spam")).isEqualTo("maps");
		}
		finally {
			System.clearProperty("properties");
		}
	}

	@Test
	public void propertyPlaceholderLocationWithSystemPropertyMissing() {
		assertThatExceptionOfType(FatalBeanException.class).isThrownBy(() ->
				new ClassPathXmlApplicationContext("contextNamespaceHandlerTests-location-placeholder.xml", getClass()))
			.havingRootCause()
			.isInstanceOf(IllegalArgumentException.class)
			.withMessage("Could not resolve placeholder 'foo' in value \"${foo}\"");
	}

	@Test
	public void propertyPlaceholderIgnored() {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"contextNamespaceHandlerTests-replace-ignore.xml", getClass());
		assertThat(applicationContext.getBean("string")).isEqualTo("${bar}");
		assertThat(applicationContext.getBean("nullString")).isEqualTo("null");
	}

	@Test
	public void propertyOverride() {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"contextNamespaceHandlerTests-override.xml", getClass());
		Date date = (Date) applicationContext.getBean("date");
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		assertThat(calendar.get(Calendar.MINUTE)).isEqualTo(42);
	}

}
