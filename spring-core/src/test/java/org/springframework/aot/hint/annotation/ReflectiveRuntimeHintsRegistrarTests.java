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

package org.springframework.aot.hint.annotation;

import java.io.Closeable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.aot.hint.FieldHint;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.core.annotation.AliasFor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link ReflectiveRuntimeHintsRegistrar}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class ReflectiveRuntimeHintsRegistrarTests {

	private final ReflectiveRuntimeHintsRegistrar registrar = new ReflectiveRuntimeHintsRegistrar();

	private final RuntimeHints runtimeHints = new RuntimeHints();

	@ParameterizedTest
	@ValueSource(classes = { SampleTypeAnnotatedBean.class, SampleFieldAnnotatedBean.class,
			SampleConstructorAnnotatedBean.class, SampleMethodAnnotatedBean.class, SampleInterface.class,
			SampleMethodMetaAnnotatedBeanWithAlias.class, SampleMethodAnnotatedBeanWithInterface.class })
	void isCandidateWithMatchingAnnotatedElement(Class<?> candidate) {
		assertThat(this.registrar.isCandidate(candidate)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(classes = { String.class, Closeable.class })
	void isCandidateWithNonMatchingAnnotatedElement(Class<?> candidate) {
		assertThat(this.registrar.isCandidate(candidate)).isFalse();
	}

	@Test
	void shouldIgnoreNonAnnotatedType() {
		RuntimeHints mock = mock();
		this.registrar.registerRuntimeHints(mock, String.class);
		verifyNoInteractions(mock);
	}

	@Test
	void shouldProcessAnnotationOnType() {
		process(SampleTypeAnnotatedBean.class);
		assertThat(this.runtimeHints.reflection().getTypeHint(SampleTypeAnnotatedBean.class))
				.isNotNull();
	}

	@Test
	void shouldProcessWithMultipleProcessorsWithAnnotationOnType() {
		process(SampleMultipleCustomProcessors.class);
		TypeHint typeHint = this.runtimeHints.reflection().getTypeHint(SampleMultipleCustomProcessors.class);
		assertThat(typeHint).isNotNull();
		assertThat(typeHint.getMemberCategories()).containsExactly(MemberCategory.INVOKE_DECLARED_METHODS);
	}

	@Test
	void shouldProcessAnnotationOnConstructor() {
		process(SampleConstructorAnnotatedBean.class);
		assertThat(this.runtimeHints.reflection().getTypeHint(SampleConstructorAnnotatedBean.class))
				.satisfies(typeHint -> assertThat(typeHint.constructors()).singleElement()
						.satisfies(constructorHint -> assertThat(constructorHint.getParameterTypes())
								.containsExactly(TypeReference.of(String.class))));
	}

	@Test
	void shouldProcessAnnotationOnField() {
		process(SampleFieldAnnotatedBean.class);
		assertThat(this.runtimeHints.reflection().getTypeHint(SampleFieldAnnotatedBean.class))
				.satisfies(typeHint -> assertThat(typeHint.fields()).singleElement()
						.satisfies(fieldHint -> assertThat(fieldHint.getName()).isEqualTo("managed")));
	}

	@Test
	void shouldProcessAnnotationOnMethod() {
		process(SampleMethodAnnotatedBean.class);
		assertThat(this.runtimeHints.reflection().getTypeHint(SampleMethodAnnotatedBean.class))
				.satisfies(typeHint -> assertThat(typeHint.methods()).singleElement()
						.satisfies(methodHint -> assertThat(methodHint.getName()).isEqualTo("managed")));
	}

	@Test
	void shouldProcessAnnotationOnInterface() {
		process(SampleMethodAnnotatedBeanWithInterface.class);
		assertThat(this.runtimeHints.reflection().getTypeHint(SampleInterface.class))
				.satisfies(typeHint -> assertThat(typeHint.methods()).singleElement()
						.satisfies(methodHint -> assertThat(methodHint.getName()).isEqualTo("managed")));
		assertThat(this.runtimeHints.reflection().getTypeHint(SampleMethodAnnotatedBeanWithInterface.class))
				.satisfies(typeHint -> assertThat(typeHint.methods()).singleElement()
						.satisfies(methodHint -> assertThat(methodHint.getName()).isEqualTo("managed")));
	}

	@Test
	void shouldProcessAnnotationOnInheritedClass() {
		process(SampleMethodAnnotatedBeanWithInheritance.class);
		assertThat(this.runtimeHints.reflection().getTypeHint(SampleInheritedClass.class))
				.satisfies(typeHint -> assertThat(typeHint.methods()).singleElement()
						.satisfies(methodHint -> assertThat(methodHint.getName()).isEqualTo("managed")));
		assertThat(this.runtimeHints.reflection().getTypeHint(SampleMethodAnnotatedBeanWithInheritance.class))
				.satisfies(typeHint -> assertThat(typeHint.methods()).singleElement()
						.satisfies(methodHint -> assertThat(methodHint.getName()).isEqualTo("managed")));
	}

	@Test
	void shouldProcessDifferentAnnotationsOnTypeAndField() {
		process(SampleTypeAndFieldAnnotatedBean.class);
		assertThat(this.runtimeHints.reflection().getTypeHint(SampleTypeAndFieldAnnotatedBean.class))
				.satisfies(typeHint -> {
					assertThat(typeHint.fields().map(FieldHint::getName)).containsOnly("MESSAGE");
					assertThat(typeHint.getMemberCategories()).containsOnly(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
					assertThat(typeHint.methods()).isEmpty();
					assertThat(typeHint.constructors()).isEmpty();
				});
	}

	@Test
	void shouldInvokeCustomProcessor() {
		process(SampleCustomProcessor.class);
		assertThat(RuntimeHintsPredicates.reflection()
				.onMethod(SampleCustomProcessor.class, "managed")).accepts(this.runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onType(String.class)
				.withMemberCategory(MemberCategory.INVOKE_DECLARED_METHODS)).accepts(this.runtimeHints);

	}

	private void process(Class<?> beanClass) {
		this.registrar.registerRuntimeHints(this.runtimeHints, beanClass);
	}


	@Reflective
	@SuppressWarnings("unused")
	static class SampleTypeAnnotatedBean {

		private String notManaged;

		public void notManaged() {
		}
	}

	@SuppressWarnings("unused")
	static class SampleConstructorAnnotatedBean {

		@Reflective
		SampleConstructorAnnotatedBean(String name) {
		}

		SampleConstructorAnnotatedBean(Integer nameAsNumber) {
		}
	}

	@SuppressWarnings("unused")
	static class SampleFieldAnnotatedBean {

		@Reflective
		String managed;

		String notManaged;

	}

	@SuppressWarnings("unused")
	static class SampleMethodAnnotatedBean {

		@Reflective
		void managed() {
		}

		void notManaged() {
		}
	}

	@RegisterReflection(memberCategories = MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
	static class SampleTypeAndFieldAnnotatedBean {

		@Reflective
		private static final String MESSAGE = "Hello";

	}

	@SuppressWarnings("unused")
	static class SampleMethodMetaAnnotatedBean {

		@SampleInvoker
		void invoke() {
		}

		void notManaged() {
		}
	}

	@SuppressWarnings("unused")
	static class SampleMethodMetaAnnotatedBeanWithAlias {

		@RetryInvoker
		void invoke() {
		}

		void notManaged() {
		}
	}

	static class SampleMethodAnnotatedBeanWithInterface implements SampleInterface {

		@Override
		public void managed() {
		}

		public void notManaged() {
		}
	}

	static class SampleMethodAnnotatedBeanWithInheritance extends SampleInheritedClass {

		@Override
		public void managed() {
		}

		public void notManaged() {
		}
	}

	@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Reflective
	@interface SampleInvoker {

		int retries() default 0;
	}

	@Target({ ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@SampleInvoker
	@interface RetryInvoker {

		@AliasFor(attribute = "retries", annotation = SampleInvoker.class)
		int value() default 1;
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Reflective(TestTypeHintReflectiveProcessor.class)
	@interface ReflectiveWithCustomProcessor {
	}

	interface SampleInterface {

		@Reflective
		void managed();
	}

	static class SampleInheritedClass {

		@Reflective
		void managed() {
		}
	}

	static class SampleCustomProcessor {

		@Reflective(TestMethodHintReflectiveProcessor.class)
		public String managed() {
			return "test";
		}
	}

	@Reflective
	@ReflectiveWithCustomProcessor
	static class SampleMultipleCustomProcessors {

		public String managed() {
			return "test";
		}
	}

	private static class TestMethodHintReflectiveProcessor extends SimpleReflectiveProcessor {

		@Override
		protected void registerMethodHint(ReflectionHints hints, Method method) {
			super.registerMethodHint(hints, method);
			hints.registerType(method.getReturnType(), MemberCategory.INVOKE_DECLARED_METHODS);
		}
	}

	private static class TestTypeHintReflectiveProcessor extends SimpleReflectiveProcessor {

		@Override
		protected void registerTypeHint(ReflectionHints hints, Class<?> type) {
			super.registerTypeHint(hints, type);
			hints.registerType(type, MemberCategory.INVOKE_DECLARED_METHODS);
		}
	}

}
