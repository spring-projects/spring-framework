/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test.web.servlet.setup;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * A concrete implementation of {@link AbstractMockMvcBuilder} that provides
 * the {@link WebApplicationContext} supplied to it as a constructor argument.
 *
 * <p>In addition, if the {@link ServletContext} in the supplied
 * {@code WebApplicationContext} does not contain an entry for the
 * {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}
 * key, the root {@code WebApplicationContext} will be detected and stored
 * in the {@code ServletContext} under the
 * {@code ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE} key.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Sam Brannen
 * @since 3.2
 */
public class DefaultMockMvcBuilder extends AbstractMockMvcBuilder<DefaultMockMvcBuilder> {

	private final WebApplicationContext webAppContext;


	/**
	 * Protected constructor. Not intended for direct instantiation.
	 * @see MockMvcBuilders#webAppContextSetup(WebApplicationContext)
	 */
	protected DefaultMockMvcBuilder(WebApplicationContext webAppContext) {
		Assert.notNull(webAppContext, "WebApplicationContext is required");
		Assert.notNull(webAppContext.getServletContext(), "WebApplicationContext must have a ServletContext");
		this.webAppContext = webAppContext;
	}

	@Override
	protected WebApplicationContext initWebAppContext() {

		ServletContext servletContext = this.webAppContext.getServletContext();
		ApplicationContext rootWac = WebApplicationContextUtils.getWebApplicationContext(servletContext);

		if (rootWac == null) {
			rootWac = this.webAppContext;
			ApplicationContext parent = this.webAppContext.getParent();
			while (parent != null) {
				if (parent instanceof WebApplicationContext && !(parent.getParent() instanceof WebApplicationContext)) {
					rootWac = parent;
					break;
				}
				parent = parent.getParent();
			}
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, rootWac);
		}

		return this.webAppContext;
	}

}
