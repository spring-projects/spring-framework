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

package org.springframework.beans;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Common delegate methods for Spring's internal {@link PropertyDescriptor} implementations.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 */
abstract class PropertyDescriptorUtils {

	public static final PropertyDescriptor[] EMPTY_PROPERTY_DESCRIPTOR_ARRAY = {};


	/**
	 * Simple introspection algorithm for basic set/get/is accessor methods,
	 * building corresponding JavaBeans property descriptors for them.
	 * <p>This just supports the basic JavaBeans conventions, without indexed
	 * properties or any customizers, and without other BeanInfo metadata.
	 * For standard JavaBeans introspection, use the JavaBeans Introspector.
	 * @param beanClass the target class to introspect
	 * @return a collection of property descriptors
	 * @throws IntrospectionException from introspecting the given bean class
	 * @since 5.3.24
	 * @see SimpleBeanInfoFactory
	 * @see java.beans.Introspector#getBeanInfo(Class)
	 */
	public static Collection<? extends PropertyDescriptor> determineBasicProperties(Class<?> beanClass)
			throws IntrospectionException {

		Map<String, BasicPropertyDescriptor> pdMap = new TreeMap<>();

		for (Method method : beanClass.getMethods()) {
			String methodName = method.getName();

			boolean setter;
			int nameIndex;
			if (methodName.startsWith("set") && method.getParameterCount() == 1) {
				setter = true;
				nameIndex = 3;
			}
			else if (methodName.startsWith("get") && method.getParameterCount() == 0 && method.getReturnType() != Void.TYPE) {
				setter = false;
				nameIndex = 3;
			}
			else if (methodName.startsWith("is") && method.getParameterCount() == 0 && method.getReturnType() == boolean.class) {
				setter = false;
				nameIndex = 2;
			}
			else {
				continue;
			}

			String propertyName = StringUtils.uncapitalizeAsProperty(methodName.substring(nameIndex));
			if (propertyName.isEmpty()) {
				continue;
			}

			BasicPropertyDescriptor pd = pdMap.get(propertyName);
			if (pd != null) {
				if (setter) {
					Method writeMethod = pd.getWriteMethod();
					if (writeMethod == null ||
							writeMethod.getParameterTypes()[0].isAssignableFrom(method.getParameterTypes()[0])) {
						pd.setWriteMethod(method);
					}
					else {
						pd.addWriteMethod(method);
					}
				}
				else {
					Method readMethod = pd.getReadMethod();
					if (readMethod == null ||
							(readMethod.getReturnType() == method.getReturnType() && method.getName().startsWith("is"))) {
						pd.setReadMethod(method);
					}
				}
			}
			else {
				pd = new BasicPropertyDescriptor(propertyName, (!setter ? method : null), (setter ? method : null));
				pdMap.put(propertyName, pd);
			}
		}

		return pdMap.values();
	}

	/**
	 * See {@link java.beans.FeatureDescriptor}.
	 */
	public static void copyNonMethodProperties(PropertyDescriptor source, PropertyDescriptor target) {
		target.setExpert(source.isExpert());
		target.setHidden(source.isHidden());
		target.setPreferred(source.isPreferred());
		target.setName(source.getName());
		target.setShortDescription(source.getShortDescription());
		target.setDisplayName(source.getDisplayName());

		// Copy all attributes (emulating behavior of private FeatureDescriptor#addTable)
		Enumeration<String> keys = source.attributeNames();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			target.setValue(key, source.getValue(key));
		}

		// See java.beans.PropertyDescriptor#PropertyDescriptor(PropertyDescriptor)
		target.setPropertyEditorClass(source.getPropertyEditorClass());
		target.setBound(source.isBound());
		target.setConstrained(source.isConstrained());
	}

	/**
	 * See {@link java.beans.PropertyDescriptor#findPropertyType}.
	 */
	@Nullable
	public static Class<?> findPropertyType(@Nullable Method readMethod, @Nullable Method writeMethod)
			throws IntrospectionException {

		Class<?> propertyType = null;

		if (readMethod != null) {
			if (readMethod.getParameterCount() != 0) {
				throw new IntrospectionException("Bad read method arg count: " + readMethod);
			}
			propertyType = readMethod.getReturnType();
			if (propertyType == Void.TYPE) {
				throw new IntrospectionException("Read method returns void: " + readMethod);
			}
		}

		if (writeMethod != null) {
			Class<?>[] params = writeMethod.getParameterTypes();
			if (params.length != 1) {
				throw new IntrospectionException("Bad write method arg count: " + writeMethod);
			}
			if (propertyType != null) {
				if (propertyType.isAssignableFrom(params[0])) {
					// Write method's property type potentially more specific
					propertyType = params[0];
				}
				else if (params[0].isAssignableFrom(propertyType)) {
					// Proceed with read method's property type
				}
				else {
					throw new IntrospectionException(
							"Type mismatch between read and write methods: " + readMethod + " - " + writeMethod);
				}
			}
			else {
				propertyType = params[0];
			}
		}

		return propertyType;
	}

	/**
	 * See {@link java.beans.IndexedPropertyDescriptor#findIndexedPropertyType}.
	 */
	@Nullable
	public static Class<?> findIndexedPropertyType(String name, @Nullable Class<?> propertyType,
			@Nullable Method indexedReadMethod, @Nullable Method indexedWriteMethod) throws IntrospectionException {

		Class<?> indexedPropertyType = null;

		if (indexedReadMethod != null) {
			Class<?>[] params = indexedReadMethod.getParameterTypes();
			if (params.length != 1) {
				throw new IntrospectionException("Bad indexed read method arg count: " + indexedReadMethod);
			}
			if (params[0] != Integer.TYPE) {
				throw new IntrospectionException("Non int index to indexed read method: " + indexedReadMethod);
			}
			indexedPropertyType = indexedReadMethod.getReturnType();
			if (indexedPropertyType == Void.TYPE) {
				throw new IntrospectionException("Indexed read method returns void: " + indexedReadMethod);
			}
		}

		if (indexedWriteMethod != null) {
			Class<?>[] params = indexedWriteMethod.getParameterTypes();
			if (params.length != 2) {
				throw new IntrospectionException("Bad indexed write method arg count: " + indexedWriteMethod);
			}
			if (params[0] != Integer.TYPE) {
				throw new IntrospectionException("Non int index to indexed write method: " + indexedWriteMethod);
			}
			if (indexedPropertyType != null) {
				if (indexedPropertyType.isAssignableFrom(params[1])) {
					// Write method's property type potentially more specific
					indexedPropertyType = params[1];
				}
				else if (params[1].isAssignableFrom(indexedPropertyType)) {
					// Proceed with read method's property type
				}
				else {
					throw new IntrospectionException("Type mismatch between indexed read and write methods: " +
							indexedReadMethod + " - " + indexedWriteMethod);
				}
			}
			else {
				indexedPropertyType = params[1];
			}
		}

		if (propertyType != null && (!propertyType.isArray() ||
				propertyType.componentType() != indexedPropertyType)) {
			throw new IntrospectionException("Type mismatch between indexed and non-indexed methods: " +
					indexedReadMethod + " - " + indexedWriteMethod);
		}

		return indexedPropertyType;
	}

	/**
	 * Compare the given {@code PropertyDescriptors} and return {@code true} if
	 * they are equivalent, i.e. their read method, write method, property type,
	 * property editor and flags are equivalent.
	 * @see java.beans.PropertyDescriptor#equals(Object)
	 */
	public static boolean equals(PropertyDescriptor pd, PropertyDescriptor otherPd) {
		return (ObjectUtils.nullSafeEquals(pd.getReadMethod(), otherPd.getReadMethod()) &&
				ObjectUtils.nullSafeEquals(pd.getWriteMethod(), otherPd.getWriteMethod()) &&
				ObjectUtils.nullSafeEquals(pd.getPropertyType(), otherPd.getPropertyType()) &&
				ObjectUtils.nullSafeEquals(pd.getPropertyEditorClass(), otherPd.getPropertyEditorClass()) &&
				pd.isBound() == otherPd.isBound() && pd.isConstrained() == otherPd.isConstrained());
	}


	/**
	 * PropertyDescriptor for {@link #determineBasicProperties(Class)},
	 * not performing any early type determination for
	 * {@link #setReadMethod}/{@link #setWriteMethod}.
	 * @since 5.3.24
	 */
	private static class BasicPropertyDescriptor extends PropertyDescriptor {

		@Nullable
		private Method readMethod;

		@Nullable
		private Method writeMethod;

		private final List<Method> alternativeWriteMethods = new ArrayList<>();

		public BasicPropertyDescriptor(String propertyName, @Nullable Method readMethod, @Nullable Method writeMethod)
				throws IntrospectionException {

			super(propertyName, readMethod, writeMethod);
		}

		@Override
		public void setReadMethod(@Nullable Method readMethod) {
			this.readMethod = readMethod;
		}

		@Override
		@Nullable
		public Method getReadMethod() {
			return this.readMethod;
		}

		@Override
		public void setWriteMethod(@Nullable Method writeMethod) {
			this.writeMethod = writeMethod;
		}

		public void addWriteMethod(Method writeMethod) {
			if (this.writeMethod != null) {
				this.alternativeWriteMethods.add(this.writeMethod);
				this.writeMethod = null;
			}
			this.alternativeWriteMethods.add(writeMethod);
		}

		@Override
		@Nullable
		public Method getWriteMethod() {
			if (this.writeMethod == null && !this.alternativeWriteMethods.isEmpty()) {
				if (this.readMethod == null) {
					return this.alternativeWriteMethods.get(0);
				}
				else {
					for (Method method : this.alternativeWriteMethods) {
						if (this.readMethod.getReturnType().isAssignableFrom(method.getParameterTypes()[0])) {
							this.writeMethod = method;
							break;
						}
					}
				}
			}
			return this.writeMethod;
		}
	}


}
