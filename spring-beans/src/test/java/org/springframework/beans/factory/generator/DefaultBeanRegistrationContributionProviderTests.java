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

package org.springframework.beans.factory.generator;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.factory.generator.SimpleConfiguration;
import org.springframework.core.Ordered;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link DefaultBeanRegistrationContributionProvider}.
 *
 * @author Stephane Nicoll
 */
class DefaultBeanRegistrationContributionProviderTests {

	@Test
	void aotContributingBeanPostProcessorsAreIncluded() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		AotContributingBeanPostProcessor first = mockNoOpPostProcessor(-1);
		AotContributingBeanPostProcessor second = mockNoOpPostProcessor(5);
		beanFactory.registerBeanDefinition("second", BeanDefinitionBuilder.rootBeanDefinition(
				AotContributingBeanPostProcessor.class, () -> second).getBeanDefinition());
		beanFactory.registerBeanDefinition("first", BeanDefinitionBuilder.rootBeanDefinition(
				AotContributingBeanPostProcessor.class, () -> first).getBeanDefinition());
		RootBeanDefinition beanDefinition = new RootBeanDefinition(SimpleConfiguration.class);
		new DefaultBeanRegistrationContributionProvider(beanFactory).getContributionFor(
				"test", beanDefinition);
		verify((Ordered) second).getOrder();
		verify((Ordered) first).getOrder();
		verify(first).contribute(beanDefinition, SimpleConfiguration.class, "test");
		verify(second).contribute(beanDefinition, SimpleConfiguration.class, "test");
		verifyNoMoreInteractions(first, second);
	}


	private AotContributingBeanPostProcessor mockNoOpPostProcessor(int order) {
		AotContributingBeanPostProcessor postProcessor = mock(AotContributingBeanPostProcessor.class);
		given(postProcessor.contribute(any(), any(), any())).willReturn(null);
		given(postProcessor.getOrder()).willReturn(order);
		return postProcessor;
	}

}
