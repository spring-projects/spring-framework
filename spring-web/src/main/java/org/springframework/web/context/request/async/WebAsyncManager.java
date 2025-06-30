/*
 * Copyright 2002-present the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.async.DeferredResult.DeferredResultHandler;

/**
 * The central class for managing asynchronous request processing, mainly intended
 * as an SPI and not typically used directly by application classes.
 *
 * <p>An async scenario starts with request processing as usual in a thread (T1).
 * Concurrent request handling can be initiated by calling
 * {@link #startCallableProcessing(Callable, Object...) startCallableProcessing} or
 * {@link #startDeferredResultProcessing(DeferredResult, Object...) startDeferredResultProcessing},
 * both of which produce a result in a separate thread (T2). The result is saved
 * and the request dispatched to the container, to resume processing with the saved
 * result in a third thread (T3). Within the dispatched thread (T3), the saved
 * result can be accessed via {@link #getConcurrentResult()} or its presence
 * detected via {@link #hasConcurrentResult()}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.2
 * @see org.springframework.web.context.request.AsyncWebRequestInterceptor
 * @see org.springframework.web.servlet.AsyncHandlerInterceptor
 * @see org.springframework.web.filter.OncePerRequestFilter#shouldNotFilterAsyncDispatch
 * @see org.springframework.web.filter.OncePerRequestFilter#isAsyncDispatch
 */
public final class WebAsyncManager {

	private static final Object RESULT_NONE = new Object();

	private static final AsyncTaskExecutor DEFAULT_TASK_EXECUTOR =
			new SimpleAsyncTaskExecutor(WebAsyncManager.class.getSimpleName());

	private static final Log logger = LogFactory.getLog(WebAsyncManager.class);

	private static final CallableProcessingInterceptor timeoutCallableInterceptor =
			new TimeoutCallableProcessingInterceptor();

	private static final DeferredResultProcessingInterceptor timeoutDeferredResultInterceptor =
			new TimeoutDeferredResultProcessingInterceptor();


	private @Nullable AsyncWebRequest asyncWebRequest;

	private AsyncTaskExecutor taskExecutor = DEFAULT_TASK_EXECUTOR;

	private boolean isMultipartRequestParsed;

	private volatile @Nullable Object concurrentResult = RESULT_NONE;

	private volatile Object @Nullable [] concurrentResultContext;

	private final AtomicReference<State> state = new AtomicReference<>(State.NOT_STARTED);

	private final Map<Object, CallableProcessingInterceptor> callableInterceptors = new LinkedHashMap<>();

	private final Map<Object, DeferredResultProcessingInterceptor> deferredResultInterceptors = new LinkedHashMap<>();


	/**
	 * Package-private constructor.
	 * @see WebAsyncUtils#getAsyncManager(jakarta.servlet.ServletRequest)
	 * @see WebAsyncUtils#getAsyncManager(org.springframework.web.context.request.WebRequest)
	 */
	WebAsyncManager() {
	}


	/**
	 * Configure the {@link AsyncWebRequest} to use. This property may be set
	 * more than once during a single request to accurately reflect the current
	 * state of the request (for example, following a forward, request/response
	 * wrapping, etc). However, it should not be set while concurrent handling
	 * is in progress, i.e. while {@link #isConcurrentHandlingStarted()} is
	 * {@code true}.
	 * @param asyncWebRequest the web request to use
	 */
	public void setAsyncWebRequest(AsyncWebRequest asyncWebRequest) {
		Assert.notNull(asyncWebRequest, "AsyncWebRequest must not be null");
		this.asyncWebRequest = asyncWebRequest;
		this.asyncWebRequest.addCompletionHandler(() -> asyncWebRequest.removeAttribute(
				WebAsyncUtils.WEB_ASYNC_MANAGER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST));
	}

	/**
	 * Return the current {@link AsyncWebRequest}.
	 * @since 5.3.33
	 */
	public @Nullable AsyncWebRequest getAsyncWebRequest() {
		return this.asyncWebRequest;
	}

	/**
	 * Configure an AsyncTaskExecutor for use with concurrent processing via
	 * {@link #startCallableProcessing(Callable, Object...)}.
	 * <p>By default a {@link SimpleAsyncTaskExecutor} instance is used.
	 */
	public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Return whether the selected handler for the current request chose to handle
	 * the request asynchronously. A return value of "true" indicates concurrent
	 * handling is under way and the response will remain open. A return value
	 * of "false" means concurrent handling was either not started or possibly
	 * that it has completed and the request was dispatched for further
	 * processing of the concurrent result.
	 */
	public boolean isConcurrentHandlingStarted() {
		return (this.asyncWebRequest != null && this.asyncWebRequest.isAsyncStarted());
	}

	/**
	 * Return whether a result value exists as a result of concurrent handling.
	 */
	public boolean hasConcurrentResult() {
		return (this.concurrentResult != RESULT_NONE);
	}

	/**
	 * Get the result from concurrent handling.
	 * @return an Object, possibly an {@code Exception} or {@code Throwable} if
	 * concurrent handling raised one
	 * @see #clearConcurrentResult()
	 */
	public @Nullable Object getConcurrentResult() {
		return this.concurrentResult;
	}

	/**
	 * Get the additional processing context saved at the start of concurrent handling.
	 * @see #clearConcurrentResult()
	 */
	public Object @Nullable [] getConcurrentResultContext() {
		return this.concurrentResultContext;
	}

	/**
	 * Get the {@link CallableProcessingInterceptor} registered under the given key.
	 * @param key the key
	 * @return the interceptor registered under that key, or {@code null} if none
	 */
	public @Nullable CallableProcessingInterceptor getCallableInterceptor(Object key) {
		return this.callableInterceptors.get(key);
	}

	/**
	 * Get the {@link DeferredResultProcessingInterceptor} registered under the given key.
	 * @param key the key
	 * @return the interceptor registered under that key, or {@code null} if none
	 */
	public @Nullable DeferredResultProcessingInterceptor getDeferredResultInterceptor(Object key) {
		return this.deferredResultInterceptors.get(key);
	}

	/**
	 * Register a {@link CallableProcessingInterceptor} under the given key.
	 * @param key the key
	 * @param interceptor the interceptor to register
	 */
	public void registerCallableInterceptor(Object key, CallableProcessingInterceptor interceptor) {
		Assert.notNull(key, "Key is required");
		Assert.notNull(interceptor, "CallableProcessingInterceptor is required");
		this.callableInterceptors.put(key, interceptor);
	}

	/**
	 * Register a {@link CallableProcessingInterceptor} without a key.
	 * The key is derived from the class name and hash code.
	 * @param interceptors one or more interceptors to register
	 */
	public void registerCallableInterceptors(CallableProcessingInterceptor... interceptors) {
		Assert.notNull(interceptors, "A CallableProcessingInterceptor is required");
		for (CallableProcessingInterceptor interceptor : interceptors) {
			String key = interceptor.getClass().getName() + ":" + interceptor.hashCode();
			this.callableInterceptors.put(key, interceptor);
		}
	}

	/**
	 * Register a {@link DeferredResultProcessingInterceptor} under the given key.
	 * @param key the key
	 * @param interceptor the interceptor to register
	 */
	public void registerDeferredResultInterceptor(Object key, DeferredResultProcessingInterceptor interceptor) {
		Assert.notNull(key, "Key is required");
		Assert.notNull(interceptor, "DeferredResultProcessingInterceptor is required");
		this.deferredResultInterceptors.put(key, interceptor);
	}

	/**
	 * Register one or more {@link DeferredResultProcessingInterceptor DeferredResultProcessingInterceptors}
	 * without a specified key. The default key is derived from the interceptor class name and hash code.
	 * @param interceptors one or more interceptors to register
	 */
	public void registerDeferredResultInterceptors(DeferredResultProcessingInterceptor... interceptors) {
		Assert.notNull(interceptors, "A DeferredResultProcessingInterceptor is required");
		for (DeferredResultProcessingInterceptor interceptor : interceptors) {
			String key = interceptor.getClass().getName() + ":" + interceptor.hashCode();
			this.deferredResultInterceptors.put(key, interceptor);
		}
	}

	/**
	 * Mark the {@link WebAsyncManager} as wrapping a multipart async request.
	 * @since 6.1.12
	 */
	public void setMultipartRequestParsed(boolean isMultipart) {
		this.isMultipartRequestParsed = isMultipart;
	}

	/**
	 * Return {@code true} if this {@link WebAsyncManager} was previously marked
	 * as wrapping a multipart async request, {@code false} otherwise.
	 * @since 6.1.12
	 */
	public boolean isMultipartRequestParsed() {
		return this.isMultipartRequestParsed;
	}

	/**
	 * Clear {@linkplain #getConcurrentResult() concurrentResult} and
	 * {@linkplain #getConcurrentResultContext() concurrentResultContext}.
	 */
	public void clearConcurrentResult() {
		if (!this.state.compareAndSet(State.RESULT_SET, State.NOT_STARTED)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Unexpected call to clear: [" + this.state.get() + "]");
			}
			return;
		}
		synchronized (WebAsyncManager.this) {
			this.concurrentResult = RESULT_NONE;
			this.concurrentResultContext = null;
		}
	}

	/**
	 * Start concurrent request processing and execute the given task with an
	 * {@link #setTaskExecutor(AsyncTaskExecutor) AsyncTaskExecutor}. The result
	 * from the task execution is saved and the request dispatched in order to
	 * resume processing of that result. If the task raises an Exception then
	 * the saved result will be the raised Exception.
	 * @param callable a unit of work to be executed asynchronously
	 * @param processingContext additional context to save that can be accessed
	 * via {@link #getConcurrentResultContext()}
	 * @throws Exception if concurrent processing failed to start
	 * @see #getConcurrentResult()
	 * @see #getConcurrentResultContext()
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void startCallableProcessing(Callable<?> callable, Object... processingContext) throws Exception {
		Assert.notNull(callable, "Callable must not be null");
		startCallableProcessing(new WebAsyncTask(callable), processingContext);
	}

	/**
	 * Use the given {@link WebAsyncTask} to configure the task executor as well as
	 * the timeout value of the {@code AsyncWebRequest} before delegating to
	 * {@link #startCallableProcessing(Callable, Object...)}.
	 * @param webAsyncTask a WebAsyncTask containing the target {@code Callable}
	 * @param processingContext additional context to save that can be accessed
	 * via {@link #getConcurrentResultContext()}
	 * @throws Exception if concurrent processing failed to start
	 */
	@SuppressWarnings("NullAway") // Lambda
	public void startCallableProcessing(final WebAsyncTask<?> webAsyncTask, Object... processingContext)
			throws Exception {

		Assert.notNull(webAsyncTask, "WebAsyncTask must not be null");
		Assert.state(this.asyncWebRequest != null, "AsyncWebRequest must not be null");

		if (!this.state.compareAndSet(State.NOT_STARTED, State.ASYNC_PROCESSING)) {
			throw new IllegalStateException(
					"Unexpected call to startCallableProcessing: [" + this.state.get() + "]");
		}

		Long timeout = webAsyncTask.getTimeout();
		if (timeout != null) {
			this.asyncWebRequest.setTimeout(timeout);
		}

		AsyncTaskExecutor executor = webAsyncTask.getExecutor();
		if (executor != null) {
			this.taskExecutor = executor;
		}

		List<CallableProcessingInterceptor> interceptors = new ArrayList<>();
		interceptors.add(webAsyncTask.getInterceptor());
		interceptors.addAll(this.callableInterceptors.values());
		interceptors.add(timeoutCallableInterceptor);

		final Callable<?> callable = webAsyncTask.getCallable();
		final CallableInterceptorChain interceptorChain = new CallableInterceptorChain(interceptors);

		this.asyncWebRequest.addTimeoutHandler(() -> {
			if (logger.isDebugEnabled()) {
				logger.debug("Servlet container timeout notification for " + formatUri(this.asyncWebRequest));
			}
			Object result = interceptorChain.triggerAfterTimeout(this.asyncWebRequest, callable);
			if (result != CallableProcessingInterceptor.RESULT_NONE) {
				setConcurrentResultAndDispatch(result);
			}
		});

		this.asyncWebRequest.addErrorHandler(ex -> {
			if (logger.isDebugEnabled()) {
				logger.debug("Servlet container error notification for " + formatUri(this.asyncWebRequest) + ": " + ex);
			}
			if (ex instanceof IOException) {
				ex = new AsyncRequestNotUsableException(
						"Servlet container error notification for disconnected client", ex);
			}
			Object result = interceptorChain.triggerAfterError(this.asyncWebRequest, callable, ex);
			result = (result != CallableProcessingInterceptor.RESULT_NONE ? result : ex);
			setConcurrentResultAndDispatch(result);
		});

		this.asyncWebRequest.addCompletionHandler(() ->
				interceptorChain.triggerAfterCompletion(this.asyncWebRequest, callable));

		interceptorChain.applyBeforeConcurrentHandling(this.asyncWebRequest, callable);
		startAsyncProcessing(processingContext);
		try {
			Future<?> future = this.taskExecutor.submit(() -> {
				Object result = null;
				try {
					interceptorChain.applyPreProcess(this.asyncWebRequest, callable);
					result = callable.call();
				}
				catch (Throwable ex) {
					result = ex;
				}
				finally {
					result = interceptorChain.applyPostProcess(this.asyncWebRequest, callable, result);
				}
				setConcurrentResultAndDispatch(result);
			});
			interceptorChain.setTaskFuture(future);
		}
		catch (Throwable ex) {
			Object result = interceptorChain.applyPostProcess(this.asyncWebRequest, callable, ex);
			setConcurrentResultAndDispatch(result);
		}
	}

	/**
	 * Start concurrent request processing and initialize the given
	 * {@link DeferredResult} with a {@link DeferredResultHandler} that saves
	 * the result and dispatches the request to resume processing of that
	 * result. The {@code AsyncWebRequest} is also updated with a completion
	 * handler that expires the {@code DeferredResult} and a timeout handler
	 * assuming the {@code DeferredResult} has a default timeout result.
	 * @param deferredResult the DeferredResult instance to initialize
	 * @param processingContext additional context to save that can be accessed
	 * via {@link #getConcurrentResultContext()}
	 * @throws Exception if concurrent processing failed to start
	 * @see #getConcurrentResult()
	 * @see #getConcurrentResultContext()
	 */
	@SuppressWarnings("NullAway") // Lambda
	public void startDeferredResultProcessing(
			final DeferredResult<?> deferredResult, Object... processingContext) throws Exception {

		Assert.notNull(deferredResult, "DeferredResult must not be null");
		Assert.state(this.asyncWebRequest != null, "AsyncWebRequest must not be null");

		if (!this.state.compareAndSet(State.NOT_STARTED, State.ASYNC_PROCESSING)) {
			throw new IllegalStateException(
					"Unexpected call to startDeferredResultProcessing: [" + this.state.get() + "]");
		}

		Long timeout = deferredResult.getTimeoutValue();
		if (timeout != null) {
			this.asyncWebRequest.setTimeout(timeout);
		}

		List<DeferredResultProcessingInterceptor> interceptors = new ArrayList<>();
		interceptors.add(deferredResult.getLifecycleInterceptor());
		interceptors.addAll(this.deferredResultInterceptors.values());
		interceptors.add(timeoutDeferredResultInterceptor);

		final DeferredResultInterceptorChain interceptorChain = new DeferredResultInterceptorChain(interceptors);

		this.asyncWebRequest.addTimeoutHandler(() -> {
			if (logger.isDebugEnabled()) {
				logger.debug("Servlet container timeout notification for " + formatUri(this.asyncWebRequest));
			}
			try {
				interceptorChain.triggerAfterTimeout(this.asyncWebRequest, deferredResult);
				synchronized (WebAsyncManager.this) {
					// If application thread set the DeferredResult first in a race,
					// we must still not return until setConcurrentResultAndDispatch is done
					return;
				}
			}
			catch (Throwable ex) {
				setConcurrentResultAndDispatch(ex);
			}
		});

		this.asyncWebRequest.addErrorHandler(ex -> {
			if (logger.isDebugEnabled()) {
				logger.debug("Servlet container error notification for " + formatUri(this.asyncWebRequest));
			}
			if (ex instanceof IOException) {
				ex = new AsyncRequestNotUsableException(
						"Servlet container error notification for disconnected client", ex);
			}
			try {
				interceptorChain.triggerAfterError(this.asyncWebRequest, deferredResult, ex);
				synchronized (WebAsyncManager.this) {
					// If application thread set the DeferredResult first in a race,
					// we must still not return until setConcurrentResultAndDispatch is done
					return;
				}
			}
			catch (Throwable interceptorEx) {
				setConcurrentResultAndDispatch(interceptorEx);
			}
		});

		this.asyncWebRequest.addCompletionHandler(() ->
				interceptorChain.triggerAfterCompletion(this.asyncWebRequest, deferredResult));

		interceptorChain.applyBeforeConcurrentHandling(this.asyncWebRequest, deferredResult);
		startAsyncProcessing(processingContext);

		try {
			interceptorChain.applyPreProcess(this.asyncWebRequest, deferredResult);
			deferredResult.setResultHandler(result -> {
				result = interceptorChain.applyPostProcess(this.asyncWebRequest, deferredResult, result);
				setConcurrentResultAndDispatch(result);
			});
		}
		catch (Throwable ex) {
			setConcurrentResultAndDispatch(ex);
		}
	}

	private void startAsyncProcessing(Object[] processingContext) {
		synchronized (WebAsyncManager.this) {
			this.concurrentResult = RESULT_NONE;
			this.concurrentResultContext = processingContext;
		}

		Assert.state(this.asyncWebRequest != null, "AsyncWebRequest must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Started async request for " + formatUri(this.asyncWebRequest));
		}

		this.asyncWebRequest.startAsync();
	}

	private void setConcurrentResultAndDispatch(@Nullable Object result) {
		Assert.state(this.asyncWebRequest != null, "AsyncWebRequest must not be null");
		synchronized (WebAsyncManager.this) {
			if (!this.state.compareAndSet(State.ASYNC_PROCESSING, State.RESULT_SET)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Async result already set: [" + this.state.get() + "], " +
							"ignored result for " + formatUri(this.asyncWebRequest));
				}
				return;
			}

			this.concurrentResult = result;
			if (logger.isDebugEnabled()) {
				logger.debug("Async result set for " + formatUri(this.asyncWebRequest));
			}

			if (this.asyncWebRequest.isAsyncComplete()) {
				if (logger.isDebugEnabled()) {
					logger.debug("Async request already completed for " + formatUri(this.asyncWebRequest));
				}
				return;
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Performing async dispatch for " + formatUri(this.asyncWebRequest));
			}
			this.asyncWebRequest.dispatch();
		}
	}

	private static String formatUri(AsyncWebRequest asyncWebRequest) {
		HttpServletRequest request = asyncWebRequest.getNativeRequest(HttpServletRequest.class);
		return (request != null ? "\"" + request.getRequestURI() + "\"" : "servlet container");
	}


	/**
	 * Represents a state for {@link WebAsyncManager} to be in.
	 * <p><pre>
	 *     +------> NOT_STARTED <------+
	 *     |             |             |
	 *     |             v             |
	 *     |      ASYNC_PROCESSING     |
	 *     |             |             |
	 *     |             v             |
	 *     <-------+ RESULT_SET -------+
	 * </pre>
	 * @since 5.3.33
	 */
	private enum State {

		/** No async processing in progress. */
		NOT_STARTED,

		/** Async handling has started, but the result hasn't been set yet. */
		ASYNC_PROCESSING,

		/** The result is set, and an async dispatch was performed, unless there is a network error. */
		RESULT_SET

	}

}
