/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.test.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * <p>
 * ReflectionTestUtils is a collection of reflection-based utility methods for
 * use in unit and integration testing scenarios.
 * </p>
 * <p>
 * There are often situations in which it would be beneficial to be able to set
 * a non-<code>public</code> field or invoke a non-<code>public</code> setter
 * method when testing code involving, for example:
 * </p>
 * <ul>
 * <li>ORM frameworks such as JPA and Hibernate which condone the usage of
 * <code>private</code> or <code>protected</code> field access as opposed to
 * <code>public</code> setter methods for properties in a domain entity.</li>
 * <li>Spring's support for annotations such as
 * {@link org.springframework.beans.factory.annotation.Autowired @Autowired} and
 * {@link javax.annotation.Resource @Resource} which provides dependency
 * injection for <code>private</code> or <code>protected</code> fields, setter
 * methods, and configuration methods.</li>
 * </ul>
 * 
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see ReflectionUtils
 */
public class ReflectionTestUtils {

	private static final String SETTER_PREFIX = "set";

	private static final String GETTER_PREFIX = "get";

	private static final Log logger = LogFactory.getLog(ReflectionTestUtils.class);


	/**
	 * Set the {@link Field field} with the given <code>name</code> on the
	 * provided {@link Object target object} to the supplied <code>value</code>.
	 * <p>
	 * This method traverses the class hierarchy in search of the desired field.
	 * In addition, an attempt will be made to make non-<code>public</code>
	 * fields <em>accessible</em>, thus allowing one to set
	 * <code>protected</code>, <code>private</code>, and
	 * <em>package-private</em> fields.
	 * 
	 * @param target the target object on which to set the field
	 * @param name the name of the field to set
	 * @param value the value to set
	 * @see ReflectionUtils#findField(Class, String, Class)
	 * @see ReflectionUtils#makeAccessible(Field)
	 * @see ReflectionUtils#setField(Field, Object, Object)
	 */
	public static void setField(Object target, String name, Object value) {
		setField(target, name, value, null);
	}

	/**
	 * Set the {@link Field field} with the given <code>name</code> on the
	 * provided {@link Object target object} to the supplied <code>value</code>.
	 * <p>
	 * This method traverses the class hierarchy in search of the desired field.
	 * In addition, an attempt will be made to make non-<code>public</code>
	 * fields <em>accessible</em>, thus allowing one to set
	 * <code>protected</code>, <code>private</code>, and
	 * <em>package-private</em> fields.
	 * 
	 * @param target the target object on which to set the field
	 * @param name the name of the field to set
	 * @param value the value to set
	 * @param type the type of the field (may be <code>null</code>)
	 * @see ReflectionUtils#findField(Class, String, Class)
	 * @see ReflectionUtils#makeAccessible(Field)
	 * @see ReflectionUtils#setField(Field, Object, Object)
	 */
	public static void setField(Object target, String name, Object value, Class<?> type) {
		Assert.notNull(target, "Target object must not be null");
		Field field = ReflectionUtils.findField(target.getClass(), name, type);
		if (field == null) {
			throw new IllegalArgumentException("Could not find field [" + name + "] on target [" + target + "]");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Setting field [" + name + "] on target [" + target + "]");
		}
		ReflectionUtils.makeAccessible(field);
		ReflectionUtils.setField(field, target, value);
	}

	/**
	 * Get the field with the given <code>name</code> from the provided target
	 * object.
	 * <p>
	 * This method traverses the class hierarchy in search of the desired field.
	 * In addition, an attempt will be made to make non-<code>public</code>
	 * fields <em>accessible</em>, thus allowing one to get
	 * <code>protected</code>, <code>private</code>, and
	 * <em>package-private</em> fields.
	 * 
	 * @param target the target object on which to set the field
	 * @param name the name of the field to get
	 * @return the field's current value
	 * @see ReflectionUtils#findField(Class, String, Class)
	 * @see ReflectionUtils#makeAccessible(Field)
	 * @see ReflectionUtils#setField(Field, Object, Object)
	 */
	public static Object getField(Object target, String name) {
		Assert.notNull(target, "Target object must not be null");
		Field field = ReflectionUtils.findField(target.getClass(), name);
		if (field == null) {
			throw new IllegalArgumentException("Could not find field [" + name + "] on target [" + target + "]");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Getting field [" + name + "] from target [" + target + "]");
		}
		ReflectionUtils.makeAccessible(field);
		return ReflectionUtils.getField(field, target);
	}

	/**
	 * Invoke the setter method with the given <code>name</code> on the supplied
	 * target object with the supplied <code>value</code>.
	 * <p>
	 * This method traverses the class hierarchy in search of the desired
	 * method. In addition, an attempt will be made to make non-
	 * <code>public</code> methods <em>accessible</em>, thus allowing one to
	 * invoke <code>protected</code>, <code>private</code>, and
	 * <em>package-private</em> setter methods.
	 * <p>
	 * In addition, this method supports JavaBean-style <em>property</em> names.
	 * For example, if you wish to set the <code>name</code> property on the
	 * target object, you may pass either &quot;name&quot; or
	 * &quot;setName&quot; as the method name.
	 * 
	 * @param target the target object on which to invoke the specified setter
	 * method
	 * @param name the name of the setter method to invoke or the corresponding
	 * property name
	 * @param value the value to provide to the setter method
	 * @see ReflectionUtils#findMethod(Class, String, Class[])
	 * @see ReflectionUtils#makeAccessible(Method)
	 * @see ReflectionUtils#invokeMethod(Method, Object, Object[])
	 */
	public static void invokeSetterMethod(Object target, String name, Object value) {
		invokeSetterMethod(target, name, value, null);
	}

	/**
	 * Invoke the setter method with the given <code>name</code> on the supplied
	 * target object with the supplied <code>value</code>.
	 * <p>
	 * This method traverses the class hierarchy in search of the desired
	 * method. In addition, an attempt will be made to make non-
	 * <code>public</code> methods <em>accessible</em>, thus allowing one to
	 * invoke <code>protected</code>, <code>private</code>, and
	 * <em>package-private</em> setter methods.
	 * <p>
	 * In addition, this method supports JavaBean-style <em>property</em> names.
	 * For example, if you wish to set the <code>name</code> property on the
	 * target object, you may pass either &quot;name&quot; or
	 * &quot;setName&quot; as the method name.
	 * 
	 * @param target the target object on which to invoke the specified setter
	 * method
	 * @param name the name of the setter method to invoke or the corresponding
	 * property name
	 * @param value the value to provide to the setter method
	 * @param type the formal parameter type declared by the setter method
	 * @see ReflectionUtils#findMethod(Class, String, Class[])
	 * @see ReflectionUtils#makeAccessible(Method)
	 * @see ReflectionUtils#invokeMethod(Method, Object, Object[])
	 */
	public static void invokeSetterMethod(Object target, String name, Object value, Class<?> type) {
		Assert.notNull(target, "Target object must not be null");
		Assert.notNull(name, "Method name must not be empty");
		Class<?>[] paramTypes = (type != null ? new Class<?>[] { type } : null);

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
			throw new IllegalArgumentException("Could not find setter method [" + setterMethodName + "] on target ["
					+ target + "] with parameter type [" + type + "]");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Invoking setter method [" + setterMethodName + "] on target [" + target + "]");
		}
		ReflectionUtils.makeAccessible(method);
		ReflectionUtils.invokeMethod(method, target, new Object[] { value });
	}

	/**
	 * Invoke the getter method with the given <code>name</code> on the supplied
	 * target object with the supplied <code>value</code>.
	 * <p>
	 * This method traverses the class hierarchy in search of the desired
	 * method. In addition, an attempt will be made to make non-
	 * <code>public</code> methods <em>accessible</em>, thus allowing one to
	 * invoke <code>protected</code>, <code>private</code>, and
	 * <em>package-private</em> getter methods.
	 * <p>
	 * In addition, this method supports JavaBean-style <em>property</em> names.
	 * For example, if you wish to get the <code>name</code> property on the
	 * target object, you may pass either &quot;name&quot; or
	 * &quot;getName&quot; as the method name.
	 * 
	 * @param target the target object on which to invoke the specified getter
	 * method
	 * @param name the name of the getter method to invoke or the corresponding
	 * property name
	 * @return the value returned from the invocation
	 * @see ReflectionUtils#findMethod(Class, String, Class[])
	 * @see ReflectionUtils#makeAccessible(Method)
	 * @see ReflectionUtils#invokeMethod(Method, Object, Object[])
	 */
	public static Object invokeGetterMethod(Object target, String name) {
		Assert.notNull(target, "Target object must not be null");
		Assert.notNull(name, "Method name must not be empty");

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
			throw new IllegalArgumentException("Could not find getter method [" + getterMethodName + "] on target ["
					+ target + "]");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Invoking getter method [" + getterMethodName + "] on target [" + target + "]");
		}
		ReflectionUtils.makeAccessible(method);
		return ReflectionUtils.invokeMethod(method, target);
	}

}
