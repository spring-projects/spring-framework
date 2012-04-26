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
public final class DeferredResult {

	private final static Object TIMEOUT_RESULT_NONE = new Object();

	private Object result;

	private final Object timeoutResult;

	private DeferredResultHandler resultHandler;

	private final CountDownLatch readySignal = new CountDownLatch(1);

	private final ReentrantLock timeoutLock = new ReentrantLock();

	/**
	 * Create a new instance.
	 */
	public DeferredResult() {
		this(TIMEOUT_RESULT_NONE);
	}

	/**
	 * Create a new instance and also provide a default result to use if a
	 * timeout occurs before {@link #set(Object)} is called.
	 */
	public DeferredResult(Object timeoutResult) {
		this.timeoutResult = timeoutResult;
	}

	boolean canHandleTimeout() {
		return this.timeoutResult != TIMEOUT_RESULT_NONE;
	}

	/**
	 * Complete async processing with the given result. If the DeferredResult is
	 * not yet fully initialized, this method will block and wait for that to
	 * occur before proceeding. See the class level javadoc for more details.
	 *
	 * @throws StaleAsyncWebRequestException if the underlying async request
	 * has already timed out or ended due to a network error.
	 */
	public void set(Object result) throws StaleAsyncWebRequestException {
		if (this.timeoutLock.tryLock() && (this.result != this.timeoutResult)) {
			try {
				handle(result);
			}
			finally {
				this.timeoutLock.unlock();
			}
		}
		else {
			// A timeout is in progress
			throw new StaleAsyncWebRequestException("Async request already timed out");
		}
	}

	/**
	 * Invoked to complete async processing when a timeout occurs before
	 * {@link #set(Object)} is called. Or if {@link #set(Object)} is already in
	 * progress, this method blocks, waits for it to complete, and then returns.
	 */
	void handleTimeout() {
		Assert.state(canHandleTimeout(), "Can't handle timeout");
		this.timeoutLock.lock();
		try {
			if (this.result == null) {
				handle(this.timeoutResult);
			}
		}
		finally {
			this.timeoutLock.unlock();
		}
	}

	private void handle(Object result) throws StaleAsyncWebRequestException {
		Assert.isNull(this.result, "A deferred result can be set once only");
		this.result = result;
		try {
			this.readySignal.await(10, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			throw new IllegalStateException(
					"Gave up on waiting for DeferredResult to be initialized. " +
					"Are you perhaps creating and setting a DeferredResult in the same thread? " +
					"The DeferredResult must be fully initialized before you can set it. " +
					"See the class javadoc for more details");
		}
		this.resultHandler.handle(result);
	}

	void init(DeferredResultHandler handler) {
		this.resultHandler = handler;
		this.readySignal.countDown();
	}


	/**
	 * Completes processing when {@link DeferredResult#set(Object)} is called.
	 */
	interface DeferredResultHandler {

		void handle(Object result) throws StaleAsyncWebRequestException;
	}

}
