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

package org.springframework.web.portlet.context;

import javax.portlet.PortletContext;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestScope;
import org.springframework.web.context.request.SessionScope;

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
	 * Register portlet-specific scopes with the given BeanFactory,
	 * as used by the Portlet ApplicationContext.
	 * @param beanFactory the BeanFactory to configure
	 */
	static void registerPortletApplicationScopes(ConfigurableListableBeanFactory beanFactory) {
		beanFactory.registerScope(WebApplicationContext.SCOPE_REQUEST, new RequestScope());
		beanFactory.registerScope(WebApplicationContext.SCOPE_SESSION, new SessionScope(false));
		beanFactory.registerScope(WebApplicationContext.SCOPE_GLOBAL_SESSION, new SessionScope(true));

		beanFactory.registerResolvableDependency(PortletRequest.class, new ObjectFactory() {
			public Object getObject() {
				RequestAttributes requestAttr = RequestContextHolder.currentRequestAttributes();
				if (!(requestAttr instanceof PortletRequestAttributes)) {
					throw new IllegalStateException("Current request is not a portlet request");
				}
				return ((PortletRequestAttributes) requestAttr).getRequest();
			}
		});
		beanFactory.registerResolvableDependency(PortletSession.class, new ObjectFactory() {
			public Object getObject() {
				RequestAttributes requestAttr = RequestContextHolder.currentRequestAttributes();
				if (!(requestAttr instanceof PortletRequestAttributes)) {
					throw new IllegalStateException("Current request is not a portlet request");
				}
				return ((PortletRequestAttributes) requestAttr).getRequest().getPortletSession();
			}
		});
	}

}
