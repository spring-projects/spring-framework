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

package org.springframework.cache.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.aop.config.AopConfigUtils;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.testfixture.aot.generate.TestGenerationContext;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CachingBeanRegistrationAotProcessor}.
 *
 * @author Sebastien Deleuze
 */
public class CachingBeanRegistrationAotProcessorTests {

	BeanRegistrationAotProcessor processor = new CachingBeanRegistrationAotProcessor();

	GenerationContext generationContext = new TestGenerationContext();


	@Test
	void ignoresNonCachingBean() {
		assertThat(createContribution(NonCaching.class, false)).isNull();
	}

	@Test
	void contributesProxyForCacheableInterface() {
		process(CacheableServiceImpl.class, false);
		RuntimeHints runtimeHints = this.generationContext.getRuntimeHints();
		assertThat(RuntimeHintsPredicates.proxies().forInterfaces(AopProxyUtils.completeJdkProxyInterfaces(CacheableServiceInterface.class))).accepts(runtimeHints);
	}

	@Test
	void ignoresProxyForCacheableInterfaceWithClassProxying() {
		assertThat(createContribution(CacheableServiceImpl.class, true)).isNull();
	}

	@Test
	void ignoresProxyForCacheableClass() {
		assertThat(createContribution(CacheableService.class, true)).isNull();
	}

	@Nullable
	private BeanRegistrationAotContribution createContribution(Class<?> beanClass, boolean forceClassProxying) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		if (forceClassProxying) {
			AopConfigUtils.registerAutoProxyCreatorIfNecessary(beanFactory);
			AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(beanFactory);
		}
		beanFactory.registerBeanDefinition(beanClass.getName(), new RootBeanDefinition(beanClass));
		return this.processor.processAheadOfTime(RegisteredBean.of(beanFactory, beanClass.getName()));
	}

	private void process(Class<?> beanClass, boolean forceClassProxying) {
		BeanRegistrationAotContribution contribution = createContribution(beanClass, forceClassProxying);
		assertThat(contribution).isNotNull();
		contribution.applyTo(this.generationContext, mock(BeanRegistrationCode.class));
	}

	static class NonCaching {
	}

	interface CacheableServiceInterface {

		@Cacheable
		void invoke();
	}

	class CacheableServiceImpl implements CacheableServiceInterface {

		@Override
		public void invoke() {
		}
	}

	class CacheableService {

		@Cacheable
		public void invoke() {
		}
	}
}
