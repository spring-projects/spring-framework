/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.aop.framework;

import java.io.Serial;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Objects;

import org.aopalliance.intercept.MethodInterceptor;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.lang.Nullable;

import static org.mockito.BDDMockito.doAnswer;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.mock;

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

	static class ObjenesisCglibAopProxyTests extends ProxyExceptionHandlingTests {
		@BeforeEach
		void beforeEach() {
			proxyFactory.setProxyTargetClass(true);
		}

		@Override
		protected void assertProxyType(Object proxy) {
			assertThat(Enhancer.isEnhanced(proxy.getClass())).isTrue();
		}
	}

	static class JdkAopProxyTests extends ProxyExceptionHandlingTests {
		@Override
		protected void assertProxyType(Object proxy) {
			assertThat(Proxy.isProxyClass(proxy.getClass())).isTrue();
		}
	}

	protected void assertProxyType(Object proxy) {
	}

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
				}
				catch (Exception e) {
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
		throwableSeenByCaller = catchThrowable(() -> Objects.requireNonNull(proxy).doSomething());
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
		@Serial
		private static final long serialVersionUID = -4199611059122356651L;
	}

	protected static class DeclaredCheckedException extends Exception {
		@Serial
		private static final long serialVersionUID = 8851375818059230518L;
	}

}
