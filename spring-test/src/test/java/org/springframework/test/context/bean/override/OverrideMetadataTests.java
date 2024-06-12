/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.context.bean.override;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.test.context.bean.override.DummyBean.DummyBeanOverrideProcessor.DummyOverrideMetadata;
import org.springframework.test.context.bean.override.example.CustomQualifier;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link OverrideMetadata}.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @since 6.2
 */
public class OverrideMetadataTests {

	@Test
	void forTestClassWithSingleField() {
		List<OverrideMetadata> overrideMetadata = OverrideMetadata.forTestClass(SingleAnnotation.class);
		assertThat(overrideMetadata).singleElement().satisfies(hasTestBeanMetadata(
				field(SingleAnnotation.class, "message"), String.class, null));
	}

	@Test
	void forTestClassWithMultipleFields() {
		List<OverrideMetadata> overrideMetadata = OverrideMetadata.forTestClass(MultipleAnnotations.class);
		assertThat(overrideMetadata).hasSize(2)
				.anySatisfy(hasTestBeanMetadata(
						field(MultipleAnnotations.class, "message"), String.class, null))
				.anySatisfy(hasTestBeanMetadata(
						field(MultipleAnnotations.class, "counter"), Integer.class, null));
	}

	@Test
	void forTestClassWithMultipleFieldsSameMetadata() {
		List<OverrideMetadata> overrideMetadata = OverrideMetadata.forTestClass(MultipleAnnotationsDuplicate.class);
		assertThat(overrideMetadata).hasSize(2)
				.anySatisfy(hasTestBeanMetadata(
						field(MultipleAnnotationsDuplicate.class, "message1"), String.class, "messageBean"))
				.anySatisfy(hasTestBeanMetadata(
						field(MultipleAnnotationsDuplicate.class, "message2"), String.class, "messageBean"));
		assertThat(new HashSet<>(overrideMetadata)).hasSize(1);
	}

	@Test
	void forTestClassWithDifferentOverrideMetadataOnSameField() {
		Field faultyField = field(MultipleAnnotationsOnSameField.class, "message");
		assertThatIllegalStateException()
				.isThrownBy(() -> OverrideMetadata.forTestClass(MultipleAnnotationsOnSameField.class))
				.withMessageStartingWith("Multiple @BeanOverride annotations found")
				.withMessageContaining(faultyField.toString());
	}

	@Test
	void getBeanNameIsNullByDefault() {
		OverrideMetadata metadata = createMetadata(field(ConfigA.class, "noQualifier"));
		assertThat(metadata.getBeanName()).isNull();
	}

	@Test
	void isEqualToWithSameInstance() {
		OverrideMetadata metadata = createMetadata(field(ConfigA.class, "noQualifier"));
		assertThat(metadata).isEqualTo(metadata);
		assertThat(metadata).hasSameHashCodeAs(metadata);
	}

	@Test
	void isEqualToWithSameMetadata() {
		OverrideMetadata metadata = createMetadata(field(ConfigA.class, "noQualifier"));
		OverrideMetadata metadata2 = createMetadata(field(ConfigA.class, "noQualifier"));
		assertThat(metadata).isEqualTo(metadata2);
		assertThat(metadata).hasSameHashCodeAs(metadata2);
	}

	@Test
	void isEqualToWithSameMetadataAndBeanNames() {
		OverrideMetadata metadata = createMetadata(field(ConfigA.class, "noQualifier"), "testBean");
		OverrideMetadata metadata2 = createMetadata(field(ConfigA.class, "noQualifier"), "testBean");
		assertThat(metadata).isEqualTo(metadata2);
		assertThat(metadata).hasSameHashCodeAs(metadata2);
	}

	@Test
	void isNotEqualToWithSameMetadataAndDifferentBeaName() {
		OverrideMetadata metadata = createMetadata(field(ConfigA.class, "noQualifier"), "testBean");
		OverrideMetadata metadata2 = createMetadata(field(ConfigA.class, "noQualifier"), "testBean2");
		assertThat(metadata).isNotEqualTo(metadata2);
	}

	@Test
	void isEqualToWithSameMetadataButDifferentFields() {
		OverrideMetadata metadata = createMetadata(field(ConfigA.class, "noQualifier"));
		OverrideMetadata metadata2 = createMetadata(field(ConfigB.class, "noQualifier"));
		assertThat(metadata).isEqualTo(metadata2);
		assertThat(metadata).hasSameHashCodeAs(metadata2);
	}

	@Test
	void isEqualToWithByNameLookupAndDifferentFieldNames() {
		OverrideMetadata metadata = createMetadata(field(ConfigA.class, "noQualifier"), "beanToOverride");
		OverrideMetadata metadata2 = createMetadata(field(ConfigB.class, "example"), "beanToOverride");
		assertThat(metadata).isEqualTo(metadata2);
		assertThat(metadata).hasSameHashCodeAs(metadata2);
	}

	@Test
	void isEqualToWithSameMetadataAndSameQualifierValues() {
		OverrideMetadata metadata = createMetadata(field(ConfigA.class, "directQualifier"));
		OverrideMetadata metadata2 = createMetadata(field(ConfigB.class, "directQualifier"));
		assertThat(metadata).isEqualTo(metadata2);
		assertThat(metadata).hasSameHashCodeAs(metadata2);
	}

	@Test
	void isNotEqualToWithSameMetadataAndDifferentQualifierValues() {
		OverrideMetadata metadata = createMetadata(field(ConfigA.class, "directQualifier"));
		OverrideMetadata metadata2 = createMetadata(field(ConfigA.class, "differentDirectQualifier"));
		assertThat(metadata).isNotEqualTo(metadata2);
	}

	@Test
	void isNotEqualToWithSameMetadataAndDifferentQualifiers() {
		OverrideMetadata metadata = createMetadata(field(ConfigA.class, "directQualifier"));
		OverrideMetadata metadata2 = createMetadata(field(ConfigA.class, "customQualifier"));
		assertThat(metadata).isNotEqualTo(metadata2);
	}

	@Test
	void isNotEqualToWithByTypeLookupAndDifferentFieldNames() {
		OverrideMetadata metadata = createMetadata(field(ConfigA.class, "noQualifier"));
		OverrideMetadata metadata2 = createMetadata(field(ConfigB.class, "example"));
		assertThat(metadata).isNotEqualTo(metadata2);
	}

	private OverrideMetadata createMetadata(Field field) {
		return createMetadata(field, null);
	}

	private OverrideMetadata createMetadata(Field field, @Nullable String name) {
		return new DummyOverrideMetadata(field, field.getType(), name, BeanOverrideStrategy.REPLACE_DEFINITION);
	}

	private Field field(Class<?> target, String fieldName) {
		Field field = ReflectionUtils.findField(target, fieldName);
		assertThat(field).isNotNull();
		return field;
	}

	private Consumer<OverrideMetadata> hasTestBeanMetadata(Field field, Class<?> beanType, @Nullable String beanName) {
		return hasOverrideMetadata(field, beanType, BeanOverrideStrategy.REPLACE_DEFINITION, beanName);
	}

	private Consumer<OverrideMetadata> hasOverrideMetadata(Field field, Class<?> beanType, BeanOverrideStrategy strategy, @Nullable String beanName) {
		return metadata -> {
			assertThat(metadata.getField()).isEqualTo(field);
			assertThat(metadata.getBeanType().toClass()).isEqualTo(beanType);
			assertThat(metadata.getStrategy()).isEqualTo(strategy);
			assertThat(metadata.getBeanName()).isEqualTo(beanName);
		};
	}


	static class SingleAnnotation {

		@DummyBean
		String message;

	}

	static class MultipleAnnotations {

		@DummyBean
		String message;

		@DummyBean
		Integer counter;
	}

	static class MultipleAnnotationsDuplicate {

		@DummyBean(beanName = "messageBean")
		String message1;

		@DummyBean(beanName = "messageBean")
		String message2;

	}

	static class MultipleAnnotationsOnSameField {

		@MetaDummyBean()
		@DummyBean
		String message;

		static String foo() {
			return "foo";
		}
	}

	public static class ConfigA {

		private ExampleService noQualifier;

		@Qualifier("test")
		private ExampleService directQualifier;

		@Qualifier("different")
		private ExampleService differentDirectQualifier;

		@CustomQualifier
		private ExampleService customQualifier;

	}

	public static class ConfigB {

		private ExampleService noQualifier;

		private ExampleService example;

		@Qualifier("test")
		private ExampleService directQualifier;

	}

	// Simple OverrideMetadata implementation

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@DummyBean
	public @interface MetaDummyBean {}

}
