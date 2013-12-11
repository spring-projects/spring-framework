/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Test;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.test.context.MetaAnnotationUtils.AnnotationDescriptor;

import static org.junit.Assert.*;
import static org.springframework.test.context.MetaAnnotationUtils.*;

/**
 * Unit tests for {@link MetaAnnotationUtils} that verify support for overridden
 * meta-annotation attributes.
 *
 * <p>See <a href="https://jira.springsource.org/browse/SPR-10181">SPR-10181</a>.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class OverriddenMetaAnnotationAttributesTests {

	@Test
	public void contextConfigurationValue() throws Exception {
		Class<MetaValueConfigTest> declaringClass = MetaValueConfigTest.class;
		AnnotationDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(declaringClass,
			ContextConfiguration.class);
		assertNotNull(descriptor);
		assertEquals(declaringClass, descriptor.getRootDeclaringClass());
		assertEquals(MetaValueConfig.class, descriptor.getComposedAnnotationType());
		assertEquals(ContextConfiguration.class, descriptor.getAnnotationType());
		assertNotNull(descriptor.getComposedAnnotation());
		assertEquals(MetaValueConfig.class, descriptor.getComposedAnnotationType());

		// direct access to annotation value:
		assertArrayEquals(new String[] { "foo.xml" }, descriptor.getAnnotation().value());
	}

	@Test
	public void overriddenContextConfigurationValue() throws Exception {
		Class<?> declaringClass = OverriddenMetaValueConfigTest.class;
		AnnotationDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(declaringClass,
			ContextConfiguration.class);
		assertNotNull(descriptor);
		assertEquals(declaringClass, descriptor.getRootDeclaringClass());
		assertEquals(MetaValueConfig.class, descriptor.getComposedAnnotationType());
		assertEquals(ContextConfiguration.class, descriptor.getAnnotationType());
		assertNotNull(descriptor.getComposedAnnotation());
		assertEquals(MetaValueConfig.class, descriptor.getComposedAnnotationType());

		// direct access to annotation value:
		assertArrayEquals(new String[] { "foo.xml" }, descriptor.getAnnotation().value());

		// overridden attribute:
		AnnotationAttributes attributes = descriptor.getAnnotationAttributes();

		// NOTE: we would like to be able to override the 'value' attribute; however,
		// Spring currently does not allow overrides for the 'value' attribute.
		// TODO Determine if Spring should allow overrides for the 'value' attribute in
		// custom annotations.
		assertArrayEquals(new String[] { "foo.xml" }, attributes.getStringArray("value"));
	}

	@Test
	public void contextConfigurationLocationsAndInheritLocations() throws Exception {
		Class<MetaLocationsConfigTest> declaringClass = MetaLocationsConfigTest.class;
		AnnotationDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(declaringClass,
			ContextConfiguration.class);
		assertNotNull(descriptor);
		assertEquals(declaringClass, descriptor.getRootDeclaringClass());
		assertEquals(MetaLocationsConfig.class, descriptor.getComposedAnnotationType());
		assertEquals(ContextConfiguration.class, descriptor.getAnnotationType());
		assertNotNull(descriptor.getComposedAnnotation());
		assertEquals(MetaLocationsConfig.class, descriptor.getComposedAnnotationType());

		// direct access to annotation attributes:
		assertArrayEquals(new String[] { "foo.xml" }, descriptor.getAnnotation().locations());
		assertFalse(descriptor.getAnnotation().inheritLocations());
	}

	@Test
	public void overriddenContextConfigurationLocationsAndInheritLocations() throws Exception {
		Class<?> declaringClass = OverriddenMetaLocationsConfigTest.class;
		AnnotationDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(declaringClass,
			ContextConfiguration.class);
		assertNotNull(descriptor);
		assertEquals(declaringClass, descriptor.getRootDeclaringClass());
		assertEquals(MetaLocationsConfig.class, descriptor.getComposedAnnotationType());
		assertEquals(ContextConfiguration.class, descriptor.getAnnotationType());
		assertNotNull(descriptor.getComposedAnnotation());
		assertEquals(MetaLocationsConfig.class, descriptor.getComposedAnnotationType());

		// direct access to annotation attributes:
		assertArrayEquals(new String[] { "foo.xml" }, descriptor.getAnnotation().locations());
		assertFalse(descriptor.getAnnotation().inheritLocations());

		// overridden attributes:
		AnnotationAttributes attributes = descriptor.getAnnotationAttributes();
		assertArrayEquals(new String[] { "bar.xml" }, attributes.getStringArray("locations"));
		assertTrue(attributes.getBoolean("inheritLocations"));
	}


	// -------------------------------------------------------------------------

	@ContextConfiguration("foo.xml")
	@Retention(RetentionPolicy.RUNTIME)
	static @interface MetaValueConfig {

		String[] value() default {};
	}

	@MetaValueConfig
	public static class MetaValueConfigTest {
	}

	@MetaValueConfig("bar.xml")
	public static class OverriddenMetaValueConfigTest {
	}

	@ContextConfiguration(locations = "foo.xml", inheritLocations = false)
	@Retention(RetentionPolicy.RUNTIME)
	static @interface MetaLocationsConfig {

		String[] locations() default {};

		boolean inheritLocations();
	}

	@MetaLocationsConfig(inheritLocations = true)
	static class MetaLocationsConfigTest {
	}

	@MetaLocationsConfig(locations = "bar.xml", inheritLocations = true)
	static class OverriddenMetaLocationsConfigTest {
	}

}
