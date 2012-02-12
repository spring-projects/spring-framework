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

package org.springframework.web.portlet.context;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.portlet.PortletContext;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.util.Assert;

/**
 * {@link Scope} wrapper for a PortletContext, i.e. for global web application attributes.
 *
 * <p>This differs from traditional Spring singletons in that it exposes attributes in the
 * PortletContext. Those attributes will get destroyed whenever the entire application
 * shuts down, which might be earlier or later than the shutdown of the containing Spring
 * ApplicationContext.
 *
 * <p>The associated destruction mechanism relies on a
 * {@link org.springframework.web.context.ContextCleanupListener} being registered in
 * <code>web.xml</code>. Note that {@link org.springframework.web.context.ContextLoaderListener}
 * includes ContextCleanupListener's functionality.
 *
 * <p>This scope is registered as default scope with key
 * {@link org.springframework.web.context.WebApplicationContext#SCOPE_APPLICATION "application"}.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.web.context.ContextCleanupListener
 */
public class PortletContextScope implements Scope, DisposableBean {

	private final PortletContext portletContext;

	private final Map<String, Runnable> destructionCallbacks = new LinkedHashMap<String, Runnable>();


	/**
	 * Create a new Scope wrapper for the given PortletContext.
	 * @param portletContext the PortletContext to wrap
	 */
	public PortletContextScope(PortletContext portletContext) {
		Assert.notNull(portletContext, "PortletContext must not be null");
		this.portletContext = portletContext;
	}


	public Object get(String name, ObjectFactory<?> objectFactory) {
		Object scopedObject = this.portletContext.getAttribute(name);
		if (scopedObject == null) {
			scopedObject = objectFactory.getObject();
			this.portletContext.setAttribute(name, scopedObject);
		}
		return scopedObject;
	}

	public Object remove(String name) {
		Object scopedObject = this.portletContext.getAttribute(name);
		if (scopedObject != null) {
			this.portletContext.removeAttribute(name);
			this.destructionCallbacks.remove(name);
			return scopedObject;
		}
		else {
			return null;
		}
	}

	public void registerDestructionCallback(String name, Runnable callback) {
		this.destructionCallbacks.put(name, callback);
	}

	public Object resolveContextualObject(String key) {
		return null;
	}

	public String getConversationId() {
		return null;
	}


	/**
	 * Invoke all registered destruction callbacks.
	 * To be called on ServletContext shutdown.
	 * @see org.springframework.web.context.ContextCleanupListener
	 */
	public void destroy() {
		for (Runnable runnable : this.destructionCallbacks.values()) {
			runnable.run();
		}
		this.destructionCallbacks.clear();
	}

}
