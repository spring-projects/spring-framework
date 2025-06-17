/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.aop.retry;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.AccessDeniedException;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.retry.annotation.RetryAnnotationBeanPostProcessor;
import org.springframework.aop.retry.annotation.RetryAnnotationInterceptor;
import org.springframework.aop.retry.annotation.Retryable;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * @author Juergen Hoeller
 * @since 7.0
 */
public class RetryInterceptorTests {

	@Test
	void withSimpleInterceptor() {
		NonAnnotatedBean target = new NonAnnotatedBean();
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(target);
		pf.addAdvice(new SimpleRetryInterceptor(new MethodRetrySpec((m, t) -> true, 5, 10)));
		NonAnnotatedBean proxy = (NonAnnotatedBean) pf.getProxy();

		assertThatIOException().isThrownBy(proxy::retryOperation).withMessage("6");
		assertThat(target.counter).isEqualTo(6);
	}

	@Test
	void withAnnotationInterceptorForMethod() {
		AnnotatedMethodBean target = new AnnotatedMethodBean();
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(target);
		pf.addAdvice(new RetryAnnotationInterceptor());
		AnnotatedMethodBean proxy = (AnnotatedMethodBean) pf.getProxy();

		assertThatIOException().isThrownBy(proxy::retryOperation).withMessage("6");
		assertThat(target.counter).isEqualTo(6);
	}

	@Test
	void withPostProcessorForMethod() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("bean", new RootBeanDefinition(AnnotatedMethodBean.class));
		RetryAnnotationBeanPostProcessor bpp = new RetryAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		AnnotatedMethodBean proxy = bf.getBean(AnnotatedMethodBean.class);
		AnnotatedMethodBean target = (AnnotatedMethodBean) AopProxyUtils.getSingletonTarget(proxy);

		assertThatIOException().isThrownBy(proxy::retryOperation).withMessage("6");
		assertThat(target.counter).isEqualTo(6);
	}

	@Test
	void withAnnotationInterceptorForClass() {
		AnnotatedClassBean target = new AnnotatedClassBean();
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(target);
		pf.addAdvice(new RetryAnnotationInterceptor());
		AnnotatedClassBean proxy = (AnnotatedClassBean) pf.getProxy();

		assertThatIOException().isThrownBy(proxy::retryOperation).withMessage("3");
		assertThat(target.counter).isEqualTo(3);
		assertThatIOException().isThrownBy(proxy::otherOperation);
		assertThat(target.counter).isEqualTo(4);
		assertThatIOException().isThrownBy(proxy::overrideOperation);
		assertThat(target.counter).isEqualTo(6);
	}

	@Test
	void withPostProcessorForClass() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("bean", new RootBeanDefinition(AnnotatedClassBean.class));
		RetryAnnotationBeanPostProcessor bpp = new RetryAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		AnnotatedClassBean proxy = bf.getBean(AnnotatedClassBean.class);
		AnnotatedClassBean target = (AnnotatedClassBean) AopProxyUtils.getSingletonTarget(proxy);

		assertThatIOException().isThrownBy(proxy::retryOperation).withMessage("3");
		assertThat(target.counter).isEqualTo(3);
		assertThatIOException().isThrownBy(proxy::otherOperation);
		assertThat(target.counter).isEqualTo(4);
		assertThatIOException().isThrownBy(proxy::overrideOperation);
		assertThat(target.counter).isEqualTo(6);
	}


	public static class NonAnnotatedBean {

		int counter = 0;

		public void retryOperation() throws IOException {
			counter++;
			throw new IOException(Integer.toString(counter));
		}
	}


	public static class AnnotatedMethodBean {

		int counter = 0;

		@Retryable(maxAttempts = 5, delay = 10)
		public void retryOperation() throws IOException {
			counter++;
			throw new IOException(Integer.toString(counter));
		}
	}


	@Retryable(delay = 10, jitterDelay = 5, delayMultiplier = 2.0, maxDelay = 40,
			includes = IOException.class, excludes = AccessDeniedException.class,
			predicate = CustomPredicate.class)
	public static class AnnotatedClassBean {

		int counter = 0;

		public void retryOperation() throws IOException {
			counter++;
			throw new IOException(Integer.toString(counter));
		}

		public void otherOperation() throws IOException {
			counter++;
			throw new AccessDeniedException(Integer.toString(counter));
		}

		@Retryable(value = IOException.class, maxAttempts = 1, delay = 10)
		public void overrideOperation() throws IOException {
			counter++;
			throw new AccessDeniedException(Integer.toString(counter));
		}
	}


	private static class CustomPredicate implements MethodRetryPredicate {

		@Override
		public boolean shouldRetry(Method method, Throwable throwable) {
			return !"3".equals(throwable.getMessage());
		}
	}

}
