/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.bind;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;

/**
 * {@link ServletRequestBindingException} subclass that indicates a missing parameter.
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 */
@SuppressWarnings("serial")
public class MissingServletRequestParameterException extends MissingRequestValueException {

	private final String parameterName;

	private final String parameterType;

	private final @Nullable MethodParameter parameter;


	/**
	 * Constructor for MissingServletRequestParameterException.
	 * @param parameterName the name of the missing parameter
	 * @param parameterType the expected type of the missing parameter
	 */
	public MissingServletRequestParameterException(String parameterName, String parameterType) {
		super("", false, null, new Object[] {parameterName});
		this.parameterName = parameterName;
		this.parameterType = parameterType;
		this.parameter = null;
		getBody().setDetail(initBodyDetail(this.parameterName));
	}

	/**
	 * Constructor with a {@link MethodParameter} instead of a String parameterType.
	 * @param parameterName the name of the missing parameter
	 * @param parameter the target method parameter for the missing value
	 * @param missingAfterConversion whether the value became null after conversion
	 * @since 6.1
	 */
	public MissingServletRequestParameterException(
			String parameterName, MethodParameter parameter, boolean missingAfterConversion) {

		super("", missingAfterConversion, null, new Object[] {parameterName});
		this.parameterName = parameterName;
		this.parameterType = parameter.getNestedParameterType().getSimpleName();
		this.parameter = parameter;
		getBody().setDetail(initBodyDetail(this.parameterName));
	}

	private static String initBodyDetail(String name) {
		return "Required parameter '" + name + "' is not present.";
	}


	@Override
	public String getMessage() {
		return "Required request parameter '" + this.parameterName + "' for method parameter type " +
				this.parameterType + " is " +
				(isMissingAfterConversion() ? "present but converted to null" : "not present");
	}

	/**
	 * Return the name of the offending parameter.
	 */
	public final String getParameterName() {
		return this.parameterName;
	}

	/**
	 * Return the expected type of the offending parameter.
	 */
	public final String getParameterType() {
		return this.parameterType;
	}

	/**
	 * Return the target {@link MethodParameter} if the exception was raised for
	 * a controller method argument.
	 * @since 6.1
	 */
	public @Nullable MethodParameter getMethodParameter() {
		return this.parameter;
	}

}
