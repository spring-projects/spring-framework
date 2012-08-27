/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.web.servlet.mvc.method.annotation;

import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

/**
 * A convenient base class for {@link ControllerAdvice @ControllerAdvice} classes
 * that wish to provide centralized exception handling across all
 * {@code @RequestMapping} methods through {@code @ExceptionHandler} methods.
 *
 * <p>This base class provides an {@code @ExceptionHandler} for handling standard
 * Spring MVC exceptions that returns a {@code ResponseEntity} to be written with
 * {@link HttpMessageConverter message converters}. This is in contrast to
 * {@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver
 * DefaultHandlerExceptionResolver} which returns a {@code ModelAndView} instead.
 *
 * <p>If there is no need to write error content to the response body, or if using
 * view resolution, e.g. {@code ContentNegotiatingViewResolver}, then use
 * {@code DefaultHandlerExceptionResolver} instead.
 *
 * <p>Note that in order for an {@code @ControllerAdvice} sub-class to be
 * detected, {@link ExceptionHandlerExceptionResolver} must be configured.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 *
 * @see org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver
 */
public abstract class ResponseEntityExceptionHandler {

	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * Log category to use when no mapped handler is found for a request.
	 * @see #pageNotFoundLogger
	 */
	public static final String PAGE_NOT_FOUND_LOG_CATEGORY = "org.springframework.web.servlet.PageNotFound";

	/**
	 * Additional logger to use when no mapped handler is found for a request.
	 * @see #PAGE_NOT_FOUND_LOG_CATEGORY
	 */
	protected static final Log pageNotFoundLogger = LogFactory.getLog(PAGE_NOT_FOUND_LOG_CATEGORY);


	/**
	 * Provides handling for standard Spring MVC exceptions.
	 * @param ex the target exception
	 * @param request the current request
	 */
	@ExceptionHandler(value={
			NoSuchRequestHandlingMethodException.class,
			HttpRequestMethodNotSupportedException.class,
			HttpMediaTypeNotSupportedException.class,
			HttpMediaTypeNotAcceptableException.class,
			MissingServletRequestParameterException.class,
			ServletRequestBindingException.class,
			ConversionNotSupportedException.class,
			TypeMismatchException.class,
			HttpMessageNotReadableException.class,
			HttpMessageNotWritableException.class,
			MethodArgumentNotValidException.class,
			MissingServletRequestPartException.class,
			BindException.class
		})
	public final ResponseEntity<Object> handleException(Exception ex, WebRequest request) {

		HttpHeaders headers = new HttpHeaders();

		if (ex instanceof NoSuchRequestHandlingMethodException) {
			HttpStatus status = HttpStatus.NOT_FOUND;
			return handleNoSuchRequestHandlingMethod((NoSuchRequestHandlingMethodException) ex, headers, status, request);
		}
		else if (ex instanceof HttpRequestMethodNotSupportedException) {
			HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
			return handleHttpRequestMethodNotSupported((HttpRequestMethodNotSupportedException) ex, headers, status, request);
		}
		else if (ex instanceof HttpMediaTypeNotSupportedException) {
			HttpStatus status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
			return handleHttpMediaTypeNotSupported((HttpMediaTypeNotSupportedException) ex, headers, status, request);
		}
		else if (ex instanceof HttpMediaTypeNotAcceptableException) {
			HttpStatus status = HttpStatus.NOT_ACCEPTABLE;
			return handleHttpMediaTypeNotAcceptable((HttpMediaTypeNotAcceptableException) ex, headers, status, request);
		}
		else if (ex instanceof MissingServletRequestParameterException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleMissingServletRequestParameter((MissingServletRequestParameterException) ex, headers, status, request);
		}
		else if (ex instanceof ServletRequestBindingException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleServletRequestBindingException((ServletRequestBindingException) ex, headers, status, request);
		}
		else if (ex instanceof ConversionNotSupportedException) {
			HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
			return handleConversionNotSupported((ConversionNotSupportedException) ex, headers, status, request);
		}
		else if (ex instanceof TypeMismatchException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleTypeMismatch((TypeMismatchException) ex, headers, status, request);
		}
		else if (ex instanceof HttpMessageNotReadableException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleHttpMessageNotReadable((HttpMessageNotReadableException) ex, headers, status, request);
		}
		else if (ex instanceof HttpMessageNotWritableException) {
			HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
			return handleHttpMessageNotWritable((HttpMessageNotWritableException) ex, headers, status, request);
		}
		else if (ex instanceof MethodArgumentNotValidException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleMethodArgumentNotValid((MethodArgumentNotValidException) ex, headers, status, request);
		}
		else if (ex instanceof MissingServletRequestPartException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleMissingServletRequestPart((MissingServletRequestPartException) ex, headers, status, request);
		}
		else if (ex instanceof BindException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleBindException((BindException) ex, headers, status, request);
		}
		else {
			logger.warn("Unknown exception type: " + ex.getClass().getName());
			HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
			return handleExceptionInternal(ex, null, headers, status, request);
		}
	}

	/**
	 * A single place to customize the response body of all Exception types.
	 * This method returns {@code null} by default.
	 * @param ex the exception
	 * @param body the body to use for the response
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 */
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		return new ResponseEntity<Object>(body, headers, status);
	}

	/**
	 * Customize the response for NoSuchRequestHandlingMethodException.
	 * This method logs a warning and delegates to
	 * {@link #handleExceptionInternal(Exception, Object, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return a {@code ResponseEntity} instance
	 */
	protected ResponseEntity<Object> handleNoSuchRequestHandlingMethod(NoSuchRequestHandlingMethodException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		pageNotFoundLogger.warn(ex.getMessage());

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for HttpRequestMethodNotSupportedException.
	 * This method logs a warning, sets the "Allow" header, and delegates to
	 * {@link #handleExceptionInternal(Exception, Object, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return a {@code ResponseEntity} instance
	 */
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		pageNotFoundLogger.warn(ex.getMessage());

		Set<HttpMethod> supportedMethods = ex.getSupportedHttpMethods();
		if (!supportedMethods.isEmpty()) {
			headers.setAllow(supportedMethods);
		}

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for HttpMediaTypeNotSupportedException.
	 * This method sets the "Accept" header and delegates to
	 * {@link #handleExceptionInternal(Exception, Object, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return a {@code ResponseEntity} instance
	 */
	protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		List<MediaType> mediaTypes = ex.getSupportedMediaTypes();
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			headers.setAccept(mediaTypes);
		}

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for HttpMediaTypeNotAcceptableException.
	 * This method delegates to {@link #handleExceptionInternal(Exception, Object, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return a {@code ResponseEntity} instance
	 */
	protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for MissingServletRequestParameterException.
	 * This method delegates to {@link #handleExceptionInternal(Exception, Object, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return a {@code ResponseEntity} instance
	 */
	protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for ServletRequestBindingException.
	 * This method delegates to {@link #handleExceptionInternal(Exception, Object, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return a {@code ResponseEntity} instance
	 */
	protected ResponseEntity<Object> handleServletRequestBindingException(ServletRequestBindingException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for ConversionNotSupportedException.
	 * This method delegates to {@link #handleExceptionInternal(Exception, Object, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return a {@code ResponseEntity} instance
	 */
	protected ResponseEntity<Object> handleConversionNotSupported(ConversionNotSupportedException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for TypeMismatchException.
	 * This method delegates to {@link #handleExceptionInternal(Exception, Object, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return a {@code ResponseEntity} instance
	 */
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
			HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for HttpMessageNotReadableException.
	 * This method delegates to {@link #handleExceptionInternal(Exception, Object, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return a {@code ResponseEntity} instance
	 */
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for HttpMessageNotWritableException.
	 * This method delegates to {@link #handleExceptionInternal(Exception, Object, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return a {@code ResponseEntity} instance
	 */
	protected ResponseEntity<Object> handleHttpMessageNotWritable(HttpMessageNotWritableException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for MethodArgumentNotValidException.
	 * This method delegates to {@link #handleExceptionInternal(Exception, Object, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return a {@code ResponseEntity} instance
	 */
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for MissingServletRequestPartException.
	 * This method delegates to {@link #handleExceptionInternal(Exception, Object, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return a {@code ResponseEntity} instance
	 */
	protected ResponseEntity<Object> handleMissingServletRequestPart(MissingServletRequestPartException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * Customize the response for BindException.
	 * This method delegates to {@link #handleExceptionInternal(Exception, Object, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return a {@code ResponseEntity} instance
	 */
	protected ResponseEntity<Object> handleBindException(BindException ex, HttpHeaders headers,
			HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

}
