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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Callable;

import javax.servlet.ServletRequest;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.DeferredResult.DeferredResultHandler;

/**
 * The central class for managing async request processing, mainly intended as
 * an SPI and not typically used directly by application classes.
 *
 * <p>An async execution chain consists of a sequence of Callable instances that
 * represent the work required to complete request processing in a separate thread.
 * To construct the chain, each level of the call stack pushes an
 * {@link AbstractDelegatingCallable} during the course of a normal request and
 * pops (removes) it on the way out. If async processing has not started, the pop
 * operation succeeds and the processing continues as normal, or otherwise if async
 * processing has begun, the main processing thread must be exited.
 *
 * <p>For example the DispatcherServlet might contribute a Callable that completes
 * view resolution or the HandlerAdapter might contribute a Callable that prepares a
 * ModelAndView while the last Callable in the chain is usually associated with the
 * application, e.g. the return value of an {@code @RequestMapping} method.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public final class AsyncExecutionChain {

	public static final String CALLABLE_CHAIN_ATTRIBUTE = AsyncExecutionChain.class.getName() + ".CALLABLE_CHAIN";

	private final Deque<AbstractDelegatingCallable> callables = new ArrayDeque<AbstractDelegatingCallable>();

	private Callable<Object> lastCallable;

	private AsyncWebRequest asyncWebRequest;

	private AsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor("MvcAsync");

	/**
	 * Private constructor
	 * @see #getForCurrentRequest()
	 */
	private AsyncExecutionChain() {
	}

	/**
	 * Obtain the AsyncExecutionChain for the current request.
	 * Or if not found, create it and associate it with the request.
	 */
	public static AsyncExecutionChain getForCurrentRequest(ServletRequest request) {
		AsyncExecutionChain chain = (AsyncExecutionChain) request.getAttribute(CALLABLE_CHAIN_ATTRIBUTE);
		if (chain == null) {
			chain = new AsyncExecutionChain();
			request.setAttribute(CALLABLE_CHAIN_ATTRIBUTE, chain);
		}
		return chain;
	}

	/**
	 * Obtain the AsyncExecutionChain for the current request.
	 * Or if not found, create it and associate it with the request.
	 */
	public static AsyncExecutionChain getForCurrentRequest(WebRequest request) {
		int scope = RequestAttributes.SCOPE_REQUEST;
		AsyncExecutionChain chain = (AsyncExecutionChain) request.getAttribute(CALLABLE_CHAIN_ATTRIBUTE, scope);
		if (chain == null) {
			chain = new AsyncExecutionChain();
			request.setAttribute(CALLABLE_CHAIN_ATTRIBUTE, chain, scope);
		}
		return chain;
	}

	/**
	 * Provide an instance of an AsyncWebRequest -- required for async processing.
	 */
	public void setAsyncWebRequest(AsyncWebRequest asyncRequest) {
		Assert.state(!isAsyncStarted(), "Cannot set AsyncWebRequest after the start of async processing.");
		this.asyncWebRequest = asyncRequest;
	}

	/**
	 * Provide an AsyncTaskExecutor for use with {@link #startCallableProcessing()}.
	 * <p>By default a {@link SimpleAsyncTaskExecutor} instance is used. Applications are
	 * advised to provide a TaskExecutor configured for production use.
	 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#setAsyncTaskExecutor
	 */
	public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Push an async Callable for the current stack level. This method should be
	 * invoked before delegating to the next level of the stack where async
	 * processing may start.
	 */
	public void push(AbstractDelegatingCallable callable) {
		Assert.notNull(callable, "Async Callable is required");
		this.callables.addFirst(callable);
	}

	/**
	 * Pop the Callable of the current stack level. Ensure this method is invoked
	 * after delegation to the next level of the stack where async processing may
	 * start. The pop operation succeeds if async processing did not start.
	 * @return {@code true} if the Callable was removed, or {@code false}
	 * 	otherwise (i.e. async started).
	 */
	public boolean pop() {
		if (isAsyncStarted()) {
			return false;
		}
		else {
			this.callables.removeFirst();
			return true;
		}
	}

	/**
	 * Whether async request processing has started.
	 */
	public boolean isAsyncStarted() {
		return ((this.asyncWebRequest != null) && this.asyncWebRequest.isAsyncStarted());
	}

	/**
	 * Set the last Callable, e.g. the one returned by the controller.
	 */
	public AsyncExecutionChain setLastCallable(Callable<Object> callable) {
		Assert.notNull(callable, "Callable required");
		this.lastCallable = callable;
		return this;
	}

	/**
	 * Start async processing and execute the async chain with an AsyncTaskExecutor.
	 * This method returns immediately.
	 */
	public void startCallableProcessing() {
		Assert.state(this.asyncWebRequest != null, "AsyncWebRequest was not set");
		this.asyncWebRequest.startAsync();
		this.taskExecutor.execute(new AsyncExecutionChainRunnable(this.asyncWebRequest, buildChain()));
	}

	private Callable<Object> buildChain() {
		Assert.state(this.lastCallable != null, "The last Callable was not set");
		AbstractDelegatingCallable head = new StaleAsyncRequestCheckingCallable(this.asyncWebRequest);
		head.setNext(this.lastCallable);
		for (AbstractDelegatingCallable callable : this.callables) {
			callable.setNext(head);
			head = callable;
		}
		return head;
	}

	/**
	 * Start async processing and initialize the given DeferredResult so when
	 * its value is set, the async chain is executed with an AsyncTaskExecutor.
	 */
	public void startDeferredResultProcessing(final DeferredResult<?> deferredResult) {
		Assert.notNull(deferredResult, "DeferredResult is required");
		Assert.state(this.asyncWebRequest != null, "AsyncWebRequest was not set");
		this.asyncWebRequest.startAsync();

		deferredResult.init(new DeferredResultHandler() {
			public void handle(Object result) {
				if (asyncWebRequest.isAsyncCompleted()) {
					throw new StaleAsyncWebRequestException("Too late to set DeferredResult: " + result);
				}
				setLastCallable(new PassThroughCallable(result));
				taskExecutor.execute(new AsyncExecutionChainRunnable(asyncWebRequest, buildChain()));
			}
		});

		this.asyncWebRequest.setTimeoutHandler(deferredResult.getTimeoutHandler());
	}


	private static class PassThroughCallable implements Callable<Object> {

		private final Object value;

		public PassThroughCallable(Object value) {
			this.value = value;
		}

		public Object call() throws Exception {
			return this.value;
		}
	}

}
