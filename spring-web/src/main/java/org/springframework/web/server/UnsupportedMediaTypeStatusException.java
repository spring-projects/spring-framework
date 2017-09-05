/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.server;

import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

/**
 * Exception for errors that fit response status 416 (unsupported media type).
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@SuppressWarnings("serial")
public class UnsupportedMediaTypeStatusException extends ResponseStatusException {

	@Nullable
	private final MediaType contentType;

	private final List<MediaType> supportedMediaTypes;


	/**
	 * Constructor for when the specified Content-Type is invalid.
	 */
	public UnsupportedMediaTypeStatusException(@Nullable String reason) {
		super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, reason);
		this.contentType = null;
		this.supportedMediaTypes = Collections.emptyList();
	}

	/**
	 * Constructor for when the Content-Type can be parsed but is not supported.
	 */
	public UnsupportedMediaTypeStatusException(@Nullable MediaType contentType, List<MediaType> supportedMediaTypes) {
		super(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
				"Content type '" + (contentType != null ? contentType : "") + "' not supported");
		this.contentType = contentType;
		this.supportedMediaTypes = Collections.unmodifiableList(supportedMediaTypes);
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

}
