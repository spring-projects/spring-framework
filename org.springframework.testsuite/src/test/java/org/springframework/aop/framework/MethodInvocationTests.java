/*
 * Copyright 2002-2005 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.beans.TestBean;

/**
 * TODO: could refactor to be generic.
 * @author Rod Johnson
 * @since 14.03.2003
 */
public class MethodInvocationTests extends TestCase {
	
	/*
	public static MethodInvocation testInvocation(Object o, String methodName, Class[] args, Interceptor[] interceptors) throws Exception {
		Method m = o.getClass().getMethod(methodName, args);
		MethodInvocationImpl invocation = new MethodInvocationImpl(null, null, m.getDeclaringClass(), 
	m, null, interceptors, // list
	new Attrib4jAttributeRegistry());
	return invocation;
	}*/

	/*
	public void testNullInterceptor() throws Exception {
		Method m = Object.class.getMethod("hashCode", null);
		Object proxy = new Object();
		try {
				MethodInvocationImpl invocation = new MethodInvocationImpl(proxy, null, m.getDeclaringClass(), //?
		m, null, null // could customize here
	);
			fail("Shouldn't be able to create methodInvocationImpl with null interceptors");
		} catch (AopConfigException ex) {
		}
	}

	public void testEmptyInterceptorList() throws Exception {
		Method m = Object.class.getMethod("hashCode", null);
		Object proxy = new Object();
		try {
				MethodInvocationImpl invocation = new MethodInvocationImpl(proxy, null, m.getDeclaringClass(), //?
		m, null, new LinkedList() // list
	);
			fail("Shouldn't be able to create methodInvocationImpl with no interceptors");
		} catch (AopConfigException ex) {
		}
	}
	*/

	public void testValidInvocation() throws Throwable {
		Method m = Object.class.getMethod("hashCode", (Class[]) null);
		Object proxy = new Object();
		final Object returnValue = new Object();
		List is = new LinkedList();
		is.add(new MethodInterceptor() {
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
	 * ToString on target can cause failure.
	 */
	public void testToStringDoesntHitTarget() throws Throwable {
		Object target = new TestBean() {
			public String toString() {
				throw new UnsupportedOperationException("toString");
			}
		};
		final Object returnValue = new Object();
		List is = new LinkedList();

		Method m = Object.class.getMethod("hashCode", (Class[]) null);
		Object proxy = new Object();
		ReflectiveMethodInvocation invocation =
		    new ReflectiveMethodInvocation(proxy, target, m, null, null, is);

		// If it hits target, the test will fail with the UnsupportedOpException
		// in the inner class above.
		invocation.toString();
	}

}
