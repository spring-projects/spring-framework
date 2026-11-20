/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.bind;

import org.springframework.core.MethodParameter;

/**
 * {@link ServletRequestBindingException} subclass that indicates that a matrix
 * variable expected in the method parameters of an {@code @RequestMapping}
 * method is not present among the matrix variables extracted from the URL.
 *
 * @author Juergen Hoeller
 * @since 5.1
 * @see MissingPathVariableException
 */
@SuppressWarnings("serial")
public class MissingMatrixVariableException extends MissingRequestValueException {

	private final String variableName;

	private final MethodParameter parameter;


	/**
	 * Constructor for MissingMatrixVariableException.
	 * @param variableName the name of the missing matrix variable
	 * @param parameter the method parameter
	 */
	public MissingMatrixVariableException(String variableName, MethodParameter parameter) {
		this(variableName, parameter, false);
	}

	/**
	 * Constructor for use when a value was present but converted to {@code null}.
	 * @param variableName the name of the missing matrix variable
	 * @param parameter the method parameter
	 * @param missingAfterConversion whether the value became null after conversion
	 * @since 5.3.6
	 */
	public MissingMatrixVariableException(
			String variableName, MethodParameter parameter, boolean missingAfterConversion) {

		super("", missingAfterConversion, null, new Object[] {variableName});
		this.variableName = variableName;
		this.parameter = parameter;
		getBody().setDetail("Required path parameter '" + this.variableName + "' is not present.");
	}


	@Override
	public String getMessage() {
		return "Required matrix variable '" + this.variableName + "' for method parameter type " +
				this.parameter.getNestedParameterType().getSimpleName() + " is " +
				(isMissingAfterConversion() ? "present but converted to null" : "not present");
	}

	/**
	 * Return the expected name of the matrix variable.
	 */
	public final String getVariableName() {
		return this.variableName;
	}

	/**
	 * Return the method parameter bound to the matrix variable.
	 */
	public final MethodParameter getParameter() {
		return this.parameter;
	}

}
