/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.context.support;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ObjectUtils;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link DelegatingSmartContextLoader}.
 *
 * @author Sam Brannen
 * @since 3.1
 */
public class DelegatingSmartContextLoaderTests {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];
	private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

	private final DelegatingSmartContextLoader loader = new DelegatingSmartContextLoader();

	@Rule
	public ExpectedException expectedException = ExpectedException.none();


	private static void assertEmpty(Object[] array) {
		assertTrue(ObjectUtils.isEmpty(array));
	}

	// --- SmartContextLoader - processContextConfiguration() ------------------

	@Test
	public void processContextConfigurationWithDefaultXmlConfigGeneration() {
		ContextConfigurationAttributes configAttributes = new ContextConfigurationAttributes(
				XmlTestCase.class, EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, true, null, true, ContextLoader.class);
		loader.processContextConfiguration(configAttributes);
		assertEquals(1, configAttributes.getLocations().length);
		assertEmpty(configAttributes.getClasses());
	}

	@Test
	public void processContextConfigurationWithDefaultConfigurationClassGeneration() {
		ContextConfigurationAttributes configAttributes = new ContextConfigurationAttributes(
				ConfigClassTestCase.class, EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, true, null, true, ContextLoader.class);
		loader.processContextConfiguration(configAttributes);
		assertEquals(1, configAttributes.getClasses().length);
		assertEmpty(configAttributes.getLocations());
	}

	@Test
	public void processContextConfigurationWithDefaultXmlConfigAndConfigurationClassGeneration() {
		expectedException.expect(IllegalStateException.class);
		expectedException.expectMessage(containsString("both default locations AND default configuration classes were detected"));

		ContextConfigurationAttributes configAttributes = new ContextConfigurationAttributes(
				ImproperDuplicateDefaultXmlAndConfigClassTestCase.class, EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY,
				true, null, true, ContextLoader.class);
		loader.processContextConfiguration(configAttributes);
	}

	@Test
	public void processContextConfigurationWithLocation() {
		String[] locations = new String[] {"classpath:/foo.xml"};
		ContextConfigurationAttributes configAttributes = new ContextConfigurationAttributes(
				getClass(), locations, EMPTY_CLASS_ARRAY, true, null, true, ContextLoader.class);
		loader.processContextConfiguration(configAttributes);
		assertArrayEquals(locations, configAttributes.getLocations());
		assertEmpty(configAttributes.getClasses());
	}

	@Test
	public void processContextConfigurationWithConfigurationClass() {
		Class<?>[] classes = new Class<?>[] {getClass()};
		ContextConfigurationAttributes configAttributes = new ContextConfigurationAttributes(
				getClass(), EMPTY_STRING_ARRAY, classes, true, null, true, ContextLoader.class);
		loader.processContextConfiguration(configAttributes);
		assertArrayEquals(classes, configAttributes.getClasses());
		assertEmpty(configAttributes.getLocations());
	}

	// --- SmartContextLoader - loadContext() ----------------------------------

	@Test(expected = IllegalArgumentException.class)
	public void loadContextWithNullConfig() throws Exception {
		MergedContextConfiguration mergedConfig = null;
		loader.loadContext(mergedConfig);
	}

	@Test
	public void loadContextWithoutLocationsAndConfigurationClasses() throws Exception {
		expectedException.expect(IllegalStateException.class);
		expectedException.expectMessage(startsWith("Neither"));
		expectedException.expectMessage(containsString("was able to load an ApplicationContext from"));

		MergedContextConfiguration mergedConfig = new MergedContextConfiguration(
				getClass(), EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
		loader.loadContext(mergedConfig);
	}

	/**
	 * @since 4.1
	 */
	@Test
	public void loadContextWithLocationsAndConfigurationClasses() throws Exception {
		expectedException.expect(IllegalStateException.class);
		expectedException.expectMessage(startsWith("Neither"));
		expectedException.expectMessage(endsWith("declare either 'locations' or 'classes' but not both."));

		MergedContextConfiguration mergedConfig = new MergedContextConfiguration(getClass(),
				new String[] {"test.xml"}, new Class<?>[] {getClass()}, EMPTY_STRING_ARRAY, loader);
		loader.loadContext(mergedConfig);
	}

	private void assertApplicationContextLoadsAndContainsFooString(MergedContextConfiguration mergedConfig)
			throws Exception {

		ApplicationContext applicationContext = loader.loadContext(mergedConfig);
		assertNotNull(applicationContext);
		assertEquals("foo", applicationContext.getBean(String.class));
		assertTrue(applicationContext instanceof ConfigurableApplicationContext);
		((ConfigurableApplicationContext) applicationContext).close();
	}

	@Test
	public void loadContextWithXmlConfig() throws Exception {
		MergedContextConfiguration mergedConfig = new MergedContextConfiguration(
				XmlTestCase.class,
				new String[] {"classpath:/org/springframework/test/context/support/DelegatingSmartContextLoaderTests$XmlTestCase-context.xml"},
				EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
		assertApplicationContextLoadsAndContainsFooString(mergedConfig);
	}

	@Test
	public void loadContextWithConfigurationClass() throws Exception {
		MergedContextConfiguration mergedConfig = new MergedContextConfiguration(ConfigClassTestCase.class,
				EMPTY_STRING_ARRAY, new Class<?>[] {ConfigClassTestCase.Config.class}, EMPTY_STRING_ARRAY, loader);
		assertApplicationContextLoadsAndContainsFooString(mergedConfig);
	}

	// --- ContextLoader -------------------------------------------------------

	@Test(expected = UnsupportedOperationException.class)
	public void processLocations() {
		loader.processLocations(getClass(), EMPTY_STRING_ARRAY);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void loadContextFromLocations() throws Exception {
		loader.loadContext(EMPTY_STRING_ARRAY);
	}


	// -------------------------------------------------------------------------

	static class XmlTestCase {
	}

	static class ConfigClassTestCase {

		@Configuration
		static class Config {

			@Bean
			public String foo() {
				return new String("foo");
			}
		}

		static class NotAConfigClass {
		}
	}

	static class ImproperDuplicateDefaultXmlAndConfigClassTestCase {

		@Configuration
		static class Config {
			// intentionally empty: we just need the class to be present to fail
			// the test
		}
	}

}
