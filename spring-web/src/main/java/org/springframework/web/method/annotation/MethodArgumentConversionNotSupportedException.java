/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.method.annotation;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.core.MethodParameter;

/**
 * A ConversionNotSupportedException raised while resolving a method argument.
 * Provides access to the target {@link org.springframework.core.MethodParameter
 * MethodParameter}.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
@SuppressWarnings("serial")
public class MethodArgumentConversionNotSupportedException extends ConversionNotSupportedException {

	private final String name;

	private final MethodParameter parameter;


	public MethodArgumentConversionNotSupportedException(@Nullable Object value,
			@Nullable Class<?> requiredType, String name, MethodParameter param, @Nullable Throwable cause) {

		super(value, requiredType, cause);
		this.name = name;
		this.parameter = param;
		initPropertyName(name);
	}


	/**
	 * Return the name of the method argument.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the target method parameter.
	 */
	public MethodParameter getParameter() {
		return this.parameter;
	}

}
