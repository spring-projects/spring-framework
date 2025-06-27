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

package org.springframework.aop.retry;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.AccessDeniedException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.retry.annotation.RetryAnnotationBeanPostProcessor;
import org.springframework.aop.retry.annotation.RetryAnnotationInterceptor;
import org.springframework.aop.retry.annotation.Retryable;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

/**
 * @author Juergen Hoeller
 * @since 7.0
 */
public class ReactiveRetryInterceptorTests {

	@Test
	void withSimpleInterceptor() {
		NonAnnotatedBean target = new NonAnnotatedBean();
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(target);
		pf.addAdvice(new SimpleRetryInterceptor(new MethodRetrySpec((m, t) -> true, 5, 10)));
		NonAnnotatedBean proxy = (NonAnnotatedBean) pf.getProxy();

		assertThatIllegalStateException().isThrownBy(() -> proxy.retryOperation().block())
				.withCauseInstanceOf(IOException.class).havingCause().withMessage("6");
		assertThat(target.counter.get()).isEqualTo(6);
	}

	@Test
	void withAnnotationInterceptorForMethod() {
		AnnotatedMethodBean target = new AnnotatedMethodBean();
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(target);
		pf.addAdvice(new RetryAnnotationInterceptor());
		AnnotatedMethodBean proxy = (AnnotatedMethodBean) pf.getProxy();

		assertThatIllegalStateException().isThrownBy(() -> proxy.retryOperation().block())
				.withCauseInstanceOf(IOException.class).havingCause().withMessage("6");
		assertThat(target.counter.get()).isEqualTo(6);
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

		assertThatIllegalStateException().isThrownBy(() -> proxy.retryOperation().block())
				.withCauseInstanceOf(IOException.class).havingCause().withMessage("6");
		assertThat(target.counter.get()).isEqualTo(6);
	}

	@Test
	void withAnnotationInterceptorForClass() {
		AnnotatedClassBean target = new AnnotatedClassBean();
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(target);
		pf.addAdvice(new RetryAnnotationInterceptor());
		AnnotatedClassBean proxy = (AnnotatedClassBean) pf.getProxy();

		assertThatRuntimeException().isThrownBy(() -> proxy.retryOperation().block())
				.withCauseInstanceOf(IOException.class).havingCause().withMessage("3");
		assertThat(target.counter.get()).isEqualTo(3);
		assertThatRuntimeException().isThrownBy(() -> proxy.otherOperation().block())
				.withCauseInstanceOf(IOException.class);
		assertThat(target.counter.get()).isEqualTo(4);
		assertThatIllegalStateException().isThrownBy(() -> proxy.overrideOperation().blockFirst())
				.withCauseInstanceOf(IOException.class);
		assertThat(target.counter.get()).isEqualTo(6);
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

		assertThatRuntimeException().isThrownBy(() -> proxy.retryOperation().block())
				.withCauseInstanceOf(IOException.class).havingCause().withMessage("3");
		assertThat(target.counter.get()).isEqualTo(3);
		assertThatRuntimeException().isThrownBy(() -> proxy.otherOperation().block())
				.withCauseInstanceOf(IOException.class);
		assertThat(target.counter.get()).isEqualTo(4);
		assertThatIllegalStateException().isThrownBy(() -> proxy.overrideOperation().blockFirst())
				.withCauseInstanceOf(IOException.class);
		assertThat(target.counter.get()).isEqualTo(6);
	}


	public static class NonAnnotatedBean {

		AtomicInteger counter = new AtomicInteger();

		public Mono<Object> retryOperation() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				throw new IOException(counter.toString());
			});
		}
	}


	public static class AnnotatedMethodBean {

		AtomicInteger counter = new AtomicInteger();

		@Retryable(maxAttempts = 5, delay = 10)
		public Mono<Object> retryOperation() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				throw new IOException(counter.toString());
			});
		}
	}


	@Retryable(delay = 10, jitter = 5, multiplier = 2.0, maxDelay = 40,
			includes = IOException.class, excludes = AccessDeniedException.class,
			predicate = CustomPredicate.class)
	public static class AnnotatedClassBean {

		AtomicInteger counter = new AtomicInteger();

		public Mono<Object> retryOperation() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				throw new IOException(counter.toString());
			});
		}

		public Mono<Object> otherOperation() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				throw new AccessDeniedException(counter.toString());
			});
		}

		@Retryable(value = IOException.class, maxAttempts = 1, delay = 10)
		public Flux<Object> overrideOperation() {
			return Flux.from(Mono.fromCallable(() -> {
				counter.incrementAndGet();
				throw new AccessDeniedException(counter.toString());
			}));
		}
	}


	private static class CustomPredicate implements MethodRetryPredicate {

		@Override
		public boolean shouldRetry(Method method, Throwable throwable) {
			return !"3".equals(throwable.getMessage());
		}
	}

}
