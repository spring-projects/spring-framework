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

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestScope;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.SessionScope;

/**
 * Convenience methods for retrieving the root
 * {@link org.springframework.web.context.WebApplicationContext} for a given
 * <code>ServletContext</code>. This is e.g. useful for accessing a Spring
 * context from within custom web views or Struts actions.
 *
 * <p>Note that there are more convenient ways of accessing the root context for
 * many web frameworks, either part of Spring or available as external library.
 * This helper class is just the most generic way to access the root context.
 *
 * @author Juergen Hoeller
 * @see org.springframework.web.context.ContextLoader
 * @see org.springframework.web.servlet.FrameworkServlet
 * @see org.springframework.web.servlet.DispatcherServlet
 * @see org.springframework.web.jsf.FacesContextUtils
 * @see org.springframework.web.jsf.DelegatingVariableResolver
 */
public abstract class WebApplicationContextUtils {
	
	/**
	 * Find the root WebApplicationContext for this web application, which is
	 * typically loaded via {@link org.springframework.web.context.ContextLoaderListener}.
	 * <p>Will rethrow an exception that happened on root context startup,
	 * to differentiate between a failed context startup and no context at all.
	 * @param sc ServletContext to find the web application context for
	 * @return the root WebApplicationContext for this web app
	 * @throws IllegalStateException if the root WebApplicationContext could not be found
	 * @see org.springframework.web.context.WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
	 */
	public static WebApplicationContext getRequiredWebApplicationContext(ServletContext sc)
	    throws IllegalStateException {

		WebApplicationContext wac = getWebApplicationContext(sc);
		if (wac == null) {
			throw new IllegalStateException("No WebApplicationContext found: no ContextLoaderListener registered?");
		}
		return wac;
	}

	/**
	 * Find the root WebApplicationContext for this web application, which is
	 * typically loaded via {@link org.springframework.web.context.ContextLoaderListener}.
	 * <p>Will rethrow an exception that happened on root context startup,
	 * to differentiate between a failed context startup and no context at all.
	 * @param sc ServletContext to find the web application context for
	 * @return the root WebApplicationContext for this web app, or <code>null</code> if none
	 * @see org.springframework.web.context.WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
	 */
	public static WebApplicationContext getWebApplicationContext(ServletContext sc) {
		return getWebApplicationContext(sc, WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
	}

	/**
	 * Find a custom WebApplicationContext for this web application.
	 * @param sc ServletContext to find the web application context for
	 * @param attrName the name of the ServletContext attribute to look for
	 * @return the desired WebApplicationContext for this web app, or <code>null</code> if none
	 */
	public static WebApplicationContext getWebApplicationContext(ServletContext sc, String attrName) {
		Assert.notNull(sc, "ServletContext must not be null");
		Object attr = sc.getAttribute(attrName);
		if (attr == null) {
			return null;
		}
		if (attr instanceof RuntimeException) {
			throw (RuntimeException) attr;
		}
		if (attr instanceof Error) {
			throw (Error) attr;
		}
		if (attr instanceof Exception) {
			throw new IllegalStateException((Exception) attr);
		}
		if (!(attr instanceof WebApplicationContext)) {
			throw new IllegalStateException("Context attribute is not of type WebApplicationContext: " + attr);
		}
		return (WebApplicationContext) attr;
	}


	/**
	 * Register web-specific scopes with the given BeanFactory,
	 * as used by the WebApplicationContext.
	 * @param beanFactory the BeanFactory to configure
	 */
	static void registerWebApplicationScopes(ConfigurableListableBeanFactory beanFactory) {
		beanFactory.registerScope(WebApplicationContext.SCOPE_REQUEST, new RequestScope());
		beanFactory.registerScope(WebApplicationContext.SCOPE_SESSION, new SessionScope(false));
		beanFactory.registerScope(WebApplicationContext.SCOPE_GLOBAL_SESSION, new SessionScope(true));

		beanFactory.registerResolvableDependency(ServletRequest.class, new ObjectFactory<ServletRequest>() {
			public ServletRequest getObject() {
				RequestAttributes requestAttr = RequestContextHolder.currentRequestAttributes();
				if (!(requestAttr instanceof ServletRequestAttributes)) {
					throw new IllegalStateException("Current request is not a servlet request");
				}
				return ((ServletRequestAttributes) requestAttr).getRequest();
			}
		});
		beanFactory.registerResolvableDependency(HttpSession.class, new ObjectFactory<HttpSession>() {
			public HttpSession getObject() {
				RequestAttributes requestAttr = RequestContextHolder.currentRequestAttributes();
				if (!(requestAttr instanceof ServletRequestAttributes)) {
					throw new IllegalStateException("Current request is not a servlet request");
				}
				return ((ServletRequestAttributes) requestAttr).getRequest().getSession();
			}
		});
	}

	/**
	 * Register web-specific environment beans with the given BeanFactory,
	 * as used by the WebApplicationContext.
	 * @param bf the BeanFactory to configure
	 * @param sc the ServletContext that we're running within
	 */
	static void registerEnvironmentBeans(ConfigurableListableBeanFactory bf, ServletContext sc) {
		if (!bf.containsBean(WebApplicationContext.CONTEXT_PROPERTIES_BEAN_NAME)) {
			Map<String, String> parameterMap = new HashMap<String, String>();
			if (sc != null) {
				Enumeration paramNameEnum = sc.getInitParameterNames();
				while (paramNameEnum.hasMoreElements()) {
					String paramName = (String) paramNameEnum.nextElement();
					parameterMap.put(paramName, sc.getInitParameter(paramName));
				}
			}
			bf.registerSingleton(WebApplicationContext.CONTEXT_PROPERTIES_BEAN_NAME,
					Collections.unmodifiableMap(parameterMap));
		}

		if (!bf.containsBean(WebApplicationContext.CONTEXT_ATTRIBUTES_BEAN_NAME)) {
			Map<String, Object> attributeMap = new HashMap<String, Object>();
			if (sc != null) {
				Enumeration attrNameEnum = sc.getAttributeNames();
				while (attrNameEnum.hasMoreElements()) {
					String attrName = (String) attrNameEnum.nextElement();
					attributeMap.put(attrName, sc.getAttribute(attrName));
				}
			}
			bf.registerSingleton(WebApplicationContext.CONTEXT_ATTRIBUTES_BEAN_NAME,
					Collections.unmodifiableMap(attributeMap));
		}
	}

}
