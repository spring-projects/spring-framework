/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web;

import java.util.Collections;
import java.util.List;

import jakarta.servlet.ServletException;
import org.jspecify.annotations.Nullable;

import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

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

	private final Object @Nullable [] messageDetailArguments;


	/**
	 * Create a new HttpMediaTypeException with a list of supported media types.
	 * @param supportedMediaTypes the list of supported media types
	 * @param messageDetailCode the code to use to resolve the problem "detail"
	 * through a {@link org.springframework.context.MessageSource}
	 * @param messageDetailArguments the arguments to make available when
	 * resolving the problem "detail" through a {@code MessageSource}
	 * @since 6.0
	 */
	protected HttpMediaTypeException(@Nullable String message, List<MediaType> supportedMediaTypes,
			@Nullable String messageDetailCode, Object @Nullable [] messageDetailArguments) {

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
	public Object @Nullable [] getDetailMessageArguments() {
		return this.messageDetailArguments;
	}

}
