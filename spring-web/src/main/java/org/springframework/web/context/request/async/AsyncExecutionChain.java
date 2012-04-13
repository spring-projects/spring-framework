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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.servlet.ServletRequest;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.web.context.request.async.DeferredResult.DeferredResultHandler;

/**
 * The central class for managing async request processing, mainly intended as
 * an SPI and typically not by non-framework classes.
 *
 * <p>An async execution chain consists of a sequence of Callable instances and
 * represents the work required to complete request processing in a separate
 * thread. To construct the chain, each layer in the call stack of a normal
 * request (e.g. filter, servlet) may contribute an
 * {@link AbstractDelegatingCallable} when a request is being processed.
 * For example the DispatcherServlet might contribute a Callable that
 * performs view resolution while a HandlerAdapter might contribute a Callable
 * that returns the ModelAndView, etc. The last Callable is the one that
 * actually produces an application-specific value, for example the Callable
 * returned by an {@code @RequestMapping} method.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public final class AsyncExecutionChain {

	public static final String CALLABLE_CHAIN_ATTRIBUTE = AsyncExecutionChain.class.getName() + ".CALLABLE_CHAIN";

	private final List<AbstractDelegatingCallable> delegatingCallables = new ArrayList<AbstractDelegatingCallable>();

	private Callable<Object> callable;

	private AsyncWebRequest asyncWebRequest;

	private AsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor("AsyncExecutionChain");

	/**
	 * Private constructor
	 * @see #getForCurrentRequest()
	 */
	private AsyncExecutionChain() {
	}

	/**
	 * Obtain the AsyncExecutionChain for the current request.
	 * Or if not found, create an instance and associate it with the request.
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
	 * Provide an instance of an AsyncWebRequest.
	 * This property must be set before async request processing can begin.
	 */
	public void setAsyncWebRequest(AsyncWebRequest asyncRequest) {
		this.asyncWebRequest = asyncRequest;
	}

	/**
	 * Provide an AsyncTaskExecutor to use when
	 * {@link #startCallableChainProcessing()} is invoked, for example when a
	 * controller method returns a Callable.
	 * <p>By default a {@link SimpleAsyncTaskExecutor} instance is used.
	 */
	public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Whether async request processing has started through one of:
	 * <ul>
	 * 	<li>{@link #startCallableChainProcessing()}
	 * 	<li>{@link #startDeferredResultProcessing(DeferredResult)}
	 * </ul>
	 */
	public boolean isAsyncStarted() {
		return ((this.asyncWebRequest != null) && this.asyncWebRequest.isAsyncStarted());
	}

	/**
	 * Add a Callable with logic required to complete request processing in a
	 * separate thread. See {@link AbstractDelegatingCallable} for details.
	 */
	public void addDelegatingCallable(AbstractDelegatingCallable callable) {
		Assert.notNull(callable, "Callable required");
		this.delegatingCallables.add(callable);
	}

	/**
	 * Add the last Callable, for example the one returned by the controller.
	 * This property must be set prior to invoking
	 * {@link #startCallableChainProcessing()}.
	 */
	public AsyncExecutionChain setCallable(Callable<Object> callable) {
		Assert.notNull(callable, "Callable required");
		this.callable = callable;
		return this;
	}

	/**
	 * Start the async execution chain by submitting an
	 * {@link AsyncExecutionChainRunnable} instance to the TaskExecutor provided via
	 * {@link #setTaskExecutor(AsyncTaskExecutor)} and returning immediately.
	 * @see AsyncExecutionChainRunnable
	 */
	public void startCallableChainProcessing() {
		startAsync();
		this.taskExecutor.execute(new AsyncExecutionChainRunnable(this.asyncWebRequest, buildChain()));
	}

	private void startAsync() {
		Assert.state(this.asyncWebRequest != null, "An AsyncWebRequest is required to start async processing");
		this.asyncWebRequest.startAsync();
	}

	private Callable<Object> buildChain() {
		Assert.state(this.callable != null, "The callable field is required to complete the chain");
		this.delegatingCallables.add(new StaleAsyncRequestCheckingCallable(asyncWebRequest));
		Callable<Object> result = this.callable;
		for (int i = this.delegatingCallables.size() - 1; i >= 0; i--) {
			AbstractDelegatingCallable callable = this.delegatingCallables.get(i);
			callable.setNextCallable(result);
			result = callable;
		}
		return result;
	}

	/**
	 * Mark the start of async request processing accepting the provided
	 * DeferredResult and initializing it such that if
	 * {@link DeferredResult#set(Object)} is called (from another thread),
	 * the set Object value will be processed with the execution chain by
	 * invoking {@link AsyncExecutionChainRunnable}.
	 * <p>The resulting processing from this method is identical to
	 * {@link #startCallableChainProcessing()}. The main difference is in
	 * the threading model, i.e. whether a TaskExecutor is used.
	 * @see DeferredResult
	 */
	public void startDeferredResultProcessing(DeferredResult deferredResult) {
		Assert.notNull(deferredResult, "A DeferredResult is required");
		startAsync();
		deferredResult.setValueProcessor(new DeferredResultHandler() {
			public void handle(Object value) {
				if (asyncWebRequest.isAsyncCompleted()) {
					throw new StaleAsyncWebRequestException("Async request processing already completed");
				}
				setCallable(getSimpleCallable(value));
				new AsyncExecutionChainRunnable(asyncWebRequest, buildChain()).run();
			}
		});
	}

	private Callable<Object> getSimpleCallable(final Object value) {
		return new Callable<Object>() {
			public Object call() throws Exception {
				return value;
			}
		};
	}
}
