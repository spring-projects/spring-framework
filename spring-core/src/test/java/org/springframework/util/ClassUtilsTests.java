/*
 * Copyright 2002-2024 the original author or authors.
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

import java.io.Externalizable;
import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import a.ClassHavingNestedClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.tests.sample.objects.DerivedTestObject;
import org.springframework.tests.sample.objects.ITestInterface;
import org.springframework.tests.sample.objects.ITestObject;
import org.springframework.tests.sample.objects.TestObject;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ClassUtils}.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rick Evans
 * @author Sam Brannen
 */
class ClassUtilsTests {

	private final ClassLoader classLoader = getClass().getClassLoader();


	@Test
	void isPresent() {
		assertThat(ClassUtils.isPresent("java.lang.String", classLoader)).isTrue();
		assertThat(ClassUtils.isPresent("java.lang.MySpecialString", classLoader)).isFalse();
	}

	@Test
	void forName() throws ClassNotFoundException {
		assertThat(ClassUtils.forName("java.lang.String", classLoader)).isEqualTo(String.class);
		assertThat(ClassUtils.forName("java.lang.String[]", classLoader)).isEqualTo(String[].class);
		assertThat(ClassUtils.forName(String[].class.getName(), classLoader)).isEqualTo(String[].class);
		assertThat(ClassUtils.forName(String[][].class.getName(), classLoader)).isEqualTo(String[][].class);
		assertThat(ClassUtils.forName(String[][][].class.getName(), classLoader)).isEqualTo(String[][][].class);
		assertThat(ClassUtils.forName("org.springframework.tests.sample.objects.TestObject", classLoader)).isEqualTo(TestObject.class);
		assertThat(ClassUtils.forName("org.springframework.tests.sample.objects.TestObject[]", classLoader)).isEqualTo(TestObject[].class);
		assertThat(ClassUtils.forName(TestObject[].class.getName(), classLoader)).isEqualTo(TestObject[].class);
		assertThat(ClassUtils.forName("org.springframework.tests.sample.objects.TestObject[][]", classLoader)).isEqualTo(TestObject[][].class);
		assertThat(ClassUtils.forName(TestObject[][].class.getName(), classLoader)).isEqualTo(TestObject[][].class);
		assertThat(ClassUtils.forName("[[[S", classLoader)).isEqualTo(short[][][].class);
	}

	@Test
	void forNameWithNestedType() throws ClassNotFoundException {
		assertThat(ClassUtils.forName("org.springframework.util.ClassUtilsTests$NestedClass", classLoader)).isEqualTo(NestedClass.class);
		assertThat(ClassUtils.forName("org.springframework.util.ClassUtilsTests.NestedClass", classLoader)).isEqualTo(NestedClass.class);

		// Precondition: package name must have length == 1.
		assertThat(ClassHavingNestedClass.class.getPackageName().length()).isEqualTo(1);
		assertThat(ClassUtils.forName("a.ClassHavingNestedClass$NestedClass", classLoader)).isEqualTo(ClassHavingNestedClass.NestedClass.class);
		assertThat(ClassUtils.forName("a.ClassHavingNestedClass.NestedClass", classLoader)).isEqualTo(ClassHavingNestedClass.NestedClass.class);
	}

	@Test
	void forNameWithPrimitiveClasses() throws ClassNotFoundException {
		assertThat(ClassUtils.forName("boolean", classLoader)).isEqualTo(boolean.class);
		assertThat(ClassUtils.forName("byte", classLoader)).isEqualTo(byte.class);
		assertThat(ClassUtils.forName("char", classLoader)).isEqualTo(char.class);
		assertThat(ClassUtils.forName("short", classLoader)).isEqualTo(short.class);
		assertThat(ClassUtils.forName("int", classLoader)).isEqualTo(int.class);
		assertThat(ClassUtils.forName("long", classLoader)).isEqualTo(long.class);
		assertThat(ClassUtils.forName("float", classLoader)).isEqualTo(float.class);
		assertThat(ClassUtils.forName("double", classLoader)).isEqualTo(double.class);
		assertThat(ClassUtils.forName("void", classLoader)).isEqualTo(void.class);
	}

	@Test
	void forNameWithPrimitiveArrays() throws ClassNotFoundException {
		assertThat(ClassUtils.forName("boolean[]", classLoader)).isEqualTo(boolean[].class);
		assertThat(ClassUtils.forName("byte[]", classLoader)).isEqualTo(byte[].class);
		assertThat(ClassUtils.forName("char[]", classLoader)).isEqualTo(char[].class);
		assertThat(ClassUtils.forName("short[]", classLoader)).isEqualTo(short[].class);
		assertThat(ClassUtils.forName("int[]", classLoader)).isEqualTo(int[].class);
		assertThat(ClassUtils.forName("long[]", classLoader)).isEqualTo(long[].class);
		assertThat(ClassUtils.forName("float[]", classLoader)).isEqualTo(float[].class);
		assertThat(ClassUtils.forName("double[]", classLoader)).isEqualTo(double[].class);
	}

	@Test
	void forNameWithPrimitiveArraysInternalName() throws ClassNotFoundException {
		assertThat(ClassUtils.forName(boolean[].class.getName(), classLoader)).isEqualTo(boolean[].class);
		assertThat(ClassUtils.forName(byte[].class.getName(), classLoader)).isEqualTo(byte[].class);
		assertThat(ClassUtils.forName(char[].class.getName(), classLoader)).isEqualTo(char[].class);
		assertThat(ClassUtils.forName(short[].class.getName(), classLoader)).isEqualTo(short[].class);
		assertThat(ClassUtils.forName(int[].class.getName(), classLoader)).isEqualTo(int[].class);
		assertThat(ClassUtils.forName(long[].class.getName(), classLoader)).isEqualTo(long[].class);
		assertThat(ClassUtils.forName(float[].class.getName(), classLoader)).isEqualTo(float[].class);
		assertThat(ClassUtils.forName(double[].class.getName(), classLoader)).isEqualTo(double[].class);
	}

	@Test
	void isCacheSafe() {
		ClassLoader childLoader1 = new ClassLoader(classLoader) {};
		ClassLoader childLoader2 = new ClassLoader(classLoader) {};
		ClassLoader childLoader3 = new ClassLoader(classLoader) {
			@Override
			public Class<?> loadClass(String name) throws ClassNotFoundException {
				return childLoader1.loadClass(name);
			}
		};
		Class<?> composite = ClassUtils.createCompositeInterface(
				new Class<?>[] {Serializable.class, Externalizable.class}, childLoader1);

		assertThat(ClassUtils.isCacheSafe(String.class, null)).isTrue();
		assertThat(ClassUtils.isCacheSafe(String.class, classLoader)).isTrue();
		assertThat(ClassUtils.isCacheSafe(String.class, childLoader1)).isTrue();
		assertThat(ClassUtils.isCacheSafe(String.class, childLoader2)).isTrue();
		assertThat(ClassUtils.isCacheSafe(String.class, childLoader3)).isTrue();
		assertThat(ClassUtils.isCacheSafe(NestedClass.class, null)).isFalse();
		assertThat(ClassUtils.isCacheSafe(NestedClass.class, classLoader)).isTrue();
		assertThat(ClassUtils.isCacheSafe(NestedClass.class, childLoader1)).isTrue();
		assertThat(ClassUtils.isCacheSafe(NestedClass.class, childLoader2)).isTrue();
		assertThat(ClassUtils.isCacheSafe(NestedClass.class, childLoader3)).isTrue();
		assertThat(ClassUtils.isCacheSafe(composite, null)).isFalse();
		assertThat(ClassUtils.isCacheSafe(composite, classLoader)).isFalse();
		assertThat(ClassUtils.isCacheSafe(composite, childLoader1)).isTrue();
		assertThat(ClassUtils.isCacheSafe(composite, childLoader2)).isFalse();
		assertThat(ClassUtils.isCacheSafe(composite, childLoader3)).isTrue();
	}

	@ParameterizedTest(name = "''{0}'' -> {1}")
	@CsvSource(textBlock = """
		boolean, boolean
		byte, byte
		char, char
		short, short
		int, int
		long, long
		float, float
		double, double
		[Z, boolean[]
		[B, byte[]
		[C, char[]
		[S, short[]
		[I, int[]
		[J, long[]
		[F, float[]
		[D, double[]
		""")
	void resolvePrimitiveClassName(String input, Class<?> output) {
		assertThat(ClassUtils.resolvePrimitiveClassName(input)).isEqualTo(output);
	}

	@Test
	void getShortName() {
		String className = ClassUtils.getShortName(getClass());
		assertThat(className).as("Class name did not match").isEqualTo("ClassUtilsTests");
	}

	@Test
	void getShortNameForObjectArrayClass() {
		String className = ClassUtils.getShortName(Object[].class);
		assertThat(className).as("Class name did not match").isEqualTo("Object[]");
	}

	@Test
	void getShortNameForMultiDimensionalObjectArrayClass() {
		String className = ClassUtils.getShortName(Object[][].class);
		assertThat(className).as("Class name did not match").isEqualTo("Object[][]");
	}

	@Test
	void getShortNameForPrimitiveArrayClass() {
		String className = ClassUtils.getShortName(byte[].class);
		assertThat(className).as("Class name did not match").isEqualTo("byte[]");
	}

	@Test
	void getShortNameForMultiDimensionalPrimitiveArrayClass() {
		String className = ClassUtils.getShortName(byte[][][].class);
		assertThat(className).as("Class name did not match").isEqualTo("byte[][][]");
	}

	@Test
	void getShortNameForNestedClass() {
		String className = ClassUtils.getShortName(NestedClass.class);
		assertThat(className).as("Class name did not match").isEqualTo("ClassUtilsTests.NestedClass");
	}

	@Test
	void getShortNameAsProperty() {
		String shortName = ClassUtils.getShortNameAsProperty(this.getClass());
		assertThat(shortName).as("Class name did not match").isEqualTo("classUtilsTests");
	}

	@Test
	void getClassFileName() {
		assertThat(ClassUtils.getClassFileName(String.class)).isEqualTo("String.class");
		assertThat(ClassUtils.getClassFileName(getClass())).isEqualTo("ClassUtilsTests.class");
	}

	@Test
	void getPackageName() {
		assertThat(ClassUtils.getPackageName(String.class)).isEqualTo("java.lang");
		assertThat(ClassUtils.getPackageName(getClass())).isEqualTo(getClass().getPackage().getName());
	}

	@Test
	void getQualifiedName() {
		String className = ClassUtils.getQualifiedName(getClass());
		assertThat(className).as("Class name did not match").isEqualTo("org.springframework.util.ClassUtilsTests");
	}

	@Test
	void getQualifiedNameForObjectArrayClass() {
		String className = ClassUtils.getQualifiedName(Object[].class);
		assertThat(className).as("Class name did not match").isEqualTo("java.lang.Object[]");
	}

	@Test
	void getQualifiedNameForMultiDimensionalObjectArrayClass() {
		String className = ClassUtils.getQualifiedName(Object[][].class);
		assertThat(className).as("Class name did not match").isEqualTo("java.lang.Object[][]");
	}

	@Test
	void getQualifiedNameForPrimitiveArrayClass() {
		String className = ClassUtils.getQualifiedName(byte[].class);
		assertThat(className).as("Class name did not match").isEqualTo("byte[]");
	}

	@Test
	void getQualifiedNameForMultiDimensionalPrimitiveArrayClass() {
		String className = ClassUtils.getQualifiedName(byte[][].class);
		assertThat(className).as("Class name did not match").isEqualTo("byte[][]");
	}

	@Test
	void hasMethod() {
		assertThat(ClassUtils.hasMethod(Collection.class, "size")).isTrue();
		assertThat(ClassUtils.hasMethod(Collection.class, "remove", Object.class)).isTrue();
		assertThat(ClassUtils.hasMethod(Collection.class, "remove")).isFalse();
		assertThat(ClassUtils.hasMethod(Collection.class, "someOtherMethod")).isFalse();
	}

	@Test
	void getMethodIfAvailable() {
		Method method = ClassUtils.getMethodIfAvailable(Collection.class, "size");
		assertThat(method).isNotNull();
		assertThat(method.getName()).isEqualTo("size");

		method = ClassUtils.getMethodIfAvailable(Collection.class, "remove", Object.class);
		assertThat(method).isNotNull();
		assertThat(method.getName()).isEqualTo("remove");

		assertThat(ClassUtils.getMethodIfAvailable(Collection.class, "remove")).isNull();
		assertThat(ClassUtils.getMethodIfAvailable(Collection.class, "someOtherMethod")).isNull();
	}

	@Test
	void getMethodCountForName() {
		assertThat(ClassUtils.getMethodCountForName(OverloadedMethodsClass.class, "print")).as("Verifying number of overloaded 'print' methods for OverloadedMethodsClass.").isEqualTo(2);
		assertThat(ClassUtils.getMethodCountForName(SubOverloadedMethodsClass.class, "print")).as("Verifying number of overloaded 'print' methods for SubOverloadedMethodsClass.").isEqualTo(4);
	}

	@Test
	void countOverloadedMethods() {
		assertThat(ClassUtils.hasAtLeastOneMethodWithName(TestObject.class, "foobar")).isFalse();
		// no args
		assertThat(ClassUtils.hasAtLeastOneMethodWithName(TestObject.class, "hashCode")).isTrue();
		// matches although it takes an arg
		assertThat(ClassUtils.hasAtLeastOneMethodWithName(TestObject.class, "setAge")).isTrue();
	}

	@Test
	void isAssignable() {
		assertThat(ClassUtils.isAssignable(Object.class, Object.class)).isTrue();
		assertThat(ClassUtils.isAssignable(String.class, String.class)).isTrue();
		assertThat(ClassUtils.isAssignable(Object.class, String.class)).isTrue();
		assertThat(ClassUtils.isAssignable(Object.class, Integer.class)).isTrue();
		assertThat(ClassUtils.isAssignable(Number.class, Integer.class)).isTrue();
		assertThat(ClassUtils.isAssignable(Number.class, int.class)).isTrue();
		assertThat(ClassUtils.isAssignable(Integer.class, int.class)).isTrue();
		assertThat(ClassUtils.isAssignable(int.class, Integer.class)).isTrue();
		assertThat(ClassUtils.isAssignable(String.class, Object.class)).isFalse();
		assertThat(ClassUtils.isAssignable(Integer.class, Number.class)).isFalse();
		assertThat(ClassUtils.isAssignable(Integer.class, double.class)).isFalse();
		assertThat(ClassUtils.isAssignable(double.class, Integer.class)).isFalse();
	}

	@Test
	void classPackageAsResourcePath() {
		String result = ClassUtils.classPackageAsResourcePath(Proxy.class);
		assertThat(result).isEqualTo("java/lang/reflect");
	}

	@Test
	void addResourcePathToPackagePath() {
		String result = "java/lang/reflect/xyzabc.xml";
		assertThat(ClassUtils.addResourcePathToPackagePath(Proxy.class, "xyzabc.xml")).isEqualTo(result);
		assertThat(ClassUtils.addResourcePathToPackagePath(Proxy.class, "/xyzabc.xml")).isEqualTo(result);

		assertThat(ClassUtils.addResourcePathToPackagePath(Proxy.class, "a/b/c/d.xml")).isEqualTo("java/lang/reflect/a/b/c/d.xml");
	}

	@Test
	void getAllInterfaces() {
		DerivedTestObject testBean = new DerivedTestObject();
		List<Class<?>> ifcs = Arrays.asList(ClassUtils.getAllInterfaces(testBean));
		assertThat(ifcs).as("Correct number of interfaces").hasSize(4);
		assertThat(ifcs.contains(Serializable.class)).as("Contains Serializable").isTrue();
		assertThat(ifcs.contains(ITestObject.class)).as("Contains ITestBean").isTrue();
		assertThat(ifcs.contains(ITestInterface.class)).as("Contains IOther").isTrue();
	}

	@Test
	void classNamesToString() {
		List<Class<?>> ifcs = new ArrayList<>();
		ifcs.add(Serializable.class);
		ifcs.add(Runnable.class);
		assertThat(ifcs.toString()).isEqualTo("[interface java.io.Serializable, interface java.lang.Runnable]");
		assertThat(ClassUtils.classNamesToString(ifcs)).isEqualTo("[java.io.Serializable, java.lang.Runnable]");

		List<Class<?>> classes = new ArrayList<>();
		classes.add(ArrayList.class);
		classes.add(Integer.class);
		assertThat(classes.toString()).isEqualTo("[class java.util.ArrayList, class java.lang.Integer]");
		assertThat(ClassUtils.classNamesToString(classes)).isEqualTo("[java.util.ArrayList, java.lang.Integer]");

		assertThat(Collections.singletonList(List.class).toString()).isEqualTo("[interface java.util.List]");
		assertThat(ClassUtils.classNamesToString(List.class)).isEqualTo("[java.util.List]");

		assertThat(Collections.EMPTY_LIST.toString()).isEqualTo("[]");
		assertThat(ClassUtils.classNamesToString(Collections.emptyList())).isEqualTo("[]");
	}

	@Test
	void determineCommonAncestor() {
		assertThat(ClassUtils.determineCommonAncestor(Integer.class, Number.class)).isEqualTo(Number.class);
		assertThat(ClassUtils.determineCommonAncestor(Number.class, Integer.class)).isEqualTo(Number.class);
		assertThat(ClassUtils.determineCommonAncestor(Number.class, null)).isEqualTo(Number.class);
		assertThat(ClassUtils.determineCommonAncestor(null, Integer.class)).isEqualTo(Integer.class);
		assertThat(ClassUtils.determineCommonAncestor(Integer.class, Integer.class)).isEqualTo(Integer.class);

		assertThat(ClassUtils.determineCommonAncestor(Integer.class, Float.class)).isEqualTo(Number.class);
		assertThat(ClassUtils.determineCommonAncestor(Float.class, Integer.class)).isEqualTo(Number.class);
		assertThat(ClassUtils.determineCommonAncestor(Integer.class, String.class)).isNull();
		assertThat(ClassUtils.determineCommonAncestor(String.class, Integer.class)).isNull();

		assertThat(ClassUtils.determineCommonAncestor(List.class, Collection.class)).isEqualTo(Collection.class);
		assertThat(ClassUtils.determineCommonAncestor(Collection.class, List.class)).isEqualTo(Collection.class);
		assertThat(ClassUtils.determineCommonAncestor(Collection.class, null)).isEqualTo(Collection.class);
		assertThat(ClassUtils.determineCommonAncestor(null, List.class)).isEqualTo(List.class);
		assertThat(ClassUtils.determineCommonAncestor(List.class, List.class)).isEqualTo(List.class);

		assertThat(ClassUtils.determineCommonAncestor(List.class, Set.class)).isNull();
		assertThat(ClassUtils.determineCommonAncestor(Set.class, List.class)).isNull();
		assertThat(ClassUtils.determineCommonAncestor(List.class, Runnable.class)).isNull();
		assertThat(ClassUtils.determineCommonAncestor(Runnable.class, List.class)).isNull();

		assertThat(ClassUtils.determineCommonAncestor(List.class, ArrayList.class)).isEqualTo(List.class);
		assertThat(ClassUtils.determineCommonAncestor(ArrayList.class, List.class)).isEqualTo(List.class);
		assertThat(ClassUtils.determineCommonAncestor(List.class, String.class)).isNull();
		assertThat(ClassUtils.determineCommonAncestor(String.class, List.class)).isNull();
	}

	@Test
	void getMostSpecificMethod() throws NoSuchMethodException {
		Method defaultPrintMethod = ClassUtils.getMethod(MethodsInterface.class, "defaultPrint");
		assertThat(ClassUtils.getMostSpecificMethod(defaultPrintMethod, MethodsInterfaceImplementation.class))
				.isEqualTo(defaultPrintMethod);
		assertThat(ClassUtils.getMostSpecificMethod(defaultPrintMethod, SubMethodsInterfaceImplementation.class))
				.isEqualTo(defaultPrintMethod);

		Method printMethod = ClassUtils.getMethod(MethodsInterface.class, "print", String.class);
		assertThat(ClassUtils.getMostSpecificMethod(printMethod, MethodsInterfaceImplementation.class))
				.isNotEqualTo(printMethod);
		assertThat(ClassUtils.getMostSpecificMethod(printMethod, MethodsInterfaceImplementation.class))
				.isEqualTo(ClassUtils.getMethod(MethodsInterfaceImplementation.class, "print", String.class));
		assertThat(ClassUtils.getMostSpecificMethod(printMethod, SubMethodsInterfaceImplementation.class))
				.isEqualTo(ClassUtils.getMethod(MethodsInterfaceImplementation.class, "print", String.class));

		Method protectedPrintMethod = MethodsInterfaceImplementation.class.getDeclaredMethod("protectedPrint");
		assertThat(ClassUtils.getMostSpecificMethod(protectedPrintMethod, MethodsInterfaceImplementation.class))
				.isEqualTo(protectedPrintMethod);
		assertThat(ClassUtils.getMostSpecificMethod(protectedPrintMethod, SubMethodsInterfaceImplementation.class))
				.isEqualTo(SubMethodsInterfaceImplementation.class.getDeclaredMethod("protectedPrint"));

		Method packageAccessiblePrintMethod = MethodsInterfaceImplementation.class.getDeclaredMethod("packageAccessiblePrint");
		assertThat(ClassUtils.getMostSpecificMethod(packageAccessiblePrintMethod, MethodsInterfaceImplementation.class))
				.isEqualTo(packageAccessiblePrintMethod);
		assertThat(ClassUtils.getMostSpecificMethod(packageAccessiblePrintMethod, SubMethodsInterfaceImplementation.class))
				.isEqualTo(ClassUtils.getMethod(SubMethodsInterfaceImplementation.class, "packageAccessiblePrint"));
	}

	@ParameterizedTest
	@WrapperTypes
	void isPrimitiveWrapper(Class<?> type) {
		assertThat(ClassUtils.isPrimitiveWrapper(type)).isTrue();
	}

	@ParameterizedTest
	@PrimitiveTypes
	void isPrimitiveOrWrapperWithPrimitive(Class<?> type) {
		assertThat(ClassUtils.isPrimitiveOrWrapper(type)).isTrue();
	}

	@ParameterizedTest
	@WrapperTypes
	void isPrimitiveOrWrapperWithWrapper(Class<?> type) {
		assertThat(ClassUtils.isPrimitiveOrWrapper(type)).isTrue();
	}

	@Test
	void isLambda() {
		assertIsLambda(ClassUtilsTests.staticLambdaExpression);
		assertIsLambda(ClassUtilsTests::staticStringFactory);

		assertIsLambda(this.instanceLambdaExpression);
		assertIsLambda(this::instanceStringFactory);
	}

	@Test
	@SuppressWarnings("Convert2Lambda")
	void isNotLambda() {
		assertIsNotLambda(new EnigmaSupplier());

		assertIsNotLambda(new Supplier<>() {
			@Override
			public String get() {
				return "anonymous inner class";
			}
		});

		assertIsNotLambda(new Fake$$LambdaSupplier());
	}


	@Nested
	class GetStaticMethodTests {

		@BeforeEach
		void clearStatics() {
			NestedClass.noArgCalled = false;
			NestedClass.argCalled = false;
			NestedClass.overloadedCalled = false;
		}

		@Test
		void noArgsStaticMethod() throws IllegalAccessException, InvocationTargetException {
			Method method = ClassUtils.getStaticMethod(NestedClass.class, "staticMethod");
			method.invoke(null, (Object[]) null);
			assertThat(NestedClass.noArgCalled).as("no argument method was not invoked.").isTrue();
		}

		@Test
		void argsStaticMethod() throws IllegalAccessException, InvocationTargetException {
			Method method = ClassUtils.getStaticMethod(NestedClass.class, "argStaticMethod", String.class);
			method.invoke(null, "test");
			assertThat(NestedClass.argCalled).as("argument method was not invoked.").isTrue();
		}

		@Test
		void overloadedStaticMethod() throws IllegalAccessException, InvocationTargetException {
			Method method = ClassUtils.getStaticMethod(NestedClass.class, "staticMethod", String.class);
			method.invoke(null, "test");
			assertThat(NestedClass.overloadedCalled).as("argument method was not invoked.").isTrue();
		}

	}


	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@ValueSource(classes = { Boolean.class, Character.class, Byte.class, Short.class,
		Integer.class, Long.class, Float.class, Double.class, Void.class })
	@interface WrapperTypes {
	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@ValueSource(classes = { boolean.class, char.class, byte.class, short.class,
		int.class, long.class, float.class, double.class, void.class })
	@interface PrimitiveTypes {
	}

	public static class NestedClass {

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

	private static void assertIsLambda(Supplier<String> supplier) {
		assertThat(ClassUtils.isLambdaClass(supplier.getClass())).isTrue();
	}

	private static void assertIsNotLambda(Supplier<String> supplier) {
		assertThat(ClassUtils.isLambdaClass(supplier.getClass())).isFalse();
	}

	private static final Supplier<String> staticLambdaExpression = () -> "static lambda expression";

	private final Supplier<String> instanceLambdaExpression = () -> "instance lambda expressions";

	private static String staticStringFactory() {
		return "static string factory";
	}

	private String instanceStringFactory() {
		return "instance string factory";
	}

	private static class EnigmaSupplier implements Supplier<String> {
		@Override
		public String get() {
			return "enigma";
		}
	}

	private static class Fake$$LambdaSupplier implements Supplier<String> {
		@Override
		public String get() {
			return "fake lambda";
		}
	}

	@SuppressWarnings("unused")
	private interface MethodsInterface {

		default void defaultPrint() {

		}

		void print(String messages);
	}

	@SuppressWarnings("unused")
	private class MethodsInterfaceImplementation implements MethodsInterface {

		@Override
		public void print(String message) {

		}

		protected void protectedPrint() {

		}

		void packageAccessiblePrint() {

		}
	}

	@SuppressWarnings("unused")
	private class SubMethodsInterfaceImplementation extends MethodsInterfaceImplementation {

		@Override
		protected void protectedPrint() {

		}

		@Override
		public void packageAccessiblePrint() {

		}

	}

}
