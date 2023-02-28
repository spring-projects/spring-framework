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

import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.ErrorResponse;

/**
 * Exception for errors that fit response status 406 (not acceptable).
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@SuppressWarnings("serial")
public class NotAcceptableStatusException extends ResponseStatusException {

	private static final String PARSE_ERROR_DETAIL_CODE =
			ErrorResponse.getDefaultDetailMessageCode(NotAcceptableStatusException.class, "parseError");


	private final List<MediaType> supportedMediaTypes;


	/**
	 * Constructor for when the requested Content-Type is invalid.
	 */
	public NotAcceptableStatusException(String reason) {
		super(HttpStatus.NOT_ACCEPTABLE, reason, null, PARSE_ERROR_DETAIL_CODE, null);
		this.supportedMediaTypes = Collections.emptyList();
		setDetail("Could not parse Accept header.");
	}

	/**
	 * Constructor for when the requested Content-Type is not supported.
	 */
	public NotAcceptableStatusException(List<MediaType> mediaTypes) {
		super(HttpStatus.NOT_ACCEPTABLE,
				"Could not find acceptable representation", null, null, new Object[] {mediaTypes});

		this.supportedMediaTypes = Collections.unmodifiableList(mediaTypes);
		setDetail("Acceptable representations: " + mediaTypes + ".");
	}


	/**
	 * Return HttpHeaders with an "Accept" header that documents the supported
	 * media types, if available, or an empty instance otherwise.
	 */
	@Override
	public HttpHeaders getHeaders() {
		if (CollectionUtils.isEmpty(this.supportedMediaTypes)) {
			return HttpHeaders.EMPTY;
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(this.supportedMediaTypes);
		return headers;
	}

	/**
	 * Delegates to {@link #getHeaders()}.
	 * @since 5.1.13
	 * @deprecated as of 6.0 in favor of {@link #getHeaders()}
	 */
	@Deprecated(since = "6.0")
	@Override
	public HttpHeaders getResponseHeaders() {
		return getHeaders();
	}

	/**
	 * Return the list of supported content types in cases when the Accept
	 * header is parsed but not supported, or an empty list otherwise.
	 */
	public List<MediaType> getSupportedMediaTypes() {
		return this.supportedMediaTypes;
	}

}
