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

import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AbstractDelegatingCallable;

/**
 * Extends {@link HanderInterceptor} with lifecycle methods specific to async
 * request processing.
 *
 * <p>This is the sequence of events on the main thread in an async scenario:
 * <ol>
 * 	<li>{@link #preHandle(WebRequest)}
 * 	<li>{@link #getAsyncCallable(WebRequest)}
 * 	<li>... <em>handler execution</em>
 * 	<li>{@link #postHandleAsyncStarted(WebRequest)}
 * </ol>
 *
 * <p>This is the sequence of events on the async thread:
 * <ol>
 * 	<li>Async {@link Callable#call()} (the {@code Callable} returned by {@code getAsyncCallable})
 * 	<li>... <em>async handler execution</em>
 * 	<li>{@link #postHandle(WebRequest, org.springframework.ui.ModelMap)}
 * 	<li>{@link #afterCompletion(WebRequest, Exception)}
 * </ol>
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public interface AsyncHandlerInterceptor extends HandlerInterceptor {

	/**
	 * Invoked <em>after</em> {@link #preHandle(WebRequest)} and <em>before</em>
	 * the handler is executed. The returned {@link Callable} is used only if
	 * handler execution leads to teh start of async processing. It is invoked
	 *  the async thread before the request is handled fro.
	 * <p>Implementations can use this <code>Callable</code> to initialize
	 * ThreadLocal attributes on the async thread.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler chosen handler to execute, for type and/or instance examination
	 * @return a {@link Callable} instance or <code>null</code>
	 */
	AbstractDelegatingCallable getAsyncCallable(HttpServletRequest request, HttpServletResponse response, Object handler);

	/**
	 * Invoked <em>after</em> the execution of a handler if the handler started
	 * async processing instead of handling the request. Effectively this method
	 * is invoked on the way out of the main processing thread instead of
	 * {@link #postHandle(WebRequest, org.springframework.ui.ModelMap)}. The
	 * <code>postHandle</code> method is invoked after the request is handled
	 * in the async thread.
	 * <p>Implementations of this method can ensure ThreadLocal attributes bound
	 * to the main thread are cleared and also prepare for binding them to the
	 * async thread.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler chosen handler to execute, for type and/or instance examination
	 */
	void postHandleAsyncStarted(HttpServletRequest request, HttpServletResponse response, Object handler);

}
