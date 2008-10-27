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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.portlet.PortletMode;
import javax.portlet.PortletRequest;

import org.springframework.beans.BeansException;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Implementation of the {@link org.springframework.web.portlet.HandlerMapping}
 * interface to map from the current PortletMode and a request parameter to
 * request handler beans. The mapping consists of two levels: first the
 * PortletMode and then the parameter value. In order to be mapped,
 * both elements must match the mapping definition.
 *
 * <p>This is a combination of the methods used in {@link PortletModeHandlerMapping PortletModeHandlerMapping}
 * and {@link ParameterHandlerMapping ParameterHandlerMapping}.  Unlike
 * those two classes, this mapping cannot be initialized with properties since it
 * requires a two-level map.
 *
 * <p>The default name of the parameter is "action", but can be changed using
 * {@link #setParameterName setParameterName()}.
 *
 * <p>By default, the same parameter value may not be used in two different portlet
 * modes.  This is so that if the portal itself changes the portlet mode, the request
 * will no longer be valid in the mapping.  This behavior can be changed with
 * {@link #setAllowDuplicateParameters setAllowDupParameters()}.
 *
 * <p>The bean configuration for this mapping will look somthing like this:
 *
 * <pre class="code">
 * &lt;bean id="portletModeParameterHandlerMapping" class="org.springframework.web.portlet.handler.PortletModeParameterHandlerMapping"&gt;
 *   &lt;property name="portletModeParameterMap"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="view"&gt; &lt;!-- portlet mode: view --&gt;
 *         &lt;map&gt;
 *           &lt;entry key="add"&gt;&lt;ref bean="addItemHandler"/&gt;&lt;/entry&gt;
 *           &lt;entry key="edit"&gt;&lt;ref bean="editItemHandler"/&gt;&lt;/entry&gt;
 *           &lt;entry key="delete"&gt;&lt;ref bean="deleteItemHandler"/&gt;&lt;/entry&gt;
 *         &lt;/map&gt;
 *       &lt;/entry&gt;
 *       &lt;entry key="edit"&gt; &lt;!-- portlet mode: edit --&gt;
 *         &lt;map&gt;
 *           &lt;entry key="prefs"&gt;&lt;ref bean="preferencesHandler"/&gt;&lt;/entry&gt;
 *           &lt;entry key="resetPrefs"&gt;&lt;ref bean="resetPreferencesHandler"/&gt;&lt;/entry&gt;
 *         &lt;/map&gt;
 *       &lt;/entry&gt;
 *     &lt;/map&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>This mapping can be chained ahead of a {@link PortletModeHandlerMapping PortletModeHandlerMapping},
 * which can then provide defaults for each mode and an overall default as well.
 *
 * <p>Thanks to Rainer Schmitz and Yujin Kim for suggesting this mapping strategy!
 *
 * @author John A. Lewis
 * @author Juergen Hoeller
 * @since 2.0
 * @see ParameterMappingInterceptor
 */
public class PortletModeParameterHandlerMapping extends AbstractMapBasedHandlerMapping {

	/**
	 * Default request parameter name to use for mapping to handlers: "action".
	 */
	public final static String DEFAULT_PARAMETER_NAME = "action";


	private String parameterName = DEFAULT_PARAMETER_NAME;

	private Map portletModeParameterMap;

	private boolean allowDuplicateParameters = false;

	private final Set parametersUsed = new HashSet();


	/**
	 * Set the name of the parameter used for mapping to handlers.
	 * <p>Default is "action".
	 */
	public void setParameterName(String parameterName) {
		Assert.hasText(parameterName, "'parameterName' must not be empty");
		this.parameterName = parameterName;
	}

	/**
	 * Set a Map with portlet mode names as keys and another Map as values.
	 * The sub-map has parameter names as keys and handler bean or bean names as values.
	 * <p>Convenient for population with bean references.
	 * @param portletModeParameterMap two-level map of portlet modes and parameters to handler beans
	 */
	public void setPortletModeParameterMap(Map portletModeParameterMap) {
		this.portletModeParameterMap = portletModeParameterMap;
	}

	/**
	 * Set whether to allow duplicate parameter values across different portlet modes.
	 * Default is "false".
	 * <p>Doing this is dangerous because the portlet mode can be changed by the
	 * portal itself and the only way to see that is a rerender of the portlet.
	 * If the same parameter value is legal in multiple modes, then a change in
	 * mode could result in a matched mapping that is not intended and the user
	 * could end up in a strange place in the application.
	 */
	public void setAllowDuplicateParameters(boolean allowDuplicateParameters) {
		this.allowDuplicateParameters = allowDuplicateParameters;
	}


	/**
	 * Calls the <code>registerHandlers</code> method in addition
	 * to the superclass's initialization.
	 * @see #registerHandlers
	 */
	public void initApplicationContext() throws BeansException {
		super.initApplicationContext();
		registerHandlers(this.portletModeParameterMap);
	}

	/**
	 * Register all handlers specified in the Portlet mode map for the corresponding modes.
	 * @param portletModeParameterMap Map with mode names as keys and parameter Maps as values
	 * @throws BeansException if the handler couldn't be registered
	 */
	protected void registerHandlers(Map portletModeParameterMap) throws BeansException {
		if (CollectionUtils.isEmpty(portletModeParameterMap)) {
			logger.warn("'portletModeParameterMap' not set on PortletModeParameterHandlerMapping");
		}
		else {
			for (Iterator it = portletModeParameterMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				String modeKey = (String) entry.getKey();
				PortletMode mode = new PortletMode(modeKey);
				Object parameterMap = entry.getValue();
				if (!(parameterMap instanceof Map)) {
					throw new IllegalArgumentException(
							"The value for the portlet mode must be a Map of parameter Strings to handler Objects");
				}
				registerHandler(mode, (Map) parameterMap);
			}
		}
	}

	/**
	 * Register all handlers specified in the given parameter map.
	 * @param parameterMap Map with parameter names as keys and handler beans or bean names as values
	 * @throws BeansException if the handler couldn't be registered
	 */
	protected void registerHandler(PortletMode mode, Map parameterMap) throws BeansException {
		for (Iterator it = parameterMap.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			String parameter = (String) entry.getKey();
			Object handler = entry.getValue();
			registerHandler(mode, parameter, handler);
		}
	}

	/**
	 * Register the given handler instance for the given PortletMode and parameter value,
	 * under an appropriate lookup key.
	 * @param mode the PortletMode for which this mapping is valid
	 * @param parameter the parameter value to which this handler is mapped
	 * @param handler the handler instance bean
	 * @throws BeansException if the handler couldn't be registered
	 * @throws IllegalStateException if there is a conflicting handler registered
	 * @see #registerHandler(Object, Object)
	 */
	protected void registerHandler(PortletMode mode, String parameter, Object handler)
			throws BeansException, IllegalStateException {

		// Check for duplicate parameter values across all portlet modes.
		if (!this.allowDuplicateParameters && this.parametersUsed.contains(parameter)) {
			throw new IllegalStateException(
					"Duplicate entries for parameter [" + parameter + "] in different Portlet modes");
		}
		this.parametersUsed.add(parameter);

		registerHandler(new LookupKey(mode, parameter), handler);
	}


	/**
	 * Returns a lookup key that combines the current PortletMode and the current
	 * value of the specified parameter.
	 * @see javax.portlet.PortletRequest#getPortletMode()
	 * @see #setParameterName
	 */
	protected Object getLookupKey(PortletRequest request) throws Exception {
		PortletMode mode = request.getPortletMode();
		String parameter = request.getParameter(this.parameterName);
		return new LookupKey(mode, parameter);
	}


	/**
	 * Internal class used as lookup key, combining PortletMode and parameter value.
	 */
	private static class LookupKey {

		private final PortletMode mode;

		private final String parameter;

		public LookupKey(PortletMode portletMode, String parameter) {
			this.mode = portletMode;
			this.parameter = parameter;
		}

		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof LookupKey)) {
				return false;
			}
			LookupKey otherKey = (LookupKey) other;
			return (this.mode.equals(otherKey.mode) &&
					ObjectUtils.nullSafeEquals(this.parameter, otherKey.parameter));
		}

		public int hashCode() {
			return (this.mode.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.parameter));
		}

		public String toString() {
			return "Portlet mode '" + this.mode + "', parameter '" + this.parameter + "'";
		}
	}

}
