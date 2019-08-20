/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.tests.sample.objects.TestObject;

import static org.assertj.core.api.Assertions.assertThat;

class PrioritizedParameterNameDiscovererTests {

	private static final String[] FOO_BAR = new String[] { "foo", "bar" };

	private static final String[] SOMETHING_ELSE = new String[] { "something", "else" };

	private final ParameterNameDiscoverer returnsFooBar = new ParameterNameDiscoverer() {
		@Override
		public String[] getParameterNames(Method m) {
			return FOO_BAR;
		}
		@Override
		public String[] getParameterNames(Constructor<?> ctor) {
			return FOO_BAR;
		}
	};

	private final ParameterNameDiscoverer returnsSomethingElse = new ParameterNameDiscoverer() {
		@Override
		public String[] getParameterNames(Method m) {
			return SOMETHING_ELSE;
		}
		@Override
		public String[] getParameterNames(Constructor<?> ctor) {
			return SOMETHING_ELSE;
		}
	};

	private final Method anyMethod;

	public PrioritizedParameterNameDiscovererTests() throws SecurityException, NoSuchMethodException {
		anyMethod = TestObject.class.getMethod("getAge");
	}

	@Test
	void noParametersDiscoverers() {
		ParameterNameDiscoverer pnd = new PrioritizedParameterNameDiscoverer();
		assertThat(pnd.getParameterNames(anyMethod)).isNull();
		assertThat(pnd.getParameterNames((Constructor<?>) null)).isNull();
	}

	@Test
	void orderedParameterDiscoverers1() {
		PrioritizedParameterNameDiscoverer pnd = new PrioritizedParameterNameDiscoverer();
		pnd.addDiscoverer(returnsFooBar);
		assertThat(Arrays.equals(FOO_BAR, pnd.getParameterNames(anyMethod))).isTrue();
		assertThat(Arrays.equals(FOO_BAR, pnd.getParameterNames((Constructor<?>) null))).isTrue();
		pnd.addDiscoverer(returnsSomethingElse);
		assertThat(Arrays.equals(FOO_BAR, pnd.getParameterNames(anyMethod))).isTrue();
		assertThat(Arrays.equals(FOO_BAR, pnd.getParameterNames((Constructor<?>) null))).isTrue();
	}

	@Test
	void orderedParameterDiscoverers2() {
		PrioritizedParameterNameDiscoverer pnd = new PrioritizedParameterNameDiscoverer();
		pnd.addDiscoverer(returnsSomethingElse);
		assertThat(Arrays.equals(SOMETHING_ELSE, pnd.getParameterNames(anyMethod))).isTrue();
		assertThat(Arrays.equals(SOMETHING_ELSE, pnd.getParameterNames((Constructor<?>) null))).isTrue();
		pnd.addDiscoverer(returnsFooBar);
		assertThat(Arrays.equals(SOMETHING_ELSE, pnd.getParameterNames(anyMethod))).isTrue();
		assertThat(Arrays.equals(SOMETHING_ELSE, pnd.getParameterNames((Constructor<?>) null))).isTrue();
	}

}
