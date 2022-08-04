/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web;

import java.util.List;

import org.springframework.http.MediaType;

/**
 * Exception thrown when the request handler cannot generate a response that is acceptable by the client.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
@SuppressWarnings("serial")
public class HttpMediaTypeNotAcceptableException extends HttpMediaTypeException {

	/**
	 * Create a new HttpMediaTypeNotAcceptableException.
	 * @param message the exception message
	 */
	public HttpMediaTypeNotAcceptableException(String message) {
		super(message);
	}

	/**
	 * Create a new HttpMediaTypeNotSupportedException.
	 * @param supportedMediaTypes the list of supported media types
	 */
	public HttpMediaTypeNotAcceptableException(List<MediaType> supportedMediaTypes) {
		super("Could not find acceptable representation", supportedMediaTypes);
	}

}
