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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.async.DeferredResult.DeferredResultHandler;
import org.springframework.web.util.UrlPathHelper;

/**
 * The central class for managing asynchronous request processing, mainly intended
 * as an SPI and not typically used directly by application classes.
 *
 * <p>An async scenario starts with request processing as usual in a thread (T1).
 * Concurrent request handling can be innitiated by calling
 * {@linkplain #startCallableProcessing(Callable, Object...) startCallableProcessing} or
 * {@linkplain #startDeferredResultProcessing(DeferredResult, Object...) startDeferredResultProcessing}
 * both of which produce a result in a separate thread (T2). The result is saved
 * and the request dispatched to the container, to resume processing with the saved
 * result in a third thread (T3). Within the dispatched thread (T3), the saved
 * result can be accessed via {@link #getConcurrentResult()} or its presence
 * detected via {@link #hasConcurrentResult()}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 *
 * @see org.springframework.web.context.request.AsyncWebRequestInterceptor
 * @see org.springframework.web.servlet.AsyncHandlerInterceptor
 * @see org.springframework.web.filter.OncePerRequestFilter#shouldFilterAsyncDispatches
 * @see org.springframework.web.filter.OncePerRequestFilter#isAsyncDispatch
 * @see org.springframework.web.filter.OncePerRequestFilter#isLastRequestThread
 */
public final class WebAsyncManager {

	private static final Object RESULT_NONE = new Object();

	private static final Log logger = LogFactory.getLog(WebAsyncManager.class);

	private static final UrlPathHelper urlPathHelper = new UrlPathHelper();


	private AsyncWebRequest asyncWebRequest;

	private AsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor(this.getClass().getSimpleName());

	private Object concurrentResult = RESULT_NONE;

	private Object[] concurrentResultContext;

	private final Map<Object, CallableProcessingInterceptor> callableInterceptors =
			new LinkedHashMap<Object, CallableProcessingInterceptor>();

	private final Map<Object, DeferredResultProcessingInterceptor> deferredResultInterceptors =
			new LinkedHashMap<Object, DeferredResultProcessingInterceptor>();


	/**
	 * Package private constructor.
	 * @see WebAsyncUtils#getAsyncManager(javax.servlet.ServletRequest)
	 * @see WebAsyncUtils#getAsyncManager(org.springframework.web.context.request.WebRequest)
	 */
	WebAsyncManager() {
	}

	/**
	 * Configure the {@link AsyncWebRequest} to use. This property may be set
	 * more than once during a single request to accurately reflect the current
	 * state of the request (e.g. following a forward, request/response
	 * wrapping, etc). However, it should not be set while concurrent handling
	 * is in progress, i.e. while {@link #isConcurrentHandlingStarted()} is
	 * {@code true}.
	 *
	 * @param asyncWebRequest the web request to use
	 */
	public void setAsyncWebRequest(final AsyncWebRequest asyncWebRequest) {
		Assert.notNull(asyncWebRequest, "AsyncWebRequest must not be null");
		Assert.state(!isConcurrentHandlingStarted(), "Can't set AsyncWebRequest with concurrent handling in progress");
		this.asyncWebRequest = asyncWebRequest;
		this.asyncWebRequest.addCompletionHandler(new Runnable() {
			public void run() {
				asyncWebRequest.removeAttribute(WebAsyncUtils.WEB_ASYNC_MANAGER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
			}
		});
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
	 * Whether the selected handler for the current request chose to handle the
	 * request asynchronously. A return value of "true" indicates concurrent
	 * handling is under way and the response will remain open. A return value
	 * of "false" means concurrent handling was either not started or possibly
	 * that it has completed and the request was dispatched for further
	 * processing of the concurrent result.
	 */
	public boolean isConcurrentHandlingStarted() {
		return ((this.asyncWebRequest != null) && this.asyncWebRequest.isAsyncStarted());
	}

	/**
	 * Whether a result value exists as a result of concurrent handling.
	 */
	public boolean hasConcurrentResult() {
		return (this.concurrentResult != RESULT_NONE);
	}

	/**
	 * Provides access to the result from concurrent handling.
	 *
	 * @return an Object, possibly an {@code Exception} or {@code Throwable} if
	 * concurrent handling raised one.
	 * @see #clearConcurrentResult()
	 */
	public Object getConcurrentResult() {
		return this.concurrentResult;
	}

	/**
	 * Provides access to additional processing context saved at the start of
	 * concurrent handling.
	 *
	 * @see #clearConcurrentResult()
	 */
	public Object[] getConcurrentResultContext() {
		return this.concurrentResultContext;
	}

	public CallableProcessingInterceptor getCallableInterceptor(Object key) {
		return this.callableInterceptors.get(key);
	}

	public DeferredResultProcessingInterceptor getDeferredResultInterceptor(Object key) {
		return this.deferredResultInterceptors.get(key);
	}

	/**
	 * Register a {@link CallableProcessingInterceptor} that will be applied
	 * when concurrent request handling with a {@link Callable} starts.
	 *
	 * @param key a unique the key under which to register the interceptor
	 * @param interceptor the interceptor to register
	 */
	public void registerCallableInterceptor(Object key, CallableProcessingInterceptor interceptor) {
		Assert.notNull(interceptor, "interceptor is required");
		this.callableInterceptors.put(key, interceptor);
	}

	public void registerAllCallableInterceptors(Map<Object, CallableProcessingInterceptor> interceptors) {
		Assert.notNull(interceptors);
		this.callableInterceptors.putAll(interceptors);
	}

	/**
	 * Register a {@link DeferredResultProcessingInterceptor} that will be
	 * applied when concurrent request handling with a {@link DeferredResult}
	 * starts.
	 *
	 * @param key a unique the key under which to register the interceptor
	 * @param interceptor the interceptor to register
	 */
	public void registerDeferredResultInterceptor(Object key, DeferredResultProcessingInterceptor interceptor) {
		Assert.notNull(interceptor, "interceptor is required");
		this.deferredResultInterceptors.put(key, interceptor);
	}

	public void registerAllDeferredResultInterceptors(Map<Object, DeferredResultProcessingInterceptor> interceptors) {
		Assert.notNull(interceptors);
		this.deferredResultInterceptors.putAll(interceptors);
	}

	/**
	 * Clear {@linkplain #getConcurrentResult() concurrentResult} and
	 * {@linkplain #getConcurrentResultContext() concurrentResultContext}.
	 */
	public void clearConcurrentResult() {
		this.concurrentResult = RESULT_NONE;
		this.concurrentResultContext = null;
	}

	/**
	 * Start concurrent request processing and execute the given task with an
	 * {@link #setTaskExecutor(AsyncTaskExecutor) AsyncTaskExecutor}. The result
	 * from the task execution is saved and the request dispatched in order to
	 * resume processing of that result. If the task raises an Exception then
	 * the saved result will be the raised Exception.
	 *
	 * @param callable a unit of work to be executed asynchronously
	 * @param processingContext additional context to save that can be accessed
	 * via {@link #getConcurrentResultContext()}
	 *
	 * @see #getConcurrentResult()
	 * @see #getConcurrentResultContext()
	 */
	public void startCallableProcessing(final Callable<?> callable, Object... processingContext) {
		Assert.notNull(callable, "Callable must not be null");
		Assert.state(this.asyncWebRequest != null, "AsyncWebRequest must not be null");

		final CallableInterceptorChain chain = new CallableInterceptorChain(this.callableInterceptors.values());

		this.asyncWebRequest.setTimeoutHandler(new Runnable() {
			public void run() {
				logger.debug("Processing timeout");
				Object result = chain.triggerAfterTimeout(asyncWebRequest, callable);
				if (result != CallableProcessingInterceptor.RESULT_NONE) {
					setConcurrentResultAndDispatch(result);
				}
			}
		});

		this.asyncWebRequest.addCompletionHandler(new Runnable() {
			public void run() {
				chain.triggerAfterCompletion(asyncWebRequest, callable);
			}
		});

		startAsyncProcessing(processingContext);

		this.taskExecutor.submit(new Runnable() {
			public void run() {
				Object result = null;
				try {
					chain.applyPreProcess(asyncWebRequest, callable);
					result = callable.call();
				}
				catch (Throwable t) {
					result = t;
				}
				finally {
					result = chain.applyPostProcess(asyncWebRequest, callable, result);
				}
				setConcurrentResultAndDispatch(result);
			}
		});
	}

	private void setConcurrentResultAndDispatch(Object result) {
		synchronized (WebAsyncManager.this) {
			if (hasConcurrentResult()) {
				return;
			}
			concurrentResult = result;
		}

		if (asyncWebRequest.isAsyncComplete()) {
			logger.error("Could not complete async processing due to timeout or network error");
			return;
		}

		logger.debug("Concurrent result value [" + concurrentResult + "]");
		logger.debug("Dispatching request to resume processing");

		asyncWebRequest.dispatch();
	}

	/**
	 * Use the given {@link AsyncTask} to configure the task executor as well as
	 * the timeout value of the {@code AsyncWebRequest} before delegating to
	 * {@link #startCallableProcessing(Callable, Object...)}.
	 *
	 * @param asyncTask an asyncTask containing the target {@code Callable}
	 * @param processingContext additional context to save that can be accessed
	 * via {@link #getConcurrentResultContext()}
	 */
	public void startCallableProcessing(AsyncTask<?> asyncTask, Object... processingContext) {
		Assert.notNull(asyncTask, "AsyncTask must not be null");
		Assert.state(this.asyncWebRequest != null, "AsyncWebRequest must not be null");

		Long timeout = asyncTask.getTimeout();
		if (timeout != null) {
			this.asyncWebRequest.setTimeout(timeout);
		}

		AsyncTaskExecutor executor = asyncTask.getExecutor();
		if (executor != null) {
			this.taskExecutor = executor;
		}

		startCallableProcessing(asyncTask.getCallable(), processingContext);
	}

	/**
	 * Start concurrent request processing and initialize the given
	 * {@link DeferredResult} with a {@link DeferredResultHandler} that saves
	 * the result and dispatches the request to resume processing of that
	 * result. The {@code AsyncWebRequest} is also updated with a completion
	 * handler that expires the {@code DeferredResult} and a timeout handler
	 * assuming the {@code DeferredResult} has a default timeout result.
	 *
	 * @param deferredResult the DeferredResult instance to initialize
	 * @param processingContext additional context to save that can be accessed
	 * via {@link #getConcurrentResultContext()}
	 *
	 * @see #getConcurrentResult()
	 * @see #getConcurrentResultContext()
	 */
	public void startDeferredResultProcessing(
			final DeferredResult<?> deferredResult, Object... processingContext) {

		Assert.notNull(deferredResult, "DeferredResult must not be null");
		Assert.state(this.asyncWebRequest != null, "AsyncWebRequest must not be null");

		Long timeout = deferredResult.getTimeoutMilliseconds();
		if (timeout != null) {
			this.asyncWebRequest.setTimeout(timeout);
		}

		final DeferredResultInterceptorChain chain =
				new DeferredResultInterceptorChain(this.deferredResultInterceptors.values());

		this.asyncWebRequest.setTimeoutHandler(new Runnable() {
			public void run() {
				if (!deferredResult.applyTimeoutResult()) {
					try {
						chain.triggerAfterTimeout(asyncWebRequest, deferredResult);
					}
					catch (Throwable t) {
						setConcurrentResultAndDispatch(t);
					}
				}
			}
		});

		this.asyncWebRequest.addCompletionHandler(new Runnable() {
			public void run() {
				deferredResult.expire();
				chain.triggerAfterCompletion(asyncWebRequest, deferredResult);
			}
		});

		startAsyncProcessing(processingContext);

		try {
			chain.applyPreProcess(this.asyncWebRequest, deferredResult);
			deferredResult.setResultHandler(new DeferredResultHandler() {
				public void handleResult(Object result) {
					result = chain.applyPostProcess(asyncWebRequest, deferredResult, result);
					setConcurrentResultAndDispatch(result);
				}
			});
		}
		catch (Throwable t) {
			setConcurrentResultAndDispatch(t);
		}
	}

	private void startAsyncProcessing(Object[] processingContext) {

		clearConcurrentResult();
		this.concurrentResultContext = processingContext;

		this.asyncWebRequest.startAsync();

		if (logger.isDebugEnabled()) {
			HttpServletRequest request = this.asyncWebRequest.getNativeRequest(HttpServletRequest.class);
			String requestUri = urlPathHelper.getRequestUri(request);
			logger.debug("Concurrent handling starting for " + request.getMethod() + " [" + requestUri + "]");
		}
	}

}
