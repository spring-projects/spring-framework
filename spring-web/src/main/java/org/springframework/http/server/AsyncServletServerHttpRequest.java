/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.http.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;


public class AsyncServletServerHttpRequest extends ServletServerHttpRequest
		implements AsyncServerHttpRequest, AsyncListener {

	private Long timeout;

	private AsyncContext asyncContext;

	private AtomicBoolean asyncCompleted = new AtomicBoolean(false);

	private final List<Runnable> timeoutHandlers = new ArrayList<Runnable>();

	private final List<Runnable> completionHandlers = new ArrayList<Runnable>();

	private final HttpServletResponse servletResponse;


	/**
	 * Create a new instance for the given request/response pair.
	 */
	public AsyncServletServerHttpRequest(HttpServletRequest request, HttpServletResponse response) {
		super(request);
		this.servletResponse = response;
	}

	/**
	 * Timeout period begins after the container thread has exited.
	 */
	public void setTimeout(long timeout) {
		Assert.state(!isAsyncStarted(), "Cannot change the timeout with concurrent handling in progress");
		this.timeout = timeout;
	}

	public void addTimeoutHandler(Runnable timeoutHandler) {
		this.timeoutHandlers.add(timeoutHandler);
	}

	public void addCompletionHandler(Runnable runnable) {
		this.completionHandlers.add(runnable);
	}

	public boolean isAsyncStarted() {
		return ((this.asyncContext != null) && getServletRequest().isAsyncStarted());
	}

	/**
	 * Whether async request processing has completed.
	 * <p>It is important to avoid use of request and response objects after async
	 * processing has completed. Servlet containers often re-use them.
	 */
	public boolean isAsyncCompleted() {
		return this.asyncCompleted.get();
	}

	public void startAsync() {
		Assert.state(getServletRequest().isAsyncSupported(),
				"Async support must be enabled on a servlet and for all filters involved " +
				"in async request processing. This is done in Java code using the Servlet API " +
				"or by adding \"<async-supported>true</async-supported>\" to servlet and " +
				"filter declarations in web.xml.");
		Assert.state(!isAsyncCompleted(), "Async processing has already completed");
		if (isAsyncStarted()) {
			return;
		}
		this.asyncContext = getServletRequest().startAsync(getServletRequest(), this.servletResponse);
		this.asyncContext.addListener(this);
		if (this.timeout != null) {
			this.asyncContext.setTimeout(this.timeout);
		}
	}

	public void dispatch() {
		Assert.notNull(this.asyncContext, "Cannot dispatch without an AsyncContext");
		this.asyncContext.dispatch();
	}

	public void completeAsync() {
		Assert.notNull(this.asyncContext, "Cannot dispatch without an AsyncContext");
		if (isAsyncStarted() && !isAsyncCompleted()) {
			this.asyncContext.complete();
		}
	}

	// ---------------------------------------------------------------------
	// Implementation of AsyncListener methods
	// ---------------------------------------------------------------------

	public void onStartAsync(AsyncEvent event) throws IOException {
	}

	public void onError(AsyncEvent event) throws IOException {
	}

	public void onTimeout(AsyncEvent event) throws IOException {
		for (Runnable handler : this.timeoutHandlers) {
			handler.run();
		}
	}

	public void onComplete(AsyncEvent event) throws IOException {
		for (Runnable handler : this.completionHandlers) {
			handler.run();
		}
		this.asyncContext = null;
		this.asyncCompleted.set(true);
	}

}
