/*
 * Copyright 2002-2006 the original author or authors.
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

import junit.framework.TestCase;

import org.springframework.beans.TestBean;

/**
 * @author Adrian Colyer
 */
public class LocalVariableTableParameterNameDiscovererTests extends TestCase {

	private LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();


	public void testMethodParameterNameDiscoveryNoArgs() throws NoSuchMethodException {
		Method getName = TestBean.class.getMethod("getName", new Class[0]);
		String[] names = discoverer.getParameterNames(getName);
		assertNotNull("should find method info", names);
		assertEquals("no argument names", 0, names.length);
	}

	public void testMethodParameterNameDiscoveryWithArgs() throws NoSuchMethodException {
		Method setName = TestBean.class.getMethod("setName", new Class[]{String.class});
		String[] names = discoverer.getParameterNames(setName);
		assertNotNull("should find method info", names);
		assertEquals("one argument", 1, names.length);
		assertEquals("name", names[0]);
	}

	public void testConsParameterNameDiscoveryNoArgs() throws NoSuchMethodException {
		Constructor noArgsCons = TestBean.class.getConstructor(new Class[0]);
		String[] names = discoverer.getParameterNames(noArgsCons);
		assertNotNull("should find cons info", names);
		assertEquals("no argument names", 0, names.length);
	}

	public void testConsParameterNameDiscoveryArgs() throws NoSuchMethodException {
		Constructor twoArgCons = TestBean.class.getConstructor(new Class[]{String.class, int.class});
		String[] names = discoverer.getParameterNames(twoArgCons);
		assertNotNull("should find cons info", names);
		assertEquals("one argument", 2, names.length);
		assertEquals("name", names[0]);
		assertEquals("age", names[1]);
	}

	public void testStaticMethodParameterNameDiscoveryNoArgs() throws NoSuchMethodException {
		Method m = getClass().getMethod("staticMethodNoLocalVars", new Class[0]);
		String[] names = discoverer.getParameterNames(m);
		assertNotNull("should find method info", names);
		assertEquals("no argument names", 0, names.length);
	}

	public void testOverloadedStaticMethod() throws Exception {
		Class clazz = this.getClass();

		Method m1 = clazz.getMethod("staticMethod", new Class[]{Long.TYPE, Long.TYPE});
		String[] names = discoverer.getParameterNames(m1);
		assertNotNull("should find method info", names);
		assertEquals("two arguments", 2, names.length);
		assertEquals("x", names[0]);
		assertEquals("y", names[1]);

		Method m2 = clazz.getMethod("staticMethod", new Class[]{Long.TYPE, Long.TYPE, Long.TYPE});
		names = discoverer.getParameterNames(m2);
		assertNotNull("should find method info", names);
		assertEquals("three arguments", 3, names.length);
		assertEquals("x", names[0]);
		assertEquals("y", names[1]);
		assertEquals("z", names[2]);
	}

	public void testOverloadedStaticMethodInInnerClass() throws Exception {
		Class clazz = InnerClass.class;

		Method m1 = clazz.getMethod("staticMethod", new Class[]{Long.TYPE});
		String[] names = discoverer.getParameterNames(m1);
		assertNotNull("should find method info", names);
		assertEquals("one argument", 1, names.length);
		assertEquals("x", names[0]);

		Method m2 = clazz.getMethod("staticMethod", new Class[]{Long.TYPE, Long.TYPE});
		names = discoverer.getParameterNames(m2);
		assertNotNull("should find method info", names);
		assertEquals("two arguments", 2, names.length);
		assertEquals("x", names[0]);
		assertEquals("y", names[1]);
	}

	public void testOverloadedMethod() throws Exception {
		Class clazz = this.getClass();

		Method m1 = clazz.getMethod("instanceMethod", new Class[]{Double.TYPE, Double.TYPE});
		String[] names = discoverer.getParameterNames(m1);
		assertNotNull("should find method info", names);
		assertEquals("two arguments", 2, names.length);
		assertEquals("x", names[0]);
		assertEquals("y", names[1]);

		Method m2 = clazz.getMethod("instanceMethod", new Class[]{Double.TYPE, Double.TYPE, Double.TYPE});
		names = discoverer.getParameterNames(m2);
		assertNotNull("should find method info", names);
		assertEquals("three arguments", 3, names.length);
		assertEquals("x", names[0]);
		assertEquals("y", names[1]);
		assertEquals("z", names[2]);
	}

	public void testOverloadedMethodInInnerClass() throws Exception {
		Class clazz = InnerClass.class;

		Method m1 = clazz.getMethod("instanceMethod", new Class[]{String.class});
		String[] names = discoverer.getParameterNames(m1);
		assertNotNull("should find method info", names);
		assertEquals("one argument", 1, names.length);
		assertEquals("aa", names[0]);

		Method m2 = clazz.getMethod("instanceMethod", new Class[]{String.class, String.class});
		names = discoverer.getParameterNames(m2);
		assertNotNull("should find method info", names);
		assertEquals("two arguments", 2, names.length);
		assertEquals("aa", names[0]);
		assertEquals("bb", names[1]);
	}


	public static void staticMethodNoLocalVars() {
	}

	public static long staticMethod(long x, long y) {
		long u = x * y;
		return u;
	}

	public static long staticMethod(long x, long y, long z) {
		long u = x * y * z;
		return u;
	}

	public double instanceMethod(double x, double y) {
		double u = x * y;
		return u;
	}

	public double instanceMethod(double x, double y, double z) {
		double u = x * y * z;
		return u;
	}


	public static class InnerClass {

		public int waz = 0;

		public InnerClass() {
		}

		public InnerClass(String firstArg, long secondArg, Object thirdArg) {
			long foo = 0;
			short bar = 10;
			this.waz = (int) (foo + bar);
		}

		public String instanceMethod(String aa) {
			return aa;
		}

		public String instanceMethod(String aa, String bb) {
			return aa + bb;
		}

		public static long staticMethod(long x) {
			long u = x;
			return u;
		}

		public static long staticMethod(long x, long y) {
			long u = x * y;
			return u;
		}
	}

}
