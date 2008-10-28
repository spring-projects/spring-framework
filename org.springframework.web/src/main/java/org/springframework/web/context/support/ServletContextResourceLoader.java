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

package org.springframework.web.context.support;

import javax.servlet.ServletContext;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * ResourceLoader implementation that resolves paths as ServletContext
 * resources, for use outside a WebApplicationContext (for example,
 * in a HttpServletBean or GenericFilterBean subclass).
 *
 * <p>Within a WebApplicationContext, resource paths are automatically
 * resolved as ServletContext resources by the context implementation.
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 * @see #getResourceByPath
 * @see ServletContextResource
 * @see org.springframework.web.context.WebApplicationContext
 * @see org.springframework.web.servlet.HttpServletBean
 * @see org.springframework.web.filter.GenericFilterBean
 */
public class ServletContextResourceLoader extends DefaultResourceLoader {

	private final ServletContext servletContext;


	/**
	 * Create a new ServletContextResourceLoader.
	 * @param servletContext the ServletContext to load resources with
	 */
	public ServletContextResourceLoader(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * This implementation supports file paths beneath the root of the web application.
	 * @see ServletContextResource
	 */
	@Override
	protected Resource getResourceByPath(String path) {
		return new ServletContextResource(this.servletContext, path);
	}

}
