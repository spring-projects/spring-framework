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

package org.springframework.web.portlet.context;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestScope;
import org.springframework.web.context.request.SessionScope;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Convenience methods for retrieving the root WebApplicationContext for a given
 * PortletContext. This is e.g. useful for accessing a Spring context from
 * within custom Portlet implementations.
 *
 * @author Juergen Hoeller
 * @author John A. Lewis
 * @since 2.0
 * @see org.springframework.web.context.ContextLoader
 * @see org.springframework.web.context.support.WebApplicationContextUtils
 * @see org.springframework.web.portlet.FrameworkPortlet
 * @see org.springframework.web.portlet.DispatcherPortlet
 */
public abstract class PortletApplicationContextUtils {
	
	/**
	 * Find the root WebApplicationContext for this portlet application, which is
	 * typically loaded via ContextLoaderListener or ContextLoaderServlet.
	 * <p>Will rethrow an exception that happened on root context startup,
	 * to differentiate between a failed context startup and no context at all.
	 * @param pc PortletContext to find the web application context for
	 * @return the root WebApplicationContext for this web app, or <code>null</code> if none
	 * (typed to ApplicationContext to avoid a Servlet API dependency; can usually
	 * be casted to WebApplicationContext, but there shouldn't be a need to)
	 * @see org.springframework.web.context.WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
	 */
	public static ApplicationContext getWebApplicationContext(PortletContext pc) {
		Assert.notNull(pc, "PortletContext must not be null");
		Object attr = pc.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (attr == null) {
			return null;
		}
		if (attr instanceof RuntimeException) {
			throw (RuntimeException) attr;
		}
		if (attr instanceof Error) {
			throw (Error) attr;
		}
		if (!(attr instanceof ApplicationContext)) {
			throw new IllegalStateException("Root context attribute is not of type WebApplicationContext: " + attr);
		}
		return (ApplicationContext) attr;
	}

	/**
	 * Find the root WebApplicationContext for this portlet application, which is
	 * typically loaded via ContextLoaderListener or ContextLoaderServlet.
	 * <p>Will rethrow an exception that happened on root context startup,
	 * to differentiate between a failed context startup and no context at all.
	 * @param pc PortletContext to find the web application context for
	 * @return the root WebApplicationContext for this web app
	 * (typed to ApplicationContext to avoid a Servlet API dependency; can usually
	 * be casted to WebApplicationContext, but there shouldn't be a need to)
	 * @throws IllegalStateException if the root WebApplicationContext could not be found
	 * @see org.springframework.web.context.WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
	 */
	public static ApplicationContext getRequiredWebApplicationContext(PortletContext pc)
	    throws IllegalStateException {

		ApplicationContext wac = getWebApplicationContext(pc);
		if (wac == null) {
			throw new IllegalStateException("No WebApplicationContext found: no ContextLoaderListener registered?");
		}
		return wac;
	}


	/**
	 * Register web-specific scopes ("request", "session", "globalSession")
	 * with the given BeanFactory, as used by the Portlet ApplicationContext.
	 * @param beanFactory the BeanFactory to configure
	 * @param pc the PortletContext that we're running within
	 */
	static void registerPortletApplicationScopes(ConfigurableListableBeanFactory beanFactory, PortletContext pc) {
		beanFactory.registerScope(WebApplicationContext.SCOPE_REQUEST, new RequestScope());
		beanFactory.registerScope(WebApplicationContext.SCOPE_SESSION, new SessionScope(false));
		beanFactory.registerScope(WebApplicationContext.SCOPE_GLOBAL_SESSION, new SessionScope(true));
		if (pc != null) {
			PortletContextScope appScope = new PortletContextScope(pc);
			beanFactory.registerScope(WebApplicationContext.SCOPE_APPLICATION, appScope);
			// Register as PortletContext attribute, for ContextCleanupListener to detect it.
			pc.setAttribute(PortletContextScope.class.getName(), appScope);
		}

		beanFactory.registerResolvableDependency(PortletRequest.class, new RequestObjectFactory());
		beanFactory.registerResolvableDependency(PortletSession.class, new SessionObjectFactory());
		beanFactory.registerResolvableDependency(WebRequest.class, new WebRequestObjectFactory());
	}

	/**
	 * Register web-specific environment beans ("contextParameters", "contextAttributes")
	 * with the given BeanFactory, as used by the Portlet ApplicationContext.
	 * @param bf the BeanFactory to configure
	 * @param sc the ServletContext that we're running within
	 * @param pc the PortletContext that we're running within
	 * @param config the PortletConfig of the containing Portlet
	 */
	static void registerEnvironmentBeans(
			ConfigurableListableBeanFactory bf, ServletContext sc, PortletContext pc, PortletConfig config) {

		if (sc != null && !bf.containsBean(WebApplicationContext.SERVLET_CONTEXT_BEAN_NAME)) {
			bf.registerSingleton(WebApplicationContext.SERVLET_CONTEXT_BEAN_NAME, sc);
		}

		if (pc != null && !bf.containsBean(ConfigurablePortletApplicationContext.PORTLET_CONTEXT_BEAN_NAME)) {
			bf.registerSingleton(ConfigurablePortletApplicationContext.PORTLET_CONTEXT_BEAN_NAME, pc);
		}

		if (config != null && !bf.containsBean(ConfigurablePortletApplicationContext.PORTLET_CONFIG_BEAN_NAME)) {
			bf.registerSingleton(ConfigurablePortletApplicationContext.PORTLET_CONFIG_BEAN_NAME, config);
		}

		if (!bf.containsBean(WebApplicationContext.CONTEXT_PARAMETERS_BEAN_NAME)) {
			Map<String, String> parameterMap = new HashMap<String, String>();
			if (pc != null) {
				Enumeration paramNameEnum = pc.getInitParameterNames();
				while (paramNameEnum.hasMoreElements()) {
					String paramName = (String) paramNameEnum.nextElement();
					parameterMap.put(paramName, pc.getInitParameter(paramName));
				}
			}
			if (config != null) {
				Enumeration paramNameEnum = config.getInitParameterNames();
				while (paramNameEnum.hasMoreElements()) {
					String paramName = (String) paramNameEnum.nextElement();
					parameterMap.put(paramName, config.getInitParameter(paramName));
				}
			}
			bf.registerSingleton(WebApplicationContext.CONTEXT_PARAMETERS_BEAN_NAME,
					Collections.unmodifiableMap(parameterMap));
		}

		if (!bf.containsBean(WebApplicationContext.CONTEXT_ATTRIBUTES_BEAN_NAME)) {
			Map<String, Object> attributeMap = new HashMap<String, Object>();
			if (pc != null) {
				Enumeration attrNameEnum = pc.getAttributeNames();
				while (attrNameEnum.hasMoreElements()) {
					String attrName = (String) attrNameEnum.nextElement();
					attributeMap.put(attrName, pc.getAttribute(attrName));
				}
			}
			bf.registerSingleton(WebApplicationContext.CONTEXT_ATTRIBUTES_BEAN_NAME,
					Collections.unmodifiableMap(attributeMap));
		}
	}

	/**
	 * Replace {@code Servlet}- and {@code Portlet}-based {@link
	 * org.springframework.core.env.PropertySource.StubPropertySource stub property
	 * sources} with actual instances populated with the given {@code servletContext},
	 * {@code portletContext} and {@code portletConfig} objects.
	 * <p>This method is idempotent with respect to the fact it may be called any number
	 * of times but will perform replacement of stub property sources with their
	 * corresponding actual property sources once and only once.
	 * @param propertySources the {@link MutablePropertySources} to initialize (must not be {@code null})
	 * @param servletContext the current {@link ServletContext} (ignored if {@code null}
	 * or if the {@link org.springframework.web.context.support.StandardServletEnvironment#SERVLET_CONTEXT_PROPERTY_SOURCE_NAME
	 * servlet context property source} has already been initialized)
	 * @param portletContext the current {@link PortletContext} (ignored if {@code null}
	 * or if the {@link StandardPortletEnvironment#PORTLET_CONTEXT_PROPERTY_SOURCE_NAME
	 * portlet context property source} has already been initialized)
	 * @param portletConfig the current {@link PortletConfig} (ignored if {@code null}
	 * or if the {@link StandardPortletEnvironment#PORTLET_CONFIG_PROPERTY_SOURCE_NAME
	 * portlet config property source} has already been initialized)
	 * @see org.springframework.core.env.PropertySource.StubPropertySource
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#initServletPropertySources(MutablePropertySources, ServletContext)
	 * @see org.springframework.core.env.ConfigurableEnvironment#getPropertySources()
	 */
	public static void initPortletPropertySources(MutablePropertySources propertySources, ServletContext servletContext,
			PortletContext portletContext, PortletConfig portletConfig) {
		Assert.notNull(propertySources, "propertySources must not be null");

		WebApplicationContextUtils.initServletPropertySources(propertySources, servletContext);

		if(portletContext != null && propertySources.contains(StandardPortletEnvironment.PORTLET_CONTEXT_PROPERTY_SOURCE_NAME)) {
			propertySources.replace(StandardPortletEnvironment.PORTLET_CONTEXT_PROPERTY_SOURCE_NAME,
					new PortletContextPropertySource(StandardPortletEnvironment.PORTLET_CONTEXT_PROPERTY_SOURCE_NAME, portletContext));
		}
		if(portletConfig != null && propertySources.contains(StandardPortletEnvironment.PORTLET_CONFIG_PROPERTY_SOURCE_NAME)) {
			propertySources.replace(StandardPortletEnvironment.PORTLET_CONFIG_PROPERTY_SOURCE_NAME,
					new PortletConfigPropertySource(StandardPortletEnvironment.PORTLET_CONFIG_PROPERTY_SOURCE_NAME, portletConfig));
		}
	}

	/**
	 * Return the current RequestAttributes instance as PortletRequestAttributes.
	 * @see RequestContextHolder#currentRequestAttributes()
	 */
	private static PortletRequestAttributes currentRequestAttributes() {
		RequestAttributes requestAttr = RequestContextHolder.currentRequestAttributes();
		if (!(requestAttr instanceof PortletRequestAttributes)) {
			throw new IllegalStateException("Current request is not a portlet request");
		}
		return (PortletRequestAttributes) requestAttr;
	}


	/**
	 * Factory that exposes the current request object on demand.
	 */
	private static class RequestObjectFactory implements ObjectFactory<PortletRequest>, Serializable {

		public PortletRequest getObject() {
			return currentRequestAttributes().getRequest();
		}

		@Override
		public String toString() {
			return "Current PortletRequest";
		}
	}


	/**
	 * Factory that exposes the current session object on demand.
	 */
	private static class SessionObjectFactory implements ObjectFactory<PortletSession>, Serializable {

		public PortletSession getObject() {
			return currentRequestAttributes().getRequest().getPortletSession();
		}

		@Override
		public String toString() {
			return "Current PortletSession";
		}
	}


	/**
	 * Factory that exposes the current WebRequest object on demand.
	 */
	private static class WebRequestObjectFactory implements ObjectFactory<WebRequest>, Serializable {

		public WebRequest getObject() {
			return new PortletWebRequest(currentRequestAttributes().getRequest());
		}

		@Override
		public String toString() {
			return "Current PortletWebRequest";
		}
	}

}
