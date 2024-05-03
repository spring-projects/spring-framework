/*
 * Copyright 2002-2023 the original author or authors.
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

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.aot.hint.predicate.ReflectionHintsPredicates;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ReflectiveScan;
import org.springframework.context.testfixture.context.aot.scan.reflective2.Reflective2OnType;
import org.springframework.context.testfixture.context.aot.scan.reflective2.reflective21.Reflective21OnType;
import org.springframework.context.testfixture.context.aot.scan.reflective2.reflective22.Reflective22OnType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReflectiveProcessorBeanFactoryInitializationAotProcessor}.
 *
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 */
class ReflectiveProcessorBeanFactoryInitializationAotProcessorTests {

	private final ReflectiveProcessorBeanFactoryInitializationAotProcessor processor = new ReflectiveProcessorBeanFactoryInitializationAotProcessor();

	private final GenerationContext generationContext = new TestGenerationContext();

	@Test
	void processorIsRegistered() {
		assertThat(AotServices.factories(getClass().getClassLoader()).load(BeanFactoryInitializationAotProcessor.class))
				.anyMatch(ReflectiveProcessorBeanFactoryInitializationAotProcessor.class::isInstance);
	}

	@Test
	void shouldProcessAnnotationOnType() {
		process(SampleTypeAnnotatedBean.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(SampleTypeAnnotatedBean.class))
				.accepts(this.generationContext.getRuntimeHints());
	}

	@Test
	void shouldProcessAllBeans() throws NoSuchMethodException {
		ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
		process(SampleTypeAnnotatedBean.class, SampleConstructorAnnotatedBean.class);
		Constructor<?> constructor = SampleConstructorAnnotatedBean.class.getDeclaredConstructor(String.class);
		assertThat(reflection.onType(SampleTypeAnnotatedBean.class).and(reflection.onConstructor(constructor)))
				.accepts(this.generationContext.getRuntimeHints());
	}

	@Test
	void shouldTriggerScanningIfBeanUsesReflectiveScan() {
		process(SampleBeanWithReflectiveScan.class);
		assertThat(this.generationContext.getRuntimeHints().reflection().typeHints().map(TypeHint::getType))
				.containsExactlyInAnyOrderElementsOf(TypeReference.listOf(
						Reflective2OnType.class, Reflective21OnType.class, Reflective22OnType.class));
	}

	@Test
	void findBasePackagesToScanWhenNoCandidateIsEmpty() {
		Class<?>[] candidates = { String.class };
		assertThat(this.processor.findBasePackagesToScan(candidates)).isEmpty();
	}

	@Test
	void findBasePackagesToScanWithBasePackageClasses() {
		Class<?>[] candidates = { SampleBeanWithReflectiveScan.class };
		assertThat(this.processor.findBasePackagesToScan(candidates))
				.containsOnly(Reflective2OnType.class.getPackageName());
	}

	@Test
	void findBasePackagesToScanWithBasePackages() {
		Class<?>[] candidates = { SampleBeanWithReflectiveScanWithName.class };
		assertThat(this.processor.findBasePackagesToScan(candidates))
				.containsOnly(Reflective2OnType.class.getPackageName());
	}

	@Test
	void findBasePackagesToScanWithBasePackagesAndClasses() {
		Class<?>[] candidates = { SampleBeanWithMultipleReflectiveScan.class };
		assertThat(this.processor.findBasePackagesToScan(candidates))
				.containsOnly(Reflective21OnType.class.getPackageName(), Reflective22OnType.class.getPackageName());
	}

	@Test
	void findBasePackagesToScanWithDuplicatesFiltersThem() {
		Class<?>[] candidates = { SampleBeanWithReflectiveScan.class, SampleBeanWithReflectiveScanWithName.class };
		assertThat(this.processor.findBasePackagesToScan(candidates))
				.containsOnly(Reflective2OnType.class.getPackageName());
	}

	private void process(Class<?>... beanClasses) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		for (Class<?> beanClass : beanClasses) {
			beanFactory.registerBeanDefinition(beanClass.getName(), new RootBeanDefinition(beanClass));
		}
		BeanFactoryInitializationAotContribution contribution = this.processor.processAheadOfTime(beanFactory);
		assertThat(contribution).isNotNull();
		contribution.applyTo(this.generationContext, mock());
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

	@ReflectiveScan(basePackageClasses = Reflective2OnType.class)
	static class SampleBeanWithReflectiveScan {
	}

	@ReflectiveScan("org.springframework.context.testfixture.context.aot.scan.reflective2")
	static class SampleBeanWithReflectiveScanWithName {
	}

	@ReflectiveScan(basePackageClasses = Reflective22OnType.class,
			basePackages = "org.springframework.context.testfixture.context.aot.scan.reflective2.reflective21")
	static class SampleBeanWithMultipleReflectiveScan {
	}

}
