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

package org.springframework.beans.factory.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.factory.generator.lifecycle.Destroy;
import org.springframework.beans.testfixture.beans.factory.generator.lifecycle.InferredDestroyBean;
import org.springframework.beans.testfixture.beans.factory.generator.lifecycle.Init;
import org.springframework.beans.testfixture.beans.factory.generator.lifecycle.InitDestroyBean;
import org.springframework.beans.testfixture.beans.factory.generator.lifecycle.MultiInitDestroyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * Tests for {@link InitDestroyAnnotationBeanPostProcessor}.
 *
 * @since 6.0
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Sam Brannen
 */
class InitDestroyAnnotationBeanPostProcessorTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	@Test
	void processAheadOfTimeWhenNoCallbackDoesNotMutateRootBeanDefinition() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(NoInitDestroyBean.class);
		processAheadOfTime(beanDefinition);
		RootBeanDefinition mergedBeanDefinition = getMergedBeanDefinition();
		assertThat(mergedBeanDefinition.getInitMethodNames()).isNull();
		assertThat(mergedBeanDefinition.getDestroyMethodNames()).isNull();
	}

	@Test
	void processAheadOfTimeWhenHasInitDestroyAnnotationsAddsMethodNames() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(InitDestroyBean.class);
		processAheadOfTime(beanDefinition);
		RootBeanDefinition mergedBeanDefinition = getMergedBeanDefinition();
		assertThat(mergedBeanDefinition.getInitMethodNames()).containsExactly("initMethod");
		assertThat(mergedBeanDefinition.getDestroyMethodNames()).containsExactly("destroyMethod");
	}

	@Test
	void processAheadOfTimeWhenHasInitDestroyAnnotationsAndCustomDefinedMethodNamesAddsMethodNames() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(InitDestroyBean.class);
		beanDefinition.setInitMethodName("customInitMethod");
		beanDefinition.setDestroyMethodNames("customDestroyMethod");
		processAheadOfTime(beanDefinition);
		RootBeanDefinition mergedBeanDefinition = getMergedBeanDefinition();
		assertThat(mergedBeanDefinition.getInitMethodNames()).containsExactly("initMethod", "customInitMethod");
		assertThat(mergedBeanDefinition.getDestroyMethodNames()).containsExactly("destroyMethod", "customDestroyMethod");
	}

	@Test
	void processAheadOfTimeWhenHasInitDestroyAnnotationsAndOverlappingCustomDefinedMethodNamesFiltersDuplicates() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(InitDestroyBean.class);
		beanDefinition.setInitMethodName("initMethod");
		beanDefinition.setDestroyMethodNames("destroyMethod");
		processAheadOfTime(beanDefinition);
		RootBeanDefinition mergedBeanDefinition = getMergedBeanDefinition();
		assertThat(mergedBeanDefinition.getInitMethodNames()).containsExactly("initMethod");
		assertThat(mergedBeanDefinition.getDestroyMethodNames()).containsExactly("destroyMethod");
	}

	@Test
	void processAheadOfTimeWhenHasInferredDestroyMethodAddsDestroyMethodName() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(InferredDestroyBean.class);
		beanDefinition.setDestroyMethodNames(AbstractBeanDefinition.INFER_METHOD);
		processAheadOfTime(beanDefinition);
		RootBeanDefinition mergedBeanDefinition = getMergedBeanDefinition();
		assertThat(mergedBeanDefinition.getInitMethodNames()).isNull();
		assertThat(mergedBeanDefinition.getDestroyMethodNames()).containsExactly("close");
	}

	@Test
	void processAheadOfTimeWhenHasInferredDestroyMethodAndNoCandidateDoesNotMutateRootBeanDefinition() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(NoInitDestroyBean.class);
		beanDefinition.setDestroyMethodNames(AbstractBeanDefinition.INFER_METHOD);
		processAheadOfTime(beanDefinition);
		RootBeanDefinition mergedBeanDefinition = getMergedBeanDefinition();
		assertThat(mergedBeanDefinition.getInitMethodNames()).isNull();
		assertThat(mergedBeanDefinition.getDestroyMethodNames()).isNull();
	}

	@Test
	void processAheadOfTimeWhenHasMultipleInitDestroyAnnotationsAddsAllMethodNames() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(MultiInitDestroyBean.class);
		processAheadOfTime(beanDefinition);
		RootBeanDefinition mergedBeanDefinition = getMergedBeanDefinition();
		assertThat(mergedBeanDefinition.getInitMethodNames()).containsExactly("initMethod", "anotherInitMethod");
		assertThat(mergedBeanDefinition.getDestroyMethodNames()).containsExactly("anotherDestroyMethod", "destroyMethod");
	}

	@Test
	void processAheadOfTimeWithMultipleLevelsOfPublicAndPrivateInitAndDestroyMethods() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(CustomAnnotatedPrivateSameNameInitDestroyBean.class);
		// We explicitly define "afterPropertiesSet" as a "custom init method"
		// to ensure that it will be tracked as such even though it has the same
		// name as InitializingBean#afterPropertiesSet().
		beanDefinition.setInitMethodNames("afterPropertiesSet", "customInit");
		// We explicitly define "destroy" as a "custom destroy method"
		// to ensure that it will be tracked as such even though it has the same
		// name as DisposableBean#destroy().
		beanDefinition.setDestroyMethodNames("destroy", "customDestroy");
		processAheadOfTime(beanDefinition);
		RootBeanDefinition mergedBeanDefinition = getMergedBeanDefinition();
		assertSoftly(softly -> {
			softly.assertThat(mergedBeanDefinition.getInitMethodNames()).containsExactly(
					CustomAnnotatedPrivateInitDestroyBean.class.getName() + ".privateInit", // fully-qualified private method
					CustomAnnotatedPrivateSameNameInitDestroyBean.class.getName() + ".privateInit", // fully-qualified private method
					"afterPropertiesSet",
					"customInit"
				);
			softly.assertThat(mergedBeanDefinition.getDestroyMethodNames()).containsExactly(
					CustomAnnotatedPrivateSameNameInitDestroyBean.class.getName() + ".privateDestroy", // fully-qualified private method
					CustomAnnotatedPrivateInitDestroyBean.class.getName() + ".privateDestroy", // fully-qualified private method
					"destroy",
					"customDestroy"
				);
		});
	}

	private void processAheadOfTime(RootBeanDefinition beanDefinition) {
		RegisteredBean registeredBean = registerBean(beanDefinition);
		assertThat(createAotBeanPostProcessor().processAheadOfTime(registeredBean)).isNull();
	}

	private RegisteredBean registerBean(RootBeanDefinition beanDefinition) {
		String beanName = "test";
		this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
		return RegisteredBean.of(this.beanFactory, beanName);
	}

	private RootBeanDefinition getMergedBeanDefinition() {
		return (RootBeanDefinition) this.beanFactory.getMergedBeanDefinition("test");
	}

	private InitDestroyAnnotationBeanPostProcessor createAotBeanPostProcessor() {
		InitDestroyAnnotationBeanPostProcessor beanPostProcessor = new InitDestroyAnnotationBeanPostProcessor();
		beanPostProcessor.setInitAnnotationType(Init.class);
		beanPostProcessor.setDestroyAnnotationType(Destroy.class);
		return beanPostProcessor;
	}

	static class NoInitDestroyBean {}

	static class CustomInitDestroyBean {

		public void customInit() {
		}

		public void customDestroy() {
		}
	}

	static class CustomInitializingDisposableBean extends CustomInitDestroyBean
			implements InitializingBean, DisposableBean {

		@Override
		public void afterPropertiesSet() {
		}

		@Override
		public void destroy() {
		}
	}

	static class CustomAnnotatedPrivateInitDestroyBean extends CustomInitializingDisposableBean {

		@Init
		private void privateInit() {
		}

		@Destroy
		private void privateDestroy() {
		}
	}

	static class CustomAnnotatedPrivateSameNameInitDestroyBean extends CustomAnnotatedPrivateInitDestroyBean {

		@Init
		@SuppressWarnings("unused")
		private void privateInit() {
		}

		@Destroy
		@SuppressWarnings("unused")
		private void privateDestroy() {
		}
	}

}
