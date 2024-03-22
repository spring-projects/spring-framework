/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.servlet.mvc.support;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.util.WebUtils;

/**
 * The default implementation of the {@link org.springframework.web.servlet.HandlerExceptionResolver}
 * interface, resolving standard Spring MVC exceptions and translating them to corresponding
 * HTTP status codes.
 *
 * <p>This exception resolver is enabled by default in the common Spring
 * {@link org.springframework.web.servlet.DispatcherServlet}.
 *
 * <h3>Supported Exceptions</h3>
 * <table>
 * <thead>
 * <tr>
 * <th class="table-header col-first">Exception</th>
 * <th class="table-header col-last">HTTP Status Code</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr class="even-row-color">
 * <td><div class="block">HttpRequestMethodNotSupportedException</div></td>
 * <td><div class="block">405 (SC_METHOD_NOT_ALLOWED)</div></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><div class="block">HttpMediaTypeNotSupportedException</div></td>
 * <td><div class="block">415 (SC_UNSUPPORTED_MEDIA_TYPE)</div></td>
 * </tr>
 * <tr class="even-row-color">
 * <td><div class="block">HttpMediaTypeNotAcceptableException</div></td>
 * <td><div class="block">406 (SC_NOT_ACCEPTABLE)</div></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><div class="block">MissingPathVariableException</div></td>
 * <td><div class="block">500 (SC_INTERNAL_SERVER_ERROR)</div></td>
 * </tr>
 * <tr class="even-row-color">
 * <td><div class="block">MissingServletRequestParameterException</div></td>
 * <td><div class="block">400 (SC_BAD_REQUEST)</div></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><div class="block">MissingServletRequestPartException</div></td>
 * <td><div class="block">400 (SC_BAD_REQUEST)</div></td>
 * </tr>
 * <tr class="even-row-color">
 * <td><div class="block">ServletRequestBindingException</div></td>
 * <td><div class="block">400 (SC_BAD_REQUEST)</div></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><div class="block">ConversionNotSupportedException</div></td>
 * <td><div class="block">500 (SC_INTERNAL_SERVER_ERROR)</div></td>
 * </tr>
 * <tr class="even-row-color">
 * <td><div class="block">TypeMismatchException</div></td>
 * <td><div class="block">400 (SC_BAD_REQUEST)</div></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><div class="block">HttpMessageNotReadableException</div></td>
 * <td><div class="block">400 (SC_BAD_REQUEST)</div></td>
 * </tr>
 * <tr class="even-row-color">
 * <td><div class="block">HttpMessageNotWritableException</div></td>
 * <td><div class="block">500 (SC_INTERNAL_SERVER_ERROR)</div></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><div class="block">MethodArgumentNotValidException</div></td>
 * <td><div class="block">400 (SC_BAD_REQUEST)</div></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><div class="block">{@link MethodValidationException}</div></td>
 * <td><div class="block">500 (SC_INTERNAL_SERVER_ERROR)</div></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><div class="block">{@link HandlerMethodValidationException}</div></td>
 * <td><div class="block">400 (SC_BAD_REQUEST)</div></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><div class="block">NoHandlerFoundException</div></td>
 * <td><div class="block">404 (SC_NOT_FOUND)</div></td>
 * </tr>
 * <tr class="even-row-color">
 * <td><div class="block">NoResourceFoundException</div></td>
 * <td><div class="block">404 (SC_NOT_FOUND)</div></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><div class="block">AsyncRequestTimeoutException</div></td>
 * <td><div class="block">503 (SC_SERVICE_UNAVAILABLE)</div></td>
 * </tr>
 * <tr class="even-row-color">
 * <td><div class="block">AsyncRequestNotUsableException</div></td>
 * <td><div class="block">Not applicable</div></td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
 */
public class DefaultHandlerExceptionResolver extends AbstractHandlerExceptionResolver {

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
	 * Sets the {@linkplain #setOrder(int) order} to {@link #LOWEST_PRECEDENCE}.
	 */
	public DefaultHandlerExceptionResolver() {
		setOrder(Ordered.LOWEST_PRECEDENCE);
		setWarnLogCategory(getClass().getName());
	}


	@Override
	@Nullable
	protected ModelAndView doResolveException(
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {

		try {
			// ErrorResponse exceptions that expose HTTP response details
			if (ex instanceof ErrorResponse errorResponse) {
				ModelAndView mav = null;
				if (ex instanceof HttpRequestMethodNotSupportedException theEx) {
					mav = handleHttpRequestMethodNotSupported(theEx, request, response, handler);
				}
				else if (ex instanceof HttpMediaTypeNotSupportedException theEx) {
					mav = handleHttpMediaTypeNotSupported(theEx, request, response, handler);
				}
				else if (ex instanceof HttpMediaTypeNotAcceptableException theEx) {
					mav = handleHttpMediaTypeNotAcceptable(theEx, request, response, handler);
				}
				else if (ex instanceof MissingPathVariableException theEx) {
					mav = handleMissingPathVariable(theEx, request, response, handler);
				}
				else if (ex instanceof MissingServletRequestParameterException theEx) {
					mav = handleMissingServletRequestParameter(theEx, request, response, handler);
				}
				else if (ex instanceof MissingServletRequestPartException theEx) {
					mav = handleMissingServletRequestPartException(theEx, request, response, handler);
				}
				else if (ex instanceof ServletRequestBindingException theEx) {
					mav = handleServletRequestBindingException(theEx, request, response, handler);
				}
				else if (ex instanceof MethodArgumentNotValidException theEx) {
					mav = handleMethodArgumentNotValidException(theEx, request, response, handler);
				}
				else if (ex instanceof HandlerMethodValidationException theEx) {
					mav = handleHandlerMethodValidationException(theEx, request, response, handler);
				}
				else if (ex instanceof NoHandlerFoundException theEx) {
					mav = handleNoHandlerFoundException(theEx, request, response, handler);
				}
				else if (ex instanceof NoResourceFoundException theEx) {
					mav = handleNoResourceFoundException(theEx, request, response, handler);
				}
				else if (ex instanceof AsyncRequestTimeoutException theEx) {
					mav = handleAsyncRequestTimeoutException(theEx, request, response, handler);
				}

				return (mav != null ? mav :
						handleErrorResponse(errorResponse, request, response, handler));
			}

			// Other, lower level exceptions

			if (ex instanceof ConversionNotSupportedException theEx) {
				return handleConversionNotSupported(theEx, request, response, handler);
			}
			else if (ex instanceof TypeMismatchException theEx) {
				return handleTypeMismatch(theEx, request, response, handler);
			}
			else if (ex instanceof HttpMessageNotReadableException theEx) {
				return handleHttpMessageNotReadable(theEx, request, response, handler);
			}
			else if (ex instanceof HttpMessageNotWritableException theEx) {
				return handleHttpMessageNotWritable(theEx, request, response, handler);
			}
			else if (ex instanceof MethodValidationException theEx) {
				return handleMethodValidationException(theEx, request, response, handler);
			}
			else if (ex instanceof AsyncRequestNotUsableException) {
				return handleAsyncRequestNotUsableException(
						(AsyncRequestNotUsableException) ex, request, response, handler);
			}
		}
		catch (Exception handlerEx) {
			if (logger.isWarnEnabled()) {
				logger.warn("Failure while trying to resolve exception [" + ex.getClass().getName() + "]", handlerEx);
			}
		}

		return null;
	}

	/**
	 * Handle the case where no handler was found for the HTTP method.
	 * <p>The default implementation returns {@code null} in which case the
	 * exception is handled in {@link #handleErrorResponse}.
	 * @param ex the HttpRequestMethodNotSupportedException to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler, or {@code null} if none chosen
	 * at the time of the exception (for example, if multipart resolution failed)
	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	@Nullable
	protected ModelAndView handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		return null;
	}

	/**
	 * Handle the case where no
	 * {@linkplain org.springframework.http.converter.HttpMessageConverter message converters}
	 * were found for PUT or POSTed content.
	 * <p>The default implementation returns {@code null} in which case the
	 * exception is handled in {@link #handleErrorResponse}.
	 * @param ex the HttpMediaTypeNotSupportedException to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	@Nullable
	protected ModelAndView handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		return null;
	}

	/**
	 * Handle the case where no
	 * {@linkplain org.springframework.http.converter.HttpMessageConverter message converters}
	 * were found that were acceptable for the client (expressed via the {@code Accept} header).
	 * <p>The default implementation returns {@code null} in which case the
	 * exception is handled in {@link #handleErrorResponse}.
	 * @param ex the HttpMediaTypeNotAcceptableException to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	@Nullable
	protected ModelAndView handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		return null;
	}

	/**
	 * Handle the case when a declared path variable does not match any extracted URI variable.
	 * <p>The default implementation returns {@code null} in which case the
	 * exception is handled in {@link #handleErrorResponse}.
	 * @param ex the MissingPathVariableException to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 * @since 4.2
	 */
	@Nullable
	protected ModelAndView handleMissingPathVariable(MissingPathVariableException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		return null;
	}

	/**
	 * Handle the case when a required parameter is missing.
	 * <p>The default implementation returns {@code null} in which case the
	 * exception is handled in {@link #handleErrorResponse}.
	 * @param ex the MissingServletRequestParameterException to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	@Nullable
	protected ModelAndView handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		return null;
	}

	/**
	 * Handle the case where an {@linkplain RequestPart @RequestPart}, a {@link MultipartFile},
	 * or a {@code jakarta.servlet.http.Part} argument is required but is missing.
	 * <p>By default, an HTTP 400 error is sent back to the client.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	@Nullable
	protected ModelAndView handleMissingServletRequestPartException(MissingServletRequestPartException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		return null;
	}

	/**
	 * Handle the case when an unrecoverable binding exception occurs - e.g.
	 * required header, required cookie.
	 * <p>The default implementation returns {@code null} in which case the
	 * exception is handled in {@link #handleErrorResponse}.
	 * @param ex the exception to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	@Nullable
	protected ModelAndView handleServletRequestBindingException(ServletRequestBindingException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		return null;
	}

	/**
	 * Handle the case where an argument annotated with {@code @Valid} such as
	 * an {@link RequestBody} or {@link RequestPart} argument fails validation.
	 * <p>The default implementation returns {@code null} in which case the
	 * exception is handled in {@link #handleErrorResponse}.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	@Nullable
	protected ModelAndView handleMethodArgumentNotValidException(MethodArgumentNotValidException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		return null;
	}

	/**
	 * Handle the case where method validation for a controller method failed.
	 * <p>The default implementation returns {@code null} in which case the
	 * exception is handled in {@link #handleErrorResponse}.
	 * @param ex the exception to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 * @since 6.1
	 */
	@Nullable
	protected ModelAndView handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		return null;
	}

	/**
	 * Handle the case where no handler was found during the dispatch.
	 * <p>The default implementation returns {@code null} in which case the
	 * exception is handled in {@link #handleErrorResponse}.
	 * @param ex the NoHandlerFoundException to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler, or {@code null} if none chosen
	 * at the time of the exception (for example, if multipart resolution failed)
	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 * @since 4.0
	 */
	@Nullable
	protected ModelAndView handleNoHandlerFoundException(NoHandlerFoundException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		pageNotFoundLogger.warn(ex.getMessage());
		return null;
	}

	/**
	 * Handle the case where no static resource was found.
	 * <p>The default implementation returns {@code null} in which case the
	 * exception is handled in {@link #handleErrorResponse}.
	 * @param ex the {@link NoResourceFoundException} to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the resource handler
	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 * @since 6.1
	 */
	@Nullable
	protected ModelAndView handleNoResourceFoundException(NoResourceFoundException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		return null;
	}

	/**
	 * Handle the case where an async request timed out.
	 * <p>The default implementation returns {@code null} in which case the
	 * exception is handled in {@link #handleErrorResponse}.
	 * @param ex the {@link AsyncRequestTimeoutException} to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler, or {@code null} if none chosen
	 * at the time of the exception (for example, if multipart resolution failed)
	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 * @since 4.2.8
	 */
	@Nullable
	protected ModelAndView handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		return null;
	}

	/**
	 * Handle the case of an I/O failure from the ServletOutputStream.
	 * <p>By default, do nothing since the response is not usable.
	 * @param ex the {@link AsyncRequestTimeoutException} to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler, or {@code null} if none chosen
	 * at the time of the exception (for example, if multipart resolution failed)
	 * @return an empty ModelAndView indicating the exception was handled
	 * @since 5.3.33
	 */
	protected ModelAndView handleAsyncRequestNotUsableException(AsyncRequestNotUsableException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) {

		return new ModelAndView();
	}

	/**
	 * Handle an {@link ErrorResponse} exception.
	 * <p>The default implementation sets status and the headers of the response
	 * to those obtained from the {@code ErrorResponse}. If available, the
	 * {@link ProblemDetail#getDetail()} is used as the message for
	 * {@link HttpServletResponse#sendError(int, String)}.
	 * @param errorResponse the exception to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty {@code ModelAndView} indicating the exception was handled
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 * @since 6.0
	 */
	protected ModelAndView handleErrorResponse(ErrorResponse errorResponse,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		if (!response.isCommitted()) {
			HttpHeaders headers = errorResponse.getHeaders();
			headers.forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));

			int status = errorResponse.getStatusCode().value();
			String message = errorResponse.getBody().getDetail();
			if (message != null) {
				response.sendError(status, message);
			}
			else {
				response.sendError(status);
			}
		}
		else if (logger.isWarnEnabled()) {
			logger.warn("Ignoring exception, response committed already: " + errorResponse);
		}

		return new ModelAndView();
	}

	/**
	 * Handle the case when a {@link org.springframework.web.bind.WebDataBinder} conversion cannot occur.
	 * <p>The default implementation sends an HTTP 500 error, and returns an empty {@code ModelAndView}.
	 * Alternatively, a fallback view could be chosen, or the ConversionNotSupportedException could be
	 * rethrown as-is.
	 * @param ex the ConversionNotSupportedException to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty {@code ModelAndView} indicating the exception was handled
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	protected ModelAndView handleConversionNotSupported(ConversionNotSupportedException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		sendServerError(ex, request, response);
		return new ModelAndView();
	}

	/**
	 * Handle the case when a {@link org.springframework.web.bind.WebDataBinder} conversion error occurs.
	 * <p>The default implementation sends an HTTP 400 error, and returns an empty {@code ModelAndView}.
	 * Alternatively, a fallback view could be chosen, or the TypeMismatchException could be rethrown as-is.
	 * @param ex the TypeMismatchException to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty {@code ModelAndView} indicating the exception was handled
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	protected ModelAndView handleTypeMismatch(TypeMismatchException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		return new ModelAndView();
	}

	/**
	 * Handle the case where a {@linkplain org.springframework.http.converter.HttpMessageConverter message converter}
	 * cannot read from an HTTP request.
	 * <p>The default implementation sends an HTTP 400 error, and returns an empty {@code ModelAndView}.
	 * Alternatively, a fallback view could be chosen, or the HttpMessageNotReadableException could be
	 * rethrown as-is.
	 * @param ex the HttpMessageNotReadableException to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty {@code ModelAndView} indicating the exception was handled
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	protected ModelAndView handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		if (!response.isCommitted()) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		}
		else if (logger.isWarnEnabled()) {
			logger.warn("Ignoring exception, response committed already: " + ex);
		}
		return new ModelAndView();
	}

	/**
	 * Handle the case where a
	 * {@linkplain org.springframework.http.converter.HttpMessageConverter message converter}
	 * cannot write to an HTTP response.
	 * <p>The default implementation sends an HTTP 500 error, and returns an empty {@code ModelAndView}.
	 * Alternatively, a fallback view could be chosen, or the HttpMessageNotWritableException could
	 * be rethrown as-is.
	 * @param ex the HttpMessageNotWritableException to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty {@code ModelAndView} indicating the exception was handled
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 */
	protected ModelAndView handleHttpMessageNotWritable(HttpMessageNotWritableException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		if (!response.isCommitted()) {
			sendServerError(ex, request, response);
		}
		else if (logger.isWarnEnabled()) {
			logger.warn("Ignoring exception, response committed already: " + ex);
		}
		return new ModelAndView();
	}

	/**
	 * Handle the case where method validation failed on a component that is
	 * not a web controller, e.g. on some underlying service.
	 * <p>The default implementation sends an HTTP 500 error, and returns an empty {@code ModelAndView}.
	 * Alternatively, a fallback view could be chosen, or the HttpMessageNotWritableException could
	 * be rethrown as-is.
	 * @param ex the exception to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler
	 * @return an empty {@code ModelAndView} indicating the exception was handled
	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
	 * @since 6.1
	 */
	protected ModelAndView handleMethodValidationException(MethodValidationException ex,
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		sendServerError(ex, request, response);
		return new ModelAndView();
	}

	/**
	 * Invoked to send a server error. Sets the status to 500 and also sets the
	 * request attribute "jakarta.servlet.error.exception" to the Exception.
	 */
	protected void sendServerError(Exception ex, HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex);
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}

	@Override
	protected void logException(Exception ex, HttpServletRequest request) {
		if (ex instanceof NoHandlerFoundException || ex instanceof NoResourceFoundException) {
			if (logger.isDebugEnabled()) {
				logger.debug(buildLogMessage(ex, request));
			}
			return;
		}
		super.logException(ex, request);
	}

}
