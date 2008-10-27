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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.portlet.PortletMode;
import javax.portlet.PortletRequest;

import org.springframework.beans.BeansException;
import org.springframework.util.CollectionUtils;

/**
 * Implementation of the {@link org.springframework.web.portlet.HandlerMapping}
 * interface to map from the current PortletMode to request handler beans.
 *
 * <p>The bean configuration for this mapping will look something like this:
 * <pre>
 * 	&lt;bean id="portletModeHandlerMapping" class="org.springframework.web.portlet.handler.PortletModeHandlerMapping"&gt;
 * 		&lt;property name="portletModeMap"&gt;
 * 			&lt;map&gt;
 * 				&lt;entry key="view"&gt;&lt;ref bean="viewHandler"/&gt;&lt;/entry&gt;
 * 				&lt;entry key="edit"&gt;&lt;ref bean="editHandler"/&gt;&lt;/entry&gt;
 * 				&lt;entry key="help"&gt;&lt;ref bean="helpHandler"/&gt;&lt;/entry&gt;
 * 			&lt;/map&gt;
 * 		&lt;/property&gt;
 * 	&lt;/bean&gt;
 * </pre>
 *
 * @author William G. Thompson, Jr.
 * @author John A. Lewis
 * @since 2.0
 */
public class PortletModeHandlerMapping extends AbstractMapBasedHandlerMapping {

	private final Map portletModeMap = new HashMap();


	/**
	 * Set PortletMode to handler bean name mappings from a Properties object.
	 * @param mappings properties with PortletMode names as keys and bean names as values
	 */
	public void setMappings(Properties mappings) {
		this.portletModeMap.putAll(mappings);
	}

	/**
	 * Set a Map with PortletModes as keys and handler beans as values.
	 * Convenient for population with bean references.
	 * @param portletModeMap map with PortletMode names as keys and beans or bean names as values
	 */
	public void setPortletModeMap(Map portletModeMap) {
		this.portletModeMap.putAll(portletModeMap);
	}


	/**
	 * Calls the <code>registerHandlers</code> method in addition
	 * to the superclass's initialization.
	 * @see #registerHandlers
	 */
	public void initApplicationContext() throws BeansException {
		super.initApplicationContext();
		registerHandlers(this.portletModeMap);
	}

	/**
	 * Register all handlers specified in the Portlet mode map for the corresponding modes.
	 * @param portletModeMap Map with mode names as keys and handler beans or bean names as values
	 * @throws BeansException if the handler couldn't be registered
	 */
	protected void registerHandlers(Map portletModeMap) throws BeansException {
		if (CollectionUtils.isEmpty(portletModeMap)) {
			logger.warn("Neither 'portletModeMap' nor 'mappings' set on PortletModeHandlerMapping");
		}
		else {
			for (Iterator it = portletModeMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				String modeKey = (String) entry.getKey();
				PortletMode mode = new PortletMode(modeKey);
				Object handler = entry.getValue();
				registerHandler(mode, handler);
			}
		}
	}


	/**
	 * Uses the current PortletMode as lookup key.
	 */
	protected Object getLookupKey(PortletRequest request) throws Exception {
		return request.getPortletMode();
	}

}
