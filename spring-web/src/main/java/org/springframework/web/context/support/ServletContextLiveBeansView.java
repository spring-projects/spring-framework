/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.context.support;

import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.ServletContext;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.context.support.LiveBeansView} subclass
 * which looks for all ApplicationContexts in the web application,
 * as exposed in ServletContext attributes.
 *
 * @author Juergen Hoeller
 * @since 3.2
 * @deprecated as of 5.3, in favor of using Spring Boot actuators for such needs
 */
@Deprecated
public class ServletContextLiveBeansView extends org.springframework.context.support.LiveBeansView {

	private final ServletContext servletContext;

	/**
	 * Create a new LiveBeansView for the given ServletContext.
	 * @param servletContext current ServletContext
	 */
	public ServletContextLiveBeansView(ServletContext servletContext) {
		Assert.notNull(servletContext, "ServletContext must not be null");
		this.servletContext = servletContext;
	}

	@Override
	protected Set<ConfigurableApplicationContext> findApplicationContexts() {
		Set<ConfigurableApplicationContext> contexts = new LinkedHashSet<>();
		Enumeration<String> attrNames = this.servletContext.getAttributeNames();
		while (attrNames.hasMoreElements()) {
			String attrName = attrNames.nextElement();
			Object attrValue = this.servletContext.getAttribute(attrName);
			if (attrValue instanceof ConfigurableApplicationContext) {
				contexts.add((ConfigurableApplicationContext) attrValue);
			}
		}
		return contexts;
	}

}
