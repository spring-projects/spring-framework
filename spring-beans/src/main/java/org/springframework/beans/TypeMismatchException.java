/*
 * Copyright 2002-2022 the original author or authors.
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

import java.beans.PropertyChangeEvent;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Exception thrown on a type mismatch when trying to set a bean property.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class TypeMismatchException extends PropertyAccessException {

	/**
	 * Error code that a type mismatch error will be registered with.
	 */
	public static final String ERROR_CODE = "typeMismatch";


	@Nullable
	private String propertyName;

	@Nullable
	private final transient Object value;

	@Nullable
	private final Class<?> requiredType;


	/**
	 * Create a new {@code TypeMismatchException}.
	 * @param propertyChangeEvent the PropertyChangeEvent that resulted in the problem
	 * @param requiredType the required target type
	 */
	public TypeMismatchException(PropertyChangeEvent propertyChangeEvent, Class<?> requiredType) {
		this(propertyChangeEvent, requiredType, null);
	}

	/**
	 * Create a new {@code TypeMismatchException}.
	 * @param propertyChangeEvent the PropertyChangeEvent that resulted in the problem
	 * @param requiredType the required target type (or {@code null} if not known)
	 * @param cause the root cause (may be {@code null})
	 */
	public TypeMismatchException(PropertyChangeEvent propertyChangeEvent, @Nullable Class<?> requiredType,
			@Nullable Throwable cause) {

		super(propertyChangeEvent,
				"Failed to convert property value of type '" +
				ClassUtils.getDescriptiveType(propertyChangeEvent.getNewValue()) + "'" +
				(requiredType != null ?
				" to required type '" + ClassUtils.getQualifiedName(requiredType) + "'" : "") +
				(propertyChangeEvent.getPropertyName() != null ?
				" for property '" + propertyChangeEvent.getPropertyName() + "'" : "") +
				(cause != null ? "; " + cause.getMessage() : ""),
				cause);
		this.propertyName = propertyChangeEvent.getPropertyName();
		this.value = propertyChangeEvent.getNewValue();
		this.requiredType = requiredType;
	}

	/**
	 * Create a new {@code TypeMismatchException} without a {@code PropertyChangeEvent}.
	 * @param value the offending value that couldn't be converted (may be {@code null})
	 * @param requiredType the required target type (or {@code null} if not known)
	 * @see #initPropertyName
	 */
	public TypeMismatchException(@Nullable Object value, @Nullable Class<?> requiredType) {
		this(value, requiredType, null);
	}

	/**
	 * Create a new {@code TypeMismatchException} without a {@code PropertyChangeEvent}.
	 * @param value the offending value that couldn't be converted (may be {@code null})
	 * @param requiredType the required target type (or {@code null} if not known)
	 * @param cause the root cause (may be {@code null})
	 * @see #initPropertyName
	 */
	public TypeMismatchException(@Nullable Object value, @Nullable Class<?> requiredType, @Nullable Throwable cause) {
		super("Failed to convert value of type '" + ClassUtils.getDescriptiveType(value) + "'" +
				(requiredType != null ? " to required type '" + ClassUtils.getQualifiedName(requiredType) + "'" : "") +
				(cause != null ? "; " + cause.getMessage() : ""),
				cause);
		this.value = value;
		this.requiredType = requiredType;
	}


	/**
	 * Initialize this exception's property name for exposure through {@link #getPropertyName()},
	 * as an alternative to having it initialized via a {@link PropertyChangeEvent}.
	 * @param propertyName the property name to expose
	 * @since 5.0.4
	 * @see #TypeMismatchException(Object, Class)
	 * @see #TypeMismatchException(Object, Class, Throwable)
	 */
	public void initPropertyName(String propertyName) {
		Assert.state(this.propertyName == null, "Property name already initialized");
		this.propertyName = propertyName;
	}

	/**
	 * Return the name of the affected property, if available.
	 */
	@Override
	@Nullable
	public String getPropertyName() {
		return this.propertyName;
	}

	/**
	 * Return the offending value (may be {@code null}).
	 */
	@Override
	@Nullable
	public Object getValue() {
		return this.value;
	}

	/**
	 * Return the required target type, if any.
	 */
	@Nullable
	public Class<?> getRequiredType() {
		return this.requiredType;
	}

	@Override
	public String getErrorCode() {
		return ERROR_CODE;
	}

}
