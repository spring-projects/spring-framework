/*
 * Copyright 2002-2018 the original author or authors.
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
public class MissingRequestHeaderException extends ServletRequestBindingException {

	private final String headerName;

	private final MethodParameter parameter;


	/**
	 * Constructor for MissingRequestHeaderException.
	 * @param headerName the name of the missing request header
	 * @param parameter the method parameter
	 */
	public MissingRequestHeaderException(String headerName, MethodParameter parameter) {
		super("");
		this.headerName = headerName;
		this.parameter = parameter;
	}


	@Override
	public String getMessage() {
		return "Missing request header '" + this.headerName +
				"' for method parameter of type " + this.parameter.getNestedParameterType().getSimpleName();
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
