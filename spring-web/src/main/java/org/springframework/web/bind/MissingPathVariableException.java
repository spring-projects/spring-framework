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

package org.springframework.web.bind;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * {@link ServletRequestBindingException} subclass that indicates that a path
 * variable expected in the method parameters of an {@code @RequestMapping}
 * method is not present among the URI variables extracted from the URL.
 * Typically that means the URI template does not match the path variable name
 * declared on the method parameter.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 * @see MissingMatrixVariableException
 */
@SuppressWarnings("serial")
public class MissingPathVariableException extends MissingRequestValueException {

	private final String variableName;

	private final MethodParameter parameter;


	/**
	 * Constructor for MissingPathVariableException.
	 * @param variableName the name of the missing path variable
	 * @param parameter the method parameter
	 */
	public MissingPathVariableException(String variableName, MethodParameter parameter) {
		this(variableName, parameter, false);
	}

	/**
	 * Constructor for use when a value was present but converted to {@code null}.
	 * @param variableName the name of the missing path variable
	 * @param parameter the method parameter
	 * @param missingAfterConversion whether the value became null after conversion
	 * @since 5.3.6
	 */
	public MissingPathVariableException(
			String variableName, MethodParameter parameter, boolean missingAfterConversion) {

		super("", missingAfterConversion, null, new Object[] {variableName});
		this.variableName = variableName;
		this.parameter = parameter;
		getBody().setDetail("Required path variable '" + this.variableName + "' is not present.");
	}


	@Override
	public String getMessage() {
		return "Required URI template variable '" + this.variableName + "' for method parameter type " +
				this.parameter.getNestedParameterType().getSimpleName() + " is " +
				(isMissingAfterConversion() ? "present but converted to null" : "not present");
	}

	/**
	 * Return the expected name of the path variable.
	 */
	public final String getVariableName() {
		return this.variableName;
	}

	/**
	 * Return the method parameter bound to the path variable.
	 */
	public final MethodParameter getParameter() {
		return this.parameter;
	}

	@Override
	public HttpStatusCode getStatusCode() {
		return HttpStatus.INTERNAL_SERVER_ERROR;
	}

}
