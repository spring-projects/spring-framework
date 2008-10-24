/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.web.context;

import javax.servlet.ServletConfig;

/**
 * Interface to be implemented by any object that wishes to be notified
 * of the ServletConfig (typically determined by the WebApplicationContext)
 * that it runs in.
 *
 * <p>Only satisfied if actually running within a Servlet-specific
 * WebApplicationContext. If this callback interface is encountered
 * elsewhere, an exception will be thrown on bean creation.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see ServletContextAware
 */
public interface ServletConfigAware {

	/**
	 * Set the ServletConfig that this object runs in.
	 * <p>Invoked after population of normal bean properties but before an init
	 * callback like InitializingBean's <code>afterPropertiesSet</code> or a
	 * custom init-method. Invoked after ApplicationContextAware's
	 * <code>setApplicationContext</code>.
	 * @param servletConfig ServletConfig object to be used by this object
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext
	 */
	void setServletConfig(ServletConfig servletConfig);

}
