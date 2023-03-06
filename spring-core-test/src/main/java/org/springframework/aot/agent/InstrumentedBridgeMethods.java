/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aot.agent;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;

/**
 * Instrumented version of JDK methods to be used by bytecode rewritten by the {@link RuntimeHintsAgent}.
 * <p>Methods implemented here follow a specific naming pattern "lowercase type name + bridged method name",
 * so that the agent can consistently rewrite calls to instrumented methods.
 * For example {@code Class#forName(String)} will be here names {@code classforName(String)}.
 *
 * @author Brian Clozel
 * @see InstrumentedMethod
 * @deprecated This class should only be used by the runtime-hints agent when instrumenting bytecode
 * and is not considered public API.
 */
@Deprecated
public abstract class InstrumentedBridgeMethods {

	private InstrumentedBridgeMethods() {

	}

	/*
	 * Bridge methods for java.lang.Class
	 */

	public static Class<?> classforName(String className) throws ClassNotFoundException {
		Class<?> result = null;
		try {
			result = Class.forName(className);
		}
		finally {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_FORNAME).withArguments(className).returnValue(result).build();
			RecordedInvocationsPublisher.publish(invocation);
		}
		return result;
	}

	public static Class<?> classforName(String className, boolean initialize, ClassLoader loader) throws ClassNotFoundException {
		Class<?> result = null;
		try {
			result = Class.forName(className, initialize, loader);
		}
		finally {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_FORNAME).withArguments(className, initialize, loader).returnValue(result).build();
			RecordedInvocationsPublisher.publish(invocation);
		}
		return result;
	}

	public static Constructor<?>[] classgetConstructors(Class<?> clazz) throws SecurityException {
		Constructor<?>[] result = null;
		try {
			result = clazz.getConstructors();
		}
		finally {
			RecordedInvocationsPublisher.publish(RecordedInvocation.of(InstrumentedMethod.CLASS_GETCONSTRUCTORS).onInstance(clazz).returnValue(result).build());
		}
		return result;
	}

	public static Constructor<?> classgetConstructor(Class<?> clazz, Class<?>[] parameterTypes) throws NoSuchMethodException {
		Constructor<?> result = null;
		try {
			result = clazz.getConstructor(parameterTypes);
		}
		finally {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETCONSTRUCTOR)
					.onInstance(clazz).withArgument(parameterTypes).returnValue(result).build();
			RecordedInvocationsPublisher.publish(invocation);
		}
		return result;
	}

	public static Constructor<?>[] classgetDeclaredConstructors(Class<?> clazz) throws SecurityException {
		Constructor<?>[] result = null;
		try {
			result = clazz.getDeclaredConstructors();
		}
		finally {
			RecordedInvocationsPublisher.publish(RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTORS).onInstance(clazz).returnValue(result).build());
		}
		return result;
	}

	public static Constructor<?> classgetDeclaredConstructor(Class<?> clazz, Class<?>[] parameterTypes) throws NoSuchMethodException {
		Constructor<?> result = null;
		try {
			result = clazz.getDeclaredConstructor(parameterTypes);
		}
		finally {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTOR)
					.onInstance(clazz).withArgument(parameterTypes).returnValue(result).build();
			RecordedInvocationsPublisher.publish(invocation);
		}
		return result;
	}

	public static Method[] classgetMethods(Class<?> clazz) throws SecurityException {
		Method[] result = null;
		try {
			result = clazz.getMethods();
		}
		finally {
			RecordedInvocationsPublisher.publish(RecordedInvocation.of(InstrumentedMethod.CLASS_GETMETHODS)
					.onInstance(clazz).returnValue(result).build());
		}
		return result;
	}

	public static Method classgetMethod(Class<?> clazz, String name, Class<?>... parameterTypes) throws NoSuchMethodException, SecurityException {
		Method result = null;
		try {
			result = clazz.getMethod(name, parameterTypes);
		}
		finally {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETMETHOD)
					.onInstance(clazz).withArguments(name, parameterTypes).returnValue(result).build();
			RecordedInvocationsPublisher.publish(invocation);
		}
		return result;
	}

	public static Method classgetDeclaredMethod(Class<?> clazz, String name, Class<?>... params)
			throws SecurityException, NoSuchMethodException {
		Method result = null;
		try {
			result = clazz.getDeclaredMethod(name, params);
		}
		finally {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDMETHOD)
					.onInstance(clazz).withArguments(name, params).returnValue(result).build();
			RecordedInvocationsPublisher.publish(invocation);
		}
		return result;
	}

	public static Method[] classgetDeclaredMethods(Class<?> clazz) {
		Method[] result = clazz.getDeclaredMethods();
		RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDMETHODS)
				.onInstance(clazz).returnValue(result).build();
		RecordedInvocationsPublisher.publish(invocation);
		return result;
	}

	public static Class<?>[] classgetDeclaredClasses(Class<?> clazz) {
		Class<?>[] result = clazz.getDeclaredClasses();
		RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDCLASSES)
				.onInstance(clazz).returnValue(result).build();
		RecordedInvocationsPublisher.publish(invocation);
		return result;
	}

	public static Class<?>[] classgetClasses(Class<?> clazz) {
		Class<?>[] result = clazz.getClasses();
		RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETCLASSES)
				.onInstance(clazz).returnValue(result).build();
		RecordedInvocationsPublisher.publish(invocation);
		return result;
	}

	public static Field[] classgetDeclaredFields(Class<?> clazz) {
		Field[] result = clazz.getDeclaredFields();
		RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDFIELDS)
				.onInstance(clazz).returnValue(result).build();
		RecordedInvocationsPublisher.publish(invocation);
		return result;
	}

	public static Field classgetDeclaredField(Class<?> clazz, String name) throws NoSuchFieldException {
		Field result = null;
		try {
			result = clazz.getDeclaredField(name);
		}
		finally {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDFIELD)
					.onInstance(clazz).withArgument(name).returnValue(result).build();
			RecordedInvocationsPublisher.publish(invocation);
		}
		return result;
	}

	public static Field[] classgetFields(Class<?> clazz) {
		Field[] result = clazz.getFields();
		RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETFIELDS)
				.onInstance(clazz).returnValue(result).build();
		RecordedInvocationsPublisher.publish(invocation);
		return result;
	}

	public static Field classgetField(Class<?> clazz, String name) throws NoSuchFieldException {
		Field result = null;
		try {
			result = clazz.getField(name);
		}
		finally {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETFIELD)
					.onInstance(clazz).withArgument(name).returnValue(result).build();
			RecordedInvocationsPublisher.publish(invocation);
		}
		return result;
	}

	@Nullable
	public static URL classgetResource(Class<?> clazz, String name) {
		URL result = clazz.getResource(name);
		RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETRESOURCE)
				.onInstance(clazz).withArgument(name).returnValue(result).build();
		RecordedInvocationsPublisher.publish(invocation);
		return result;
	}

	@Nullable
	public static InputStream classgetResourceAsStream(Class<?> clazz, String name) {
		InputStream result = clazz.getResourceAsStream(name);
		RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETRESOURCEASSTREAM)
				.onInstance(clazz).withArgument(name).returnValue(result).build();
		RecordedInvocationsPublisher.publish(invocation);
		return result;
	}

	/*
	 * Bridge methods for java.lang.ClassLoader
	 */

	public static Class<?> classloaderloadClass(ClassLoader classLoader, String name) throws ClassNotFoundException {
		Class<?> result = null;
		try {
			result = classLoader.loadClass(name);
		}
		finally {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASSLOADER_LOADCLASS)
					.onInstance(classLoader).withArgument(name).returnValue(result).build();
			RecordedInvocationsPublisher.publish(invocation);
		}
		return result;
	}

	@Nullable
	public static URL classloadergetResource(ClassLoader classLoader, String name) {
		URL result = classLoader.getResource(name);
		RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASSLOADER_GETRESOURCE)
				.onInstance(classLoader).withArgument(name).returnValue(result).build();
		RecordedInvocationsPublisher.publish(invocation);
		return result;
	}

	@Nullable
	public static InputStream classloadergetResourceAsStream(ClassLoader classLoader, String name) {
		InputStream result = classLoader.getResourceAsStream(name);
		RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASSLOADER_GETRESOURCEASSTREAM)
				.onInstance(classLoader).withArgument(name).returnValue(result).build();
		RecordedInvocationsPublisher.publish(invocation);
		return result;
	}

	public static Stream<URL> classloaderresources(ClassLoader classLoader, String name) {
		Stream<URL> result = classLoader.resources(name);
		RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASSLOADER_RESOURCES)
				.onInstance(classLoader).withArgument(name).returnValue(result).build();
		RecordedInvocationsPublisher.publish(invocation);
		return result;
	}

	public static Enumeration<URL> classloadergetResources(ClassLoader classLoader, String name) throws IOException {
		Enumeration<URL> result = null;
		try {
			result = classLoader.getResources(name);
		}
		finally {
			RecordedInvocationsPublisher.publish(RecordedInvocation.of(InstrumentedMethod.CLASSLOADER_GETRESOURCES)
					.onInstance(classLoader).withArgument(name).returnValue(result).build());
		}
		return result;
	}

	/*
	 * Bridge methods for java.lang.Constructor
	 */
	public static Object constructornewInstance(Constructor<?> constructor, Object... arguments) throws InvocationTargetException, InstantiationException, IllegalAccessException {
		Object result = null;
		boolean accessibilityChanged = false;
		RecordedInvocation.Builder builder = RecordedInvocation.of(InstrumentedMethod.CONSTRUCTOR_NEWINSTANCE)
				.onInstance(constructor).withArguments(arguments);
		try {
			if (!Modifier.isPublic(constructor.getModifiers()) ||
					!Modifier.isPublic(constructor.getDeclaringClass().getModifiers())) {
				constructor.setAccessible(true);
				accessibilityChanged = true;
			}
			result = constructor.newInstance(arguments);
		}
		finally {
			RecordedInvocationsPublisher.publish(builder.returnValue(result).build());
			if (accessibilityChanged) {
				constructor.setAccessible(false);
			}
		}
		return result;
	}

	/*
	 * Bridge methods for java.lang.reflect.Method
	 */

	public static Object methodinvoke(Method method, Object object, Object... arguments) throws InvocationTargetException, IllegalAccessException {
		Object result = null;
		boolean accessibilityChanged = false;
		try {
			if (!Modifier.isPublic(method.getModifiers())
				|| !Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
				method.setAccessible(true);
				accessibilityChanged = true;
			}
			result = method.invoke(object, arguments);
		}
		finally {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.METHOD_INVOKE)
					.onInstance(method).withArguments(object, arguments).returnValue(result).build();
			RecordedInvocationsPublisher.publish(invocation);
			if (accessibilityChanged) {
				method.setAccessible(false);
			}
		}
		return result;
	}

	/*
	 * Bridge methods for java.lang.reflect.Field
	 */

	public static Object fieldget(Field field, Object object) throws IllegalArgumentException, IllegalAccessException {
		Object result = null;
		boolean accessibilityChanged = false;
		try {
			if ((!Modifier.isPublic(field.getModifiers()) ||
					!Modifier.isPublic(field.getDeclaringClass().getModifiers())) && !field.canAccess(object)) {
				field.setAccessible(true);
				accessibilityChanged = true;
			}
			result = field.get(object);
		}
		finally {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.FIELD_GET)
					.onInstance(field).withArguments(object).returnValue(result).build();
			RecordedInvocationsPublisher.publish(invocation);
			if (accessibilityChanged) {
				field.setAccessible(false);
			}
		}
		return result;
	}

	public static void fieldset(Field field, Object object, Object value) throws IllegalArgumentException, IllegalAccessException {
		boolean accessibilityChanged = false;
		try {
			if ((!Modifier.isPublic(field.getModifiers()) ||
					!Modifier.isPublic(field.getDeclaringClass().getModifiers())) && !field.canAccess(object)) {
				field.setAccessible(true);
				accessibilityChanged = true;
			}
			field.set(object, value);
		}
		finally {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.FIELD_SET)
					.onInstance(field).withArguments(object, value).build();
			RecordedInvocationsPublisher.publish(invocation);
			if (accessibilityChanged) {
				field.setAccessible(false);
			}
		}
	}


	/*
	 * Bridge methods for java.lang.Module
	 */

	public static InputStream modulegetResourceAsStream(Module module, String name) throws IOException {
		InputStream result = module.getResourceAsStream(name);
		RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.MODULE_GETRESOURCEASSTREAM)
				.onInstance(module).withArgument(name).returnValue(result).build();
		RecordedInvocationsPublisher.publish(invocation);
		return result;
	}

	/*
	 * Bridge methods for java.util.ResourceBundle
	 */

	public static ResourceBundle resourcebundlegetBundle(String baseName) {
		RecordedInvocation.Builder builder = RecordedInvocation.of(InstrumentedMethod.RESOURCEBUNDLE_GETBUNDLE).withArgument(baseName);
		ResourceBundle result = null;
		try {
			result = ResourceBundle.getBundle(baseName);
		}
		finally {
			RecordedInvocationsPublisher.publish(builder.returnValue(result).build());
		}
		return result;
	}

	public static ResourceBundle resourcebundlegetBundle(String baseName, ResourceBundle.Control control) {
		RecordedInvocation.Builder builder = RecordedInvocation.of(InstrumentedMethod.RESOURCEBUNDLE_GETBUNDLE).withArguments(baseName, control);
		ResourceBundle result = null;
		try {
			result = ResourceBundle.getBundle(baseName, control);
		}
		finally {
			RecordedInvocationsPublisher.publish(builder.returnValue(result).build());
		}
		return result;
	}

	public static ResourceBundle resourcebundlegetBundle(String baseName, Locale locale) {
		RecordedInvocation.Builder builder = RecordedInvocation.of(InstrumentedMethod.RESOURCEBUNDLE_GETBUNDLE).withArguments(baseName, locale);
		ResourceBundle result = null;
		try {
			result = ResourceBundle.getBundle(baseName, locale);
		}
		finally {
			RecordedInvocationsPublisher.publish(builder.returnValue(result).build());
		}
		return result;
	}

	public static ResourceBundle resourcebundlegetBundle(String baseName, Module module) {
		RecordedInvocation.Builder builder = RecordedInvocation.of(InstrumentedMethod.RESOURCEBUNDLE_GETBUNDLE).withArguments(baseName, module);
		ResourceBundle result = null;
		try {
			result = ResourceBundle.getBundle(baseName, module);
		}
		finally {
			RecordedInvocationsPublisher.publish(builder.returnValue(result).build());
		}
		return result;
	}

	public static ResourceBundle resourcebundlegetBundle(String baseName, Locale targetLocale, Module module) {
		RecordedInvocation.Builder builder = RecordedInvocation.of(InstrumentedMethod.RESOURCEBUNDLE_GETBUNDLE).withArguments(baseName, targetLocale, module);
		ResourceBundle result = null;
		try {
			result = ResourceBundle.getBundle(baseName, targetLocale, module);
		}
		finally {
			RecordedInvocationsPublisher.publish(builder.returnValue(result).build());
		}
		return result;
	}

	public static ResourceBundle resourcebundlegetBundle( String baseName, Locale targetLocale,	ResourceBundle.Control control) {
		RecordedInvocation.Builder builder = RecordedInvocation.of(InstrumentedMethod.RESOURCEBUNDLE_GETBUNDLE).withArguments(baseName, targetLocale, control);
		ResourceBundle result = null;
		try {
			result = ResourceBundle.getBundle(baseName, targetLocale, control);
		}
		finally {
			RecordedInvocationsPublisher.publish(builder.returnValue(result).build());
		}
		return result;
	}

	public static ResourceBundle resourcebundlegetBundle(String baseName, Locale locale, ClassLoader loader) {
		RecordedInvocation.Builder builder = RecordedInvocation.of(InstrumentedMethod.RESOURCEBUNDLE_GETBUNDLE).withArguments(baseName, locale, loader);
		ResourceBundle result = null;
		try {
			result = ResourceBundle.getBundle(baseName, locale, loader);
		}
		finally {
			RecordedInvocationsPublisher.publish(builder.returnValue(result).build());
		}
		return result;
	}

	public static ResourceBundle resourcebundlegetBundle(String baseName, Locale targetLocale, ClassLoader loader, ResourceBundle.Control control) {
		RecordedInvocation.Builder builder = RecordedInvocation.of(InstrumentedMethod.RESOURCEBUNDLE_GETBUNDLE).withArguments(baseName, targetLocale, loader, control);
		ResourceBundle result = null;
		try {
			result = ResourceBundle.getBundle(baseName, targetLocale, loader, control);
		}
		finally {
			RecordedInvocationsPublisher.publish(builder.returnValue(result).build());
		}
		return result;
	}

	/*
	 * Bridge methods for JDK Proxies
	 */

	public static Object proxynewProxyInstance(ClassLoader loader, Class<?>[] interfaces, InvocationHandler h) {
		RecordedInvocation.Builder builder = RecordedInvocation.of(InstrumentedMethod.PROXY_NEWPROXYINSTANCE)
				.withArguments(loader, interfaces, h);
		Object result = null;
		try {
			result = Proxy.newProxyInstance(loader, interfaces, h);
		}
		finally {
			RecordedInvocationsPublisher.publish(builder.returnValue(result).build());
		}
		return result;
	}

}
