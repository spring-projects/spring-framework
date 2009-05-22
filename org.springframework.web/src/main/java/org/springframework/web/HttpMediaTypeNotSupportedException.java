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

import java.util.Collections;
import java.util.List;
import javax.servlet.ServletException;

import org.springframework.http.MediaType;

/**
 * Exception thrown when a client POSTs or PUTs content 
 * not supported by request handler.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public class HttpMediaTypeNotSupportedException extends ServletException {

	private final MediaType contentType;

	private final List<MediaType> supportedMediaTypes;

	/**
	 * Create a new HttpMediaTypeNotSupportedException.
	 * @param message the exception message
	 */
	public HttpMediaTypeNotSupportedException(String message) {
		super(message);
		this.contentType = null;
		this.supportedMediaTypes = Collections.emptyList();
	}

	/**
	 * Create a new HttpMediaTypeNotSupportedException.
	 * @param contentType the unsupported content type
	 * @param supportedMediaTypes the list of supported media types
	 */
	public HttpMediaTypeNotSupportedException(MediaType contentType, List<MediaType> supportedMediaTypes) {
		this(contentType, supportedMediaTypes, "Content type '" + contentType + "' not supported");
	}

	/**
	 * Create a new HttpMediaTypeNotSupportedException.
	 * @param contentType the unsupported content type
	 * @param supportedMediaTypes the list of supported media types
	 * @param msg the detail message
	 */
	public HttpMediaTypeNotSupportedException(MediaType contentType, List<MediaType> supportedMediaTypes, String msg) {
		super(msg);
		this.contentType = contentType;
		this.supportedMediaTypes = supportedMediaTypes;
	}

	/**
	 * Return the HTTP request content type method that caused the failure.
	 */
	public MediaType getContentType() {
		return contentType;
	}

	/**
	 * Return the list of supported media types.
	 */
	public List<MediaType> getSupportedMediaTypes() {
		return supportedMediaTypes;
	}
}
