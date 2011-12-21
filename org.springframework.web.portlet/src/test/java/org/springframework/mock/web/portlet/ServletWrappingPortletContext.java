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

package org.springframework.mock.web.portlet;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import javax.portlet.PortletContext;
import javax.portlet.PortletRequestDispatcher;
import javax.servlet.ServletContext;

import org.springframework.util.Assert;

/**
 * Mock implementation of the {@link javax.portlet.PortletContext} interface,
 * wrapping an underlying {@link javax.servlet.ServletContext}.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.mock.web.portlet.MockPortletContext
 */
public class ServletWrappingPortletContext implements PortletContext {

	private final ServletContext servletContext;


	/**
	 * Create a new PortletContext wrapping the given ServletContext.
	 * @param servletContext the ServletContext to wrap
	 */
	public ServletWrappingPortletContext(ServletContext servletContext) {
		Assert.notNull(servletContext, "ServletContext must not be null");
		this.servletContext = servletContext;
	}

	/**
	 * Return the underlying ServletContext that this PortletContext wraps.
	 */
	public final ServletContext getServletContext() {
		return this.servletContext;
	}


	public String getServerInfo() {
		return this.servletContext.getServerInfo();
	}

	public PortletRequestDispatcher getRequestDispatcher(String path) {
		return null;
	}

	public PortletRequestDispatcher getNamedDispatcher(String name) {
		return null;
	}

	public InputStream getResourceAsStream(String path) {
		return this.servletContext.getResourceAsStream(path);
	}

	public int getMajorVersion() {
		return 2;
	}

	public int getMinorVersion() {
		return 0;
	}

	public String getMimeType(String file) {
		return this.servletContext.getMimeType(file);
	}

	public String getRealPath(String path) {
		return this.servletContext.getRealPath(path);
	}

	@SuppressWarnings("unchecked")
	public Set<String> getResourcePaths(String path) {
		return this.servletContext.getResourcePaths(path);
	}

	public URL getResource(String path) throws MalformedURLException {
		return this.servletContext.getResource(path);
	}

	public Object getAttribute(String name) {
		return this.servletContext.getAttribute(name);
	}

	@SuppressWarnings("unchecked")
	public Enumeration<String> getAttributeNames() {
		return this.servletContext.getAttributeNames();
	}

	public String getInitParameter(String name) {
		return this.servletContext.getInitParameter(name);
	}

	@SuppressWarnings("unchecked")
	public Enumeration<String> getInitParameterNames() {
		return this.servletContext.getInitParameterNames();
	}

	public void log(String msg) {
		this.servletContext.log(msg);
	}

	public void log(String message, Throwable throwable) {
		this.servletContext.log(message, throwable);
	}

	public void removeAttribute(String name) {
		this.servletContext.removeAttribute(name);
	}

	public void setAttribute(String name, Object object) {
		this.servletContext.setAttribute(name, object);
	}

	public String getPortletContextName() {
		return this.servletContext.getServletContextName();
	}

	public Enumeration<String> getContainerRuntimeOptions() {
		return Collections.enumeration(new HashSet<String>());
	}

}
