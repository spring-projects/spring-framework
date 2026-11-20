/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans;

import java.beans.PropertyChangeEvent;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when a property path is not well-formed according to the grammar
 * implemented by {@link PropertyPath}.
 *
 * <p>This signals a syntactically invalid path, as opposed to a syntactically valid
 * path that happens not to resolve against a particular target object. The latter
 * cases are reported as {@link NotReadablePropertyException} or
 * {@link NotWritablePropertyException} instead.
 *
 * <p>Extends {@link PropertyAccessException} so that a malformed path
 * encountered while binding a single property value can be collected into a
 * {@link PropertyBatchUpdateException} alongside other per-property failures,
 * rather than aborting the whole binding operation.
 *
 * @author Brian Clozel
 * @since 7.1
 * @see PropertyPath#parse(String)
 */
@SuppressWarnings("serial")
public class InvalidPropertyPathException extends PropertyAccessException {

	/**
	 * Error code that an {@code InvalidPropertyPathException} is registered with.
	 */
	public static final String ERROR_CODE = "invalidPropertyPath";


	private final String propertyPath;


	/**
	 * Create a new {@code InvalidPropertyPathException}.
	 * @param propertyPath the offending property path
	 * @param reason a description of the grammar rule that was violated
	 */
	public InvalidPropertyPathException(String propertyPath, String reason) {
		super("Invalid property path '" + propertyPath + "': " + reason, null);
		this.propertyPath = propertyPath;
	}

	/**
	 * Create a new {@code InvalidPropertyPathException}.
	 * @param propertyChangeEvent the event for the property
	 * @param cause the original parsing exception
	 */
	public InvalidPropertyPathException(PropertyChangeEvent propertyChangeEvent, InvalidPropertyPathException cause) {
		super(propertyChangeEvent, Objects.requireNonNull(cause.getMessage()), cause);
		this.propertyPath = cause.propertyPath;
	}

	/**
	 * Create a new {@code InvalidPropertyPathException}.
	 * @param source the bean that fired the event
	 * @param propertyName the programmatic name of the property that was changed
	 * @param newValue the new value of the property
	 * @param cause the original parsing exception
	 */
	public InvalidPropertyPathException(Object source, String propertyName, @Nullable Object newValue, InvalidPropertyPathException cause) {
		this(new PropertyChangeEvent(source, propertyName, null, newValue), cause);
	}


	/**
	 * Return the offending property path.
	 */
	public String getPropertyPath() {
		return this.propertyPath;
	}

	@Override
	public String getErrorCode() {
		return ERROR_CODE;
	}

}
