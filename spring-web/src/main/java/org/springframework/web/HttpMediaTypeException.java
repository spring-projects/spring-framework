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

import jakarta.servlet.ServletException;

import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.Nullable;

/**
 * Abstract base for exceptions related to media types. Adds a list of supported {@link MediaType MediaTypes}.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
@SuppressWarnings("serial")
public abstract class HttpMediaTypeException extends ServletException implements ErrorResponse {

	private final List<MediaType> supportedMediaTypes;

	private final ProblemDetail body = ProblemDetail.forStatus(getStatusCode());

	private final String messageDetailCode;

	@Nullable
	private final Object[] messageDetailArguments;


	/**
	 * Create a new HttpMediaTypeException.
	 * @param message the exception message
	 * @deprecated as of 6.0
	 */
	@Deprecated
	protected HttpMediaTypeException(String message) {
		this(message, Collections.emptyList());
	}

	/**
	 * Create a new HttpMediaTypeException with a list of supported media types.
	 * @param supportedMediaTypes the list of supported media types
	 * @deprecated as of 6.0
	 */
	@Deprecated
	protected HttpMediaTypeException(String message, List<MediaType> supportedMediaTypes) {
		this(message, supportedMediaTypes, null, null);
	}

	/**
	 * Create a new HttpMediaTypeException with a list of supported media types.
	 * @param supportedMediaTypes the list of supported media types
	 * @param messageDetailCode the code to use to resolve the problem "detail"
	 * through a {@link org.springframework.context.MessageSource}
	 * @param messageDetailArguments the arguments to make available when
	 * resolving the problem "detail" through a {@code MessageSource}
	 * @since 6.0
	 */
	protected HttpMediaTypeException(String message, List<MediaType> supportedMediaTypes,
			@Nullable String messageDetailCode, @Nullable Object[] messageDetailArguments) {

		super(message);
		this.supportedMediaTypes = Collections.unmodifiableList(supportedMediaTypes);
		this.messageDetailCode = (messageDetailCode != null ?
				messageDetailCode : ErrorResponse.getDefaultDetailMessageCode(getClass(), null));
		this.messageDetailArguments = messageDetailArguments;
	}


	/**
	 * Return the list of supported media types.
	 */
	public List<MediaType> getSupportedMediaTypes() {
		return this.supportedMediaTypes;
	}

	@Override
	public ProblemDetail getBody() {
		return this.body;
	}

	@Override
	public String getDetailMessageCode() {
		return this.messageDetailCode;
	}

	@Override
	public Object[] getDetailMessageArguments() {
		return this.messageDetailArguments;
	}

}
