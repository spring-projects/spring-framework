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

import java.awt.Component;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Date;

import junit.framework.TestCase;

import org.junit.Ignore;
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
		Method setName = TestBean.class.getMethod("setName", new Class[] { String.class });
		String[] names = discoverer.getParameterNames(setName);
		assertNotNull("should find method info", names);
		assertEquals("one argument", 1, names.length);
		assertEquals("name", names[0]);
	}

	public void testConsParameterNameDiscoveryNoArgs() throws NoSuchMethodException {
		Constructor<TestBean> noArgsCons = TestBean.class.getConstructor(new Class[0]);
		String[] names = discoverer.getParameterNames(noArgsCons);
		assertNotNull("should find cons info", names);
		assertEquals("no argument names", 0, names.length);
	}

	public void testConsParameterNameDiscoveryArgs() throws NoSuchMethodException {
		Constructor<TestBean> twoArgCons = TestBean.class.getConstructor(new Class[] { String.class, int.class });
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
		Class<? extends LocalVariableTableParameterNameDiscovererTests> clazz = this.getClass();

		Method m1 = clazz.getMethod("staticMethod", new Class[] { Long.TYPE, Long.TYPE });
		String[] names = discoverer.getParameterNames(m1);
		assertNotNull("should find method info", names);
		assertEquals("two arguments", 2, names.length);
		assertEquals("x", names[0]);
		assertEquals("y", names[1]);

		Method m2 = clazz.getMethod("staticMethod", new Class[] { Long.TYPE, Long.TYPE, Long.TYPE });
		names = discoverer.getParameterNames(m2);
		assertNotNull("should find method info", names);
		assertEquals("three arguments", 3, names.length);
		assertEquals("x", names[0]);
		assertEquals("y", names[1]);
		assertEquals("z", names[2]);
	}

	public void testOverloadedStaticMethodInInnerClass() throws Exception {
		Class<InnerClass> clazz = InnerClass.class;

		Method m1 = clazz.getMethod("staticMethod", new Class[] { Long.TYPE });
		String[] names = discoverer.getParameterNames(m1);
		assertNotNull("should find method info", names);
		assertEquals("one argument", 1, names.length);
		assertEquals("x", names[0]);

		Method m2 = clazz.getMethod("staticMethod", new Class[] { Long.TYPE, Long.TYPE });
		names = discoverer.getParameterNames(m2);
		assertNotNull("should find method info", names);
		assertEquals("two arguments", 2, names.length);
		assertEquals("x", names[0]);
		assertEquals("y", names[1]);
	}

	public void testOverloadedMethod() throws Exception {
		Class<? extends LocalVariableTableParameterNameDiscovererTests> clazz = this.getClass();

		Method m1 = clazz.getMethod("instanceMethod", new Class[] { Double.TYPE, Double.TYPE });
		String[] names = discoverer.getParameterNames(m1);
		assertNotNull("should find method info", names);
		assertEquals("two arguments", 2, names.length);
		assertEquals("x", names[0]);
		assertEquals("y", names[1]);

		Method m2 = clazz.getMethod("instanceMethod", new Class[] { Double.TYPE, Double.TYPE, Double.TYPE });
		names = discoverer.getParameterNames(m2);
		assertNotNull("should find method info", names);
		assertEquals("three arguments", 3, names.length);
		assertEquals("x", names[0]);
		assertEquals("y", names[1]);
		assertEquals("z", names[2]);
	}

	public void testOverloadedMethodInInnerClass() throws Exception {
		Class<InnerClass> clazz = InnerClass.class;

		Method m1 = clazz.getMethod("instanceMethod", new Class[] { String.class });
		String[] names = discoverer.getParameterNames(m1);
		assertNotNull("should find method info", names);
		assertEquals("one argument", 1, names.length);
		assertEquals("aa", names[0]);

		Method m2 = clazz.getMethod("instanceMethod", new Class[] { String.class, String.class });
		names = discoverer.getParameterNames(m2);
		assertNotNull("should find method info", names);
		assertEquals("two arguments", 2, names.length);
		assertEquals("aa", names[0]);
		assertEquals("bb", names[1]);
	}

	public void testGenerifiedClass() throws Exception {
		Class<?> clazz = (Class<?>)GenerifiedClass.class;

		Constructor<?> ctor = clazz.getDeclaredConstructor(Object.class);
		String[] names = discoverer.getParameterNames(ctor);
		assertEquals(1, names.length);
		assertEquals("key", names[0]);

		ctor = clazz.getDeclaredConstructor(Object.class, Object.class);
		names = discoverer.getParameterNames(ctor);
		assertEquals(2, names.length);
		assertEquals("key", names[0]);
		assertEquals("value", names[1]);

		Method m = clazz.getMethod("generifiedStaticMethod", Object.class);
		names = discoverer.getParameterNames(m);
		assertEquals(1, names.length);
		assertEquals("param", names[0]);

		m = clazz.getMethod("generifiedMethod", Object.class, long.class, Object.class, Object.class);
		names = discoverer.getParameterNames(m);
		assertEquals(4, names.length);
		assertEquals("param", names[0]);
		assertEquals("x", names[1]);
		assertEquals("key", names[2]);
		assertEquals("value", names[3]);

		m = clazz.getMethod("voidStaticMethod", Object.class, long.class, int.class);
		names = discoverer.getParameterNames(m);
		assertEquals(3, names.length);
		assertEquals("obj", names[0]);
		assertEquals("x", names[1]);
		assertEquals("i", names[2]);

		m = clazz.getMethod("nonVoidStaticMethod", Object.class, long.class, int.class);
		names = discoverer.getParameterNames(m);
		assertEquals(3, names.length);
		assertEquals("obj", names[0]);
		assertEquals("x", names[1]);
		assertEquals("i", names[2]);

		m = clazz.getMethod("getDate");
		names = discoverer.getParameterNames(m);
		assertEquals(0, names.length);

		//System.in.read();
	}

	/**
	 * Ignored because Ubuntu packages OpenJDK with debug symbols enabled.
	 * See SPR-8078.
	 */
	@Ignore
	public void ignore_testClassesWithoutDebugSymbols() throws Exception {
		// JDK classes don't have debug information (usually)
		Class<Component> clazz = Component.class;
		String methodName = "list";

		Method m = clazz.getMethod(methodName);
		String[] names = discoverer.getParameterNames(m);
		assertNull(names);

		m = clazz.getMethod(methodName, PrintStream.class);
		names = discoverer.getParameterNames(m);
		assertNull(names);

		m = clazz.getMethod(methodName, PrintStream.class, int.class);
		names = discoverer.getParameterNames(m);
		assertNull(names);

		//System.in.read();
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

	public static class GenerifiedClass<K, V> {
		private static long date;

		static {
			// some custom static bloc or <clinit>
			date = new Date().getTime();
		}

		public GenerifiedClass() {
			this(null, null);
		}

		public GenerifiedClass(K key) {
			this(key, null);
		}

		public GenerifiedClass(K key, V value) {
		}

		public static <P> long generifiedStaticMethod(P param) {
			return date;
		}

		public <P> void generifiedMethod(P param, long x, K key, V value) {
			// nothing
		}

		public static void voidStaticMethod(Object obj, long x, int i) {
			// nothing
		}

		public static long nonVoidStaticMethod(Object obj, long x, int i) {
			return date;
		}

		public static long getDate() {
			return date;
		}
	}
}
