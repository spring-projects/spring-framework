/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans;

import java.beans.PropertyChangeEvent;

import org.jspecify.annotations.Nullable;

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


	private @Nullable String propertyName;

	private final transient @Nullable Object value;

	private final @Nullable Class<?> requiredType;


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
	public @Nullable String getPropertyName() {
		return this.propertyName;
	}

	/**
	 * Return the offending value (may be {@code null}).
	 */
	@Override
	public @Nullable Object getValue() {
		return this.value;
	}

	/**
	 * Return the required target type, if any.
	 */
	public @Nullable Class<?> getRequiredType() {
		return this.requiredType;
	}

	@Override
	public String getErrorCode() {
		return ERROR_CODE;
	}

}
