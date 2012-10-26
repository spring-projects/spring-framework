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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

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


	private final Long timeout;

	private final Object timeoutResult;

	private DeferredResultHandler resultHandler;

	private Object result = RESULT_NONE;

	private boolean expired;


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
	 * @param timeoutResult the result to use
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
	 * Provide a handler to use to handle the result value.
	 * @param resultHandler the handler
	 * @see {@link DeferredResultProcessingInterceptor}
	 */
	public void setResultHandler(DeferredResultHandler resultHandler) {
		Assert.notNull(resultHandler, "DeferredResultHandler is required");
		synchronized (this) {
			this.resultHandler = resultHandler;
			if ((this.result != RESULT_NONE) && (!this.expired)) {
				try {
					this.resultHandler.handleResult(this.result);
				}
				catch (Throwable t) {
					logger.trace("DeferredResult not handled", t);
				}
			}
		}
	}

	/**
	 * Set the value for the DeferredResult and handle it.
	 * @param result the value to set
	 * @return "true" if the result was set and passed on for handling; "false"
	 * if the result was already set or the async request expired.
	 * @see #isSetOrExpired()
	 */
	public boolean setResult(T result) {
		return setResultInternal(result);
	}

	private boolean setResultInternal(Object result) {
		synchronized (this) {
			if (isSetOrExpired()) {
				return false;
			}
			this.result = result;
			if (this.resultHandler != null) {
				this.resultHandler.handleResult(this.result);
			}
		}
		return true;
	}

	/**
	 * Set an error value for the {@link DeferredResult} and handle it. The value
	 * may be an {@link Exception} or {@link Throwable} in which case it will be
	 * processed as if a handler raised the exception.
	 * @param result the error result value
	 * @return "true" if the result was set to the error value and passed on for
	 * handling; "false" if the result was already set or the async request
	 * expired.
	 * @see #isSetOrExpired()
	 */
	public boolean setErrorResult(Object result) {
		return setResultInternal(result);
	}

	/**
	 * Return {@code true} if this DeferredResult is no longer usable either
	 * because it was previously set or because the underlying request expired.
	 * <p>
	 * The result may have been set with a call to {@link #setResult(Object)},
	 * or {@link #setErrorResult(Object)}, or as a result of a timeout, if a
	 * timeout result was provided to the constructor. The request may also
	 * expire due to a timeout or network error.
	 */
	public boolean isSetOrExpired() {
		return ((this.result != RESULT_NONE) || this.expired);
	}

	/**
	 * Mark this instance expired so it may no longer be used.
	 * @return the previous value of the expiration flag
	 */
	boolean expire() {
		synchronized (this) {
			boolean previous = this.expired;
			this.expired = true;
			return previous;
		}
	}

	boolean applyTimeoutResult() {
		return  (this.timeoutResult != RESULT_NONE) ? setResultInternal(this.timeoutResult) : false;
	}


	/**
	 * Handles a DeferredResult value when set.
	 */
	public interface DeferredResultHandler {

		void handleResult(Object result);
	}

}
