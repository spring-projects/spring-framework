/*
 * Copyright 2002-2010 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.async.AsyncExecutionChain;

/**
 * Handler execution chain, consisting of handler object and any handler interceptors.
 * Returned by HandlerMapping's {@link HandlerMapping#getHandler} method.
 *
 * @author Juergen Hoeller
 * @since 20.06.2003
 * @see HandlerInterceptor
 */
public class HandlerExecutionChain {

	private static final Log logger = LogFactory.getLog(HandlerExecutionChain.class);

	private final Object handler;

	private HandlerInterceptor[] interceptors;

	private List<HandlerInterceptor> interceptorList;

	private int interceptorIndex = -1;

	/**
	 * Create a new HandlerExecutionChain.
	 * @param handler the handler object to execute
	 */
	public HandlerExecutionChain(Object handler) {
		this(handler, null);
	}

	/**
	 * Create a new HandlerExecutionChain.
	 * @param handler the handler object to execute
	 * @param interceptors the array of interceptors to apply
	 * (in the given order) before the handler itself executes
	 */
	public HandlerExecutionChain(Object handler, HandlerInterceptor[] interceptors) {
		if (handler instanceof HandlerExecutionChain) {
			HandlerExecutionChain originalChain = (HandlerExecutionChain) handler;
			this.handler = originalChain.getHandler();
			this.interceptorList = new ArrayList<HandlerInterceptor>();
			CollectionUtils.mergeArrayIntoCollection(originalChain.getInterceptors(), this.interceptorList);
			CollectionUtils.mergeArrayIntoCollection(interceptors, this.interceptorList);
		}
		else {
			this.handler = handler;
			this.interceptors = interceptors;
		}
	}

	/**
	 * Return the handler object to execute.
	 * @return the handler object
	 */
	public Object getHandler() {
		return this.handler;
	}

	public void addInterceptor(HandlerInterceptor interceptor) {
		initInterceptorList();
		this.interceptorList.add(interceptor);
	}

	public void addInterceptors(HandlerInterceptor[] interceptors) {
		if (interceptors != null) {
			initInterceptorList();
			this.interceptorList.addAll(Arrays.asList(interceptors));
		}
	}

	private void initInterceptorList() {
		if (this.interceptorList == null) {
			this.interceptorList = new ArrayList<HandlerInterceptor>();
		}
		if (this.interceptors != null) {
			this.interceptorList.addAll(Arrays.asList(this.interceptors));
			this.interceptors = null;
		}
	}

	/**
	 * Return the array of interceptors to apply (in the given order).
	 * @return the array of HandlerInterceptors instances (may be <code>null</code>)
	 */
	public HandlerInterceptor[] getInterceptors() {
		if (this.interceptors == null && this.interceptorList != null) {
			this.interceptors = this.interceptorList.toArray(new HandlerInterceptor[this.interceptorList.size()]);
		}
		return this.interceptors;
	}

	/**
	 * Apply preHandle methods of registered interceptors.
	 * @return <code>true</code> if the execution chain should proceed with the
	 * next interceptor or the handler itself. Else, DispatcherServlet assumes
	 * that this interceptor has already dealt with the response itself.
	 */
	boolean applyPreHandle(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		if (getInterceptors() != null) {
			for (int i = 0; i < getInterceptors().length; i++) {
				HandlerInterceptor interceptor = getInterceptors()[i];
				if (!interceptor.preHandle(request, response, this.handler)) {
					triggerAfterCompletion(request, response, null);
					return false;
				}
				this.interceptorIndex = i;
			}
		}
		return true;
	}

	/**
	 * Apply postHandle methods of registered interceptors.
	 */
	void applyPostHandle(HttpServletRequest request, HttpServletResponse response, ModelAndView mv)
			throws Exception {

		if (getInterceptors() == null) {
			return;
		}
		for (int i = getInterceptors().length - 1; i >= 0; i--) {
			HandlerInterceptor interceptor = getInterceptors()[i];
			interceptor.postHandle(request, response, this.handler, mv);
		}
	}

	/**
	 * Add delegating, async Callable instances to the {@link AsyncExecutionChain}
	 * for use in case of asynchronous request processing.
	 */
	void addDelegatingCallables(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		if (getInterceptors() == null) {
			return;
		}
		for (int i = getInterceptors().length - 1; i >= 0; i--) {
			HandlerInterceptor interceptor = getInterceptors()[i];
			if (interceptor instanceof AsyncHandlerInterceptor) {
				try {
					AsyncHandlerInterceptor asyncInterceptor = (AsyncHandlerInterceptor) interceptor;
					AsyncExecutionChain chain = AsyncExecutionChain.getForCurrentRequest(request);
					chain.addDelegatingCallable(asyncInterceptor.getAsyncCallable(request, response, this.handler));
				}
				catch (Throwable ex) {
					logger.error("HandlerInterceptor.addAsyncCallables threw exception", ex);
				}
			}
		}
	}

	/**
	 * Trigger postHandleAsyncStarted callbacks on the mapped HandlerInterceptors.
	 */
	void applyPostHandleAsyncStarted(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		if (getInterceptors() == null) {
			return;
		}
		for (int i = getInterceptors().length - 1; i >= 0; i--) {
			HandlerInterceptor interceptor = getInterceptors()[i];
			if (interceptor instanceof AsyncHandlerInterceptor) {
				try {
					((AsyncHandlerInterceptor) interceptor).postHandleAsyncStarted(request, response, this.handler);
				}
				catch (Throwable ex) {
					logger.error("HandlerInterceptor.postHandleAsyncStarted threw exception", ex);
				}
			}
		}
	}

	/**
	 * Trigger afterCompletion callbacks on the mapped HandlerInterceptors.
	 * Will just invoke afterCompletion for all interceptors whose preHandle invocation
	 * has successfully completed and returned true.
	 */
	void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response, Exception ex)
			throws Exception {

		if (getInterceptors() == null) {
			return;
		}
		for (int i = this.interceptorIndex; i >= 0; i--) {
			HandlerInterceptor interceptor = getInterceptors()[i];
			try {
				interceptor.afterCompletion(request, response, this.handler, ex);
			}
			catch (Throwable ex2) {
				logger.error("HandlerInterceptor.afterCompletion threw exception", ex2);
			}
		}
	}


	/**
	 * Delegates to the handler's <code>toString()</code>.
	 */
	@Override
	public String toString() {
		if (this.handler == null) {
			return "HandlerExecutionChain with no handler";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("HandlerExecutionChain with handler [").append(this.handler).append("]");
		if (!CollectionUtils.isEmpty(this.interceptorList)) {
			sb.append(" and ").append(this.interceptorList.size()).append(" interceptor");
			if (this.interceptorList.size() > 1) {
				sb.append("s");
			}
		}
		return sb.toString();
	}

}
