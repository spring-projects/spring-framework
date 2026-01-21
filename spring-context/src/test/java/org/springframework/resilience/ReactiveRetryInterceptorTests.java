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
import java.lang.reflect.Method;
import java.nio.charset.MalformedInputException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.resilience.annotation.RetryAnnotationBeanPostProcessor;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.resilience.retry.MethodRetrySpec;
import org.springframework.resilience.retry.SimpleRetryInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

/**
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 7.0
 */
class ReactiveRetryInterceptorTests {

	@Test
	void withSimpleInterceptor() {
		NonAnnotatedBean target = new NonAnnotatedBean();
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(target);
		pf.addAdvice(new SimpleRetryInterceptor(
				new MethodRetrySpec((m, t) -> true, 5, Duration.ofMillis(10))));
		NonAnnotatedBean proxy = (NonAnnotatedBean) pf.getProxy();

		assertThatIllegalStateException()
				.isThrownBy(() -> proxy.retryOperation().block())
				.satisfies(isRetryExhaustedException())
				.havingCause()
					.isInstanceOf(IOException.class)
					.withMessage("6");
		assertThat(target.counter).hasValue(6);
	}

	@Test
	void withPostProcessorForMethod() {
		AnnotatedMethodBean proxy = getProxiedAnnotatedMethodBean();
		AnnotatedMethodBean target = (AnnotatedMethodBean) AopProxyUtils.getSingletonTarget(proxy);

		assertThatIllegalStateException()
				.isThrownBy(() -> proxy.retryOperation().block())
				.satisfies(isRetryExhaustedException())
				.havingCause()
					.isInstanceOf(IOException.class)
					.withMessage("6");
		assertThat(target.counter).hasValue(6);
	}

	@Test
	void withPostProcessorForClassWithExactIncludesMatch() {
		AnnotatedClassBean proxy = getProxiedAnnotatedClassBean();
		AnnotatedClassBean target = (AnnotatedClassBean) AopProxyUtils.getSingletonTarget(proxy);

		// Exact includes match: IOException
		assertThatRuntimeException()
				.isThrownBy(() -> proxy.ioOperation().block())
				// Does NOT throw a RetryExhaustedException, because RejectMalformedInputException3Predicate
				// rejects a retry if the last exception was a MalformedInputException with message "3".
				.satisfies(isReactiveException())
				.havingCause()
					.isInstanceOf(MalformedInputException.class)
					.withMessageContaining("3");

		// 3 = 1 initial invocation + 2 retry attempts
		// Not 3 retry attempts, because RejectMalformedInputException3Predicate rejects
		// a retry if the last exception was a MalformedInputException with message "3".
		assertThat(target.counter).hasValue(3);
	}

	@Test
	void withPostProcessorForClassWithSubtypeIncludesMatch() {
		AnnotatedClassBean proxy = getProxiedAnnotatedClassBean();
		AnnotatedClassBean target = (AnnotatedClassBean) AopProxyUtils.getSingletonTarget(proxy);

		// Subtype includes match: FileSystemException
		assertThatRuntimeException()
				.isThrownBy(() -> proxy.fileSystemOperation().block())
				.satisfies(isRetryExhaustedException())
				.withCauseInstanceOf(FileSystemException.class);
		// 1 initial attempt + 3 retries
		assertThat(target.counter).hasValue(4);
	}

	@Test  // gh-35583
	void withPostProcessorForClassWithCauseIncludesMatch() {
		AnnotatedClassBean proxy = getProxiedAnnotatedClassBean();
		AnnotatedClassBean target = (AnnotatedClassBean) AopProxyUtils.getSingletonTarget(proxy);

		// Subtype includes match: FileSystemException
		assertThatRuntimeException()
				.isThrownBy(() -> proxy.fileSystemOperationWithNestedException().block())
				.satisfies(isRetryExhaustedException())
				.havingCause()
					.isExactlyInstanceOf(RuntimeException.class)
					.withCauseExactlyInstanceOf(FileSystemException.class);
		// 1 initial attempt + 3 retries
		assertThat(target.counter).hasValue(4);
	}

	@Test
	void withPostProcessorForClassWithExcludesMatch() {
		AnnotatedClassBean proxy = getProxiedAnnotatedClassBean();
		AnnotatedClassBean target = (AnnotatedClassBean) AopProxyUtils.getSingletonTarget(proxy);

		// Exact excludes match: AccessDeniedException
		assertThatRuntimeException()
				.isThrownBy(() -> proxy.accessOperation().block())
				// Does NOT throw a RetryExhaustedException, because no retry is
				// performed for an AccessDeniedException.
				.satisfies(isReactiveException())
				.withCauseInstanceOf(AccessDeniedException.class);
		// 1 initial attempt + 0 retries
		assertThat(target.counter).hasValue(1);
	}

	@Test
	void withPostProcessorForClassWithIncludesMismatch() {
		AnnotatedClassBean proxy = getProxiedAnnotatedClassBean();
		AnnotatedClassBean target = (AnnotatedClassBean) AopProxyUtils.getSingletonTarget(proxy);

		// No match: ArithmeticException
		//
		// Does NOT throw a RetryExhaustedException because no retry is performed
		// for an ArithmeticException, since it is not an IOException.
		// Does NOT throw a ReactiveException because ArithmeticException is a
		// RuntimeException, which reactor.core.Exceptions.propagate(Throwable)
		// does not wrap.
		assertThatExceptionOfType(ArithmeticException.class)
				.isThrownBy(() -> proxy.arithmeticOperation().block())
				.withMessage("1");
		// 1 initial attempt + 0 retries
		assertThat(target.counter).hasValue(1);
	}

	@Test
	void withPostProcessorForClassWithMethodLevelOverride() {
		AnnotatedClassBean proxy = getProxiedAnnotatedClassBean();
		AnnotatedClassBean target = (AnnotatedClassBean) AopProxyUtils.getSingletonTarget(proxy);

		// Overridden, local @Retryable declaration
		assertThatIllegalStateException()
				.isThrownBy(() -> proxy.overrideOperation().blockFirst())
				.satisfies(isRetryExhaustedException())
				.withCauseInstanceOf(IOException.class);
		// 1 initial attempt + 1 retry
		assertThat(target.counter).hasValue(2);
	}

	@Test
	void withMethodRetryEventListener() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.registerBeanDefinition("bean", new RootBeanDefinition(AnnotatedMethodBean.class));
		ctx.registerBeanDefinition("config", new RootBeanDefinition(EnablingConfig.class));
		MethodRetryEventListener listener = new MethodRetryEventListener();
		ctx.addApplicationListener(listener);
		ctx.refresh();
		AnnotatedMethodBean proxy = ctx.getBean(AnnotatedMethodBean.class);
		AnnotatedMethodBean target = (AnnotatedMethodBean) AopProxyUtils.getSingletonTarget(proxy);

		Method method1 = AnnotatedMethodBean.class.getMethod("retryOperation");
		assertThatIllegalStateException()
				.isThrownBy(() -> proxy.retryOperation().block())
				.satisfies(isRetryExhaustedException());
		assertThat(target.counter).hasValue(6);
		assertThat(listener.events).hasSize(7);
		for (int i = 0; i < 6; i++) {
			String msg = Integer.toString(i + 1);
			assertThat(listener.events.get(i))
					.satisfies(event -> assertThat(event.getMethod()).isEqualTo(method1))
					.satisfies(event -> assertThat(event.getFailure()).hasMessage(msg).isInstanceOf(IOException.class))
					.satisfies(event -> assertThat(event.isRetryAborted()).isFalse());
		}
		assertThat(listener.events.get(6))
				.satisfies(event -> assertThat(event.getMethod()).isEqualTo(method1))
				.satisfies(event -> assertThat(event.getFailure()).satisfies(isRetryExhaustedException()))
				.satisfies(event -> assertThat(event.isRetryAborted()).isTrue());

		listener.events.clear();
		target.counter.set(0);
		assertThatNoException().isThrownBy(() -> proxy.retryOperationWithInitialSuccess().block());
		assertThat(target.counter).hasValue(1);
		assertThat(listener.events).isEmpty();

		target.counter.set(0);
		Method method2 = AnnotatedMethodBean.class.getMethod("retryOperationWithSuccessAfterInitialFailure");
		assertThatNoException().isThrownBy(() -> proxy.retryOperationWithSuccessAfterInitialFailure().block());
		assertThat(target.counter).hasValue(2);
		assertThat(listener.events).hasSize(1);
		assertThat(listener.events.get(0))
				.satisfies(event -> assertThat(event.getMethod()).isEqualTo(method2))
				.satisfies(event -> assertThat(event.getFailure()).hasMessage("1").isInstanceOf(IOException.class))
				.satisfies(event -> assertThat(event.isRetryAborted()).isFalse());
	}

	@Test
	void adaptReactiveResultWithMinimalRetrySpec() {
		// Test minimal retry configuration: maxRetries=1, delay=0, jitter=0, multiplier=1.0, maxDelay=0
		MinimalRetryBean target = new MinimalRetryBean();
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(target);
		pf.addAdvice(new SimpleRetryInterceptor(
				new MethodRetrySpec((m, t) -> true, 1, Duration.ZERO, Duration.ZERO, 1.0, Duration.ZERO)));
		MinimalRetryBean proxy = (MinimalRetryBean) pf.getProxy();

		// Should execute only 2 times, because maxRetries=1 means 1 call + 1 retry
		assertThatIllegalStateException()
				.isThrownBy(() -> proxy.retryOperation().block())
				.satisfies(isRetryExhaustedException())
				.havingCause()
					.isInstanceOf(IOException.class)
					.withMessage("2");
		assertThat(target.counter).hasValue(2);
	}

	@Test
	void adaptReactiveResultWithZeroAttempts() {
		// Test minimal retry configuration: maxRetries=1, delay=0, jitter=0, multiplier=1.0, maxDelay=0
		MinimalRetryBean target = new MinimalRetryBean();
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(target);
		pf.addAdvice(new SimpleRetryInterceptor(
				new MethodRetrySpec((m, t) -> true, 0, Duration.ZERO, Duration.ZERO, 1.0, Duration.ZERO)));
		MinimalRetryBean proxy = (MinimalRetryBean) pf.getProxy();

		// Should execute only 1 time, because maxRetries=0 means initial call only
		assertThatIllegalStateException()
				.isThrownBy(() -> proxy.retryOperation().block())
				.satisfies(isRetryExhaustedException())
				.havingCause()
				.isInstanceOf(IOException.class)
				.withMessage("1");
		assertThat(target.counter).hasValue(1);
	}

	@Test
	void adaptReactiveResultWithZeroDelayAndJitter() {
		// Test case where delay=0 and jitter>0
		ZeroDelayJitterBean target = new ZeroDelayJitterBean();
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(target);
		pf.addAdvice(new SimpleRetryInterceptor(
				new MethodRetrySpec((m, t) -> true, 3, Duration.ZERO, Duration.ofMillis(10), 2.0, Duration.ofMillis(100))));
		ZeroDelayJitterBean proxy = (ZeroDelayJitterBean) pf.getProxy();

		assertThatIllegalStateException()
				.isThrownBy(() -> proxy.retryOperation().block())
				.satisfies(isRetryExhaustedException())
				.havingCause()
					.isInstanceOf(IOException.class)
					.withMessage("4");
		assertThat(target.counter).hasValue(4);
	}

	@Test
	void adaptReactiveResultWithJitterGreaterThanDelay() {
		// Test case where jitter > delay
		JitterGreaterThanDelayBean target = new JitterGreaterThanDelayBean();
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(target);
		pf.addAdvice(new SimpleRetryInterceptor(
				new MethodRetrySpec((m, t) -> true, 3, Duration.ofMillis(5), Duration.ofMillis(20), 1.5, Duration.ofMillis(50))));
		JitterGreaterThanDelayBean proxy = (JitterGreaterThanDelayBean) pf.getProxy();

		assertThatIllegalStateException()
				.isThrownBy(() -> proxy.retryOperation().block())
				.satisfies(isRetryExhaustedException())
				.havingCause()
					.isInstanceOf(IOException.class)
					.withMessage("4");
		assertThat(target.counter).hasValue(4);
	}

	@Test
	void adaptReactiveResultWithFluxMultiValue() {
		// Test Flux multi-value stream case
		FluxMultiValueBean target = new FluxMultiValueBean();
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(target);
		pf.addAdvice(new SimpleRetryInterceptor(
				new MethodRetrySpec((m, t) -> true, 3, Duration.ofMillis(10), Duration.ofMillis(5), 2.0, Duration.ofMillis(100))));
		FluxMultiValueBean proxy = (FluxMultiValueBean) pf.getProxy();

		assertThatIllegalStateException()
				.isThrownBy(() -> proxy.retryOperation().blockFirst())
				.satisfies(isRetryExhaustedException())
				.havingCause()
					.isInstanceOf(IOException.class)
					.withMessage("4");
		assertThat(target.counter).hasValue(4);
	}

	@Test
	void adaptReactiveResultWithSuccessfulOperation() {
		// Test successful return case, ensuring retry mechanism doesn't activate
		SuccessfulOperationBean target = new SuccessfulOperationBean();
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(target);
		pf.addAdvice(new SimpleRetryInterceptor(
				new MethodRetrySpec((m, t) -> true, 5, Duration.ofMillis(10), Duration.ofMillis(5), 2.0, Duration.ofMillis(100))));
		SuccessfulOperationBean proxy = (SuccessfulOperationBean) pf.getProxy();

		String result = proxy.retryOperation().block();
		assertThat(result).isEqualTo("success");
		// Should execute only once because of successful return
		assertThat(target.counter).hasValue(1);
	}

	@Test
	void adaptReactiveResultWithAlwaysFailingOperation() {
		// Test "always fails" case, ensuring retry mechanism stops after maxRetries (3)
		AlwaysFailsBean target = new AlwaysFailsBean();
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(target);
		pf.addAdvice(new SimpleRetryInterceptor(
				new MethodRetrySpec((m, t) -> true, 3, Duration.ofMillis(10), Duration.ofMillis(5), 1.5, Duration.ofMillis(50))));
		AlwaysFailsBean proxy = (AlwaysFailsBean) pf.getProxy();

		assertThatIllegalStateException()
				.isThrownBy(() -> proxy.retryOperation().block())
				.satisfies(isRetryExhaustedException())
				.havingCause()
					.isInstanceOf(NumberFormatException.class)
					.withMessage("always fails");
		// 1 initial attempt + 3 retries
		assertThat(target.counter).hasValue(4);
	}


	@Nested
	class TimeoutTests {

		private final AnnotatedMethodBean proxy = getProxiedAnnotatedMethodBean();
		private final AnnotatedMethodBean target = (AnnotatedMethodBean) AopProxyUtils.getSingletonTarget(proxy);

		@Test
		void timeoutNotExceededAfterInitialSuccess() {
			String result = proxy.retryOperationWithTimeoutNotExceededAfterInitialSuccess().block();
			assertThat(result).isEqualTo("success");
			// 1 initial attempt + 0 retries
			assertThat(target.counter).hasValue(1);
		}

		@Test
		void timeoutNotExceededAndRetriesExhausted() {
			assertThatIllegalStateException()
					.isThrownBy(() -> proxy.retryOperationWithTimeoutNotExceededAndRetriesExhausted().block())
					.satisfies(isRetryExhaustedException())
					.havingCause()
						.isInstanceOf(IOException.class)
						.withMessage("4");
			// 1 initial attempt + 3 retries
			assertThat(target.counter).hasValue(4);
		}

		@Test
		void timeoutExceededAfterInitialFailure() {
			assertThatRuntimeException()
					.isThrownBy(() -> proxy.retryOperationWithTimeoutExceededAfterInitialFailure().block())
					.satisfies(isReactiveException())
					.havingCause()
						.isInstanceOf(TimeoutException.class)
						.withMessageContaining("within 20ms");
			// 1 initial attempt + 0 retries
			assertThat(target.counter).hasValue(1);
		}

		@Test
		void timeoutExceededAfterFirstDelayButBeforeFirstRetry() {
			assertThatRuntimeException()
					.isThrownBy(() -> proxy.retryOperationWithTimeoutExceededAfterFirstDelayButBeforeFirstRetry().block())
					.satisfies(isReactiveException())
					.havingCause()
						.isInstanceOf(TimeoutException.class)
						.withMessageContaining("within 20ms");
			// 1 initial attempt + 0 retries
			assertThat(target.counter).hasValue(1);
		}

		@Test
		void timeoutExceededAfterFirstRetry() {
			assertThatRuntimeException()
					.isThrownBy(() -> proxy.retryOperationWithTimeoutExceededAfterFirstRetry().block())
					.satisfies(isReactiveException())
					.havingCause()
						.isInstanceOf(TimeoutException.class)
						.withMessageContaining("within 20ms");
			// 1 initial attempt + 1 retry
			assertThat(target.counter).hasValue(2);
		}

		@Test
		void timeoutExceededAfterSecondRetry() {
			assertThatRuntimeException()
					.isThrownBy(() -> proxy.retryOperationWithTimeoutExceededAfterSecondRetry().block())
					.satisfies(isReactiveException())
					.havingCause()
						.isInstanceOf(TimeoutException.class)
						.withMessageContaining("within 20ms");
			// 1 initial attempt + 2 retries
			assertThat(target.counter).hasValue(3);
		}
	}


	private static ThrowingConsumer<? super Throwable> isReactiveException() {
		return ex -> assertThat(ex.getClass().getName()).isEqualTo("reactor.core.Exceptions$ReactiveException");
	}

	private static ThrowingConsumer<? super Throwable> isRetryExhaustedException() {
		return ex -> assertThat(ex).matches(Exceptions::isRetryExhausted, "is RetryExhaustedException");
	}

	private static AnnotatedMethodBean getProxiedAnnotatedMethodBean() {
		BeanFactory bf = createBeanFactoryFor(AnnotatedMethodBean.class);
		return bf.getBean(AnnotatedMethodBean.class);
	}

	private static AnnotatedClassBean getProxiedAnnotatedClassBean() {
		BeanFactory bf = createBeanFactoryFor(AnnotatedClassBean.class);
		return bf.getBean(AnnotatedClassBean.class);
	}

	private static BeanFactory createBeanFactoryFor(Class<?> beanClass) {
		/*
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("bean", new RootBeanDefinition(beanClass));
		RetryAnnotationBeanPostProcessor bpp = new RetryAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		*/
		GenericApplicationContext bf = new GenericApplicationContext();
		bf.registerBeanDefinition("bean", new RootBeanDefinition(beanClass));
		bf.registerBeanDefinition("processor", new RootBeanDefinition(RetryAnnotationBeanPostProcessor.class));
		bf.registerBeanDefinition("listener", new RootBeanDefinition(MethodRetryEventListener.class));
		bf.refresh();

		return bf;
	}


	static class NonAnnotatedBean {

		AtomicInteger counter = new AtomicInteger();

		public Mono<Object> retryOperation() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				throw new IOException(counter.toString());
			});
		}
	}


	static class AnnotatedMethodBean {

		AtomicInteger counter = new AtomicInteger();

		@Retryable(maxRetries = 5, delay = 10)
		public Mono<Object> retryOperation() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				throw new IOException(counter.toString());
			});
		}

		@Retryable(maxRetries = 5, delay = 10)
		public Mono<String> retryOperationWithInitialSuccess() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				return "success";
			});
		}

		@Retryable(maxRetries = 5, delay = 10)
		public Mono<String> retryOperationWithSuccessAfterInitialFailure() {
			return Mono.fromCallable(() -> {
				if (counter.incrementAndGet() == 1) {
					throw new IOException(counter.toString());
				}
				return "success";
			});
		}

		@Retryable(timeout = 555, delay = 10)
		public Mono<String> retryOperationWithTimeoutNotExceededAfterInitialSuccess() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				return "success";
			});
		}

		@Retryable(timeout = 555, delay = 10)
		public Mono<Object> retryOperationWithTimeoutNotExceededAndRetriesExhausted() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				throw new IOException(counter.toString());
			});
		}

		@Retryable(timeout = 20, delay = 0)
		public Mono<Object> retryOperationWithTimeoutExceededAfterInitialFailure() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				Thread.sleep(100);
				throw new IOException(counter.toString());
			});
		}

		@Retryable(timeout = 20, delay = 100) // Delay > Timeout
		public Mono<Object> retryOperationWithTimeoutExceededAfterFirstDelayButBeforeFirstRetry() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				throw new IOException(counter.toString());
			});
		}

		@Retryable(timeout = 20, delay = 0)
		public Mono<Object> retryOperationWithTimeoutExceededAfterFirstRetry() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				if (counter.get() == 2) {
					Thread.sleep(100);
				}
				throw new IOException(counter.toString());
			});
		}

		@Retryable(timeout = 20, delay = 0)
		public Mono<Object> retryOperationWithTimeoutExceededAfterSecondRetry() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				if (counter.get() == 3) {
					Thread.sleep(100);
				}
				throw new IOException(counter.toString());
			});
		}
	}


	@Retryable(delay = 10, jitter = 5, multiplier = 2.0, maxDelay = 40,
			includes = IOException.class, excludes = AccessDeniedException.class,
			predicate = RejectMalformedInputException3Predicate.class)
	static class AnnotatedClassBean {

		AtomicInteger counter = new AtomicInteger();

		public Mono<Object> ioOperation() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				if (counter.get() == 3) {
					throw new MalformedInputException(counter.get());
				}
				throw new IOException(counter.toString());
			});
		}

		public Mono<Object> fileSystemOperation() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				throw new FileSystemException(counter.toString());
			});
		}

		public Mono<Object> fileSystemOperationWithNestedException() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				throw new RuntimeException(new FileSystemException(counter.toString()));
			});
		}

		public Mono<Object> accessOperation() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				throw new AccessDeniedException(counter.toString());
			});
		}

		public Mono<Object> arithmeticOperation() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				throw new ArithmeticException(counter.toString());
			});
		}

		@Retryable(includes = IOException.class, maxRetries = 1, delay = 10)
		public Flux<Object> overrideOperation() {
			return Flux.from(Mono.fromCallable(() -> {
				counter.incrementAndGet();
				throw new AccessDeniedException(counter.toString());
			}));
		}
	}


	@EnableResilientMethods
	static class EnablingConfig {
	}


	// Bean classes for boundary testing

	static class MinimalRetryBean {

		AtomicInteger counter = new AtomicInteger();

		public Mono<Object> retryOperation() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				throw new IOException(counter.toString());
			});
		}
	}


	static class ZeroDelayJitterBean {

		AtomicInteger counter = new AtomicInteger();

		public Mono<Object> retryOperation() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				throw new IOException(counter.toString());
			});
		}
	}


	static class JitterGreaterThanDelayBean {

		AtomicInteger counter = new AtomicInteger();

		public Mono<Object> retryOperation() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				throw new IOException(counter.toString());
			});
		}
	}


	static class FluxMultiValueBean {

		AtomicInteger counter = new AtomicInteger();

		public Flux<Object> retryOperation() {
			return Flux.from(Mono.fromCallable(() -> {
				counter.incrementAndGet();
				throw new IOException(counter.toString());
			}));
		}
	}


	static class SuccessfulOperationBean {

		AtomicInteger counter = new AtomicInteger();

		public Mono<String> retryOperation() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				return "success";
			});
		}
	}


	static class AlwaysFailsBean {

		AtomicInteger counter = new AtomicInteger();

		public Mono<Object> retryOperation() {
			return Mono.fromCallable(() -> {
				counter.incrementAndGet();
				throw new NumberFormatException("always fails");
			});
		}
	}

}
