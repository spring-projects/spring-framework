/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ObjectUtils;

/**
 * Exception to be thrown when an actual type conversion attempt fails.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
@SuppressWarnings("serial")
public class ConversionFailedException extends ConversionException {

	private final @Nullable TypeDescriptor sourceType;

	private final TypeDescriptor targetType;

	private final @Nullable Object value;


	/**
	 * Create a new conversion exception.
	 * @param sourceType the value's original type
	 * @param targetType the value's target type
	 * @param value the value we tried to convert
	 * @param cause the cause of the conversion failure
	 */
	public ConversionFailedException(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType,
			@Nullable Object value, Throwable cause) {

		super("Failed to convert from type [" + sourceType + "] to type [" + targetType +
				"] for value [" + ObjectUtils.nullSafeConciseToString(value) + "]", cause);
		this.sourceType = sourceType;
		this.targetType = targetType;
		this.value = value;
	}


	/**
	 * Return the source type we tried to convert the value from.
	 */
	public @Nullable TypeDescriptor getSourceType() {
		return this.sourceType;
	}

	/**
	 * Return the target type we tried to convert the value to.
	 */
	public TypeDescriptor getTargetType() {
		return this.targetType;
	}

	/**
	 * Return the offending value.
	 */
	public @Nullable Object getValue() {
		return this.value;
	}

}
