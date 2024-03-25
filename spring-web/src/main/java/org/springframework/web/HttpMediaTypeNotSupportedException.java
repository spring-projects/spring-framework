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

package org.springframework.web;

import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * Exception thrown when a client POSTs, PUTs, or PATCHes content of a type
 * not supported by request handler.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.0
 */
@SuppressWarnings("serial")
public class HttpMediaTypeNotSupportedException extends HttpMediaTypeException {

	private static final String PARSE_ERROR_DETAIL_CODE =
			ErrorResponse.getDefaultDetailMessageCode(HttpMediaTypeNotSupportedException.class, "parseError");


	@Nullable
	private final MediaType contentType;

	@Nullable
	private final HttpMethod httpMethod;


	/**
	 * Create a new HttpMediaTypeNotSupportedException.
	 * @param message the exception message
	 */
	public HttpMediaTypeNotSupportedException(String message) {
		this(message, Collections.emptyList());
	}

	/**
	 * Create a new HttpMediaTypeNotSupportedException for a parse error.
	 * @param message the exception message
	 * @param mediaTypes list of supported media types
	 * @since 6.0.5
	 */
	public HttpMediaTypeNotSupportedException(@Nullable String message, List<MediaType> mediaTypes) {
		super(message, mediaTypes, PARSE_ERROR_DETAIL_CODE, null);
		this.contentType = null;
		this.httpMethod = null;
		getBody().setDetail("Could not parse Content-Type.");
	}

	/**
	 * Create a new HttpMediaTypeNotSupportedException.
	 * @param contentType the unsupported content type
	 * @param mediaTypes the list of supported media types
	 */
	public HttpMediaTypeNotSupportedException(@Nullable MediaType contentType, List<MediaType> mediaTypes) {
		this(contentType, mediaTypes, null);
	}

	/**
	 * Create a new HttpMediaTypeNotSupportedException.
	 * @param contentType the unsupported content type
	 * @param mediaTypes the list of supported media types
	 * @param httpMethod the HTTP method of the request
	 * @since 6.0
	 */
	public HttpMediaTypeNotSupportedException(
			@Nullable MediaType contentType, List<MediaType> mediaTypes, @Nullable HttpMethod httpMethod) {

		this(contentType, mediaTypes, httpMethod,
				"Content-Type " + (contentType != null ? "'" + contentType + "' " : "") + "is not supported");
	}

	/**
	 * Create a new HttpMediaTypeNotSupportedException.
	 * @param contentType the unsupported content type
	 * @param supportedMediaTypes the list of supported media types
	 * @param httpMethod the HTTP method of the request
	 * @param message the detail message
	 * @since 6.0
	 */
	public HttpMediaTypeNotSupportedException(@Nullable MediaType contentType,
			List<MediaType> supportedMediaTypes, @Nullable HttpMethod httpMethod, String message) {

		super(message, supportedMediaTypes, null, new Object[] {contentType, supportedMediaTypes});
		this.contentType = contentType;
		this.httpMethod = httpMethod;
		getBody().setDetail("Content-Type '" + this.contentType + "' is not supported.");
	}


	/**
	 * Return the HTTP request content type method that caused the failure.
	 */
	@Nullable
	public MediaType getContentType() {
		return this.contentType;
	}

	@Override
	public HttpStatusCode getStatusCode() {
		return HttpStatus.UNSUPPORTED_MEDIA_TYPE;
	}

	@Override
	public HttpHeaders getHeaders() {
		if (CollectionUtils.isEmpty(getSupportedMediaTypes())) {
			return HttpHeaders.EMPTY;
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(getSupportedMediaTypes());
		if (HttpMethod.PATCH.equals(this.httpMethod)) {
			headers.setAcceptPatch(getSupportedMediaTypes());
		}
		return headers;
	}

}
