/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
