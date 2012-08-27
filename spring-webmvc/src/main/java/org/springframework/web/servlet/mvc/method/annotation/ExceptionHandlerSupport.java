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

import java.util.HashSet;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

/**
 * A convenient base for classes with {@link ExceptionHandler} methods providing
 * infrastructure to handle standard Spring MVC exceptions. The functionality is
 * equivalent to that of the {@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver DefaultHandlerExceptionResolver}
 * except it can be customized to write error content to the body of the response. If
 * there is no need to write error content, use {@code DefaultHandlerExceptionResolver}
 * instead.
 *
 * <p>It is expected the sub-classes will be annotated with
 * {@link ControllerAdvice @ControllerAdvice} and that
 * {@link ExceptionHandlerExceptionResolver} is configured to ensure this class
 * applies to exceptions from any {@link RequestMapping @RequestMapping} method.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 *
 * @see org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver
 */
public abstract class ExceptionHandlerSupport {

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

		HttpStatus status;
		Object body;

		if (ex instanceof NoSuchRequestHandlingMethodException) {
			status = HttpStatus.NOT_FOUND;
			body = handleNoSuchRequestHandlingMethod((NoSuchRequestHandlingMethodException) ex, headers, status, request);
		}
		else if (ex instanceof HttpRequestMethodNotSupportedException) {
			status = HttpStatus.METHOD_NOT_ALLOWED;
			body = handleHttpRequestMethodNotSupported((HttpRequestMethodNotSupportedException) ex, headers, status, request);
		}
		else if (ex instanceof HttpMediaTypeNotSupportedException) {
			status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
			body = handleHttpMediaTypeNotSupported((HttpMediaTypeNotSupportedException) ex, headers, status, request);
		}
		else if (ex instanceof HttpMediaTypeNotAcceptableException) {
			status = HttpStatus.NOT_ACCEPTABLE;
			body = handleHttpMediaTypeNotAcceptable((HttpMediaTypeNotAcceptableException) ex, headers, status, request);
		}
		else if (ex instanceof MissingServletRequestParameterException) {
			status = HttpStatus.BAD_REQUEST;
			body = handleMissingServletRequestParameter((MissingServletRequestParameterException) ex, headers, status, request);
		}
		else if (ex instanceof ServletRequestBindingException) {
			status = HttpStatus.BAD_REQUEST;
			body = handleServletRequestBindingException((ServletRequestBindingException) ex, headers, status, request);
		}
		else if (ex instanceof ConversionNotSupportedException) {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
			body = handleConversionNotSupported((ConversionNotSupportedException) ex, headers, status, request);
		}
		else if (ex instanceof TypeMismatchException) {
			status = HttpStatus.BAD_REQUEST;
			body = handleTypeMismatch((TypeMismatchException) ex, headers, status, request);
		}
		else if (ex instanceof HttpMessageNotReadableException) {
			status = HttpStatus.BAD_REQUEST;
			body = handleHttpMessageNotReadable((HttpMessageNotReadableException) ex, headers, status, request);
		}
		else if (ex instanceof HttpMessageNotWritableException) {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
			body = handleHttpMessageNotWritable((HttpMessageNotWritableException) ex, headers, status, request);
		}
		else if (ex instanceof MethodArgumentNotValidException) {
			status = HttpStatus.BAD_REQUEST;
			body = handleMethodArgumentNotValid((MethodArgumentNotValidException) ex, headers, status, request);
		}
		else if (ex instanceof MissingServletRequestPartException) {
			status = HttpStatus.BAD_REQUEST;
			body = handleMissingServletRequestPart((MissingServletRequestPartException) ex, headers, status, request);
		}
		else if (ex instanceof BindException) {
			status = HttpStatus.BAD_REQUEST;
			body = handleBindException((BindException) ex, headers, status, request);
		}
		else {
			logger.warn("Unknown exception type: " + ex.getClass().getName());
			status = HttpStatus.INTERNAL_SERVER_ERROR;
			body = handleExceptionInternal(ex, headers, status, request);
		}

		return new ResponseEntity<Object>(body, headers, status);
	}

	/**
	 * A single place to customize the response body of all Exception types.
	 * This method returns {@code null} by default.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 */
	protected Object handleExceptionInternal(Exception ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
		return null;
	}

	/**
	 * Customize the response for NoSuchRequestHandlingMethodException.
	 * This method logs a warning and delegates to
	 * {@link #handleExceptionInternal(Exception, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return an Object or {@code null}
	 */
	protected Object handleNoSuchRequestHandlingMethod(NoSuchRequestHandlingMethodException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		pageNotFoundLogger.warn(ex.getMessage());

		return handleExceptionInternal(ex, headers, status, request);
	}

	/**
	 * Customize the response for HttpRequestMethodNotSupportedException.
	 * This method logs a warning, sets the "Allow" header, and delegates to
	 * {@link #handleExceptionInternal(Exception, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return an Object or {@code null}
	 */
	protected Object handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		pageNotFoundLogger.warn(ex.getMessage());

		Set<HttpMethod> mediaTypes = new HashSet<HttpMethod>();
		for (String value : ex.getSupportedMethods()) {
			mediaTypes.add(HttpMethod.valueOf(value));
		}
		if (!mediaTypes.isEmpty()) {
			headers.setAllow(mediaTypes);
		}

		return handleExceptionInternal(ex, headers, status, request);
	}

	/**
	 * Customize the response for HttpMediaTypeNotSupportedException.
	 * This method sets the "Accept" header and delegates to
	 * {@link #handleExceptionInternal(Exception, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return an Object or {@code null}
	 */
	protected Object handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		List<MediaType> mediaTypes = ex.getSupportedMediaTypes();
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			headers.setAccept(mediaTypes);
		}
		return handleExceptionInternal(ex, headers, status, request);
	}

	/**
	 * Customize the response for HttpMediaTypeNotAcceptableException.
	 * This method delegates to {@link #handleExceptionInternal(Exception, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return an Object or {@code null}
	 */
	protected Object handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, headers, status, request);
	}

	/**
	 * Customize the response for MissingServletRequestParameterException.
	 * This method delegates to {@link #handleExceptionInternal(Exception, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return an Object or {@code null}
	 */
	protected Object handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, headers, status, request);
	}

	/**
	 * Customize the response for ServletRequestBindingException.
	 * This method delegates to {@link #handleExceptionInternal(Exception, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return an Object or {@code null}
	 */
	protected Object handleServletRequestBindingException(ServletRequestBindingException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, headers, status, request);
	}

	/**
	 * Customize the response for ConversionNotSupportedException.
	 * This method delegates to {@link #handleExceptionInternal(Exception, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return an Object or {@code null}
	 */
	protected Object handleConversionNotSupported(ConversionNotSupportedException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, headers, status, request);
	}

	/**
	 * Customize the response for TypeMismatchException.
	 * This method delegates to {@link #handleExceptionInternal(Exception, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return an Object or {@code null}
	 */
	protected Object handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
			HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, headers, status, request);
	}

	/**
	 * Customize the response for HttpMessageNotReadableException.
	 * This method delegates to {@link #handleExceptionInternal(Exception, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return an Object or {@code null}
	 */
	protected Object handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, headers, status, request);
	}

	/**
	 * Customize the response for HttpMessageNotWritableException.
	 * This method delegates to {@link #handleExceptionInternal(Exception, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return an Object or {@code null}
	 */
	protected Object handleHttpMessageNotWritable(HttpMessageNotWritableException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, headers, status, request);
	}

	/**
	 * Customize the response for MethodArgumentNotValidException.
	 * This method delegates to {@link #handleExceptionInternal(Exception, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return an Object or {@code null}
	 */
	protected Object handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, headers, status, request);
	}

	/**
	 * Customize the response for MissingServletRequestPartException.
	 * This method delegates to {@link #handleExceptionInternal(Exception, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return an Object or {@code null}
	 */
	protected Object handleMissingServletRequestPart(MissingServletRequestPartException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, headers, status, request);
	}

	/**
	 * Customize the response for BindException.
	 * This method delegates to {@link #handleExceptionInternal(Exception, HttpHeaders, HttpStatus, WebRequest)}.
	 * @param ex the exception
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 * @return an Object or {@code null}
	 */
	protected Object handleBindException(BindException ex, HttpHeaders headers,
			HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, headers, status, request);
	}

}
