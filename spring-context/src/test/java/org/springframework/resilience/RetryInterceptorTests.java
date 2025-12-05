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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.MalformedInputException;
import java.nio.file.AccessDeniedException;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.aop.config.AopConfigUtils;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.aop.interceptor.SimpleTraceInterceptor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.resilience.annotation.RetryAnnotationBeanPostProcessor;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.resilience.retry.MethodRetrySpec;
import org.springframework.resilience.retry.SimpleRetryInterceptor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

/**
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 7.0
 */
class RetryInterceptorTests {

	@Test
	void withSimpleInterceptor() {
		NonAnnotatedBean target = new NonAnnotatedBean();
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(target);
		pf.addAdvice(new SimpleRetryInterceptor(
				new MethodRetrySpec((m, t) -> true, 5, Duration.ofMillis(10))));
		pf.addAdvice(new SimpleTraceInterceptor());
		PlainInterface proxy = (PlainInterface) pf.getProxy();

		assertThatIOException().isThrownBy(proxy::retryOperation).withMessage("6");
		assertThat(target.counter).isEqualTo(6);
	}

	@Test
	void withSimpleInterceptorAndNoTarget() {
		NonAnnotatedBean target = new NonAnnotatedBean();
		ProxyFactory pf = new ProxyFactory();
		pf.addAdvice(new SimpleRetryInterceptor(
				new MethodRetrySpec((m, t) -> true, 5, Duration.ofMillis(10))));
		pf.addAdvice(new SimpleTraceInterceptor());
		pf.addAdvice((MethodInterceptor) invocation -> {
			try {
				return invocation.getMethod().invoke(target, invocation.getArguments());
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		});
		pf.addInterface(PlainInterface.class);
		PlainInterface proxy = (PlainInterface) pf.getProxy();

		assertThatIOException().isThrownBy(proxy::retryOperation).withMessage("6");
		assertThat(target.counter).isEqualTo(6);
	}

	@Test
	void withPostProcessorForMethod() {
		DefaultListableBeanFactory bf = createBeanFactoryFor(AnnotatedMethodBean.class);
		AnnotatedMethodBean proxy = bf.getBean(AnnotatedMethodBean.class);
		AnnotatedMethodBean target = (AnnotatedMethodBean) AopProxyUtils.getSingletonTarget(proxy);

		assertThatIOException().isThrownBy(proxy::retryOperation).withMessage("6");
		assertThat(target.counter).isEqualTo(6);
	}

	@Test
	void withPostProcessorForMethodWithInterface() {
		DefaultListableBeanFactory bf = createBeanFactoryFor(AnnotatedMethodBeanWithInterface.class);
		AnnotatedInterface proxy = bf.getBean(AnnotatedInterface.class);
		AnnotatedMethodBeanWithInterface target = (AnnotatedMethodBeanWithInterface) AopProxyUtils.getSingletonTarget(proxy);

		assertThat(AopUtils.isJdkDynamicProxy(proxy)).isTrue();
		assertThatIOException().isThrownBy(proxy::retryOperation).withMessage("6");
		assertThat(target.counter).isEqualTo(6);
	}

	@Test
	void withPostProcessorForMethodWithInterfaceAndDefaultTargetClass() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(bf);
		bf.registerBeanDefinition("bean", new RootBeanDefinition(AnnotatedMethodBeanWithInterface.class));
		RetryAnnotationBeanPostProcessor bpp = new RetryAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		AnnotatedInterface proxy = bf.getBean(AnnotatedInterface.class);
		AnnotatedMethodBeanWithInterface target = (AnnotatedMethodBeanWithInterface) AopProxyUtils.getSingletonTarget(proxy);

		assertThat(AopUtils.isCglibProxy(proxy)).isTrue();
		assertThatIOException().isThrownBy(proxy::retryOperation).withMessage("6");
		assertThat(target.counter).isEqualTo(6);
	}

	@Test
	void withPostProcessorForMethodWithInterfaceAndPreserveTargetClass() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition bd = new RootBeanDefinition(AnnotatedMethodBeanWithInterface.class);
		bd.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
		bf.registerBeanDefinition("bean", bd);
		RetryAnnotationBeanPostProcessor bpp = new RetryAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		AnnotatedInterface proxy = bf.getBean(AnnotatedInterface.class);
		AnnotatedMethodBeanWithInterface target = (AnnotatedMethodBeanWithInterface) AopProxyUtils.getSingletonTarget(proxy);

		assertThat(AopUtils.isCglibProxy(proxy)).isTrue();
		assertThatIOException().isThrownBy(proxy::retryOperation).withMessage("6");
		assertThat(target.counter).isEqualTo(6);
	}

	@Test
	void withPostProcessorForMethodWithInterfaceAndExposeInterfaces() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(bf);
		RootBeanDefinition bd = new RootBeanDefinition(AnnotatedMethodBeanWithInterface.class);
		bd.setAttribute(AutoProxyUtils.EXPOSED_INTERFACES_ATTRIBUTE, AutoProxyUtils.ALL_INTERFACES_ATTRIBUTE_VALUE);
		bf.registerBeanDefinition("bean", bd);
		RetryAnnotationBeanPostProcessor bpp = new RetryAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		AnnotatedInterface proxy = bf.getBean(AnnotatedInterface.class);
		AnnotatedMethodBeanWithInterface target = (AnnotatedMethodBeanWithInterface) AopProxyUtils.getSingletonTarget(proxy);

		assertThat(AopUtils.isJdkDynamicProxy(proxy)).isTrue();
		assertThatIOException().isThrownBy(proxy::retryOperation).withMessage("6");
		assertThat(target.counter).isEqualTo(6);
	}

	@Test
	void withPostProcessorForClass() {
		DefaultListableBeanFactory bf = createBeanFactoryFor(AnnotatedClassBean.class);
		AnnotatedClassBean proxy = bf.getBean(AnnotatedClassBean.class);
		AnnotatedClassBean target = (AnnotatedClassBean) AopProxyUtils.getSingletonTarget(proxy);

		// 3 = 1 initial invocation + 2 retry attempts
		// Not 3 retry attempts, because RejectMalformedInputException3Predicate rejects
		// a retry if the last exception was a MalformedInputException with message "3".
		assertThatIOException().isThrownBy(proxy::retryOperation).withMessageContaining("3");
		assertThat(target.counter).isEqualTo(3);
		// 7 = 3 + 1 initial invocation + 3 retry attempts
		assertThatRuntimeException()
				.isThrownBy(proxy::retryOperationWithNestedException)
				.havingCause()
					.isExactlyInstanceOf(IOException.class)
					.withMessage("7");
		assertThat(target.counter).isEqualTo(7);
		assertThatIOException().isThrownBy(proxy::otherOperation);
		assertThat(target.counter).isEqualTo(8);
		assertThatIOException().isThrownBy(proxy::overrideOperation);
		assertThat(target.counter).isEqualTo(10);
	}

	@Test
	void withPostProcessorForClassWithStrings() {
		Properties props = new Properties();
		props.setProperty("delay", "10");
		props.setProperty("jitter", "5");
		props.setProperty("multiplier", "2.0");
		props.setProperty("maxDelay", "40");
		props.setProperty("limitedRetries", "1");

		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("props", props));
		ctx.registerBeanDefinition("bean", new RootBeanDefinition(AnnotatedClassBeanWithStrings.class));
		ctx.registerBeanDefinition("bpp", new RootBeanDefinition(RetryAnnotationBeanPostProcessor.class));
		ctx.refresh();
		AnnotatedClassBeanWithStrings proxy = ctx.getBean(AnnotatedClassBeanWithStrings.class);
		AnnotatedClassBeanWithStrings target = (AnnotatedClassBeanWithStrings) AopProxyUtils.getSingletonTarget(proxy);

		// 3 = 1 initial invocation + 2 retry attempts
		// Not 3 retry attempts, because RejectMalformedInputException3Predicate rejects
		// a retry if the last exception was a MalformedInputException with message "3".
		assertThatIOException().isThrownBy(proxy::retryOperation).withMessageContaining("3");
		assertThat(target.counter).isEqualTo(3);
		assertThatIOException().isThrownBy(proxy::otherOperation);
		assertThat(target.counter).isEqualTo(4);
		assertThatIOException().isThrownBy(proxy::overrideOperation);
		assertThat(target.counter).isEqualTo(6);
	}

	@Test
	void withPostProcessorForClassWithZeroAttempts() {
		Properties props = new Properties();
		props.setProperty("delay", "10");
		props.setProperty("jitter", "5");
		props.setProperty("multiplier", "2.0");
		props.setProperty("maxDelay", "40");
		props.setProperty("limitedRetries", "0");

		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("props", props));
		ctx.registerBeanDefinition("bean", new RootBeanDefinition(AnnotatedClassBeanWithStrings.class));
		ctx.registerBeanDefinition("bpp", new RootBeanDefinition(RetryAnnotationBeanPostProcessor.class));
		ctx.refresh();
		AnnotatedClassBeanWithStrings proxy = ctx.getBean(AnnotatedClassBeanWithStrings.class);
		AnnotatedClassBeanWithStrings target = (AnnotatedClassBeanWithStrings) AopProxyUtils.getSingletonTarget(proxy);

		// 3 = 1 initial invocation + 2 retry attempts
		// Not 3 retry attempts, because RejectMalformedInputException3Predicate rejects
		// a retry if the last exception was a MalformedInputException with message "3".
		assertThatIOException().isThrownBy(proxy::retryOperation).withMessageContaining("3");
		assertThat(target.counter).isEqualTo(3);
		assertThatIOException().isThrownBy(proxy::otherOperation);
		assertThat(target.counter).isEqualTo(4);
		assertThatIOException().isThrownBy(proxy::overrideOperation);
		assertThat(target.counter).isEqualTo(5);
	}

	@Test
	void withEnableAnnotation() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.registerBeanDefinition("bean", new RootBeanDefinition(ConcurrencyLimitAnnotatedBean.class));
		ctx.registerBeanDefinition("config", new RootBeanDefinition(EnablingConfig.class));
		ctx.refresh();
		ConcurrencyLimitAnnotatedBean proxy = ctx.getBean(ConcurrencyLimitAnnotatedBean.class);
		ConcurrencyLimitAnnotatedBean target = (ConcurrencyLimitAnnotatedBean) AopProxyUtils.getSingletonTarget(proxy);

		Thread thread = new Thread(() -> assertThatIOException().isThrownBy(proxy::retryOperation));
		thread.start();
		assertThatIOException().isThrownBy(proxy::retryOperation);
		thread.join();
		assertThat(target.counter).hasValue(6);
		assertThat(target.threadChange).hasValue(2);
	}

	@Test
	void withEnableAnnotationAndDefaultTargetClass() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(ctx);
		ctx.registerBeanDefinition("bean", new RootBeanDefinition(AnnotatedMethodBeanWithInterface.class));
		ctx.registerBeanDefinition("config", new RootBeanDefinition(EnablingConfig.class));
		ctx.refresh();
		AnnotatedInterface proxy = ctx.getBean(AnnotatedInterface.class);
		AnnotatedMethodBeanWithInterface target = (AnnotatedMethodBeanWithInterface) AopProxyUtils.getSingletonTarget(proxy);

		assertThat(AopUtils.isCglibProxy(proxy)).isTrue();
		assertThatIOException().isThrownBy(proxy::retryOperation).withMessage("6");
		assertThat(target.counter).isEqualTo(6);
	}

	@Test
	void withEnableAnnotationAndPreserveTargetClass() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		RootBeanDefinition bd = new RootBeanDefinition(AnnotatedMethodBeanWithInterface.class);
		bd.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
		ctx.registerBeanDefinition("bean", bd);
		ctx.registerBeanDefinition("config", new RootBeanDefinition(EnablingConfig.class));
		ctx.refresh();
		AnnotatedInterface proxy = ctx.getBean(AnnotatedInterface.class);
		AnnotatedMethodBeanWithInterface target = (AnnotatedMethodBeanWithInterface) AopProxyUtils.getSingletonTarget(proxy);

		assertThat(AopUtils.isCglibProxy(proxy)).isTrue();
		assertThatIOException().isThrownBy(proxy::retryOperation).withMessage("6");
		assertThat(target.counter).isEqualTo(6);
	}

	@Test
	void withAsyncAnnotation() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.registerBeanDefinition("bean", new RootBeanDefinition(AsyncAnnotatedBean.class));
		ctx.registerBeanDefinition("config", new RootBeanDefinition(EnablingConfigWithAsync.class));
		ctx.refresh();
		AsyncAnnotatedBean proxy = ctx.getBean(AsyncAnnotatedBean.class);
		AsyncAnnotatedBean target = (AsyncAnnotatedBean) AopProxyUtils.getSingletonTarget(proxy);

		assertThatExceptionOfType(CompletionException.class).isThrownBy(() -> proxy.retryOperation().join())
				.withCauseInstanceOf(IllegalStateException.class);
		assertThat(target.counter).hasValue(3);
	}


	@Nested
	class TimeoutTests {

		private final DefaultListableBeanFactory bf = createBeanFactoryFor(AnnotatedMethodBean.class);
		private final AnnotatedMethodBean proxy = bf.getBean(AnnotatedMethodBean.class);
		private final AnnotatedMethodBean target = (AnnotatedMethodBean) AopProxyUtils.getSingletonTarget(proxy);

		@Test
		void timeoutNotExceededAfterInitialSuccess() {
			String result = proxy.retryOperationWithTimeoutNotExceededAfterInitialSuccess();
			assertThat(result).isEqualTo("success");
			// 1 initial attempt + 0 retries
			assertThat(target.counter).isEqualTo(1);
		}

		@Test
		void timeoutNotExceededAndRetriesExhausted() {
			assertThatIOException()
					.isThrownBy(proxy::retryOperationWithTimeoutNotExceededAndRetriesExhausted)
					.withMessage("4");
			// 1 initial attempt + 3 retries
			assertThat(target.counter).isEqualTo(4);
		}

		@Test
		void timeoutExceededAfterInitialFailure() {
			assertThatIOException()
					.isThrownBy(proxy::retryOperationWithTimeoutExceededAfterInitialFailure)
					.withMessage("1");
			// 1 initial attempt + 0 retries
			assertThat(target.counter).isEqualTo(1);
		}

		@Test
		void timeoutExceededAfterFirstDelayButBeforeFirstRetry() {
			assertThatIOException()
					.isThrownBy(proxy::retryOperationWithTimeoutExceededAfterFirstDelayButBeforeFirstRetry)
					.withMessage("1");
			// 1 initial attempt + 0 retries
			assertThat(target.counter).isEqualTo(1);
		}

		@Test
		void timeoutExceededAfterFirstRetry() {
			assertThatIOException()
					.isThrownBy(proxy::retryOperationWithTimeoutExceededAfterFirstRetry)
					.withMessage("2");
			// 1 initial attempt + 1 retry
			assertThat(target.counter).isEqualTo(2);
		}

		@Test
		void timeoutExceededAfterSecondRetry() {
			assertThatIOException()
					.isThrownBy(proxy::retryOperationWithTimeoutExceededAfterSecondRetry)
					.withMessage("3");
			// 1 initial attempt + 2 retries
			assertThat(target.counter).isEqualTo(3);
		}

	}


	private static DefaultListableBeanFactory createBeanFactoryFor(Class<?> beanClass) {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("bean", new RootBeanDefinition(beanClass));
		RetryAnnotationBeanPostProcessor bpp = new RetryAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		return bf;
	}


	static class NonAnnotatedBean implements PlainInterface {

		int counter = 0;

		@Override
		public void retryOperation() throws IOException {
			counter++;
			throw new IOException(Integer.toString(counter));
		}
	}


	public interface PlainInterface {

		void retryOperation() throws IOException;
	}


	static class AnnotatedMethodBean {

		int counter = 0;

		@Retryable(maxRetries = 5, delay = 10)
		public void retryOperation() throws IOException {
			counter++;
			throw new IOException(Integer.toString(counter));
		}

		@Retryable(timeout = 555, delay = 10)
		public String retryOperationWithTimeoutNotExceededAfterInitialSuccess() {
			counter++;
			return "success";
		}

		@Retryable(timeout = 555, delay = 10)
		public void retryOperationWithTimeoutNotExceededAndRetriesExhausted() throws Exception {
			counter++;
			throw new IOException(Integer.toString(counter));
		}

		@Retryable(timeout = 5, delay = 10)
		public void retryOperationWithTimeoutExceededAfterInitialFailure() throws Exception {
			counter++;
			Thread.sleep(10);
			throw new IOException(Integer.toString(counter));
		}

		@Retryable(timeout = 5, delay = 10)
		public void retryOperationWithTimeoutExceededAfterFirstDelayButBeforeFirstRetry() throws IOException {
			counter++;
			throw new IOException(Integer.toString(counter));
		}

		@Retryable(timeout = 5, delay = 0)
		public void retryOperationWithTimeoutExceededAfterFirstRetry() throws Exception {
			counter++;
			if (counter == 2) {
				Thread.sleep(10);
			}
			throw new IOException(Integer.toString(counter));
		}

		@Retryable(timeout = 5, delay = 0)
		public void retryOperationWithTimeoutExceededAfterSecondRetry() throws Exception {
			counter++;
			if (counter == 3) {
				Thread.sleep(10);
			}
			throw new IOException(Integer.toString(counter));
		}
	}


	static class AnnotatedMethodBeanWithInterface implements AnnotatedInterface {

		int counter = 0;

		@Retryable(maxRetries = 5, delay = 10)
		@Override
		public void retryOperation() throws IOException {
			counter++;
			throw new IOException(Integer.toString(counter));
		}
	}


	interface AnnotatedInterface {

		@Retryable(maxRetries = 5, delay = 10)
		void retryOperation() throws IOException;
	}


	@Retryable(delay = 10, jitter = 5, multiplier = 2.0, maxDelay = 40,
			includes = IOException.class, excludes = AccessDeniedException.class,
			predicate = RejectMalformedInputException3Predicate.class)
	static class AnnotatedClassBean {

		int counter = 0;

		public void retryOperation() throws IOException {
			counter++;
			if (counter == 3) {
				throw new MalformedInputException(counter);
			}
			throw new IOException(Integer.toString(counter));
		}

		public void retryOperationWithNestedException() {
			counter++;
			throw new RuntimeException(new IOException(Integer.toString(counter)));
		}

		public void otherOperation() throws IOException {
			counter++;
			throw new AccessDeniedException(Integer.toString(counter));
		}

		@Retryable(value = IOException.class, maxRetries = 1, delay = 10)
		public void overrideOperation() throws IOException {
			counter++;
			throw new AccessDeniedException(Integer.toString(counter));
		}
	}


	@Retryable(delayString = "${delay}", jitterString = "${jitter}",
			multiplierString = "${multiplier}", maxDelayString = "${maxDelay}",
			includes = IOException.class, excludes = AccessDeniedException.class,
			predicate = RejectMalformedInputException3Predicate.class)
	static class AnnotatedClassBeanWithStrings {

		int counter = 0;

		public void retryOperation() throws IOException {
			counter++;
			if (counter == 3) {
				throw new MalformedInputException(counter);
			}
			throw new IOException(Integer.toString(counter));
		}

		public void otherOperation() throws IOException {
			counter++;
			throw new AccessDeniedException(Integer.toString(counter));
		}

		@Retryable(value = IOException.class, maxRetriesString = "${limitedRetries}", delayString = "10ms")
		public void overrideOperation() throws IOException {
			counter++;
			throw new AccessDeniedException(Integer.toString(counter));
		}
	}


	static class ConcurrencyLimitAnnotatedBean {

		AtomicInteger current = new AtomicInteger();

		AtomicInteger counter = new AtomicInteger();

		AtomicInteger threadChange = new AtomicInteger();

		volatile String lastThreadName;

		@ConcurrencyLimit(1)
		@Retryable(maxRetries = 2, delay = 10)
		public void retryOperation() throws IOException, InterruptedException {
			if (current.incrementAndGet() > 1) {
				throw new IllegalStateException();
			}
			Thread.sleep(100);
			current.decrementAndGet();
			if (!Thread.currentThread().getName().equals(lastThreadName)) {
				lastThreadName = Thread.currentThread().getName();
				threadChange.incrementAndGet();
			}
			throw new IOException(Integer.toString(counter.incrementAndGet()));
		}
	}


	static class AsyncAnnotatedBean {

		AtomicInteger counter = new AtomicInteger();

		@Async
		@Retryable(maxRetries = 2, delay = 10)
		public CompletableFuture<Void> retryOperation() {
			throw new IllegalStateException(Integer.toString(counter.incrementAndGet()));
		}
	}


	@EnableResilientMethods
	static class EnablingConfig {
	}


	@EnableAsync
	@EnableResilientMethods
	static class EnablingConfigWithAsync {
	}

}
