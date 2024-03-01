/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.context.request.async;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * A Servlet implementation of {@link AsyncWebRequest}.
 *
 * <p>The servlet and all filters involved in an async request must have async
 * support enabled using the Servlet API or by adding an
 * <code>&lt;async-supported&gt;true&lt;/async-supported&gt;</code> element to servlet and filter
 * declarations in {@code web.xml}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class StandardServletAsyncWebRequest extends ServletWebRequest implements AsyncWebRequest, AsyncListener {

	private final List<Runnable> timeoutHandlers = new ArrayList<>();

	private final List<Consumer<Throwable>> exceptionHandlers = new ArrayList<>();

	private final List<Runnable> completionHandlers = new ArrayList<>();

	@Nullable
	private Long timeout;

	@Nullable
	private AsyncContext asyncContext;

	private State state;

	private final ReentrantLock stateLock = new ReentrantLock();


	/**
	 * Create a new instance for the given request/response pair.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 */
	public StandardServletAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
		this(request, response, null);
	}

	/**
	 * Constructor to wrap the request and response for the current dispatch that
	 * also picks up the state of the last (probably the REQUEST) dispatch.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param previousRequest the existing request from the last dispatch
	 * @since 5.3.33
	 */
	StandardServletAsyncWebRequest(HttpServletRequest request, HttpServletResponse response,
			@Nullable StandardServletAsyncWebRequest previousRequest) {

		super(request, new LifecycleHttpServletResponse(response));

		this.state = (previousRequest != null ? previousRequest.state : State.NEW);

		//noinspection DataFlowIssue
		((LifecycleHttpServletResponse) getResponse()).setAsyncWebRequest(this);
	}


	/**
	 * In Servlet 3 async processing, the timeout period begins after the
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
	public void addErrorHandler(Consumer<Throwable> exceptionHandler) {
		this.exceptionHandlers.add(exceptionHandler);
	}

	@Override
	public void addCompletionHandler(Runnable runnable) {
		this.completionHandlers.add(runnable);
	}

	@Override
	public boolean isAsyncStarted() {
		return (this.asyncContext != null && getRequest().isAsyncStarted());
	}

	/**
	 * Whether async request processing has completed.
	 * <p>It is important to avoid use of request and response objects after async
	 * processing has completed. Servlet containers often re-use them.
	 */
	@Override
	public boolean isAsyncComplete() {
		return (this.state == State.COMPLETED);
	}

	@Override
	public void startAsync() {
		Assert.state(getRequest().isAsyncSupported(),
				"Async support must be enabled on a servlet and for all filters involved " +
				"in async request processing. This is done in Java code using the Servlet API " +
				"or by adding \"<async-supported>true</async-supported>\" to servlet and " +
				"filter declarations in web.xml.");

		if (isAsyncStarted()) {
			return;
		}

		if (this.state == State.NEW) {
			this.state = State.ASYNC;
		}
		else {
			Assert.state(this.state == State.ASYNC, "Cannot start async: [" + this.state + "]");
		}

		this.asyncContext = getRequest().startAsync(getRequest(), getResponse());
		this.asyncContext.addListener(this);
		if (this.timeout != null) {
			this.asyncContext.setTimeout(this.timeout);
		}
	}

	@Override
	public void dispatch() {
		Assert.state(this.asyncContext != null, "AsyncContext not yet initialized");
		if (!this.isAsyncComplete()) {
			this.asyncContext.dispatch();
		}
	}


	// ---------------------------------------------------------------------
	// Implementation of AsyncListener methods
	// ---------------------------------------------------------------------

	@Override
	public void onStartAsync(AsyncEvent event) throws IOException {
	}

	@Override
	public void onTimeout(AsyncEvent event) throws IOException {
		this.timeoutHandlers.forEach(Runnable::run);
	}

	@Override
	public void onError(AsyncEvent event) throws IOException {
		this.stateLock.lock();
		try {
			transitionToErrorState();
			Throwable ex = event.getThrowable();
			this.exceptionHandlers.forEach(consumer -> consumer.accept(ex));
		}
		finally {
			this.stateLock.unlock();
		}
	}

	private void transitionToErrorState() {
		if (!isAsyncComplete()) {
			this.state = State.ERROR;
		}
	}

	@Override
	public void onComplete(AsyncEvent event) throws IOException {
		this.stateLock.lock();
		try {
			this.completionHandlers.forEach(Runnable::run);
			this.asyncContext = null;
			this.state = State.COMPLETED;
		}
		finally {
			this.stateLock.unlock();
		}
	}


	/**
	 * Response wrapper to wrap the output stream with {@link LifecycleServletOutputStream}.
	 */
	private static final class LifecycleHttpServletResponse extends HttpServletResponseWrapper {

		@Nullable
		private StandardServletAsyncWebRequest asyncWebRequest;

		@Nullable
		private ServletOutputStream outputStream;

		public LifecycleHttpServletResponse(HttpServletResponse response) {
			super(response);
		}

		public void setAsyncWebRequest(StandardServletAsyncWebRequest asyncWebRequest) {
			this.asyncWebRequest = asyncWebRequest;
		}

		@Override
		public ServletOutputStream getOutputStream() {
			if (this.outputStream == null) {
				Assert.notNull(this.asyncWebRequest, "Not initialized");
				this.outputStream = new LifecycleServletOutputStream(
						(HttpServletResponse) getResponse(), this.asyncWebRequest);
			}
			return this.outputStream;
		}
	}


	/**
	 * Wraps a ServletOutputStream to prevent use after Servlet container onError
	 * notifications, and after async request completion.
	 */
	private static final class LifecycleServletOutputStream extends ServletOutputStream {

		private final HttpServletResponse delegate;

		private final StandardServletAsyncWebRequest asyncWebRequest;

		private LifecycleServletOutputStream(
				HttpServletResponse delegate, StandardServletAsyncWebRequest asyncWebRequest) {

			this.delegate = delegate;
			this.asyncWebRequest = asyncWebRequest;
		}

		@Override
		public boolean isReady() {
			return false;
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void write(int b) throws IOException {
			obtainLockAndCheckState();
			try {
				this.delegate.getOutputStream().write(b);
			}
			catch (IOException ex) {
				handleIOException(ex, "ServletOutputStream failed to write");
			}
			finally {
				releaseLock();
			}
		}

		public void write(byte[] buf, int offset, int len) throws IOException {
			obtainLockAndCheckState();
			try {
				this.delegate.getOutputStream().write(buf, offset, len);
			}
			catch (IOException ex) {
				handleIOException(ex, "ServletOutputStream failed to write");
			}
			finally {
				releaseLock();
			}
		}

		@Override
		public void flush() throws IOException {
			obtainLockAndCheckState();
			try {
				this.delegate.getOutputStream().flush();
			}
			catch (IOException ex) {
				handleIOException(ex, "ServletOutputStream failed to flush");
			}
			finally {
				releaseLock();
			}
		}

		@Override
		public void close() throws IOException {
			obtainLockAndCheckState();
			try {
				this.delegate.getOutputStream().close();
			}
			catch (IOException ex) {
				handleIOException(ex, "ServletOutputStream failed to close");
			}
			finally {
				releaseLock();
			}
		}

		private void obtainLockAndCheckState() throws AsyncRequestNotUsableException {
			if (state() != State.NEW) {
				stateLock().lock();
				if (state() != State.ASYNC) {
					stateLock().unlock();
					throw new AsyncRequestNotUsableException("Response not usable after " +
							(state() == State.COMPLETED ?
									"async request completion" : "onError notification") + ".");
				}
			}
		}

		private void releaseLock() {
			if (state() != State.NEW) {
				stateLock().unlock();
			}
		}

		private State state() {
			return this.asyncWebRequest.state;
		}

		private ReentrantLock stateLock() {
			return this.asyncWebRequest.stateLock;
		}

		private void handleIOException(IOException ex, String msg) throws AsyncRequestNotUsableException {
			this.asyncWebRequest.transitionToErrorState();
			throw new AsyncRequestNotUsableException(msg, ex);
		}

	}


	/**
	 * Represents a state for {@link StandardServletAsyncWebRequest} to be in.
	 * <p><pre>
	 *        NEW
	 *         |
	 *         v
	 *       ASYNC----> +
	 *         |        |
	 *         v        |
	 *       ERROR      |
	 *         |        |
	 *         v        |
	 *     COMPLETED <--+
	 * </pre>
	 * @since 5.3.33
	 */
	private enum State {

		/** New request (thas may not do async handling). */
		NEW,

		/** Async handling has started. */
		ASYNC,

		/** onError notification received, or ServletOutputStream failed. */
		ERROR,

		/** onComplete notification received. */
		COMPLETED

	}

}
