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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.util.Assert;

/**
 * DeferredResult provides an alternative to using a Callable to complete async
 * request processing. Whereas with a Callable the framework manages a thread on
 * behalf of the application through an {@link AsyncTaskExecutor}, with a
 * DeferredResult the application can produce a value using a thread of its choice.
 *
 * <p>The following sequence describes typical use of a DeferredResult:
 * <ol>
 * <li>Application method (e.g. controller method) returns a DeferredResult instance
 * <li>The framework completes initialization of the returned DeferredResult in the same thread
 * <li>The application calls {@link DeferredResult#set(Object)} from another thread
 * <li>The framework completes request processing in the thread in which it is invoked
 * </ol>
 *
 * <p><strong>Note:</strong> {@link DeferredResult#set(Object)} will block if
 * called before the DeferredResult is fully initialized (by the framework).
 * Application code should never create a DeferredResult and set it immediately:
 *
 * <pre>
 * DeferredResult value = new DeferredResult();
 * value.set(1);  // blocks
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public final class DeferredResult {

	private final AtomicReference<Object> value = new AtomicReference<Object>();

	private final BlockingQueue<DeferredResultHandler> handlers = new ArrayBlockingQueue<DeferredResultHandler>(1);

	/**
	 * Provide a value to use to complete async request processing.
	 * This method should be invoked only once and usually from a separate
	 * thread to allow the framework to fully initialize the created
	 * DeferrredValue. See the class level documentation for more details.
	 *
	 * @throws StaleAsyncWebRequestException if the underlying async request
	 * ended due to a timeout or an error before the value was set.
	 */
	public void set(Object value) throws StaleAsyncWebRequestException {
		Assert.isNull(this.value.get(), "Value already set");
		this.value.set(value);
		try {
			this.handlers.take().handle(value);
		}
		catch (InterruptedException e) {
			throw new IllegalStateException("Failed to process deferred return value: " + value, e);
		}
	}

	void setValueProcessor(DeferredResultHandler handler) {
		this.handlers.add(handler);
	}


	/**
	 * Puts the set value through processing wiht the async execution chain.
	 */
	interface DeferredResultHandler {

		void handle(Object result) throws StaleAsyncWebRequestException;
	}

}
