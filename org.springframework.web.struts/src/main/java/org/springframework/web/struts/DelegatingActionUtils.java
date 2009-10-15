/*
 * Copyright 2002-2006 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.config.ModuleConfig;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Common methods for letting Struts Actions work with a
 * Spring WebApplicationContext.
 *
 * <p>As everything in Struts is based on concrete inheritance,
 * we have to provide an Action subclass (DelegatingActionProxy) and
 * two RequestProcessor subclasses (DelegatingRequestProcessor and
 * DelegatingTilesRequestProcessor). The only way to share common
 * functionality is a utility class like this one.
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 * @see DelegatingActionProxy
 * @see DelegatingRequestProcessor
 * @see DelegatingTilesRequestProcessor
 * @deprecated as of Spring 3.0
 */
@Deprecated
public abstract class DelegatingActionUtils {

	/**
	 * The name of the autowire init-param specified on the Struts ActionServlet:
	 * "spring.autowire"
	 */
	public static final String PARAM_AUTOWIRE = "spring.autowire";

	/**
	 * The name of the dependency check init-param specified on the Struts ActionServlet:
	 * "spring.dependencyCheck"
	 */
	public static final String PARAM_DEPENDENCY_CHECK = "spring.dependencyCheck";

	/**
	 * Value of the autowire init-param that indicates autowiring by name:
	 * "byName"
	 */
	public static final String AUTOWIRE_BY_NAME = "byName";

	/**
	 * Value of the autowire init-param that indicates autowiring by type:
	 * "byType"
	 */
	public static final String AUTOWIRE_BY_TYPE = "byType";


	private static final Log logger = LogFactory.getLog(DelegatingActionUtils.class);


	/**
	 * Fetch ContextLoaderPlugIn's WebApplicationContext from the ServletContext.
	 * <p>Checks for a module-specific context first, falling back to the
	 * context for the default module else.
	 * @param actionServlet the associated ActionServlet
	 * @param moduleConfig the associated ModuleConfig (can be <code>null</code>)
	 * @return the WebApplicationContext, or <code>null</code> if none
	 * @see ContextLoaderPlugIn#SERVLET_CONTEXT_PREFIX
	 */
	public static WebApplicationContext getWebApplicationContext(
			ActionServlet actionServlet, ModuleConfig moduleConfig) {

		WebApplicationContext wac = null;
		String modulePrefix = null;

		// Try module-specific attribute.
		if (moduleConfig != null) {
			modulePrefix = moduleConfig.getPrefix();
			wac = (WebApplicationContext) actionServlet.getServletContext().getAttribute(
					ContextLoaderPlugIn.SERVLET_CONTEXT_PREFIX + modulePrefix);
		}

		// If not found, try attribute for default module.
		if (wac == null && !"".equals(modulePrefix)) {
			wac = (WebApplicationContext) actionServlet.getServletContext().getAttribute(
					ContextLoaderPlugIn.SERVLET_CONTEXT_PREFIX);
		}

		return wac;
	}

	/**
	 * Fetch ContextLoaderPlugIn's WebApplicationContext from the ServletContext.
	 * <p>Checks for a module-specific context first, falling back to the
	 * context for the default module else.
	 * @param actionServlet the associated ActionServlet
	 * @param moduleConfig the associated ModuleConfig (can be <code>null</code>)
	 * @return the WebApplicationContext
	 * @throws IllegalStateException if no WebApplicationContext could be found
	 * @see ContextLoaderPlugIn#SERVLET_CONTEXT_PREFIX
	 */
	public static WebApplicationContext getRequiredWebApplicationContext(
			ActionServlet actionServlet, ModuleConfig moduleConfig) throws IllegalStateException {

		WebApplicationContext wac = getWebApplicationContext(actionServlet, moduleConfig);
		// If no Struts-specific context found, throw an exception.
		if (wac == null) {
			throw new IllegalStateException(
					"Could not find ContextLoaderPlugIn's WebApplicationContext as ServletContext attribute [" +
					ContextLoaderPlugIn.SERVLET_CONTEXT_PREFIX + "]: Did you register [" +
					ContextLoaderPlugIn.class.getName() + "]?");
		}
		return wac;
	}

	/**
	 * Find most specific context available: check ContextLoaderPlugIn's
	 * WebApplicationContext first, fall back to root WebApplicationContext else.
	 * <p>When checking the ContextLoaderPlugIn context: checks for a module-specific
	 * context first, falling back to the context for the default module else.
	 * @param actionServlet the associated ActionServlet
	 * @param moduleConfig the associated ModuleConfig (can be <code>null</code>)
	 * @return the WebApplicationContext
	 * @throws IllegalStateException if no WebApplicationContext could be found
	 * @see #getWebApplicationContext
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#getRequiredWebApplicationContext
	 */
	public static WebApplicationContext findRequiredWebApplicationContext(
			ActionServlet actionServlet, ModuleConfig moduleConfig) throws IllegalStateException {

		WebApplicationContext wac = getWebApplicationContext(actionServlet, moduleConfig);
		// If no Struts-specific context found, fall back to root context.
		if (wac == null) {
			wac = WebApplicationContextUtils.getRequiredWebApplicationContext(actionServlet.getServletContext());
		}
		return wac;
	}

	/**
	 * Default implementation of Action bean determination, taking
	 * the mapping path and prepending the module prefix, if any.
	 * @param mapping the Struts ActionMapping
	 * @return the name of the Action bean
	 * @see org.apache.struts.action.ActionMapping#getPath
	 * @see org.apache.struts.config.ModuleConfig#getPrefix
	 */
	public static String determineActionBeanName(ActionMapping mapping) {
		String prefix = mapping.getModuleConfig().getPrefix();
		String path = mapping.getPath();
		String beanName = prefix + path;
		if (logger.isDebugEnabled()) {
			logger.debug("DelegatingActionProxy with mapping path '" + path + "' and module prefix '" +
					prefix + "' delegating to Spring bean with name [" + beanName + "]");
		}
		return beanName;
	}

	/**
	 * Determine the autowire mode from the "autowire" init-param of the
	 * Struts ActionServlet, falling back to "AUTOWIRE_BY_TYPE" as default.
	 * @param actionServlet the Struts ActionServlet
	 * @return the autowire mode to use
	 * @see #PARAM_AUTOWIRE
	 * @see #AUTOWIRE_BY_NAME
	 * @see #AUTOWIRE_BY_TYPE
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#autowireBeanProperties
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#AUTOWIRE_BY_TYPE
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#AUTOWIRE_BY_NAME
	 */
	public static int getAutowireMode(ActionServlet actionServlet) {
		String autowire = actionServlet.getInitParameter(PARAM_AUTOWIRE);
		if (autowire != null) {
			if (AUTOWIRE_BY_NAME.equals(autowire)) {
				return AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;
			}
			else if (!AUTOWIRE_BY_TYPE.equals(autowire)) {
				throw new IllegalArgumentException("ActionServlet 'autowire' parameter must be 'byName' or 'byType'");
			}
		}
		return AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;
	}

	/**
	 * Determine the dependency check to use from the "dependencyCheck" init-param
	 * of the Struts ActionServlet, falling back to no dependency check as default.
	 * @param actionServlet the Struts ActionServlet
	 * @return whether to enforce a dependency check or not
	 * @see #PARAM_DEPENDENCY_CHECK
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#autowireBeanProperties
	 */
	public static boolean getDependencyCheck(ActionServlet actionServlet) {
		String dependencyCheck = actionServlet.getInitParameter(PARAM_DEPENDENCY_CHECK);
		return Boolean.valueOf(dependencyCheck).booleanValue();
	}

}
