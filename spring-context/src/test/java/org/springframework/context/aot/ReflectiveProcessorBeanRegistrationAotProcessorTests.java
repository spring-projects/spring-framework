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

package org.springframework.context.aot;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsPredicates;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.SynthesizedAnnotation;
import org.springframework.core.testfixture.aot.generate.TestGenerationContext;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReflectiveProcessorBeanRegistrationAotProcessor}.
 *
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 */
class ReflectiveProcessorBeanRegistrationAotProcessorTests {

	private final ReflectiveProcessorBeanRegistrationAotProcessor processor = new ReflectiveProcessorBeanRegistrationAotProcessor();

	private final GenerationContext generationContext = new TestGenerationContext();

	@Test
	void shouldIgnoreNonAnnotatedType() {
		assertThat(createContribution(String.class)).isNull();
	}

	@Test
	void shouldProcessAnnotationOnType() {
		process(SampleTypeAnnotatedBean.class);
		assertThat(this.generationContext.getRuntimeHints().reflection().getTypeHint(SampleTypeAnnotatedBean.class))
				.isNotNull();
	}

	@Test
	void shouldProcessAnnotationOnConstructor() {
		process(SampleConstructorAnnotatedBean.class);
		assertThat(this.generationContext.getRuntimeHints().reflection().getTypeHint(SampleConstructorAnnotatedBean.class))
				.satisfies(typeHint -> assertThat(typeHint.constructors()).singleElement()
						.satisfies(constructorHint -> assertThat(constructorHint.getParameterTypes())
								.containsExactly(TypeReference.of(String.class))));
	}

	@Test
	void shouldProcessAnnotationOnField() {
		process(SampleFieldAnnotatedBean.class);
		assertThat(this.generationContext.getRuntimeHints().reflection().getTypeHint(SampleFieldAnnotatedBean.class))
				.satisfies(typeHint -> assertThat(typeHint.fields()).singleElement()
						.satisfies(fieldHint -> assertThat(fieldHint.getName()).isEqualTo("managed")));
	}

	@Test
	void shouldProcessAnnotationOnMethod() {
		process(SampleMethodAnnotatedBean.class);
		assertThat(this.generationContext.getRuntimeHints().reflection().getTypeHint(SampleMethodAnnotatedBean.class))
				.satisfies(typeHint -> assertThat(typeHint.methods()).singleElement()
						.satisfies(methodHint -> assertThat(methodHint.getName()).isEqualTo("managed")));
	}

	@Test
	void shouldRegisterAnnotation() {
		process(SampleMethodMetaAnnotatedBean.class);
		RuntimeHints runtimeHints = this.generationContext.getRuntimeHints();
		assertThat(RuntimeHintsPredicates.reflection().onType(SampleInvoker.class).withMemberCategory(MemberCategory.INVOKE_DECLARED_METHODS)).accepts(runtimeHints);
		assertThat(runtimeHints.proxies().jdkProxies()).isEmpty();
	}

	@Test
	void shouldRegisterAnnotationAndProxyWithAliasFor() {
		process(SampleMethodMetaAnnotatedBeanWithAlias.class);
		RuntimeHints runtimeHints = this.generationContext.getRuntimeHints();
		assertThat(RuntimeHintsPredicates.reflection().onType(RetryInvoker.class).withMemberCategory(MemberCategory.INVOKE_DECLARED_METHODS)).accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.proxies().forInterfaces(RetryInvoker.class, SynthesizedAnnotation.class)).accepts(runtimeHints);
	}

	@Test
	void shouldProcessAnnotationOnInterface() {
		process(SampleMethodAnnotatedBeanWithInterface.class);
		assertThat(this.generationContext.getRuntimeHints().reflection().getTypeHint(SampleInterface.class))
				.satisfies(typeHint -> assertThat(typeHint.methods()).singleElement()
						.satisfies(methodHint -> assertThat(methodHint.getName()).isEqualTo("managed")));
		assertThat(this.generationContext.getRuntimeHints().reflection().getTypeHint(SampleMethodAnnotatedBeanWithInterface.class))
				.satisfies(typeHint -> assertThat(typeHint.methods()).singleElement()
						.satisfies(methodHint -> assertThat(methodHint.getName()).isEqualTo("managed")));
	}

	@Test
	void shouldProcessAnnotationOnInheritedClass() {
		process(SampleMethodAnnotatedBeanWithInheritance.class);
		assertThat(this.generationContext.getRuntimeHints().reflection().getTypeHint(SampleInheritedClass.class))
				.satisfies(typeHint -> assertThat(typeHint.methods()).singleElement()
						.satisfies(methodHint -> assertThat(methodHint.getName()).isEqualTo("managed")));
		assertThat(this.generationContext.getRuntimeHints().reflection().getTypeHint(SampleMethodAnnotatedBeanWithInheritance.class))
				.satisfies(typeHint -> assertThat(typeHint.methods()).singleElement()
						.satisfies(methodHint -> assertThat(methodHint.getName()).isEqualTo("managed")));
	}

	@Nullable
	private BeanRegistrationAotContribution createContribution(Class<?> beanClass) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition(beanClass.getName(), new RootBeanDefinition(beanClass));
		return this.processor.processAheadOfTime(RegisteredBean.of(beanFactory, beanClass.getName()));
	}

	private void process(Class<?> beanClass) {
		BeanRegistrationAotContribution contribution = createContribution(beanClass);
		assertThat(contribution).isNotNull();
		contribution.applyTo(this.generationContext, mock(BeanRegistrationCode.class));
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

	@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Reflective
	@interface SampleInvoker {

		int retries() default 0;

	}

	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@SampleInvoker
	@interface RetryInvoker {

		@AliasFor(attribute = "retries", annotation = SampleInvoker.class)
		int value() default 1;

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

}
