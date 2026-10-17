/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.method.annotation;

import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.DisconnectedClientHelper;

/**
 * Last-resort exception handler that renders any exception as an
 * RFC 9457 problem detail.
 *
 * <p>This handler is meant to be invoked by the exception handler method
 * infrastructure after all application {@link ExceptionHandler @ExceptionHandler}
 * methods, local or declared in a {@code @ControllerAdvice}, so that the
 * returned {@link ErrorResponse} is rendered like any other handler method
 * return value. It is not meant to be declared as a bean by applications.
 *
 * <p>Exceptions are translated as follows:
 * <ul>
 * <li>{@link AsyncRequestNotUsableException}: no response, since the response
 * is not usable.
 * <li>{@link ErrorResponse} exceptions are used as is.
 * <li>Exceptions that look like a client disconnect, as determined by
 * {@link DisconnectedClientHelper}, result in a 500 status, in case the
 * connection issue is with another remote host rather than with the client.
 * <li>{@link ResponseStatusException} and exceptions annotated with
 * {@link ResponseStatus @ResponseStatus}, also as the cause of the given
 * exception, result in the declared status. The reason is used as the detail,
 * resolved through the {@link MessageSource} if possible.
 * <li>{@link TypeMismatchException}, {@link ConversionNotSupportedException},
 * {@link HttpMessageNotReadableException}, {@link HttpMessageNotWritableException}
 * and {@link MethodValidationException} result in the same status and detail
 * as with {@code ResponseEntityExceptionHandler}.
 * <li>Any other exception results in a 500 status without detail, so that
 * the exception message is never exposed.
 * </ul>
 *
 * <p>In all cases, the type, title and detail of the problem detail can be
 * customized through the {@link MessageSource}, see
 * {@link ErrorResponse#updateAndGetBody(MessageSource, Locale)}.
 *
 * <p>Unexpected exceptions, that is, 5xx server errors that do not come from
 * an {@link ErrorResponse}, are logged at ERROR level; other exceptions are
 * logged at DEBUG level.
 *
 * @author Brian Clozel
 * @since 7.1
 */
public final class ProblemDetailExceptionHandler {

	private static final String DISCONNECTED_CLIENT_LOG_CATEGORY =
			"org.springframework.web.method.annotation.DisconnectedClient";

	private static final DisconnectedClientHelper disconnectedClientHelper =
			new DisconnectedClientHelper(DISCONNECTED_CLIENT_LOG_CATEGORY);

	private static final Log logger = LogFactory.getLog(ProblemDetailExceptionHandler.class);


	private final @Nullable MessageSource messageSource;


	/**
	 * Create a new instance.
	 * @param messageSource the {@code MessageSource} to use to resolve the
	 * type, title and detail of problem details, and {@code @ResponseStatus}
	 * reasons, if any
	 */
	public ProblemDetailExceptionHandler(@Nullable MessageSource messageSource) {
		this.messageSource = messageSource;
	}


	/**
	 * Handle the given exception and return the {@link ErrorResponse} to render.
	 * @param ex the exception to handle
	 * @param locale the locale to use for {@code MessageSource} lookups
	 * @return the error response, or {@code null} if no response should be rendered
	 */
	@ExceptionHandler
	public @Nullable ErrorResponse handleException(Exception ex, Locale locale) {
		if (ex instanceof AsyncRequestNotUsableException) {
			return null;
		}
		ErrorResponse errorResponse = resolveErrorResponse(ex, locale);
		logException(ex, errorResponse);
		errorResponse.updateAndGetBody(this.messageSource, locale);
		return errorResponse;
	}

	private ErrorResponse resolveErrorResponse(Exception ex, Locale locale) {
		if (ex instanceof ErrorResponse errorResponse) {
			return errorResponse;
		}
		if (DisconnectedClientHelper.isClientDisconnectedException(ex)) {
			return createErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, null);
		}
		ErrorResponse responseStatus = resolveResponseStatus(ex, locale);
		if (responseStatus != null) {
			return responseStatus;
		}

		// Lower level exceptions, and exceptions used symmetrically on client and server

		if (ex instanceof ConversionNotSupportedException theEx) {
			Object[] args = {theEx.getPropertyName(), theEx.getValue()};
			String detail = "Failed to convert '" + args[0] + "' with value: '" + args[1] + "'";
			return ErrorResponse.builder(ex, HttpStatus.INTERNAL_SERVER_ERROR, detail)
					.detailMessageArguments(args).build();
		}
		else if (ex instanceof TypeMismatchException theEx) {
			Object[] args = {theEx.getPropertyName(), theEx.getValue(),
					(theEx.getRequiredType() != null ? theEx.getRequiredType().getSimpleName() : "")};
			String detail = "Failed to convert '" + args[0] + "' with value: '" + args[1] + "'";
			return ErrorResponse.builder(ex, HttpStatus.BAD_REQUEST, detail)
					.detailMessageCode(ErrorResponse.getDefaultDetailMessageCode(TypeMismatchException.class, null))
					.detailMessageArguments(args).build();
		}
		else if (ex instanceof HttpMessageNotReadableException) {
			return ErrorResponse.create(ex, HttpStatus.BAD_REQUEST, "Failed to read request");
		}
		else if (ex instanceof HttpMessageNotWritableException) {
			return ErrorResponse.create(ex, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write request");
		}
		else if (ex instanceof MethodValidationException) {
			return ErrorResponse.create(ex, HttpStatus.INTERNAL_SERVER_ERROR, "Validation failed");
		}
		return createErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, null);
	}

	private @Nullable ErrorResponse resolveResponseStatus(Throwable ex, Locale locale) {
		Throwable current = ex;
		while (current != null) {
			if (current instanceof ResponseStatusException rse) {
				return rse;
			}
			ResponseStatus status = AnnotatedElementUtils.findMergedAnnotation(current.getClass(), ResponseStatus.class);
			if (status != null) {
				return createErrorResponse(current, status.code(), resolveReason(status.reason(), locale));
			}
			Throwable cause = current.getCause();
			current = (cause != current ? cause : null);
		}
		return null;
	}

	private @Nullable String resolveReason(String reason, Locale locale) {
		if (!StringUtils.hasLength(reason)) {
			return null;
		}
		return (this.messageSource != null ? this.messageSource.getMessage(reason, null, reason, locale) : reason);
	}

	private static ErrorResponse createErrorResponse(Throwable ex, HttpStatusCode status, @Nullable String detail) {
		ProblemDetail problemDetail = ProblemDetail.forStatus(status);
		problemDetail.setDetail(detail);
		return ErrorResponse.builder(ex, problemDetail).build();
	}

	private void logException(Exception ex, ErrorResponse errorResponse) {
		if (disconnectedClientHelper.checkAndLogClientDisconnectedException(ex)) {
			return;
		}
		// ErrorResponse exceptions are raised on purpose, unlike translated exceptions
		if (errorResponse.getStatusCode().is5xxServerError() && !(errorResponse instanceof Throwable)) {
			logger.error("Unexpected exception rendered as " + errorResponse.getStatusCode() + " problem detail", ex);
		}
		else if (logger.isDebugEnabled()) {
			logger.debug("Rendering " + errorResponse.getStatusCode() + " problem detail for " + ex);
		}
	}

}
