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

package org.springframework.web.servlet.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.web.context.request.AsyncWebRequestInterceptor;
import org.springframework.web.context.request.WebRequestInterceptor;
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


	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		this.requestInterceptor.preHandle(new DispatcherServletWebRequest(request, response));
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable ModelAndView modelAndView) throws Exception {

		this.requestInterceptor.postHandle(new DispatcherServletWebRequest(request, response),
				(modelAndView != null && !modelAndView.wasCleared() ? modelAndView.getModelMap() : null));
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable Exception ex) throws Exception {

		this.requestInterceptor.afterCompletion(new DispatcherServletWebRequest(request, response), ex);
	}

	@Override
	public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (this.requestInterceptor instanceof AsyncWebRequestInterceptor asyncInterceptor) {
			DispatcherServletWebRequest webRequest = new DispatcherServletWebRequest(request, response);
			asyncInterceptor.afterConcurrentHandlingStarted(webRequest);
		}
	}

}
