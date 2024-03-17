package org.springframework.aop.framework;

import static java.util.Objects.requireNonNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collection;
import java.util.Set;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.lang.Nullable;

abstract class ProxyExceptionHandlingTests implements WithAssertions {

	private static final RuntimeException uncheckedException = new RuntimeException();
	private static final DeclaredCheckedException declaredCheckedException = new DeclaredCheckedException();
	private static final UndeclaredCheckedException undeclaredCheckedException = new UndeclaredCheckedException();

	protected final MyClass target = mock(MyClass.class);
	protected final ProxyFactory proxyFactory = new ProxyFactory(target);

	@Nullable
	protected MyInterface proxy;
	@Nullable
	private Throwable throwableSeenByCaller;

	static class ObjenesisCglibAopProxyTest extends ProxyExceptionHandlingTests {
		@BeforeEach
		void beforeEach() {
			proxyFactory.setProxyTargetClass(true);
		}

		@Override
		protected void assertProxyType(Object proxy) {
			assertThat(Enhancer.isEnhanced(proxy.getClass())).isTrue();
		}
	}

	static class JdkAopProxyTest extends ProxyExceptionHandlingTests {
		@Override
		protected void assertProxyType(Object proxy) {
			assertThat(Proxy.isProxyClass(proxy.getClass())).isTrue();
		}
	}

	protected void assertProxyType(Object proxy) {};

	@BeforeEach
	void beforeEach() {
		Mockito.clearInvocations(target);
	}

	@Nested
	class WhenThereIsOneInterceptor {

		@Nullable
		private Throwable throwableSeenByInterceptor;

		@BeforeEach
		void beforeEach() {
			proxyFactory.addAdvice(captureThrowable());

			proxy = (MyInterface) proxyFactory.getProxy();

			assertProxyType(proxy);
		}

		@Test
		void targetThrowsUndeclaredCheckedException() throws DeclaredCheckedException {
			doAnswer(sneakyThrow(undeclaredCheckedException)).when(target).doSomething();

			invokeProxy();

			assertThat(throwableSeenByInterceptor).isSameAs(undeclaredCheckedException);
			assertThat(throwableSeenByCaller)
					.isInstanceOf(UndeclaredThrowableException.class)
					.hasCauseReference(undeclaredCheckedException);
		}

		@Test
		void targetThrowsDeclaredCheckedException() throws DeclaredCheckedException {
			doThrow(declaredCheckedException).when(target).doSomething();

			invokeProxy();

			assertThat(throwableSeenByInterceptor).isSameAs(declaredCheckedException);
			assertThat(throwableSeenByCaller).isSameAs(declaredCheckedException);
		}

		@Test
		void targetThrowsUncheckedException() throws DeclaredCheckedException {
			doThrow(uncheckedException).when(target).doSomething();

			invokeProxy();

			assertThat(throwableSeenByInterceptor).isSameAs(uncheckedException);
			assertThat(throwableSeenByCaller).isSameAs(uncheckedException);
		}

		private MethodInterceptor captureThrowable() {
			return invocation -> {
				try {
					return invocation.proceed();
				} catch (Exception e) {
					throwableSeenByInterceptor = e;
					throw e;
				}
			};
		}
	}

	@Nested
	class WhenThereAreNoInterceptors {

		@BeforeEach
		void beforeEach() {
			proxy = (MyInterface) proxyFactory.getProxy();

			assertProxyType(proxy);
		}

		@Test
		void targetThrowsUndeclaredCheckedException() throws DeclaredCheckedException {
			doAnswer(sneakyThrow(undeclaredCheckedException)).when(target).doSomething();

			invokeProxy();

			assertThat(throwableSeenByCaller)
					.isInstanceOf(UndeclaredThrowableException.class)
					.hasCauseReference(undeclaredCheckedException);
		}

		@Test
		void targetThrowsDeclaredCheckedException() throws DeclaredCheckedException {
			doThrow(declaredCheckedException).when(target).doSomething();

			invokeProxy();

			assertThat(throwableSeenByCaller).isSameAs(declaredCheckedException);
		}

		@Test
		void targetThrowsUncheckedException() throws DeclaredCheckedException {
			doThrow(uncheckedException).when(target).doSomething();

			invokeProxy();

			assertThat(throwableSeenByCaller).isSameAs(uncheckedException);
		}
	}

	private void invokeProxy() {
		throwableSeenByCaller = catchThrowable(() -> requireNonNull(proxy).doSomething());
	}

	private Answer<?> sneakyThrow(@SuppressWarnings("SameParameterValue") Throwable throwable) {
		return invocation -> {
			throw throwable;
		};
	}

	static class MyClass implements MyInterface {
		@Override
		public void doSomething() throws DeclaredCheckedException {
			throw declaredCheckedException;
		}
	}

	protected interface MyInterface {
		void doSomething() throws DeclaredCheckedException;
	}

	protected static class UndeclaredCheckedException extends Exception {
	}

	protected static class DeclaredCheckedException extends Exception {
	}

}
