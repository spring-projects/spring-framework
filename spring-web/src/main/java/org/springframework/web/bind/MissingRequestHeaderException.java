/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.bind;

import org.springframework.core.MethodParameter;

/**
 * {@link ServletRequestBindingException} subclass that indicates
 * that a request header expected in the method parameters of an
 * {@code @RequestMapping} method is not present.
 *
 * @author Juergen Hoeller
 * @since 5.1
 * @see MissingRequestCookieException
 */
@SuppressWarnings("serial")
public class MissingRequestHeaderException extends MissingRequestValueException {

	private final String headerName;

	private final MethodParameter parameter;


	/**
	 * Constructor for MissingRequestHeaderException.
	 * @param headerName the name of the missing request header
	 * @param parameter the method parameter
	 */
	public MissingRequestHeaderException(String headerName, MethodParameter parameter) {
		this(headerName, parameter, false);
	}

	/**
	 * Constructor for use when a value was present but converted to {@code null}.
	 * @param headerName the name of the missing request header
	 * @param parameter the method parameter
	 * @param missingAfterConversion whether the value became null after conversion
	 * @since 5.3.6
	 */
	public MissingRequestHeaderException(
			String headerName, MethodParameter parameter, boolean missingAfterConversion) {

		super("", missingAfterConversion, null, new Object[] {headerName});
		this.headerName = headerName;
		this.parameter = parameter;
		getBody().setDetail("Required header '" + this.headerName + "' is not present.");
	}


	@Override
	public String getMessage() {
		String typeName = this.parameter.getNestedParameterType().getSimpleName();
		return "Required request header '" + this.headerName + "' for method parameter type " + typeName + " is " +
				(isMissingAfterConversion() ? "present but converted to null" : "not present");
	}

	/**
	 * Return the expected name of the request header.
	 */
	public final String getHeaderName() {
		return this.headerName;
	}

	/**
	 * Return the method parameter bound to the request header.
	 */
	public final MethodParameter getParameter() {
		return this.parameter;
	}

}
