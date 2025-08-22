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
import java.lang.reflect.Method;
import java.nio.file.AccessDeniedException;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.framework.ProxyConfig;
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
import org.springframework.resilience.retry.MethodRetryPredicate;
import org.springframework.resilience.retry.MethodRetrySpec;
import org.springframework.resilience.retry.SimpleRetryInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * @author Juergen Hoeller
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
	void withPostProcessorForMethodWithInterface() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("bean", new RootBeanDefinition(AnnotatedMethodBeanWithInterface.class));
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
	void withPostProcessorForMethodWithInterfaceAndDefaultTargetClass() {
		ProxyConfig defaultProxyConfig = new ProxyConfig();
		defaultProxyConfig.setProxyTargetClass(true);

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton(AutoProxyUtils.DEFAULT_PROXY_CONFIG_BEAN_NAME, defaultProxyConfig);
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
		ProxyConfig defaultProxyConfig = new ProxyConfig();
		defaultProxyConfig.setProxyTargetClass(true);

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton(AutoProxyUtils.DEFAULT_PROXY_CONFIG_BEAN_NAME, defaultProxyConfig);
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

	@Test
	void withPostProcessorForClassWithStrings() {
		Properties props = new Properties();
		props.setProperty("delay", "10");
		props.setProperty("jitter", "5");
		props.setProperty("multiplier", "2.0");
		props.setProperty("maxDelay", "40");
		props.setProperty("limitedAttempts", "1");

		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("props", props));
		ctx.registerBeanDefinition("bean", new RootBeanDefinition(AnnotatedClassBeanWithStrings.class));
		ctx.registerBeanDefinition("bpp", new RootBeanDefinition(RetryAnnotationBeanPostProcessor.class));
		ctx.refresh();
		AnnotatedClassBeanWithStrings proxy = ctx.getBean(AnnotatedClassBeanWithStrings.class);
		AnnotatedClassBeanWithStrings target = (AnnotatedClassBeanWithStrings) AopProxyUtils.getSingletonTarget(proxy);

		assertThatIOException().isThrownBy(proxy::retryOperation).withMessage("3");
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
		props.setProperty("limitedAttempts", "0");

		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("props", props));
		ctx.registerBeanDefinition("bean", new RootBeanDefinition(AnnotatedClassBeanWithStrings.class));
		ctx.registerBeanDefinition("bpp", new RootBeanDefinition(RetryAnnotationBeanPostProcessor.class));
		ctx.refresh();
		AnnotatedClassBeanWithStrings proxy = ctx.getBean(AnnotatedClassBeanWithStrings.class);
		AnnotatedClassBeanWithStrings target = (AnnotatedClassBeanWithStrings) AopProxyUtils.getSingletonTarget(proxy);

		assertThatIOException().isThrownBy(proxy::retryOperation).withMessage("3");
		assertThat(target.counter).isEqualTo(3);
		assertThatIOException().isThrownBy(proxy::otherOperation);
		assertThat(target.counter).isEqualTo(4);
		assertThatIOException().isThrownBy(proxy::overrideOperation);
		assertThat(target.counter).isEqualTo(5);
	}

	@Test
	void withEnableAnnotation() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.registerBeanDefinition("bean", new RootBeanDefinition(DoubleAnnotatedBean.class));
		ctx.registerBeanDefinition("config", new RootBeanDefinition(EnablingConfig.class));
		ctx.refresh();
		DoubleAnnotatedBean proxy = ctx.getBean(DoubleAnnotatedBean.class);
		DoubleAnnotatedBean target = (DoubleAnnotatedBean) AopProxyUtils.getSingletonTarget(proxy);

		Thread thread = new Thread(() -> assertThatIOException().isThrownBy(proxy::retryOperation));
		thread.start();
		assertThatIOException().isThrownBy(proxy::retryOperation);
		thread.join();
		assertThat(target.counter).hasValue(6);
		assertThat(target.threadChange).hasValue(2);
	}


	static class NonAnnotatedBean implements PlainInterface {

		int counter = 0;

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

		@Retryable(maxAttempts = 5, delay = 10)
		public void retryOperation() throws IOException {
			counter++;
			throw new IOException(Integer.toString(counter));
		}
	}


	static class AnnotatedMethodBeanWithInterface implements AnnotatedInterface {

		int counter = 0;

		@Retryable(maxAttempts = 5, delay = 10)
		@Override
		public void retryOperation() throws IOException {
			counter++;
			throw new IOException(Integer.toString(counter));
		}
	}


	interface AnnotatedInterface {

		@Retryable(maxAttempts = 5, delay = 10)
		void retryOperation() throws IOException;
	}


	@Retryable(delay = 10, jitter = 5, multiplier = 2.0, maxDelay = 40,
			includes = IOException.class, excludes = AccessDeniedException.class,
			predicate = CustomPredicate.class)
	static class AnnotatedClassBean {

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


	@Retryable(delayString = "${delay}", jitterString = "${jitter}",
			multiplierString = "${multiplier}", maxDelayString = "${maxDelay}",
			includes = IOException.class, excludes = AccessDeniedException.class,
			predicate = CustomPredicate.class)
	static class AnnotatedClassBeanWithStrings {

		int counter = 0;

		public void retryOperation() throws IOException {
			counter++;
			throw new IOException(Integer.toString(counter));
		}

		public void otherOperation() throws IOException {
			counter++;
			throw new AccessDeniedException(Integer.toString(counter));
		}

		@Retryable(value = IOException.class, maxAttemptsString = "${limitedAttempts}", delayString = "10ms")
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


	static class DoubleAnnotatedBean {

		AtomicInteger current = new AtomicInteger();

		AtomicInteger counter = new AtomicInteger();

		AtomicInteger threadChange = new AtomicInteger();

		volatile String lastThreadName;

		@ConcurrencyLimit(1)
		@Retryable(maxAttempts = 2, delay = 10)
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


	@EnableResilientMethods
	static class EnablingConfig {
	}

}
