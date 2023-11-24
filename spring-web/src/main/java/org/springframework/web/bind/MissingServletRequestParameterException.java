/*
 * Copyright 2002-2023 the original author or authors.
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
import org.springframework.lang.Nullable;

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

	@Nullable
	private final MethodParameter parameter;


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

	/**
	 * Constructor for use when a value was present but converted to {@code null}.
	 * @param parameterName the name of the missing parameter
	 * @param parameterType the expected type of the missing parameter
	 * @param missingAfterConversion whether the value became null after conversion
	 * @since 5.3.6
	 * @deprecated in favor of {@link #MissingServletRequestParameterException(String, MethodParameter, boolean)}
	 */
	@Deprecated(since = "6.1", forRemoval = true)
	public MissingServletRequestParameterException(
			String parameterName, String parameterType, boolean missingAfterConversion) {

		super("", missingAfterConversion, null, new Object[] {parameterName});
		this.parameterName = parameterName;
		this.parameterType = parameterType;
		this.parameter = null;
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
	@Nullable
	public MethodParameter getMethodParameter() {
		return this.parameter;
	}

}
