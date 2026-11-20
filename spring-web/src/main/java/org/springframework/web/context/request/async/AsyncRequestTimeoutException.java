/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.context.request.async;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;

/**
 * Exception to be thrown when an async request times out.
 * Alternatively an applications can register a
 * {@link DeferredResultProcessingInterceptor} or a
 * {@link CallableProcessingInterceptor} to handle the timeout through
 * the MVC Java config or the MVC XML namespace or directly through properties
 * of the {@code RequestMappingHandlerAdapter}.
 *
 * <p>By default the exception will be handled as a 503 error.
 *
 * @author Rossen Stoyanchev
 * @since 4.2.8
 */
@SuppressWarnings("serial")
public class AsyncRequestTimeoutException extends RuntimeException implements ErrorResponse {

	private final ProblemDetail body = ProblemDetail.forStatus(getStatusCode());


	@Override
	public HttpStatusCode getStatusCode() {
		return HttpStatus.SERVICE_UNAVAILABLE;
	}

	@Override
	public ProblemDetail getBody() {
		return this.body;
	}

}
