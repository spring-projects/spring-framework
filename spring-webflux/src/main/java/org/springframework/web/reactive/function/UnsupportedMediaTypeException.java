/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.function;

import java.util.Collections;
import java.util.List;

import org.springframework.core.NestedRuntimeException;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

/**
 * Exception thrown to indicate that a {@code Content-Type} is not supported.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
@SuppressWarnings("serial")
public class UnsupportedMediaTypeException extends NestedRuntimeException {

	@Nullable
	private final MediaType contentType;

	private final List<MediaType> supportedMediaTypes;

	@Nullable
	private final ResolvableType bodyType;


	/**
	 * Constructor for when the specified Content-Type is invalid.
	 */
	public UnsupportedMediaTypeException(String reason) {
		super(reason);
		this.contentType = null;
		this.supportedMediaTypes = Collections.emptyList();
		this.bodyType = null;
	}

	/**
	 * Constructor for when the Content-Type can be parsed but is not supported.
	 */
	public UnsupportedMediaTypeException(@Nullable MediaType contentType, List<MediaType> supportedTypes) {
		this(contentType, supportedTypes, null);
	}

	/**
	 * Constructor for when trying to encode from or decode to a specific Java type.
	 * @since 5.1
	 */
	public UnsupportedMediaTypeException(@Nullable MediaType contentType, List<MediaType> supportedTypes,
			@Nullable ResolvableType bodyType) {

		super(initReason(contentType, bodyType));
		this.contentType = contentType;
		this.supportedMediaTypes = Collections.unmodifiableList(supportedTypes);
		this.bodyType = bodyType;
	}

	private static String initReason(@Nullable MediaType contentType, @Nullable ResolvableType bodyType) {
		return "Content type '" + (contentType != null ? contentType : "") + "' not supported" +
				(bodyType != null ? " for bodyType=" + bodyType.toString() : "");
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
	 * This is applicable when the exception was raised as a result trying to
	 * encode from or decode to a specific Java type.
	 * @return the body type, or {@code null} if not available
	 * @since 5.1
	 */
	@Nullable
	public ResolvableType getBodyType() {
		return this.bodyType;
	}

}
