/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.web.portlet.handler;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.Ordered;
import org.springframework.web.portlet.HandlerExceptionResolver;
import org.springframework.web.portlet.ModelAndView;

/**
 * {@link org.springframework.web.portlet.HandlerExceptionResolver} implementation
 * that allows for mapping exception class names to view names, either for a
 * set of given handlers or for all handlers in the DispatcherPortlet.
 *
 * <p>Error views are analogous to error page JSPs, but can be used with any
 * kind of exception including any checked one, with fine-granular mappings for
 * specific handlers.
 *
 * @author Juergen Hoeller
 * @author John A. Lewis
 * @since 2.0
 */
public class SimpleMappingExceptionResolver implements HandlerExceptionResolver, Ordered {

	/**
	 * The default name of the exception attribute: "exception".
	 */
	public static final String DEFAULT_EXCEPTION_ATTRIBUTE = "exception";


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private int order = Integer.MAX_VALUE;  // default: same as non-Ordered

	private Set mappedHandlers;

	private Class[] mappedHandlerClasses;

	private boolean renderWhenMinimized = false;

	private Log warnLogger;

	private Properties exceptionMappings;

	private String defaultErrorView;

	private String exceptionAttribute = DEFAULT_EXCEPTION_ATTRIBUTE;


	public void setOrder(int order) {
		this.order = order;
	}

	public int getOrder() {
		return this.order;
	}

	/**
	 * Specify the set of handlers that this exception resolver should map.
	 * The exception mappings and the default error view will only apply
	 * to the specified handlers.
	 * <p>If no handlers set, both the exception mappings and the default error
	 * view will apply to all handlers. This means that a specified default
	 * error view will be used as fallback for all exceptions; any further
	 * HandlerExceptionResolvers in the chain will be ignored in this case.
	 */
	public void setMappedHandlers(Set mappedHandlers) {
		this.mappedHandlers = mappedHandlers;
	}

	/**
	 * Specify the set of classes that this exception resolver should apply to.
	 * The exception mappings and the default error view will only apply
	 * to handlers of the specified type; the specified types may be interfaces
	 * and superclasses of handlers as well.
	 * <p>If no handlers and handler classes are set, the exception mappings
	 * and the default error view will apply to all handlers. This means that
	 * a specified default error view will be used as fallback for all exceptions;
	 * any further HandlerExceptionResolvers in the chain will be ignored in
	 * this case.
	 */
	public void setMappedHandlerClasses(Class[] mappedHandlerClasses) {
		this.mappedHandlerClasses = mappedHandlerClasses;
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
	 * Set the log category for warn logging. The name will be passed to the
	 * underlying logger implementation through Commons Logging, getting
	 * interpreted as log category according to the logger's configuration.
	 * <p>Default is no warn logging. Specify this setting to activate
	 * warn logging into a specific category. Alternatively, override
	 * the {@link #logException} method for custom logging.
	 * @see org.apache.commons.logging.LogFactory#getLog(String)
	 * @see org.apache.log4j.Logger#getLogger(String)
	 * @see java.util.logging.Logger#getLogger(String)
	 */
	public void setWarnLogCategory(String loggerName) {
		this.warnLogger = LogFactory.getLog(loggerName);
	}

	/**
	 * Set the mappings between exception class names and error view names.
	 * The exception class name can be a substring, with no wildcard support
	 * at present. A value of "PortletException" would match
	 * <code>javax.portet.PortletException</code> and subclasses, for example.
	 * <p><b>NB:</b> Consider carefully how specific the pattern is, and whether
	 * to include package information (which isn't mandatory). For example,
	 * "Exception" will match nearly anything, and will probably hide other rules.
	 * "java.lang.Exception" would be correct if "Exception" was meant to define
	 * a rule for all checked exceptions. With more unusual exception names such
	 * as "BaseBusinessException" there's no need to use a FQN.
	 * <p>Follows the same matching algorithm as RuleBasedTransactionAttribute
	 * and RollbackRuleAttribute.
	 * @param mappings exception patterns (can also be fully qualified class names)
	 * as keys, and error view names as values
	 * @see org.springframework.transaction.interceptor.RuleBasedTransactionAttribute
	 * @see org.springframework.transaction.interceptor.RollbackRuleAttribute
	 */
	public void setExceptionMappings(Properties mappings) {
		this.exceptionMappings = mappings;
	}

	/**
	 * Set the name of the default error view.
	 * This view will be returned if no specific mapping was found.
	 * <p>Default is none.
	 */
	public void setDefaultErrorView(String defaultErrorView) {
		this.defaultErrorView = defaultErrorView;
	}

	/**
	 * Set the name of the model attribute as which the exception should
	 * be exposed. Default is "exception".
	 * @see #DEFAULT_EXCEPTION_ATTRIBUTE
	 */
	public void setExceptionAttribute(String exceptionAttribute) {
		this.exceptionAttribute = exceptionAttribute;
	}


	/**
	 * Checks whether this resolver is supposed to apply (i.e. the handler
	 * matches in case of "mappedHandlers" having been specified), then
	 * delegates to the {@link #doResolveException} template method.
	 */
	public ModelAndView resolveException(
			RenderRequest request, RenderResponse response, Object handler, Exception ex) {

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
	 * and handler classes, if any, and alspo checks the window state (according
	 * to the "renderWhenMinimize" property).
	 * @param request current portlet request
	 * @param handler the executed handler, or <code>null</code> if none chosen at the
	 * time of the exception (for example, if multipart resolution failed)
	 * @return whether this resolved should proceed with resolving the exception
	 * for the given request and handler
	 * @see #setMappedHandlers
	 * @see #setMappedHandlerClasses
	 */
	protected boolean shouldApplyTo(RenderRequest request, Object handler) {
		// If the portlet is minimized and we don't want to render then return null.
		if (WindowState.MINIMIZED.equals(request.getWindowState()) && !this.renderWhenMinimized) {
			return false;
		}

		if (handler != null) {
			if (this.mappedHandlers != null && this.mappedHandlers.contains(handler)) {
				return true;
			}
			if (this.mappedHandlerClasses != null) {
				for (int i = 0; i < this.mappedHandlerClasses.length; i++) {
					if (this.mappedHandlerClasses[i].isInstance(handler)) {
						return true;
					}
				}
			}
		}
		// Else only apply if there are no explicit handler mappings.
		return (this.mappedHandlers == null && this.mappedHandlerClasses == null);
	}

	/**
	 * Actually resolve the given exception that got thrown during on handler execution,
	 * returning a ModelAndView that represents a specific error page if appropriate.
	 * @param request current portlet request
	 * @param response current portlet response
	 * @param handler the executed handler, or null if none chosen at the time of
	 * the exception (for example, if multipart resolution failed)
	 * @param ex the exception that got thrown during handler execution
	 * @return a corresponding ModelAndView to forward to, or null for default processing
	 */
	protected ModelAndView doResolveException(
			RenderRequest request, RenderResponse response, Object handler, Exception ex) {

		// Log exception, both at debug log level and at warn level, if desired.
		if (logger.isDebugEnabled()) {
			logger.debug("Resolving exception from handler [" + handler + "]: " + ex);
		}
		logException(ex, request);

		// Expose ModelAndView for chosen error view.
		String viewName = determineViewName(ex, request);
		if (viewName != null) {
			return getModelAndView(viewName, ex, request);
		}
		else {
			return null;
		}
	}


	/**
	 * Log the given exception at warn level, provided that warn logging has been
	 * activated through the {@link #setWarnLogCategory "warnLogCategory"} property.
	 * <p>Calls {@link #buildLogMessage} in order to determine the concrete message
	 * to log. Always passes the full exception to the logger.
	 * @param ex the exception that got thrown during handler execution
	 * @param request current portlet request (useful for obtaining metadata)
	 * @see #setWarnLogCategory
	 * @see #buildLogMessage
	 * @see org.apache.commons.logging.Log#warn(Object, Throwable)
	 */
	protected void logException(Exception ex, RenderRequest request) {
		if (this.warnLogger != null && this.warnLogger.isWarnEnabled()) {
			this.warnLogger.warn(buildLogMessage(ex, request), ex);
		}
	}

	/**
	 * Build a log message for the given exception, occured during processing
	 * the given request.
	 * @param ex the exception that got thrown during handler execution
	 * @param request current portlet request (useful for obtaining metadata)
	 * @return the log message to use
	 */
	protected String buildLogMessage(Exception ex, RenderRequest request) {
		return "Handler execution resulted in exception";
	}


	/**
	 * Determine the view name for the given exception, searching the
	 * {@link #setExceptionMappings "exceptionMappings"}, using the
	 * {@link #setDefaultErrorView "defaultErrorView"} as fallback.
	 * @param ex the exception that got thrown during handler execution
	 * @param request current portlet request (useful for obtaining metadata)
	 * @return the resolved view name, or <code>null</code> if none found
	 */
	protected String determineViewName(Exception ex, RenderRequest request) {
		String viewName = null;
		// Check for specific exception mappings.
		if (this.exceptionMappings != null) {
			viewName = findMatchingViewName(this.exceptionMappings, ex);
		}
		// Return default error view else, if defined.
		if (viewName == null && this.defaultErrorView != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Resolving to default view '" + this.defaultErrorView +
						"' for exception of type [" + ex.getClass().getName() + "]");
			}
			viewName = this.defaultErrorView;
		}
		return viewName;
	}

	/**
	 * Find a matching view name in the given exception mappings
	 * @param exceptionMappings mappings between exception class names and error view names
	 * @param ex the exception that got thrown during handler execution
	 * @return the view name, or <code>null</code> if none found
	 * @see #setExceptionMappings
	 */
	protected String findMatchingViewName(Properties exceptionMappings, Exception ex) {
		String viewName = null;
		String dominantMapping = null;
		int deepest = Integer.MAX_VALUE;
		for (Enumeration names = exceptionMappings.propertyNames(); names.hasMoreElements();) {
			String exceptionMapping = (String) names.nextElement();
			int depth = getDepth(exceptionMapping, ex);
			if (depth >= 0 && depth < deepest) {
				deepest = depth;
				dominantMapping = exceptionMapping;
				viewName = exceptionMappings.getProperty(exceptionMapping);
			}
		}
		if (viewName != null && logger.isDebugEnabled()) {
			logger.debug("Resolving to view '" + viewName + "' for exception of type [" + ex.getClass().getName() +
					"], based on exception mapping [" + dominantMapping + "]");
		}
		return viewName;
	}

	/**
	 * Return the depth to the superclass matching.
	 * <p>0 means ex matches exactly. Returns -1 if there's no match.
	 * Otherwise, returns depth. Lowest depth wins.
	 * <p>Follows the same algorithm as
	 * {@link org.springframework.transaction.interceptor.RollbackRuleAttribute}.
	 */
	protected int getDepth(String exceptionMapping, Exception ex) {
		return getDepth(exceptionMapping, ex.getClass(), 0);
	}

	private int getDepth(String exceptionMapping, Class exceptionClass, int depth) {
		if (exceptionClass.getName().indexOf(exceptionMapping) != -1) {
			// Found it!
			return depth;
		}
		// If we've gone as far as we can go and haven't found it...
		if (exceptionClass.equals(Throwable.class)) {
			return -1;
		}
		return getDepth(exceptionMapping, exceptionClass.getSuperclass(), depth + 1);
	}


	/**
	 * Return a ModelAndView for the given request, view name and exception.
	 * Default implementation delegates to <code>getModelAndView(viewName, ex)</code>.
	 * @param viewName the name of the error view
	 * @param ex the exception that got thrown during handler execution
	 * @param request current portlet request (useful for obtaining metadata)
	 * @return the ModelAndView instance
	 * @see #getModelAndView(String, Exception)
	 */
	protected ModelAndView getModelAndView(String viewName, Exception ex, RenderRequest request) {
		return getModelAndView(viewName, ex);
	}

	/**
	 * Return a ModelAndView for the given view name and exception.
	 * Default implementation adds the specified exception attribute.
	 * Can be overridden in subclasses.
	 * @param viewName the name of the error view
	 * @param ex the exception that got thrown during handler execution
	 * @return the ModelAndView instance
	 * @see #setExceptionAttribute
	 */
	protected ModelAndView getModelAndView(String viewName, Exception ex) {
		ModelAndView mv = new ModelAndView(viewName);
		if (this.exceptionAttribute != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Exposing Exception as model attribute '" + this.exceptionAttribute + "'");
			}
			mv.addObject(this.exceptionAttribute, ex);
		}
		return mv;
	}

}
