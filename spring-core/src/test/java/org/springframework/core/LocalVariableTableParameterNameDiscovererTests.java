/*
 * Copyright 2002-2022 the original author or authors.
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

import java.awt.Component;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Date;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.tests.sample.objects.TestObject;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Adrian Colyer
 */
class LocalVariableTableParameterNameDiscovererTests {

	private final ParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();


	@Test
	void methodParameterNameDiscoveryNoArgs() throws NoSuchMethodException {
		Method getName = TestObject.class.getMethod("getName");
		String[] names = discoverer.getParameterNames(getName);
		assertThat(names).as("should find method info").isNotNull();
		assertThat(names.length).as("no argument names").isEqualTo(0);
	}

	@Test
	void methodParameterNameDiscoveryWithArgs() throws NoSuchMethodException {
		Method setName = TestObject.class.getMethod("setName", String.class);
		String[] names = discoverer.getParameterNames(setName);
		assertThat(names).as("should find method info").isNotNull();
		assertThat(names.length).as("one argument").isEqualTo(1);
		assertThat(names[0]).isEqualTo("name");
	}

	@Test
	void consParameterNameDiscoveryNoArgs() throws NoSuchMethodException {
		Constructor<TestObject> noArgsCons = TestObject.class.getConstructor();
		String[] names = discoverer.getParameterNames(noArgsCons);
		assertThat(names).as("should find cons info").isNotNull();
		assertThat(names.length).as("no argument names").isEqualTo(0);
	}

	@Test
	void consParameterNameDiscoveryArgs() throws NoSuchMethodException {
		Constructor<TestObject> twoArgCons = TestObject.class.getConstructor(String.class, int.class);
		String[] names = discoverer.getParameterNames(twoArgCons);
		assertThat(names).as("should find cons info").isNotNull();
		assertThat(names.length).as("one argument").isEqualTo(2);
		assertThat(names[0]).isEqualTo("name");
		assertThat(names[1]).isEqualTo("age");
	}

	@Test
	void staticMethodParameterNameDiscoveryNoArgs() throws NoSuchMethodException {
		Method m = getClass().getMethod("staticMethodNoLocalVars");
		String[] names = discoverer.getParameterNames(m);
		assertThat(names).as("should find method info").isNotNull();
		assertThat(names.length).as("no argument names").isEqualTo(0);
	}

	@Test
	void overloadedStaticMethod() throws Exception {
		Class<? extends LocalVariableTableParameterNameDiscovererTests> clazz = this.getClass();

		Method m1 = clazz.getMethod("staticMethod", Long.TYPE, Long.TYPE);
		String[] names = discoverer.getParameterNames(m1);
		assertThat(names).as("should find method info").isNotNull();
		assertThat(names.length).as("two arguments").isEqualTo(2);
		assertThat(names[0]).isEqualTo("x");
		assertThat(names[1]).isEqualTo("y");

		Method m2 = clazz.getMethod("staticMethod", Long.TYPE, Long.TYPE, Long.TYPE);
		names = discoverer.getParameterNames(m2);
		assertThat(names).as("should find method info").isNotNull();
		assertThat(names.length).as("three arguments").isEqualTo(3);
		assertThat(names[0]).isEqualTo("x");
		assertThat(names[1]).isEqualTo("y");
		assertThat(names[2]).isEqualTo("z");
	}

	@Test
	void overloadedStaticMethodInInnerClass() throws Exception {
		Class<InnerClass> clazz = InnerClass.class;

		Method m1 = clazz.getMethod("staticMethod", Long.TYPE);
		String[] names = discoverer.getParameterNames(m1);
		assertThat(names).as("should find method info").isNotNull();
		assertThat(names.length).as("one argument").isEqualTo(1);
		assertThat(names[0]).isEqualTo("x");

		Method m2 = clazz.getMethod("staticMethod", Long.TYPE, Long.TYPE);
		names = discoverer.getParameterNames(m2);
		assertThat(names).as("should find method info").isNotNull();
		assertThat(names.length).as("two arguments").isEqualTo(2);
		assertThat(names[0]).isEqualTo("x");
		assertThat(names[1]).isEqualTo("y");
	}

	@Test
	void overloadedMethod() throws Exception {
		Class<? extends LocalVariableTableParameterNameDiscovererTests> clazz = this.getClass();

		Method m1 = clazz.getMethod("instanceMethod", Double.TYPE, Double.TYPE);
		String[] names = discoverer.getParameterNames(m1);
		assertThat(names).as("should find method info").isNotNull();
		assertThat(names.length).as("two arguments").isEqualTo(2);
		assertThat(names[0]).isEqualTo("x");
		assertThat(names[1]).isEqualTo("y");

		Method m2 = clazz.getMethod("instanceMethod", Double.TYPE, Double.TYPE, Double.TYPE);
		names = discoverer.getParameterNames(m2);
		assertThat(names).as("should find method info").isNotNull();
		assertThat(names.length).as("three arguments").isEqualTo(3);
		assertThat(names[0]).isEqualTo("x");
		assertThat(names[1]).isEqualTo("y");
		assertThat(names[2]).isEqualTo("z");
	}

	@Test
	void overloadedMethodInInnerClass() throws Exception {
		Class<InnerClass> clazz = InnerClass.class;

		Method m1 = clazz.getMethod("instanceMethod", String.class);
		String[] names = discoverer.getParameterNames(m1);
		assertThat(names).as("should find method info").isNotNull();
		assertThat(names.length).as("one argument").isEqualTo(1);
		assertThat(names[0]).isEqualTo("aa");

		Method m2 = clazz.getMethod("instanceMethod", String.class, String.class);
		names = discoverer.getParameterNames(m2);
		assertThat(names).as("should find method info").isNotNull();
		assertThat(names.length).as("two arguments").isEqualTo(2);
		assertThat(names[0]).isEqualTo("aa");
		assertThat(names[1]).isEqualTo("bb");
	}

	@Test
	void generifiedClass() throws Exception {
		Class<?> clazz = GenerifiedClass.class;

		Constructor<?> ctor = clazz.getDeclaredConstructor(Object.class);
		String[] names = discoverer.getParameterNames(ctor);
		assertThat(names).hasSize(1);
		assertThat(names[0]).isEqualTo("key");

		ctor = clazz.getDeclaredConstructor(Object.class, Object.class);
		names = discoverer.getParameterNames(ctor);
		assertThat(names).hasSize(2);
		assertThat(names[0]).isEqualTo("key");
		assertThat(names[1]).isEqualTo("value");

		Method m = clazz.getMethod("generifiedStaticMethod", Object.class);
		names = discoverer.getParameterNames(m);
		assertThat(names).hasSize(1);
		assertThat(names[0]).isEqualTo("param");

		m = clazz.getMethod("generifiedMethod", Object.class, long.class, Object.class, Object.class);
		names = discoverer.getParameterNames(m);
		assertThat(names).hasSize(4);
		assertThat(names[0]).isEqualTo("param");
		assertThat(names[1]).isEqualTo("x");
		assertThat(names[2]).isEqualTo("key");
		assertThat(names[3]).isEqualTo("value");

		m = clazz.getMethod("voidStaticMethod", Object.class, long.class, int.class);
		names = discoverer.getParameterNames(m);
		assertThat(names).hasSize(3);
		assertThat(names[0]).isEqualTo("obj");
		assertThat(names[1]).isEqualTo("x");
		assertThat(names[2]).isEqualTo("i");

		m = clazz.getMethod("nonVoidStaticMethod", Object.class, long.class, int.class);
		names = discoverer.getParameterNames(m);
		assertThat(names).hasSize(3);
		assertThat(names[0]).isEqualTo("obj");
		assertThat(names[1]).isEqualTo("x");
		assertThat(names[2]).isEqualTo("i");

		m = clazz.getMethod("getDate");
		names = discoverer.getParameterNames(m);
		assertThat(names).isEmpty();
	}

	@Disabled("Ignored because Ubuntu packages OpenJDK with debug symbols enabled. See SPR-8078.")
	@Test
	void classesWithoutDebugSymbols() throws Exception {
		// JDK classes don't have debug information (usually)
		Class<Component> clazz = Component.class;
		String methodName = "list";

		Method m = clazz.getMethod(methodName);
		String[] names = discoverer.getParameterNames(m);
		assertThat(names).isNull();

		m = clazz.getMethod(methodName, PrintStream.class);
		names = discoverer.getParameterNames(m);
		assertThat(names).isNull();

		m = clazz.getMethod(methodName, PrintStream.class, int.class);
		names = discoverer.getParameterNames(m);
		assertThat(names).isNull();
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
