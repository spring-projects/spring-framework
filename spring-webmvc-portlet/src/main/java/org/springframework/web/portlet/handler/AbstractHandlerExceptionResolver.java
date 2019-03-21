/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.portlet.handler;

import java.util.Set;
import javax.portlet.MimeResponse;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.WindowState;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.Ordered;
import org.springframework.web.portlet.HandlerExceptionResolver;
import org.springframework.web.portlet.ModelAndView;

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

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private int order = Ordered.LOWEST_PRECEDENCE;

	private Set<?> mappedHandlers;

	private Class<?>[] mappedHandlerClasses;

	private Log warnLogger;

	private boolean renderWhenMinimized = false;


	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Specify the set of handlers that this exception resolver should apply to.
	 * <p>The exception mappings and the default error view will only apply to the specified handlers.
	 * <p>If no handlers or handler classes are set, the exception mappings and the default error
	 * view will apply to all handlers. This means that a specified default error view will be used
	 * as a fallback for all exceptions; any further HandlerExceptionResolvers in the chain will be
	 * ignored in this case.
	 */
	public void setMappedHandlers(Set<?> mappedHandlers) {
		this.mappedHandlers = mappedHandlers;
	}

	/**
	 * Specify the set of classes that this exception resolver should apply to.
	 * <p>The exception mappings and the default error view will only apply to handlers of the
	 * specified types; the specified types may be interfaces or superclasses of handlers as well.
	 * <p>If no handlers or handler classes are set, the exception mappings and the default error
	 * view will apply to all handlers. This means that a specified default error view will be used
	 * as a fallback for all exceptions; any further HandlerExceptionResolvers in the chain will be
	 * ignored in this case.
	 */
	public void setMappedHandlerClasses(Class<?>... mappedHandlerClasses) {
		this.mappedHandlerClasses = mappedHandlerClasses;
	}

	/**
	 * Set the log category for warn logging. The name will be passed to the underlying logger
	 * implementation through Commons Logging, getting interpreted as a log category according
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
	 * Set if the resolver should render a view when the portlet is in
	 * a minimized window. The default is "false".
	 * @see javax.portlet.RenderRequest#getWindowState()
	 * @see javax.portlet.WindowState#MINIMIZED
	 */
	public void setRenderWhenMinimized(boolean renderWhenMinimized) {
		this.renderWhenMinimized = renderWhenMinimized;
	}


	/**
	 * Checks whether this resolver is supposed to apply (i.e. the handler
	 * matches in case of "mappedHandlers" having been specified), then
	 * delegates to the {@link #doResolveException} template method.
	 */
	@Override
	public ModelAndView resolveException(RenderRequest request, RenderResponse response, Object handler, Exception ex) {
		if (shouldApplyTo(request, handler)) {
			return doResolveException(request, response, handler, ex);
		}
		else {
			return null;
		}
	}

	@Override
	public ModelAndView resolveException(ResourceRequest request, ResourceResponse response, Object handler, Exception ex) {
		if (shouldApplyTo(request, handler)) {
			return doResolveException(request, response, handler, ex);
		}
		else {
			return null;
		}
	}

	/**
	 * Check whether this resolver is supposed to apply to the given handler.
	 * <p>The default implementation checks against the specified mapped handlers
	 * and handler classes, if any, and also checks the window state (according
	 * to the "renderWhenMinimize" property).
	 * @param request current portlet request
	 * @param handler the executed handler, or {@code null} if none chosen at the
	 * time of the exception (for example, if multipart resolution failed)
	 * @return whether this resolved should proceed with resolving the exception
	 * for the given request and handler
	 * @see #setMappedHandlers
	 * @see #setMappedHandlerClasses
	 */
	protected boolean shouldApplyTo(PortletRequest request, Object handler) {
		// If the portlet is minimized and we don't want to render then return null.
		if (WindowState.MINIMIZED.equals(request.getWindowState()) && !this.renderWhenMinimized) {
			return false;
		}
		// Check mapped handlers...
		if (handler != null) {
			if (this.mappedHandlers != null && this.mappedHandlers.contains(handler)) {
				return true;
			}
			if (this.mappedHandlerClasses != null) {
				for (Class<?> mappedClass : this.mappedHandlerClasses) {
					if (mappedClass.isInstance(handler)) {
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
	 * @param ex the exception that got thrown during handler execution
	 * @param request current portlet request (useful for obtaining metadata)
	 * @see #setWarnLogCategory
	 * @see #buildLogMessage
	 * @see org.apache.commons.logging.Log#warn(Object, Throwable)
	 */
	protected void logException(Exception ex, PortletRequest request) {
		if (this.warnLogger != null && this.warnLogger.isWarnEnabled()) {
			this.warnLogger.warn(buildLogMessage(ex, request));
		}
	}

	/**
	 * Build a log message for the given exception, occurred during processing the given request.
	 * @param ex the exception that got thrown during handler execution
	 * @param request current portlet request (useful for obtaining metadata)
	 * @return the log message to use
	 */
	protected String buildLogMessage(Exception ex, PortletRequest request) {
		return "Handler execution resulted in exception: " + ex;
	}


	/**
	 * Actually resolve the given exception that got thrown during on handler execution,
	 * returning a ModelAndView that represents a specific error page if appropriate.
	 * <p>Must be overridden in subclasses, in order to apply specific exception checks.
	 * Note that this template method will be invoked <i>after</i> checking whether this
	 * resolved applies ("mappedHandlers" etc), so an implementation may simply proceed
	 * with its actual exception handling.
	 * @param request current portlet request
	 * @param response current portlet response
	 * @param handler the executed handler, or null if none chosen at the time of
	 * the exception (for example, if multipart resolution failed)
	 * @param ex the exception that got thrown during handler execution
	 * @return a corresponding ModelAndView to forward to, or null for default processing
	 */
	protected abstract ModelAndView doResolveException(PortletRequest request, MimeResponse response,
			Object handler, Exception ex);

}
