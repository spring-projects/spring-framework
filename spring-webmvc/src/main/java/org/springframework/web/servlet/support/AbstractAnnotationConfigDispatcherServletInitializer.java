/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.support;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * Base class for {@link org.springframework.web.WebApplicationInitializer
 * WebApplicationInitializer} implementations that register one or more {@link
 * org.springframework.web.servlet.DispatcherServlet DispatcherServlet}s in the servlet
 * context based on annotated classes, e.g. {@link org.springframework.context.annotation.Configuration
 * &#064Configuration} classes.
 *
 * <p>Concrete implementations are required to implement {@link #getRootConfigClasses()},
 * {@link #getServletConfigClasses()}, as well as {@link #getServletMappings()}. Further
 * template and customization methods are provided by {@link AbstractDispatcherServletInitializer}.
 *
 * @author Arjen Poutsma
 * @since 3.2
 */
public abstract class AbstractAnnotationConfigDispatcherServletInitializer
		extends AbstractDispatcherServletInitializer {

	@Override
	protected WebApplicationContext createRootApplicationContext() {
		Class<?>[] rootConfigClasses = getRootConfigClasses();
		if (!ObjectUtils.isEmpty(rootConfigClasses)) {
			AnnotationConfigWebApplicationContext rootAppContext =
					new AnnotationConfigWebApplicationContext();
			rootAppContext.register(rootConfigClasses);
			return rootAppContext;
		}
		else {
			return null;
		}
	}

	@Override
	protected WebApplicationContext createServletApplicationContext() {
		AnnotationConfigWebApplicationContext servletAppContext =
				new AnnotationConfigWebApplicationContext();
		Class<?>[] servletConfigClasses = getServletConfigClasses();
		Assert.notEmpty(servletConfigClasses,
				"getServletConfigClasses did not return" + "any configuration classes");

		servletAppContext.register(servletConfigClasses);
		return servletAppContext;
	}

	/**
	 * Abstract template method that returns {@link org.springframework.context.annotation.Configuration
	 * &#064Configuration} classes for the root application context.
	 *
	 * @return the configuration classes for the root application context, or {@code null} if
	 *         a root context is not desired
	 */
	protected abstract Class<?>[] getRootConfigClasses();

	/**
	 * Abstract template method that returns {@link org.springframework.context.annotation.Configuration
	 * &#064Configuration} classes for the servlet application context.
	 *
	 * @return the configuration classes for the servlet application context
	 */
	protected abstract Class<?>[] getServletConfigClasses();

}
