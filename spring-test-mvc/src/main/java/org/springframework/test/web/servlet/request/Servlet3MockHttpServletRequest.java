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
package org.springframework.test.web.servlet.request;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * A Servlet 3 sub-class of MockHttpServletRequest.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
class Servlet3MockHttpServletRequest extends MockHttpServletRequest {

	private boolean asyncStarted;

	private MockAsyncContext asyncContext;

	private Map<String, Part> parts = new HashMap<String, Part>();


	public Servlet3MockHttpServletRequest(ServletContext servletContext) {
		super(servletContext);
	}

	@Override
	public boolean isAsyncSupported() {
		return true;
	}

	@Override
	public AsyncContext startAsync() {
		return startAsync(this, null);
	}

	@Override
	public AsyncContext startAsync(ServletRequest request, ServletResponse response) {
		this.asyncStarted = true;
		this.asyncContext = new MockAsyncContext(request, response);
		return this.asyncContext;
	}

	@Override
	public AsyncContext getAsyncContext() {
		return this.asyncContext;
	}

	public void setAsyncContext(MockAsyncContext asyncContext) {
		this.asyncContext = asyncContext;
	}

	@Override
	public DispatcherType getDispatcherType() {
		return DispatcherType.REQUEST;
	}

	@Override
	public boolean isAsyncStarted() {
		return this.asyncStarted;
	}

	@Override
	public void setAsyncStarted(boolean asyncStarted) {
		this.asyncStarted = asyncStarted;
	}

	@Override
	public void addPart(Part part) {
		this.parts.put(part.getName(), part);
	}

	@Override
	public Part getPart(String key) throws IOException, IllegalStateException, ServletException {
		return this.parts.get(key);
	}

	@Override
	public Collection<Part> getParts() throws IOException, IllegalStateException, ServletException {
		return this.parts.values();
	}

	@Override
	public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
		throw new UnsupportedOperationException();
	}

}
