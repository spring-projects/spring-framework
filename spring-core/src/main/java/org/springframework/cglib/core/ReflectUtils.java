/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cglib.core;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.asm.Attribute;
import org.springframework.asm.Type;

/**
 * @version $Id: ReflectUtils.java,v 1.30 2009/01/11 19:47:49 herbyderby Exp $
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ReflectUtils {

	private ReflectUtils() {
	}

	private static final Map primitives = new HashMap(8);

	private static final Map transforms = new HashMap(8);

	private static final ClassLoader defaultLoader = ReflectUtils.class.getClassLoader();

	private static final Method classLoaderDefineClassMethod;

	private static final Throwable THROWABLE;

	private static final ProtectionDomain PROTECTION_DOMAIN;

	private static final List<Method> OBJECT_METHODS = new ArrayList<Method>();

	// SPRING PATCH BEGIN
	static {
		// Resolve protected ClassLoader.defineClass method for fallback use
		// (even if JDK 9+ Lookup.defineClass is preferably used below)
		Method classLoaderDefineClass;
		Throwable throwable = null;
		try {
			classLoaderDefineClass = ClassLoader.class.getDeclaredMethod("defineClass",
							String.class, byte[].class, Integer.TYPE, Integer.TYPE, ProtectionDomain.class);
		}
		catch (Throwable t) {
			classLoaderDefineClass = null;
			throwable = t;
		}

		classLoaderDefineClassMethod = classLoaderDefineClass;
		THROWABLE = throwable;
		PROTECTION_DOMAIN = getProtectionDomain(ReflectUtils.class);

		for (Method method : Object.class.getDeclaredMethods()) {
			if ("finalize".equals(method.getName())
					|| (method.getModifiers() & (Modifier.FINAL | Modifier.STATIC)) > 0) {
				continue;
			}
			OBJECT_METHODS.add(method);
		}
	}
	// SPRING PATCH END

	private static final String[] CGLIB_PACKAGES = {"java.lang"};

	static {
		primitives.put("byte", Byte.TYPE);
		primitives.put("char", Character.TYPE);
		primitives.put("double", Double.TYPE);
		primitives.put("float", Float.TYPE);
		primitives.put("int", Integer.TYPE);
		primitives.put("long", Long.TYPE);
		primitives.put("short", Short.TYPE);
		primitives.put("boolean", Boolean.TYPE);

		transforms.put("byte", "B");
		transforms.put("char", "C");
		transforms.put("double", "D");
		transforms.put("float", "F");
		transforms.put("int", "I");
		transforms.put("long", "J");
		transforms.put("short", "S");
		transforms.put("boolean", "Z");
	}

	public static ProtectionDomain getProtectionDomain(final Class source) {
		if (source == null) {
			return null;
		}
		return source.getProtectionDomain();
	}

	public static Type[] getExceptionTypes(Member member) {
		if (member instanceof Method) {
			return TypeUtils.getTypes(((Method) member).getExceptionTypes());
		}
		else if (member instanceof Constructor) {
			return TypeUtils.getTypes(((Constructor) member).getExceptionTypes());
		}
		else {
			throw new IllegalArgumentException("Cannot get exception types of a field");
		}
	}

	public static Signature getSignature(Member member) {
		if (member instanceof Method) {
			return new Signature(member.getName(), Type.getMethodDescriptor((Method) member));
		}
		else if (member instanceof Constructor) {
			Type[] types = TypeUtils.getTypes(((Constructor) member).getParameterTypes());
			return new Signature(Constants.CONSTRUCTOR_NAME,
					Type.getMethodDescriptor(Type.VOID_TYPE, types));

		}
		else {
			throw new IllegalArgumentException("Cannot get signature of a field");
		}
	}

	public static Constructor findConstructor(String desc) {
		return findConstructor(desc, defaultLoader);
	}

	public static Constructor findConstructor(String desc, ClassLoader loader) {
		try {
			int lparen = desc.indexOf('(');
			String className = desc.substring(0, lparen).trim();
			return getClass(className, loader).getConstructor(parseTypes(desc, loader));
		}
		catch (ClassNotFoundException | NoSuchMethodException ex) {
			throw new CodeGenerationException(ex);
		}
	}

	public static Method findMethod(String desc) {
		return findMethod(desc, defaultLoader);
	}

	public static Method findMethod(String desc, ClassLoader loader) {
		try {
			int lparen = desc.indexOf('(');
			int dot = desc.lastIndexOf('.', lparen);
			String className = desc.substring(0, dot).trim();
			String methodName = desc.substring(dot + 1, lparen).trim();
			return getClass(className, loader).getDeclaredMethod(methodName, parseTypes(desc, loader));
		}
		catch (ClassNotFoundException | NoSuchMethodException ex) {
			throw new CodeGenerationException(ex);
		}
	}

	private static Class[] parseTypes(String desc, ClassLoader loader) throws ClassNotFoundException {
		int lparen = desc.indexOf('(');
		int rparen = desc.indexOf(')', lparen);
		List params = new ArrayList();
		int start = lparen + 1;
		for (; ; ) {
			int comma = desc.indexOf(',', start);
			if (comma < 0) {
				break;
			}
			params.add(desc.substring(start, comma).trim());
			start = comma + 1;
		}
		if (start < rparen) {
			params.add(desc.substring(start, rparen).trim());
		}
		Class[] types = new Class[params.size()];
		for (int i = 0; i < types.length; i++) {
			types[i] = getClass((String) params.get(i), loader);
		}
		return types;
	}

	private static Class getClass(String className, ClassLoader loader) throws ClassNotFoundException {
		return getClass(className, loader, CGLIB_PACKAGES);
	}

	private static Class getClass(String className, ClassLoader loader, String[] packages) throws ClassNotFoundException {
		String save = className;
		int dimensions = 0;
		int index = 0;
		while ((index = className.indexOf("[]", index) + 1) > 0) {
			dimensions++;
		}
		StringBuilder brackets = new StringBuilder(className.length() - dimensions);
		for (int i = 0; i < dimensions; i++) {
			brackets.append('[');
		}
		className = className.substring(0, className.length() - 2 * dimensions);

		String prefix = (dimensions > 0) ? brackets + "L" : "";
		String suffix = (dimensions > 0) ? ";" : "";
		try {
			return Class.forName(prefix + className + suffix, false, loader);
		}
		catch (ClassNotFoundException ignore) {
		}
		for (int i = 0; i < packages.length; i++) {
			try {
				return Class.forName(prefix + packages[i] + '.' + className + suffix, false, loader);
			}
			catch (ClassNotFoundException ignore) {
			}
		}
		if (dimensions == 0) {
			Class c = (Class) primitives.get(className);
			if (c != null) {
				return c;
			}
		}
		else {
			String transform = (String) transforms.get(className);
			if (transform != null) {
				try {
					return Class.forName(brackets + transform, false, loader);
				}
				catch (ClassNotFoundException ignore) {
				}
			}
		}
		throw new ClassNotFoundException(save);
	}

	public static Object newInstance(Class type) {
		return newInstance(type, Constants.EMPTY_CLASS_ARRAY, null);
	}

	public static Object newInstance(Class type, Class[] parameterTypes, Object[] args) {
		return newInstance(getConstructor(type, parameterTypes), args);
	}

	@SuppressWarnings("deprecation")
	public static Object newInstance(final Constructor cstruct, final Object[] args) {
		boolean flag = cstruct.isAccessible();
		try {
			if (!flag) {
				cstruct.setAccessible(true);
			}
			Object result = cstruct.newInstance(args);
			return result;
		}
		catch (InstantiationException e) {
			throw new CodeGenerationException(e);
		}
		catch (IllegalAccessException e) {
			throw new CodeGenerationException(e);
		}
		catch (InvocationTargetException e) {
			throw new CodeGenerationException(e.getTargetException());
		}
		finally {
			if (!flag) {
				cstruct.setAccessible(flag);
			}
		}
	}

	public static Constructor getConstructor(Class type, Class[] parameterTypes) {
		try {
			Constructor constructor = type.getDeclaredConstructor(parameterTypes);
			constructor.setAccessible(true);
			return constructor;
		}
		catch (NoSuchMethodException e) {
			throw new CodeGenerationException(e);
		}
	}

	public static String[] getNames(Class[] classes) {
		if (classes == null)
			return null;
		String[] names = new String[classes.length];
		for (int i = 0; i < names.length; i++) {
			names[i] = classes[i].getName();
		}
		return names;
	}

	public static Class[] getClasses(Object[] objects) {
		Class[] classes = new Class[objects.length];
		for (int i = 0; i < objects.length; i++) {
			classes[i] = objects[i].getClass();
		}
		return classes;
	}

	public static Method findNewInstance(Class iface) {
		Method m = findInterfaceMethod(iface);
		if (!m.getName().equals("newInstance")) {
			throw new IllegalArgumentException(iface + " missing newInstance method");
		}
		return m;
	}

	public static Method[] getPropertyMethods(PropertyDescriptor[] properties, boolean read, boolean write) {
		Set methods = new HashSet();
		for (int i = 0; i < properties.length; i++) {
			PropertyDescriptor pd = properties[i];
			if (read) {
				methods.add(pd.getReadMethod());
			}
			if (write) {
				methods.add(pd.getWriteMethod());
			}
		}
		methods.remove(null);
		return (Method[]) methods.toArray(new Method[methods.size()]);
	}

	public static PropertyDescriptor[] getBeanProperties(Class type) {
		return getPropertiesHelper(type, true, true);
	}

	public static PropertyDescriptor[] getBeanGetters(Class type) {
		return getPropertiesHelper(type, true, false);
	}

	public static PropertyDescriptor[] getBeanSetters(Class type) {
		return getPropertiesHelper(type, false, true);
	}

	private static PropertyDescriptor[] getPropertiesHelper(Class type, boolean read, boolean write) {
		try {
			BeanInfo info = Introspector.getBeanInfo(type, Object.class);
			PropertyDescriptor[] all = info.getPropertyDescriptors();
			if (read && write) {
				return all;
			}
			List properties = new ArrayList(all.length);
			for (int i = 0; i < all.length; i++) {
				PropertyDescriptor pd = all[i];
				if ((read && pd.getReadMethod() != null) ||
						(write && pd.getWriteMethod() != null)) {
					properties.add(pd);
				}
			}
			return (PropertyDescriptor[]) properties.toArray(new PropertyDescriptor[properties.size()]);
		}
		catch (IntrospectionException e) {
			throw new CodeGenerationException(e);
		}
	}

	public static Method findDeclaredMethod(final Class type,
			final String methodName, final Class[] parameterTypes)
			throws NoSuchMethodException {

		Class cl = type;
		while (cl != null) {
			try {
				return cl.getDeclaredMethod(methodName, parameterTypes);
			}
			catch (NoSuchMethodException e) {
				cl = cl.getSuperclass();
			}
		}
		throw new NoSuchMethodException(methodName);
	}

	public static List addAllMethods(final Class type, final List list) {
		if (type == Object.class) {
			list.addAll(OBJECT_METHODS);
		}
		else
			list.addAll(java.util.Arrays.asList(type.getDeclaredMethods()));

		Class superclass = type.getSuperclass();
		if (superclass != null) {
			addAllMethods(superclass, list);
		}
		Class[] interfaces = type.getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			addAllMethods(interfaces[i], list);
		}

		return list;
	}

	public static List addAllInterfaces(Class type, List list) {
		Class superclass = type.getSuperclass();
		if (superclass != null) {
			list.addAll(Arrays.asList(type.getInterfaces()));
			addAllInterfaces(superclass, list);
		}
		return list;
	}


	public static Method findInterfaceMethod(Class iface) {
		if (!iface.isInterface()) {
			throw new IllegalArgumentException(iface + " is not an interface");
		}
		Method[] methods = iface.getDeclaredMethods();
		if (methods.length != 1) {
			throw new IllegalArgumentException("expecting exactly 1 method in " + iface);
		}
		return methods[0];
	}

	// SPRING PATCH BEGIN
	public static Class defineClass(String className, byte[] b, ClassLoader loader) throws Exception {
		return defineClass(className, b, loader, null, null);
	}

	public static Class defineClass(String className, byte[] b, ClassLoader loader,
			ProtectionDomain protectionDomain) throws Exception {

		return defineClass(className, b, loader, protectionDomain, null);
	}

	@SuppressWarnings("deprecation")
	public static Class defineClass(String className, byte[] b, ClassLoader loader,
			ProtectionDomain protectionDomain, Class<?> contextClass) throws Exception {

		Class c = null;
		Throwable t = THROWABLE;

		// Preferred option: JDK 9+ Lookup.defineClass API if ClassLoader matches
		if (contextClass != null && contextClass.getClassLoader() == loader) {
			try {
				MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(contextClass, MethodHandles.lookup());
				c = lookup.defineClass(b);
			}
			catch (LinkageError | IllegalArgumentException ex) {
				// in case of plain LinkageError (class already defined)
				// or IllegalArgumentException (class in different package):
				// fall through to traditional ClassLoader.defineClass below
				t = ex;
			}
			catch (Throwable ex) {
				throw new CodeGenerationException(ex);
			}
		}

		// Direct defineClass attempt on the target Classloader
		if (c == null) {
			if (protectionDomain == null) {
				protectionDomain = PROTECTION_DOMAIN;
			}

			// Look for publicDefineClass(String name, byte[] b, ProtectionDomain protectionDomain)
			try {
				Method publicDefineClass = loader.getClass().getMethod(
						"publicDefineClass", String.class, byte[].class, ProtectionDomain.class);
				c = (Class) publicDefineClass.invoke(loader, className, b, protectionDomain);
			}
			catch (InvocationTargetException ex) {
				if (!(ex.getTargetException() instanceof UnsupportedOperationException)) {
					throw new CodeGenerationException(ex.getTargetException());
				}
				// in case of UnsupportedOperationException, fall through
				t = ex.getTargetException();
			}
			catch (Throwable ex) {
				// publicDefineClass method not available -> fall through
				t = ex;
			}

			// Classic option: protected ClassLoader.defineClass method
			if (c == null && classLoaderDefineClassMethod != null) {
				Object[] args = new Object[]{className, b, 0, b.length, protectionDomain};
				try {
					if (!classLoaderDefineClassMethod.isAccessible()) {
						classLoaderDefineClassMethod.setAccessible(true);
					}
					c = (Class) classLoaderDefineClassMethod.invoke(loader, args);
				}
				catch (InvocationTargetException ex) {
					throw new CodeGenerationException(ex.getTargetException());
				}
				catch (Throwable ex) {
					// Fall through if setAccessible fails with InaccessibleObjectException on JDK 9+
					// (on the module path and/or with a JVM bootstrapped with --illegal-access=deny)
					if (!ex.getClass().getName().endsWith("InaccessibleObjectException")) {
						throw new CodeGenerationException(ex);
					}
					t = ex;
				}
			}
		}

		// Fallback option: JDK 9+ Lookup.defineClass API even if ClassLoader does not match
		if (c == null && contextClass != null && contextClass.getClassLoader() != loader) {
			try {
				MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(contextClass, MethodHandles.lookup());
				c = lookup.defineClass(b);
			}
			catch (Throwable ex) {
				throw new CodeGenerationException(ex);
			}
		}

		// No defineClass variant available at all?
		if (c == null) {
			throw new CodeGenerationException(t);
		}

		// Force static initializers to run.
		Class.forName(className, true, loader);
		return c;
	}
	// SPRING PATCH END

	public static int findPackageProtected(Class[] classes) {
		for (int i = 0; i < classes.length; i++) {
			if (!Modifier.isPublic(classes[i].getModifiers())) {
				return i;
			}
		}
		return 0;
	}

	public static MethodInfo getMethodInfo(final Member member, final int modifiers) {
		final Signature sig = getSignature(member);
		return new MethodInfo() {
			private ClassInfo ci;

			public ClassInfo getClassInfo() {
				if (ci == null)
					ci = ReflectUtils.getClassInfo(member.getDeclaringClass());
				return ci;
			}

			public int getModifiers() {
				return modifiers;
			}

			public Signature getSignature() {
				return sig;
			}

			public Type[] getExceptionTypes() {
				return ReflectUtils.getExceptionTypes(member);
			}

			public Attribute getAttribute() {
				return null;
			}
		};
	}

	public static MethodInfo getMethodInfo(Member member) {
		return getMethodInfo(member, member.getModifiers());
	}

	public static ClassInfo getClassInfo(final Class clazz) {
		final Type type = Type.getType(clazz);
		final Type sc = (clazz.getSuperclass() == null) ? null : Type.getType(clazz.getSuperclass());
		return new ClassInfo() {
			public Type getType() {
				return type;
			}
			public Type getSuperType() {
				return sc;
			}
			public Type[] getInterfaces() {
				return TypeUtils.getTypes(clazz.getInterfaces());
			}
			public int getModifiers() {
				return clazz.getModifiers();
			}
		};
	}

	// used by MethodInterceptorGenerated generated code
	public static Method[] findMethods(String[] namesAndDescriptors, Method[] methods) {
		Map map = new HashMap();
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];
			map.put(method.getName() + Type.getMethodDescriptor(method), method);
		}
		Method[] result = new Method[namesAndDescriptors.length / 2];
		for (int i = 0; i < result.length; i++) {
			result[i] = (Method) map.get(namesAndDescriptors[i * 2] + namesAndDescriptors[i * 2 + 1]);
			if (result[i] == null) {
				// TODO: error?
			}
		}
		return result;
	}

}
