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

package org.springframework.web.server;

import org.springframework.core.MethodParameter;

/**
 * {@link ServerWebInputException} subclass that indicates a missing request
 * value such as a request header, cookie value, query parameter, etc.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
@SuppressWarnings("serial")
public class MissingRequestValueException extends ServerWebInputException {

	private final String name;

	private final Class<?> type;

	private final String label;


	public MissingRequestValueException(String name, Class<?> type, String label, MethodParameter parameter) {
		super("Required " + label + " '" + name + "' is not present.", parameter,
				null, null, new Object[] {label, name});

		this.name = name;
		this.type = type;
		this.label = label;
	}


	/**
	 * Return the name of the missing value, e.g. the name of the missing request
	 * header, or cookie, etc.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the target type the value is converted when present.
	 */
	public Class<?> getType() {
		return this.type;
	}

	/**
	 * Return a label that describes the request value, e.g. "request header",
	 * "cookie value", etc. Use this to create a custom message.
	 */
	public String getLabel() {
		return this.label;
	}

}
