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
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * A Servlet 3.0 implementation of {@link AsyncWebRequest}.
 *
 * <p>The servlet processing an async request as well as all filters involved
 * must async support enabled. This can be done in Java using the Servlet API
 * or by adding an {@code <async-support>true</async-support>} element to
 * servlet and filter declarations in web.xml
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class StandardServletAsyncWebRequest extends ServletWebRequest implements AsyncWebRequest, AsyncListener {

	private Long timeout;

	private AsyncContext asyncContext;

	private AtomicBoolean asyncCompleted = new AtomicBoolean(false);

	public StandardServletAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
		super(request, response);
	}

	public void setTimeout(Long timeout) {
		this.timeout = timeout;
	}

	public boolean isAsyncStarted() {
		assertNotStale();
		return ((this.asyncContext != null) && getRequest().isAsyncStarted());
	}

	public boolean isAsyncCompleted() {
		return this.asyncCompleted.get();
	}

	public void startAsync() {
		Assert.state(getRequest().isAsyncSupported(),
				"Async support must be enabled on a servlet and for all filters involved " +
				"in async request processing. This is done in Java code using the Servlet API " +
				"or by adding \"<async-supported>true</async-supported>\" to servlet and " +
				"filter declarations in web.xml.");
		assertNotStale();
		Assert.state(!isAsyncStarted(), "Async processing already started");
		this.asyncContext = getRequest().startAsync(getRequest(), getResponse());
		this.asyncContext.addListener(this);
		if (this.timeout != null) {
			this.asyncContext.setTimeout(this.timeout);
		}
	}

	public void complete() {
		assertNotStale();
		if (!isAsyncCompleted()) {
			this.asyncContext.complete();
		}
	}

	public void sendError(HttpStatus status, String message) {
		try {
			if (!isAsyncCompleted()) {
				getResponse().sendError(500, message);
			}
		}
		catch (IOException ioEx) {
			// absorb
		}
	}

	private void assertNotStale() {
		Assert.state(!isAsyncCompleted(), "Cannot use async request after completion");
	}

	// ---------------------------------------------------------------------
	// Implementation of AsyncListener methods
	// ---------------------------------------------------------------------

	public void onTimeout(AsyncEvent event) throws IOException {
		this.asyncCompleted.set(true);
	}

	public void onError(AsyncEvent event) throws IOException {
		this.asyncCompleted.set(true);
	}

	public void onStartAsync(AsyncEvent event) throws IOException {
	}

	public void onComplete(AsyncEvent event) throws IOException {
		this.asyncCompleted.set(true);
	}

}
