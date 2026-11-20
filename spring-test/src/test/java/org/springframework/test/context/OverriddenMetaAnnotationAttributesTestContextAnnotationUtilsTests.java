/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
 * Tests for {@link TestContextAnnotationUtils} that verify support for
 * overridden meta-annotation attributes.
 *
 * <p>See <a href="https://jira.spring.io/browse/SPR-10181">SPR-10181</a>.
 *
 * @author Sam Brannen
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
