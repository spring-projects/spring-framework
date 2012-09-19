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

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * {@code DeferredResult} provides an alternative to using a {@link Callable}
 * for asynchronous request processing. While a {@code Callable} is executed
 * concurrently on behalf of the application, with a {@code DeferredResult} the
 * application can produce the result from a thread of its choice.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public final class DeferredResult<T> {

	private static final Log logger = LogFactory.getLog(DeferredResult.class);

	private static final Object RESULT_NONE = new Object();


	private final Object timeoutResult;

	private final Long timeout;

	private DeferredResultHandler resultHandler;

	private final AtomicBoolean expired = new AtomicBoolean(false);

	private final Object lock = new Object();

	private final CountDownLatch latch = new CountDownLatch(1);


	/**
	 * Create a DeferredResult.
	 */
	public DeferredResult() {
		this(null, RESULT_NONE);
	}

	/**
	 * Create a DeferredResult with a timeout.
	 * @param timeout timeout value in milliseconds
	 */
	public DeferredResult(long timeout) {
		this(timeout, RESULT_NONE);
	}

	/**
	 * Create a DeferredResult with a timeout and a default result to use on timeout.
	 * @param timeout timeout value in milliseconds; ignored if {@code null}
	 * @param timeoutResult the result to use, possibly {@code null}
	 */
	public DeferredResult(Long timeout, Object timeoutResult) {
		this.timeoutResult = timeoutResult;
		this.timeout = timeout;
	}

	/**
	 * Return the configured timeout value in milliseconds.
	 */
	public Long getTimeoutMilliseconds() {
		return this.timeout;
	}

	/**
	 * Set a handler to handle the result when set. There can be only handler
	 * for a {@code DeferredResult}. At runtime it will be set by the framework.
	 * However applications may set it when unit testing.
	 *
	 * <p>If you need to be called back when a {@code DeferredResult} is set or
	 * expires, register a {@link DeferredResultProcessingInterceptor} instead.
	 */
	public void setResultHandler(DeferredResultHandler resultHandler) {
		this.resultHandler = resultHandler;
		this.latch.countDown();
	}

	/**
	 * Set the result value and pass it on for handling.
	 * @param result the result value
	 * @return "true" if the result was set and passed on for handling;
	 * 	"false" if the result was already set or the async request expired.
	 * @see #isSetOrExpired()
	 */
	public boolean setResult(T result) {
		return processResult(result);
	}

	/**
	 * Set an error result value and pass it on for handling. If the result is an
	 * {@link Exception} or {@link Throwable}, it will be processed as though the
	 * controller raised the exception. Otherwise it will be processed as if the
	 * controller returned the given result.
	 * @param result the error result value
	 * @return "true" if the result was set to the error value and passed on for handling;
	 * 	"false" if the result was already set or the async request expired.
	 * @see #isSetOrExpired()
	 */
	public boolean setErrorResult(Object result) {
		return processResult(result);
	}

	private boolean processResult(Object result) {

		synchronized (this.lock) {

			boolean wasExpired = getAndSetExpired();
			if (wasExpired) {
				return false;
			}

			if (!awaitResultHandler()) {
				throw new IllegalStateException("DeferredResultHandler not set");
			}

			try {
				this.resultHandler.handleResult(result);
			}
			catch (Throwable t) {
				logger.trace("DeferredResult not handled", t);
				return false;
			}

			return true;
		}
	}

	private boolean awaitResultHandler() {
		try {
			return this.latch.await(5, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			return false;
		}
	}

	/**
	 * Return {@code true} if this DeferredResult is no longer usable either
	 * because it was previously set or because the underlying request ended
	 * before it could be set.
	 * <p>
	 * The result may have been set with a call to {@link #setResult(Object)},
	 * or {@link #setErrorResult(Object)}, or following a timeout, assuming a
	 * timeout result was provided to the constructor. The request may before
	 * the result set due to a timeout or network error.
	 */
	public boolean isSetOrExpired() {
		return this.expired.get();
	}

	/**
	 * Atomically set the expired flag and return its previous value.
	 */
	boolean getAndSetExpired() {
		return this.expired.getAndSet(true);
	}

	boolean hasTimeoutResult() {
		return this.timeoutResult != RESULT_NONE;
	}

	boolean applyTimeoutResult() {
		return  hasTimeoutResult() ? processResult(this.timeoutResult) : false;
	}


	/**
	 * Handles a DeferredResult value when set.
	 */
	public interface DeferredResultHandler {

		void handleResult(Object result);
	}

}
