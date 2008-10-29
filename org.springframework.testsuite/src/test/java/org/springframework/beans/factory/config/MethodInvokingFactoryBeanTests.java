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

package org.springframework.beans.factory.config;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.beans.support.ArgumentConvertingMethodInvoker;
import org.springframework.util.MethodInvoker;
import org.springframework.util.MethodInvokerTests;

/**
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 21.11.2003
 */
public class MethodInvokingFactoryBeanTests extends TestCase {

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
		mcfb.setTargetClass(MethodInvokerTests.TestClass1.class);
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
		mcfb.setTargetClass(MethodInvokerTests.TestClass1.class);
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
		MethodInvokerTests.TestClass1._staticField1 = 0;
		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetClass(MethodInvokerTests.TestClass1.class);
		mcfb.setTargetMethod("staticMethod1");
		mcfb.afterPropertiesSet();

		// non-static method
		MethodInvokerTests.TestClass1 tc1 = new MethodInvokerTests.TestClass1();
		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetObject(tc1);
		mcfb.setTargetMethod("method1");
		mcfb.afterPropertiesSet();
	}

	public void testGetObjectType() throws Exception {
		MethodInvokerTests.TestClass1 tc1 = new MethodInvokerTests.TestClass1();
		MethodInvokingFactoryBean mcfb = new MethodInvokingFactoryBean();
		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetObject(tc1);
		mcfb.setTargetMethod("method1");
		mcfb.afterPropertiesSet();
		assertTrue(int.class.equals(mcfb.getObjectType()));

		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetClass(MethodInvokerTests.TestClass1.class);
		mcfb.setTargetMethod("voidRetvalMethod");
		mcfb.afterPropertiesSet();
		Class objType = mcfb.getObjectType();
		assertTrue(objType.equals(void.class));

		// verify that we can call a method with args that are subtypes of the
		// target method arg types
		MethodInvokerTests.TestClass1._staticField1 = 0;
		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetClass(MethodInvokerTests.TestClass1.class);
		mcfb.setTargetMethod("supertypes");
		mcfb.setArguments(new Object[] {new ArrayList(), new ArrayList(), "hello"});
		mcfb.afterPropertiesSet();
		mcfb.getObjectType();

		// fail on improper argument types at afterPropertiesSet
		mcfb = new MethodInvokingFactoryBean();
		mcfb.registerCustomEditor(String.class, new StringTrimmerEditor(false));
		mcfb.setTargetClass(MethodInvokerTests.TestClass1.class);
		mcfb.setTargetMethod("supertypes");
		mcfb.setArguments(new Object[] {"1", new Object()});
		try {
			mcfb.afterPropertiesSet();
			fail("Should have thrown NoSuchMethodException");
		}
		catch (NoSuchMethodException ex) {
			// expected
		}
	}

	public void testGetObject() throws Exception {
		// singleton, non-static
		MethodInvokerTests.TestClass1 tc1 = new MethodInvokerTests.TestClass1();
		MethodInvokingFactoryBean mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetObject(tc1);
		mcfb.setTargetMethod("method1");
		mcfb.afterPropertiesSet();
		Integer i = (Integer) mcfb.getObject();
		assertEquals(1, i.intValue());
		i = (Integer) mcfb.getObject();
		assertEquals(1, i.intValue());

		// non-singleton, non-static
		tc1 = new MethodInvokerTests.TestClass1();
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
		MethodInvokerTests.TestClass1._staticField1 = 0;
		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetClass(MethodInvokerTests.TestClass1.class);
		mcfb.setTargetMethod("staticMethod1");
		mcfb.afterPropertiesSet();
		i = (Integer) mcfb.getObject();
		assertEquals(1, i.intValue());
		i = (Integer) mcfb.getObject();
		assertEquals(1, i.intValue());

		// non-singleton, static
		MethodInvokerTests.TestClass1._staticField1 = 0;
		mcfb = new MethodInvokingFactoryBean();
		mcfb.setStaticMethod("org.springframework.util.MethodInvokerTests$TestClass1.staticMethod1");
		mcfb.setSingleton(false);
		mcfb.afterPropertiesSet();
		i = (Integer) mcfb.getObject();
		assertEquals(1, i.intValue());
		i = (Integer) mcfb.getObject();
		assertEquals(2, i.intValue());

		// void return value
		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetClass(MethodInvokerTests.TestClass1.class);
		mcfb.setTargetMethod("voidRetvalMethod");
		mcfb.afterPropertiesSet();
		assertNull(mcfb.getObject());

		// now see if we can match methods with arguments that have supertype arguments
		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetClass(MethodInvokerTests.TestClass1.class);
		mcfb.setTargetMethod("supertypes");
		mcfb.setArguments(new Object[] {new ArrayList(), new ArrayList(), "hello"});
		// should pass
		mcfb.afterPropertiesSet();
	}

	public void testArgumentConversion() throws Exception {
		MethodInvokingFactoryBean mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetClass(MethodInvokerTests.TestClass1.class);
		mcfb.setTargetMethod("supertypes");
		mcfb.setArguments(new Object[] {new ArrayList(), new ArrayList(), "hello", "bogus"});
		try {
			mcfb.afterPropertiesSet();
			fail("Matched method with wrong number of args");
		}
		catch (NoSuchMethodException ex) {
			// expected
		}

		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetClass(MethodInvokerTests.TestClass1.class);
		mcfb.setTargetMethod("supertypes");
		mcfb.setArguments(new Object[] {new Integer(1), new Object()});
		try {
			mcfb.afterPropertiesSet();
			mcfb.getObject();
			fail("Should have failed on getObject with mismatched argument types");
		}
		catch (NoSuchMethodException ex) {
			// expected
		}

		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetClass(MethodInvokerTests.TestClass1.class);
		mcfb.setTargetMethod("supertypes2");
		mcfb.setArguments(new Object[] {new ArrayList(), new ArrayList(), "hello", "bogus"});
		mcfb.afterPropertiesSet();
		assertEquals("hello", mcfb.getObject());

		mcfb = new MethodInvokingFactoryBean();
		mcfb.setTargetClass(MethodInvokerTests.TestClass1.class);
		mcfb.setTargetMethod("supertypes2");
		mcfb.setArguments(new Object[] {new ArrayList(), new ArrayList(), new Object()});
		try {
			mcfb.afterPropertiesSet();
			fail("Matched method when shouldn't have matched");
		}
		catch (NoSuchMethodException ex) {
			// expected
		}
	}

	public void testInvokeWithNullArgument() throws Exception {
		MethodInvoker methodInvoker = new MethodInvoker();
		methodInvoker.setTargetClass(MethodInvokerTests.TestClass1.class);
		methodInvoker.setTargetMethod("nullArgument");
		methodInvoker.setArguments(new Object[] {null});
		methodInvoker.prepare();
		methodInvoker.invoke();
	}

	public void testInvokeWithIntArgument() throws Exception {
		ArgumentConvertingMethodInvoker methodInvoker = new ArgumentConvertingMethodInvoker();
		methodInvoker.setTargetClass(MethodInvokerTests.TestClass1.class);
		methodInvoker.setTargetMethod("intArgument");
		methodInvoker.setArguments(new Object[] {new Integer(5)});
		methodInvoker.prepare();
		methodInvoker.invoke();

		methodInvoker = new ArgumentConvertingMethodInvoker();
		methodInvoker.setTargetClass(MethodInvokerTests.TestClass1.class);
		methodInvoker.setTargetMethod("intArgument");
		methodInvoker.setArguments(new Object[] {"5"});
		methodInvoker.prepare();
		methodInvoker.invoke();
	}

	public void testInvokeWithIntArguments() throws Exception {
		ArgumentConvertingMethodInvoker methodInvoker = new ArgumentConvertingMethodInvoker();
		methodInvoker.setTargetClass(MethodInvokerTests.TestClass1.class);
		methodInvoker.setTargetMethod("intArguments");
		methodInvoker.setArguments(new Object[] {new Integer[] {new Integer(5), new Integer(10)}});
		methodInvoker.prepare();
		methodInvoker.invoke();

		methodInvoker = new ArgumentConvertingMethodInvoker();
		methodInvoker.setTargetClass(MethodInvokerTests.TestClass1.class);
		methodInvoker.setTargetMethod("intArguments");
		methodInvoker.setArguments(new Object[] {new String[] {"5", "10"}});
		methodInvoker.prepare();
		methodInvoker.invoke();

		methodInvoker = new ArgumentConvertingMethodInvoker();
		methodInvoker.setTargetClass(MethodInvokerTests.TestClass1.class);
		methodInvoker.setTargetMethod("intArguments");
		methodInvoker.setArguments(new Integer[] {new Integer(5), new Integer(10)});
		methodInvoker.prepare();
		methodInvoker.invoke();

		methodInvoker = new ArgumentConvertingMethodInvoker();
		methodInvoker.setTargetClass(MethodInvokerTests.TestClass1.class);
		methodInvoker.setTargetMethod("intArguments");
		methodInvoker.setArguments(new String[] {"5", "10"});
		methodInvoker.prepare();
		methodInvoker.invoke();

		methodInvoker = new ArgumentConvertingMethodInvoker();
		methodInvoker.setTargetClass(MethodInvokerTests.TestClass1.class);
		methodInvoker.setTargetMethod("intArguments");
		methodInvoker.setArguments(new Object[] {new Integer(5), new Integer(10)});
		methodInvoker.prepare();
		methodInvoker.invoke();

		methodInvoker = new ArgumentConvertingMethodInvoker();
		methodInvoker.setTargetClass(MethodInvokerTests.TestClass1.class);
		methodInvoker.setTargetMethod("intArguments");
		methodInvoker.setArguments(new Object[] {"5", "10"});
		methodInvoker.prepare();
		methodInvoker.invoke();
	}

}
