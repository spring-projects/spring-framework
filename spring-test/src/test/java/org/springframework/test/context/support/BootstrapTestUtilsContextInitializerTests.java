/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.support;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.BootstrapTestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * Tests for {@link BootstrapTestUtils} involving {@link ApplicationContextInitializer}s.
 *
 * @author Sam Brannen
 * @since 3.1
 */
@SuppressWarnings("unchecked")
class BootstrapTestUtilsContextInitializerTests extends AbstractContextConfigurationUtilsTests {

	@Test
	void buildMergedConfigWithSingleLocalInitializer() {
		Class<?> testClass = SingleInitializer.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY,
			initializers(FooInitializer.class), DelegatingSmartContextLoader.class);
	}

	@Test
	void buildMergedConfigWithLocalInitializerAndConfigClass() {
		Class<?> testClass = InitializersFoo.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, classes(FooConfig.class),
			initializers(FooInitializer.class), DelegatingSmartContextLoader.class);
	}

	@Test
	void buildMergedConfigWithLocalAndInheritedInitializer() {
		Class<?> testClass = InitializersBar.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, classes(FooConfig.class, BarConfig.class),
			initializers(FooInitializer.class, BarInitializer.class), DelegatingSmartContextLoader.class);
	}

	@Test
	void buildMergedConfigWithOverriddenInitializers() {
		Class<?> testClass = OverriddenInitializersBar.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, classes(FooConfig.class, BarConfig.class),
			initializers(BarInitializer.class), DelegatingSmartContextLoader.class);
	}

	@Test
	void buildMergedConfigWithOverriddenInitializersAndClasses() {
		Class<?> testClass = OverriddenInitializersAndClassesBar.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, classes(BarConfig.class),
			initializers(BarInitializer.class), DelegatingSmartContextLoader.class);
	}

	private Set<Class<? extends ApplicationContextInitializer<?>>> initializers(
			Class<? extends ApplicationContextInitializer<?>>... classes) {

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
