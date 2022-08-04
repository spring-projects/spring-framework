/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.MetaAnnotationUtils.AnnotationDescriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.MetaAnnotationUtils.findAnnotationDescriptor;

/**
 * Unit tests for {@link MetaAnnotationUtils} that verify support for overridden
 * meta-annotation attributes.
 *
 * <p>See <a href="https://jira.spring.io/browse/SPR-10181">SPR-10181</a>.
 *
 * @author Sam Brannen
 * @since 4.0
 * @see MetaAnnotationUtilsTests
 */
@SuppressWarnings("deprecation")
class OverriddenMetaAnnotationAttributesTests {

	@Test
	void contextConfigurationValue() throws Exception {
		Class<MetaValueConfigTestCase> declaringClass = MetaValueConfigTestCase.class;
		AnnotationDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(declaringClass,
			ContextConfiguration.class);
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(declaringClass);
		assertThat(descriptor.getComposedAnnotationType()).isEqualTo(MetaValueConfig.class);
		assertThat(descriptor.getAnnotationType()).isEqualTo(ContextConfiguration.class);
		assertThat(descriptor.getComposedAnnotation()).isNotNull();
		assertThat(descriptor.getComposedAnnotationType()).isEqualTo(MetaValueConfig.class);

		// direct access to annotation value:
		assertThat(descriptor.getAnnotation().value()).isEqualTo(new String[] { "foo.xml" });
	}

	@Test
	void overriddenContextConfigurationValue() throws Exception {
		Class<?> declaringClass = OverriddenMetaValueConfigTestCase.class;
		AnnotationDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(declaringClass,
			ContextConfiguration.class);
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(declaringClass);
		assertThat(descriptor.getComposedAnnotationType()).isEqualTo(MetaValueConfig.class);
		assertThat(descriptor.getAnnotationType()).isEqualTo(ContextConfiguration.class);
		assertThat(descriptor.getComposedAnnotation()).isNotNull();
		assertThat(descriptor.getComposedAnnotationType()).isEqualTo(MetaValueConfig.class);

		// direct access to annotation value:
		assertThat(descriptor.getAnnotation().value()).isEqualTo(new String[] { "foo.xml" });

		// overridden attribute:
		AnnotationAttributes attributes = descriptor.getAnnotationAttributes();

		// NOTE: we would like to be able to override the 'value' attribute; however,
		// Spring currently does not allow overrides for the 'value' attribute.
		// See SPR-11393 for related discussions.
		assertThat(attributes.getStringArray("value")).isEqualTo(new String[] { "foo.xml" });
	}

	@Test
	void contextConfigurationLocationsAndInheritLocations() throws Exception {
		Class<MetaLocationsConfigTestCase> declaringClass = MetaLocationsConfigTestCase.class;
		AnnotationDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(declaringClass,
			ContextConfiguration.class);
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(declaringClass);
		assertThat(descriptor.getComposedAnnotationType()).isEqualTo(MetaLocationsConfig.class);
		assertThat(descriptor.getAnnotationType()).isEqualTo(ContextConfiguration.class);
		assertThat(descriptor.getComposedAnnotation()).isNotNull();
		assertThat(descriptor.getComposedAnnotationType()).isEqualTo(MetaLocationsConfig.class);

		// direct access to annotation attributes:
		assertThat(descriptor.getAnnotation().locations()).isEqualTo(new String[] { "foo.xml" });
		assertThat(descriptor.getAnnotation().inheritLocations()).isFalse();
	}

	@Test
	void overriddenContextConfigurationLocationsAndInheritLocations() throws Exception {
		Class<?> declaringClass = OverriddenMetaLocationsConfigTestCase.class;
		AnnotationDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(declaringClass,
			ContextConfiguration.class);
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(declaringClass);
		assertThat(descriptor.getComposedAnnotationType()).isEqualTo(MetaLocationsConfig.class);
		assertThat(descriptor.getAnnotationType()).isEqualTo(ContextConfiguration.class);
		assertThat(descriptor.getComposedAnnotation()).isNotNull();
		assertThat(descriptor.getComposedAnnotationType()).isEqualTo(MetaLocationsConfig.class);

		// direct access to annotation attributes:
		assertThat(descriptor.getAnnotation().locations()).isEqualTo(new String[] { "foo.xml" });
		assertThat(descriptor.getAnnotation().inheritLocations()).isFalse();

		// overridden attributes:
		AnnotationAttributes attributes = descriptor.getAnnotationAttributes();
		assertThat(attributes.getStringArray("locations")).isEqualTo(new String[] { "bar.xml" });
		assertThat(attributes.getBoolean("inheritLocations")).isTrue();
	}


	// -------------------------------------------------------------------------

	@ContextConfiguration("foo.xml")
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaValueConfig {

		String[] value() default {};
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

		String[] locations() default {};

		boolean inheritLocations();
	}

	@MetaLocationsConfig(inheritLocations = true)
	static class MetaLocationsConfigTestCase {
	}

	@MetaLocationsConfig(locations = "bar.xml", inheritLocations = true)
	static class OverriddenMetaLocationsConfigTestCase {
	}

}
