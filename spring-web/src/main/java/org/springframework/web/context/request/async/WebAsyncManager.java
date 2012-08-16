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
 * When a handler decides to handle the request concurrently, it calls
 * {@linkplain #startCallableProcessing(Callable, Object...) startCallableProcessing} or
 * {@linkplain #startDeferredResultProcessing(DeferredResult, Object...) startDeferredResultProcessing}
 * both of which will process in a separate thread (T2).
 * After the start of concurrent handling {@link #isConcurrentHandlingStarted()}
 * returns "true" and this can be used by classes involved in processing on the
 * main thread (T1) quickly and with very minimal processing.
 *
 * <p>When the concurrent handling completes in a separate thread (T2), both
 * {@code startCallableProcessing} and {@code startDeferredResultProcessing}
 * save the results and dispatched to the container, essentially to the
 * same request URI as the one that started concurrent handling. This allows for
 * further processing of the concurrent results. Classes in the dispatched
 * thread (T3), can access the results via {@link #getConcurrentResult()} or
 * detect their presence via {@link #hasConcurrentResult()}. Also in the
 * dispatched thread {@link #isConcurrentHandlingStarted()} will return "false"
 * unless concurrent handling is started once again.
 *
 * TODO .. mention Servlet 3 configuration
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

	private final Map<Object, AsyncThreadInitializer> threadInitializers = new LinkedHashMap<Object, AsyncThreadInitializer>();

	private Object concurrentResult = RESULT_NONE;

	private Object[] concurrentResultContext;

	private static final UrlPathHelper urlPathHelper = new UrlPathHelper();

	/**
	 * Package private constructor
	 * @see AsyncWebUtils
	 */
	WebAsyncManager() {
	}

	/**
	 * Configure an AsyncTaskExecutor for use with {@link #startCallableProcessing(Callable)}.
	 * <p>By default a {@link SimpleAsyncTaskExecutor} instance is used. Applications
	 * are advised to provide a TaskExecutor configured for production use.
	 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#setAsyncTaskExecutor
	 */
	public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Provide an {@link AsyncWebRequest} to use to start and to dispatch request.
	 * This property must be set before the start of concurrent handling.
	 * @param asyncWebRequest the request to use
	 */
	public void setAsyncWebRequest(final AsyncWebRequest asyncWebRequest) {
		Assert.notNull(asyncWebRequest, "Expected AsyncWebRequest");
		Assert.state(!isConcurrentHandlingStarted(), "Can't set AsyncWebRequest with concurrent handling in progress");
		this.asyncWebRequest = asyncWebRequest;
		this.asyncWebRequest.addCompletionHandler(new Runnable() {
			public void run() {
				asyncWebRequest.removeAttribute(AsyncWebUtils.WEB_ASYNC_MANAGER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
			}
		});
	}

	/**
	 * Whether the handler for the current request is executed concurrently.
	 * Once concurrent handling is done, the result is saved, and the request
	 * dispatched again to resume processing where the result of concurrent
	 * handling is available via {@link #getConcurrentResult()}.
	 */
	public boolean isConcurrentHandlingStarted() {
		return ((this.asyncWebRequest != null) && (this.asyncWebRequest.isAsyncStarted()));
	}

	/**
	 * Whether the current thread was dispatched to continue processing the result
	 * of concurrent handler execution.
	 */
	public boolean hasConcurrentResult() {

		// TODO:
		//	Add check for asyncWebRequest.isDispatched() once Apache id=53632 is fixed.
		// 	That ensure "true" is returned in the dispatched thread only.

		return this.concurrentResult != RESULT_NONE;
	}

	/**
	 * Return the result of concurrent handler execution. This may be an Object
	 * value on successful return or an {@code Exception} or {@code Throwable}.
	 */
	public Object getConcurrentResult() {
		return this.concurrentResult;
	}

	/**
	 * Return the processing context saved at the start of concurrent handling.
	 */
	public Object[] getConcurrentResultContext() {
		return this.concurrentResultContext;
	}

	/**
	 * Reset the {@linkplain #getConcurrentResult() concurrentResult} and the
	 * {@linkplain #getConcurrentResultContext() concurrentResultContext}.
	 */
	public void resetConcurrentResult() {
		this.concurrentResult = RESULT_NONE;
		this.concurrentResultContext = null;
	}

	/**
	 * Register an {@link AsyncThreadInitializer} with the WebAsyncManager instance
	 * for the current request. It may later be accessed and applied via
	 * {@link #applyAsyncThreadInitializer(String)} and will also be used to
	 * initialize and reset threads for concurrent handler execution.
	 * @param key a unique the key under which to keep the initializer
	 * @param initializer the initializer  instance
	 */
	public void registerAsyncThreadInitializer(Object key, AsyncThreadInitializer initializer) {
		Assert.notNull(initializer, "An AsyncThreadInitializer instance is required");
		this.threadInitializers.put(key, initializer);
	}

	/**
	 * Invoke the {@linkplain AsyncThreadInitializer#initialize() initialize()}
	 * method of the named {@link AsyncThreadInitializer}.
	 * @param key the key under which the initializer was registered
	 * @return whether an initializer was found and applied
	 */
	public boolean applyAsyncThreadInitializer(Object key) {
		AsyncThreadInitializer initializer = this.threadInitializers.get(key);
		if (initializer != null) {
			initializer.initialize();
			return true;
		}
		return false;
	}

	/**
	 * Submit a request handling task for concurrent execution. Returns immediately
	 * and subsequent calls to {@link #isConcurrentHandlingStarted()} return "true".
	 * <p>When concurrent handling is done, the resulting value, which may be an
	 * Object or a raised {@code Exception} or {@code Throwable}, is saved and the
	 * request is dispatched for further processing of that result. In the dispatched
	 * thread, the result can be accessed via {@link #getConcurrentResult()} while
	 * {@link #hasConcurrentResult()} returns "true" and
	 * {@link #isConcurrentHandlingStarted()} is back to returning "false".
	 *
	 * @param callable a unit of work to be executed asynchronously
	 * @param processingContext additional context to save for later access via
	 * 	{@link #getConcurrentResultContext()}
	 */
	public void startCallableProcessing(final Callable<?> callable, Object... processingContext) {
		Assert.notNull(callable, "Callable is required");

		startAsyncProcessing(processingContext);

		this.taskExecutor.submit(new Runnable() {

			public void run() {
				List<AsyncThreadInitializer> initializers =
						new ArrayList<AsyncThreadInitializer>(threadInitializers.values());

				try {
					for (AsyncThreadInitializer initializer : initializers) {
						initializer.initialize();
					}
					concurrentResult = callable.call();
				}
				catch (Throwable t) {
					concurrentResult = t;
				}
				finally {
					Collections.reverse(initializers);
					for (AsyncThreadInitializer initializer : initializers) {
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
	 * Initialize the given given {@link DeferredResult} so that whenever the
	 * DeferredResult is set, the resulting value, which may be an Object or a
	 * raised {@code Exception} or {@code Throwable}, is saved and the request
	 * is dispatched for further processing of the result. In the dispatch
	 * thread, the result value can be accessed via {@link #getConcurrentResult()}.
	 * <p>The method returns immediately and it's up to the caller to set the
	 * DeferredResult. Subsequent calls to {@link #isConcurrentHandlingStarted()}
	 * return "true" until after the dispatch when {@link #hasConcurrentResult()}
	 * returns "true" and {@link #isConcurrentHandlingStarted()} is back to "false".
	 *
	 * @param deferredResult the DeferredResult instance to initialize
	 * @param processingContext additional context to save for later access via
	 * 	{@link #getConcurrentResultContext()}
	 */
	public void startDeferredResultProcessing(final DeferredResult<?> deferredResult, Object... processingContext) {
		Assert.notNull(deferredResult, "DeferredResult is required");

		startAsyncProcessing(processingContext);

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

	private void startAsyncProcessing(Object... context) {

		Assert.state(this.asyncWebRequest != null, "AsyncWebRequest was not set");
		this.asyncWebRequest.startAsync();

		this.concurrentResult = null;
		this.concurrentResultContext = context;

		if (logger.isDebugEnabled()) {
			HttpServletRequest request = asyncWebRequest.getNativeRequest(HttpServletRequest.class);
			String requestUri = urlPathHelper.getRequestUri(request);
			logger.debug("Concurrent handling starting for " + request.getMethod() + " [" + requestUri + "]");
		}
	}


	/**
	 * A contract for initializing and resetting a thread.
	 */
	public interface AsyncThreadInitializer {

		void initialize();

		void reset();

	}

}
