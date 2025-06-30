/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.mock.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import jakarta.el.ELContext;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.PageContext;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Mock implementation of the {@link jakarta.servlet.jsp.PageContext} interface.
 * Only necessary for testing applications when testing custom JSP tags.
 *
 * <p>Note: Expects initialization via the constructor rather than via the
 * {@code PageContext.initialize} method. Does not support writing to a
 * JspWriter, request dispatching, or {@code handlePageException} calls.
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 */
public class MockPageContext extends PageContext {

	private final ServletContext servletContext;

	private final HttpServletRequest request;

	private final HttpServletResponse response;

	private final ServletConfig servletConfig;

	private final Map<String, Object> attributes = new LinkedHashMap<>();

	private @Nullable JspWriter out;


	/**
	 * Create new MockPageContext with a default {@link MockServletContext},
	 * {@link MockHttpServletRequest}, {@link MockHttpServletResponse},
	 * {@link MockServletConfig}.
	 */
	public MockPageContext() {
		this(null, null, null, null);
	}

	/**
	 * Create new MockPageContext with a default {@link MockHttpServletRequest},
	 * {@link MockHttpServletResponse}, {@link MockServletConfig}.
	 * @param servletContext the ServletContext that the JSP page runs in
	 * (only necessary when actually accessing the ServletContext)
	 */
	public MockPageContext(@Nullable ServletContext servletContext) {
		this(servletContext, null, null, null);
	}

	/**
	 * Create new MockPageContext with a MockHttpServletResponse,
	 * MockServletConfig.
	 * @param servletContext the ServletContext that the JSP page runs in
	 * @param request the current HttpServletRequest
	 * (only necessary when actually accessing the request)
	 */
	public MockPageContext(@Nullable ServletContext servletContext, @Nullable HttpServletRequest request) {
		this(servletContext, request, null, null);
	}

	/**
	 * Create new MockPageContext with a MockServletConfig.
	 * @param servletContext the ServletContext that the JSP page runs in
	 * @param request the current HttpServletRequest
	 * @param response the current HttpServletResponse
	 * (only necessary when actually writing to the response)
	 */
	public MockPageContext(@Nullable ServletContext servletContext, @Nullable HttpServletRequest request,
			@Nullable HttpServletResponse response) {

		this(servletContext, request, response, null);
	}

	/**
	 * Create new MockServletConfig.
	 * @param servletContext the ServletContext that the JSP page runs in
	 * @param request the current HttpServletRequest
	 * @param response the current HttpServletResponse
	 * @param servletConfig the ServletConfig (hardly ever accessed from within a tag)
	 */
	public MockPageContext(@Nullable ServletContext servletContext, @Nullable HttpServletRequest request,
			@Nullable HttpServletResponse response, @Nullable ServletConfig servletConfig) {

		this.servletContext = (servletContext != null ? servletContext : new MockServletContext());
		this.request = (request != null ? request : new MockHttpServletRequest(servletContext));
		this.response = (response != null ? response : new MockHttpServletResponse());
		this.servletConfig = (servletConfig != null ? servletConfig : new MockServletConfig(servletContext));
	}


	@Override
	public void initialize(
			Servlet servlet, ServletRequest request, ServletResponse response,
			String errorPageURL, boolean needsSession, int bufferSize, boolean autoFlush) {

		throw new UnsupportedOperationException("Use appropriate constructor");
	}

	@Override
	public void release() {
	}

	@Override
	public void setAttribute(String name, @Nullable Object value) {
		Assert.notNull(name, "Attribute name must not be null");
		if (value != null) {
			this.attributes.put(name, value);
		}
		else {
			this.attributes.remove(name);
		}
	}

	@Override
	public void setAttribute(String name, @Nullable Object value, int scope) {
		Assert.notNull(name, "Attribute name must not be null");
		switch (scope) {
			case PAGE_SCOPE -> setAttribute(name, value);
			case REQUEST_SCOPE -> this.request.setAttribute(name, value);
			case SESSION_SCOPE -> this.request.getSession().setAttribute(name, value);
			case APPLICATION_SCOPE -> this.servletContext.setAttribute(name, value);
			default -> throw new IllegalArgumentException("Invalid scope: " + scope);
		}
	}

	@Override
	public @Nullable Object getAttribute(String name) {
		Assert.notNull(name, "Attribute name must not be null");
		return this.attributes.get(name);
	}

	@Override
	public @Nullable Object getAttribute(String name, int scope) {
		Assert.notNull(name, "Attribute name must not be null");
		return switch (scope) {
			case PAGE_SCOPE -> getAttribute(name);
			case REQUEST_SCOPE -> this.request.getAttribute(name);
			case SESSION_SCOPE -> {
				HttpSession session = this.request.getSession(false);
				yield (session != null ? session.getAttribute(name) : null);
			}
			case APPLICATION_SCOPE -> this.servletContext.getAttribute(name);
			default -> throw new IllegalArgumentException("Invalid scope: " + scope);
		};
	}

	@Override
	public @Nullable Object findAttribute(String name) {
		Object value = getAttribute(name);
		if (value == null) {
			value = getAttribute(name, REQUEST_SCOPE);
			if (value == null) {
				value = getAttribute(name, SESSION_SCOPE);
				if (value == null) {
					value = getAttribute(name, APPLICATION_SCOPE);
				}
			}
		}
		return value;
	}

	@Override
	public void removeAttribute(String name) {
		Assert.notNull(name, "Attribute name must not be null");
		this.removeAttribute(name, PageContext.PAGE_SCOPE);
		this.removeAttribute(name, PageContext.REQUEST_SCOPE);
		this.removeAttribute(name, PageContext.SESSION_SCOPE);
		this.removeAttribute(name, PageContext.APPLICATION_SCOPE);
	}

	@Override
	public void removeAttribute(String name, int scope) {
		Assert.notNull(name, "Attribute name must not be null");
		switch (scope) {
			case PAGE_SCOPE -> this.attributes.remove(name);
			case REQUEST_SCOPE -> this.request.removeAttribute(name);
			case SESSION_SCOPE -> this.request.getSession().removeAttribute(name);
			case APPLICATION_SCOPE -> this.servletContext.removeAttribute(name);
			default -> throw new IllegalArgumentException("Invalid scope: " + scope);
		}
	}

	@Override
	public int getAttributesScope(String name) {
		if (getAttribute(name) != null) {
			return PAGE_SCOPE;
		}
		else if (getAttribute(name, REQUEST_SCOPE) != null) {
			return REQUEST_SCOPE;
		}
		else if (getAttribute(name, SESSION_SCOPE) != null) {
			return SESSION_SCOPE;
		}
		else if (getAttribute(name, APPLICATION_SCOPE) != null) {
			return APPLICATION_SCOPE;
		}
		else {
			return 0;
		}
	}

	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(new LinkedHashSet<>(this.attributes.keySet()));
	}

	@Override
	public Enumeration<String> getAttributeNamesInScope(int scope) {
		return switch (scope) {
			case PAGE_SCOPE -> getAttributeNames();
			case REQUEST_SCOPE -> this.request.getAttributeNames();
			case SESSION_SCOPE -> {
				HttpSession session = this.request.getSession(false);
				yield (session != null ? session.getAttributeNames() : Collections.emptyEnumeration());
			}
			case APPLICATION_SCOPE -> this.servletContext.getAttributeNames();
			default -> throw new IllegalArgumentException("Invalid scope: " + scope);
		};
	}

	@Override
	public JspWriter getOut() {
		if (this.out == null) {
			this.out = new MockJspWriter(this.response);
		}
		return this.out;
	}

	@Override
	public @Nullable ELContext getELContext() {
		return null;
	}

	@Override
	public HttpSession getSession() {
		return this.request.getSession();
	}

	@Override
	public Object getPage() {
		return this;
	}

	@Override
	public ServletRequest getRequest() {
		return this.request;
	}

	@Override
	public ServletResponse getResponse() {
		return this.response;
	}

	@Override
	public @Nullable Exception getException() {
		return null;
	}

	@Override
	public ServletConfig getServletConfig() {
		return this.servletConfig;
	}

	@Override
	public ServletContext getServletContext() {
		return this.servletContext;
	}

	@Override
	public void forward(String path) throws ServletException, IOException {
		this.request.getRequestDispatcher(path).forward(this.request, this.response);
	}

	@Override
	public void include(String path) throws ServletException, IOException {
		this.request.getRequestDispatcher(path).include(this.request, this.response);
	}

	@Override
	public void include(String path, boolean flush) throws ServletException, IOException {
		this.request.getRequestDispatcher(path).include(this.request, this.response);
		if (flush) {
			this.response.flushBuffer();
		}
	}

	public byte[] getContentAsByteArray() {
		if (this.response instanceof MockHttpServletResponse mockResponse) {
			return mockResponse.getContentAsByteArray();
		}
		throw new IllegalStateException("MockHttpServletResponse is required");
	}

	public String getContentAsString() throws UnsupportedEncodingException {
		if (this.response instanceof MockHttpServletResponse mockResponse) {
			return mockResponse.getContentAsString();
		}
		throw new IllegalStateException("MockHttpServletResponse is required");
	}

	@Override
	public void handlePageException(Exception ex) throws ServletException, IOException {
		throw new ServletException("Page exception", ex);
	}

	@Override
	public void handlePageException(Throwable ex) throws ServletException, IOException {
		throw new ServletException("Page exception", ex);
	}

}
