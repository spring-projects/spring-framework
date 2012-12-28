/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.aop.framework;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;

import test.beans.TestBean;

/**
 * @author Rod Johnson
 * @author Chris Beams
 * @since 14.03.2003
 */
public final class MethodInvocationTests {

	@Test
	public void testValidInvocation() throws Throwable {
		Method m = Object.class.getMethod("hashCode", (Class[]) null);
		Object proxy = new Object();
		final Object returnValue = new Object();
		List<Object> is = new LinkedList<Object>();
		is.add(new MethodInterceptor() {
			@Override
			public Object invoke(MethodInvocation invocation) throws Throwable {
				return returnValue;
			}
		});
			ReflectiveMethodInvocation invocation = new ReflectiveMethodInvocation(proxy, null, //?
		m, null, null, is // list
	);
		Object rv = invocation.proceed();
		assertTrue("correct response", rv == returnValue);
	}

	/**
	 * toString on target can cause failure.
	 */
	@Test
	public void testToStringDoesntHitTarget() throws Throwable {
		Object target = new TestBean() {
			public String toString() {
				throw new UnsupportedOperationException("toString");
			}
		};
		List<Object> is = new LinkedList<Object>();

		Method m = Object.class.getMethod("hashCode", (Class[]) null);
		Object proxy = new Object();
		ReflectiveMethodInvocation invocation =
			new ReflectiveMethodInvocation(proxy, target, m, null, null, is);

		// If it hits target, the test will fail with the UnsupportedOpException
		// in the inner class above.
		invocation.toString();
	}

}
