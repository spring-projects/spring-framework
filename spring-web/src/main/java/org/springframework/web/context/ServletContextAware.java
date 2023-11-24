/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.context;

import jakarta.servlet.ServletContext;

import org.springframework.beans.factory.Aware;

/**
 * Interface to be implemented by any object that wishes to be notified of the
 * {@link ServletContext} (typically determined by the {@link WebApplicationContext})
 * that it runs in.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 12.03.2004
 * @see ServletConfigAware
 */
public interface ServletContextAware extends Aware {

	/**
	 * Set the {@link ServletContext} that this object runs in.
	 * <p>Invoked after population of normal bean properties but before an init
	 * callback like InitializingBean's {@code afterPropertiesSet} or a
	 * custom init-method. Invoked after ApplicationContextAware's
	 * {@code setApplicationContext}.
	 * @param servletContext the ServletContext object to be used by this object
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext
	 */
	void setServletContext(ServletContext servletContext);

}
