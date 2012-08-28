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

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Extends the HandlerInterceptor contract for scenarios where a handler may be
 * executed asynchronously. Since the handler will complete execution in another
 * thread, the results are not available in the current thread, and therefore the
 * DispatcherServlet exits quickly and on its way out invokes
 * {@link #afterConcurrentHandlingStarted(HttpServletRequest, HttpServletResponse)}
 * instead of {@code postHandle} and {@code afterCompletion}.
 * When the async handler execution completes, and the request is dispatched back
 * for further processing, the DispatcherServlet will invoke {@code preHandle}
 * again, as well as {@code postHandle} and {@code afterCompletion}.
 *
 * <p>Existing implementations should consider the fact that {@code preHandle} may
 * be invoked twice before {@code postHandle} and {@code afterCompletion} are
 * called if they don't implement this contract. Once before the start of concurrent
 * handling and a second time as part of an asynchronous dispatch after concurrent
 * handling is done. This may be not important in most cases but when some work
 * needs to be done after concurrent handling starts (e.g. clearing thread locals)
 * then this contract can be implemented.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 *
 * @see org.springframework.web.context.request.async.WebAsyncManager
 */
public interface AsyncHandlerInterceptor extends HandlerInterceptor {

	/**
	 * Called instead of {@code postHandle} and {@code afterCompletion}, when the
	 * a handler is being executed concurrently. Implementations may use the provided
	 * request and response but should avoid modifying them in ways that would
	 * conflict with the concurrent execution of the handler. A typical use of
	 * this method would be to clean thread local variables.
	 *
	 * @param request the current request
	 * @param response the current response
	 */
	void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response);

}
