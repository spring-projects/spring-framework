/*
 * Copyright 2026-present the original author or authors.
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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;

import java.util.Collections;

/**
 * Exception thrown when the request handler cannot resolve {@link MediaType}
 * due to the {@code mimeTypes} contains too many elements.
 */
public class TooManyHttpMediaTypesException extends HttpMediaTypeException {

	private static final String PARSE_ERROR_DETAIL_CODE =
			ErrorResponse.getDefaultDetailMessageCode(TooManyHttpMediaTypesException.class, "parseError");


	/**
	 * Constructor for when the {@code Accept} header cannot be parsed.
	 * @param message the parse error message
	 */
	public TooManyHttpMediaTypesException(String message) {
		super(message, Collections.emptyList(), PARSE_ERROR_DETAIL_CODE, null);
		getBody().setDetail("Too many elements in Accept header.");
	}

	@Override
	public HttpStatusCode getStatusCode() {
		return HttpStatus.NOT_ACCEPTABLE;
	}

	@Override
	public HttpHeaders getHeaders() {
		if (CollectionUtils.isEmpty(getSupportedMediaTypes())) {
			return HttpHeaders.EMPTY;
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(getSupportedMediaTypes());
		return headers;
	}

}
