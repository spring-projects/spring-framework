/*
 * Copyright 2002-2013 the original author or authors.
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

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.tests.sample.objects.DerivedTestObject;
import org.springframework.tests.sample.objects.ITestInterface;
import org.springframework.tests.sample.objects.ITestObject;
import org.springframework.tests.sample.objects.TestObject;

/**
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rick Evans
 */
public class ClassUtilsTests extends TestCase {

	private ClassLoader classLoader = getClass().getClassLoader();

	@Override
	public void setUp() {
		InnerClass.noArgCalled = false;
		InnerClass.argCalled = false;
		InnerClass.overloadedCalled = false;
	}

	public void testIsPresent() throws Exception {
		assertTrue(ClassUtils.isPresent("java.lang.String", classLoader));
		assertFalse(ClassUtils.isPresent("java.lang.MySpecialString", classLoader));
	}

	public void testForName() throws ClassNotFoundException {
		assertEquals(String.class, ClassUtils.forName("java.lang.String", classLoader));
		assertEquals(String[].class, ClassUtils.forName("java.lang.String[]", classLoader));
		assertEquals(String[].class, ClassUtils.forName(String[].class.getName(), classLoader));
		assertEquals(String[][].class, ClassUtils.forName(String[][].class.getName(), classLoader));
		assertEquals(String[][][].class, ClassUtils.forName(String[][][].class.getName(), classLoader));
		assertEquals(TestObject.class, ClassUtils.forName("org.springframework.tests.sample.objects.TestObject", classLoader));
		assertEquals(TestObject[].class, ClassUtils.forName("org.springframework.tests.sample.objects.TestObject[]", classLoader));
		assertEquals(TestObject[].class, ClassUtils.forName(TestObject[].class.getName(), classLoader));
		assertEquals(TestObject[][].class, ClassUtils.forName("org.springframework.tests.sample.objects.TestObject[][]", classLoader));
		assertEquals(TestObject[][].class, ClassUtils.forName(TestObject[][].class.getName(), classLoader));
		assertEquals(short[][][].class, ClassUtils.forName("[[[S", classLoader));
	}

	public void testForNameWithPrimitiveClasses() throws ClassNotFoundException {
		assertEquals(boolean.class, ClassUtils.forName("boolean", classLoader));
		assertEquals(byte.class, ClassUtils.forName("byte", classLoader));
		assertEquals(char.class, ClassUtils.forName("char", classLoader));
		assertEquals(short.class, ClassUtils.forName("short", classLoader));
		assertEquals(int.class, ClassUtils.forName("int", classLoader));
		assertEquals(long.class, ClassUtils.forName("long", classLoader));
		assertEquals(float.class, ClassUtils.forName("float", classLoader));
		assertEquals(double.class, ClassUtils.forName("double", classLoader));
		assertEquals(void.class, ClassUtils.forName("void", classLoader));
	}

	public void testForNameWithPrimitiveArrays() throws ClassNotFoundException {
		assertEquals(boolean[].class, ClassUtils.forName("boolean[]", classLoader));
		assertEquals(byte[].class, ClassUtils.forName("byte[]", classLoader));
		assertEquals(char[].class, ClassUtils.forName("char[]", classLoader));
		assertEquals(short[].class, ClassUtils.forName("short[]", classLoader));
		assertEquals(int[].class, ClassUtils.forName("int[]", classLoader));
		assertEquals(long[].class, ClassUtils.forName("long[]", classLoader));
		assertEquals(float[].class, ClassUtils.forName("float[]", classLoader));
		assertEquals(double[].class, ClassUtils.forName("double[]", classLoader));
	}

	public void testForNameWithPrimitiveArraysInternalName() throws ClassNotFoundException {
		assertEquals(boolean[].class, ClassUtils.forName(boolean[].class.getName(), classLoader));
		assertEquals(byte[].class, ClassUtils.forName(byte[].class.getName(), classLoader));
		assertEquals(char[].class, ClassUtils.forName(char[].class.getName(), classLoader));
		assertEquals(short[].class, ClassUtils.forName(short[].class.getName(), classLoader));
		assertEquals(int[].class, ClassUtils.forName(int[].class.getName(), classLoader));
		assertEquals(long[].class, ClassUtils.forName(long[].class.getName(), classLoader));
		assertEquals(float[].class, ClassUtils.forName(float[].class.getName(), classLoader));
		assertEquals(double[].class, ClassUtils.forName(double[].class.getName(), classLoader));
	}

	public void testGetShortName() {
		String className = ClassUtils.getShortName(getClass());
		assertEquals("Class name did not match", "ClassUtilsTests", className);
	}

	public void testGetShortNameForObjectArrayClass() {
		String className = ClassUtils.getShortName(Object[].class);
		assertEquals("Class name did not match", "Object[]", className);
	}

	public void testGetShortNameForMultiDimensionalObjectArrayClass() {
		String className = ClassUtils.getShortName(Object[][].class);
		assertEquals("Class name did not match", "Object[][]", className);
	}

	public void testGetShortNameForPrimitiveArrayClass() {
		String className = ClassUtils.getShortName(byte[].class);
		assertEquals("Class name did not match", "byte[]", className);
	}

	public void testGetShortNameForMultiDimensionalPrimitiveArrayClass() {
		String className = ClassUtils.getShortName(byte[][][].class);
		assertEquals("Class name did not match", "byte[][][]", className);
	}

	public void testGetShortNameForInnerClass() {
		String className = ClassUtils.getShortName(InnerClass.class);
		assertEquals("Class name did not match", "ClassUtilsTests.InnerClass", className);
	}

	public void testGetShortNameAsProperty() {
		String shortName = ClassUtils.getShortNameAsProperty(this.getClass());
		assertEquals("Class name did not match", "classUtilsTests", shortName);
	}

	public void testGetClassFileName() {
		assertEquals("String.class", ClassUtils.getClassFileName(String.class));
		assertEquals("ClassUtilsTests.class", ClassUtils.getClassFileName(getClass()));
	}

	public void testGetPackageName() {
		assertEquals("java.lang", ClassUtils.getPackageName(String.class));
		assertEquals(getClass().getPackage().getName(), ClassUtils.getPackageName(getClass()));
	}

	public void testGetQualifiedName() {
		String className = ClassUtils.getQualifiedName(getClass());
		assertEquals("Class name did not match", "org.springframework.util.ClassUtilsTests", className);
	}

	public void testGetQualifiedNameForObjectArrayClass() {
		String className = ClassUtils.getQualifiedName(Object[].class);
		assertEquals("Class name did not match", "java.lang.Object[]", className);
	}

	public void testGetQualifiedNameForMultiDimensionalObjectArrayClass() {
		String className = ClassUtils.getQualifiedName(Object[][].class);
		assertEquals("Class name did not match", "java.lang.Object[][]", className);
	}

	public void testGetQualifiedNameForPrimitiveArrayClass() {
		String className = ClassUtils.getQualifiedName(byte[].class);
		assertEquals("Class name did not match", "byte[]", className);
	}

	public void testGetQualifiedNameForMultiDimensionalPrimitiveArrayClass() {
		String className = ClassUtils.getQualifiedName(byte[][].class);
		assertEquals("Class name did not match", "byte[][]", className);
	}

	public void testHasMethod() throws Exception {
		assertTrue(ClassUtils.hasMethod(Collection.class, "size"));
		assertTrue(ClassUtils.hasMethod(Collection.class, "remove", Object.class));
		assertFalse(ClassUtils.hasMethod(Collection.class, "remove"));
		assertFalse(ClassUtils.hasMethod(Collection.class, "someOtherMethod"));
	}

	public void testGetMethodIfAvailable() throws Exception {
		Method method = ClassUtils.getMethodIfAvailable(Collection.class, "size");
		assertNotNull(method);
		assertEquals("size", method.getName());

		method = ClassUtils.getMethodIfAvailable(Collection.class, "remove", new Class[] {Object.class});
		assertNotNull(method);
		assertEquals("remove", method.getName());

		assertNull(ClassUtils.getMethodIfAvailable(Collection.class, "remove"));
		assertNull(ClassUtils.getMethodIfAvailable(Collection.class, "someOtherMethod"));
	}

	public void testGetMethodCountForName() {
		assertEquals("Verifying number of overloaded 'print' methods for OverloadedMethodsClass.", 2,
				ClassUtils.getMethodCountForName(OverloadedMethodsClass.class, "print"));
		assertEquals("Verifying number of overloaded 'print' methods for SubOverloadedMethodsClass.", 4,
				ClassUtils.getMethodCountForName(SubOverloadedMethodsClass.class, "print"));
	}

	public void testCountOverloadedMethods() {
		assertFalse(ClassUtils.hasAtLeastOneMethodWithName(TestObject.class, "foobar"));
		// no args
		assertTrue(ClassUtils.hasAtLeastOneMethodWithName(TestObject.class, "hashCode"));
		// matches although it takes an arg
		assertTrue(ClassUtils.hasAtLeastOneMethodWithName(TestObject.class, "setAge"));
	}

	public void testNoArgsStaticMethod() throws IllegalAccessException, InvocationTargetException {
		Method method = ClassUtils.getStaticMethod(InnerClass.class, "staticMethod", (Class[]) null);
		method.invoke(null, (Object[]) null);
		assertTrue("no argument method was not invoked.",
				InnerClass.noArgCalled);
	}

	public void testArgsStaticMethod() throws IllegalAccessException, InvocationTargetException {
		Method method = ClassUtils.getStaticMethod(InnerClass.class, "argStaticMethod",
				new Class[] {String.class});
		method.invoke(null, new Object[] {"test"});
		assertTrue("argument method was not invoked.", InnerClass.argCalled);
	}

	public void testOverloadedStaticMethod() throws IllegalAccessException, InvocationTargetException {
		Method method = ClassUtils.getStaticMethod(InnerClass.class, "staticMethod",
				new Class[] {String.class});
		method.invoke(null, new Object[] {"test"});
		assertTrue("argument method was not invoked.",
				InnerClass.overloadedCalled);
	}

	public void testIsAssignable() {
		assertTrue(ClassUtils.isAssignable(Object.class, Object.class));
		assertTrue(ClassUtils.isAssignable(String.class, String.class));
		assertTrue(ClassUtils.isAssignable(Object.class, String.class));
		assertTrue(ClassUtils.isAssignable(Object.class, Integer.class));
		assertTrue(ClassUtils.isAssignable(Number.class, Integer.class));
		assertTrue(ClassUtils.isAssignable(Number.class, int.class));
		assertTrue(ClassUtils.isAssignable(Integer.class, int.class));
		assertTrue(ClassUtils.isAssignable(int.class, Integer.class));
		assertFalse(ClassUtils.isAssignable(String.class, Object.class));
		assertFalse(ClassUtils.isAssignable(Integer.class, Number.class));
		assertFalse(ClassUtils.isAssignable(Integer.class, double.class));
		assertFalse(ClassUtils.isAssignable(double.class, Integer.class));
	}

	public void testClassPackageAsResourcePath() {
		String result = ClassUtils.classPackageAsResourcePath(Proxy.class);
		assertTrue(result.equals("java/lang/reflect"));
	}

	public void testAddResourcePathToPackagePath() {
		String result = "java/lang/reflect/xyzabc.xml";
		assertEquals(result, ClassUtils.addResourcePathToPackagePath(Proxy.class, "xyzabc.xml"));
		assertEquals(result, ClassUtils.addResourcePathToPackagePath(Proxy.class, "/xyzabc.xml"));

		assertEquals("java/lang/reflect/a/b/c/d.xml",
				ClassUtils.addResourcePathToPackagePath(Proxy.class, "a/b/c/d.xml"));
	}

	public void testGetAllInterfaces() {
		DerivedTestObject testBean = new DerivedTestObject();
		List ifcs = Arrays.asList(ClassUtils.getAllInterfaces(testBean));
		assertEquals("Correct number of interfaces", 4, ifcs.size());
		assertTrue("Contains Serializable", ifcs.contains(Serializable.class));
		assertTrue("Contains ITestBean", ifcs.contains(ITestObject.class));
		assertTrue("Contains IOther", ifcs.contains(ITestInterface.class));
	}

	public void testClassNamesToString() {
		List ifcs = new LinkedList();
		ifcs.add(Serializable.class);
		ifcs.add(Runnable.class);
		assertEquals("[interface java.io.Serializable, interface java.lang.Runnable]", ifcs.toString());
		assertEquals("[java.io.Serializable, java.lang.Runnable]", ClassUtils.classNamesToString(ifcs));

		List classes = new LinkedList();
		classes.add(LinkedList.class);
		classes.add(Integer.class);
		assertEquals("[class java.util.LinkedList, class java.lang.Integer]", classes.toString());
		assertEquals("[java.util.LinkedList, java.lang.Integer]", ClassUtils.classNamesToString(classes));

		assertEquals("[interface java.util.List]", Collections.singletonList(List.class).toString());
		assertEquals("[java.util.List]", ClassUtils.classNamesToString(List.class));

		assertEquals("[]", Collections.EMPTY_LIST.toString());
		assertEquals("[]", ClassUtils.classNamesToString(Collections.EMPTY_LIST));
	}


	public static class InnerClass {

		static boolean noArgCalled;
		static boolean argCalled;
		static boolean overloadedCalled;

		public static void staticMethod() {
			noArgCalled = true;
		}

		public static void staticMethod(String anArg) {
			overloadedCalled = true;
		}

		public static void argStaticMethod(String anArg) {
			argCalled = true;
		}
	}

	@SuppressWarnings("unused")
	private static class OverloadedMethodsClass {

		public void print(String messages) {
			/* no-op */
		}

		public void print(String[] messages) {
			/* no-op */
		}
	}

	@SuppressWarnings("unused")
	private static class SubOverloadedMethodsClass extends OverloadedMethodsClass {

		public void print(String header, String[] messages) {
			/* no-op */
		}

		void print(String header, String[] messages, String footer) {
			/* no-op */
		}
	}

}
