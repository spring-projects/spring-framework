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

package org.springframework.test.context.support;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link DelegatingSmartContextLoader}.
 *
 * @author Sam Brannen
 * @since 3.1
 */
class DelegatingSmartContextLoaderTests {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];
	private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

	private final DelegatingSmartContextLoader loader = new DelegatingSmartContextLoader();


	private static void assertEmpty(Object[] array) {
		assertThat(ObjectUtils.isEmpty(array)).isTrue();
	}

	// --- SmartContextLoader - processContextConfiguration() ------------------

	@Test
	void processContextConfigurationWithDefaultXmlConfigGeneration() {
		ContextConfigurationAttributes configAttributes = new ContextConfigurationAttributes(
				XmlTestCase.class, EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, true, null, true, ContextLoader.class);
		loader.processContextConfiguration(configAttributes);
		assertThat(configAttributes.getLocations().length).isEqualTo(1);
		assertEmpty(configAttributes.getClasses());
	}

	@Test
	void processContextConfigurationWithDefaultConfigurationClassGeneration() {
		ContextConfigurationAttributes configAttributes = new ContextConfigurationAttributes(
				ConfigClassTestCase.class, EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, true, null, true, ContextLoader.class);
		loader.processContextConfiguration(configAttributes);
		assertThat(configAttributes.getClasses().length).isEqualTo(1);
		assertEmpty(configAttributes.getLocations());
	}

	@Test
	void processContextConfigurationWithDefaultXmlConfigAndConfigurationClassGeneration() {
		ContextConfigurationAttributes configAttributes = new ContextConfigurationAttributes(
				ImproperDuplicateDefaultXmlAndConfigClassTestCase.class, EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY,
				true, null, true, ContextLoader.class);
		assertThatIllegalStateException().isThrownBy(() ->
					loader.processContextConfiguration(configAttributes))
			.withMessageContaining("both default locations AND default configuration classes were detected");
	}

	@Test
	void processContextConfigurationWithLocation() {
		String[] locations = new String[] {"classpath:/foo.xml"};
		ContextConfigurationAttributes configAttributes = new ContextConfigurationAttributes(
				getClass(), locations, EMPTY_CLASS_ARRAY, true, null, true, ContextLoader.class);
		loader.processContextConfiguration(configAttributes);
		assertThat(configAttributes.getLocations()).isEqualTo(locations);
		assertEmpty(configAttributes.getClasses());
	}

	@Test
	void processContextConfigurationWithConfigurationClass() {
		Class<?>[] classes = new Class<?>[] {getClass()};
		ContextConfigurationAttributes configAttributes = new ContextConfigurationAttributes(
				getClass(), EMPTY_STRING_ARRAY, classes, true, null, true, ContextLoader.class);
		loader.processContextConfiguration(configAttributes);
		assertThat(configAttributes.getClasses()).isEqualTo(classes);
		assertEmpty(configAttributes.getLocations());
	}

	// --- SmartContextLoader - loadContext() ----------------------------------

	@Test
	void loadContextWithNullConfig() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				loader.loadContext((MergedContextConfiguration) null));
	}

	@Test
	void loadContextWithoutLocationsAndConfigurationClasses() throws Exception {
		MergedContextConfiguration mergedConfig = new MergedContextConfiguration(
				getClass(), EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
		assertThatIllegalStateException().isThrownBy(() ->
				loader.loadContext(mergedConfig))
			.withMessageStartingWith("Neither")
			.withMessageContaining("was able to load an ApplicationContext from");
	}

	/**
	 * @since 4.1
	 */
	@Test
	void loadContextWithLocationsAndConfigurationClasses() throws Exception {
		MergedContextConfiguration mergedConfig = new MergedContextConfiguration(getClass(),
				new String[] {"test.xml"}, new Class<?>[] {getClass()}, EMPTY_STRING_ARRAY, loader);
		assertThatIllegalStateException().isThrownBy(() ->
				loader.loadContext(mergedConfig))
			.withMessageStartingWith("Neither")
			.withMessageContaining("declare either 'locations' or 'classes' but not both.");
	}

	private void assertApplicationContextLoadsAndContainsFooString(MergedContextConfiguration mergedConfig)
			throws Exception {

		ApplicationContext applicationContext = loader.loadContext(mergedConfig);
		assertThat(applicationContext).isNotNull();
		assertThat(applicationContext.getBean(String.class)).isEqualTo("foo");
		boolean condition = applicationContext instanceof ConfigurableApplicationContext;
		assertThat(condition).isTrue();
		((ConfigurableApplicationContext) applicationContext).close();
	}

	@Test
	void loadContextWithXmlConfig() throws Exception {
		MergedContextConfiguration mergedConfig = new MergedContextConfiguration(
				XmlTestCase.class,
				new String[] {"classpath:/org/springframework/test/context/support/DelegatingSmartContextLoaderTests$XmlTestCase-context.xml"},
				EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
		assertApplicationContextLoadsAndContainsFooString(mergedConfig);
	}

	@Test
	void loadContextWithConfigurationClass() throws Exception {
		MergedContextConfiguration mergedConfig = new MergedContextConfiguration(ConfigClassTestCase.class,
				EMPTY_STRING_ARRAY, new Class<?>[] {ConfigClassTestCase.Config.class}, EMPTY_STRING_ARRAY, loader);
		assertApplicationContextLoadsAndContainsFooString(mergedConfig);
	}

	// --- ContextLoader -------------------------------------------------------

	@Test
	void processLocations() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				loader.processLocations(getClass(), EMPTY_STRING_ARRAY));
	}

	@Test
	void loadContextFromLocations() throws Exception {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				loader.loadContext(EMPTY_STRING_ARRAY));
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
