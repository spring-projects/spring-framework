/*
 * Copyright 2002-2009 the original author or authors.
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
 * {@link FactoryBean} that exposes the ServletContext for bean references.
 * Can be used as alternative to implementing the ServletContextAware
 * callback interface. Allows for passing the ServletContext reference
 * to a constructor argument or any custom bean property.
 *
 * <p>Note that there's a special FactoryBean for exposing a specific
 * ServletContext attribute, named ServletContextAttributeFactoryBean.
 * So if all you need from the ServletContext is access to a specific
 * attribute, ServletContextAttributeFactoryBean allows you to expose
 * a constructor argument or bean property of the attribute type,
 * which is a preferable to a dependency on the full ServletContext.
 *
 * @author Juergen Hoeller
 * @since 1.1.4
 * @see javax.servlet.ServletContext
 * @see org.springframework.web.context.ServletContextAware
 * @see ServletContextAttributeFactoryBean
 * @see org.springframework.web.context.WebApplicationContext#SERVLET_CONTEXT_BEAN_NAME
 * @deprecated as of Spring 3.0, since "servletContext" is now available
 * as a default bean in every WebApplicationContext
 */
@Deprecated
public class ServletContextFactoryBean implements FactoryBean<ServletContext>, ServletContextAware {

	private ServletContext servletContext;


	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}


	@Override
	public ServletContext getObject() {
		return this.servletContext;
	}

	@Override
	public Class<? extends ServletContext> getObjectType() {
		return (this.servletContext != null ? this.servletContext.getClass() : ServletContext.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
