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

package org.springframework.web.servlet.mvc.method.annotation;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.util.WebUtils;

/**
 * A convenient base class for {@link ControllerAdvice @ControllerAdvice} classes
 * that wish to provide centralized exception handling across all
 * {@code @RequestMapping} methods through {@code @ExceptionHandler} methods.
 *
 * <p>This base class provides an {@code @ExceptionHandler} method for handling
 * internal Spring MVC exceptions. This method returns a {@code ResponseEntity}
 * for writing to the response with a {@link HttpMessageConverter message converter},
 * in contrast to
 * {@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver
 * DefaultHandlerExceptionResolver} which returns a
 * {@link org.springframework.web.servlet.ModelAndView ModelAndView}.
 *
 * <p>If there is no need to write error content to the response body, or when
 * using view resolution (e.g., via {@code ContentNegotiatingViewResolver}),
 * then {@code DefaultHandlerExceptionResolver} is good enough.
 *
 * <p>Note that in order for an {@code @ControllerAdvice} subclass to be
 * detected, {@link ExceptionHandlerExceptionResolver} must be configured.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 * @see #handleException(Exception, WebRequest)
 * @see org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver
 */
public abstract class ResponseEntityExceptionHandler {

	/**
	 * Log category to use when no mapped handler is found for a request.
	 * @see #pageNotFoundLogger
	 */
	public static final String PAGE_NOT_FOUND_LOG_CATEGORY = "org.springframework.web.servlet.PageNotFound";

	/**
	 * Specific logger to use when no mapped handler is found for a request.
	 * @see #PAGE_NOT_FOUND_LOG_CATEGORY
	 */
	protected static final Log pageNotFoundLogger = LogFactory.getLog(PAGE_NOT_FOUND_LOG_CATEGORY);

	/**
	 * Common logger for use in subclasses.
	 */
	protected final Log logger = LogFactory.getLog(getClass());


	/**
	 * Provides handling for standard Spring MVC exceptions.
	 * @param ex the target exception
	 * @param request the current request
	 */
	@ExceptionHandler({
			HttpRequestMethodNotSupportedException.class,
			HttpMediaTypeNotSupportedException.class,
			HttpMediaTypeNotAcceptableException.class,
			MissingPathVariableException.class,
			MissingServletRequestParameterException.class,
			MissingServletRequestPartException.class,
			ServletRequestBindingException.class,
			MethodArgumentNotValidException.class,
			NoHandlerFoundException.class,
			AsyncRequestTimeoutException.class,
			ErrorResponseException.class,
			ConversionNotSupportedException.class,
			TypeMismatchException.class,
			HttpMessageNotReadableException.class,
			HttpMessageNotWritableException.class,
			BindException.class
		})
	@Nullable
	public final ResponseEntity<Object> handleException(Exception ex, WebRequest request) throws Exception {
		HttpHeaders headers = new HttpHeaders();

		// ErrorResponse exceptions that expose HTTP response details

		if (ex instanceof ErrorResponse errorEx) {
			if (ex instanceof HttpRequestMethodNotSupportedException subEx) {
				return handleHttpRequestMethodNotSupported(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
			}
			else if (ex instanceof HttpMediaTypeNotSupportedException subEx) {
				return handleHttpMediaTypeNotSupported(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
			}
			else if (ex instanceof HttpMediaTypeNotAcceptableException subEx) {
				return handleHttpMediaTypeNotAcceptable(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
			}
			else if (ex instanceof MissingPathVariableException subEx) {
				return handleMissingPathVariable(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
			}
			else if (ex instanceof MissingServletRequestParameterException subEx) {
				return handleMissingServletRequestParameter(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
			}
			else if (ex instanceof MissingServletRequestPartException subEx) {
				return handleMissingServletRequestPart(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
			}
			else if (ex instanceof ServletRequestBindingException subEx) {
				return handleServletRequestBindingException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
			}
			else if (ex instanceof MethodArgumentNotValidException subEx) {
				return handleMethodArgumentNotValid(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
			}
			else if (ex instanceof NoHandlerFoundException subEx) {
				return handleNoHandlerFoundException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
			}
			else if (ex instanceof AsyncRequestTimeoutException subEx) {
				return handleAsyncRequestTimeoutException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
			}
			else {
				// Another ErrorResponseException
				return handleExceptionInternal(ex, null, errorEx.getHeaders(), errorEx.getStatusCode(), request);
			}
		}

		// Other, lower level exceptions

		if (ex instanceof ConversionNotSupportedException cnse) {
			return handleConversionNotSupported(cnse, headers, HttpStatus.INTERNAL_SERVER_ERROR, request);
		}
		else if (ex instanceof TypeMismatchException tme) {
			return handleTypeMismatch(tme, headers, HttpStatus.BAD_REQUEST, request);
		}
		else if (ex instanceof HttpMessageNotReadableException hmnre) {
			return handleHttpMessageNotReadable(hmnre, headers, HttpStatus.BAD_REQUEST, request);
		}
		else if (ex instanceof HttpMessageNotWritableException hmnwe) {
			return handleHttpMessageNotWritable(hmnwe, headers, HttpStatus.INTERNAL_SERVER_ERROR, request);
		}
		else if (ex instanceof BindException be) {
			return handleBindException(be, headers, HttpStatus.BAD_REQUEST, request);
		}
		else {
			// Unknown exception, typically a wrapper with a common MVC exception as cause
			// (since @ExceptionHandler type declarations also match first-level causes):
			// We only deal with top-level MVC exceptions here, so let's rethrow the given
			// exception for further processing through the HandlerExceptionResolver chain.
			throw ex;
		}
	}

	/**
	 * Customize the response for HttpRequestMethodNotSupportedException.
	 * <p>This method logs a warning, and delegates to {@link #handleExceptionInternal}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return {@code ResponseEntity} or {@code null} if response is committed
	 */
	@Nullable
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
			HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		pageNotFoundLogger.warn(ex.getMessage());
		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for HttpMediaTypeNotSupportedException.
	 * <p>This method delegates to {@link #handleExceptionInternal}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return {@code ResponseEntity} or {@code null} if response is committed
	 */
	@Nullable
	protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
			HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for HttpMediaTypeNotAcceptableException.
	 * <p>This method delegates to {@link #handleExceptionInternal}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return {@code ResponseEntity} or {@code null} if response is committed
	 */
	@Nullable
	protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(
			HttpMediaTypeNotAcceptableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for MissingPathVariableException.
	 * <p>This method delegates to {@link #handleExceptionInternal}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return {@code ResponseEntity} or {@code null} if response is committed
	 * @since 4.2
	 */
	@Nullable
	protected ResponseEntity<Object> handleMissingPathVariable(
			MissingPathVariableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for MissingServletRequestParameterException.
	 * <p>This method delegates to {@link #handleExceptionInternal}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return {@code ResponseEntity} or {@code null} if response is committed
	 */
	@Nullable
	protected ResponseEntity<Object> handleMissingServletRequestParameter(
			MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for MissingServletRequestPartException.
	 * <p>This method delegates to {@link #handleExceptionInternal}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return {@code ResponseEntity} or {@code null} if response is committed
	 */
	@Nullable
	protected ResponseEntity<Object> handleMissingServletRequestPart(
			MissingServletRequestPartException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for ServletRequestBindingException.
	 * <p>This method delegates to {@link #handleExceptionInternal}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return {@code ResponseEntity} or {@code null} if response is committed
	 */
	@Nullable
	protected ResponseEntity<Object> handleServletRequestBindingException(
			ServletRequestBindingException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for MethodArgumentNotValidException.
	 * <p>This method delegates to {@link #handleExceptionInternal}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return {@code ResponseEntity} or {@code null} if response is committed
	 */
	@Nullable
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for NoHandlerFoundException.
	 * <p>This method delegates to {@link #handleExceptionInternal}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return {@code ResponseEntity} or {@code null} if response is committed
	 * @since 4.0
	 */
	@Nullable
	protected ResponseEntity<Object> handleNoHandlerFoundException(
			NoHandlerFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for AsyncRequestTimeoutException.
	 * <p>This method delegates to {@link #handleExceptionInternal}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param webRequest the current request
	 * @return {@code ResponseEntity} or {@code null} if response is committed
	 * @since 4.2.8
	 */
	@Nullable
	protected ResponseEntity<Object> handleAsyncRequestTimeoutException(
			AsyncRequestTimeoutException ex, HttpHeaders headers, HttpStatusCode status, WebRequest webRequest) {

		return handleExceptionInternal(ex, null, headers, status, webRequest);
	}

	/**
	 * Customize the response for ConversionNotSupportedException.
	 * <p>This method delegates to {@link #handleExceptionInternal}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return {@code ResponseEntity} or {@code null} if response is committed
	 */
	@Nullable
	protected ResponseEntity<Object> handleConversionNotSupported(
			ConversionNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for TypeMismatchException.
	 * <p>This method delegates to {@link #handleExceptionInternal}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return {@code ResponseEntity} or {@code null} if response is committed
	 */
	@Nullable
	protected ResponseEntity<Object> handleTypeMismatch(
			TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for HttpMessageNotReadableException.
	 * <p>This method delegates to {@link #handleExceptionInternal}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return {@code ResponseEntity} or {@code null} if response is committed
	 */
	@Nullable
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for HttpMessageNotWritableException.
	 * <p>This method delegates to {@link #handleExceptionInternal}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return {@code ResponseEntity} or {@code null} if response is committed
	 */
	@Nullable
	protected ResponseEntity<Object> handleHttpMessageNotWritable(
			HttpMessageNotWritableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for BindException.
	 * <p>This method delegates to {@link #handleExceptionInternal}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return {@code ResponseEntity} or {@code null} if response is committed
	 */
	@Nullable
	protected ResponseEntity<Object> handleBindException(
			BindException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * A single place to customize the response body of all exception types.
	 * <p>The default implementation sets the {@link WebUtils#ERROR_EXCEPTION_ATTRIBUTE}
	 * request attribute and creates a {@link ResponseEntity} from the given
	 * body, headers, and status.
	 * @param ex the exception
	 * @param body the body for the response
	 * @param headers the headers for the response
	 * @param statusCode the response status
	 * @param webRequest the current request
	 * @return {@code ResponseEntity} or {@code null} if response is committed
	 */
	@Nullable
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest webRequest) {

		if (webRequest instanceof ServletWebRequest servletWebRequest) {
			HttpServletResponse response = servletWebRequest.getResponse();
			if (response != null && response.isCommitted()) {
				if (logger.isWarnEnabled()) {
					logger.warn("Ignoring exception, response committed. : " + ex);
				}
				return null;
			}
		}

		if (HttpStatus.INTERNAL_SERVER_ERROR.equals(statusCode)) {
			webRequest.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST);
		}

		if (body == null && ex instanceof ErrorResponse errorResponse) {
			body = errorResponse.getBody();
		}

		return new ResponseEntity<>(body, headers, statusCode);
	}

}
