/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
		headers.setAccept(getSupportedMediaTypes());
		return headers;
	}

}
