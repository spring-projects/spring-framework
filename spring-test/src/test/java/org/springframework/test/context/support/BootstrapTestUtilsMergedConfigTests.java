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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.test.context.BootstrapTestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.web.WebDelegatingSmartContextLoader;
import org.springframework.test.context.web.WebMergedContextConfiguration;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Unit tests for {@link BootstrapTestUtils} involving {@link MergedContextConfiguration}.
 *
 * @author Sam Brannen
 * @since 3.1
 */
public class BootstrapTestUtilsMergedConfigTests extends AbstractContextConfigurationUtilsTests {

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void buildImplicitMergedConfigWithoutAnnotation() {
		Class<?> testClass = Enigma.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, DelegatingSmartContextLoader.class);
	}

	/**
	 * @since 4.3
	 */
	@Test
	public void buildMergedConfigWithContextConfigurationWithoutLocationsClassesOrInitializers() {
		exception.expect(IllegalStateException.class);
		exception.expectMessage(startsWith("DelegatingSmartContextLoader was unable to detect defaults, "
				+ "and no ApplicationContextInitializers or ContextCustomizers were declared for context configuration attributes"));

		buildMergedContextConfiguration(MissingContextAttributesTestCase.class);
	}

	@Test
	public void buildMergedConfigWithBareAnnotations() {
		Class<?> testClass = BareAnnotations.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(
			mergedConfig,
			testClass,
			new String[] { "classpath:org/springframework/test/context/support/AbstractContextConfigurationUtilsTests$BareAnnotations-context.xml" },
			EMPTY_CLASS_ARRAY, DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithLocalAnnotationAndLocations() {
		Class<?> testClass = LocationsFoo.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, new String[] { "classpath:/foo.xml" }, EMPTY_CLASS_ARRAY,
			DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithMetaAnnotationAndLocations() {
		Class<?> testClass = MetaLocationsFoo.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, new String[] { "classpath:/foo.xml" }, EMPTY_CLASS_ARRAY,
			DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithMetaAnnotationAndClasses() {
		buildMergedConfigWithMetaAnnotationAndClasses(Dog.class);
		buildMergedConfigWithMetaAnnotationAndClasses(WorkingDog.class);
		buildMergedConfigWithMetaAnnotationAndClasses(GermanShepherd.class);
	}

	private void buildMergedConfigWithMetaAnnotationAndClasses(Class<?> testClass) {
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);
		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, new Class<?>[] { FooConfig.class,
			BarConfig.class }, DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithLocalAnnotationAndClasses() {
		Class<?> testClass = ClassesFoo.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, new Class<?>[] { FooConfig.class },
			DelegatingSmartContextLoader.class);
	}

	/**
	 * Introduced to investigate claims made in a discussion on
	 * <a href="https://stackoverflow.com/questions/24725438/what-could-cause-a-class-implementing-applicationlistenercontextrefreshedevent">Stack Overflow</a>.
	 */
	@Test
	public void buildMergedConfigWithAtWebAppConfigurationWithAnnotationAndClassesOnSuperclass() {
		Class<?> webTestClass = WebClassesFoo.class;
		Class<?> standardTestClass = ClassesFoo.class;
		WebMergedContextConfiguration webMergedConfig = (WebMergedContextConfiguration) buildMergedContextConfiguration(webTestClass);
		MergedContextConfiguration standardMergedConfig = buildMergedContextConfiguration(standardTestClass);

		assertEquals(webMergedConfig, webMergedConfig);
		assertEquals(standardMergedConfig, standardMergedConfig);
		assertNotEquals(standardMergedConfig, webMergedConfig);
		assertNotEquals(webMergedConfig, standardMergedConfig);

		assertMergedConfig(webMergedConfig, webTestClass, EMPTY_STRING_ARRAY, new Class<?>[] { FooConfig.class },
			WebDelegatingSmartContextLoader.class);
		assertMergedConfig(standardMergedConfig, standardTestClass, EMPTY_STRING_ARRAY,
			new Class<?>[] { FooConfig.class }, DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithLocalAnnotationAndOverriddenContextLoaderAndLocations() {
		Class<?> testClass = PropertiesLocationsFoo.class;
		Class<? extends ContextLoader> expectedContextLoaderClass = GenericPropertiesContextLoader.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, new String[] { "classpath:/foo.properties" }, EMPTY_CLASS_ARRAY,
			expectedContextLoaderClass);
	}

	@Test
	public void buildMergedConfigWithLocalAnnotationAndOverriddenContextLoaderAndClasses() {
		Class<?> testClass = PropertiesClassesFoo.class;
		Class<? extends ContextLoader> expectedContextLoaderClass = GenericPropertiesContextLoader.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, new Class<?>[] { FooConfig.class },
			expectedContextLoaderClass);
	}

	@Test
	public void buildMergedConfigWithLocalAndInheritedAnnotationsAndLocations() {
		Class<?> testClass = LocationsBar.class;
		String[] expectedLocations = new String[] { "/foo.xml", "/bar.xml" };
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, expectedLocations, EMPTY_CLASS_ARRAY,
			AnnotationConfigContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithLocalAndInheritedAnnotationsAndClasses() {
		Class<?> testClass = ClassesBar.class;
		Class<?>[] expectedClasses = new Class<?>[] { FooConfig.class, BarConfig.class };
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses,
			AnnotationConfigContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithAnnotationsAndOverriddenLocations() {
		Class<?> testClass = OverriddenLocationsBar.class;
		String[] expectedLocations = new String[] { "/bar.xml" };
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, expectedLocations, EMPTY_CLASS_ARRAY,
			AnnotationConfigContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithAnnotationsAndOverriddenClasses() {
		Class<?> testClass = OverriddenClassesBar.class;
		Class<?>[] expectedClasses = new Class<?>[] { BarConfig.class };
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses,
			AnnotationConfigContextLoader.class);
	}


	@ContextConfiguration
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface SpringAppConfig {

		Class<?>[] classes() default {};
	}

	@SpringAppConfig(classes = { FooConfig.class, BarConfig.class })
	public static abstract class Dog {
	}

	public static abstract class WorkingDog extends Dog {
	}

	public static class GermanShepherd extends WorkingDog {
	}

	@ContextConfiguration
	static class MissingContextAttributesTestCase {
	}

}
