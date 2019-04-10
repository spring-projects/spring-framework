/*
 * Copyright 2002-2017 the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.Set;

import org.mockito.Mockito;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.BootstrapContext;
import org.springframework.test.context.BootstrapTestUtils;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.Assert.*;

/**
 * Abstract base class for tests involving {@link ContextLoaderUtils},
 * {@link BootstrapTestUtils}, and {@link ActiveProfilesUtils}.
 *
 * @author Sam Brannen
 * @since 3.1
 */
abstract class AbstractContextConfigurationUtilsTests {

	static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

	static final String[] EMPTY_STRING_ARRAY = new String[0];

	static final Set<Class<? extends ApplicationContextInitializer<?>>>
			EMPTY_INITIALIZER_CLASSES = Collections.<Class<? extends ApplicationContextInitializer<?>>> emptySet();


	MergedContextConfiguration buildMergedContextConfiguration(Class<?> testClass) {
		CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate = Mockito.mock(CacheAwareContextLoaderDelegate.class);
		BootstrapContext bootstrapContext = BootstrapTestUtils.buildBootstrapContext(testClass, cacheAwareContextLoaderDelegate);
		TestContextBootstrapper bootstrapper = BootstrapTestUtils.resolveTestContextBootstrapper(bootstrapContext);
		return bootstrapper.buildMergedContextConfiguration();
	}

	void assertAttributes(ContextConfigurationAttributes attributes, Class<?> expectedDeclaringClass,
			String[] expectedLocations, Class<?>[] expectedClasses,
			Class<? extends ContextLoader> expectedContextLoaderClass, boolean expectedInheritLocations) {

		assertEquals("declaring class", expectedDeclaringClass, attributes.getDeclaringClass());
		assertArrayEquals("locations", expectedLocations, attributes.getLocations());
		assertArrayEquals("classes", expectedClasses, attributes.getClasses());
		assertEquals("inherit locations", expectedInheritLocations, attributes.isInheritLocations());
		assertEquals("context loader", expectedContextLoaderClass, attributes.getContextLoaderClass());
	}

	void assertMergedConfig(MergedContextConfiguration mergedConfig, Class<?> expectedTestClass,
			String[] expectedLocations, Class<?>[] expectedClasses,
			Class<? extends ContextLoader> expectedContextLoaderClass) {

		assertMergedConfig(mergedConfig, expectedTestClass, expectedLocations, expectedClasses,
				EMPTY_INITIALIZER_CLASSES, expectedContextLoaderClass);
	}

	void assertMergedConfig(
			MergedContextConfiguration mergedConfig,
			Class<?> expectedTestClass,
			String[] expectedLocations,
			Class<?>[] expectedClasses,
			Set<Class<? extends ApplicationContextInitializer<?>>> expectedInitializerClasses,
			Class<? extends ContextLoader> expectedContextLoaderClass) {

		assertNotNull(mergedConfig);
		assertEquals(expectedTestClass, mergedConfig.getTestClass());
		assertNotNull(mergedConfig.getLocations());
		assertArrayEquals(expectedLocations, mergedConfig.getLocations());
		assertNotNull(mergedConfig.getClasses());
		assertArrayEquals(expectedClasses, mergedConfig.getClasses());
		assertNotNull(mergedConfig.getActiveProfiles());
		if (expectedContextLoaderClass == null) {
			assertNull(mergedConfig.getContextLoader());
		}
		else {
			assertEquals(expectedContextLoaderClass, mergedConfig.getContextLoader().getClass());
		}
		assertNotNull(mergedConfig.getContextInitializerClasses());
		assertEquals(expectedInitializerClasses, mergedConfig.getContextInitializerClasses());
	}

	@SafeVarargs
	static <T> T[] array(T... objects) {
		return objects;
	}


	static class Enigma {
	}

	@ContextConfiguration
	@ActiveProfiles
	static class BareAnnotations {
	}

	@Configuration
	static class FooConfig {
	}

	@Configuration
	static class BarConfig {
	}

	@ContextConfiguration("/foo.xml")
	@ActiveProfiles(profiles = "foo")
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface MetaLocationsFooConfig {
	}

	@ContextConfiguration
	@ActiveProfiles
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface MetaLocationsFooConfigWithOverrides {

		String[] locations() default "/foo.xml";

		String[] profiles() default "foo";
	}

	@ContextConfiguration("/bar.xml")
	@ActiveProfiles(profiles = "bar")
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface MetaLocationsBarConfig {
	}

	@MetaLocationsFooConfig
	static class MetaLocationsFoo {
	}

	@MetaLocationsBarConfig
	static class MetaLocationsBar extends MetaLocationsFoo {
	}

	@MetaLocationsFooConfigWithOverrides
	static class MetaLocationsFooWithOverrides {
	}

	@MetaLocationsFooConfigWithOverrides(locations = {"foo1.xml", "foo2.xml"}, profiles = {"foo1", "foo2"})
	static class MetaLocationsFooWithOverriddenAttributes {
	}

	@ContextConfiguration(locations = "/foo.xml", inheritLocations = false)
	@ActiveProfiles("foo")
	static class LocationsFoo {
	}

	@ContextConfiguration(classes = FooConfig.class, inheritLocations = false)
	@ActiveProfiles("foo")
	static class ClassesFoo {
	}

	@WebAppConfiguration
	static class WebClassesFoo extends ClassesFoo {
	}

	@ContextConfiguration(locations = "/bar.xml", inheritLocations = true, loader = AnnotationConfigContextLoader.class)
	@ActiveProfiles("bar")
	static class LocationsBar extends LocationsFoo {
	}

	@ContextConfiguration(locations = "/bar.xml", inheritLocations = false, loader = AnnotationConfigContextLoader.class)
	@ActiveProfiles("bar")
	static class OverriddenLocationsBar extends LocationsFoo {
	}

	@ContextConfiguration(classes = BarConfig.class, inheritLocations = true, loader = AnnotationConfigContextLoader.class)
	@ActiveProfiles("bar")
	static class ClassesBar extends ClassesFoo {
	}

	@ContextConfiguration(classes = BarConfig.class, inheritLocations = false, loader = AnnotationConfigContextLoader.class)
	@ActiveProfiles("bar")
	static class OverriddenClassesBar extends ClassesFoo {
	}

	@ContextConfiguration(locations = "/foo.properties", loader = GenericPropertiesContextLoader.class)
	@ActiveProfiles("foo")
	static class PropertiesLocationsFoo {
	}

	// Combining @Configuration classes with a Properties based loader doesn't really make
	// sense, but that's OK for unit testing purposes.
	@ContextConfiguration(classes = FooConfig.class, loader = GenericPropertiesContextLoader.class)
	@ActiveProfiles("foo")
	static class PropertiesClassesFoo {
	}

}
