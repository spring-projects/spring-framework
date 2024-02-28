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

package org.springframework.beans;

import java.beans.ConstructorProperties;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.jvm.KCallablesJvm;
import kotlin.reflect.jvm.ReflectJvmMapping;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Static convenience methods for JavaBeans: for instantiating beans,
 * checking bean property types, copying bean properties, etc.
 *
 * <p>Mainly for internal use within the framework, but to some degree also
 * useful for application classes. Consider
 * <a href="https://commons.apache.org/proper/commons-beanutils/">Apache Commons BeanUtils</a>,
 * <a href="https://github.com/ExpediaGroup/bull">BULL - Bean Utils Light Library</a>,
 * or similar third-party frameworks for more comprehensive bean utilities.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sam Brannen
 * @author Sebastien Deleuze
 */
public abstract class BeanUtils {

	private static final ParameterNameDiscoverer parameterNameDiscoverer =
			new DefaultParameterNameDiscoverer();

	private static final Set<Class<?>> unknownEditorTypes =
			Collections.newSetFromMap(new ConcurrentReferenceHashMap<>(64));

	private static final Map<Class<?>, Object> DEFAULT_TYPE_VALUES = Map.of(
			boolean.class, false,
			byte.class, (byte) 0,
			short.class, (short) 0,
			int.class, 0,
			long.class, 0L,
			float.class, 0F,
			double.class, 0D,
			char.class, '\0');


	/**
	 * Convenience method to instantiate a class using its no-arg constructor.
	 * @param clazz class to instantiate
	 * @return the new instance
	 * @throws BeanInstantiationException if the bean cannot be instantiated
	 * @see Class#newInstance()
	 * @deprecated as of Spring 5.0, following the deprecation of
	 * {@link Class#newInstance()} in JDK 9
	 */
	@Deprecated
	public static <T> T instantiate(Class<T> clazz) throws BeanInstantiationException {
		Assert.notNull(clazz, "Class must not be null");
		if (clazz.isInterface()) {
			throw new BeanInstantiationException(clazz, "Specified class is an interface");
		}
		try {
			return clazz.newInstance();
		}
		catch (InstantiationException ex) {
			throw new BeanInstantiationException(clazz, "Is it an abstract class?", ex);
		}
		catch (IllegalAccessException ex) {
			throw new BeanInstantiationException(clazz, "Is the constructor accessible?", ex);
		}
	}

	/**
	 * Instantiate a class using its 'primary' constructor (for Kotlin classes,
	 * potentially having default arguments declared) or its default constructor
	 * (for regular Java classes, expecting a standard no-arg setup).
	 * <p>Note that this method tries to set the constructor accessible
	 * if given a non-accessible (that is, non-public) constructor.
	 * @param clazz the class to instantiate
	 * @return the new instance
	 * @throws BeanInstantiationException if the bean cannot be instantiated.
	 * The cause may notably indicate a {@link NoSuchMethodException} if no
	 * primary/default constructor was found, a {@link NoClassDefFoundError}
	 * or other {@link LinkageError} in case of an unresolvable class definition
	 * (e.g. due to a missing dependency at runtime), or an exception thrown
	 * from the constructor invocation itself.
	 * @see Constructor#newInstance
	 */
	public static <T> T instantiateClass(Class<T> clazz) throws BeanInstantiationException {
		Assert.notNull(clazz, "Class must not be null");
		if (clazz.isInterface()) {
			throw new BeanInstantiationException(clazz, "Specified class is an interface");
		}
		Constructor<T> ctor;
		try {
			ctor = clazz.getDeclaredConstructor();
		}
		catch (NoSuchMethodException ex) {
			ctor = findPrimaryConstructor(clazz);
			if (ctor == null) {
				throw new BeanInstantiationException(clazz, "No default constructor found", ex);
			}
		}
		catch (LinkageError err) {
			throw new BeanInstantiationException(clazz, "Unresolvable class definition", err);
		}
		return instantiateClass(ctor);
	}

	/**
	 * Instantiate a class using its no-arg constructor and return the new instance
	 * as the specified assignable type.
	 * <p>Useful in cases where the type of the class to instantiate (clazz) is not
	 * available, but the type desired (assignableTo) is known.
	 * <p>Note that this method tries to set the constructor accessible if given a
	 * non-accessible (that is, non-public) constructor.
	 * @param clazz class to instantiate
	 * @param assignableTo type that clazz must be assignableTo
	 * @return the new instance
	 * @throws BeanInstantiationException if the bean cannot be instantiated
	 * @see Constructor#newInstance
	 */
	@SuppressWarnings("unchecked")
	public static <T> T instantiateClass(Class<?> clazz, Class<T> assignableTo) throws BeanInstantiationException {
		Assert.isAssignable(assignableTo, clazz);
		return (T) instantiateClass(clazz);
	}

	/**
	 * Convenience method to instantiate a class using the given constructor.
	 * <p>Note that this method tries to set the constructor accessible if given a
	 * non-accessible (that is, non-public) constructor, and supports Kotlin classes
	 * with optional parameters and default values.
	 * @param ctor the constructor to instantiate
	 * @param args the constructor arguments to apply (use {@code null} for an unspecified
	 * parameter, Kotlin optional parameters and Java primitive types are supported)
	 * @return the new instance
	 * @throws BeanInstantiationException if the bean cannot be instantiated
	 * @see Constructor#newInstance
	 */
	public static <T> T instantiateClass(Constructor<T> ctor, Object... args) throws BeanInstantiationException {
		Assert.notNull(ctor, "Constructor must not be null");
		try {
			ReflectionUtils.makeAccessible(ctor);
			if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(ctor.getDeclaringClass())) {
				return KotlinDelegate.instantiateClass(ctor, args);
			}
			else {
				int parameterCount = ctor.getParameterCount();
				Assert.isTrue(args.length <= parameterCount, "Can't specify more arguments than constructor parameters");
				if (parameterCount == 0) {
					return ctor.newInstance();
				}
				Class<?>[] parameterTypes = ctor.getParameterTypes();
				Object[] argsWithDefaultValues = new Object[args.length];
				for (int i = 0 ; i < args.length; i++) {
					if (args[i] == null) {
						Class<?> parameterType = parameterTypes[i];
						argsWithDefaultValues[i] = (parameterType.isPrimitive() ? DEFAULT_TYPE_VALUES.get(parameterType) : null);
					}
					else {
						argsWithDefaultValues[i] = args[i];
					}
				}
				return ctor.newInstance(argsWithDefaultValues);
			}
		}
		catch (InstantiationException ex) {
			throw new BeanInstantiationException(ctor, "Is it an abstract class?", ex);
		}
		catch (IllegalAccessException ex) {
			throw new BeanInstantiationException(ctor, "Is the constructor accessible?", ex);
		}
		catch (IllegalArgumentException ex) {
			throw new BeanInstantiationException(ctor, "Illegal arguments for constructor", ex);
		}
		catch (InvocationTargetException ex) {
			throw new BeanInstantiationException(ctor, "Constructor threw exception", ex.getTargetException());
		}
	}

	/**
	 * Return a resolvable constructor for the provided class, either a primary or single
	 * public constructor with arguments, or a single non-public constructor with arguments,
	 * or simply a default constructor. Callers have to be prepared to resolve arguments
	 * for the returned constructor's parameters, if any.
	 * @param clazz the class to check
	 * @throws IllegalStateException in case of no unique constructor found at all
	 * @since 5.3
	 * @see #findPrimaryConstructor
	 */
	@SuppressWarnings("unchecked")
	public static <T> Constructor<T> getResolvableConstructor(Class<T> clazz) {
		Constructor<T> ctor = findPrimaryConstructor(clazz);
		if (ctor != null) {
			return ctor;
		}

		Constructor<?>[] ctors = clazz.getConstructors();
		if (ctors.length == 1) {
			// A single public constructor
			return (Constructor<T>) ctors[0];
		}
		else if (ctors.length == 0) {
			// No public constructors -> check non-public
			ctors = clazz.getDeclaredConstructors();
			if (ctors.length == 1) {
				// A single non-public constructor, e.g. from a non-public record type
				return (Constructor<T>) ctors[0];
			}
		}

		// Several constructors -> let's try to take the default constructor
		try {
			return clazz.getDeclaredConstructor();
		}
		catch (NoSuchMethodException ex) {
			// Giving up...
		}

		// No unique constructor at all
		throw new IllegalStateException("No primary or single unique constructor found for " + clazz);
	}

	/**
	 * Return the primary constructor of the provided class. For Kotlin classes, this
	 * returns the Java constructor corresponding to the Kotlin primary constructor
	 * (as defined in the Kotlin specification). Otherwise, in particular for non-Kotlin
	 * classes, this simply returns {@code null}.
	 * @param clazz the class to check
	 * @since 5.0
	 * @see <a href="https://kotlinlang.org/docs/reference/classes.html#constructors">Kotlin docs</a>
	 */
	@Nullable
	public static <T> Constructor<T> findPrimaryConstructor(Class<T> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(clazz)) {
			return KotlinDelegate.findPrimaryConstructor(clazz);
		}
		return null;
	}

	/**
	 * Find a method with the given method name and the given parameter types,
	 * declared on the given class or one of its superclasses. Prefers public methods,
	 * but will return a protected, package access, or private method too.
	 * <p>Checks {@code Class.getMethod} first, falling back to
	 * {@code findDeclaredMethod}. This allows to find public methods
	 * without issues even in environments with restricted Java security settings.
	 * @param clazz the class to check
	 * @param methodName the name of the method to find
	 * @param paramTypes the parameter types of the method to find
	 * @return the Method object, or {@code null} if not found
	 * @see Class#getMethod
	 * @see #findDeclaredMethod
	 */
	@Nullable
	public static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		try {
			return clazz.getMethod(methodName, paramTypes);
		}
		catch (NoSuchMethodException ex) {
			return findDeclaredMethod(clazz, methodName, paramTypes);
		}
	}

	/**
	 * Find a method with the given method name and the given parameter types,
	 * declared on the given class or one of its superclasses. Will return a public,
	 * protected, package access, or private method.
	 * <p>Checks {@code Class.getDeclaredMethod}, cascading upwards to all superclasses.
	 * @param clazz the class to check
	 * @param methodName the name of the method to find
	 * @param paramTypes the parameter types of the method to find
	 * @return the Method object, or {@code null} if not found
	 * @see Class#getDeclaredMethod
	 */
	@Nullable
	public static Method findDeclaredMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		try {
			return clazz.getDeclaredMethod(methodName, paramTypes);
		}
		catch (NoSuchMethodException ex) {
			if (clazz.getSuperclass() != null) {
				return findDeclaredMethod(clazz.getSuperclass(), methodName, paramTypes);
			}
			return null;
		}
	}

	/**
	 * Find a method with the given method name and minimal parameters (best case: none),
	 * declared on the given class or one of its superclasses. Prefers public methods,
	 * but will return a protected, package access, or private method too.
	 * <p>Checks {@code Class.getMethods} first, falling back to
	 * {@code findDeclaredMethodWithMinimalParameters}. This allows for finding public
	 * methods without issues even in environments with restricted Java security settings.
	 * @param clazz the class to check
	 * @param methodName the name of the method to find
	 * @return the Method object, or {@code null} if not found
	 * @throws IllegalArgumentException if methods of the given name were found but
	 * could not be resolved to a unique method with minimal parameters
	 * @see Class#getMethods
	 * @see #findDeclaredMethodWithMinimalParameters
	 */
	@Nullable
	public static Method findMethodWithMinimalParameters(Class<?> clazz, String methodName)
			throws IllegalArgumentException {

		Method targetMethod = findMethodWithMinimalParameters(clazz.getMethods(), methodName);
		if (targetMethod == null) {
			targetMethod = findDeclaredMethodWithMinimalParameters(clazz, methodName);
		}
		return targetMethod;
	}

	/**
	 * Find a method with the given method name and minimal parameters (best case: none),
	 * declared on the given class or one of its superclasses. Will return a public,
	 * protected, package access, or private method.
	 * <p>Checks {@code Class.getDeclaredMethods}, cascading upwards to all superclasses.
	 * @param clazz the class to check
	 * @param methodName the name of the method to find
	 * @return the Method object, or {@code null} if not found
	 * @throws IllegalArgumentException if methods of the given name were found but
	 * could not be resolved to a unique method with minimal parameters
	 * @see Class#getDeclaredMethods
	 */
	@Nullable
	public static Method findDeclaredMethodWithMinimalParameters(Class<?> clazz, String methodName)
			throws IllegalArgumentException {

		Method targetMethod = findMethodWithMinimalParameters(clazz.getDeclaredMethods(), methodName);
		if (targetMethod == null && clazz.getSuperclass() != null) {
			targetMethod = findDeclaredMethodWithMinimalParameters(clazz.getSuperclass(), methodName);
		}
		return targetMethod;
	}

	/**
	 * Find a method with the given method name and minimal parameters (best case: none)
	 * in the given list of methods.
	 * @param methods the methods to check
	 * @param methodName the name of the method to find
	 * @return the Method object, or {@code null} if not found
	 * @throws IllegalArgumentException if methods of the given name were found but
	 * could not be resolved to a unique method with minimal parameters
	 */
	@Nullable
	public static Method findMethodWithMinimalParameters(Method[] methods, String methodName)
			throws IllegalArgumentException {

		Method targetMethod = null;
		int numMethodsFoundWithCurrentMinimumArgs = 0;
		for (Method method : methods) {
			if (method.getName().equals(methodName)) {
				int numParams = method.getParameterCount();
				if (targetMethod == null || numParams < targetMethod.getParameterCount()) {
					targetMethod = method;
					numMethodsFoundWithCurrentMinimumArgs = 1;
				}
				else if (!method.isBridge() && targetMethod.getParameterCount() == numParams) {
					if (targetMethod.isBridge()) {
						// Prefer regular method over bridge...
						targetMethod = method;
					}
					else {
						// Additional candidate with same length
						numMethodsFoundWithCurrentMinimumArgs++;
					}
				}
			}
		}
		if (numMethodsFoundWithCurrentMinimumArgs > 1) {
			throw new IllegalArgumentException("Cannot resolve method '" + methodName +
					"' to a unique method. Attempted to resolve to overloaded method with " +
					"the least number of parameters but there were " +
					numMethodsFoundWithCurrentMinimumArgs + " candidates.");
		}
		return targetMethod;
	}

	/**
	 * Parse a method signature in the form {@code methodName[([arg_list])]},
	 * where {@code arg_list} is an optional, comma-separated list of fully-qualified
	 * type names, and attempts to resolve that signature against the supplied {@code Class}.
	 * <p>When not supplying an argument list ({@code methodName}) the method whose name
	 * matches and has the least number of parameters will be returned. When supplying an
	 * argument type list, only the method whose name and argument types match will be returned.
	 * <p>Note then that {@code methodName} and {@code methodName()} are <strong>not</strong>
	 * resolved in the same way. The signature {@code methodName} means the method called
	 * {@code methodName} with the least number of arguments, whereas {@code methodName()}
	 * means the method called {@code methodName} with exactly 0 arguments.
	 * <p>If no method can be found, then {@code null} is returned.
	 * @param signature the method signature as String representation
	 * @param clazz the class to resolve the method signature against
	 * @return the resolved Method
	 * @see #findMethod
	 * @see #findMethodWithMinimalParameters
	 */
	@Nullable
	public static Method resolveSignature(String signature, Class<?> clazz) {
		Assert.hasText(signature, "'signature' must not be empty");
		Assert.notNull(clazz, "Class must not be null");
		int startParen = signature.indexOf('(');
		int endParen = signature.indexOf(')');
		if (startParen > -1 && endParen == -1) {
			throw new IllegalArgumentException("Invalid method signature '" + signature +
					"': expected closing ')' for args list");
		}
		else if (startParen == -1 && endParen > -1) {
			throw new IllegalArgumentException("Invalid method signature '" + signature +
					"': expected opening '(' for args list");
		}
		else if (startParen == -1) {
			return findMethodWithMinimalParameters(clazz, signature);
		}
		else {
			String methodName = signature.substring(0, startParen);
			String[] parameterTypeNames =
					StringUtils.commaDelimitedListToStringArray(signature.substring(startParen + 1, endParen));
			Class<?>[] parameterTypes = new Class<?>[parameterTypeNames.length];
			for (int i = 0; i < parameterTypeNames.length; i++) {
				String parameterTypeName = parameterTypeNames[i].trim();
				try {
					parameterTypes[i] = ClassUtils.forName(parameterTypeName, clazz.getClassLoader());
				}
				catch (Throwable ex) {
					throw new IllegalArgumentException("Invalid method signature: unable to resolve type [" +
							parameterTypeName + "] for argument " + i + ". Root cause: " + ex);
				}
			}
			return findMethod(clazz, methodName, parameterTypes);
		}
	}


	/**
	 * Retrieve the JavaBeans {@code PropertyDescriptor}s of a given class.
	 * @param clazz the Class to retrieve the PropertyDescriptors for
	 * @return an array of {@code PropertyDescriptors} for the given class
	 * @throws BeansException if PropertyDescriptor look fails
	 */
	public static PropertyDescriptor[] getPropertyDescriptors(Class<?> clazz) throws BeansException {
		return CachedIntrospectionResults.forClass(clazz).getPropertyDescriptors();
	}

	/**
	 * Retrieve the JavaBeans {@code PropertyDescriptors} for the given property.
	 * @param clazz the Class to retrieve the PropertyDescriptor for
	 * @param propertyName the name of the property
	 * @return the corresponding PropertyDescriptor, or {@code null} if none
	 * @throws BeansException if PropertyDescriptor lookup fails
	 */
	@Nullable
	public static PropertyDescriptor getPropertyDescriptor(Class<?> clazz, String propertyName) throws BeansException {
		return CachedIntrospectionResults.forClass(clazz).getPropertyDescriptor(propertyName);
	}

	/**
	 * Find a JavaBeans {@code PropertyDescriptor} for the given method,
	 * with the method either being the read method or the write method for
	 * that bean property.
	 * @param method the method to find a corresponding PropertyDescriptor for,
	 * introspecting its declaring class
	 * @return the corresponding PropertyDescriptor, or {@code null} if none
	 * @throws BeansException if PropertyDescriptor lookup fails
	 */
	@Nullable
	public static PropertyDescriptor findPropertyForMethod(Method method) throws BeansException {
		return findPropertyForMethod(method, method.getDeclaringClass());
	}

	/**
	 * Find a JavaBeans {@code PropertyDescriptor} for the given method,
	 * with the method either being the read method or the write method for
	 * that bean property.
	 * @param method the method to find a corresponding PropertyDescriptor for
	 * @param clazz the (most specific) class to introspect for descriptors
	 * @return the corresponding PropertyDescriptor, or {@code null} if none
	 * @throws BeansException if PropertyDescriptor lookup fails
	 * @since 3.2.13
	 */
	@Nullable
	public static PropertyDescriptor findPropertyForMethod(Method method, Class<?> clazz) throws BeansException {
		Assert.notNull(method, "Method must not be null");
		PropertyDescriptor[] pds = getPropertyDescriptors(clazz);
		for (PropertyDescriptor pd : pds) {
			if (method.equals(pd.getReadMethod()) || method.equals(pd.getWriteMethod())) {
				return pd;
			}
		}
		return null;
	}

	/**
	 * Find a JavaBeans PropertyEditor following the 'Editor' suffix convention
	 * (e.g. "mypackage.MyDomainClass" &rarr; "mypackage.MyDomainClassEditor").
	 * <p>Compatible to the standard JavaBeans convention as implemented by
	 * {@link java.beans.PropertyEditorManager} but isolated from the latter's
	 * registered default editors for primitive types.
	 * @param targetType the type to find an editor for
	 * @return the corresponding editor, or {@code null} if none found
	 */
	@Nullable
	public static PropertyEditor findEditorByConvention(@Nullable Class<?> targetType) {
		if (targetType == null || targetType.isArray() || unknownEditorTypes.contains(targetType)) {
			return null;
		}

		ClassLoader cl = targetType.getClassLoader();
		if (cl == null) {
			try {
				cl = ClassLoader.getSystemClassLoader();
				if (cl == null) {
					return null;
				}
			}
			catch (Throwable ex) {
				// e.g. AccessControlException on Google App Engine
				return null;
			}
		}

		String targetTypeName = targetType.getName();
		String editorName = targetTypeName + "Editor";
		try {
			Class<?> editorClass = cl.loadClass(editorName);
			if (editorClass != null) {
				if (!PropertyEditor.class.isAssignableFrom(editorClass)) {
					unknownEditorTypes.add(targetType);
					return null;
				}
				return (PropertyEditor) instantiateClass(editorClass);
			}
			// Misbehaving ClassLoader returned null instead of ClassNotFoundException
			// - fall back to unknown editor type registration below
		}
		catch (ClassNotFoundException ex) {
			// Ignore - fall back to unknown editor type registration below
		}
		unknownEditorTypes.add(targetType);
		return null;
	}

	/**
	 * Determine the bean property type for the given property from the
	 * given classes/interfaces, if possible.
	 * @param propertyName the name of the bean property
	 * @param beanClasses the classes to check against
	 * @return the property type, or {@code Object.class} as fallback
	 */
	public static Class<?> findPropertyType(String propertyName, @Nullable Class<?>... beanClasses) {
		if (beanClasses != null) {
			for (Class<?> beanClass : beanClasses) {
				PropertyDescriptor pd = getPropertyDescriptor(beanClass, propertyName);
				if (pd != null) {
					return pd.getPropertyType();
				}
			}
		}
		return Object.class;
	}

	/**
	 * Determine whether the specified property has a unique write method,
	 * i.e. is writable but does not declare overloaded setter methods.
	 * @param pd the PropertyDescriptor for the property
	 * @return {@code true} if writable and unique, {@code false} otherwise
	 * @since 6.1.4
	 */
	public static boolean hasUniqueWriteMethod(PropertyDescriptor pd) {
		if (pd instanceof GenericTypeAwarePropertyDescriptor gpd) {
			return gpd.hasUniqueWriteMethod();
		}
		else {
			return (pd.getWriteMethod() != null);
		}
	}

	/**
	 * Obtain a new MethodParameter object for the write method of the
	 * specified property.
	 * @param pd the PropertyDescriptor for the property
	 * @return a corresponding MethodParameter object
	 */
	public static MethodParameter getWriteMethodParameter(PropertyDescriptor pd) {
		if (pd instanceof GenericTypeAwarePropertyDescriptor gpd) {
			return new MethodParameter(gpd.getWriteMethodParameter());
		}
		else {
			Method writeMethod = pd.getWriteMethod();
			Assert.state(writeMethod != null, "No write method available");
			return new MethodParameter(writeMethod, 0);
		}
	}

	/**
	 * Determine required parameter names for the given constructor,
	 * considering the JavaBeans {@link ConstructorProperties} annotation
	 * as well as Spring's {@link DefaultParameterNameDiscoverer}.
	 * @param ctor the constructor to find parameter names for
	 * @return the parameter names (matching the constructor's parameter count)
	 * @throws IllegalStateException if the parameter names are not resolvable
	 * @since 5.3
	 * @see ConstructorProperties
	 * @see DefaultParameterNameDiscoverer
	 */
	public static String[] getParameterNames(Constructor<?> ctor) {
		ConstructorProperties cp = ctor.getAnnotation(ConstructorProperties.class);
		String[] paramNames = (cp != null ? cp.value() : parameterNameDiscoverer.getParameterNames(ctor));
		Assert.state(paramNames != null, () -> "Cannot resolve parameter names for constructor " + ctor);
		Assert.state(paramNames.length == ctor.getParameterCount(),
				() -> "Invalid number of parameter names: " + paramNames.length + " for constructor " + ctor);
		return paramNames;
	}

	/**
	 * Check if the given type represents a "simple" property: a simple value
	 * type or an array of simple value types.
	 * <p>See {@link #isSimpleValueType(Class)} for the definition of <em>simple
	 * value type</em>.
	 * <p>Used to determine properties to check for a "simple" dependency-check.
	 * @param type the type to check
	 * @return whether the given type represents a "simple" property
	 * @see org.springframework.beans.factory.support.RootBeanDefinition#DEPENDENCY_CHECK_SIMPLE
	 * @see org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#checkDependencies
	 * @see #isSimpleValueType(Class)
	 */
	public static boolean isSimpleProperty(Class<?> type) {
		Assert.notNull(type, "'type' must not be null");
		return isSimpleValueType(type) || (type.isArray() && isSimpleValueType(type.componentType()));
	}

	/**
	 * Check if the given type represents a "simple" value type for
	 * bean property and data binding purposes:
	 * a primitive or primitive wrapper, an {@code Enum}, a {@code String}
	 * or other {@code CharSequence}, a {@code Number}, a {@code Date},
	 * a {@code Temporal}, a {@code UUID}, a {@code URI}, a {@code URL},
	 * a {@code Locale}, or a {@code Class}.
	 * <p>{@code Void} and {@code void} are not considered simple value types.
	 * <p>As of 6.1, this method delegates to {@link ClassUtils#isSimpleValueType}
	 * as-is but could potentially add further rules for bean property purposes.
	 * @param type the type to check
	 * @return whether the given type represents a "simple" value type
	 * @see #isSimpleProperty(Class)
	 * @see ClassUtils#isSimpleValueType(Class)
	 */
	public static boolean isSimpleValueType(Class<?> type) {
		return ClassUtils.isSimpleValueType(type);
	}


	/**
	 * Copy the property values of the given source bean into the target bean.
	 * <p>Note: The source and target classes do not have to match or even be derived
	 * from each other, as long as the properties match. Any bean properties that the
	 * source bean exposes but the target bean does not will silently be ignored.
	 * <p>This is just a convenience method. For more complex transfer needs,
	 * consider using a full {@link BeanWrapper}.
	 * <p>As of Spring Framework 5.3, this method honors generic type information
	 * when matching properties in the source and target objects.
	 * <p>The following table provides a non-exhaustive set of examples of source
	 * and target property types that can be copied as well as source and target
	 * property types that cannot be copied.
	 * <table border="1">
	 * <tr><th>source property type</th><th>target property type</th><th>copy supported</th></tr>
	 * <tr><td>{@code Integer}</td><td>{@code Integer}</td><td>yes</td></tr>
	 * <tr><td>{@code Integer}</td><td>{@code Number}</td><td>yes</td></tr>
	 * <tr><td>{@code List<Integer>}</td><td>{@code List<Integer>}</td><td>yes</td></tr>
	 * <tr><td>{@code List<?>}</td><td>{@code List<?>}</td><td>yes</td></tr>
	 * <tr><td>{@code List<Integer>}</td><td>{@code List<?>}</td><td>yes</td></tr>
	 * <tr><td>{@code List<Integer>}</td><td>{@code List<? extends Number>}</td><td>yes</td></tr>
	 * <tr><td>{@code String}</td><td>{@code Integer}</td><td>no</td></tr>
	 * <tr><td>{@code Number}</td><td>{@code Integer}</td><td>no</td></tr>
	 * <tr><td>{@code List<Integer>}</td><td>{@code List<Long>}</td><td>no</td></tr>
	 * <tr><td>{@code List<Integer>}</td><td>{@code List<Number>}</td><td>no</td></tr>
	 * </table>
	 * @param source the source bean
	 * @param target the target bean
	 * @throws BeansException if the copying failed
	 * @see BeanWrapper
	 */
	public static void copyProperties(Object source, Object target) throws BeansException {
		copyProperties(source, target, null, (String[]) null);
	}

	/**
	 * Copy the property values of the given source bean into the given target bean,
	 * only setting properties defined in the given "editable" class (or interface).
	 * <p>Note: The source and target classes do not have to match or even be derived
	 * from each other, as long as the properties match. Any bean properties that the
	 * source bean exposes but the target bean does not will silently be ignored.
	 * <p>This is just a convenience method. For more complex transfer needs,
	 * consider using a full {@link BeanWrapper}.
	 * <p>As of Spring Framework 5.3, this method honors generic type information
	 * when matching properties in the source and target objects. See the
	 * documentation for {@link #copyProperties(Object, Object)} for details.
	 * @param source the source bean
	 * @param target the target bean
	 * @param editable the class (or interface) to restrict property setting to
	 * @throws BeansException if the copying failed
	 * @see BeanWrapper
	 */
	public static void copyProperties(Object source, Object target, Class<?> editable) throws BeansException {
		copyProperties(source, target, editable, (String[]) null);
	}

	/**
	 * Copy the property values of the given source bean into the given target bean,
	 * ignoring the given "ignoreProperties".
	 * <p>Note: The source and target classes do not have to match or even be derived
	 * from each other, as long as the properties match. Any bean properties that the
	 * source bean exposes but the target bean does not will silently be ignored.
	 * <p>This is just a convenience method. For more complex transfer needs,
	 * consider using a full {@link BeanWrapper}.
	 * <p>As of Spring Framework 5.3, this method honors generic type information
	 * when matching properties in the source and target objects. See the
	 * documentation for {@link #copyProperties(Object, Object)} for details.
	 * @param source the source bean
	 * @param target the target bean
	 * @param ignoreProperties array of property names to ignore
	 * @throws BeansException if the copying failed
	 * @see BeanWrapper
	 */
	public static void copyProperties(Object source, Object target, String... ignoreProperties) throws BeansException {
		copyProperties(source, target, null, ignoreProperties);
	}

	/**
	 * Copy the property values of the given source bean into the given target bean.
	 * <p>Note: The source and target classes do not have to match or even be derived
	 * from each other, as long as the properties match. Any bean properties that the
	 * source bean exposes but the target bean does not will silently be ignored.
	 * <p>As of Spring Framework 5.3, this method honors generic type information
	 * when matching properties in the source and target objects. See the
	 * documentation for {@link #copyProperties(Object, Object)} for details.
	 * @param source the source bean
	 * @param target the target bean
	 * @param editable the class (or interface) to restrict property setting to
	 * @param ignoreProperties array of property names to ignore
	 * @throws BeansException if the copying failed
	 * @see BeanWrapper
	 */
	private static void copyProperties(Object source, Object target, @Nullable Class<?> editable,
			@Nullable String... ignoreProperties) throws BeansException {

		Assert.notNull(source, "Source must not be null");
		Assert.notNull(target, "Target must not be null");

		Class<?> actualEditable = target.getClass();
		if (editable != null) {
			if (!editable.isInstance(target)) {
				throw new IllegalArgumentException("Target class [" + target.getClass().getName() +
						"] not assignable to editable class [" + editable.getName() + "]");
			}
			actualEditable = editable;
		}
		PropertyDescriptor[] targetPds = getPropertyDescriptors(actualEditable);
		Set<String> ignoredProps = (ignoreProperties != null ? new HashSet<>(Arrays.asList(ignoreProperties)) : null);
		CachedIntrospectionResults sourceResults = (actualEditable != source.getClass() ?
				CachedIntrospectionResults.forClass(source.getClass()) : null);

		for (PropertyDescriptor targetPd : targetPds) {
			Method writeMethod = targetPd.getWriteMethod();
			if (writeMethod != null && (ignoredProps == null || !ignoredProps.contains(targetPd.getName()))) {
				PropertyDescriptor sourcePd = (sourceResults != null ?
						sourceResults.getPropertyDescriptor(targetPd.getName()) : targetPd);
				if (sourcePd != null) {
					Method readMethod = sourcePd.getReadMethod();
					if (readMethod != null) {
						if (isAssignable(writeMethod, readMethod, sourcePd, targetPd)) {
							try {
								ReflectionUtils.makeAccessible(readMethod);
								Object value = readMethod.invoke(source);
								ReflectionUtils.makeAccessible(writeMethod);
								writeMethod.invoke(target, value);
							}
							catch (Throwable ex) {
								throw new FatalBeanException(
										"Could not copy property '" + targetPd.getName() + "' from source to target", ex);
							}
						}
					}
				}
			}
		}
	}

	private static boolean isAssignable(Method writeMethod, Method readMethod,
			PropertyDescriptor sourcePd, PropertyDescriptor targetPd) {

		Type paramType = writeMethod.getGenericParameterTypes()[0];
		if (paramType instanceof Class<?> clazz) {
			return ClassUtils.isAssignable(clazz, readMethod.getReturnType());
		}
		else if (paramType.equals(readMethod.getGenericReturnType())) {
			return true;
		}
		else {
			ResolvableType sourceType = ((GenericTypeAwarePropertyDescriptor) sourcePd).getReadMethodType();
			ResolvableType targetType = ((GenericTypeAwarePropertyDescriptor) targetPd).getWriteMethodType();
			// Ignore generic types in assignable check if either ResolvableType has unresolvable generics.
			return (sourceType.hasUnresolvableGenerics() || targetType.hasUnresolvableGenerics() ?
					ClassUtils.isAssignable(writeMethod.getParameterTypes()[0], readMethod.getReturnType()) :
					targetType.isAssignableFrom(sourceType));
		}
	}


	/**
	 * Inner class to avoid a hard dependency on Kotlin at runtime.
	 */
	private static class KotlinDelegate {

		/**
		 * Retrieve the Java constructor corresponding to the Kotlin primary constructor, if any.
		 * @param clazz the {@link Class} of the Kotlin class
		 * @see <a href="https://kotlinlang.org/docs/reference/classes.html#constructors">
		 * https://kotlinlang.org/docs/reference/classes.html#constructors</a>
		 */
		@SuppressWarnings("unchecked")
		@Nullable
		public static <T> Constructor<T> findPrimaryConstructor(Class<T> clazz) {
			try {
				KClass<T> kClass = JvmClassMappingKt.getKotlinClass(clazz);
				KFunction<T> primaryCtor = KClasses.getPrimaryConstructor(kClass);
				if (primaryCtor == null) {
					return null;
				}
				if (KotlinDetector.isInlineClass(clazz)) {
					Constructor<?>[] constructors = clazz.getDeclaredConstructors();
					Assert.state(constructors.length == 1,
							"Kotlin value classes annotated with @JvmInline are expected to have a single JVM constructor");
					return (Constructor<T>) constructors[0];
				}
				Constructor<T> constructor = ReflectJvmMapping.getJavaConstructor(primaryCtor);
				if (constructor == null) {
					throw new IllegalStateException(
							"Failed to find Java constructor for Kotlin primary constructor: " + clazz.getName());
				}
				return constructor;
			}
			catch (UnsupportedOperationException ex) {
				return null;
			}
		}

		/**
		 * Instantiate a Kotlin class using the provided constructor.
		 * @param ctor the constructor of the Kotlin class to instantiate
		 * @param args the constructor arguments to apply
		 * (use {@code null} for unspecified parameter if needed)
		 */
		public static <T> T instantiateClass(Constructor<T> ctor, Object... args)
				throws IllegalAccessException, InvocationTargetException, InstantiationException {

			KFunction<T> kotlinConstructor = ReflectJvmMapping.getKotlinFunction(ctor);
			if (kotlinConstructor == null) {
				return ctor.newInstance(args);
			}

			if ((!Modifier.isPublic(ctor.getModifiers()) || !Modifier.isPublic(ctor.getDeclaringClass().getModifiers()))) {
				KCallablesJvm.setAccessible(kotlinConstructor, true);
			}

			List<KParameter> parameters = kotlinConstructor.getParameters();

			Assert.isTrue(args.length <= parameters.size(),
					"Number of provided arguments must be less than or equal to the number of constructor parameters");
			if (parameters.isEmpty()) {
				return kotlinConstructor.call();
			}
			Map<KParameter, Object> argParameters = CollectionUtils.newHashMap(parameters.size());
			for (int i = 0 ; i < args.length ; i++) {
				if (!(parameters.get(i).isOptional() && args[i] == null)) {
					argParameters.put(parameters.get(i), args[i]);
				}
			}
			return kotlinConstructor.callBy(argParameters);
		}
	}

}
