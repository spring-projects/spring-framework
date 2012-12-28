/*
 * Copyright 2002-2012 the original author or authors.
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
 * {@link FactoryBean} that retrieves a specific ServletContext init parameter
 * (that is, a "context-param" defined in {@code web.xml}).
 * Exposes that ServletContext init parameter when used as bean reference,
 * effectively making it available as named Spring bean instance.
 *
 * <p><b>NOTE:</b> As of Spring 3.0, you may also use the "contextParameters" default
 * bean which is of type Map, and dereference it using an "#{contextParameters.myKey}"
 * expression to access a specific parameter by name.
 *
 * @author Juergen Hoeller
 * @since 1.2.4
 * @see org.springframework.web.context.WebApplicationContext#CONTEXT_PARAMETERS_BEAN_NAME
 * @see ServletContextAttributeFactoryBean
 */
public class ServletContextParameterFactoryBean implements FactoryBean<String>, ServletContextAware {

	private String initParamName;

	private String paramValue;


	/**
	 * Set the name of the ServletContext init parameter to expose.
	 */
	public void setInitParamName(String initParamName) {
		this.initParamName = initParamName;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		if (this.initParamName == null) {
			throw new IllegalArgumentException("initParamName is required");
		}
		this.paramValue = servletContext.getInitParameter(this.initParamName);
		if (this.paramValue == null) {
			throw new IllegalStateException("No ServletContext init parameter '" + this.initParamName + "' found");
		}
	}


	@Override
	public String getObject() {
		return this.paramValue;
	}

	@Override
	public Class<String> getObjectType() {
		return String.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
