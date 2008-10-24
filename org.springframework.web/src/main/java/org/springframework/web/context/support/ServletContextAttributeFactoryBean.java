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

import org.springframework.beans.factory.FactoryBean;
import org.springframework.web.context.ServletContextAware;

/**
 * FactoryBean that fetches a specific, existing ServletContext attribute.
 * Exposes that ServletContext attribute when used as bean reference,
 * effectively making it available as named Spring bean instance.
 *
 * <p>Intended to link in ServletContext attributes that exist before
 * the startup of the Spring application context. Typically, such
 * attributes will have been put there by third-party web frameworks.
 * In a purely Spring-based web application, no such linking in of
 * ServletContext attrutes will be necessary.
 *
 * @author Juergen Hoeller
 * @since 1.1.4
 * @see ServletContextParameterFactoryBean
 */
public class ServletContextAttributeFactoryBean implements FactoryBean, ServletContextAware {

	private String attributeName;

	private Object attribute;


	/**
	 * Set the name of the ServletContext attribute to expose.
	 */
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public void setServletContext(ServletContext servletContext) {
		if (this.attributeName == null) {
			throw new IllegalArgumentException("attributeName is required");
		}
		this.attribute = servletContext.getAttribute(this.attributeName);
		if (this.attribute == null) {
			throw new IllegalStateException("No ServletContext attribute '" + this.attributeName + "' found");
		}
	}


	public Object getObject() throws Exception {
		return this.attribute;
	}

	public Class getObjectType() {
		return (this.attribute != null ? this.attribute.getClass() : null);
	}

	public boolean isSingleton() {
		return true;
	}

}
