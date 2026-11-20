/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.mock.web;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Mock implementation of the {@link jakarta.servlet.ServletConfig} interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 1.0.2
 */
public class MockServletConfig implements ServletConfig {

	private final ServletContext servletContext;

	private final String servletName;

	private final Map<String, String> initParameters = new LinkedHashMap<>();


	/**
	 * Create a new MockServletConfig with a default {@link MockServletContext}.
	 */
	public MockServletConfig() {
		this(null, "");
	}

	/**
	 * Create a new MockServletConfig with a default {@link MockServletContext}.
	 * @param servletName the name of the servlet
	 */
	public MockServletConfig(String servletName) {
		this(null, servletName);
	}

	/**
	 * Create a new MockServletConfig.
	 * @param servletContext the ServletContext that the servlet runs in
	 */
	public MockServletConfig(@Nullable ServletContext servletContext) {
		this(servletContext, "");
	}

	/**
	 * Create a new MockServletConfig.
	 * @param servletContext the ServletContext that the servlet runs in
	 * @param servletName the name of the servlet
	 */
	public MockServletConfig(@Nullable ServletContext servletContext, String servletName) {
		this.servletContext = (servletContext != null ? servletContext : new MockServletContext());
		this.servletName = servletName;
	}


	@Override
	public String getServletName() {
		return this.servletName;
	}

	@Override
	public ServletContext getServletContext() {
		return this.servletContext;
	}

	public void addInitParameter(String name, String value) {
		Assert.notNull(name, "Parameter name must not be null");
		this.initParameters.put(name, value);
	}

	@Override
	public @Nullable String getInitParameter(String name) {
		Assert.notNull(name, "Parameter name must not be null");
		return this.initParameters.get(name);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return Collections.enumeration(this.initParameters.keySet());
	}

}
