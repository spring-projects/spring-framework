/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Holder for a typed String value. Can be added to bean definitions
 * in order to explicitly specify a target type for a String value,
 * for example for collection elements.
 *
 * <p>This holder will just store the String value and the target type.
 * The actual conversion will be performed by the bean factory.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see BeanDefinition#getPropertyValues
 * @see org.springframework.beans.MutablePropertyValues#addPropertyValue
 */
public class TypedStringValue implements BeanMetadataElement {

	private String value;

	private volatile Object targetType;

	private Object source;

	private String specifiedTypeName;

	private volatile boolean dynamic;


	/**
	 * Create a new {@link TypedStringValue} for the given String value.
	 * @param value the String value
	 */
	public TypedStringValue(String value) {
		setValue(value);
	}

	/**
	 * Create a new {@link TypedStringValue} for the given String value
	 * and target type.
	 * @param value the String value
	 * @param targetType the type to convert to
	 */
	public TypedStringValue(String value, Class<?> targetType) {
		setValue(value);
		setTargetType(targetType);
	}

	/**
	 * Create a new {@link TypedStringValue} for the given String value
	 * and target type.
	 * @param value the String value
	 * @param targetTypeName the type to convert to
	 */
	public TypedStringValue(String value, String targetTypeName) {
		setValue(value);
		setTargetTypeName(targetTypeName);
	}


	/**
	 * Set the String value.
	 * <p>Only necessary for manipulating a registered value,
	 * for example in BeanFactoryPostProcessors.
	 * @see PropertyPlaceholderConfigurer
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Return the String value.
	 */
	public String getValue() {
		return this.value;
	}

	/**
	 * Set the type to convert to.
	 * <p>Only necessary for manipulating a registered value,
	 * for example in BeanFactoryPostProcessors.
	 * @see PropertyPlaceholderConfigurer
	 */
	public void setTargetType(Class<?> targetType) {
		Assert.notNull(targetType, "'targetType' must not be null");
		this.targetType = targetType;
	}

	/**
	 * Return the type to convert to.
	 */
	public Class<?> getTargetType() {
		Object targetTypeValue = this.targetType;
		if (!(targetTypeValue instanceof Class)) {
			throw new IllegalStateException("Typed String value does not carry a resolved target type");
		}
		return (Class<?>) targetTypeValue;
	}

	/**
	 * Specify the type to convert to.
	 */
	public void setTargetTypeName(String targetTypeName) {
		Assert.notNull(targetTypeName, "'targetTypeName' must not be null");
		this.targetType = targetTypeName;
	}

	/**
	 * Return the type to convert to.
	 */
	public String getTargetTypeName() {
		Object targetTypeValue = this.targetType;
		if (targetTypeValue instanceof Class) {
			return ((Class<?>) targetTypeValue).getName();
		}
		else {
			return (String) targetTypeValue;
		}
	}

	/**
	 * Return whether this typed String value carries a target type .
	 */
	public boolean hasTargetType() {
		return (this.targetType instanceof Class);
	}

	/**
	 * Determine the type to convert to, resolving it from a specified class name
	 * if necessary. Will also reload a specified Class from its name when called
	 * with the target type already resolved.
	 * @param classLoader the ClassLoader to use for resolving a (potential) class name
	 * @return the resolved type to convert to
	 * @throws ClassNotFoundException if the type cannot be resolved
	 */
	public Class<?> resolveTargetType(ClassLoader classLoader) throws ClassNotFoundException {
		if (this.targetType == null) {
			return null;
		}
		Class<?> resolvedClass = ClassUtils.forName(getTargetTypeName(), classLoader);
		this.targetType = resolvedClass;
		return resolvedClass;
	}


	/**
	 * Set the configuration source {@code Object} for this metadata element.
	 * <p>The exact type of the object will depend on the configuration mechanism used.
	 */
	public void setSource(Object source) {
		this.source = source;
	}

	@Override
	public Object getSource() {
		return this.source;
	}

	/**
	 * Set the type name as actually specified for this particular value, if any.
	 */
	public void setSpecifiedTypeName(String specifiedTypeName) {
		this.specifiedTypeName = specifiedTypeName;
	}

	/**
	 * Return the type name as actually specified for this particular value, if any.
	 */
	public String getSpecifiedTypeName() {
		return this.specifiedTypeName;
	}

	/**
	 * Mark this value as dynamic, i.e. as containing an expression
	 * and hence not being subject to caching.
	 */
	public void setDynamic() {
		this.dynamic = true;
	}

	/**
	 * Return whether this value has been marked as dynamic.
	 */
	public boolean isDynamic() {
		return this.dynamic;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TypedStringValue)) {
			return false;
		}
		TypedStringValue otherValue = (TypedStringValue) other;
		return (ObjectUtils.nullSafeEquals(this.value, otherValue.value) &&
				ObjectUtils.nullSafeEquals(this.targetType, otherValue.targetType));
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.value) * 29 + ObjectUtils.nullSafeHashCode(this.targetType);
	}

	@Override
	public String toString() {
		return "TypedStringValue: value [" + this.value + "], target type [" + this.targetType + "]";
	}

}
