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
 * {@code DeferredResult} provides an alternative to returning a {@link Callable}
 * for asynchronous request processing. While with a Callable, a thread is used
 * to execute it on behalf of the application, with a DeferredResult the application
 * sets the result whenever it needs to from a thread of its choice.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public final class DeferredResult<T> {

	private static final Log logger = LogFactory.getLog(DeferredResult.class);

	private static final Object RESULT_NONE = new Object();

	private Object result = RESULT_NONE;

	private final Object timeoutResult;

	private final AtomicBoolean expired = new AtomicBoolean(false);

	private DeferredResultHandler resultHandler;

	private final Object lock = new Object();

	private final CountDownLatch latch = new CountDownLatch(1);


	/**
	 * Create a DeferredResult instance.
	 */
	public DeferredResult() {
		this(RESULT_NONE);
	}

	/**
	 * Create a DeferredResult with a default result to use in case of a timeout.
	 * @param timeoutResult the result to use
	 */
	public DeferredResult(Object timeoutResult) {
		this.timeoutResult = timeoutResult;
	}

	/**
	 * Set a handler to handle the result when set. Normally applications do not
	 * use this method at runtime but may do so during testing.
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

			if (isSetOrExpired()) {
				return false;
			}

			this.result = result;

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
	 * Whether the DeferredResult can no longer be set either because the async
	 * request expired or because it was already set.
	 */
	public boolean isSetOrExpired() {
		return (this.expired.get() || (this.result != RESULT_NONE));
	}

	void setExpired() {
		this.expired.set(true);
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
