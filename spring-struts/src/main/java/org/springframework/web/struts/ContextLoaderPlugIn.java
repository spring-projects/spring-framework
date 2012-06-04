/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.web.struts;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.PlugIn;
import org.apache.struts.config.ModuleConfig;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * Struts 1.1+ PlugIn that loads a Spring application context for the Struts
 * ActionServlet. This context will automatically refer to the root
 * WebApplicationContext (loaded by ContextLoaderListener/Servlet) as parent.
 *
 * <p>The default namespace of the WebApplicationContext is the name of the
 * Struts ActionServlet, suffixed with "-servlet" (e.g. "action-servlet").
 * The default location of the XmlWebApplicationContext configuration file
 * is therefore "/WEB-INF/action-servlet.xml".
 *
 * <pre>
 * &lt;plug-in className="org.springframework.web.struts.ContextLoaderPlugIn"/&gt;</pre>
 *
 * The location of the context configuration files can be customized
 * through the "contextConfigLocation" setting, analogous to the root
 * WebApplicationContext and FrameworkServlet contexts.
 *
 * <pre>
 * &lt;plug-in className="org.springframework.web.struts.ContextLoaderPlugIn"&gt;
 *   &lt;set-property property="contextConfigLocation" value="/WEB-INF/action-servlet.xml /WEB-INF/myContext.xml"/&gt;
 * &lt;/plug-in&gt;</pre>
 *
 * Beans defined in the ContextLoaderPlugIn context can be accessed
 * from conventional Struts Actions, via fetching the WebApplicationContext
 * reference from the ServletContext. ActionSupport and DispatchActionSupport
 * are pre-built convenience classes that provide easy access to the context.
 *
 * <p>It is normally preferable to access Spring's root WebApplicationContext
 * in such scenarios, though: A shared middle tier should be defined there
 * rather than in a ContextLoaderPlugin context, for access by any web component.
 * ActionSupport and DispatchActionSupport autodetect the root context too.
 *
 * <p>A special usage of this PlugIn is to define Struts Actions themselves
 * as beans, typically wiring them with middle tier components defined in the
 * root context. Such Actions will then be delegated to by proxy definitions
 * in the Struts configuration, using the DelegatingActionProxy class or
 * the DelegatingRequestProcessor.
 *
 * <p>Note that you can use a single ContextLoaderPlugIn for all Struts modules.
 * That context can in turn be loaded from multiple XML files, for example split
 * according to Struts modules. Alternatively, define one ContextLoaderPlugIn per
 * Struts module, specifying appropriate "contextConfigLocation" parameters.
 *
 * <p>Note: The idea of delegating to Spring-managed Struts Actions originated in
 * Don Brown's <a href="http://struts.sourceforge.net/struts-spring">Spring Struts Plugin</a>.
 * ContextLoaderPlugIn and DelegatingActionProxy constitute a clean-room
 * implementation of the same idea, essentially superseding the original plugin.
 * Many thanks to Don Brown and Matt Raible for the original work and for the
 * agreement to reimplement the idea in Spring proper!
 *
 * @author Juergen Hoeller
 * @since 1.0.1
 * @see #SERVLET_CONTEXT_PREFIX
 * @see ActionSupport
 * @see DispatchActionSupport
 * @see DelegatingActionProxy
 * @see DelegatingRequestProcessor
 * @see DelegatingTilesRequestProcessor
 * @see org.springframework.web.context.ContextLoaderListener
 * @see org.springframework.web.servlet.FrameworkServlet
 * @deprecated as of Spring 3.0
 */
@Deprecated
public class ContextLoaderPlugIn implements PlugIn {

	/**
	 * Suffix for WebApplicationContext namespaces. If a Struts ActionServlet is
	 * given the name "action" in a context, the namespace used by this PlugIn will
	 * resolve to "action-servlet".
	 */
	public static final String DEFAULT_NAMESPACE_SUFFIX = "-servlet";

	/**
	 * Default context class for ContextLoaderPlugIn.
	 * @see org.springframework.web.context.support.XmlWebApplicationContext
	 */
	public static final Class DEFAULT_CONTEXT_CLASS = XmlWebApplicationContext.class;

	/**
	 * Prefix for the ServletContext attribute for the WebApplicationContext.
	 * The completion is the Struts module name.
	 */
	public static final String SERVLET_CONTEXT_PREFIX = ContextLoaderPlugIn.class.getName() + ".CONTEXT.";


	protected final Log logger = LogFactory.getLog(getClass());

	/** Custom WebApplicationContext class */
	private Class contextClass = DEFAULT_CONTEXT_CLASS;

	/** Namespace for this servlet */
	private String namespace;

	/** Explicit context config location */
	private String contextConfigLocation;

	/** The Struts ActionServlet that this PlugIn is registered with */
	private ActionServlet actionServlet;

	/** The Struts ModuleConfig that this PlugIn is registered with */
	private ModuleConfig moduleConfig;

	/** WebApplicationContext for the ActionServlet */
	private WebApplicationContext webApplicationContext;


	/**
	 * Set a custom context class by name. This class must be of type WebApplicationContext,
	 * when using the default ContextLoaderPlugIn implementation, the context class
	 * must also implement ConfigurableWebApplicationContext.
	 * @see #createWebApplicationContext
	 */
	public void setContextClassName(String contextClassName) throws ClassNotFoundException {
		this.contextClass = ClassUtils.forName(contextClassName);
	}

	/**
	 * Set a custom context class. This class must be of type WebApplicationContext,
	 * when using the default ContextLoaderPlugIn implementation, the context class
	 * must also implement ConfigurableWebApplicationContext.
	 * @see #createWebApplicationContext
	 */
	public void setContextClass(Class contextClass) {
		this.contextClass = contextClass;
	}

	/**
	 * Return the custom context class.
	 */
	public Class getContextClass() {
		return this.contextClass;
	}

	/**
	 * Set a custom namespace for the ActionServlet,
	 * to be used for building a default context config location.
	 */
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	/**
	 * Return the namespace for the ActionServlet, falling back to default scheme if
	 * no custom namespace was set: e.g. "test-servlet" for a servlet named "test".
	 */
	public String getNamespace() {
		if (this.namespace != null) {
			return this.namespace;
		}
		if (this.actionServlet != null) {
			return this.actionServlet.getServletName() + DEFAULT_NAMESPACE_SUFFIX;
		}
		return null;
	}

	/**
	 * Set the context config location explicitly, instead of relying on the default
	 * location built from the namespace. This location string can consist of
	 * multiple locations separated by any number of commas and spaces.
	 */
	public void setContextConfigLocation(String contextConfigLocation) {
		this.contextConfigLocation = contextConfigLocation;
	}

	/**
	 * Return the explicit context config location, if any.
	 */
	public String getContextConfigLocation() {
		return this.contextConfigLocation;
	}


	/**
	 * Create the ActionServlet's WebApplicationContext.
	 */
	public final void init(ActionServlet actionServlet, ModuleConfig moduleConfig) throws ServletException {
		long startTime = System.currentTimeMillis();
		if (logger.isInfoEnabled()) {
			logger.info("ContextLoaderPlugIn for Struts ActionServlet '" + actionServlet.getServletName() +
					", module '" + moduleConfig.getPrefix() + "': initialization started");
		}

		this.actionServlet = actionServlet;
		this.moduleConfig = moduleConfig;
		try {
			this.webApplicationContext = initWebApplicationContext();
			onInit();
		}
		catch (RuntimeException ex) {
			logger.error("Context initialization failed", ex);
			throw ex;
		}

		if (logger.isInfoEnabled()) {
			long elapsedTime = System.currentTimeMillis() - startTime;
			logger.info("ContextLoaderPlugIn for Struts ActionServlet '" + actionServlet.getServletName() +
					"', module '" + moduleConfig.getPrefix() + "': initialization completed in " + elapsedTime + " ms");
		}
	}

	/**
	 * Return the Struts ActionServlet that this PlugIn is associated with.
	 */
	public final ActionServlet getActionServlet() {
		return actionServlet;
	}

	/**
	 * Return the name of the ActionServlet that this PlugIn is associated with.
	 */
	public final String getServletName() {
		return this.actionServlet.getServletName();
	}

	/**
	 * Return the ServletContext that this PlugIn is associated with.
	 */
	public final ServletContext getServletContext() {
		return this.actionServlet.getServletContext();
	}

	/**
	 * Return the Struts ModuleConfig that this PlugIn is associated with.
	 */
	public final ModuleConfig getModuleConfig() {
		return this.moduleConfig;
	}

	/**
	 * Return the prefix of the ModuleConfig that this PlugIn is associated with.
	 * @see org.apache.struts.config.ModuleConfig#getPrefix
	 */
	public final String getModulePrefix() {
		return this.moduleConfig.getPrefix();
	}

	/**
	 * Initialize and publish the WebApplicationContext for the ActionServlet.
	 * <p>Delegates to {@link #createWebApplicationContext} for actual creation.
	 * <p>Can be overridden in subclasses. Call <code>getActionServlet()</code>
	 * and/or <code>getModuleConfig()</code> to access the Struts configuration
	 * that this PlugIn is associated with.
	 * @throws org.springframework.beans.BeansException if the context couldn't be initialized
	 * @throws IllegalStateException if there is already a context for the Struts ActionServlet
	 * @see #getActionServlet()
	 * @see #getServletName()
	 * @see #getServletContext()
	 * @see #getModuleConfig()
	 * @see #getModulePrefix()
	 */
	protected WebApplicationContext initWebApplicationContext() throws BeansException, IllegalStateException {
		getServletContext().log("Initializing WebApplicationContext for Struts ActionServlet '" +
				getServletName() + "', module '" + getModulePrefix() + "'");
		WebApplicationContext parent = WebApplicationContextUtils.getWebApplicationContext(getServletContext());

		WebApplicationContext wac = createWebApplicationContext(parent);
		if (logger.isInfoEnabled()) {
			logger.info("Using context class '" + wac.getClass().getName() + "' for servlet '" + getServletName() + "'");
		}

		// Publish the context as a servlet context attribute.
		String attrName = getServletContextAttributeName();
		getServletContext().setAttribute(attrName, wac);
		if (logger.isDebugEnabled()) {
			logger.debug("Published WebApplicationContext of Struts ActionServlet '" + getServletName() +
					"', module '" + getModulePrefix() + "' as ServletContext attribute with name [" + attrName + "]");
		}
		
		return wac;
	}

	/**
	 * Instantiate the WebApplicationContext for the ActionServlet, either a default
	 * XmlWebApplicationContext or a custom context class if set.
	 * <p>This implementation expects custom contexts to implement ConfigurableWebApplicationContext.
	 * Can be overridden in subclasses.
	 * @throws org.springframework.beans.BeansException if the context couldn't be initialized
	 * @see #setContextClass
	 * @see org.springframework.web.context.support.XmlWebApplicationContext
	 */
	protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent)
			throws BeansException {

		if (logger.isDebugEnabled()) {
			logger.debug("ContextLoaderPlugIn for Struts ActionServlet '" + getServletName() +
					"', module '" + getModulePrefix() + "' will try to create custom WebApplicationContext " +
					"context of class '" + getContextClass().getName() + "', using parent context [" + parent + "]");
		}
		if (!ConfigurableWebApplicationContext.class.isAssignableFrom(getContextClass())) {
			throw new ApplicationContextException(
					"Fatal initialization error in ContextLoaderPlugIn for Struts ActionServlet '" + getServletName() +
					"', module '" + getModulePrefix() + "': custom WebApplicationContext class [" +
					getContextClass().getName() + "] is not of type ConfigurableWebApplicationContext");
		}

		ConfigurableWebApplicationContext wac =
				(ConfigurableWebApplicationContext) BeanUtils.instantiateClass(getContextClass());
		wac.setParent(parent);
		wac.setServletContext(getServletContext());
		wac.setNamespace(getNamespace());
		if (getContextConfigLocation() != null) {
			wac.setConfigLocations(
			    StringUtils.tokenizeToStringArray(
							getContextConfigLocation(), ConfigurableWebApplicationContext.CONFIG_LOCATION_DELIMITERS));
		}
		wac.addBeanFactoryPostProcessor(
				new BeanFactoryPostProcessor() {
					public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
						beanFactory.addBeanPostProcessor(new ActionServletAwareProcessor(getActionServlet()));
						beanFactory.ignoreDependencyType(ActionServlet.class);
					}
				}
		);

		wac.refresh();
		return wac;
	}

	/**
	 * Return the ServletContext attribute name for this PlugIn's WebApplicationContext.
	 * <p>The default implementation returns SERVLET_CONTEXT_PREFIX + module prefix.
	 * @see #SERVLET_CONTEXT_PREFIX
	 * @see #getModulePrefix()
	 */
	public String getServletContextAttributeName() {
		return SERVLET_CONTEXT_PREFIX + getModulePrefix();
	}

	/**
	 * Return this PlugIn's WebApplicationContext.
	 */
	public final WebApplicationContext getWebApplicationContext() {
		return webApplicationContext;
	}

	/**
	 * Callback for custom initialization after the context has been set up.
	 * @throws ServletException if initialization failed
	 */
	protected void onInit() throws ServletException {
	}


	/**
	 * Close the WebApplicationContext of the ActionServlet.
	 * @see org.springframework.context.ConfigurableApplicationContext#close()
	 */
	public void destroy() {
		getServletContext().log("Closing WebApplicationContext of Struts ActionServlet '" +
				getServletName() + "', module '" + getModulePrefix() + "'");
		if (getWebApplicationContext() instanceof ConfigurableApplicationContext) {
			((ConfigurableApplicationContext) getWebApplicationContext()).close();
		}
	}

}
