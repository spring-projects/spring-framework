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

package org.springframework.web.server;

import java.util.Collections;
import java.util.List;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.ErrorResponse;

/**
 * Exception for errors that fit response status 415 (unsupported media type).
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@SuppressWarnings("serial")
public class UnsupportedMediaTypeStatusException extends ResponseStatusException {

	private static final String PARSE_ERROR_DETAIL_CODE =
			ErrorResponse.getDefaultDetailMessageCode(UnsupportedMediaTypeStatusException.class, "parseError");


	@Nullable
	private final MediaType contentType;

	private final List<MediaType> supportedMediaTypes;

	@Nullable
	private final ResolvableType bodyType;

	@Nullable
	private final HttpMethod method;


	/**
	 * Constructor for when the specified Content-Type is invalid.
	 */
	public UnsupportedMediaTypeStatusException(@Nullable String reason) {
		this(reason, Collections.emptyList());
	}

	/**
	 * Constructor for when the specified Content-Type is invalid.
	 * @since 6.0.5
	 */
	public UnsupportedMediaTypeStatusException(@Nullable String reason, List<MediaType> supportedTypes) {
		super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, reason, null, PARSE_ERROR_DETAIL_CODE, null);
		this.contentType = null;
		this.supportedMediaTypes = Collections.unmodifiableList(supportedTypes);
		this.bodyType = null;
		this.method = null;
		setDetail("Could not parse Content-Type.");
	}

	/**
	 * Constructor for when the Content-Type can be parsed but is not supported.
	 */
	public UnsupportedMediaTypeStatusException(@Nullable MediaType contentType, List<MediaType> supportedTypes) {
		this(contentType, supportedTypes, null, null);
	}

	/**
	 * Constructor for when trying to encode from or decode to a specific Java type.
	 * @since 5.1
	 */
	public UnsupportedMediaTypeStatusException(@Nullable MediaType contentType, List<MediaType> supportedTypes,
			@Nullable ResolvableType bodyType) {
		this(contentType, supportedTypes, bodyType, null);
	}

	/**
	 * Constructor that provides the HTTP method.
	 * @since 5.3.6
	 */
	public UnsupportedMediaTypeStatusException(@Nullable MediaType contentType, List<MediaType> supportedTypes,
			@Nullable HttpMethod method) {
		this(contentType, supportedTypes, null, method);
	}

	/**
	 * Constructor for when trying to encode from or decode to a specific Java type.
	 * @since 5.3.6
	 */
	public UnsupportedMediaTypeStatusException(@Nullable MediaType contentType, List<MediaType> supportedTypes,
			@Nullable ResolvableType bodyType, @Nullable HttpMethod method) {

		super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, initMessage(contentType, bodyType),
				null, null, new Object[] {contentType, supportedTypes});

		this.contentType = contentType;
		this.supportedMediaTypes = Collections.unmodifiableList(supportedTypes);
		this.bodyType = bodyType;
		this.method = method;

		setDetail(contentType != null ? "Content-Type '" + contentType + "' is not supported." : null);
	}

	private static String initMessage(@Nullable MediaType contentType, @Nullable ResolvableType bodyType) {
		return "Content type '" + (contentType != null ? contentType : "") + "' not supported" +
				(bodyType != null ? " for bodyType=" + bodyType : "");
	}


	/**
	 * Return the request Content-Type header if it was parsed successfully,
	 * or {@code null} otherwise.
	 */
	@Nullable
	public MediaType getContentType() {
		return this.contentType;
	}

	/**
	 * Return the list of supported content types in cases when the Content-Type
	 * header is parsed but not supported, or an empty list otherwise.
	 */
	public List<MediaType> getSupportedMediaTypes() {
		return this.supportedMediaTypes;
	}

	/**
	 * Return the body type in the context of which this exception was generated.
	 * <p>This is applicable when the exception was raised as a result trying to
	 * encode from or decode to a specific Java type.
	 * @return the body type, or {@code null} if not available
	 * @since 5.1
	 */
	@Nullable
	public ResolvableType getBodyType() {
		return this.bodyType;
	}

	/**
	 * Return HttpHeaders with an "Accept" header that documents the supported
	 * media types, if available, or an empty instance otherwise.
	 */
	@Override
	public HttpHeaders getHeaders() {
		if (CollectionUtils.isEmpty(this.supportedMediaTypes) ) {
			return HttpHeaders.EMPTY;
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(this.supportedMediaTypes);
		if (this.method == HttpMethod.PATCH) {
			headers.setAcceptPatch(this.supportedMediaTypes);
		}
		return headers;
	}

	/**
	 * Delegates to {@link #getHeaders()}.
	 * @deprecated as of 6.0 in favor of {@link #getHeaders()}
	 */
	@Deprecated(since = "6.0")
	@Override
	public HttpHeaders getResponseHeaders() {
		return getHeaders();
	}

}
