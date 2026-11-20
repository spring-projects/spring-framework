/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.handler;

import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.handler.ResponseStatusExceptionHandler;

/**
 * Common WebFlux exception handler that detects instances of
 * {@link org.springframework.web.server.ResponseStatusException}
 * (inherited from the base class) as well as exceptions annotated with
 * {@link ResponseStatus @ResponseStatus} by determining the HTTP status
 * for them and updating the status of the response accordingly.
 *
 * <p>If the response is already committed, the error remains unresolved
 * and is propagated.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 5.0.5
 */
public class WebFluxResponseStatusExceptionHandler extends ResponseStatusExceptionHandler {

	@Override
	protected @Nullable HttpStatusCode determineStatus(Throwable ex) {
		HttpStatusCode statusCode = super.determineStatus(ex);
		if (statusCode == null) {
			ResponseStatus ann = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
			if (ann != null) {
				statusCode = ann.code();
			}
		}
		return statusCode;
	}

}
