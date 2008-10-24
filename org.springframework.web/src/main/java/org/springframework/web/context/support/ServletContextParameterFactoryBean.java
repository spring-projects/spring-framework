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
 * FactoryBean that retrieves a specific ServletContext init parameter
 * (that is, a "context-param" defined in <code>web.xml</code>).
 * Exposes that ServletContext init parameter when used as bean reference,
 * effectively making it available as named Spring bean instance.
 *
 * @author Juergen Hoeller
 * @since 1.2.4
 * @see ServletContextAttributeFactoryBean
 */
public class ServletContextParameterFactoryBean implements FactoryBean, ServletContextAware {

	private String initParamName;

	private String paramValue;


	/**
	 * Set the name of the ServletContext init parameter to expose.
	 */
	public void setInitParamName(String initParamName) {
		this.initParamName = initParamName;
	}

	public void setServletContext(ServletContext servletContext) {
		if (this.initParamName == null) {
			throw new IllegalArgumentException("initParamName is required");
		}
		this.paramValue = servletContext.getInitParameter(this.initParamName);
		if (this.paramValue == null) {
			throw new IllegalStateException("No ServletContext init parameter '" + this.initParamName + "' found");
		}
	}


	public Object getObject() throws Exception {
		return this.paramValue;
	}

	public Class getObjectType() {
		return String.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
