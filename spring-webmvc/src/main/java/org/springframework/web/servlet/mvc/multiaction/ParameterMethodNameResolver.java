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

package org.springframework.web.servlet.mvc.multiaction;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * Implementation of {@link MethodNameResolver} which supports several strategies
 * for mapping parameter values to the names of methods to invoke.
 *
 * <p>The simplest strategy looks for a specific named parameter, whose value is
 * considered the name of the method to invoke. The name of the parameter may be
 * specified as a JavaBean property, if the default <code>action</code> is not
 * acceptable.
 *
 * <p>The alternative strategy uses the very existence of a request parameter (
 * i.e. a request parameter with a certain name is found) as an indication that a
 * method with the same name should be dispatched to. In this case, the actual
 * request parameter value is ignored. The list of parameter/method names may
 * be set via the <code>methodParamNames<code> JavaBean property.
 *
 * <p>The second resolution strategy is primarily expected to be used with web
 * pages containing multiple submit buttons. The 'name' attribute of each
 * button should be set to the mapped method name, while the 'value' attribute
 * is normally displayed as the button label by the browser, and will be
 * ignored by the resolver.
 *
 * <p>Note that the second strategy also supports the use of submit buttons of
 * type 'image'. That is, an image submit button named 'reset' will normally be
 * submitted by the browser as two request paramters called 'reset.x', and
 * 'reset.y'. When checking for the existence of a paramter from the
 * <code>methodParamNames</code> list, to indicate that a specific method should
 * be called, the code will look for request parameter in the "reset" form
 * (exactly as spcified in the list), and in the "reset.x" form ('.x' appended to
 * the name in the list). In this way it can handle both normal and image submit
 * buttons. The actual method name resolved if there is a match will always be
 * the bare form without the ".x".
 *
 * <p><b>Note:</b> If both strategies are configured, i.e. both "paramName"
 * and "methodParamNames" are specified, then both will be checked for any given
 * request. A match for an explicit request parameter in the "methodParamNames"
 * list always wins over a value specified for a "paramName" action parameter.
 *
 * <p>For use with either strategy, the name of a default handler method to use
 * when there is no match, can be specified as a JavaBean property.
 *
 * <p>For both resolution strategies, the method name is of course coming from
 * some sort of view code, (such as a JSP page). While this may be acceptable,
 * it is sometimes desireable to treat this only as a 'logical' method name,
 * with a further mapping to a 'real' method name. As such, an optional
 * 'logical' mapping may be specified for this purpose.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 * @see #setParamName
 * @see #setMethodParamNames
 * @see #setLogicalMappings
 * @see #setDefaultMethodName
 */
public class ParameterMethodNameResolver implements MethodNameResolver {

	/**
	 * Default name for the parameter whose value identifies the method to invoke:
	 * "action".
	 */
	public static final String DEFAULT_PARAM_NAME = "action";


	protected final Log logger = LogFactory.getLog(getClass());

	private String paramName = DEFAULT_PARAM_NAME;

	private String[] methodParamNames;

	private Properties logicalMappings;

	private String defaultMethodName;


	/**
	 * Set the name of the parameter whose <i>value</i> identifies the name of
	 * the method to invoke. Default is "action".
	 * <p>Alternatively, specify parameter names where the very existence of each
	 * parameter means that a method of the same name should be invoked, via
	 * the "methodParamNames" property.
	 * @see #setMethodParamNames
	 */
	public void setParamName(String paramName) {
		if (paramName != null) {
			Assert.hasText(paramName, "'paramName' must not be empty");
		}
		this.paramName = paramName;
	}

	/**
	 * Set a String array of parameter names, where the <i>very existence of a
	 * parameter</i> in the list (with value ignored) means that a method of the
	 * same name should be invoked. This target method name may then be optionally
	 * further mapped via the {@link #logicalMappings} property, in which case it
	 * can be considered a logical name only.
	 * @see #setParamName
	 */
	public void setMethodParamNames(String[] methodParamNames) {
		this.methodParamNames = methodParamNames;
	}

	/**
	 * Specifies a set of optional logical method name mappings. For both resolution
	 * strategies, the method name initially comes in from the view layer. If that needs
	 * to be treated as a 'logical' method name, and mapped to a 'real' method name, then
	 * a name/value pair for that purpose should be added to this Properties instance.
	 * Any method name not found in this mapping will be considered to already be the
	 * real method name.
	 * <p>Note that in the case of no match, where the {@link #defaultMethodName} property
	 * is used if available, that method name is considered to already be the real method
	 * name, and is not run through the logical mapping.
	 * @param logicalMappings a Properties object mapping logical method names to real
	 * method names
	 */
	public void setLogicalMappings(Properties logicalMappings) {
		this.logicalMappings = logicalMappings;
	}

	/**
	 * Set the name of the default handler method that should be
	 * used when no parameter was found in the request
	 */
	public void setDefaultMethodName(String defaultMethodName) {
		if (defaultMethodName != null) {
			Assert.hasText(defaultMethodName, "'defaultMethodName' must not be empty");
		}
		this.defaultMethodName = defaultMethodName;
	}


	public String getHandlerMethodName(HttpServletRequest request) throws NoSuchRequestHandlingMethodException {
		String methodName = null;

		// Check parameter names where the very existence of each parameter
		// means that a method of the same name should be invoked, if any.
		if (this.methodParamNames != null) {
			for (String candidate : this.methodParamNames) {
				if (WebUtils.hasSubmitParameter(request, candidate)) {
					methodName = candidate;
					if (logger.isDebugEnabled()) {
						logger.debug("Determined handler method '" + methodName +
								"' based on existence of explicit request parameter of same name");
					}
					break;
				}
			}
		}

		// Check parameter whose value identifies the method to invoke, if any.
		if (methodName == null && this.paramName != null) {
			methodName = request.getParameter(this.paramName);
			if (methodName != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Determined handler method '" + methodName +
							"' based on value of request parameter '" + this.paramName + "'");
				}
			}
		}

		if (methodName != null && this.logicalMappings != null) {
			// Resolve logical name into real method name, if appropriate.
			String originalName = methodName;
			methodName = this.logicalMappings.getProperty(methodName, methodName);
			if (logger.isDebugEnabled()) {
				logger.debug("Resolved method name '" + originalName + "' to handler method '" + methodName + "'");
			}
		}

		if (methodName != null && !StringUtils.hasText(methodName)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Method name '" + methodName + "' is empty: treating it as no method name found");
			}
			methodName = null;
		}

		if (methodName == null) {
			if (this.defaultMethodName != null) {
				// No specific method resolved: use default method.
				methodName = this.defaultMethodName;
				if (logger.isDebugEnabled()) {
					logger.debug("Falling back to default handler method '" + this.defaultMethodName + "'");
				}
			}
			else {
				// If resolution failed completely, throw an exception.
				throw new NoSuchRequestHandlingMethodException(request);
			}
		}

		return methodName;
	}

}
