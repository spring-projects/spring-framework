/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.BootstrapTestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * Unit tests for {@link BootstrapTestUtils} involving {@link ApplicationContextInitializer}s.
 *
 * @author Sam Brannen
 * @since 3.1
 */
@SuppressWarnings("unchecked")
public class BootstrapTestUtilsContextInitializerTests extends AbstractContextConfigurationUtilsTests {

	@Test
	public void buildMergedConfigWithSingleLocalInitializer() {
		Class<?> testClass = SingleInitializer.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY,
			initializers(FooInitializer.class), DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithLocalInitializerAndConfigClass() {
		Class<?> testClass = InitializersFoo.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, classes(FooConfig.class),
			initializers(FooInitializer.class), DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithLocalAndInheritedInitializer() {
		Class<?> testClass = InitializersBar.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, classes(FooConfig.class, BarConfig.class),
			initializers(FooInitializer.class, BarInitializer.class), DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithOverriddenInitializers() {
		Class<?> testClass = OverriddenInitializersBar.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, classes(FooConfig.class, BarConfig.class),
			initializers(BarInitializer.class), DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithOverriddenInitializersAndClasses() {
		Class<?> testClass = OverriddenInitializersAndClassesBar.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, classes(BarConfig.class),
			initializers(BarInitializer.class), DelegatingSmartContextLoader.class);
	}

	private Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> initializers(
			Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>... classes) {

		return new HashSet<>(Arrays.asList(classes));
	}

	private Class<?>[] classes(Class<?>... classes) {
		return classes;
	}


	private static class FooInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

		@Override
		public void initialize(GenericApplicationContext applicationContext) {
		}
	}

	private static class BarInitializer implements ApplicationContextInitializer<GenericWebApplicationContext> {

		@Override
		public void initialize(GenericWebApplicationContext applicationContext) {
		}
	}

	@ContextConfiguration(initializers = FooInitializer.class)
	private static class SingleInitializer {
	}

	@ContextConfiguration(classes = FooConfig.class, initializers = FooInitializer.class)
	private static class InitializersFoo {
	}

	@ContextConfiguration(classes = BarConfig.class, initializers = BarInitializer.class)
	private static class InitializersBar extends InitializersFoo {
	}

	@ContextConfiguration(classes = BarConfig.class, initializers = BarInitializer.class, inheritInitializers = false)
	private static class OverriddenInitializersBar extends InitializersFoo {
	}

	@ContextConfiguration(classes = BarConfig.class, inheritLocations = false, initializers = BarInitializer.class, inheritInitializers = false)
	private static class OverriddenInitializersAndClassesBar extends InitializersFoo {
	}

}
