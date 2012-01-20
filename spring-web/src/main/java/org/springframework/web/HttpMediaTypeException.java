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

package org.springframework.web;

import java.util.List;
import java.util.Collections;
import javax.servlet.ServletException;

import org.springframework.http.MediaType;

/**
 * Abstract base for exceptions related to media types. Adds a list of supported {@link MediaType MediaTypes}.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public abstract class HttpMediaTypeException extends ServletException {

	private final List<MediaType> supportedMediaTypes;

	/**
	 * Create a new MediaTypeException.
	 * @param message the exception message
	 */
	protected HttpMediaTypeException(String message) {
		super(message);
		this.supportedMediaTypes = Collections.emptyList();
	}

	/**
	 * Create a new HttpMediaTypeNotSupportedException.
	 * @param supportedMediaTypes the list of supported media types
	 */
	protected HttpMediaTypeException(String message, List<MediaType> supportedMediaTypes) {
		super(message);
		this.supportedMediaTypes = supportedMediaTypes;
	}

	/**
	 * Return the list of supported media types.
	 */
	public List<MediaType> getSupportedMediaTypes() {
		return supportedMediaTypes;
	}
}
