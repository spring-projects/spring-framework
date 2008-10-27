/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.web.portlet.context;

import javax.portlet.PortletConfig;

/**
 * Interface to be implemented by any object that wishes to be notified
 * of the PortletConfig (typically determined by the PortletApplicationContext)
 * that it runs in.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see PortletContextAware
 */
public interface PortletConfigAware {

	/**
	 * Set the PortletConfigthat this object runs in.
	 * <p>Invoked after population of normal bean properties but before an init
	 * callback like InitializingBean's afterPropertiesSet or a custom init-method.
	 * Invoked after ApplicationContextAware's setApplicationContext.
	 * @param portletConfig PortletConfig object to be used by this object
	 */
	void setPortletConfig(PortletConfig portletConfig);

}
