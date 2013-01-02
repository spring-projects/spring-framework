/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import junit.framework.TestCase;

import org.springframework.tests.sample.objects.TestObject;

public class PrioritizedParameterNameDiscovererTests extends TestCase {

	private static final String[] FOO_BAR = new String[] { "foo", "bar" };

	private static final String[] SOMETHING_ELSE = new String[] { "something", "else" };

	ParameterNameDiscoverer returnsFooBar = new ParameterNameDiscoverer() {
		@Override
		public String[] getParameterNames(Method m) {
			return FOO_BAR;
		}
		@Override
		public String[] getParameterNames(Constructor ctor) {
			return FOO_BAR;
		}
	};

	ParameterNameDiscoverer returnsSomethingElse = new ParameterNameDiscoverer() {
		@Override
		public String[] getParameterNames(Method m) {
			return SOMETHING_ELSE;
		}
		@Override
		public String[] getParameterNames(Constructor ctor) {
			return SOMETHING_ELSE;
		}
	};

	private final Method anyMethod;
	private final Class anyClass = Object.class;

	public PrioritizedParameterNameDiscovererTests() throws SecurityException, NoSuchMethodException {
		anyMethod = TestObject.class.getMethod("getAge", (Class[]) null);
	}

	public void testNoParametersDiscoverers() {
		ParameterNameDiscoverer pnd = new PrioritizedParameterNameDiscoverer();
		assertNull(pnd.getParameterNames(anyMethod));
		assertNull(pnd.getParameterNames((Constructor) null));
	}

	public void testOrderedParameterDiscoverers1() {
		PrioritizedParameterNameDiscoverer pnd = new PrioritizedParameterNameDiscoverer();
		pnd.addDiscoverer(returnsFooBar);
		assertTrue(Arrays.equals(FOO_BAR, pnd.getParameterNames(anyMethod)));
		assertTrue(Arrays.equals(FOO_BAR, pnd.getParameterNames((Constructor) null)));
		pnd.addDiscoverer(returnsSomethingElse);
		assertTrue(Arrays.equals(FOO_BAR, pnd.getParameterNames(anyMethod)));
		assertTrue(Arrays.equals(FOO_BAR, pnd.getParameterNames((Constructor) null)));
	}

	public void testOrderedParameterDiscoverers2() {
		PrioritizedParameterNameDiscoverer pnd = new PrioritizedParameterNameDiscoverer();
		pnd.addDiscoverer(returnsSomethingElse);
		assertTrue(Arrays.equals(SOMETHING_ELSE, pnd.getParameterNames(anyMethod)));
		assertTrue(Arrays.equals(SOMETHING_ELSE, pnd.getParameterNames((Constructor) null)));
		pnd.addDiscoverer(returnsFooBar);
		assertTrue(Arrays.equals(SOMETHING_ELSE, pnd.getParameterNames(anyMethod)));
		assertTrue(Arrays.equals(SOMETHING_ELSE, pnd.getParameterNames((Constructor) null)));
	}

}
