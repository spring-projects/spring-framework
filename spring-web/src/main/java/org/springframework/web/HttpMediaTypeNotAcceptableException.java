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

package org.springframework.web;

import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;

/**
 * Exception thrown when the request handler cannot generate a response that is
 * acceptable by the client.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
@SuppressWarnings("serial")
public class HttpMediaTypeNotAcceptableException extends HttpMediaTypeException {

	private static final String PARSE_ERROR_DETAIL_CODE =
			ErrorResponse.getDefaultDetailMessageCode(HttpMediaTypeNotAcceptableException.class, "parseError");


	/**
	 * Constructor for when the {@code Accept} header cannot be parsed.
	 * @param message the parse error message
	 */
	public HttpMediaTypeNotAcceptableException(String message) {
		super(message, Collections.emptyList(), PARSE_ERROR_DETAIL_CODE, null);
		getBody().setDetail("Could not parse Accept header.");
	}

	/**
	 * Create a new HttpMediaTypeNotSupportedException.
	 * @param mediaTypes the list of supported media types
	 */
	public HttpMediaTypeNotAcceptableException(List<MediaType> mediaTypes) {
		super("No acceptable representation", mediaTypes, null, new Object[] {mediaTypes});
		getBody().setDetail("Acceptable representations: " + mediaTypes + ".");
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
		headers.setAccept(this.getSupportedMediaTypes());
		return headers;
	}

}
