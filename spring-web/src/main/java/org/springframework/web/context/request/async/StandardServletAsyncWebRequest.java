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

package org.springframework.web.context.request.async;

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
import org.springframework.web.context.request.ServletWebRequest;

/**
 * A Servlet 3.0 implementation of {@link AsyncWebRequest}.
 *
 * <p>The servlet and all filters involved in an async request must have async
 * support enabled using the Servlet API or by adding an
 * {@code <async-support>true</async-support>} element to servlet and filter
 * declarations in web.xml
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class StandardServletAsyncWebRequest extends ServletWebRequest implements AsyncWebRequest, AsyncListener {

	private Long timeout;

	private AsyncContext asyncContext;

	private AtomicBoolean asyncCompleted = new AtomicBoolean(false);

	private final List<Runnable> timeoutHandlers = new ArrayList<Runnable>();

	private final List<Runnable> completionHandlers = new ArrayList<Runnable>();

	/**
	 * Define a zero argument constructor.
	 */
	public StandardServletAsyncWebRequest() {
		super();
	}

	/**
	 * Create a new instance for the given request/response pair.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 */
	public StandardServletAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
		super(request, response);
	}

	/**
	 * {@inheritDoc}
	 * <p>In Servlet 3 async processing, the timeout period begins after the
	 * container processing thread has exited.
	 */
	@Override
	public void setTimeout(Long timeout) {
		Assert.state(!isAsyncStarted(), "Cannot change the timeout with concurrent handling in progress");
		this.timeout = timeout;
	}

	@Override
	public void addTimeoutHandler(Runnable timeoutHandler) {
		this.timeoutHandlers.add(timeoutHandler);
	}

	@Override
	public void addCompletionHandler(Runnable runnable) {
		this.completionHandlers.add(runnable);
	}

	@Override
	public boolean isAsyncStarted() {
		return ((this.asyncContext != null) && getRequest().isAsyncStarted());
	}

	/**
	 * Whether async request processing has completed.
	 * <p>It is important to avoid use of request and response objects after async
	 * processing has completed. Servlet containers often re-use them.
	 */
	@Override
	public boolean isAsyncComplete() {
		return this.asyncCompleted.get();
	}

	@Override
	public void startAsync() {
		Assert.state(getRequest().isAsyncSupported(),
				"Async support must be enabled on a servlet and for all filters involved " +
				"in async request processing. This is done in Java code using the Servlet API " +
				"or by adding \"<async-supported>true</async-supported>\" to servlet and " +
				"filter declarations in web.xml.");
		Assert.state(!isAsyncComplete(), "Async processing has already completed");
		if (isAsyncStarted()) {
			return;
		}
		this.asyncContext = getRequest().startAsync(getRequest(), getResponse());
		this.asyncContext.addListener(this);
		if (this.timeout != null) {
			this.asyncContext.setTimeout(this.timeout);
		}
	}

	@Override
	public void dispatch() {
		Assert.notNull(this.asyncContext, "Cannot dispatch without an AsyncContext");
		this.asyncContext.dispatch();
	}

	// ---------------------------------------------------------------------
	// Implementation of AsyncListener methods
	// ---------------------------------------------------------------------

	@Override
	public void onStartAsync(AsyncEvent event) throws IOException {
	}

	@Override
	public void onError(AsyncEvent event) throws IOException {
	}

	@Override
	public void onTimeout(AsyncEvent event) throws IOException {
		for (Runnable handler : this.timeoutHandlers) {
			handler.run();
		}
	}

	@Override
	public void onComplete(AsyncEvent event) throws IOException {
		for (Runnable handler : this.completionHandlers) {
			handler.run();
		}
		this.asyncContext = null;
		this.asyncCompleted.set(true);
	}

}
