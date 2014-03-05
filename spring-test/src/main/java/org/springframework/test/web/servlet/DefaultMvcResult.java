/*
 * Copyright 2002-2014 the original author or authors.
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

	private static final Object RESULT_NONE = new Object();


	private final MockHttpServletRequest mockRequest;

	private final MockHttpServletResponse mockResponse;

	private Object handler;

	private HandlerInterceptor[] interceptors;

	private ModelAndView modelAndView;

	private Exception resolvedException;

	private Object asyncResult = RESULT_NONE;

	private CountDownLatch asyncResultLatch;


	/**
	 * Create a new instance with the given request and response.
	 */
	public DefaultMvcResult(MockHttpServletRequest request, MockHttpServletResponse response) {
		this.mockRequest = request;
		this.mockResponse = response;
	}

	@Override
	public MockHttpServletResponse getResponse() {
		return mockResponse;
	}

	@Override
	public MockHttpServletRequest getRequest() {
		return mockRequest;
	}

	@Override
	public Object getHandler() {
		return this.handler;
	}

	public void setHandler(Object handler) {
		this.handler = handler;
	}

	@Override
	public HandlerInterceptor[] getInterceptors() {
		return this.interceptors;
	}

	public void setInterceptors(HandlerInterceptor[] interceptors) {
		this.interceptors = interceptors;
	}

	@Override
	public Exception getResolvedException() {
		return this.resolvedException;
	}

	public void setResolvedException(Exception resolvedException) {
		this.resolvedException = resolvedException;
	}

	@Override
	public ModelAndView getModelAndView() {
		return this.modelAndView;
	}

	public void setModelAndView(ModelAndView mav) {
		this.modelAndView = mav;
	}

	@Override
	public FlashMap getFlashMap() {
		return RequestContextUtils.getOutputFlashMap(mockRequest);
	}

	public void setAsyncResult(Object asyncResult) {
		this.asyncResult = asyncResult;
	}

	@Override
	public Object getAsyncResult() {
		return getAsyncResult(-1);
	}

	@Override
	public Object getAsyncResult(long timeout) {
		if (this.asyncResult == RESULT_NONE) {
			if ((timeout != 0) && this.mockRequest.isAsyncStarted()) {
				if (timeout == -1) {
					timeout = this.mockRequest.getAsyncContext().getTimeout();
				}
				if (!awaitAsyncResult(timeout) && this.asyncResult == RESULT_NONE) {
					throw new IllegalStateException(
							"Gave up waiting on async result from handler [" + this.handler + "] to complete");
				}
			}
		}
		return (this.asyncResult == RESULT_NONE ? null : this.asyncResult);
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

	public void setAsyncResultLatch(CountDownLatch asyncResultLatch) {
		this.asyncResultLatch = asyncResultLatch;
	}

}
