/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * {@code DeferredResult} provides an alternative to using a {@link Callable} for
 * asynchronous request processing. While a {@code Callable} is executed concurrently
 * on behalf of the application, with a {@code DeferredResult} the application can
 * produce the result from a thread of its choice.
 *
 * <p>Subclasses can extend this class to easily associate additional data or behavior
 * with the {@link DeferredResult}. For example, one might want to associate the user
 * used to create the {@link DeferredResult} by extending the class and adding an
 * additional property for the user. In this way, the user could easily be accessed
 * later without the need to use a data structure to do the mapping.
 *
 * <p>An example of associating additional behavior to this class might be realized
 * by extending the class to implement an additional interface. For example, one
 * might want to implement {@link Comparable} so that when the {@link DeferredResult}
 * is added to a {@link PriorityQueue} it is handled in the correct order.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Rob Winch
 * @since 3.2
 * @param <T> the result type
 */
public class DeferredResult<T> {

	private static final Object RESULT_NONE = new Object();

	private static final Log logger = LogFactory.getLog(DeferredResult.class);


	@Nullable
	private final Long timeoutValue;

	private final Supplier<?> timeoutResult;

	private Runnable timeoutCallback;

	private Consumer<Throwable> errorCallback;

	private Runnable completionCallback;

	private DeferredResultHandler resultHandler;

	private volatile Object result = RESULT_NONE;

	private volatile boolean expired;


	/**
	 * Create a DeferredResult.
	 */
	public DeferredResult() {
		this(null, () -> RESULT_NONE);
	}

	/**
	 * Create a DeferredResult with a custom timeout value.
	 * <p>By default not set in which case the default configured in the MVC
	 * Java Config or the MVC namespace is used, or if that's not set, then the
	 * timeout depends on the default of the underlying server.
	 * @param timeoutValue timeout value in milliseconds
	 */
	public DeferredResult(Long timeoutValue) {
		this(timeoutValue, () -> RESULT_NONE);
	}

	/**
	 * Create a DeferredResult with a timeout value and a default result to use
	 * in case of timeout.
	 * @param timeoutValue timeout value in milliseconds (ignored if {@code null})
	 * @param timeoutResult the result to use
	 */
	public DeferredResult(@Nullable Long timeoutValue, Object timeoutResult) {
		this.timeoutValue = timeoutValue;
		this.timeoutResult = () -> timeoutResult;
	}

	/**
	 * Variant of {@link #DeferredResult(Long, Object)} that accepts a dynamic
	 * fallback value based on a {@link Supplier}.
	 * @param timeoutValue timeout value in milliseconds (ignored if {@code null})
	 * @param timeoutResult the result supplier to use
	 * @since 5.1.1
	 */
	public DeferredResult(@Nullable Long timeoutValue, Supplier<?> timeoutResult) {
		this.timeoutValue = timeoutValue;
		this.timeoutResult = timeoutResult;
	}


	/**
	 * Return {@code true} if this DeferredResult is no longer usable either
	 * because it was previously set or because the underlying request expired.
	 * <p>The result may have been set with a call to {@link #setResult(Object)},
	 * or {@link #setErrorResult(Object)}, or as a result of a timeout, if a
	 * timeout result was provided to the constructor. The request may also
	 * expire due to a timeout or network error.
	 */
	public final boolean isSetOrExpired() {
		return (this.result != RESULT_NONE || this.expired);
	}

	/**
	 * Return {@code true} if the DeferredResult has been set.
	 * @since 4.0
	 */
	public boolean hasResult() {
		return (this.result != RESULT_NONE);
	}

	/**
	 * Return the result, or {@code null} if the result wasn't set. Since the result
	 * can also be {@code null}, it is recommended to use {@link #hasResult()} first
	 * to check if there is a result prior to calling this method.
	 * @since 4.0
	 */
	@Nullable
	public Object getResult() {
		Object resultToCheck = this.result;
		return (resultToCheck != RESULT_NONE ? resultToCheck : null);
	}

	/**
	 * Return the configured timeout value in milliseconds.
	 */
	@Nullable
	final Long getTimeoutValue() {
		return this.timeoutValue;
	}

	/**
	 * Register code to invoke when the async request times out.
	 * <p>This method is called from a container thread when an async request
	 * times out before the {@code DeferredResult} has been populated.
	 * It may invoke {@link DeferredResult#setResult setResult} or
	 * {@link DeferredResult#setErrorResult setErrorResult} to resume processing.
	 */
	public void onTimeout(Runnable callback) {
		this.timeoutCallback = callback;
	}

	/**
	 * Register code to invoke when an error occurred during the async request.
	 * <p>This method is called from a container thread when an error occurs
	 * while processing an async request before the {@code DeferredResult} has
	 * been populated. It may invoke {@link DeferredResult#setResult setResult}
	 * or {@link DeferredResult#setErrorResult setErrorResult} to resume
	 * processing.
	 * @since 5.0
	 */
	public void onError(Consumer<Throwable> callback) {
		this.errorCallback = callback;
	}

	/**
	 * Register code to invoke when the async request completes.
	 * <p>This method is called from a container thread when an async request
	 * completed for any reason including timeout and network error. This is useful
	 * for detecting that a {@code DeferredResult} instance is no longer usable.
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
		// Immediate expiration check outside of the result lock
		if (this.expired) {
			return;
		}
		Object resultToHandle;
		synchronized (this) {
			// Got the lock in the meantime: double-check expiration status
			if (this.expired) {
				return;
			}
			resultToHandle = this.result;
			if (resultToHandle == RESULT_NONE) {
				// No result yet: store handler for processing once it comes in
				this.resultHandler = resultHandler;
				return;
			}
		}
		// If we get here, we need to process an existing result object immediately.
		// The decision is made within the result lock; just the handle call outside
		// of it, avoiding any deadlock potential with Servlet container locks.
		try {
			resultHandler.handleResult(resultToHandle);
		}
		catch (Throwable ex) {
			logger.debug("Failed to process async result", ex);
		}
	}

	/**
	 * Set the value for the DeferredResult and handle it.
	 * @param result the value to set
	 * @return {@code true} if the result was set and passed on for handling;
	 * {@code false} if the result was already set or the async request expired
	 * @see #isSetOrExpired()
	 */
	public boolean setResult(T result) {
		return setResultInternal(result);
	}

	private boolean setResultInternal(Object result) {
		// Immediate expiration check outside of the result lock
		if (isSetOrExpired()) {
			return false;
		}
		DeferredResultHandler resultHandlerToUse;
		synchronized (this) {
			// Got the lock in the meantime: double-check expiration status
			if (isSetOrExpired()) {
				return false;
			}
			// At this point, we got a new result to process
			this.result = result;
			resultHandlerToUse = this.resultHandler;
			if (resultHandlerToUse == null) {
				// No result handler set yet -> let the setResultHandler implementation
				// pick up the result object and invoke the result handler for it.
				return true;
			}
			// Result handler available -> let's clear the stored reference since
			// we don't need it anymore.
			this.resultHandler = null;
		}
		// If we get here, we need to process an existing result object immediately.
		// The decision is made within the result lock; just the handle call outside
		// of it, avoiding any deadlock potential with Servlet container locks.
		resultHandlerToUse.handleResult(result);
		return true;
	}

	/**
	 * Set an error value for the {@link DeferredResult} and handle it.
	 * The value may be an {@link Exception} or {@link Throwable} in which case
	 * it will be processed as if a handler raised the exception.
	 * @param result the error result value
	 * @return {@code true} if the result was set to the error value and passed on
	 * for handling; {@code false} if the result was already set or the async
	 * request expired
	 * @see #isSetOrExpired()
	 */
	public boolean setErrorResult(Object result) {
		return setResultInternal(result);
	}


	final DeferredResultProcessingInterceptor getInterceptor() {
		return new DeferredResultProcessingInterceptor() {
			@Override
			public <S> boolean handleTimeout(NativeWebRequest request, DeferredResult<S> deferredResult) {
				boolean continueProcessing = true;
				try {
					if (timeoutCallback != null) {
						timeoutCallback.run();
					}
				}
				finally {
					Object value = timeoutResult.get();
					if (value != RESULT_NONE) {
						continueProcessing = false;
						try {
							setResultInternal(value);
						}
						catch (Throwable ex) {
							logger.debug("Failed to handle timeout result", ex);
						}
					}
				}
				return continueProcessing;
			}
			@Override
			public <S> boolean handleError(NativeWebRequest request, DeferredResult<S> deferredResult, Throwable t) {
				try {
					if (errorCallback != null) {
						errorCallback.accept(t);
					}
				}
				finally {
					try {
						setResultInternal(t);
					}
					catch (Throwable ex) {
						logger.debug("Failed to handle error result", ex);
					}
				}
				return false;
			}
			@Override
			public <S> void afterCompletion(NativeWebRequest request, DeferredResult<S> deferredResult) {
				expired = true;
				if (completionCallback != null) {
					completionCallback.run();
				}
			}
		};
	}


	/**
	 * Handles a DeferredResult value when set.
	 */
	@FunctionalInterface
	public interface DeferredResultHandler {

		void handleResult(Object result);
	}

}
