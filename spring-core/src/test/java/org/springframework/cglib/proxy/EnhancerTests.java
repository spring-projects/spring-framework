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

package org.springframework.cglib.proxy;

import org.junit.jupiter.api.Test;

import org.springframework.cglib.core.SpringNamingPolicy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Enhancer}.
 *
 * @author Pankraz76
 */
class EnhancerTests {

	@Test  // gh-30985
	void createClassWithAttemptLoadSkipsClassWithIncompatibleCallbackTypes() {
		ClassLoader parent = getClass().getClassLoader();
		Class<?> noOpClass = createClass(NoOp.class, parent, false);
		Class<?> dispatcherClass = createClass(Dispatcher.class, parent, false);
		assertThat(noOpClass.getName()).endsWith("$$SpringCGLIB$$0");
		assertThat(dispatcherClass.getName()).endsWith("$$SpringCGLIB$$1");

		// Request the pre-generated classes in reverse order
		ClassLoader classLoader = new ClassLoader(parent) {};
		assertThat(createClass(Dispatcher.class, classLoader, true)).isSameAs(dispatcherClass);
		assertThat(createClass(NoOp.class, classLoader, true)).isSameAs(noOpClass);
	}

	private static Class<?> createClass(Class<? extends Callback> callbackType,
			ClassLoader classLoader, boolean attemptLoad) {

		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(SampleTarget.class);
		enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
		enhancer.setCallbackTypes(new Class<?>[] {MethodInterceptor.class, callbackType});
		enhancer.setCallbackFilter(method -> 0);
		enhancer.setClassLoader(classLoader);
		enhancer.setAttemptLoad(attemptLoad);
		return enhancer.createClass();
	}


	public static class SampleTarget {
	}

}
