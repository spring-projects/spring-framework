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

import org.junit.Test;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.support.DelegatingSmartContextLoader;
import org.springframework.test.context.support.GenericPropertiesContextLoader;

import static org.springframework.test.context.ContextLoaderUtils.*;

/**
 * Unit tests for {@link ContextLoaderUtils} involving {@link MergedContextConfiguration}.
 *
 * @author Sam Brannen
 * @since 3.1
 */
public class ContextLoaderUtilsMergedConfigTests extends AbstractContextLoaderUtilsTests {

	@Test(expected = IllegalArgumentException.class)
	public void buildMergedConfigWithoutAnnotation() {
		buildMergedContextConfiguration(Enigma.class, null, null);
	}

	@Test
	public void buildMergedConfigWithBareAnnotations() {
		Class<BareAnnotations> testClass = BareAnnotations.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null, null);

		assertMergedConfig(
			mergedConfig,
			testClass,
			new String[] { "classpath:/org/springframework/test/context/AbstractContextLoaderUtilsTests$BareAnnotations-context.xml" },
			EMPTY_CLASS_ARRAY, DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithLocalAnnotationAndLocations() {
		Class<?> testClass = LocationsFoo.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null, null);

		assertMergedConfig(mergedConfig, testClass, new String[] { "classpath:/foo.xml" }, EMPTY_CLASS_ARRAY,
			DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithLocalAnnotationAndClasses() {
		Class<?> testClass = ClassesFoo.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null, null);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, new Class<?>[] { FooConfig.class },
			DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithLocalAnnotationAndOverriddenContextLoaderAndLocations() {
		Class<?> testClass = LocationsFoo.class;
		Class<? extends ContextLoader> expectedContextLoaderClass = GenericPropertiesContextLoader.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass,
			expectedContextLoaderClass.getName(), null);

		assertMergedConfig(mergedConfig, testClass, new String[] { "classpath:/foo.xml" }, EMPTY_CLASS_ARRAY,
			expectedContextLoaderClass);
	}

	@Test
	public void buildMergedConfigWithLocalAnnotationAndOverriddenContextLoaderAndClasses() {
		Class<?> testClass = ClassesFoo.class;
		Class<? extends ContextLoader> expectedContextLoaderClass = GenericPropertiesContextLoader.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass,
			expectedContextLoaderClass.getName(), null);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, new Class<?>[] { FooConfig.class },
			expectedContextLoaderClass);
	}

	@Test
	public void buildMergedConfigWithLocalAndInheritedAnnotationsAndLocations() {
		Class<?> testClass = LocationsBar.class;
		String[] expectedLocations = new String[] { "/foo.xml", "/bar.xml" };

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null, null);
		assertMergedConfig(mergedConfig, testClass, expectedLocations, EMPTY_CLASS_ARRAY,
			AnnotationConfigContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithLocalAndInheritedAnnotationsAndClasses() {
		Class<?> testClass = ClassesBar.class;
		Class<?>[] expectedClasses = new Class<?>[] { FooConfig.class, BarConfig.class };

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null, null);
		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses,
			AnnotationConfigContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithAnnotationsAndOverriddenLocations() {
		Class<?> testClass = OverriddenLocationsBar.class;
		String[] expectedLocations = new String[] { "/bar.xml" };

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null, null);
		assertMergedConfig(mergedConfig, testClass, expectedLocations, EMPTY_CLASS_ARRAY,
			AnnotationConfigContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithAnnotationsAndOverriddenClasses() {
		Class<?> testClass = OverriddenClassesBar.class;
		Class<?>[] expectedClasses = new Class<?>[] { BarConfig.class };

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null, null);
		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses,
			AnnotationConfigContextLoader.class);
	}

}
