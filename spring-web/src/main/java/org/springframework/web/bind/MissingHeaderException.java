/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
 * {@link ServletRequestBindingException} subclass that indicates that a header
 * variable expected in the method parameters of an {@code @RequestMapping}
 * is not present.
 *
 * @author Per Böckman
 *
 */
@SuppressWarnings("serial")
public class MissingHeaderException extends ServletRequestBindingException {

	private final String name;

	private final MethodParameter parameter;


	/**
	 * Constructor for MissingHeaderException.
	 * @param name the name of the missing header
	 * @param parameter the method parameter
	 */
	public MissingHeaderException(String name, MethodParameter parameter) {
		super("");
		this.name = name;
		this.parameter = parameter;
	}


	@Override
	public String getMessage() {
		return "Missing request header '" + name +
				"' for method parameter of type " + parameter.getNestedParameterType().getSimpleName();
	}

	/**
	 * Return the expected name of the header.
	 */
	public final String getName() {
		return this.name;
	}

	/**
	 * Return the method parameter bound to the path variable.
	 */
	public final MethodParameter getParameter() {
		return this.parameter;
	}

}
