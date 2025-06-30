/*
 * Copyright 2002-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * A stub implementation of the {@link MvcResult} contract.
 *
 * @author Rossen Stoyanchev
 */
public class StubMvcResult implements MvcResult {

	private MockHttpServletRequest request;

	private Object handler;

	private HandlerInterceptor[] interceptors;

	private Exception resolvedException;

	private ModelAndView mav;

	private FlashMap flashMap;

	private MockHttpServletResponse response;

	public StubMvcResult(MockHttpServletRequest request,
						Object handler,
						HandlerInterceptor[] interceptors,
						Exception resolvedException,
						ModelAndView mav,
						FlashMap flashMap,
						MockHttpServletResponse response) {
		this.request = request;
		this.handler = handler;
		this.interceptors = interceptors;
		this.resolvedException = resolvedException;
		this.mav = mav;
		this.flashMap = flashMap;
		this.response = response;
	}

	@Override
	public MockHttpServletRequest getRequest() {
		return request;
	}

	@Override
	public Object getHandler() {
		return handler;
	}

	@Override
	public HandlerInterceptor[] getInterceptors() {
		return interceptors;
	}

	@Override
	public Exception getResolvedException() {
		return resolvedException;
	}

	@Override
	public ModelAndView getModelAndView() {
		return mav;
	}

	@Override
	public FlashMap getFlashMap() {
		return flashMap;
	}

	@Override
	public MockHttpServletResponse getResponse() {
		return response;
	}

	public ModelAndView getMav() {
		return mav;
	}

	public void setMav(ModelAndView mav) {
		this.mav = mav;
	}

	public void setRequest(MockHttpServletRequest request) {
		this.request = request;
	}

	public void setHandler(Object handler) {
		this.handler = handler;
	}

	public void setInterceptors(HandlerInterceptor[] interceptors) {
		this.interceptors = interceptors;
	}

	public void setResolvedException(Exception resolvedException) {
		this.resolvedException = resolvedException;
	}

	public void setFlashMap(FlashMap flashMap) {
		this.flashMap = flashMap;
	}

	public void setResponse(MockHttpServletResponse response) {
		this.response = response;
	}

	@Override
	public @Nullable Object getAsyncResult() {
		return null;
	}

	@Override
	public @Nullable Object getAsyncResult(long timeToWait) {
		return null;
	}

}
