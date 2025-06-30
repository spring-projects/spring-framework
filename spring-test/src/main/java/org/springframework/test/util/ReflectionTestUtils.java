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

package org.springframework.test.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.support.AopUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MethodInvoker;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@code ReflectionTestUtils} is a collection of reflection-based utility
 * methods for use in unit and integration testing scenarios.
 *
 * <p>There are often times when it would be beneficial to be able to set a
 * non-{@code public} field, invoke a non-{@code public} setter method, or
 * invoke a non-{@code public} <em>configuration</em> or <em>lifecycle</em>
 * callback method when testing code involving, for example:
 *
 * <ul>
 * <li>ORM frameworks such as JPA and Hibernate which condone the usage of
 * {@code private} or {@code protected} field access as opposed to
 * {@code public} setter methods for properties in a domain entity.</li>
 * <li>Spring's support for annotations such as
 * {@link org.springframework.beans.factory.annotation.Autowired @Autowired},
 * {@link jakarta.inject.Inject @Inject}, and
 * {@link jakarta.annotation.Resource @Resource} which provides dependency
 * injection for {@code private} or {@code protected} fields, setter methods,
 * and configuration methods.</li>
 * <li>Use of annotations such as {@link jakarta.annotation.PostConstruct @PostConstruct}
 * and {@link jakarta.annotation.PreDestroy @PreDestroy} for lifecycle callback
 * methods.</li>
 * </ul>
 *
 * <p>In addition, several methods in this class provide support for {@code static}
 * fields and {@code static} methods &mdash; for example,
 * {@link #setField(Class, String, Object)}, {@link #getField(Class, String)},
 * {@link #invokeMethod(Class, String, Object...)},
 * {@link #invokeMethod(Object, Class, String, Object...)}, etc.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see ReflectionUtils
 * @see AopTestUtils
 */
public abstract class ReflectionTestUtils {

	private static final String SETTER_PREFIX = "set";

	private static final String GETTER_PREFIX = "get";

	private static final Log logger = LogFactory.getLog(ReflectionTestUtils.class);

	private static final boolean springAopPresent = ClassUtils.isPresent(
			"org.springframework.aop.framework.Advised", ReflectionTestUtils.class.getClassLoader());


	/**
	 * Set the {@linkplain Field field} with the given {@code name} on the
	 * provided {@code targetObject} to the supplied {@code value}.
	 * <p>This method delegates to {@link #setField(Object, String, Object, Class)},
	 * supplying {@code null} for the {@code type} argument.
	 * @param targetObject the target object on which to set the field; never {@code null}
	 * @param name the name of the field to set; never {@code null}
	 * @param value the value to set
	 */
	public static void setField(Object targetObject, String name, @Nullable Object value) {
		setField(targetObject, name, value, null);
	}

	/**
	 * Set the {@linkplain Field field} with the given {@code name}/{@code type}
	 * on the provided {@code targetObject} to the supplied {@code value}.
	 * <p>This method delegates to {@link #setField(Object, Class, String, Object, Class)},
	 * supplying {@code null} for the {@code targetClass} argument.
	 * @param targetObject the target object on which to set the field; never {@code null}
	 * @param name the name of the field to set; may be {@code null} if
	 * {@code type} is specified
	 * @param value the value to set
	 * @param type the type of the field to set; may be {@code null} if
	 * {@code name} is specified
	 */
	public static void setField(Object targetObject, @Nullable String name, @Nullable Object value, @Nullable Class<?> type) {
		setField(targetObject, null, name, value, type);
	}

	/**
	 * Set the static {@linkplain Field field} with the given {@code name} on
	 * the provided {@code targetClass} to the supplied {@code value}.
	 * <p>This method delegates to {@link #setField(Object, Class, String, Object, Class)},
	 * supplying {@code null} for the {@code targetObject} and {@code type} arguments.
	 * <p>This method does not support setting {@code static final} fields.
	 * @param targetClass the target class on which to set the static field;
	 * never {@code null}
	 * @param name the name of the field to set; never {@code null}
	 * @param value the value to set
	 * @since 4.2
	 */
	public static void setField(Class<?> targetClass, String name, @Nullable Object value) {
		setField(null, targetClass, name, value, null);
	}

	/**
	 * Set the static {@linkplain Field field} with the given
	 * {@code name}/{@code type} on the provided {@code targetClass} to
	 * the supplied {@code value}.
	 * <p>This method delegates to {@link #setField(Object, Class, String, Object, Class)},
	 * supplying {@code null} for the {@code targetObject} argument.
	 * <p>This method does not support setting {@code static final} fields.
	 * @param targetClass the target class on which to set the static field;
	 * never {@code null}
	 * @param name the name of the field to set; may be {@code null} if
	 * {@code type} is specified
	 * @param value the value to set
	 * @param type the type of the field to set; may be {@code null} if
	 * {@code name} is specified
	 * @since 4.2
	 */
	public static void setField(
			Class<?> targetClass, @Nullable String name, @Nullable Object value, @Nullable Class<?> type) {

		setField(null, targetClass, name, value, type);
	}

	/**
	 * Set the {@linkplain Field field} with the given {@code name}/{@code type}
	 * on the provided {@code targetObject}/{@code targetClass} to the supplied
	 * {@code value}.
	 * <p>If the supplied {@code targetObject} is a <em>proxy</em>, it will
	 * be {@linkplain AopTestUtils#getUltimateTargetObject unwrapped} allowing
	 * the field to be set on the ultimate target of the proxy.
	 * <p>This method traverses the class hierarchy in search of the desired
	 * field. In addition, an attempt will be made to make non-{@code public}
	 * fields <em>accessible</em>, thus allowing one to set {@code protected},
	 * {@code private}, and <em>package-private</em> fields.
	 * <p>This method does not support setting {@code static final} fields.
	 * @param targetObject the target object on which to set the field; may be
	 * {@code null} if the field is static
	 * @param targetClass the target class on which to set the field; may
	 * be {@code null} if the field is an instance field
	 * @param name the name of the field to set; may be {@code null} if
	 * {@code type} is specified
	 * @param value the value to set
	 * @param type the type of the field to set; may be {@code null} if
	 * {@code name} is specified
	 * @since 4.2
	 * @see ReflectionUtils#findField(Class, String, Class)
	 * @see ReflectionUtils#makeAccessible(Field)
	 * @see ReflectionUtils#setField(Field, Object, Object)
	 * @see AopTestUtils#getUltimateTargetObject(Object)
	 */
	@SuppressWarnings("NullAway") // Dataflow analysis limitation
	public static void setField(@Nullable Object targetObject, @Nullable Class<?> targetClass,
			@Nullable String name, @Nullable Object value, @Nullable Class<?> type) {

		Assert.isTrue(targetObject != null || targetClass != null,
				"Either targetObject or targetClass for the field must be specified");

		if (targetObject != null && springAopPresent) {
			targetObject = AopTestUtils.getUltimateTargetObject(targetObject);
		}
		if (targetClass == null) {
			targetClass = targetObject.getClass();
		}

		Field field = ReflectionUtils.findField(targetClass, name, type);
		if (field == null) {
			throw new IllegalArgumentException(String.format(
					"Could not find field '%s' of type [%s] on %s or target class [%s]", name, type,
					safeToString(targetObject), targetClass));
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format(
					"Setting field '%s' of type [%s] on %s or target class [%s] to value [%s]", name, type,
					safeToString(targetObject), targetClass, value));
		}
		ReflectionUtils.makeAccessible(field);
		ReflectionUtils.setField(field, targetObject, value);
	}

	/**
	 * Get the value of the {@linkplain Field field} with the given {@code name}
	 * from the provided {@code targetObject}.
	 * <p>This method delegates to {@link #getField(Object, Class, String)},
	 * supplying {@code null} for the {@code targetClass} argument.
	 * @param targetObject the target object from which to get the field;
	 * never {@code null}
	 * @param name the name of the field to get; never {@code null}
	 * @return the field's current value
	 * @see #getField(Class, String)
	 */
	public static @Nullable Object getField(Object targetObject, String name) {
		return getField(targetObject, null, name);
	}

	/**
	 * Get the value of the static {@linkplain Field field} with the given
	 * {@code name} from the provided {@code targetClass}.
	 * <p>This method delegates to {@link #getField(Object, Class, String)},
	 * supplying {@code null} for the {@code targetObject} argument.
	 * @param targetClass the target class from which to get the static field;
	 * never {@code null}
	 * @param name the name of the field to get; never {@code null}
	 * @return the field's current value
	 * @since 4.2
	 * @see #getField(Object, String)
	 */
	public static @Nullable Object getField(Class<?> targetClass, String name) {
		return getField(null, targetClass, name);
	}

	/**
	 * Get the value of the {@linkplain Field field} with the given {@code name}
	 * from the provided {@code targetObject}/{@code targetClass}.
	 * <p>If the supplied {@code targetObject} is a <em>proxy</em>, it will
	 * be {@linkplain AopTestUtils#getUltimateTargetObject unwrapped} allowing
	 * the field to be retrieved from the ultimate target of the proxy.
	 * <p>This method traverses the class hierarchy in search of the desired
	 * field. In addition, an attempt will be made to make non-{@code public}
	 * fields <em>accessible</em>, thus allowing one to get {@code protected},
	 * {@code private}, and <em>package-private</em> fields.
	 * @param targetObject the target object from which to get the field; may be
	 * {@code null} if the field is static
	 * @param targetClass the target class from which to get the field; may
	 * be {@code null} if the field is an instance field
	 * @param name the name of the field to get; never {@code null}
	 * @return the field's current value
	 * @since 4.2
	 * @see #getField(Object, String)
	 * @see #getField(Class, String)
	 * @see ReflectionUtils#findField(Class, String, Class)
	 * @see ReflectionUtils#makeAccessible(Field)
	 * @see ReflectionUtils#getField(Field, Object)
	 * @see AopTestUtils#getUltimateTargetObject(Object)
	 */
	@SuppressWarnings("NullAway") // Dataflow analysis limitation
	public static @Nullable Object getField(@Nullable Object targetObject, @Nullable Class<?> targetClass, String name) {
		Assert.isTrue(targetObject != null || targetClass != null,
			"Either targetObject or targetClass for the field must be specified");

		if (targetObject != null && springAopPresent) {
			targetObject = AopTestUtils.getUltimateTargetObject(targetObject);
		}
		if (targetClass == null) {
			targetClass = targetObject.getClass();
		}

		Field field = ReflectionUtils.findField(targetClass, name);
		if (field == null) {
			throw new IllegalArgumentException(String.format("Could not find field '%s' on %s or target class [%s]",
					name, safeToString(targetObject), targetClass));
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Getting field '%s' from %s or target class [%s]", name,
					safeToString(targetObject), targetClass));
		}
		ReflectionUtils.makeAccessible(field);
		return ReflectionUtils.getField(field, targetObject);
	}

	/**
	 * Invoke the setter method with the given {@code name} on the supplied
	 * target object with the supplied {@code value}.
	 * <p>This method delegates to
	 * {@link #invokeSetterMethod(Object, String, Object, Class)}, supplying
	 * {@code null} for the parameter type.
	 * @param target the target object on which to invoke the specified setter
	 * method
	 * @param name the name of the setter method to invoke or the corresponding
	 * property name
	 * @param value the value to provide to the setter method
	 */
	public static void invokeSetterMethod(Object target, String name, Object value) {
		invokeSetterMethod(target, name, value, null);
	}

	/**
	 * Invoke the setter method with the given {@code name} on the supplied
	 * target object with the supplied {@code value}.
	 * <p>This method traverses the class hierarchy in search of the desired
	 * method. In addition, an attempt will be made to make non-{@code public}
	 * methods <em>accessible</em>, thus allowing one to invoke {@code protected},
	 * {@code private}, and <em>package-private</em> setter methods.
	 * <p>This method also supports JavaBean-style <em>property</em> names. For
	 * example, if you wish to set the {@code name} property on the target object,
	 * you may pass either {@code "name"} or {@code "setName"} as the method name.
	 * <p>As of Spring Framework 6.2, if the supplied target object is a CGLIB
	 * proxy which does not intercept the setter method, the proxy will be
	 * {@linkplain AopTestUtils#getUltimateTargetObject unwrapped} allowing the
	 * setter method to be invoked directly on the ultimate target of the proxy.
	 * @param target the target object on which to invoke the specified setter
	 * method
	 * @param name the name of the setter method to invoke or the corresponding
	 * property name
	 * @param value the value to provide to the setter method
	 * @param type the formal parameter type declared by the setter method
	 * (may be {@code null} to indicate any type)
	 * @see ReflectionUtils#findMethod(Class, String, Class[])
	 * @see ReflectionUtils#makeAccessible(Method)
	 * @see ReflectionUtils#invokeMethod(Method, Object, Object[])
	 * @see AopTestUtils#getUltimateTargetObject(Object)
	 */
	public static void invokeSetterMethod(Object target, String name, @Nullable Object value, @Nullable Class<?> type) {
		Assert.notNull(target, "Target object must not be null");
		Assert.hasText(name, "Method name must not be empty");
		Class<?>[] paramTypes = (type != null ? new Class<?>[] {type} : null);

		String setterMethodName = name;
		if (!name.startsWith(SETTER_PREFIX)) {
			setterMethodName = SETTER_PREFIX + StringUtils.capitalize(name);
		}

		Method method = ReflectionUtils.findMethod(target.getClass(), setterMethodName, paramTypes);
		if (method == null && !setterMethodName.equals(name)) {
			setterMethodName = name;
			method = ReflectionUtils.findMethod(target.getClass(), setterMethodName, paramTypes);
		}
		if (method == null) {
			throw new IllegalArgumentException(String.format(
					"Could not find setter method '%s' on %s with parameter type [%s]", setterMethodName,
					safeToString(target), type));
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Invoking setter method '%s' on %s with value [%s]", setterMethodName,
					safeToString(target), value));
		}

		if (springAopPresent) {
			// If the target is a CGLIB proxy which does not intercept the method, invoke the
			// method on the ultimate target.
			if (isCglibProxyThatDoesNotInterceptMethod(target, method)) {
				target = AopTestUtils.getUltimateTargetObject(target);
			}
		}

		ReflectionUtils.makeAccessible(method);
		ReflectionUtils.invokeMethod(method, target, value);
	}

	/**
	 * Invoke the getter method with the given {@code name} on the supplied
	 * target object with the supplied {@code value}.
	 * <p>This method traverses the class hierarchy in search of the desired
	 * method. In addition, an attempt will be made to make non-{@code public}
	 * methods <em>accessible</em>, thus allowing one to invoke {@code protected},
	 * {@code private}, and <em>package-private</em> getter methods.
	 * <p>This method also supports JavaBean-style <em>property</em> names. For
	 * example, if you wish to get the {@code name} property on the target object,
	 * you may pass either {@code "name"} or {@code "getName"} as the method name.
	 * <p>As of Spring Framework 6.2, if the supplied target object is a CGLIB
	 * proxy which does not intercept the getter method, the proxy will be
	 * {@linkplain AopTestUtils#getUltimateTargetObject unwrapped} allowing the
	 * getter method to be invoked directly on the ultimate target of the proxy.
	 * @param target the target object on which to invoke the specified getter
	 * method
	 * @param name the name of the getter method to invoke or the corresponding
	 * property name
	 * @return the value returned from the invocation
	 * @see ReflectionUtils#findMethod(Class, String, Class[])
	 * @see ReflectionUtils#makeAccessible(Method)
	 * @see ReflectionUtils#invokeMethod(Method, Object, Object[])
	 * @see AopTestUtils#getUltimateTargetObject(Object)
	 */
	public static @Nullable Object invokeGetterMethod(Object target, String name) {
		Assert.notNull(target, "Target object must not be null");
		Assert.hasText(name, "Method name must not be empty");

		String getterMethodName = name;
		if (!name.startsWith(GETTER_PREFIX)) {
			getterMethodName = GETTER_PREFIX + StringUtils.capitalize(name);
		}
		Method method = ReflectionUtils.findMethod(target.getClass(), getterMethodName);
		if (method == null && !getterMethodName.equals(name)) {
			getterMethodName = name;
			method = ReflectionUtils.findMethod(target.getClass(), getterMethodName);
		}
		if (method == null) {
			throw new IllegalArgumentException(String.format(
					"Could not find getter method '%s' on %s", getterMethodName, safeToString(target)));
		}

		if (springAopPresent) {
			// If the target is a CGLIB proxy which does not intercept the method, invoke the
			// method on the ultimate target.
			if (isCglibProxyThatDoesNotInterceptMethod(target, method)) {
				target = AopTestUtils.getUltimateTargetObject(target);
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Invoking getter method '%s' on %s", getterMethodName, safeToString(target)));
		}
		ReflectionUtils.makeAccessible(method);
		return ReflectionUtils.invokeMethod(method, target);
	}

	/**
	 * Invoke the method with the given {@code name} on the supplied target
	 * object with the supplied arguments.
	 * <p>This method delegates to {@link #invokeMethod(Object, Class, String, Object...)},
	 * supplying {@code null} for the {@code targetClass} argument.
	 * @param target the target object on which to invoke the specified method
	 * @param name the name of the method to invoke
	 * @param args the arguments to provide to the method
	 * @return the invocation result, if any
	 * @see #invokeMethod(Class, String, Object...)
	 * @see #invokeMethod(Object, Class, String, Object...)
	 */
	public static <T> @Nullable T invokeMethod(Object target, String name, Object... args) {
		Assert.notNull(target, "Target object must not be null");
		return invokeMethod(target, null, name, args);
	}

	/**
	 * Invoke the static method with the given {@code name} on the supplied target
	 * class with the supplied arguments.
	 * <p>This method delegates to {@link #invokeMethod(Object, Class, String, Object...)},
	 * supplying {@code null} for the {@code targetObject} argument.
	 * @param targetClass the target class on which to invoke the specified method
	 * @param name the name of the method to invoke
	 * @param args the arguments to provide to the method
	 * @return the invocation result, if any
	 * @since 5.2
	 * @see #invokeMethod(Object, String, Object...)
	 * @see #invokeMethod(Object, Class, String, Object...)
	 */
	public static <T> @Nullable T invokeMethod(Class<?> targetClass, String name, Object... args) {
		Assert.notNull(targetClass, "Target class must not be null");
		return invokeMethod(null, targetClass, name, args);
	}

	/**
	 * Invoke the method with the given {@code name} on the provided
	 * {@code targetObject}/{@code targetClass} with the supplied arguments.
	 * <p>This method traverses the class hierarchy in search of the desired
	 * method. In addition, an attempt will be made to make non-{@code public}
	 * methods <em>accessible</em>, thus allowing one to invoke {@code protected},
	 * {@code private}, and <em>package-private</em> methods.
	 * <p>As of Spring Framework 6.2, if the supplied target object is a CGLIB
	 * proxy which does not intercept the method, the proxy will be
	 * {@linkplain AopTestUtils#getUltimateTargetObject unwrapped} allowing the
	 * method to be invoked directly on the ultimate target of the proxy.
	 * @param targetObject the target object on which to invoke the method; may
	 * be {@code null} if the method is static
	 * @param targetClass the target class on which to invoke the method; may
	 * be {@code null} if the method is an instance method
	 * @param name the name of the method to invoke
	 * @param args the arguments to provide to the method
	 * @return the invocation result, if any
	 * @since 5.2
	 * @see #invokeMethod(Object, String, Object...)
	 * @see #invokeMethod(Class, String, Object...)
	 * @see MethodInvoker
	 * @see ReflectionUtils#makeAccessible(Method)
	 * @see ReflectionUtils#handleReflectionException(Exception)
	 * @see AopTestUtils#getUltimateTargetObject(Object)
	 */
	@SuppressWarnings("unchecked")
	public static <T> @Nullable T invokeMethod(@Nullable Object targetObject, @Nullable Class<?> targetClass, String name,
			Object... args) {

		Assert.isTrue(targetObject != null || targetClass != null,
				"Either 'targetObject' or 'targetClass' for the method must be specified");
		Assert.hasText(name, "Method name must not be empty");

		try {
			MethodInvoker methodInvoker = new MethodInvoker();
			methodInvoker.setTargetObject(targetObject);
			if (targetClass != null) {
				methodInvoker.setTargetClass(targetClass);
			}
			methodInvoker.setTargetMethod(name);
			methodInvoker.setArguments(args);
			methodInvoker.prepare();

			if (targetObject != null && springAopPresent) {
				// If the target is a CGLIB proxy which does not intercept the method, invoke the
				// method on the ultimate target.
				if (isCglibProxyThatDoesNotInterceptMethod(targetObject, methodInvoker.getPreparedMethod())) {
					targetObject = AopTestUtils.getUltimateTargetObject(targetObject);
					methodInvoker.setTargetObject(targetObject);
				}
			}

			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Invoking method '%s' on %s or %s with arguments %s", name,
						safeToString(targetObject), safeToString(targetClass), ObjectUtils.nullSafeToString(args)));
			}

			return (T) methodInvoker.invoke();
		}
		catch (Exception ex) {
			ReflectionUtils.handleReflectionException(ex);
			throw new IllegalStateException("Should never get here");
		}
	}

	private static String safeToString(@Nullable Object target) {
		try {
			return String.format("target object [%s]", target);
		}
		catch (Exception ex) {
			return String.format("target of type [%s] whose toString() method threw [%s]",
					(target != null ? target.getClass().getName() : "unknown"), ex);
		}
	}

	private static String safeToString(@Nullable Class<?> clazz) {
		return String.format("target class [%s]", (clazz != null ? clazz.getName() : null));
	}

	/**
	 * Determine if the supplied target object is a CBLIB proxy that does not intercept the
	 * supplied method.
	 * @since 6.2
	 */
	private static boolean isCglibProxyThatDoesNotInterceptMethod(Object target, Method method) {
		return (AopUtils.isCglibProxy(target) && !method.getDeclaringClass().equals(target.getClass()));
	}

}
