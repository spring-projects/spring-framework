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
 * Extends {@code HandlerInterceptor} with a callback method invoked during
 * asynchronous request handling.
 *
 * <p>When a handler starts asynchronous request handling, the DispatcherServlet
 * exits without invoking {@code postHandle} and {@code afterCompletion}, as it
 * normally does, since the results of request handling (e.g. ModelAndView)
 * will. be produced concurrently in another thread. In such scenarios,
 * {@link #afterConcurrentHandlingStarted(HttpServletRequest, HttpServletResponse, Object)}
 * is invoked instead allowing implementations to perform tasks such as cleaning
 * up thread bound attributes.
 *
 * <p>When asynchronous handling completes, the request is dispatched to the
 * container for further processing. At this stage the DispatcherServlet invokes
 * {@code preHandle}, {@code postHandle} and {@code afterCompletion} as usual.
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
	 * the a handler is being executed concurrently. Implementations may use
	 * the provided request and response but should avoid modifying them in
	 * ways that would conflict with the concurrent execution of the handler.
	 * A typical use of this method would be to clean thread local variables.
	 * @param request the current request
	 * @param response the current response
	 * @param handler handler (or {@link HandlerMethod}) that started async
	 * execution, for type and/or instance examination
	 * @throws Exception in case of errors
	 */
	void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception;

}
