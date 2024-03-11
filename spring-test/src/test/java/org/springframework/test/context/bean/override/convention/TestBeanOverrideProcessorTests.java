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

package org.springframework.test.context.bean.override.convention;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.FailingExampleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

class TestBeanOverrideProcessorTests {

	@Test
	void ensureMethodFindsFromList() {
		Method m = TestBeanOverrideProcessor.ensureMethod(MethodConventionConf.class, ExampleService.class,
				"example1", "example2", "example3");

		assertThat(m.getName()).isEqualTo("example2");
	}

	@Test
	void ensureMethodNotFound() {
		assertThatException().isThrownBy(() -> TestBeanOverrideProcessor.ensureMethod(
				MethodConventionConf.class, ExampleService.class, "example1", "example3"))
				.withMessage("Found 0 static methods instead of exactly one, matching a name in [example1, example3] with return type " +
						ExampleService.class.getName() + " on class " + MethodConventionConf.class.getName())
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void ensureMethodTwoFound() {
		assertThatException().isThrownBy(() -> TestBeanOverrideProcessor.ensureMethod(
				MethodConventionConf.class, ExampleService.class, "example2", "example4"))
				.withMessage("Found 2 static methods instead of exactly one, matching a name in [example2, example4] with return type " +
						ExampleService.class.getName() + " on class " + MethodConventionConf.class.getName())
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void ensureMethodNoNameProvided() {
		assertThatException().isThrownBy(() -> TestBeanOverrideProcessor.ensureMethod(
				MethodConventionConf.class, ExampleService.class))
				.withMessage("At least one expectedMethodName is required")
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void createMetaDataForUnknownExplicitMethod() throws NoSuchFieldException {
		Field f = ExplicitMethodNameConf.class.getField("a");
		final TestBean overrideAnnotation = Objects.requireNonNull(AnnotationUtils.getAnnotation(f, TestBean.class));
		TestBeanOverrideProcessor processor = new TestBeanOverrideProcessor();
		assertThatException().isThrownBy(() -> processor.createMetadata(f, overrideAnnotation, ResolvableType.forClass(ExampleService.class)))
				.withMessage("Found 0 static methods instead of exactly one, matching a name in [explicit1] with return type " +
						ExampleService.class.getName() + " on class " + ExplicitMethodNameConf.class.getName())
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void createMetaDataForKnownExplicitMethod() throws NoSuchFieldException {
		Field f = ExplicitMethodNameConf.class.getField("b");
		final TestBean overrideAnnotation = Objects.requireNonNull(AnnotationUtils.getAnnotation(f, TestBean.class));
		TestBeanOverrideProcessor processor = new TestBeanOverrideProcessor();
		assertThat(processor.createMetadata(f, overrideAnnotation, ResolvableType.forClass(ExampleService.class)))
				.isInstanceOf(TestBeanOverrideProcessor.MethodConventionOverrideMetadata.class);
	}

	@Test
	void createMetaDataWithDeferredEnsureMethodCheck() throws NoSuchFieldException {
		Field f = MethodConventionConf.class.getField("field");
		final TestBean overrideAnnotation = Objects.requireNonNull(AnnotationUtils.getAnnotation(f, TestBean.class));
		TestBeanOverrideProcessor processor = new TestBeanOverrideProcessor();
		assertThat(processor.createMetadata(f, overrideAnnotation, ResolvableType.forClass(ExampleService.class)))
				.isInstanceOf(TestBeanOverrideProcessor.MethodConventionOverrideMetadata.class);
	}

	static class MethodConventionConf {

		@TestBean
		public ExampleService field;

		@Bean
		ExampleService example1() {
			return new FailingExampleService();
		}

		static ExampleService example2() {
			return new FailingExampleService();
		}

		public static ExampleService example4() {
			return new FailingExampleService();
		}
	}

	static class ExplicitMethodNameConf {

		@TestBean(methodName = "explicit1")
		public ExampleService a;

		@TestBean(methodName = "explicit2")
		public ExampleService b;

		static ExampleService explicit2() {
			return new FailingExampleService();
		}
	}
}
