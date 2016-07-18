/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.method.HandlerMethod;

/**
 * Extends {@code HandlerInterceptor} with a callback method invoked after the
 * start of asynchronous request handling.
 *
 * <p>When a handler starts an asynchronous request, the {@link DispatcherServlet}
 * exits without invoking {@code postHandle} and {@code afterCompletion} as it
 * normally does for a synchronous request, since the result of request handling
 * (e.g. ModelAndView) is likely not yet ready and will be produced concurrently
 * from another thread. In such scenarios, {@link #afterConcurrentHandlingStarted}
 * is invoked instead, allowing implementations to perform tasks such as cleaning
 * up thread-bound attributes before releasing the thread to the Servlet container.
 *
 * <p>When asynchronous handling completes, the request is dispatched to the
 * container for further processing. At this stage the {@code DispatcherServlet}
 * invokes {@code preHandle}, {@code postHandle}, and {@code afterCompletion}.
 * To distinguish between the initial request and the subsequent dispatch
 * after asynchronous handling completes, interceptors can check whether the
 * {@code javax.servlet.DispatcherType} of {@link javax.servlet.ServletRequest}
 * is {@code "REQUEST"} or {@code "ASYNC"}.
 *
 * <p>Note that {@code HandlerInterceptor} implementations may need to do work
 * when an async request times out or completes with a network error. For such
 * cases the Servlet container does not dispatch and therefore the
 * {@code postHandle} and {@code afterCompletion} methods will not be invoked.
 * Instead, interceptors can register to track an asynchronous request through
 * the {@code registerCallbackInterceptor} and {@code registerDeferredResultInterceptor}
 * methods on {@link org.springframework.web.context.request.async.WebAsyncManager
 * WebAsyncManager}. This can be done proactively on every request from
 * {@code preHandle} regardless of whether async request processing will start.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 * @see org.springframework.web.context.request.async.WebAsyncManager
 * @see org.springframework.web.context.request.async.CallableProcessingInterceptor
 * @see org.springframework.web.context.request.async.DeferredResultProcessingInterceptor
 */
public interface AsyncHandlerInterceptor extends HandlerInterceptor {

	/**
	 * Called instead of {@code postHandle} and {@code afterCompletion}, when
	 * the a handler is being executed concurrently.
	 * <p>Implementations may use the provided request and response but should
	 * avoid modifying them in ways that would conflict with the concurrent
	 * execution of the handler. A typical use of this method would be to
	 * clean up thread-local variables.
	 * 
	 * The default implementation is empty.
	 * 
	 * @param request the current request
	 * @param response the current response
	 * @param handler the handler (or {@link HandlerMethod}) that started async
	 * execution, for type and/or instance examination
	 * @throws Exception in case of errors
	 */
	default void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
	}

}
