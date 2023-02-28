/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.context;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.TestContextAnnotationUtils.AnnotationDescriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.TestContextAnnotationUtils.findAnnotationDescriptor;

/**
 * Unit tests for {@link TestContextAnnotationUtils} that verify support for
 * overridden meta-annotation attributes.
 *
 * <p>See <a href="https://jira.spring.io/browse/SPR-10181">SPR-10181</a>.
 *
 * @author Sam Brannen
 * @since 5.3, though originally since 4.0 for the deprecated
 * {@link org.springframework.test.util.MetaAnnotationUtils} support
 * @see TestContextAnnotationUtilsTests
 */
class OverriddenMetaAnnotationAttributesTestContextAnnotationUtilsTests {

	@Test
	void contextConfigurationValue() {
		Class<?> rootDeclaringClass = MetaValueConfigTestCase.class;
		AnnotationDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(rootDeclaringClass,
			ContextConfiguration.class);
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(rootDeclaringClass);
		assertThat(descriptor.getDeclaringClass()).isEqualTo(MetaValueConfig.class);
		assertThat(descriptor.getAnnotationType()).isEqualTo(ContextConfiguration.class);
		assertThat(descriptor.getAnnotation().value()).containsExactly("foo.xml");
		assertThat(descriptor.getAnnotation().locations()).containsExactly("foo.xml");
	}

	@Test
	void overriddenContextConfigurationValue() {
		Class<?> rootDeclaringClass = OverriddenMetaValueConfigTestCase.class;
		AnnotationDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(rootDeclaringClass,
			ContextConfiguration.class);
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(rootDeclaringClass);
		assertThat(descriptor.getDeclaringClass()).isEqualTo(MetaValueConfig.class);
		assertThat(descriptor.getAnnotationType()).isEqualTo(ContextConfiguration.class);
		assertThat(descriptor.getAnnotation().value()).containsExactly("bar.xml");
		assertThat(descriptor.getAnnotation().locations()).containsExactly("bar.xml");
	}

	@Test
	void contextConfigurationLocationsAndInheritLocations() {
		Class<?> rootDeclaringClass = MetaLocationsConfigTestCase.class;
		AnnotationDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(rootDeclaringClass,
			ContextConfiguration.class);
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(rootDeclaringClass);
		assertThat(descriptor.getDeclaringClass()).isEqualTo(MetaLocationsConfig.class);
		assertThat(descriptor.getAnnotationType()).isEqualTo(ContextConfiguration.class);
		assertThat(descriptor.getAnnotation().value()).isEmpty();
		assertThat(descriptor.getAnnotation().locations()).isEmpty();
		assertThat(descriptor.getAnnotation().inheritLocations()).isTrue();
	}

	@Test
	void overriddenContextConfigurationLocationsAndInheritLocations() {
		Class<?> rootDeclaringClass = OverriddenMetaLocationsConfigTestCase.class;
		AnnotationDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(rootDeclaringClass,
			ContextConfiguration.class);
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(rootDeclaringClass);
		assertThat(descriptor.getDeclaringClass()).isEqualTo(MetaLocationsConfig.class);
		assertThat(descriptor.getAnnotationType()).isEqualTo(ContextConfiguration.class);
		assertThat(descriptor.getAnnotation().value()).containsExactly("bar.xml");
		assertThat(descriptor.getAnnotation().locations()).containsExactly("bar.xml");
		assertThat(descriptor.getAnnotation().inheritLocations()).isTrue();
	}


	// -------------------------------------------------------------------------

	@ContextConfiguration
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaValueConfig {

		@AliasFor(annotation = ContextConfiguration.class)
		String[] value() default "foo.xml";
	}

	@MetaValueConfig
	static class MetaValueConfigTestCase {
	}

	@MetaValueConfig("bar.xml")
	static class OverriddenMetaValueConfigTestCase {
	}

	@ContextConfiguration(locations = "foo.xml", inheritLocations = false)
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaLocationsConfig {

		@AliasFor(annotation = ContextConfiguration.class)
		String[] locations() default {};

		@AliasFor(annotation = ContextConfiguration.class)
		boolean inheritLocations();
	}

	@MetaLocationsConfig(inheritLocations = true)
	static class MetaLocationsConfigTestCase {
	}

	@MetaLocationsConfig(locations = "bar.xml", inheritLocations = true)
	static class OverriddenMetaLocationsConfigTestCase {
	}

}
