/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.http.converter;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpInputMessage;
import org.springframework.util.Assert;

/**
 * Thrown by {@link HttpMessageConverter} implementations when the
 * {@link HttpMessageConverter#read} method fails.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
@SuppressWarnings("serial")
public class HttpMessageNotReadableException extends HttpMessageConversionException {

	private final @Nullable HttpInputMessage httpInputMessage;


	/**
	 * Create a new HttpMessageNotReadableException.
	 * @param msg the detail message
	 * @param httpInputMessage the original HTTP message
	 * @since 5.1
	 */
	public HttpMessageNotReadableException(String msg, HttpInputMessage httpInputMessage) {
		super(msg);
		this.httpInputMessage = httpInputMessage;
	}

	/**
	 * Create a new HttpMessageNotReadableException.
	 * @param msg the detail message
	 * @param cause the root cause (if any)
	 * @param httpInputMessage the original HTTP message
	 * @since 5.1
	 */
	public HttpMessageNotReadableException(String msg, @Nullable Throwable cause, HttpInputMessage httpInputMessage) {
		super(msg, cause);
		this.httpInputMessage = httpInputMessage;
	}


	/**
	 * Return the original HTTP message.
	 * @since 5.1
	 */
	public HttpInputMessage getHttpInputMessage() {
		Assert.state(this.httpInputMessage != null, "No HttpInputMessage available - use non-deprecated constructors");
		return this.httpInputMessage;
	}

}
