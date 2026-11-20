/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.method.annotation;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;

/**
 * A TypeMismatchException raised while resolving a controller method argument.
 * Provides access to the target {@link org.springframework.core.MethodParameter
 * MethodParameter}.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
@SuppressWarnings("serial")
public class MethodArgumentTypeMismatchException extends TypeMismatchException {

	private final String name;

	private final MethodParameter parameter;


	public MethodArgumentTypeMismatchException(@Nullable Object value,
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

	@Override
	public String getMessage() {
		return "Method parameter '" + getName() + "': " + super.getMessage();
	}

}
