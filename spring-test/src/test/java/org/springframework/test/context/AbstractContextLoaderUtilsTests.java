/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.test.context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.Set;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.junit.Assert.*;

/**
 * Abstract base class for tests involving {@link ContextLoaderUtils}.
 *
 * @author Sam Brannen
 * @since 3.1
 */
abstract class AbstractContextLoaderUtilsTests {

	static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
	static final String[] EMPTY_STRING_ARRAY = new String[0];
	static final Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> EMPTY_INITIALIZER_CLASSES = //
	Collections.<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> emptySet();


	void assertAttributes(ContextConfigurationAttributes attributes, Class<?> expectedDeclaringClass,
			String[] expectedLocations, Class<?>[] expectedClasses,
			Class<? extends ContextLoader> expectedContextLoaderClass, boolean expectedInheritLocations) {
		assertEquals(expectedDeclaringClass, attributes.getDeclaringClass());
		assertArrayEquals(expectedLocations, attributes.getLocations());
		assertArrayEquals(expectedClasses, attributes.getClasses());
		assertEquals(expectedInheritLocations, attributes.isInheritLocations());
		assertEquals(expectedContextLoaderClass, attributes.getContextLoaderClass());
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
			Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> expectedInitializerClasses,
			Class<? extends ContextLoader> expectedContextLoaderClass) {
		assertNotNull(mergedConfig);
		assertEquals(expectedTestClass, mergedConfig.getTestClass());
		assertNotNull(mergedConfig.getLocations());
		assertArrayEquals(expectedLocations, mergedConfig.getLocations());
		assertNotNull(mergedConfig.getClasses());
		assertArrayEquals(expectedClasses, mergedConfig.getClasses());
		assertNotNull(mergedConfig.getActiveProfiles());
		assertEquals(expectedContextLoaderClass, mergedConfig.getContextLoader().getClass());
		assertNotNull(mergedConfig.getContextInitializerClasses());
		assertEquals(expectedInitializerClasses, mergedConfig.getContextInitializerClasses());
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

	@ContextConfiguration(locations = "/foo.xml", inheritLocations = false)
	@ActiveProfiles(profiles = "foo")
	static class LocationsFoo {
	}

	@ContextConfiguration(classes = FooConfig.class, inheritLocations = false)
	@ActiveProfiles(profiles = "foo")
	static class ClassesFoo {
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

}
