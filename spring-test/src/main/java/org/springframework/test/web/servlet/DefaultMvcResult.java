/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.Assert;
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

	private final AtomicReference<Object> asyncResult = new AtomicReference<Object>(RESULT_NONE);

	private CountDownLatch asyncDispatchLatch;


	/**
	 * Create a new instance with the given request and response.
	 */
	public DefaultMvcResult(MockHttpServletRequest request, MockHttpServletResponse response) {
		this.mockRequest = request;
		this.mockResponse = response;
	}


	@Override
	public MockHttpServletRequest getRequest() {
		return this.mockRequest;
	}

	@Override
	public MockHttpServletResponse getResponse() {
		return this.mockResponse;
	}

	public void setHandler(Object handler) {
		this.handler = handler;
	}

	@Override
	public Object getHandler() {
		return this.handler;
	}

	public void setInterceptors(HandlerInterceptor... interceptors) {
		this.interceptors = interceptors;
	}

	@Override
	public HandlerInterceptor[] getInterceptors() {
		return this.interceptors;
	}

	public void setResolvedException(Exception resolvedException) {
		this.resolvedException = resolvedException;
	}

	@Override
	public Exception getResolvedException() {
		return this.resolvedException;
	}

	public void setModelAndView(ModelAndView mav) {
		this.modelAndView = mav;
	}

	@Override
	public ModelAndView getModelAndView() {
		return this.modelAndView;
	}

	@Override
	public FlashMap getFlashMap() {
		return RequestContextUtils.getOutputFlashMap(this.mockRequest);
	}

	public void setAsyncResult(Object asyncResult) {
		this.asyncResult.set(asyncResult);
	}

	@Override
	public Object getAsyncResult() {
		return getAsyncResult(-1);
	}

	@Override
	public Object getAsyncResult(long timeToWait) {
		if (this.mockRequest.getAsyncContext() != null) {
			timeToWait = (timeToWait == -1 ? this.mockRequest.getAsyncContext().getTimeout() : timeToWait);
		}
		if (!awaitAsyncDispatch(timeToWait)) {
			throw new IllegalStateException("Async result for handler [" + this.handler + "]" +
					" was not set during the specified timeToWait=" + timeToWait);
		}
		Object result = this.asyncResult.get();
		if (result == RESULT_NONE) {
			throw new IllegalStateException("Async result for handler [" + this.handler + "] was not set");
		}
		return this.asyncResult.get();
	}

	/**
	 * True if the latch count reached 0 within the specified timeout.
	 */
	private boolean awaitAsyncDispatch(long timeout) {
		Assert.state(this.asyncDispatchLatch != null,
				"The asyncDispatch CountDownLatch was not set by the TestDispatcherServlet.");
		try {
			return this.asyncDispatchLatch.await(timeout, TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException ex) {
			return false;
		}
	}

	void setAsyncDispatchLatch(CountDownLatch asyncDispatchLatch) {
		this.asyncDispatchLatch = asyncDispatchLatch;
	}

}
