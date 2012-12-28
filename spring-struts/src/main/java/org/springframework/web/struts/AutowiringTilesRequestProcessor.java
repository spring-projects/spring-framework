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

package org.springframework.web.struts;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.tiles.TilesRequestProcessor;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.WebApplicationContext;

/**
 * Subclass of Struts's TilesRequestProcessor that autowires Struts Actions
 * with Spring beans defined in ContextLoaderPlugIn's WebApplicationContext
 * or - in case of general service layer beans - in the root WebApplicationContext.
 *
 * <p>Behaves like
 * {@link AutowiringRequestProcessor AutowiringRequestProcessor},
 * but also provides the Tiles functionality of the original TilesRequestProcessor.
 * As there's just a single central class to customize in Struts, we have to provide
 * another subclass here, covering both the Tiles and the Spring delegation aspect.
 *
 * <p>The default implementation delegates to the DelegatingActionUtils
 * class as fas as possible, to reuse as much code as possible despite
 * the need to provide two RequestProcessor subclasses. If you need to
 * subclass yet another RequestProcessor, take this class as a template,
 * delegating to DelegatingActionUtils just like it.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see AutowiringRequestProcessor
 * @see ContextLoaderPlugIn
 * @see DelegatingActionUtils
 * @deprecated as of Spring 3.0
 */
@Deprecated
public class AutowiringTilesRequestProcessor extends TilesRequestProcessor {

	private WebApplicationContext webApplicationContext;

	private int autowireMode = AutowireCapableBeanFactory.AUTOWIRE_NO;

	private boolean dependencyCheck = false;


	@Override
	public void init(ActionServlet actionServlet, ModuleConfig moduleConfig) throws ServletException {
		super.init(actionServlet, moduleConfig);
		if (actionServlet != null) {
			this.webApplicationContext = initWebApplicationContext(actionServlet, moduleConfig);
			this.autowireMode = initAutowireMode(actionServlet, moduleConfig);
			this.dependencyCheck = initDependencyCheck(actionServlet, moduleConfig);
		}
	}

	/**
	 * Fetch ContextLoaderPlugIn's WebApplicationContext from the ServletContext,
	 * falling back to the root WebApplicationContext. This context is supposed
	 * to contain the service layer beans to wire the Struts Actions with.
	 * @param actionServlet the associated ActionServlet
	 * @param moduleConfig the associated ModuleConfig
	 * @return the WebApplicationContext
	 * @throws IllegalStateException if no WebApplicationContext could be found
	 * @see DelegatingActionUtils#findRequiredWebApplicationContext
	 * @see ContextLoaderPlugIn#SERVLET_CONTEXT_PREFIX
	 */
	protected WebApplicationContext initWebApplicationContext(
			ActionServlet actionServlet, ModuleConfig moduleConfig) throws IllegalStateException {

		WebApplicationContext wac =
				DelegatingActionUtils.findRequiredWebApplicationContext(actionServlet, moduleConfig);
		if (wac instanceof ConfigurableApplicationContext) {
			((ConfigurableApplicationContext) wac).getBeanFactory().ignoreDependencyType(ActionServlet.class);
		}
		return wac;
	}

	/**
	 * Determine the autowire mode to use for wiring Struts Actions.
	 * <p>The default implementation checks the "autowire" init-param of the
	 * Struts ActionServlet, falling back to "AUTOWIRE_BY_TYPE" as default.
	 * @param actionServlet the associated ActionServlet
	 * @param moduleConfig the associated ModuleConfig
	 * @return the autowire mode to use
	 * @see DelegatingActionUtils#getAutowireMode
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#autowireBeanProperties
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#AUTOWIRE_BY_TYPE
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#AUTOWIRE_BY_NAME
	 */
	protected int initAutowireMode(ActionServlet actionServlet, ModuleConfig moduleConfig) {
		return DelegatingActionUtils.getAutowireMode(actionServlet);
	}

	/**
	 * Determine whether to apply a dependency check after wiring Struts Actions.
	 * <p>The default implementation checks the "dependencyCheck" init-param of the
	 * Struts ActionServlet, falling back to no dependency check as default.
	 * @param actionServlet the associated ActionServlet
	 * @param moduleConfig the associated ModuleConfig
	 * @return whether to enforce a dependency check or not
	 * @see DelegatingActionUtils#getDependencyCheck
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#autowireBeanProperties
	 */
	protected boolean initDependencyCheck(ActionServlet actionServlet, ModuleConfig moduleConfig) {
		return DelegatingActionUtils.getDependencyCheck(actionServlet);
	}


	/**
	 * Return the current Spring WebApplicationContext.
	 */
	protected final WebApplicationContext getWebApplicationContext() {
		return this.webApplicationContext;
	}

	/**
	 * Return the autowire mode to use for wiring Struts Actions.
	 */
	protected final int getAutowireMode() {
		return autowireMode;
	}

	/**
	 * Return whether to apply a dependency check after wiring Struts Actions.
	 */
	protected final boolean getDependencyCheck() {
		return dependencyCheck;
	}


	/**
	 * Extend the base class method to autowire each created Action instance.
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#autowireBeanProperties
	 */
	@Override
	protected Action processActionCreate(
			HttpServletRequest request, HttpServletResponse response, ActionMapping mapping)
			throws IOException {

		Action action = super.processActionCreate(request, response, mapping);
		getWebApplicationContext().getAutowireCapableBeanFactory().autowireBeanProperties(
				action, getAutowireMode(), getDependencyCheck());
		return action;
	}

}
