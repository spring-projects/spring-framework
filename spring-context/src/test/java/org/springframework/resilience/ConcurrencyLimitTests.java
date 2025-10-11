/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.resilience;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.interceptor.ConcurrencyThrottleInterceptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.testfixture.env.MockPropertySource;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.ConcurrencyLimitBeanPostProcessor;
import org.springframework.resilience.annotation.EnableResilientMethods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Juergen Hoeller
 * @author Hyunsang Han
 * @author Sam Brannen
 * @since 7.0
 */
class ConcurrencyLimitTests {

	@Test
	void withSimpleInterceptor() {
		NonAnnotatedBean target = new NonAnnotatedBean();
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(target);
		pf.addAdvice(new ConcurrencyThrottleInterceptor(2));
		NonAnnotatedBean proxy = (NonAnnotatedBean) pf.getProxy();

		List<CompletableFuture<?>> futures = new ArrayList<>(10);
		for (int i = 0; i < 10; i++) {
			futures.add(CompletableFuture.runAsync(proxy::concurrentOperation));
		}
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		assertThat(target.current).hasValue(0);
		assertThat(target.counter).hasValue(10);
	}

	@Test
	void withPostProcessorForMethod() {
		AnnotatedMethodBean proxy = createProxy(AnnotatedMethodBean.class);
		AnnotatedMethodBean target = (AnnotatedMethodBean) AopProxyUtils.getSingletonTarget(proxy);

		List<CompletableFuture<?>> futures = new ArrayList<>(10);
		for (int i = 0; i < 10; i++) {
			futures.add(CompletableFuture.runAsync(proxy::concurrentOperation));
		}
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		assertThat(target.current).hasValue(0);
	}

	@Test
	void withPostProcessorForMethodWithUnboundedConcurrency() {
		AnnotatedMethodBean proxy = createProxy(AnnotatedMethodBean.class);
		AnnotatedMethodBean target = (AnnotatedMethodBean) AopProxyUtils.getSingletonTarget(proxy);

		List<CompletableFuture<?>> futures = new ArrayList<>(10);
		for (int i = 0; i < 10; i++) {
			futures.add(CompletableFuture.runAsync(proxy::unboundedConcurrency));
		}
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		assertThat(target.current).hasValue(10);
	}

	@Test
	void withPostProcessorForClass() {
		AnnotatedClassBean proxy = createProxy(AnnotatedClassBean.class);
		AnnotatedClassBean target = (AnnotatedClassBean) AopProxyUtils.getSingletonTarget(proxy);

		List<CompletableFuture<?>> futures = new ArrayList<>(30);
		for (int i = 0; i < 10; i++) {
			futures.add(CompletableFuture.runAsync(proxy::concurrentOperation));
		}
		for (int i = 0; i < 10; i++) {
			futures.add(CompletableFuture.runAsync(proxy::otherOperation));
		}
		for (int i = 0; i < 10; i++) {
			futures.add(CompletableFuture.runAsync(proxy::overrideOperation));
		}
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		assertThat(target.current).hasValue(0);
	}

	@Test
	void withPlaceholderResolution() {
		MockPropertySource mockPropertySource = new MockPropertySource("test").withProperty("test.concurrency.limit", "3");
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.getEnvironment().getPropertySources().addFirst(mockPropertySource);
		ctx.register(PlaceholderTestConfig.class, PlaceholderBean.class);
		ctx.refresh();

		PlaceholderBean proxy = ctx.getBean(PlaceholderBean.class);
		PlaceholderBean target = (PlaceholderBean) AopProxyUtils.getSingletonTarget(proxy);

		// Test with limit=3 from MockPropertySource
		List<CompletableFuture<?>> futures = new ArrayList<>(10);
		for (int i = 0; i < 10; i++) {
			futures.add(CompletableFuture.runAsync(proxy::concurrentOperation));
		}
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		assertThat(target.current).hasValue(0);
		ctx.close();
	}

	@Test
	void configurationErrors() {
		ConfigurationErrorsBean proxy = createProxy(ConfigurationErrorsBean.class);

		assertThatIllegalStateException()
				.isThrownBy(proxy::emptyDeclaration)
				.withMessageMatching("@.+?ConcurrencyLimit(.+?) must be configured with a valid limit")
				.withMessageContaining("\"\"")
				.withMessageContaining(String.valueOf(Integer.MIN_VALUE));

		assertThatIllegalStateException()
				.isThrownBy(proxy::negative42Int)
				.withMessageMatching("@.+?ConcurrencyLimit(.+?) must be configured with a valid limit")
				.withMessageContaining("-42");

		assertThatIllegalStateException()
				.isThrownBy(proxy::negative42String)
				.withMessageMatching("@.+?ConcurrencyLimit(.+?) must be configured with a valid limit")
				.withMessageContaining("-42");

		assertThatExceptionOfType(NumberFormatException.class)
				.isThrownBy(proxy::alphanumericString)
				.withMessageContaining("B2");
	}


	private static <T> T createProxy(Class<T> beanClass) {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("bean", new RootBeanDefinition(beanClass));
		ConcurrencyLimitBeanPostProcessor bpp = new ConcurrencyLimitBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		return bf.getBean(beanClass);
	}


	static class NonAnnotatedBean {

		final AtomicInteger current = new AtomicInteger();

		final AtomicInteger counter = new AtomicInteger();

		public void concurrentOperation() {
			if (current.incrementAndGet() > 2) {
				throw new IllegalStateException();
			}
			try {
				Thread.sleep(10);
			}
			catch (InterruptedException ex) {
				throw new IllegalStateException(ex);
			}
			current.decrementAndGet();
			counter.incrementAndGet();
		}
	}


	static class AnnotatedMethodBean {

		final AtomicInteger current = new AtomicInteger();

		@ConcurrencyLimit(2)
		public void concurrentOperation() {
			if (current.incrementAndGet() > 2) {
				throw new IllegalStateException();
			}
			try {
				Thread.sleep(10);
			}
			catch (InterruptedException ex) {
				throw new IllegalStateException(ex);
			}
			current.decrementAndGet();
		}

		@ConcurrencyLimit(limit = -1)
		public void unboundedConcurrency() {
			current.incrementAndGet();
			try {
				Thread.sleep(10);
			}
			catch (InterruptedException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}


	@ConcurrencyLimit(2)
	static class AnnotatedClassBean {

		final AtomicInteger current = new AtomicInteger();

		final AtomicInteger currentOverride = new AtomicInteger();

		public void concurrentOperation() {
			if (current.incrementAndGet() > 2) {
				throw new IllegalStateException();
			}
			try {
				Thread.sleep(10);
			}
			catch (InterruptedException ex) {
				throw new IllegalStateException(ex);
			}
			current.decrementAndGet();
		}

		public void otherOperation() {
			if (current.incrementAndGet() > 2) {
				throw new IllegalStateException();
			}
			try {
				Thread.sleep(10);
			}
			catch (InterruptedException ex) {
				throw new IllegalStateException(ex);
			}
			current.decrementAndGet();
		}

		@ConcurrencyLimit(limit = 1)
		public void overrideOperation() {
			if (currentOverride.incrementAndGet() > 1) {
				throw new IllegalStateException();
			}
			try {
				Thread.sleep(10);
			}
			catch (InterruptedException ex) {
				throw new IllegalStateException(ex);
			}
			currentOverride.decrementAndGet();
		}
	}


	@EnableResilientMethods
	static class PlaceholderTestConfig {
	}


	static class PlaceholderBean {

		final AtomicInteger current = new AtomicInteger();

		@ConcurrencyLimit(limitString = "${test.concurrency.limit}")
		public void concurrentOperation() {
			if (current.incrementAndGet() > 3) {  // Assumes test.concurrency.limit=3
				throw new IllegalStateException();
			}
			try {
				Thread.sleep(10);
			}
			catch (InterruptedException ex) {
				throw new IllegalStateException(ex);
			}
			current.decrementAndGet();
		}
	}


	static class ConfigurationErrorsBean {

		@ConcurrencyLimit
		public void emptyDeclaration() {
		}

		@ConcurrencyLimit(-42)
		public void negative42Int() {
		}

		@ConcurrencyLimit(limitString = "-42")
		public void negative42String() {
		}

		@ConcurrencyLimit(limitString = "B2")
		public void alphanumericString() {
		}
	}

}
