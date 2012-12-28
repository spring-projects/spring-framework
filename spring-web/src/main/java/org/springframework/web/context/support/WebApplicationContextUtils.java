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

import static org.springframework.web.context.support.StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME;
import static org.springframework.web.context.support.StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource.StubPropertySource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestScope;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.SessionScope;
import org.springframework.web.context.request.WebRequest;

/**
 * Convenience methods for retrieving the root
 * {@link org.springframework.web.context.WebApplicationContext} for a given
 * {@code ServletContext}. This is e.g. useful for accessing a Spring
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
 * @see org.springframework.web.jsf.SpringBeanVariableResolver
 * @see org.springframework.web.jsf.el.SpringBeanFacesELResolver
 */
public abstract class WebApplicationContextUtils {

	private static final boolean jsfPresent =
			ClassUtils.isPresent("javax.faces.context.FacesContext", RequestContextHolder.class.getClassLoader());


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
	 * @return the root WebApplicationContext for this web app, or {@code null} if none
	 * @see org.springframework.web.context.WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
	 */
	public static WebApplicationContext getWebApplicationContext(ServletContext sc) {
		return getWebApplicationContext(sc, WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
	}

	/**
	 * Find a custom WebApplicationContext for this web application.
	 * @param sc ServletContext to find the web application context for
	 * @param attrName the name of the ServletContext attribute to look for
	 * @return the desired WebApplicationContext for this web app, or {@code null} if none
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
	 * Register web-specific scopes ("request", "session", "globalSession")
	 * with the given BeanFactory, as used by the WebApplicationContext.
	 * @param beanFactory the BeanFactory to configure
	 */
	public static void registerWebApplicationScopes(ConfigurableListableBeanFactory beanFactory) {
		registerWebApplicationScopes(beanFactory, null);
	}

	/**
	 * Register web-specific scopes ("request", "session", "globalSession", "application")
	 * with the given BeanFactory, as used by the WebApplicationContext.
	 * @param beanFactory the BeanFactory to configure
	 * @param sc the ServletContext that we're running within
	 */
	public static void registerWebApplicationScopes(ConfigurableListableBeanFactory beanFactory, ServletContext sc) {
		beanFactory.registerScope(WebApplicationContext.SCOPE_REQUEST, new RequestScope());
		beanFactory.registerScope(WebApplicationContext.SCOPE_SESSION, new SessionScope(false));
		beanFactory.registerScope(WebApplicationContext.SCOPE_GLOBAL_SESSION, new SessionScope(true));
		if (sc != null) {
			ServletContextScope appScope = new ServletContextScope(sc);
			beanFactory.registerScope(WebApplicationContext.SCOPE_APPLICATION, appScope);
			// Register as ServletContext attribute, for ContextCleanupListener to detect it.
			sc.setAttribute(ServletContextScope.class.getName(), appScope);
		}

		beanFactory.registerResolvableDependency(ServletRequest.class, new RequestObjectFactory());
		beanFactory.registerResolvableDependency(HttpSession.class, new SessionObjectFactory());
		beanFactory.registerResolvableDependency(WebRequest.class, new WebRequestObjectFactory());
		if (jsfPresent) {
			FacesDependencyRegistrar.registerFacesDependencies(beanFactory);
		}
	}

	/**
	 * Register web-specific environment beans ("contextParameters", "contextAttributes")
	 * with the given BeanFactory, as used by the WebApplicationContext.
	 * @param bf the BeanFactory to configure
	 * @param sc the ServletContext that we're running within
	 */
	public static void registerEnvironmentBeans(ConfigurableListableBeanFactory bf, ServletContext sc) {
		registerEnvironmentBeans(bf, sc, null);
	}

	/**
	 * Register web-specific environment beans ("contextParameters", "contextAttributes")
	 * with the given BeanFactory, as used by the WebApplicationContext.
	 * @param bf the BeanFactory to configure
	 * @param sc the ServletContext that we're running within
	 * @param config the ServletConfig of the containing Portlet
	 */
	public static void registerEnvironmentBeans(
			ConfigurableListableBeanFactory bf, ServletContext sc, ServletConfig config) {

		if (sc != null && !bf.containsBean(WebApplicationContext.SERVLET_CONTEXT_BEAN_NAME)) {
			bf.registerSingleton(WebApplicationContext.SERVLET_CONTEXT_BEAN_NAME, sc);
		}

		if (config != null && !bf.containsBean(ConfigurableWebApplicationContext.SERVLET_CONFIG_BEAN_NAME)) {
			bf.registerSingleton(ConfigurableWebApplicationContext.SERVLET_CONFIG_BEAN_NAME, config);
		}

		if (!bf.containsBean(WebApplicationContext.CONTEXT_PARAMETERS_BEAN_NAME)) {
			Map<String, String> parameterMap = new HashMap<String, String>();
			if (sc != null) {
				Enumeration<?> paramNameEnum = sc.getInitParameterNames();
				while (paramNameEnum.hasMoreElements()) {
					String paramName = (String) paramNameEnum.nextElement();
					parameterMap.put(paramName, sc.getInitParameter(paramName));
				}
			}
			if (config != null) {
				Enumeration<?> paramNameEnum = config.getInitParameterNames();
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
			if (sc != null) {
				Enumeration<?> attrNameEnum = sc.getAttributeNames();
				while (attrNameEnum.hasMoreElements()) {
					String attrName = (String) attrNameEnum.nextElement();
					attributeMap.put(attrName, sc.getAttribute(attrName));
				}
			}
			bf.registerSingleton(WebApplicationContext.CONTEXT_ATTRIBUTES_BEAN_NAME,
					Collections.unmodifiableMap(attributeMap));
		}
	}

	/**
	 * Convenient variant of {@link #initServletPropertySources(MutablePropertySources,
	 * ServletContext, ServletConfig)} that always provides {@code null} for the
	 * {@link ServletConfig} parameter.
	 * @see #initServletPropertySources(MutablePropertySources, ServletContext, ServletConfig)
	 */
	public static void initServletPropertySources(
			MutablePropertySources propertySources, ServletContext servletContext) {
		initServletPropertySources(propertySources, servletContext, null);
	}

	/**
	 * Replace {@code Servlet}-based {@link StubPropertySource stub property sources} with
	 * actual instances populated with the given {@code servletContext} and
	 * {@code servletConfig} objects.
	 * <p>This method is idempotent with respect to the fact it may be called any number
	 * of times but will perform replacement of stub property sources with their
	 * corresponding actual property sources once and only once.
	 * @param propertySources the {@link MutablePropertySources} to initialize (must not
	 * be {@code null})
	 * @param servletContext the current {@link ServletContext} (ignored if {@code null}
	 * or if the {@link StandardServletEnvironment#SERVLET_CONTEXT_PROPERTY_SOURCE_NAME
	 * servlet context property source} has already been initialized)
	 * @param servletConfig the current {@link ServletConfig} (ignored if {@code null}
	 * or if the {@link StandardServletEnvironment#SERVLET_CONFIG_PROPERTY_SOURCE_NAME
	 * servlet config property source} has already been initialized)
	 * @see org.springframework.core.env.PropertySource.StubPropertySource
	 * @see org.springframework.core.env.ConfigurableEnvironment#getPropertySources()
	 */
	public static void initServletPropertySources(
			MutablePropertySources propertySources, ServletContext servletContext, ServletConfig servletConfig) {
		Assert.notNull(propertySources, "propertySources must not be null");
		if(servletContext != null &&
				propertySources.contains(SERVLET_CONTEXT_PROPERTY_SOURCE_NAME) &&
				propertySources.get(SERVLET_CONTEXT_PROPERTY_SOURCE_NAME) instanceof StubPropertySource) {
			propertySources.replace(SERVLET_CONTEXT_PROPERTY_SOURCE_NAME, new ServletContextPropertySource(SERVLET_CONTEXT_PROPERTY_SOURCE_NAME, servletContext));
		}
		if(servletConfig != null &&
				propertySources.contains(SERVLET_CONFIG_PROPERTY_SOURCE_NAME) &&
				propertySources.get(SERVLET_CONFIG_PROPERTY_SOURCE_NAME) instanceof StubPropertySource) {
			propertySources.replace(SERVLET_CONFIG_PROPERTY_SOURCE_NAME, new ServletConfigPropertySource(SERVLET_CONFIG_PROPERTY_SOURCE_NAME, servletConfig));
		}
	}

	/**
	 * Return the current RequestAttributes instance as ServletRequestAttributes.
	 * @see RequestContextHolder#currentRequestAttributes()
	 */
	private static ServletRequestAttributes currentRequestAttributes() {
		RequestAttributes requestAttr = RequestContextHolder.currentRequestAttributes();
		if (!(requestAttr instanceof ServletRequestAttributes)) {
			throw new IllegalStateException("Current request is not a servlet request");
		}
		return (ServletRequestAttributes) requestAttr;
	}


	/**
	 * Factory that exposes the current request object on demand.
	 */
	@SuppressWarnings("serial")
	private static class RequestObjectFactory implements ObjectFactory<ServletRequest>, Serializable {

		@Override
		public ServletRequest getObject() {
			return currentRequestAttributes().getRequest();
		}

		@Override
		public String toString() {
			return "Current HttpServletRequest";
		}
	}


	/**
	 * Factory that exposes the current session object on demand.
	 */
	@SuppressWarnings("serial")
	private static class SessionObjectFactory implements ObjectFactory<HttpSession>, Serializable {

		@Override
		public HttpSession getObject() {
			return currentRequestAttributes().getRequest().getSession();
		}

		@Override
		public String toString() {
			return "Current HttpSession";
		}
	}


	/**
	 * Factory that exposes the current WebRequest object on demand.
	 */
	@SuppressWarnings("serial")
	private static class WebRequestObjectFactory implements ObjectFactory<WebRequest>, Serializable {

		@Override
		public WebRequest getObject() {
			return new ServletWebRequest(currentRequestAttributes().getRequest());
		}

		@Override
		public String toString() {
			return "Current ServletWebRequest";
		}
	}


	/**
	 * Inner class to avoid hard-coded JSF dependency.
 	 */
	private static class FacesDependencyRegistrar {

		public static void registerFacesDependencies(ConfigurableListableBeanFactory beanFactory) {
			beanFactory.registerResolvableDependency(FacesContext.class, new ObjectFactory<FacesContext>() {
				@Override
				public FacesContext getObject() {
					return FacesContext.getCurrentInstance();
				}
				@Override
				public String toString() {
					return "Current JSF FacesContext";
				}
			});
			beanFactory.registerResolvableDependency(ExternalContext.class, new ObjectFactory<ExternalContext>() {
				@Override
				public ExternalContext getObject() {
					return FacesContext.getCurrentInstance().getExternalContext();
				}
				@Override
				public String toString() {
					return "Current JSF ExternalContext";
				}
			});
		}
	}

}
