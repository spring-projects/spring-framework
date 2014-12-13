/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.test.context.support;

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
public class BootstrapTestUtilsContextInitializerTests extends AbstractContextConfigurationUtilsTests {

	@Test
	public void buildMergedConfigWithLocalInitializer() {
		Class<?> testClass = InitializersFoo.class;
		Class<?>[] expectedClasses = new Class<?>[] { FooConfig.class };
		Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> expectedInitializerClasses//
		= new HashSet<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>>();
		expectedInitializerClasses.add(FooInitializer.class);

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses, expectedInitializerClasses,
			DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithLocalAndInheritedInitializer() {
		Class<?> testClass = InitializersBar.class;
		Class<?>[] expectedClasses = new Class<?>[] { FooConfig.class, BarConfig.class };
		Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> expectedInitializerClasses//
		= new HashSet<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>>();
		expectedInitializerClasses.add(FooInitializer.class);
		expectedInitializerClasses.add(BarInitializer.class);

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses, expectedInitializerClasses,
			DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithOverriddenInitializers() {
		Class<?> testClass = OverriddenInitializersBar.class;
		Class<?>[] expectedClasses = new Class<?>[] { FooConfig.class, BarConfig.class };
		Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> expectedInitializerClasses//
		= new HashSet<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>>();
		expectedInitializerClasses.add(BarInitializer.class);

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses, expectedInitializerClasses,
			DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithOverriddenInitializersAndClasses() {
		Class<?> testClass = OverriddenInitializersAndClassesBar.class;
		Class<?>[] expectedClasses = new Class<?>[] { BarConfig.class };
		Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> expectedInitializerClasses//
		= new HashSet<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>>();
		expectedInitializerClasses.add(BarInitializer.class);

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses, expectedInitializerClasses,
			DelegatingSmartContextLoader.class);
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
