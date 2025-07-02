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
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.ConcurrencyLimitBeanPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
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
		assertThat(target.counter).hasValue(0);
	}

	@Test
	void withPostProcessorForMethod() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("bean", new RootBeanDefinition(AnnotatedMethodBean.class));
		ConcurrencyLimitBeanPostProcessor bpp = new ConcurrencyLimitBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		AnnotatedMethodBean proxy = bf.getBean(AnnotatedMethodBean.class);
		AnnotatedMethodBean target = (AnnotatedMethodBean) AopProxyUtils.getSingletonTarget(proxy);

		List<CompletableFuture<?>> futures = new ArrayList<>(10);
		for (int i = 0; i < 10; i++) {
			futures.add(CompletableFuture.runAsync(proxy::concurrentOperation));
		}
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		assertThat(target.current).hasValue(0);
	}

	@Test
	void withPostProcessorForClass() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("bean", new RootBeanDefinition(AnnotatedClassBean.class));
		ConcurrencyLimitBeanPostProcessor bpp = new ConcurrencyLimitBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		AnnotatedClassBean proxy = bf.getBean(AnnotatedClassBean.class);
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


	static class NonAnnotatedBean {

		AtomicInteger counter = new AtomicInteger();

		public void concurrentOperation() {
			if (counter.incrementAndGet() > 2) {
				throw new IllegalStateException();
			}
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException ex) {
				throw new IllegalStateException(ex);
			}
			counter.decrementAndGet();
		}
	}


	static class AnnotatedMethodBean {

		AtomicInteger current = new AtomicInteger();

		@ConcurrencyLimit(2)
		public void concurrentOperation() {
			if (current.incrementAndGet() > 2) {
				throw new IllegalStateException();
			}
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException ex) {
				throw new IllegalStateException(ex);
			}
			current.decrementAndGet();
		}
	}


	@ConcurrencyLimit(2)
	static class AnnotatedClassBean {

		AtomicInteger current = new AtomicInteger();

		AtomicInteger currentOverride = new AtomicInteger();

		public void concurrentOperation() {
			if (current.incrementAndGet() > 2) {
				throw new IllegalStateException();
			}
			try {
				Thread.sleep(100);
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
				Thread.sleep(100);
			}
			catch (InterruptedException ex) {
				throw new IllegalStateException(ex);
			}
			current.decrementAndGet();
		}

		@ConcurrencyLimit(1)
		public void overrideOperation() {
			if (currentOverride.incrementAndGet() > 1) {
				throw new IllegalStateException();
			}
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException ex) {
				throw new IllegalStateException(ex);
			}
			currentOverride.decrementAndGet();
		}
	}

}
