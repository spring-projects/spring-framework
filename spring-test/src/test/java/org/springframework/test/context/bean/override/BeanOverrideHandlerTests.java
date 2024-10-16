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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.test.context.bean.override.DummyBean.DummyBeanOverrideProcessor.DummyBeanOverrideHandler;
import org.springframework.test.context.bean.override.example.CustomQualifier;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * Tests for {@link BeanOverrideHandler}.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.2
 */
class BeanOverrideHandlerTests {

	@Test
	void forTestClassWithSingleField() {
		List<BeanOverrideHandler> handlers = BeanOverrideHandler.forTestClass(SingleAnnotation.class);
		assertThat(handlers).singleElement().satisfies(hasBeanOverrideHandler(
				field(SingleAnnotation.class, "message"), String.class, null));
	}

	@Test
	void forTestClassWithMultipleFields() {
		List<BeanOverrideHandler> handlers = BeanOverrideHandler.forTestClass(MultipleAnnotations.class);
		assertThat(handlers).hasSize(2)
				.anySatisfy(hasBeanOverrideHandler(
						field(MultipleAnnotations.class, "message"), String.class, null))
				.anySatisfy(hasBeanOverrideHandler(
						field(MultipleAnnotations.class, "counter"), Integer.class, null));
	}

	@Test
	void forTestClassWithMultipleFieldsWithIdenticalMetadata() {
		List<BeanOverrideHandler> handlers = BeanOverrideHandler.forTestClass(MultipleAnnotationsDuplicate.class);
		assertThat(handlers).hasSize(2)
				.anySatisfy(hasBeanOverrideHandler(
						field(MultipleAnnotationsDuplicate.class, "message1"), String.class, "messageBean"))
				.anySatisfy(hasBeanOverrideHandler(
						field(MultipleAnnotationsDuplicate.class, "message2"), String.class, "messageBean"));
		assertThat(new HashSet<>(handlers)).hasSize(1);
	}

	@Test
	void forTestClassWithCompetingBeanOverrideAnnotationsOnSameField() {
		Field faultyField = field(MultipleAnnotationsOnSameField.class, "message");
		assertThatIllegalStateException()
				.isThrownBy(() -> BeanOverrideHandler.forTestClass(MultipleAnnotationsOnSameField.class))
				.withMessageStartingWith("Multiple @BeanOverride annotations found")
				.withMessageContaining(faultyField.toString());
	}

	@Test
	void getBeanNameIsNullByDefault() {
		BeanOverrideHandler handler = createBeanOverrideHandler(field(ConfigA.class, "noQualifier"));
		assertThat(handler.getBeanName()).isNull();
	}

	@Test
	void isEqualToWithSameInstance() {
		BeanOverrideHandler handler = createBeanOverrideHandler(field(ConfigA.class, "noQualifier"));
		assertThat(handler).isEqualTo(handler);
		assertThat(handler).hasSameHashCodeAs(handler);
	}

	@Test
	void isEqualToWithSameMetadata() {
		BeanOverrideHandler handler1 = createBeanOverrideHandler(field(ConfigA.class, "noQualifier"));
		BeanOverrideHandler handler2 = createBeanOverrideHandler(field(ConfigA.class, "noQualifier"));
		assertThat(handler1).isEqualTo(handler2);
		assertThat(handler1).hasSameHashCodeAs(handler2);
	}

	@Test
	void isEqualToWithSameMetadataAndBeanNames() {
		BeanOverrideHandler handler1 = createBeanOverrideHandler(field(ConfigA.class, "noQualifier"), "testBean");
		BeanOverrideHandler handler2 = createBeanOverrideHandler(field(ConfigA.class, "noQualifier"), "testBean");
		assertThat(handler1).isEqualTo(handler2);
		assertThat(handler1).hasSameHashCodeAs(handler2);
	}

	@Test
	void isNotEqualToWithSameMetadataAndDifferentBeaName() {
		BeanOverrideHandler handler1 = createBeanOverrideHandler(field(ConfigA.class, "noQualifier"), "testBean");
		BeanOverrideHandler handler2 = createBeanOverrideHandler(field(ConfigA.class, "noQualifier"), "testBean2");
		assertThat(handler1).isNotEqualTo(handler2);
	}

	@Test
	void isEqualToWithSameMetadataButDifferentFields() {
		BeanOverrideHandler handler1 = createBeanOverrideHandler(field(ConfigA.class, "noQualifier"));
		BeanOverrideHandler handler2 = createBeanOverrideHandler(field(ConfigB.class, "noQualifier"));
		assertThat(handler1).isEqualTo(handler2);
		assertThat(handler1).hasSameHashCodeAs(handler2);
	}

	@Test
	void isEqualToWithByNameLookupAndDifferentFieldNames() {
		BeanOverrideHandler handler1 = createBeanOverrideHandler(field(ConfigA.class, "noQualifier"), "beanToOverride");
		BeanOverrideHandler handler2 = createBeanOverrideHandler(field(ConfigB.class, "example"), "beanToOverride");
		assertThat(handler1).isEqualTo(handler2);
		assertThat(handler1).hasSameHashCodeAs(handler2);
	}

	@Test
	void isEqualToWithSameMetadataAndSameQualifierValues() {
		BeanOverrideHandler handler1 = createBeanOverrideHandler(field(ConfigA.class, "directQualifier"));
		BeanOverrideHandler handler2 = createBeanOverrideHandler(field(ConfigB.class, "directQualifier"));
		assertThat(handler1).isEqualTo(handler2);
		assertThat(handler1).hasSameHashCodeAs(handler2);
	}

	@Test
	void isEqualToWithSameMetadataAndSameQualifierValuesButWithAnnotationsDeclaredInDifferentOrder() {
		Field field1 = field(ConfigA.class, "qualifiedDummyBean");
		Field field2 = field(ConfigB.class, "qualifiedDummyBean");

		// Prerequisite
		assertThat(Arrays.equals(field1.getAnnotations(), field2.getAnnotations())).isFalse();

		BeanOverrideHandler handler1 = createBeanOverrideHandler(field1);
		BeanOverrideHandler handler2 = createBeanOverrideHandler(field2);
		assertThat(handler1).isEqualTo(handler2);
		assertThat(handler1).hasSameHashCodeAs(handler2);
	}

	@Test
	void isNotEqualToWithSameMetadataAndDifferentQualifierValues() {
		BeanOverrideHandler handler1 = createBeanOverrideHandler(field(ConfigA.class, "directQualifier"));
		BeanOverrideHandler handler2 = createBeanOverrideHandler(field(ConfigA.class, "differentDirectQualifier"));
		assertThat(handler1).isNotEqualTo(handler2);
	}

	@Test
	void isNotEqualToWithSameMetadataAndDifferentQualifiers() {
		BeanOverrideHandler handler1 = createBeanOverrideHandler(field(ConfigA.class, "directQualifier"));
		BeanOverrideHandler handler2 = createBeanOverrideHandler(field(ConfigA.class, "customQualifier"));
		assertThat(handler1).isNotEqualTo(handler2);
	}

	@Test
	void isNotEqualToWithByTypeLookupAndDifferentFieldNames() {
		BeanOverrideHandler handler1 = createBeanOverrideHandler(field(ConfigA.class, "noQualifier"));
		BeanOverrideHandler handler2 = createBeanOverrideHandler(field(ConfigB.class, "example"));
		assertThat(handler1).isNotEqualTo(handler2);
	}

	private static BeanOverrideHandler createBeanOverrideHandler(Field field) {
		return createBeanOverrideHandler(field, null);
	}

	private static BeanOverrideHandler createBeanOverrideHandler(Field field, @Nullable String name) {
		return new DummyBeanOverrideHandler(field, field.getType(), name, BeanOverrideStrategy.REPLACE);
	}

	private static Field field(Class<?> target, String fieldName) {
		Field field = ReflectionUtils.findField(target, fieldName);
		assertThat(field).isNotNull();
		return field;
	}

	private static Consumer<BeanOverrideHandler> hasBeanOverrideHandler(Field field, Class<?> beanType, @Nullable String beanName) {
		return hasBeanOverrideHandler(field, beanType, BeanOverrideStrategy.REPLACE, beanName);
	}

	private static Consumer<BeanOverrideHandler> hasBeanOverrideHandler(Field field, Class<?> beanType, BeanOverrideStrategy strategy,
			@Nullable String beanName) {

		return handler -> assertSoftly(softly -> {
				softly.assertThat(handler.getField()).as("field").isEqualTo(field);
				softly.assertThat(handler.getBeanType().toClass()).as("type").isEqualTo(beanType);
				softly.assertThat(handler.getBeanName()).as("name").isEqualTo(beanName);
				softly.assertThat(handler.getStrategy()).as("strategy").isEqualTo(strategy);
			});
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

	static class ConfigA {

		ExampleService noQualifier;

		@Qualifier("test")
		ExampleService directQualifier;

		@Qualifier("different")
		ExampleService differentDirectQualifier;

		@CustomQualifier
		ExampleService customQualifier;

		@DummyBean
		@Qualifier("test")
		ExampleService qualifiedDummyBean;
	}

	static class ConfigB {

		ExampleService noQualifier;

		ExampleService example;

		@Qualifier("test")
		ExampleService directQualifier;

		@Qualifier("test")
		@DummyBean
		ExampleService qualifiedDummyBean;
	}

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@DummyBean
	@interface MetaDummyBean {}

}
