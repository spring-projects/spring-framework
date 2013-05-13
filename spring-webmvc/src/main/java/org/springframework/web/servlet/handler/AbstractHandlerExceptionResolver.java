/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.servlet.handler;

import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * Abstract base class for {@link HandlerExceptionResolver} implementations.
 *
 * <p>Provides a set of mapped handlers that the resolver should map to,
 * and the {@link Ordered} implementation.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public abstract class AbstractHandlerExceptionResolver implements HandlerExceptionResolver, Ordered {

	private static final String HEADER_PRAGMA = "Pragma";

	private static final String HEADER_EXPIRES = "Expires";

	private static final String HEADER_CACHE_CONTROL = "Cache-Control";


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private int order = Ordered.LOWEST_PRECEDENCE;

	private Set<?> mappedHandlers;

	private Class[] mappedHandlerClasses;

	private Log warnLogger;

	private boolean preventResponseCaching = false;


	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Specify the set of handlers that this exception resolver should apply to.
	 * The exception mappings and the default error view will only apply to the specified handlers.
	 * <p>If no handlers and handler classes are set, the exception mappings and the default error
	 * view will apply to all handlers. This means that a specified default error view will be used
	 * as fallback for all exceptions; any further HandlerExceptionResolvers in the chain will be
	 * ignored in this case.
	 */
	public void setMappedHandlers(Set<?> mappedHandlers) {
		this.mappedHandlers = mappedHandlers;
	}

	/**
	 * Specify the set of classes that this exception resolver should apply to.
	 * The exception mappings and the default error view will only apply to handlers of the
	 * specified type; the specified types may be interfaces and superclasses of handlers as well.
	 * <p>If no handlers and handler classes are set, the exception mappings and the default error
	 * view will apply to all handlers. This means that a specified default error view will be used
	 * as fallback for all exceptions; any further HandlerExceptionResolvers in the chain will be
	 * ignored in this case.
	 */
	public void setMappedHandlerClasses(Class[] mappedHandlerClasses) {
		this.mappedHandlerClasses = mappedHandlerClasses;
	}

	/**
	 * Set the log category for warn logging. The name will be passed to the underlying logger
	 * implementation through Commons Logging, getting interpreted as log category according
	 * to the logger's configuration.
	 * <p>Default is no warn logging. Specify this setting to activate warn logging into a specific
	 * category. Alternatively, override the {@link #logException} method for custom logging.
	 * @see org.apache.commons.logging.LogFactory#getLog(String)
	 * @see org.apache.log4j.Logger#getLogger(String)
	 * @see java.util.logging.Logger#getLogger(String)
	 */
	public void setWarnLogCategory(String loggerName) {
		this.warnLogger = LogFactory.getLog(loggerName);
	}

	/**
	 * Specify whether to prevent HTTP response caching for any view resolved
	 * by this HandlerExceptionResolver.
	 * <p>Default is "false". Switch this to "true" in order to automatically
	 * generate HTTP response headers that suppress response caching.
	 */
	public void setPreventResponseCaching(boolean preventResponseCaching) {
		this.preventResponseCaching = preventResponseCaching;
	}


	/**
	 * Checks whether this resolver is supposed to apply (i.e. the handler matches
	 * in case of "mappedHandlers" having been specified), then delegates to the
	 * {@link #doResolveException} template method.
	 */
	@Override
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
			Object handler, Exception ex) {

		if (shouldApplyTo(request, handler)) {
			// Log exception, both at debug log level and at warn level, if desired.
			if (logger.isDebugEnabled()) {
				logger.debug("Resolving exception from handler [" + handler + "]: " + ex);
			}
			logException(ex, request);
			prepareResponse(ex, response);
			return doResolveException(request, response, handler, ex);
		}
		else {
			return null;
		}
	}

	/**
	 * Check whether this resolver is supposed to apply to the given handler.
	 * <p>The default implementation checks against the specified mapped handlers
	 * and handler classes, if any.
	 * @param request current HTTP request
	 * @param handler the executed handler, or {@code null} if none chosen
	 * at the time of the exception (for example, if multipart resolution failed)
	 * @return whether this resolved should proceed with resolving the exception
	 * for the given request and handler
	 * @see #setMappedHandlers
	 * @see #setMappedHandlerClasses
	 */
	protected boolean shouldApplyTo(HttpServletRequest request, Object handler) {
		if (handler != null) {
			if (this.mappedHandlers != null && this.mappedHandlers.contains(handler)) {
				return true;
			}
			if (this.mappedHandlerClasses != null) {
				for (Class handlerClass : this.mappedHandlerClasses) {
					if (handlerClass.isInstance(handler)) {
						return true;
					}
				}
			}
		}
		// Else only apply if there are no explicit handler mappings.
		return (this.mappedHandlers == null && this.mappedHandlerClasses == null);
	}

	/**
	 * Log the given exception at warn level, provided that warn logging has been
	 * activated through the {@link #setWarnLogCategory "warnLogCategory"} property.
	 * <p>Calls {@link #buildLogMessage} in order to determine the concrete message to log.
	 * Always passes the full exception to the logger.
	 * @param ex the exception that got thrown during handler execution
	 * @param request current HTTP request (useful for obtaining metadata)
	 * @see #setWarnLogCategory
	 * @see #buildLogMessage
	 * @see org.apache.commons.logging.Log#warn(Object, Throwable)
	 */
	protected void logException(Exception ex, HttpServletRequest request) {
		if (this.warnLogger != null && this.warnLogger.isWarnEnabled()) {
			this.warnLogger.warn(buildLogMessage(ex, request), ex);
		}
	}

	/**
	 * Build a log message for the given exception, occured during processing the given request.
	 * @param ex the exception that got thrown during handler execution
	 * @param request current HTTP request (useful for obtaining metadata)
	 * @return the log message to use
	 */
	protected String buildLogMessage(Exception ex, HttpServletRequest request) {
		return "Handler execution resulted in exception";
	}

	/**
	 * Prepare the response for the exceptional case.
	 * <p>The default implementation prevents the response from being cached,
	 * if the {@link #setPreventResponseCaching "preventResponseCaching"} property
	 * has been set to "true".
	 * @param ex the exception that got thrown during handler execution
	 * @param response current HTTP response
	 * @see #preventCaching
	 */
	protected void prepareResponse(Exception ex, HttpServletResponse response) {
		if (this.preventResponseCaching) {
			preventCaching(response);
		}
	}

	/**
	 * Prevents the response from being cached, through setting corresponding
	 * HTTP headers. See {@code http://www.mnot.net/cache_docs}.
	 * @param response current HTTP response
	 */
	protected void preventCaching(HttpServletResponse response) {
		response.setHeader(HEADER_PRAGMA, "no-cache");
		response.setDateHeader(HEADER_EXPIRES, 1L);
		response.setHeader(HEADER_CACHE_CONTROL, "no-cache");
		response.addHeader(HEADER_CACHE_CONTROL, "no-store");
	}


	/**
	 * Actually resolve the given exception that got thrown during on handler execution,
	 * returning a ModelAndView that represents a specific error page if appropriate.
	 * <p>May be overridden in subclasses, in order to apply specific exception checks.
	 * Note that this template method will be invoked <i>after</i> checking whether this
	 * resolved applies ("mappedHandlers" etc), so an implementation may simply proceed
	 * with its actual exception handling.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler, or {@code null} if none chosen at the time
	 * of the exception (for example, if multipart resolution failed)
	 * @param ex the exception that got thrown during handler execution
	 * @return a corresponding ModelAndView to forward to, or {@code null} for default processing
	 */
	protected abstract ModelAndView doResolveException(HttpServletRequest request,
			HttpServletResponse response, Object handler, Exception ex);

}
