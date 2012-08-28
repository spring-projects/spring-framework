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
package org.springframework.mock.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.beans.BeanUtils;
import org.springframework.web.util.WebUtils;

/**
 * Mock implementation of the {@link AsyncContext} interface.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class MockAsyncContext implements AsyncContext {

	private final ServletRequest request;

	private final ServletResponse response;

	private final MockHttpServletRequest mockRequest;

	private final List<AsyncListener> listeners = new ArrayList<AsyncListener>();

	private String dispatchPath;

	private long timeout = 10 * 60 * 1000L;

	public MockAsyncContext(ServletRequest request, ServletResponse response) {
		this.request = request;
		this.response = response;
		this.mockRequest = WebUtils.getNativeRequest(request, MockHttpServletRequest.class);
	}

	public ServletRequest getRequest() {
		return this.request;
	}

	public ServletResponse getResponse() {
		return this.response;
	}

	public boolean hasOriginalRequestAndResponse() {
		return false;
	}

	public String getDispatchPath() {
		return this.dispatchPath;
	}

	public void dispatch() {
		dispatch(null);
 	}

	public void dispatch(String path) {
		dispatch(null, path);
	}

	public void dispatch(ServletContext context, String path) {
		this.dispatchPath = path;
		if (this.mockRequest != null) {
			this.mockRequest.setDispatcherType(DispatcherType.ASYNC);
			this.mockRequest.setAsyncStarted(false);
		}
	}

	public void complete() {
		if (this.mockRequest != null) {
			this.mockRequest.setAsyncStarted(false);
		}
		for (AsyncListener listener : this.listeners) {
			try {
				listener.onComplete(new AsyncEvent(this, this.request, this.response));
			}
			catch (IOException e) {
				throw new IllegalStateException("AsyncListener failure", e);
			}
		}
	}

	public void start(Runnable run) {
	}

	public List<AsyncListener> getListeners() {
		return this.listeners;
	}

	public void addListener(AsyncListener listener) {
		this.listeners.add(listener);
	}

	public void addListener(AsyncListener listener, ServletRequest request, ServletResponse response) {
		this.listeners.add(listener);
	}

	public <T extends AsyncListener> T createListener(Class<T> clazz) throws ServletException {
		return BeanUtils.instantiateClass(clazz);
	}

	public long getTimeout() {
		return this.timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

}
