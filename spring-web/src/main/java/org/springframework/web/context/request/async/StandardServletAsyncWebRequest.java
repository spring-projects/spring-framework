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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

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
	@SuppressWarnings("NullAway")
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
	public void setTimeout(@Nullable Long timeout) {
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
			this.state = State.ERROR;
			Throwable ex = event.getThrowable();
			this.exceptionHandlers.forEach(consumer -> consumer.accept(ex));
		}
		finally {
			this.stateLock.unlock();
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
	 * Package private access for testing only.
	 */
	ReentrantLock stateLock() {
		return this.stateLock;
	}


	/**
	 * Response wrapper to wrap the output stream with {@link LifecycleServletOutputStream}.
	 * @since 5.3.33
	 */
	private static final class LifecycleHttpServletResponse extends HttpServletResponseWrapper {

		@Nullable
		private StandardServletAsyncWebRequest asyncWebRequest;

		@Nullable
		private ServletOutputStream outputStream;

		@Nullable
		private PrintWriter writer;

		public LifecycleHttpServletResponse(HttpServletResponse response) {
			super(response);
		}

		public void setAsyncWebRequest(StandardServletAsyncWebRequest asyncWebRequest) {
			this.asyncWebRequest = asyncWebRequest;
		}

		@Override
		@SuppressWarnings("NullAway")
		public ServletOutputStream getOutputStream() throws IOException {
			int level = obtainLockAndCheckState();
			try {
				if (this.outputStream == null) {
					Assert.notNull(this.asyncWebRequest, "Not initialized");
					ServletOutputStream delegate = getResponse().getOutputStream();
					this.outputStream = new LifecycleServletOutputStream(delegate, this);
				}
			}
			catch (IOException ex) {
				handleIOException(ex, "Failed to get ServletResponseOutput");
			}
			finally {
				releaseLock(level);
			}
			return this.outputStream;
		}

		@Override
		@SuppressWarnings("NullAway")
		public PrintWriter getWriter() throws IOException {
			int level = obtainLockAndCheckState();
			try {
				if (this.writer == null) {
					Assert.notNull(this.asyncWebRequest, "Not initialized");
					this.writer = new LifecyclePrintWriter(getResponse().getWriter(), this.asyncWebRequest);
				}
			}
			catch (IOException ex) {
				handleIOException(ex, "Failed to get PrintWriter");
			}
			finally {
				releaseLock(level);
			}
			return this.writer;
		}

		@Override
		public void flushBuffer() throws IOException {
			int level = obtainLockAndCheckState();
			try {
				getResponse().flushBuffer();
			}
			catch (IOException ex) {
				handleIOException(ex, "ServletResponse failed to flushBuffer");
			}
			finally {
				releaseLock(level);
			}
		}

		/**
		 * Return 0 if checks passed and lock is not needed, 1 if checks passed
		 * and lock is held, or raise AsyncRequestNotUsableException.
		 */
		private int obtainLockAndCheckState() throws AsyncRequestNotUsableException {
			Assert.notNull(this.asyncWebRequest, "Not initialized");
			if (this.asyncWebRequest.state == State.NEW) {
				return 0;
			}

			this.asyncWebRequest.stateLock.lock();
			if (this.asyncWebRequest.state == State.ASYNC) {
				return 1;
			}

			this.asyncWebRequest.stateLock.unlock();
			throw new AsyncRequestNotUsableException("Response not usable after " +
					(this.asyncWebRequest.state == State.COMPLETED ?
							"async request completion" : "response errors") + ".");
		}

		void handleIOException(IOException ex, String msg) throws AsyncRequestNotUsableException {
			Assert.notNull(this.asyncWebRequest, "Not initialized");
			this.asyncWebRequest.state = State.ERROR;
			throw new AsyncRequestNotUsableException(msg + ": " + ex.getMessage(), ex);
		}

		void releaseLock(int level) {
			Assert.notNull(this.asyncWebRequest, "Not initialized");
			if (level > 0) {
				this.asyncWebRequest.stateLock.unlock();
			}
		}
	}


	/**
	 * Wraps a ServletOutputStream to prevent use after Servlet container onError
	 * notifications, and after async request completion.
	 * @since 5.3.33
	 */
	private static final class LifecycleServletOutputStream extends ServletOutputStream {

		private final ServletOutputStream delegate;

		private final LifecycleHttpServletResponse response;

		private LifecycleServletOutputStream(ServletOutputStream delegate, LifecycleHttpServletResponse response) {
			this.delegate = delegate;
			this.response = response;
		}

		@Override
		public boolean isReady() {
			return this.delegate.isReady();
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
			this.delegate.setWriteListener(writeListener);
		}

		@Override
		public void write(int b) throws IOException {
			int level = this.response.obtainLockAndCheckState();
			try {
				this.delegate.write(b);
			}
			catch (IOException ex) {
				this.response.handleIOException(ex, "ServletOutputStream failed to write");
			}
			finally {
				this.response.releaseLock(level);
			}
		}

		public void write(byte[] buf, int offset, int len) throws IOException {
			int level = this.response.obtainLockAndCheckState();
			try {
				this.delegate.write(buf, offset, len);
			}
			catch (IOException ex) {
				this.response.handleIOException(ex, "ServletOutputStream failed to write");
			}
			finally {
				this.response.releaseLock(level);
			}
		}

		@Override
		public void flush() throws IOException {
			int level = this.response.obtainLockAndCheckState();
			try {
				this.delegate.flush();
			}
			catch (IOException ex) {
				this.response.handleIOException(ex, "ServletOutputStream failed to flush");
			}
			finally {
				this.response.releaseLock(level);
			}
		}

		@Override
		public void close() throws IOException {
			int level = this.response.obtainLockAndCheckState();
			try {
				this.delegate.close();
			}
			catch (IOException ex) {
				this.response.handleIOException(ex, "ServletOutputStream failed to close");
			}
			finally {
				this.response.releaseLock(level);
			}
		}

	}


	/**
	 * Wraps a PrintWriter to prevent use after Servlet container onError
	 * notifications, and after async request completion.
	 * @since 5.3.33
	 */
	private static final class LifecyclePrintWriter extends PrintWriter {

		private final PrintWriter delegate;

		private final StandardServletAsyncWebRequest asyncWebRequest;

		private LifecyclePrintWriter(PrintWriter delegate, StandardServletAsyncWebRequest asyncWebRequest) {
			super(delegate);
			this.delegate = delegate;
			this.asyncWebRequest = asyncWebRequest;
		}

		@Override
		public void flush() {
			int level = tryObtainLockAndCheckState();
			if (level > -1) {
				try {
					this.delegate.flush();
				}
				finally {
					releaseLock(level);
				}
			}
		}

		@Override
		public void close() {
			int level = tryObtainLockAndCheckState();
			if (level > -1) {
				try {
					this.delegate.close();
				}
				finally {
					releaseLock(level);
				}
			}
		}

		@Override
		public boolean checkError() {
			return this.delegate.checkError();
		}

		@Override
		public void write(int c) {
			int level = tryObtainLockAndCheckState();
			if (level > -1) {
				try {
					this.delegate.write(c);
				}
				finally {
					releaseLock(level);
				}
			}
		}

		@Override
		public void write(char[] buf, int off, int len) {
			int level = tryObtainLockAndCheckState();
			if (level > -1) {
				try {
					this.delegate.write(buf, off, len);
				}
				finally {
					releaseLock(level);
				}
			}
		}

		@Override
		public void write(char[] buf) {
			this.delegate.write(buf);
		}

		@Override
		public void write(String s, int off, int len) {
			int level = tryObtainLockAndCheckState();
			if (level > -1) {
				try {
					this.delegate.write(s, off, len);
				}
				finally {
					releaseLock(level);
				}
			}
		}

		@Override
		public void write(String s) {
			this.delegate.write(s);
		}

		/**
		 * Return 0 if checks passed and lock is not needed, 1 if checks passed
		 * and lock is held, and -1 if checks did not pass.
		 */
		private int tryObtainLockAndCheckState() {
			if (this.asyncWebRequest.state == State.NEW) {
				return 0;
			}
			this.asyncWebRequest.stateLock.lock();
			if (this.asyncWebRequest.state == State.ASYNC) {
				return 1;
			}
			this.asyncWebRequest.stateLock.unlock();
			return -1;
		}

		private void releaseLock(int level) {
			if (level > 0) {
				this.asyncWebRequest.stateLock.unlock();
			}
		}

		// Plain delegates

		@Override
		public void print(boolean b) {
			this.delegate.print(b);
		}

		@Override
		public void print(char c) {
			this.delegate.print(c);
		}

		@Override
		public void print(int i) {
			this.delegate.print(i);
		}

		@Override
		public void print(long l) {
			this.delegate.print(l);
		}

		@Override
		public void print(float f) {
			this.delegate.print(f);
		}

		@Override
		public void print(double d) {
			this.delegate.print(d);
		}

		@Override
		public void print(char[] s) {
			this.delegate.print(s);
		}

		@Override
		public void print(String s) {
			this.delegate.print(s);
		}

		@Override
		public void print(Object obj) {
			this.delegate.print(obj);
		}

		@Override
		public void println() {
			this.delegate.println();
		}

		@Override
		public void println(boolean x) {
			this.delegate.println(x);
		}

		@Override
		public void println(char x) {
			this.delegate.println(x);
		}

		@Override
		public void println(int x) {
			this.delegate.println(x);
		}

		@Override
		public void println(long x) {
			this.delegate.println(x);
		}

		@Override
		public void println(float x) {
			this.delegate.println(x);
		}

		@Override
		public void println(double x) {
			this.delegate.println(x);
		}

		@Override
		public void println(char[] x) {
			this.delegate.println(x);
		}

		@Override
		public void println(String x) {
			this.delegate.println(x);
		}

		@Override
		public void println(Object x) {
			this.delegate.println(x);
		}

		@Override
		public PrintWriter printf(String format, Object... args) {
			return this.delegate.printf(format, args);
		}

		@Override
		public PrintWriter printf(Locale l, String format, Object... args) {
			return this.delegate.printf(l, format, args);
		}

		@Override
		public PrintWriter format(String format, Object... args) {
			return this.delegate.format(format, args);
		}

		@Override
		public PrintWriter format(Locale l, String format, Object... args) {
			return this.delegate.format(l, format, args);
		}

		@Override
		public PrintWriter append(CharSequence csq) {
			return this.delegate.append(csq);
		}

		@Override
		public PrintWriter append(CharSequence csq, int start, int end) {
			return this.delegate.append(csq, start, end);
		}

		@Override
		public PrintWriter append(char c) {
			return this.delegate.append(c);
		}
	}


	/**
	 * Represents a state for {@link StandardServletAsyncWebRequest} to be in.
	 * <p><pre>
	 *    +------ NEW
	 *    |        |
	 *    |        v
	 *    |      ASYNC ----> +
	 *    |        |         |
	 *    |        v         |
	 *    +----> ERROR       |
	 *             |         |
	 *             v         |
	 *         COMPLETED <---+
	 * </pre>
	 * @since 5.3.33
	 */
	private enum State {

		/** New request (may not start async handling). */
		NEW,

		/** Async handling has started. */
		ASYNC,

		/** ServletOutputStream failed, or onError notification received. */
		ERROR,

		/** onComplete notification received. */
		COMPLETED

	}

}
