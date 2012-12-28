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

package org.springframework.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

/**
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 21.11.2003
 */
public class MethodInvokerTests extends TestCase {

	public void testPlainMethodInvoker() throws Exception {
		// sanity check: singleton, non-static should work
		TestClass1 tc1 = new TestClass1();
		MethodInvoker mi = new MethodInvoker();
		mi.setTargetObject(tc1);
		mi.setTargetMethod("method1");
		mi.prepare();
		Integer i = (Integer) mi.invoke();
		assertEquals(1, i.intValue());

		// sanity check: check that argument count matching works
		mi = new MethodInvoker();
		mi.setTargetClass(TestClass1.class);
		mi.setTargetMethod("supertypes");
		mi.setArguments(new Object[] {new ArrayList<>(), new ArrayList<>(), "hello"});
		mi.prepare();
		assertEquals("hello", mi.invoke());

		mi = new MethodInvoker();
		mi.setTargetClass(TestClass1.class);
		mi.setTargetMethod("supertypes2");
		mi.setArguments(new Object[] {new ArrayList<>(), new ArrayList<>(), "hello", "bogus"});
		mi.prepare();
		assertEquals("hello", mi.invoke());

		// Sanity check: check that argument conversion doesn't work with plain MethodInvoker
		mi = new MethodInvoker();
		mi.setTargetClass(TestClass1.class);
		mi.setTargetMethod("supertypes2");
		mi.setArguments(new Object[] {new ArrayList<>(), new ArrayList<>(), "hello", Boolean.TRUE});
		try {
			mi.prepare();
			fail("Shouldn't have matched without argument conversion");
		}
		catch (NoSuchMethodException ex) {
			// expected
		}
	}

	public void testStringWithMethodInvoker() throws Exception {
		try {
			MethodInvoker methodInvoker = new MethodInvoker();
			methodInvoker.setTargetObject(new Greeter());
			methodInvoker.setTargetMethod("greet");
			methodInvoker.setArguments(new Object[] {new String("no match")});
			methodInvoker.prepare();
			fail("Should have thrown a NoSuchMethodException");
		}
		catch (NoSuchMethodException e) {
			// expected
		}
	}

	public void testPurchaserWithMethodInvoker() throws Exception {
		MethodInvoker methodInvoker = new MethodInvoker();
		methodInvoker.setTargetObject(new Greeter());
		methodInvoker.setTargetMethod("greet");
		methodInvoker.setArguments(new Object[] {new Purchaser()});
		methodInvoker.prepare();
		String greeting = (String) methodInvoker.invoke();
		assertEquals("purchaser: hello", greeting);
	}

	public void testShopperWithMethodInvoker() throws Exception {
		MethodInvoker methodInvoker = new MethodInvoker();
		methodInvoker.setTargetObject(new Greeter());
		methodInvoker.setTargetMethod("greet");
		methodInvoker.setArguments(new Object[] {new Shopper()});
		methodInvoker.prepare();
		String greeting = (String) methodInvoker.invoke();
		assertEquals("purchaser: may I help you?", greeting);
	}

	public void testSalesmanWithMethodInvoker() throws Exception {
		MethodInvoker methodInvoker = new MethodInvoker();
		methodInvoker.setTargetObject(new Greeter());
		methodInvoker.setTargetMethod("greet");
		methodInvoker.setArguments(new Object[] {new Salesman()});
		methodInvoker.prepare();
		String greeting = (String) methodInvoker.invoke();
		assertEquals("greetable: how are sales?", greeting);
	}

	public void testCustomerWithMethodInvoker() throws Exception {
		MethodInvoker methodInvoker = new MethodInvoker();
		methodInvoker.setTargetObject(new Greeter());
		methodInvoker.setTargetMethod("greet");
		methodInvoker.setArguments(new Object[] {new Customer()});
		methodInvoker.prepare();
		String greeting = (String) methodInvoker.invoke();
		assertEquals("customer: good day", greeting);
	}

	public void testRegularWithMethodInvoker() throws Exception {
		MethodInvoker methodInvoker = new MethodInvoker();
		methodInvoker.setTargetObject(new Greeter());
		methodInvoker.setTargetMethod("greet");
		methodInvoker.setArguments(new Object[] {new Regular("Kotter")});
		methodInvoker.prepare();
		String greeting = (String) methodInvoker.invoke();
		assertEquals("regular: welcome back Kotter", greeting);
	}

	public void testVIPWithMethodInvoker() throws Exception {
		MethodInvoker methodInvoker = new MethodInvoker();
		methodInvoker.setTargetObject(new Greeter());
		methodInvoker.setTargetMethod("greet");
		methodInvoker.setArguments(new Object[] {new VIP("Fonzie")});
		methodInvoker.prepare();
		String greeting = (String) methodInvoker.invoke();
		assertEquals("regular: whassup dude?", greeting);
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


	private static interface Greetable {

		String getGreeting();
	}


	private static interface Person extends Greetable {
	}


	private static class Purchaser implements Greetable {

		public String getGreeting() {
			return "hello";
		}
	}


	private static class Shopper extends Purchaser implements Person {

		public String getGreeting() {
			return "may I help you?";
		}
	}


	private static class Salesman implements Person {

		public String getGreeting() {
			return "how are sales?";
		}
	}


	private static class Customer extends Shopper {

		public String getGreeting() {
			return "good day";
		}
	}


	private static class Regular extends Customer {

		private String name;

		public Regular(String name) {
			this.name = name;
		}

		public String getGreeting() {
			return "welcome back " + name ;
		}
	}


	private static class VIP extends Regular {

		public VIP(String name) {
			super(name);
		}

		public String getGreeting() {
			return "whassup dude?";
		}
	}

}
