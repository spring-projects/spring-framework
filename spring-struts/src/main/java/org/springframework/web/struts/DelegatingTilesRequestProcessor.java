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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.tiles.TilesRequestProcessor;

import org.springframework.beans.BeansException;
import org.springframework.web.context.WebApplicationContext;

/**
 * Subclass of Struts's TilesRequestProcessor that autowires
 * Struts Actions defined in ContextLoaderPlugIn's WebApplicationContext
 * (or, as a fallback, in the root WebApplicationContext).
 *
 * <p>Behaves like
 * {@link DelegatingRequestProcessor DelegatingRequestProcessor},
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
 * @since 1.0.2
 * @see DelegatingRequestProcessor
 * @see DelegatingActionProxy
 * @see DelegatingActionUtils
 * @see ContextLoaderPlugIn
 * @deprecated as of Spring 3.0
 */
@Deprecated
public class DelegatingTilesRequestProcessor extends TilesRequestProcessor {

	private WebApplicationContext webApplicationContext;


	@Override
	public void init(ActionServlet actionServlet, ModuleConfig moduleConfig) throws ServletException {
		super.init(actionServlet, moduleConfig);
		if (actionServlet != null) {
			this.webApplicationContext = initWebApplicationContext(actionServlet, moduleConfig);
		}
	}

	/**
	 * Fetch ContextLoaderPlugIn's WebApplicationContext from the ServletContext,
	 * falling back to the root WebApplicationContext. This context is supposed
	 * to contain the Struts Action beans to delegate to.
	 * @param actionServlet the associated ActionServlet
	 * @param moduleConfig the associated ModuleConfig
	 * @return the WebApplicationContext
	 * @throws IllegalStateException if no WebApplicationContext could be found
	 * @see DelegatingActionUtils#findRequiredWebApplicationContext
	 * @see ContextLoaderPlugIn#SERVLET_CONTEXT_PREFIX
	 */
	protected WebApplicationContext initWebApplicationContext(
			ActionServlet actionServlet, ModuleConfig moduleConfig) throws IllegalStateException {

		return DelegatingActionUtils.findRequiredWebApplicationContext(actionServlet, moduleConfig);
	}

	/**
	 * Return the WebApplicationContext that this processor delegates to.
	 */
	protected final WebApplicationContext getWebApplicationContext() {
		return webApplicationContext;
	}


	/**
	 * Override the base class method to return the delegate action.
	 * @see #getDelegateAction
	 */
	@Override
	protected Action processActionCreate(
			HttpServletRequest request, HttpServletResponse response, ActionMapping mapping)
			throws IOException {

		Action action = getDelegateAction(mapping);
		if (action != null) {
			return action;
		}
		return super.processActionCreate(request, response, mapping);
	}

	/**
	 * Return the delegate Action for the given mapping.
	 * <p>The default implementation determines a bean name from the
	 * given ActionMapping and looks up the corresponding bean in the
	 * WebApplicationContext.
	 * @param mapping the Struts ActionMapping
	 * @return the delegate Action, or <code>null</code> if none found
	 * @throws BeansException if thrown by WebApplicationContext methods
	 * @see #determineActionBeanName
	 */
	protected Action getDelegateAction(ActionMapping mapping) throws BeansException {
		String beanName = determineActionBeanName(mapping);
		if (!getWebApplicationContext().containsBean(beanName)) {
			return null;
		}
		return (Action) getWebApplicationContext().getBean(beanName, Action.class);
	}

	/**
	 * Determine the name of the Action bean, to be looked up in
	 * the WebApplicationContext.
	 * <p>The default implementation takes the mapping path and
	 * prepends the module prefix, if any.
	 * @param mapping the Struts ActionMapping
	 * @return the name of the Action bean
	 * @see DelegatingActionUtils#determineActionBeanName
	 * @see ActionMapping#getPath
	 * @see ModuleConfig#getPrefix
	 */
	protected String determineActionBeanName(ActionMapping mapping) {
		return DelegatingActionUtils.determineActionBeanName(mapping);
	}

}
