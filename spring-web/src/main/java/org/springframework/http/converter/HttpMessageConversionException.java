/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.http.converter;

import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpStatus;

import java.util.Optional;

/**
 * Thrown by {@link HttpMessageConverter} implementations when a conversion attempt fails.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 3.0
 */
@SuppressWarnings("serial")
public class HttpMessageConversionException extends NestedRuntimeException {

	private final HttpStatus errorStatus;

	/**
	 * Create a new HttpMessageConversionException.
	 * @param msg the detail message
	 */
	public HttpMessageConversionException(String msg) {
		super(msg);
		this.errorStatus = null;
	}

	/**
	 * Create a new HttpMessageConversionException.
	 * @param msg the detail message
	 * @param cause the root cause (if any)
	 */
	public HttpMessageConversionException(String msg, Throwable cause) {
		super(msg, cause);
		this.errorStatus = null;
	}

	/**
	 * Create a new HttpMessageConversionException.
	 * @since 5.0
	 * @param msg the detail message
	 * @param cause the root cause (if any)
	 * @param errorStatus the HTTP error status related to this exception
	 */
	public HttpMessageConversionException(String msg, Throwable cause, HttpStatus errorStatus) {
		super(msg, cause);
		this.errorStatus = errorStatus;
	}

	/**
	 * Return the HTTP error status related to this exception if any.
	 */
	public Optional<HttpStatus> getErrorStatus() {
		return Optional.ofNullable(errorStatus);
	}
}
