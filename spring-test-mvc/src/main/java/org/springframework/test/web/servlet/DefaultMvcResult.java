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
package org.springframework.test.web.servlet;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * A simple implementation of {@link MvcResult} with setters.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 3.2
 */
class DefaultMvcResult implements MvcResult {

	private final MockHttpServletRequest mockRequest;

	private final MockHttpServletResponse mockResponse;

	private Object handler;

	private HandlerInterceptor[] interceptors;

	private ModelAndView modelAndView;

	private Exception resolvedException;

	private CountDownLatch asyncResultLatch;


	/**
	 * Create a new instance with the given request and response.
	 */
	public DefaultMvcResult(MockHttpServletRequest request, MockHttpServletResponse response) {
		this.mockRequest = request;
		this.mockResponse = response;
	}

	public MockHttpServletResponse getResponse() {
		return mockResponse;
	}

	public MockHttpServletRequest getRequest() {
		return mockRequest;
	}

	public Object getHandler() {
		return this.handler;
	}

	public void setHandler(Object handler) {
		this.handler = handler;
	}

	public HandlerInterceptor[] getInterceptors() {
		return this.interceptors;
	}

	public void setInterceptors(HandlerInterceptor[] interceptors) {
		this.interceptors = interceptors;
	}

	public Exception getResolvedException() {
		return this.resolvedException;
	}

	public void setResolvedException(Exception resolvedException) {
		this.resolvedException = resolvedException;
	}

	public ModelAndView getModelAndView() {
		return this.modelAndView;
	}

	public void setModelAndView(ModelAndView mav) {
		this.modelAndView = mav;
	}

	public FlashMap getFlashMap() {
		return RequestContextUtils.getOutputFlashMap(mockRequest);
	}

	public void setAsyncResultLatch(CountDownLatch asyncResultLatch) {
		this.asyncResultLatch = asyncResultLatch;
	}

	public Object getAsyncResult() {
		HttpServletRequest request = this.mockRequest;
		if (request.isAsyncStarted()) {

			long timeout = request.getAsyncContext().getTimeout();
			if (!awaitAsyncResult(timeout)) {
				throw new IllegalStateException(
						"Gave up waiting on async result from [" + this.handler + "] to complete");
			}

			WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(this.mockRequest);
			if (asyncManager.hasConcurrentResult()) {
				return asyncManager.getConcurrentResult();
			}
		}
		return null;
	}

	private boolean awaitAsyncResult(long timeout) {
		if (this.asyncResultLatch != null) {
			try {
				return this.asyncResultLatch.await(timeout, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e) {
				return false;
			}
		}
		return true;
	}

}
