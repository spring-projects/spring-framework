/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.beans.factory.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.beans.support.ArgumentConvertingMethodInvoker;
import org.springframework.util.MethodInvoker;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link MethodInvokingFactoryBean} and {@link MethodInvokingBean}.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 21.11.2003
 */
public class MethodInvokingFactoryBeanTests {

	@Test
	public void testParameterValidation() throws Exception {
		String validationError = "improper validation of input properties";

		// assert that only static OR non static are set, but not both or none
		MethodInvokingFactoryBean mcfb = new MethodInvokingFactoryBean();
		try {
			mcfb.afterPropertiesSet();
			fail(validationError);
		}
		catch (IllegalArgumentException ex) {
			// expected
		}

		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetObject(this);
		mcfb.setTargetMethod("whatever");
		try {
			mcfb.afterPropertiesSet();
			fail(validationError);
		}
		catch (NoSuchMethodException ex) {
			// expected
		}

		// bogus static method
		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetClass(TestClass1.class);
		mcfb.setTargetMethod("some.bogus.Method.name");
		try {
			mcfb.afterPropertiesSet();
			fail(validationError);
		}
		catch (NoSuchMethodException ex) {
			// expected
		}

		// bogus static method
		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetClass(TestClass1.class);
		mcfb.setTargetMethod("method1");
		try {
			mcfb.afterPropertiesSet();
			fail(validationError);
		}
		catch (IllegalArgumentException ex) {
			// expected
		}

		// missing method
		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetObject(this);
		try {
			mcfb.afterPropertiesSet();
			fail(validationError);
		}
		catch (IllegalArgumentException ex) {
			// expected
		}

		// bogus method
		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetObject(this);
		mcfb.setTargetMethod("bogus");
		try {
			mcfb.afterPropertiesSet();
			fail(validationError);
		}
		catch (NoSuchMethodException ex) {
			// expected
		}

		// static method
		TestClass1._staticField1 = 0;
		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetClass(TestClass1.class);
		mcfb.setTargetMethod("staticMethod1");
		mcfb.afterPropertiesSet();

		// non-static method
		TestClass1 tc1 = new TestClass1();
		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetObject(tc1);
		mcfb.setTargetMethod("method1");
		mcfb.afterPropertiesSet();
	}

	@Test
	public void testGetObjectType() throws Exception {
		TestClass1 tc1 = new TestClass1();
		MethodInvokingFactoryBean mcfb = new MethodInvokingFactoryBean();
		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetObject(tc1);
		mcfb.setTargetMethod("method1");
		mcfb.afterPropertiesSet();
		assertTrue(int.class.equals(mcfb.getObjectType()));

		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetClass(TestClass1.class);
		mcfb.setTargetMethod("voidRetvalMethod");
		mcfb.afterPropertiesSet();
		Class<?> objType = mcfb.getObjectType();
		assertSame(objType, void.class);

		// verify that we can call a method with args that are subtypes of the
		// target method arg types
		TestClass1._staticField1 = 0;
		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetClass(TestClass1.class);
		mcfb.setTargetMethod("supertypes");
		mcfb.setArguments(new ArrayList<>(), new ArrayList<Object>(), "hello");
		mcfb.afterPropertiesSet();
		mcfb.getObjectType();

		// fail on improper argument types at afterPropertiesSet
		mcfb = new MethodInvokingFactoryBean();
		mcfb.registerCustomEditor(String.class, new StringTrimmerEditor(false));
		mcfb.setTargetClass(TestClass1.class);
		mcfb.setTargetMethod("supertypes");
		mcfb.setArguments("1", new Object());
		try {
			mcfb.afterPropertiesSet();
			fail("Should have thrown NoSuchMethodException");
		}
		catch (NoSuchMethodException ex) {
			// expected
		}
	}

	@Test
	public void testGetObject() throws Exception {
		// singleton, non-static
		TestClass1 tc1 = new TestClass1();
		MethodInvokingFactoryBean mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetObject(tc1);
		mcfb.setTargetMethod("method1");
		mcfb.afterPropertiesSet();
		Integer i = (Integer) mcfb.getObject();
		assertEquals(1, i.intValue());
		i = (Integer) mcfb.getObject();
		assertEquals(1, i.intValue());

		// non-singleton, non-static
		tc1 = new TestClass1();
		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetObject(tc1);
		mcfb.setTargetMethod("method1");
		mcfb.setSingleton(false);
		mcfb.afterPropertiesSet();
		i = (Integer) mcfb.getObject();
		assertEquals(1, i.intValue());
		i = (Integer) mcfb.getObject();
		assertEquals(2, i.intValue());

		// singleton, static
		TestClass1._staticField1 = 0;
		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetClass(TestClass1.class);
		mcfb.setTargetMethod("staticMethod1");
		mcfb.afterPropertiesSet();
		i = (Integer) mcfb.getObject();
		assertEquals(1, i.intValue());
		i = (Integer) mcfb.getObject();
		assertEquals(1, i.intValue());

		// non-singleton, static
		TestClass1._staticField1 = 0;
		mcfb = new MethodInvokingFactoryBean();
		mcfb.setStaticMethod("org.springframework.beans.factory.config.MethodInvokingFactoryBeanTests$TestClass1.staticMethod1");
		mcfb.setSingleton(false);
		mcfb.afterPropertiesSet();
		i = (Integer) mcfb.getObject();
		assertEquals(1, i.intValue());
		i = (Integer) mcfb.getObject();
		assertEquals(2, i.intValue());

		// void return value
		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetClass(TestClass1.class);
		mcfb.setTargetMethod("voidRetvalMethod");
		mcfb.afterPropertiesSet();
		assertNull(mcfb.getObject());

		// now see if we can match methods with arguments that have supertype arguments
		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetClass(TestClass1.class);
		mcfb.setTargetMethod("supertypes");
		mcfb.setArguments(new ArrayList<>(), new ArrayList<Object>(), "hello");
		// should pass
		mcfb.afterPropertiesSet();
	}

	@Test
	public void testArgumentConversion() throws Exception {
		MethodInvokingFactoryBean mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetClass(TestClass1.class);
		mcfb.setTargetMethod("supertypes");
		mcfb.setArguments(new ArrayList<>(), new ArrayList<Object>(), "hello", "bogus");
		try {
			mcfb.afterPropertiesSet();
			fail("Matched method with wrong number of args");
		}
		catch (NoSuchMethodException ex) {
			// expected
		}

		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetClass(TestClass1.class);
		mcfb.setTargetMethod("supertypes");
		mcfb.setArguments(1, new Object());
		try {
			mcfb.afterPropertiesSet();
			mcfb.getObject();
			fail("Should have failed on getObject with mismatched argument types");
		}
		catch (NoSuchMethodException ex) {
			// expected
		}

		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetClass(TestClass1.class);
		mcfb.setTargetMethod("supertypes2");
		mcfb.setArguments(new ArrayList<>(), new ArrayList<Object>(), "hello", "bogus");
		mcfb.afterPropertiesSet();
		assertEquals("hello", mcfb.getObject());

		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetClass(TestClass1.class);
		mcfb.setTargetMethod("supertypes2");
		mcfb.setArguments(new ArrayList<>(), new ArrayList<Object>(), new Object());
		try {
			mcfb.afterPropertiesSet();
			fail("Matched method when shouldn't have matched");
		}
		catch (NoSuchMethodException ex) {
			// expected
		}
	}

	@Test
	public void testInvokeWithNullArgument() throws Exception {
		MethodInvoker methodInvoker = new MethodInvoker();
		methodInvoker.setTargetClass(TestClass1.class);
		methodInvoker.setTargetMethod("nullArgument");
		methodInvoker.setArguments(new Object[] {null});
		methodInvoker.prepare();
		methodInvoker.invoke();
	}

	@Test
	public void testInvokeWithIntArgument() throws Exception {
		ArgumentConvertingMethodInvoker methodInvoker = new ArgumentConvertingMethodInvoker();
		methodInvoker.setTargetClass(TestClass1.class);
		methodInvoker.setTargetMethod("intArgument");
		methodInvoker.setArguments(5);
		methodInvoker.prepare();
		methodInvoker.invoke();

		methodInvoker = new ArgumentConvertingMethodInvoker();
		methodInvoker.setTargetClass(TestClass1.class);
		methodInvoker.setTargetMethod("intArgument");
		methodInvoker.setArguments(5);
		methodInvoker.prepare();
		methodInvoker.invoke();
	}

	@Test
	public void testInvokeWithIntArguments() throws Exception {
		MethodInvokingBean methodInvoker = new MethodInvokingBean();
		methodInvoker.setTargetClass(TestClass1.class);
		methodInvoker.setTargetMethod("intArguments");
		methodInvoker.setArguments(new Object[] {new Integer[] {5, 10}});
		methodInvoker.afterPropertiesSet();

		methodInvoker = new MethodInvokingBean();
		methodInvoker.setTargetClass(TestClass1.class);
		methodInvoker.setTargetMethod("intArguments");
		methodInvoker.setArguments(new Object[] {new String[] {"5", "10"}});
		methodInvoker.afterPropertiesSet();

		methodInvoker = new MethodInvokingBean();
		methodInvoker.setTargetClass(TestClass1.class);
		methodInvoker.setTargetMethod("intArguments");
		methodInvoker.setArguments(new Object[] {new Integer[] {5, 10}});
		methodInvoker.afterPropertiesSet();

		methodInvoker = new MethodInvokingBean();
		methodInvoker.setTargetClass(TestClass1.class);
		methodInvoker.setTargetMethod("intArguments");
		methodInvoker.setArguments("5", "10");
		methodInvoker.afterPropertiesSet();

		methodInvoker = new MethodInvokingBean();
		methodInvoker.setTargetClass(TestClass1.class);
		methodInvoker.setTargetMethod("intArguments");
		methodInvoker.setArguments(new Object[] {new Integer[] {5, 10}});
		methodInvoker.afterPropertiesSet();

		methodInvoker = new MethodInvokingBean();
		methodInvoker.setTargetClass(TestClass1.class);
		methodInvoker.setTargetMethod("intArguments");
		methodInvoker.setArguments("5", "10");
		methodInvoker.afterPropertiesSet();
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

}
