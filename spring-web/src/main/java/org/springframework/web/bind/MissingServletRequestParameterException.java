/*
 * Copyright 2002-2021 the original author or authors.
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


	/**
	 * Constructor for MissingServletRequestParameterException.
	 * @param parameterName the name of the missing parameter
	 * @param parameterType the expected type of the missing parameter
	 */
	public MissingServletRequestParameterException(String parameterName, String parameterType) {
		this(parameterName, parameterType, false);
	}

	/**
	 * Constructor for use when a value was present but converted to {@code null}.
	 * @param parameterName the name of the missing parameter
	 * @param parameterType the expected type of the missing parameter
	 * @param missingAfterConversion whether the value became null after conversion
	 * @since 5.3.6
	 */
	public MissingServletRequestParameterException(
			String parameterName, String parameterType, boolean missingAfterConversion) {

		super("", missingAfterConversion);
		this.parameterName = parameterName;
		this.parameterType = parameterType;
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

}
