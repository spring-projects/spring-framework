/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.web.portlet.handler;

import javax.portlet.PortletException;

import org.springframework.util.StringUtils;

/**
 * Exception thrown when a request handler does not support a
 * specific request method.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class PortletRequestMethodNotSupportedException extends PortletException {

	private String method;

	private String[] supportedMethods;


	/**
	 * Create a new PortletRequestMethodNotSupportedException.
	 * @param method the unsupported HTTP request method
	 */
	public PortletRequestMethodNotSupportedException(String method) {
		this(method, null);
	}

	/**
	 * Create a new PortletRequestMethodNotSupportedException.
	 * @param method the unsupported HTTP request method
	 * @param supportedMethods the actually supported HTTP methods
	 */
	public PortletRequestMethodNotSupportedException(String method, String[] supportedMethods) {
		super("Request method '" + method + "' not supported by mapped handler");
		this.method = method;
		this.supportedMethods = supportedMethods;
	}

	/**
	 * Create a new PortletRequestMethodNotSupportedException.
	 * @param supportedMethods the actually supported HTTP methods
	 */
	public PortletRequestMethodNotSupportedException(String[] supportedMethods) {
		super("Mapped handler only supports client data requests with methods " +
				StringUtils.arrayToCommaDelimitedString(supportedMethods));
		this.supportedMethods = supportedMethods;
	}


	/**
	 * Return the HTTP request method that caused the failure.
	 */
	public String getMethod() {
		return this.method;
	}

	/**
	 * Return the actually supported HTTP methods, if known.
	 */
	public String[] getSupportedMethods() {
		return this.supportedMethods;
	}

}