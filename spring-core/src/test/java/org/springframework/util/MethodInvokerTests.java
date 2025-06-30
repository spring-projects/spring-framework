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

package org.springframework.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 21.11.2003
 */
class MethodInvokerTests {

	@Test
	void plainMethodInvoker() throws Exception {
		// sanity check: singleton, non-static should work
		TestClass1 tc1 = new TestClass1();
		MethodInvoker mi = new MethodInvoker();
		mi.setTargetObject(tc1);
		mi.setTargetMethod("method1");
		mi.prepare();
		Integer i = (Integer) mi.invoke();
		assertThat(i).isEqualTo(1);

		// defensive check: singleton, non-static should work with null array
		tc1 = new TestClass1();
		mi = new MethodInvoker();
		mi.setTargetObject(tc1);
		mi.setTargetMethod("method1");
		mi.setArguments((Object[]) null);
		mi.prepare();
		i = (Integer) mi.invoke();
		assertThat(i).isEqualTo(1);

		// sanity check: check that argument count matching works
		mi = new MethodInvoker();
		mi.setTargetClass(TestClass1.class);
		mi.setTargetMethod("supertypes");
		mi.setArguments(new ArrayList<>(), new ArrayList<>(), "hello");
		mi.prepare();
		assertThat(mi.invoke()).isEqualTo("hello");

		mi = new MethodInvoker();
		mi.setTargetClass(TestClass1.class);
		mi.setTargetMethod("supertypes2");
		mi.setArguments(new ArrayList<>(), new ArrayList<>(), "hello", "bogus");
		mi.prepare();
		assertThat(mi.invoke()).isEqualTo("hello");

		// Sanity check: check that argument conversion doesn't work with plain MethodInvoker
		mi = new MethodInvoker();
		mi.setTargetClass(TestClass1.class);
		mi.setTargetMethod("supertypes2");
		mi.setArguments(new ArrayList<>(), new ArrayList<>(), "hello", Boolean.TRUE);

		assertThatExceptionOfType(NoSuchMethodException.class).isThrownBy(
				mi::prepare);
	}

	@Test
	void stringWithMethodInvoker() {
		MethodInvoker methodInvoker = new MethodInvoker();
		methodInvoker.setTargetObject(new Greeter());
		methodInvoker.setTargetMethod("greet");
		methodInvoker.setArguments("no match");

		assertThatExceptionOfType(NoSuchMethodException.class).isThrownBy(
				methodInvoker::prepare);
	}

	@Test
	void purchaserWithMethodInvoker() throws Exception {
		MethodInvoker methodInvoker = new MethodInvoker();
		methodInvoker.setTargetObject(new Greeter());
		methodInvoker.setTargetMethod("greet");
		methodInvoker.setArguments(new Purchaser());
		methodInvoker.prepare();
		String greeting = (String) methodInvoker.invoke();
		assertThat(greeting).isEqualTo("purchaser: hello");
	}

	@Test
	void shopperWithMethodInvoker() throws Exception {
		MethodInvoker methodInvoker = new MethodInvoker();
		methodInvoker.setTargetObject(new Greeter());
		methodInvoker.setTargetMethod("greet");
		methodInvoker.setArguments(new Shopper());
		methodInvoker.prepare();
		String greeting = (String) methodInvoker.invoke();
		assertThat(greeting).isEqualTo("purchaser: may I help you?");
	}

	@Test
	void salesmanWithMethodInvoker() throws Exception {
		MethodInvoker methodInvoker = new MethodInvoker();
		methodInvoker.setTargetObject(new Greeter());
		methodInvoker.setTargetMethod("greet");
		methodInvoker.setArguments(new Salesman());
		methodInvoker.prepare();
		String greeting = (String) methodInvoker.invoke();
		assertThat(greeting).isEqualTo("greetable: how are sales?");
	}

	@Test
	void customerWithMethodInvoker() throws Exception {
		MethodInvoker methodInvoker = new MethodInvoker();
		methodInvoker.setTargetObject(new Greeter());
		methodInvoker.setTargetMethod("greet");
		methodInvoker.setArguments(new Customer());
		methodInvoker.prepare();
		String greeting = (String) methodInvoker.invoke();
		assertThat(greeting).isEqualTo("customer: good day");
	}

	@Test
	void regularWithMethodInvoker() throws Exception {
		MethodInvoker methodInvoker = new MethodInvoker();
		methodInvoker.setTargetObject(new Greeter());
		methodInvoker.setTargetMethod("greet");
		methodInvoker.setArguments(new Regular("Kotter"));
		methodInvoker.prepare();
		String greeting = (String) methodInvoker.invoke();
		assertThat(greeting).isEqualTo("regular: welcome back Kotter");
	}

	@Test
	void vipWithMethodInvoker() throws Exception {
		MethodInvoker methodInvoker = new MethodInvoker();
		methodInvoker.setTargetObject(new Greeter());
		methodInvoker.setTargetMethod("greet");
		methodInvoker.setArguments(new VIP("Fonzie"));
		methodInvoker.prepare();
		String greeting = (String) methodInvoker.invoke();
		assertThat(greeting).isEqualTo("regular: whassup dude?");
	}


	public static class TestClass1 {

		public static int _staticField1;

		public int _field1 = 0;

		public int method1() {
			return ++_field1;
		}

		public static int staticMethod1() {
			return ++TestClass1._staticField1;
		}

		public static void voidRetvalMethod() {
		}

		public static void nullArgument(Object arg) {
		}

		public static void intArgument(int arg) {
		}

		public static void intArguments(int[] arg) {
		}

		public static String supertypes(Collection<?> c, Integer i) {
			return i.toString();
		}

		public static String supertypes(Collection<?> c, List<?> l, String s) {
			return s;
		}

		public static String supertypes2(Collection<?> c, List<?> l, Integer i) {
			return i.toString();
		}

		public static String supertypes2(Collection<?> c, List<?> l, String s, Integer i) {
			return s;
		}

		public static String supertypes2(Collection<?> c, List<?> l, String s, String s2) {
			return s;
		}
	}


	@SuppressWarnings("unused")
	public static class Greeter {

		// should handle Salesman (only interface)
		public String greet(Greetable greetable) {
			return "greetable: " + greetable.getGreeting();
		}

		// should handle Shopper (beats Greetable since it is a class)
		protected String greet(Purchaser purchaser) {
			return "purchaser: " + purchaser.getGreeting();
		}

		// should handle Customer (exact match)
		String greet(Customer customer) {
			return "customer: " + customer.getGreeting();
		}

		// should handle Regular (exact) and VIP (closest match)
		private String greet(Regular regular) {
			return "regular: " + regular.getGreeting();
		}
	}


	private interface Greetable {

		String getGreeting();
	}


	private interface Person extends Greetable {
	}


	private static class Purchaser implements Greetable {

		@Override
		public String getGreeting() {
			return "hello";
		}
	}


	private static class Shopper extends Purchaser implements Person {

		@Override
		public String getGreeting() {
			return "may I help you?";
		}
	}


	private static class Salesman implements Person {

		@Override
		public String getGreeting() {
			return "how are sales?";
		}
	}


	private static class Customer extends Shopper {

		@Override
		public String getGreeting() {
			return "good day";
		}
	}


	private static class Regular extends Customer {

		private String name;

		public Regular(String name) {
			this.name = name;
		}

		@Override
		public String getGreeting() {
			return "welcome back " + name ;
		}
	}


	private static class VIP extends Regular {

		public VIP(String name) {
			super(name);
		}

		@Override
		public String getGreeting() {
			return "whassup dude?";
		}
	}

}
