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

package org.springframework.beans.factory.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.generator.BeanInstantiationContribution;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.factory.generator.lifecycle.Destroy;
import org.springframework.beans.testfixture.beans.factory.generator.lifecycle.Init;
import org.springframework.beans.testfixture.beans.factory.generator.lifecycle.InitDestroyBean;
import org.springframework.beans.testfixture.beans.factory.generator.lifecycle.MultiInitDestroyBean;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link InitDestroyAnnotationBeanPostProcessor}.
 *
 * @author Stephane Nicoll
 */
class InitDestroyAnnotationBeanPostProcessorTests {

	@Test
	void contributeWithNoCallbackDoesNotMutateRootBeanDefinition() {
		RootBeanDefinition beanDefinition = mock(RootBeanDefinition.class);
		assertThat(createAotContributingBeanPostProcessor().contribute(
				beanDefinition, String.class, "test")).isNull();
		verifyNoInteractions(beanDefinition);
	}

	@Test
	void contributeWithInitDestroyCallback() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(InitDestroyBean.class);
		assertThat(createContribution(beanDefinition)).isNull();
		assertThat(beanDefinition.getInitMethodNames()).containsExactly("initMethod");
		assertThat(beanDefinition.getDestroyMethodNames()).containsExactly("destroyMethod");
	}

	@Test
	void contributeWithInitDestroyCallbackRetainCustomMethods() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(InitDestroyBean.class);
		beanDefinition.setInitMethodName("customInitMethod");
		beanDefinition.setDestroyMethodNames("customDestroyMethod");
		assertThat(createContribution(beanDefinition)).isNull();
		assertThat(beanDefinition.getInitMethodNames())
				.containsExactly("customInitMethod", "initMethod");
		assertThat(beanDefinition.getDestroyMethodNames())
				.containsExactly("customDestroyMethod", "destroyMethod");
	}

	@Test
	void contributeWithInitDestroyCallbackFilterDuplicates() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(InitDestroyBean.class);
		beanDefinition.setInitMethodName("initMethod");
		beanDefinition.setDestroyMethodNames("destroyMethod");
		assertThat(createContribution(beanDefinition)).isNull();
		assertThat(beanDefinition.getInitMethodNames()).containsExactly("initMethod");
		assertThat(beanDefinition.getDestroyMethodNames()).containsExactly("destroyMethod");
	}

	@Test
	void contributeWithMultipleInitDestroyCallbacks() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(MultiInitDestroyBean.class);
		assertThat(createContribution(beanDefinition)).isNull();
		assertThat(beanDefinition.getInitMethodNames())
				.containsExactly("initMethod", "anotherInitMethod");
		assertThat(beanDefinition.getDestroyMethodNames())
				.containsExactly("anotherDestroyMethod", "destroyMethod");
	}

	@Nullable
	private BeanInstantiationContribution createContribution(RootBeanDefinition beanDefinition) {
		InitDestroyAnnotationBeanPostProcessor bpp = createAotContributingBeanPostProcessor();
		return bpp.contribute(beanDefinition, beanDefinition.getResolvableType().toClass(), "test");
	}

	private InitDestroyAnnotationBeanPostProcessor createAotContributingBeanPostProcessor() {
		InitDestroyAnnotationBeanPostProcessor bpp = new InitDestroyAnnotationBeanPostProcessor();
		bpp.setInitAnnotationType(Init.class);
		bpp.setDestroyAnnotationType(Destroy.class);
		return bpp;
	}

}
