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

	private Runnable timeoutHandler;

	public StandardServletAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
		super(request, response);
	}

	public void setTimeout(Long timeout) {
		this.timeout = timeout;
	}

	public boolean isAsyncStarted() {
		return ((this.asyncContext != null) && getRequest().isAsyncStarted());
	}

	public boolean isAsyncCompleted() {
		return this.asyncCompleted.get();
	}

	public void setTimeoutHandler(Runnable timeoutHandler) {
		this.timeoutHandler = timeoutHandler;
	}

	public void startAsync() {
		Assert.state(getRequest().isAsyncSupported(),
				"Async support must be enabled on a servlet and for all filters involved " +
				"in async request processing. This is done in Java code using the Servlet API " +
				"or by adding \"<async-supported>true</async-supported>\" to servlet and " +
				"filter declarations in web.xml.");
		Assert.state(!isAsyncStarted(), "Async processing already started");
		Assert.state(!isAsyncCompleted(), "Cannot use async request that has completed");
		this.asyncContext = getRequest().startAsync(getRequest(), getResponse());
		this.asyncContext.addListener(this);
		if (this.timeout != null) {
			this.asyncContext.setTimeout(this.timeout);
		}
	}

	public void complete() {
		if (!isAsyncCompleted()) {
			this.asyncContext.complete();
			completeInternal();
		}
	}

	private void completeInternal() {
		this.asyncContext = null;
		this.asyncCompleted.set(true);
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

	// ---------------------------------------------------------------------
	// Implementation of AsyncListener methods
	// ---------------------------------------------------------------------

	public void onTimeout(AsyncEvent event) throws IOException {
		if (this.timeoutHandler == null) {
			getResponse().sendError(HttpStatus.SERVICE_UNAVAILABLE.value());
		}
		else {
			this.timeoutHandler.run();
		}
		completeInternal();
	}

	public void onError(AsyncEvent event) throws IOException {
		completeInternal();
	}

	public void onStartAsync(AsyncEvent event) throws IOException {
	}

	public void onComplete(AsyncEvent event) throws IOException {
		completeInternal();
	}

}
