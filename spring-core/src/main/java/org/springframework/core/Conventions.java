/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core;

import java.io.Externalizable;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Provides methods to support various naming and other conventions used
 * throughout the framework. Mainly for internal use within the framework.
 * <p> 提供方法去支持不通的命名和其他用于该framewokr,主要用于内部使用
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public abstract class Conventions {

	/**
	 * Suffix added to names when using arrays.
	 * 后缀添加到名字,当使用list时
	 */
	private static final String PLURAL_SUFFIX = "List";

	/**
	 * Set of interfaces that are supposed to be ignored
	 * when searching for the 'primary' interface of a proxy.
	 * 
	 * 应该被忽略的接口的集合,当查询代理的主要接口
	 */
	private static final Set<Class<?>> IGNORED_INTERFACES;
	static {
		IGNORED_INTERFACES = Collections.unmodifiableSet(
				new HashSet<>(Arrays.<Class<?>>asList(
						Serializable.class,
						Externalizable.class,
						Cloneable.class,
						Comparable.class)));
	}


	/**
	 * Determine the conventional variable name for the supplied
	 * {@code Object} based on its concrete type. The convention
	 * used is to return the uncapitalized short name of the {@code Class},
	 * according to JavaBeans property naming rules: So,
	 * {@code com.myapp.Product} becomes {@code product};
	 * {@code com.myapp.MyProduct} becomes {@code myProduct};
	 * {@code com.myapp.UKProduct} becomes {@code UKProduct}.
	 * <p>For arrays, we use the pluralized version of the array component type.
	 * For {@code Collection}s we attempt to 'peek ahead' in the
	 * {@code Collection} to determine the component type and
	 * return the pluralized version of that component type.
	 * <p>
	 * @param value the value to generate a variable name for
	 * @return the generated variable name
	 */
	public static String getVariableName(Object value) {
		Assert.notNull(value, "Value must not be null");
		Class<?> valueClass;
		boolean pluralize = false;

		if (value.getClass().isArray()) {
			valueClass = value.getClass().getComponentType();
			pluralize = true;
		}
		else if (value instanceof Collection) {
			Collection<?> collection = (Collection<?>) value;
			if (collection.isEmpty()) {
				throw new IllegalArgumentException("Cannot generate variable name for an empty Collection");
			}
			Object valueToCheck = peekAhead(collection);
			valueClass = getClassForValue(valueToCheck);
			pluralize = true;
		}
		else {
			valueClass = getClassForValue(value);
		}

		String name = ClassUtils.getShortNameAsProperty(valueClass);
		return (pluralize ? pluralize(name) : name);
	}

	/**
	 * Determine the conventional variable name for the supplied parameter,
	 * taking the generic collection type (if any) into account.
	 * <p> 决定conventional 变量名给根据提供的参数,将通用的集合类型给account
	 * @param parameter the method or constructor parameter to generate a variable name for
	 * @return the generated variable name
	 */
	public static String getVariableNameForParameter(MethodParameter parameter) {
		Assert.notNull(parameter, "MethodParameter must not be null");
		Class<?> valueClass;
		boolean pluralize = false;

		if (parameter.getParameterType().isArray()) {
			valueClass = parameter.getParameterType().getComponentType();
			pluralize = true;
		}
		else if (Collection.class.isAssignableFrom(parameter.getParameterType())) {
			valueClass = GenericCollectionTypeResolver.getCollectionParameterType(parameter);
			if (valueClass == null) {
				throw new IllegalArgumentException(
						"Cannot generate variable name for non-typed Collection parameter type");
			}
			pluralize = true;
		}
		else {
			valueClass = parameter.getParameterType();
		}

		String name = ClassUtils.getShortNameAsProperty(valueClass);
		return (pluralize ? pluralize(name) : name);
	}

	/**
	 * Determine the conventional variable name for the return type of the supplied method,
	 * taking the generic collection type (if any) into account.
	 * <p> 决定conventional 变量名给根据提供的方法返回类型,将通用的集合类型给account
	 * @param method the method to generate a variable name for
	 * @return the generated variable name
	 */
	public static String getVariableNameForReturnType(Method method) {
		return getVariableNameForReturnType(method, method.getReturnType(), null);
	}

	/**
	 * Determine the conventional variable name for the return type of the supplied method,
	 * taking the generic collection type (if any) into account, falling back to the
	 * given return value if the method declaration is not specific enough (i.e. in case of
	 * the return type being declared as {@code Object} or as untyped collection).
	 * 
	 * <p> 决定conventional 变量名给根据提供的方法返回类型,将通用的集合类型给account
	 * @param method the method to generate a variable name for
	 * @param value the return value (may be {@code null} if not available)
	 * @return the generated variable name
	 */
	public static String getVariableNameForReturnType(Method method, Object value) {
		return getVariableNameForReturnType(method, method.getReturnType(), value);
	}

	/**
	 * Determine the conventional variable name for the return type of the supplied method,
	 * taking the generic collection type (if any) into account, falling back to the
	 * given return value if the method declaration is not specific enough (i.e. in case of
	 * the return type being declared as {@code Object} or as untyped collection).
	 * <p> 决定conventional 变量名给根据提供的方法返回类型,将通用的集合类型给account
	 * @param method the method to generate a variable name for
	 * @param resolvedType the resolved return type of the method
	 * @param value the return value (may be {@code null} if not available)
	 * @return the generated variable name
	 */
	public static String getVariableNameForReturnType(Method method, Class<?> resolvedType, Object value) {
		Assert.notNull(method, "Method must not be null");

		if (Object.class == resolvedType) {
			if (value == null) {
				throw new IllegalArgumentException("Cannot generate variable name for an Object return type with null value");
			}
			return getVariableName(value);
		}

		Class<?> valueClass;
		boolean pluralize = false;

		if (resolvedType.isArray()) {
			valueClass = resolvedType.getComponentType();
			pluralize = true;
		}
		else if (Collection.class.isAssignableFrom(resolvedType)) {
			valueClass = GenericCollectionTypeResolver.getCollectionReturnType(method);
			if (valueClass == null) {
				if (!(value instanceof Collection)) {
					throw new IllegalArgumentException(
							"Cannot generate variable name for non-typed Collection return type and a non-Collection value");
				}
				Collection<?> collection = (Collection<?>) value;
				if (collection.isEmpty()) {
					throw new IllegalArgumentException(
							"Cannot generate variable name for non-typed Collection return type and an empty Collection value");
				}
				Object valueToCheck = peekAhead(collection);
				valueClass = getClassForValue(valueToCheck);
			}
			pluralize = true;
		}
		else {
			valueClass = resolvedType;
		}

		String name = ClassUtils.getShortNameAsProperty(valueClass);
		return (pluralize ? pluralize(name) : name);
	}

	/**
	 * Convert {@code String}s in attribute name format (lowercase, hyphens separating words)
	 * into property name format (camel-cased). For example, {@code transaction-manager} is
	 * converted into {@code transactionManager}.
	 * 转换attribute名称到属性名称,去掉-
	 */
	public static String attributeNameToPropertyName(String attributeName) {
		Assert.notNull(attributeName, "'attributeName' must not be null");
		if (!attributeName.contains("-")) {
			return attributeName;
		}
		char[] chars = attributeName.toCharArray();
		char[] result = new char[chars.length -1]; // not completely accurate but good guess
		int currPos = 0;
		boolean upperCaseNext = false;
		for (char c : chars) {
			if (c == '-') {
				upperCaseNext = true;
			}
			else if (upperCaseNext) {
				result[currPos++] = Character.toUpperCase(c);
				upperCaseNext = false;
			}
			else {
				result[currPos++] = c;
			}
		}
		return new String(result, 0, currPos);
	}

	/**
	 * Return an attribute name qualified by the supplied enclosing {@link Class}. For example,
	 * the attribute name '{@code foo}' qualified by {@link Class} '{@code com.myapp.SomeClass}'
	 * would be '{@code com.myapp.SomeClass.foo}'
	 * <p>根据给定的类和属性名,拼接为一个字符串
	 */
	public static String getQualifiedAttributeName(Class<?> enclosingClass, String attributeName) {
		Assert.notNull(enclosingClass, "'enclosingClass' must not be null");
		Assert.notNull(attributeName, "'attributeName' must not be null");
		return enclosingClass.getName() + "." + attributeName;
	}


	/**
	 * Determines the class to use for naming a variable that contains
	 * the given value.
	 * <p>Will return the class of the given value, except when
	 * encountering a JDK proxy, in which case it will determine
	 * the 'primary' interface implemented by that proxy.
	 * <p> 
	 * <p>返回给定类的类名,当为代理类是,返回代理类的第一接口
	 * @param value the value to check
	 * @return the class to use for naming a variable
	 */
	private static Class<?> getClassForValue(Object value) {
		Class<?> valueClass = value.getClass();
		if (Proxy.isProxyClass(valueClass)) {
			Class<?>[] ifcs = valueClass.getInterfaces();
			for (Class<?> ifc : ifcs) {
				if (!IGNORED_INTERFACES.contains(ifc)) {
					return ifc;
				}
			}
		}
		else if (valueClass.getName().lastIndexOf('$') != -1 && valueClass.getDeclaringClass() == null) {
			// '$' in the class name but no inner class -
			// assuming it's a special subclass (e.g. by OpenJPA)
			valueClass = valueClass.getSuperclass();
		}
		return valueClass;
	}

	/**
	 * Pluralize the given name.
	 * 多元化给定的name
	 */
	private static String pluralize(String name) {
		return name + PLURAL_SUFFIX;
	}

	/**
	 * Retrieves the {@code Class} of an element in the {@code Collection}.
	 * The exact element for which the {@code Class} is retreived will depend
	 * on the concrete {@code Collection} implementation.
	 * <p>检索给定类的元素在集合中,返回第一个元素,类的其他元素依赖于concrete的集合实现
	 */
	private static <E> E peekAhead(Collection<E> collection) {
		Iterator<E> it = collection.iterator();
		if (!it.hasNext()) {
			throw new IllegalStateException(
					"Unable to peek ahead in non-empty collection - no element found");
		}
		E value = it.next();
		if (value == null) {
			throw new IllegalStateException(
					"Unable to peek ahead in non-empty collection - only null element found");
		}
		return value;
	}

}
