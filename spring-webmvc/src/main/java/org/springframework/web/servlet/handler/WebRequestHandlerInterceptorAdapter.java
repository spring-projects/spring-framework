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

package org.springframework.web.servlet.handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.context.request.async.AbstractDelegatingCallable;
import org.springframework.web.context.request.async.AsyncWebRequestInterceptor;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Adapter that implements the Servlet HandlerInterceptor interface
 * and wraps an underlying WebRequestInterceptor.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.web.context.request.WebRequestInterceptor
 * @see org.springframework.web.servlet.HandlerInterceptor
 */
public class WebRequestHandlerInterceptorAdapter implements AsyncHandlerInterceptor {

	private final WebRequestInterceptor requestInterceptor;


	/**
	 * Create a new WebRequestHandlerInterceptorAdapter for the given WebRequestInterceptor.
	 * @param requestInterceptor the WebRequestInterceptor to wrap
	 */
	public WebRequestHandlerInterceptorAdapter(WebRequestInterceptor requestInterceptor) {
		Assert.notNull(requestInterceptor, "WebRequestInterceptor must not be null");
		this.requestInterceptor = requestInterceptor;
	}


	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		this.requestInterceptor.preHandle(new DispatcherServletWebRequest(request, response));
		return true;
	}

	public AbstractDelegatingCallable getAsyncCallable(HttpServletRequest request,
			HttpServletResponse response, Object handler) {

		if (this.requestInterceptor instanceof AsyncWebRequestInterceptor) {
			AsyncWebRequestInterceptor asyncInterceptor = (AsyncWebRequestInterceptor) this.requestInterceptor;
			DispatcherServletWebRequest webRequest = new DispatcherServletWebRequest(request, response);
			return asyncInterceptor.getAsyncCallable(webRequest);
		}
		return null;
	}

	public void postHandleAfterAsyncStarted(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (this.requestInterceptor instanceof AsyncWebRequestInterceptor) {
			AsyncWebRequestInterceptor asyncInterceptor = (AsyncWebRequestInterceptor) this.requestInterceptor;
			DispatcherServletWebRequest webRequest = new DispatcherServletWebRequest(request, response);
			asyncInterceptor.postHandleAsyncStarted(webRequest);
		}
	}

	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView)
			throws Exception {

		this.requestInterceptor.postHandle(new DispatcherServletWebRequest(request, response),
				(modelAndView != null && !modelAndView.wasCleared() ? modelAndView.getModelMap() : null));
	}

	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {

		this.requestInterceptor.afterCompletion(new DispatcherServletWebRequest(request, response), ex);
	}

}
