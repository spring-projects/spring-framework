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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
 * <p>TODO .. Servlet 3 config
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 *
 * @see org.springframework.web.context.request.async.AsyncWebRequestInterceptor
 * @see org.springframework.web.servlet.AsyncHandlerInterceptor
 * @see org.springframework.web.filter.OncePerRequestFilter#shouldFilterAsyncDispatches
 * @see org.springframework.web.filter.OncePerRequestFilter#isAsyncDispatch
 * @see org.springframework.web.filter.OncePerRequestFilter#isLastRequestThread
 */
public final class WebAsyncManager {

	private static final Object RESULT_NONE = new Object();

	private static final Log logger = LogFactory.getLog(WebAsyncManager.class);


	private AsyncWebRequest asyncWebRequest;

	private AsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor(this.getClass().getSimpleName());

	private Object concurrentResult = RESULT_NONE;

	private Object[] concurrentResultContext;

	private final Map<Object, WebAsyncThreadInitializer> threadInitializers = new LinkedHashMap<Object, WebAsyncThreadInitializer>();

	private static final UrlPathHelper urlPathHelper = new UrlPathHelper();


	/**
	 * Package private constructor.
	 * @see AsyncWebUtils#getAsyncManager(javax.servlet.ServletRequest)
	 * @see AsyncWebUtils#getAsyncManager(org.springframework.web.context.request.WebRequest)
	 */
	WebAsyncManager() {
	}

	/**
	 * Configure the {@link AsyncWebRequest} to use. This property may be
	 * set more than once during a single request to accurately reflect the
	 * current state of the request (e.g. following a forward, request/response
	 * wrapping, etc). However, it should not be set while concurrent handling is
	 * in progress, i.e. while {@link #isConcurrentHandlingStarted()} is {@code true}.
	 * @param asyncWebRequest the web request to use
	 */
	public void setAsyncWebRequest(final AsyncWebRequest asyncWebRequest) {
		Assert.notNull(asyncWebRequest, "AsyncWebRequest must not be null");
		Assert.state(!isConcurrentHandlingStarted(), "Can't set AsyncWebRequest with concurrent handling in progress");
		this.asyncWebRequest = asyncWebRequest;
		this.asyncWebRequest.addCompletionHandler(new Runnable() {
			public void run() {
				asyncWebRequest.removeAttribute(AsyncWebUtils.WEB_ASYNC_MANAGER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
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
	 * Whether the target handler chose to handle the request asynchronously.
	 * A return value of "true" indicates concurrent handling is under way and the
	 * response will remain open. A return value of "false" will be returned again after concurrent
	 * handling produces a result and the request is dispatched to resume processing.
	 */
	public boolean isConcurrentHandlingStarted() {
		return ((this.asyncWebRequest != null) && (this.asyncWebRequest.isAsyncStarted()));
	}

	/**
	 * Whether the request was dispatched to resume processing the result of
	 * concurrent handling.
	 */
	public boolean hasConcurrentResult() {

		// TODO:
		//	Add check for asyncWebRequest.isDispatched() once Apache id=53632 is fixed.
		// 	That ensure "true" is returned in the dispatched thread only.

		return this.concurrentResult != RESULT_NONE;
	}

	/**
	 * Provides access to the result from concurrent handling.
	 * @return an Object, possibly an {@code Exception} or {@code Throwable} if
	 * 	concurrent handling raised one.
	 */
	public Object getConcurrentResult() {
		return this.concurrentResult;
	}

	/**
	 * Provides access to additional processing context saved at the start of
	 * concurrent handling.
	 */
	public Object[] getConcurrentResultContext() {
		return this.concurrentResultContext;
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
	 * 	via {@link #getConcurrentResultContext()}
	 *
	 * @see #getConcurrentResult()
	 * @see #getConcurrentResultContext()
	 */
	public void startCallableProcessing(final Callable<?> callable, Object... processingContext) {
		Assert.notNull(callable, "Callable must not be null");

		startAsyncProcessing(processingContext);

		this.taskExecutor.submit(new Runnable() {

			public void run() {
				List<WebAsyncThreadInitializer> initializers =
						new ArrayList<WebAsyncThreadInitializer>(threadInitializers.values());

				try {
					for (WebAsyncThreadInitializer initializer : initializers) {
						initializer.initialize();
					}
					concurrentResult = callable.call();
				}
				catch (Throwable t) {
					concurrentResult = t;
				}
				finally {
					Collections.reverse(initializers);
					for (WebAsyncThreadInitializer initializer : initializers) {
						initializer.reset();
					}
				}

				if (logger.isDebugEnabled()) {
					logger.debug("Concurrent result value [" + concurrentResult + "]");
				}

				if (asyncWebRequest.isAsyncComplete()) {
					logger.error("Could not complete processing due to a timeout or network error");
					return;
				}

				logger.debug("Dispatching request to continue processing");
				asyncWebRequest.dispatch();
			}
		});
	}

	/**
	 * Use the given {@link AsyncTask} to configure the task executor as well as
	 * the timeout value of the {@code AsyncWebRequest} before delegating to
	 * {@link #startCallableProcessing(Callable, Object...)}.
	 * @param asyncTask an asyncTask containing the target {@code Callable}
	 * @param processingContext additional context to save that can be accessed
	 * 	via {@link #getConcurrentResultContext()}
	 */
	public void startCallableProcessing(AsyncTask asyncTask, Object... processingContext) {
		Assert.notNull(asyncTask, "AsyncTask must not be null");

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
	 * Start concurrent request processing and initialize the given {@link DeferredResult}
	 * with a {@link DeferredResultHandler} that saves the result and dispatches
	 * the request to resume processing of that result.
	 * The {@code AsyncWebRequest} is also updated with a completion handler that
	 * expires the {@code DeferredResult} and a timeout handler assuming the
	 * {@code DeferredResult} has a default timeout result.
	 *
	 * @param deferredResult the DeferredResult instance to initialize
	 * @param processingContext additional context to save that can be accessed
	 * 	via {@link #getConcurrentResultContext()}
	 *
	 * @see #getConcurrentResult()
	 * @see #getConcurrentResultContext()
	 */
	public void startDeferredResultProcessing(final DeferredResult<?> deferredResult, Object... processingContext) {
		Assert.notNull(deferredResult, "DeferredResult must not be null");

		Long timeout = deferredResult.getTimeoutMilliseconds();
		if (timeout != null) {
			this.asyncWebRequest.setTimeout(timeout);
		}

		this.asyncWebRequest.addCompletionHandler(new Runnable() {
			public void run() {
				deferredResult.setExpired();
			}
		});

		if (deferredResult.hasTimeoutResult()) {
			this.asyncWebRequest.setTimeoutHandler(new Runnable() {
				public void run() {
					deferredResult.applyTimeoutResult();
				}
			});
		}

		startAsyncProcessing(processingContext);

		deferredResult.setResultHandler(new DeferredResultHandler() {

			public void handleResult(Object result) {
				concurrentResult = result;
				if (logger.isDebugEnabled()) {
					logger.debug("Deferred result value [" + concurrentResult + "]");
				}

				Assert.state(!asyncWebRequest.isAsyncComplete(),
						"Cannot handle DeferredResult [ " + deferredResult + " ] due to a timeout or network error");

				logger.debug("Dispatching request to complete processing");
				asyncWebRequest.dispatch();
			}
		});
	}

	private void startAsyncProcessing(Object[] processingContext) {

		Assert.state(this.asyncWebRequest != null, "AsyncWebRequest must not be null");
		this.asyncWebRequest.startAsync();

		this.concurrentResult = null;
		this.concurrentResultContext = processingContext;

		if (logger.isDebugEnabled()) {
			HttpServletRequest request = asyncWebRequest.getNativeRequest(HttpServletRequest.class);
			String requestUri = urlPathHelper.getRequestUri(request);
			logger.debug("Concurrent handling starting for " + request.getMethod() + " [" + requestUri + "]");
		}
	}

	/**
	 * Register an {@link WebAsyncThreadInitializer} for the current request. It may
	 * later be accessed and applied via {@link #initializeAsyncThread(String)}
	 * and will also be used to initialize and reset threads for concurrent handler execution.
	 * @param key a unique the key under which to keep the initializer
	 * @param initializer the initializer  instance
	 */
	public void registerAsyncThreadInitializer(Object key, WebAsyncThreadInitializer initializer) {
		Assert.notNull(initializer, "WebAsyncThreadInitializer must not be null");
		this.threadInitializers.put(key, initializer);
	}

	/**
	 * Invoke the {@linkplain WebAsyncThreadInitializer#initialize() initialize()}
	 * method of the named {@link WebAsyncThreadInitializer}.
	 * @param key the key under which the initializer was registered
	 * @return whether an initializer was found and applied
	 */
	public boolean initializeAsyncThread(Object key) {
		WebAsyncThreadInitializer initializer = this.threadInitializers.get(key);
		if (initializer != null) {
			initializer.initialize();
			return true;
		}
		return false;
	}


	/**
	 * Initialize and reset thread-bound variables.
	 */
	public interface WebAsyncThreadInitializer {

		void initialize();

		void reset();

	}

}
