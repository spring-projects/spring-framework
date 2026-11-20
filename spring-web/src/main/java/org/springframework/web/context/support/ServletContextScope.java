/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.context.support;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.ServletContext;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.util.Assert;

/**
 * {@link Scope} wrapper for a ServletContext, i.e. for global web application attributes.
 *
 * <p>This differs from traditional Spring singletons in that it exposes attributes in the
 * ServletContext. Those attributes will get destroyed whenever the entire application
 * shuts down, which might be earlier or later than the shutdown of the containing Spring
 * ApplicationContext.
 *
 * <p>The associated destruction mechanism relies on a
 * {@link org.springframework.web.context.ContextCleanupListener} being registered in
 * {@code web.xml}. Note that {@link org.springframework.web.context.ContextLoaderListener}
 * includes ContextCleanupListener's functionality.
 *
 * <p>This scope is registered as default scope with key
 * {@link org.springframework.web.context.WebApplicationContext#SCOPE_APPLICATION "application"}.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.web.context.ContextCleanupListener
 */
public class ServletContextScope implements Scope, DisposableBean {

	private final ServletContext servletContext;

	private final Map<String, Runnable> destructionCallbacks = new LinkedHashMap<>();


	/**
	 * Create a new Scope wrapper for the given ServletContext.
	 * @param servletContext the ServletContext to wrap
	 */
	public ServletContextScope(ServletContext servletContext) {
		Assert.notNull(servletContext, "ServletContext must not be null");
		this.servletContext = servletContext;
	}


	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		Object scopedObject = this.servletContext.getAttribute(name);
		if (scopedObject == null) {
			scopedObject = objectFactory.getObject();
			this.servletContext.setAttribute(name, scopedObject);
		}
		return scopedObject;
	}

	@Override
	public @Nullable Object remove(String name) {
		Object scopedObject = this.servletContext.getAttribute(name);
		if (scopedObject != null) {
			synchronized (this.destructionCallbacks) {
				this.destructionCallbacks.remove(name);
			}
			this.servletContext.removeAttribute(name);
			return scopedObject;
		}
		else {
			return null;
		}
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
		synchronized (this.destructionCallbacks) {
			this.destructionCallbacks.put(name, callback);
		}
	}

	/**
	 * Invoke all registered destruction callbacks.
	 * To be called on ServletContext shutdown.
	 * @see org.springframework.web.context.ContextCleanupListener
	 */
	@Override
	public void destroy() {
		synchronized (this.destructionCallbacks) {
			for (Runnable runnable : this.destructionCallbacks.values()) {
				runnable.run();
			}
			this.destructionCallbacks.clear();
		}
	}

}
