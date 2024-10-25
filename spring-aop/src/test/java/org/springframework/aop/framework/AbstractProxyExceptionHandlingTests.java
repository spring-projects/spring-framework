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

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Objects;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

/**
 * @author Mikaël Francoeur
 * @author Sam Brannen
 * @since 6.2
 * @see JdkProxyExceptionHandlingTests
 * @see CglibProxyExceptionHandlingTests
 */
abstract class AbstractProxyExceptionHandlingTests {

	private static final RuntimeException uncheckedException = new RuntimeException();

	private static final DeclaredCheckedException declaredCheckedException = new DeclaredCheckedException();

	private static final UndeclaredCheckedException undeclaredCheckedException = new UndeclaredCheckedException();

	protected final MyClass target = mock();

	protected final ProxyFactory proxyFactory = new ProxyFactory(target);

	protected MyInterface proxy;

	private Throwable throwableSeenByCaller;


	@BeforeEach
	void clear() {
		Mockito.clearInvocations(target);
	}


	protected abstract void assertProxyType(Object proxy);


	private void invokeProxy() {
		throwableSeenByCaller = catchThrowable(() -> Objects.requireNonNull(proxy).doSomething());
	}

	@SuppressWarnings("SameParameterValue")
	private static Answer<?> sneakyThrow(Throwable throwable) {
		return invocation -> {
			throw throwable;
		};
	}


	@Nested
	class WhenThereIsOneInterceptorTests {

		@Nullable
		private Throwable throwableSeenByInterceptor;

		@BeforeEach
		void beforeEach() {
			proxyFactory.addAdvice(captureThrowable());
			proxy = (MyInterface) proxyFactory.getProxy(getClass().getClassLoader());
			assertProxyType(proxy);
		}

		@Test
		void targetThrowsUndeclaredCheckedException() throws DeclaredCheckedException {
			willAnswer(sneakyThrow(undeclaredCheckedException)).given(target).doSomething();
			invokeProxy();
			assertThat(throwableSeenByInterceptor).isSameAs(undeclaredCheckedException);
			assertThat(throwableSeenByCaller)
					.isInstanceOf(UndeclaredThrowableException.class)
					.hasCauseReference(undeclaredCheckedException);
		}

		@Test
		void targetThrowsDeclaredCheckedException() throws DeclaredCheckedException {
			willThrow(declaredCheckedException).given(target).doSomething();
			invokeProxy();
			assertThat(throwableSeenByInterceptor).isSameAs(declaredCheckedException);
			assertThat(throwableSeenByCaller).isSameAs(declaredCheckedException);
		}

		@Test
		void targetThrowsUncheckedException() throws DeclaredCheckedException {
			willThrow(uncheckedException).given(target).doSomething();
			invokeProxy();
			assertThat(throwableSeenByInterceptor).isSameAs(uncheckedException);
			assertThat(throwableSeenByCaller).isSameAs(uncheckedException);
		}

		private MethodInterceptor captureThrowable() {
			return invocation -> {
				try {
					return invocation.proceed();
				}
				catch (Exception ex) {
					throwableSeenByInterceptor = ex;
					throw ex;
				}
			};
		}
	}


	@Nested
	class WhenThereAreNoInterceptorsTests {

		@BeforeEach
		void beforeEach() {
			proxy = (MyInterface) proxyFactory.getProxy(getClass().getClassLoader());
			assertProxyType(proxy);
		}

		@Test
		void targetThrowsUndeclaredCheckedException() throws DeclaredCheckedException {
			willAnswer(sneakyThrow(undeclaredCheckedException)).given(target).doSomething();
			invokeProxy();
			assertThat(throwableSeenByCaller)
					.isInstanceOf(UndeclaredThrowableException.class)
					.hasCauseReference(undeclaredCheckedException);
		}

		@Test
		void targetThrowsDeclaredCheckedException() throws DeclaredCheckedException {
			willThrow(declaredCheckedException).given(target).doSomething();
			invokeProxy();
			assertThat(throwableSeenByCaller).isSameAs(declaredCheckedException);
		}

		@Test
		void targetThrowsUncheckedException() throws DeclaredCheckedException {
			willThrow(uncheckedException).given(target).doSomething();
			invokeProxy();
			assertThat(throwableSeenByCaller).isSameAs(uncheckedException);
		}
	}


	interface MyInterface {

		void doSomething() throws DeclaredCheckedException;
	}

	static class MyClass implements MyInterface {

		@Override
		public void doSomething() throws DeclaredCheckedException {
			throw declaredCheckedException;
		}
	}

	@SuppressWarnings("serial")
	private static class UndeclaredCheckedException extends Exception {
	}

	@SuppressWarnings("serial")
	private static class DeclaredCheckedException extends Exception {
	}

}
