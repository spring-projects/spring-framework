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

package org.springframework.web.portlet.handler;

import java.util.Map;
import javax.portlet.PortletRequest;

import org.springframework.beans.BeansException;
import org.springframework.util.Assert;

/**
 * Implementation of the {@link org.springframework.web.portlet.HandlerMapping}
 * to map from a request parameter to request handler beans.
 *
 * <p>The default name of the parameter is "action", but can be changed using
 * {@link #setParameterName setParameterName()}.
 *
 * <p>The bean configuration for this mapping will look somthing like this:
 *
 * <pre class="code">
 * &lt;bean id="parameterHandlerMapping" class="org.springframework.web.portlet.handler.ParameterHandlerMapping"&gt;
 *   &lt;property name="parameterMap"&gt;
 *     &lt;map&gt;
 * 	     &lt;entry key="add"&gt;&lt;ref bean="addItemHandler"/&gt;&lt;/entry&gt;
 *       &lt;entry key="edit"&gt;&lt;ref bean="editItemHandler"/&gt;&lt;/entry&gt;
 *       &lt;entry key="delete"&gt;&lt;ref bean="deleteItemHandler"/&gt;&lt;/entry&gt;
 *     &lt;/map&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * Thanks to Rainer Schmitz for suggesting this mapping strategy!
 *
 * @author John A. Lewis
 * @author Juergen Hoeller
 * @since 2.0
 * @see ParameterMappingInterceptor
 */
public class ParameterHandlerMapping extends AbstractMapBasedHandlerMapping<String> {

	/**
	 * Default request parameter name to use for mapping to handlers: "action".
	 */
	public final static String DEFAULT_PARAMETER_NAME = "action";


	private String parameterName = DEFAULT_PARAMETER_NAME;

	private Map<String, ?> parameterMap;


	/**
	 * Set the name of the parameter used for mapping to handlers.
	 * <p>Default is "action".
	 */
	public void setParameterName(String parameterName) {
		Assert.hasText(parameterName, "'parameterName' must not be empty");
		this.parameterName = parameterName;
	}

	/**
	 * Set a Map with parameters as keys and handler beans or bean names as values.
	 * Convenient for population with bean references.
	 * @param parameterMap map with parameters as keys and beans as values
	 */
	public void setParameterMap(Map<String, ?> parameterMap) {
		this.parameterMap = parameterMap;
	}


	/**
	 * Calls the <code>registerHandlers</code> method in addition
	 * to the superclass's initialization.
	 * @see #registerHandlers
	 */
	@Override
	public void initApplicationContext() throws BeansException {
		super.initApplicationContext();
		registerHandlers(this.parameterMap);
	}

	/**
	 * Uses the value of the specified parameter as lookup key.
	 * @see #setParameterName
	 */
	@Override
	protected String getLookupKey(PortletRequest request) throws Exception {
		return request.getParameter(this.parameterName);
	}

}
