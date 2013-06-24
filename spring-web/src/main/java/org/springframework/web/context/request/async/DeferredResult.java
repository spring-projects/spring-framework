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
package org.springframework.web.context.request.async;

import java.util.PriorityQueue;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * {@code DeferredResult} provides an alternative to using a {@link Callable}
 * for asynchronous request processing. While a {@code Callable} is executed
 * concurrently on behalf of the application, with a {@code DeferredResult} the
 * application can produce the result from a thread of its choice.
 *
 * <p>Subclasses can extend this class to easily associate additional data or
 * behavior with the {@link DeferredResult}. For example, one might want to
 * associate the user used to create the {@link DeferredResult} by extending the
 * class and adding an additional property for the user. In this way, the user
 * could easily be accessed later without the need to use a data structure to do
 * the mapping.
 *
 * <p>An example of associating additional behavior to this class might be
 * realized by extending the class to implement an additional interface. For
 * example, one might want to implement {@link Comparable} so that when the
 * {@link DeferredResult} is added to a {@link PriorityQueue} it is handled in
 * the correct order.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 3.2
 */
public class DeferredResult<T> {

	private static final Log logger = LogFactory.getLog(DeferredResult.class);

	private static final Object RESULT_NONE = new Object();


	private final Long timeout;

	private final Object timeoutResult;

	private Runnable timeoutCallback;

	private Runnable completionCallback;

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
	 * Create a DeferredResult with a timeout value.
	 * @param timeout timeout value in milliseconds
	 */
	public DeferredResult(long timeout) {
		this(timeout, RESULT_NONE);
	}

	/**
	 * Create a DeferredResult with a timeout value and a default result to use
	 * in case of timeout.
	 * @param timeout timeout value in milliseconds; ignored if {@code null}
	 * @param timeoutResult the result to use
	 */
	public DeferredResult(Long timeout, Object timeoutResult) {
		this.timeoutResult = timeoutResult;
		this.timeout = timeout;
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
	public final boolean isSetOrExpired() {
		return ((this.result != RESULT_NONE) || this.expired);
	}

	/**
	 * @return {@code true} if the DeferredResult has been set.
	 */
	public boolean hasResult() {
		return this.result != RESULT_NONE;
	}

	/**
	 * @return the result or {@code null} if the result wasn't set; since the result can
	 *         also be {@code null}, it is recommended to use {@link #hasResult()} first
	 *         to check if there is a result prior to calling this method.
	 */
	public Object getResult() {
		return hasResult() ? this.result : null;
	}

	/**
	 * Return the configured timeout value in milliseconds.
	 */
	final Long getTimeoutValue() {
		return this.timeout;
	}

	/**
	 * Register code to invoke when the async request times out. This method is
	 * called from a container thread when an async request times out before the
	 * {@code DeferredResult} has been set. It may invoke
	 * {@link DeferredResult#setResult(Object) setResult} or
	 * {@link DeferredResult#setErrorResult(Object) setErrorResult} to resume
	 * processing.
	 */
	public void onTimeout(Runnable callback) {
		this.timeoutCallback = callback;
	}

	/**
	 * Register code to invoke when the async request completes. This method is
	 * called from a container thread when an async request completed for any
	 * reason including timeout and network error. This method is useful for
	 * detecting that a {@code DeferredResult} instance is no longer usable.
	 */
	public void onCompletion(Runnable callback) {
		this.completionCallback = callback;
	}

	/**
	 * Provide a handler to use to handle the result value.
	 * @param resultHandler the handler
	 * @see DeferredResultProcessingInterceptor
	 */
	public final void setResultHandler(DeferredResultHandler resultHandler) {
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
		}
		if (this.resultHandler != null) {
			this.resultHandler.handleResult(this.result);
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

	final DeferredResultProcessingInterceptor getInterceptor() {
		return new DeferredResultProcessingInterceptorAdapter() {

			@Override
			public <S> boolean handleTimeout(NativeWebRequest request, DeferredResult<S> deferredResult) {
				if (timeoutCallback != null) {
					timeoutCallback.run();
				}
				if (DeferredResult.this.timeoutResult != RESULT_NONE) {
					setResultInternal(timeoutResult);
				}
				return true;
			}

			@Override
			public <S> void afterCompletion(NativeWebRequest request, DeferredResult<S> deferredResult) {
				synchronized (DeferredResult.this) {
					expired = true;
				}
				if (completionCallback != null) {
					completionCallback.run();
				}
			}
		};
	}


	/**
	 * Handles a DeferredResult value when set.
	 */
	public interface DeferredResultHandler {

		void handleResult(Object result);
	}

}
