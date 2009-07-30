/*
 * Copyright 2002-2008 the original author or authors.
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
import javax.portlet.MimeResponse;
import javax.portlet.PortletRequest;

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
 * @author Arjen Poutsma
 * @since 2.0
 */
public class SimpleMappingExceptionResolver extends AbstractHandlerExceptionResolver {

	/**
	 * The default name of the exception attribute: "exception".
	 */
	public static final String DEFAULT_EXCEPTION_ATTRIBUTE = "exception";

	private Properties exceptionMappings;

	private String defaultErrorView;

	private String exceptionAttribute = DEFAULT_EXCEPTION_ATTRIBUTE;

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
	 * Actually resolve the given exception that got thrown during on handler execution,
	 * returning a ModelAndView that represents a specific error page if appropriate.
	 * @param request current portlet request
	 * @param response current portlet response
	 * @param handler the executed handler, or null if none chosen at the time of
	 * the exception (for example, if multipart resolution failed)
	 * @param ex the exception that got thrown during handler execution
	 * @return a corresponding ModelAndView to forward to, or null for default processing
	 */
	@Override
	protected ModelAndView doResolveException(
			PortletRequest request, MimeResponse response, Object handler, Exception ex) {

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
	 * Determine the view name for the given exception, searching the
	 * {@link #setExceptionMappings "exceptionMappings"}, using the
	 * {@link #setDefaultErrorView "defaultErrorView"} as fallback.
	 * @param ex the exception that got thrown during handler execution
	 * @param request current portlet request (useful for obtaining metadata)
	 * @return the resolved view name, or <code>null</code> if none found
	 */
	protected String determineViewName(Exception ex, PortletRequest request) {
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
		if (exceptionClass.getName().contains(exceptionMapping)) {
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
	protected ModelAndView getModelAndView(String viewName, Exception ex, PortletRequest request) {
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
