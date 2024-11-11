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

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import org.springframework.aop.AopInvocationException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test for SPR-4675. A null value returned from around advice is very hard to debug if
 * the caller expects a primitive.
 *
 * @author Dave Syer
 */
class NullPrimitiveTests {

	interface Foo {
		int getValue();
	}

	@Test
	void testNullPrimitiveWithJdkProxy() {

		class SimpleFoo implements Foo {
			@Override
			public int getValue() {
				return 100;
			}
		}

		SimpleFoo target = new SimpleFoo();
		ProxyFactory factory = new ProxyFactory(target);
		factory.addAdvice((MethodInterceptor) invocation -> null);

		Foo foo = (Foo) factory.getProxy();

		assertThatExceptionOfType(AopInvocationException.class).isThrownBy(foo::getValue)
			.withMessageContaining("Foo.getValue()");
	}

	public static class Bar {
		public int getValue() {
			return 100;
		}
	}

	@Test
	void testNullPrimitiveWithCglibProxy() {

		Bar target = new Bar();
		ProxyFactory factory = new ProxyFactory(target);
		factory.addAdvice((MethodInterceptor) invocation -> null);

		Bar bar = (Bar) factory.getProxy();

		assertThatExceptionOfType(AopInvocationException.class).isThrownBy(bar::getValue)
			.withMessageContaining("Bar.getValue()");
	}

}
