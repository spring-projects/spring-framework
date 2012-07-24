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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.util.Assert;

/**
 * DeferredResult provides an alternative to using a Callable for async request
 * processing. With a Callable the framework manages a thread on behalf of the
 * application through an {@link AsyncTaskExecutor}. With a DeferredResult the
 * application sets the result in a thread of its choice.
 *
 * <p>The following sequence describes the intended use scenario:
 * <ol>
 * <li>thread-1: framework calls application method
 * <li>thread-1: application method returns a DeferredResult
 * <li>thread-1: framework initializes DeferredResult
 * <li>thread-2: application calls {@link #set(Object)}
 * <li>thread-2: framework completes async processing with given result
 * </ol>
 *
 * <p>If the application calls {@link #set(Object)} in thread-2 before the
 * DeferredResult is initialized by the framework in thread-1, then thread-2
 * will block and wait for the initialization to complete. Therefore an
 * application should never create and set the DeferredResult in the same
 * thread because the initialization will never complete.</p>
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public final class DeferredResult<V> {

	private static final Log logger = LogFactory.getLog(DeferredResult.class);

	private V result;

	private DeferredResultHandler resultHandler;

	private final V timeoutValue;

	private final boolean timeoutValueSet;

	private boolean timeoutValueUsed;

	private final CountDownLatch initializationLatch = new CountDownLatch(1);

	private final ReentrantLock setLock = new ReentrantLock();

	/**
	 * Create a new instance.
	 */
	public DeferredResult() {
		this.timeoutValue = null;
		this.timeoutValueSet = false;
	}

	/**
	 * Create a new instance also providing a default value to set if a timeout
	 * occurs before {@link #set(Object)} is called.
	 */
	public DeferredResult(V timeoutValue) {
		this.timeoutValue = timeoutValue;
		this.timeoutValueSet = true;
	}

	/**
	 * Complete async processing with the given value. If the DeferredResult is
	 * not fully initialized yet, this method will block and wait for that to
	 * occur before proceeding. See the class level javadoc for more details.
	 *
	 * @throws StaleAsyncWebRequestException if the underlying async request
	 * has already timed out or ended due to a network error.
	 */
	public void set(V value) throws StaleAsyncWebRequestException {
		if (this.setLock.tryLock() && (!this.timeoutValueUsed)) {
			try {
				handle(value);
			}
			finally {
				this.setLock.unlock();
			}
		}
		else {
			// A timeout is in progress or has already occurred
			throw new StaleAsyncWebRequestException("Async request timed out");
		}
	}

	/**
	 * An alternative to {@link #set(Object)} that absorbs a potential
	 * {@link StaleAsyncWebRequestException}.
	 * @return {@code false} if the outcome was a {@code StaleAsyncWebRequestException}
	 */
	public boolean trySet(V result) throws StaleAsyncWebRequestException {
		try {
			set(result);
			return true;
		}
		catch (StaleAsyncWebRequestException ex) {
			// absorb
		}
		return false;
	}

	private void handle(V result) throws StaleAsyncWebRequestException {
		Assert.isNull(this.result, "A deferred result can be set once only");
		this.result = result;
		this.timeoutValueUsed = (this.timeoutValueSet && (this.result == this.timeoutValue));
		if (!await()) {
			throw new IllegalStateException(
					"Gave up on waiting for DeferredResult to be initialized. " +
					"Are you perhaps creating and setting a DeferredResult in the same thread? " +
					"The DeferredResult must be fully initialized before you can set it. " +
					"See the class javadoc for more details");
		}
		if (this.timeoutValueUsed) {
			logger.debug("Using default timeout value");
		}
		this.resultHandler.handle(result);
	}

	private boolean await() {
		try {
			return this.initializationLatch.await(10, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			return false;
		}
	}

	/**
	 * Return a handler to use to complete processing using the default timeout value
	 * provided via {@link #DeferredResult(Object)} or {@code null} if no timeout
	 * value was provided.
	 */
	Runnable getTimeoutHandler() {
		if (!this.timeoutValueSet) {
			return null;
		}
		return new Runnable() {
			public void run() { useTimeoutValue(); }
		};
	}

	private void useTimeoutValue() {
		this.setLock.lock();
		try {
			if (this.result == null) {
				handle(this.timeoutValue);
				this.timeoutValueUsed = true;
			}
		} finally {
			this.setLock.unlock();
		}
	}

	void init(DeferredResultHandler handler) {
		this.resultHandler = handler;
		this.initializationLatch.countDown();
	}


	/**
	 * Completes processing when {@link DeferredResult#set(Object)} is called.
	 */
	interface DeferredResultHandler {

		void handle(Object result) throws StaleAsyncWebRequestException;
	}

}
