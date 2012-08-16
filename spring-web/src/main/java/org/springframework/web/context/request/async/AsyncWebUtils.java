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
package org.springframework.web.context.request.async;

import java.lang.reflect.Constructor;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeanUtils;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

/**
 * Utility methods related to processing asynchronous web requests.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public abstract class AsyncWebUtils {

	public static final String WEB_ASYNC_MANAGER_ATTRIBUTE = WebAsyncManager.class.getName() + ".WEB_ASYNC_MANAGER";

	/**
	 * Obtain the {@link WebAsyncManager} for the current request, or if not
	 * found, create and associate it with the request.
	 */
	public static WebAsyncManager getAsyncManager(ServletRequest servletRequest) {
		WebAsyncManager asyncManager = (WebAsyncManager) servletRequest.getAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE);
		if (asyncManager == null) {
			asyncManager = new WebAsyncManager();
			servletRequest.setAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE, asyncManager);
		}
		return asyncManager;
	}

	/**
	 * Obtain the {@link WebAsyncManager} for the current request, or if not
	 * found, create and associate it with the request.
	 */
	public static WebAsyncManager getAsyncManager(WebRequest webRequest) {
		int scope = RequestAttributes.SCOPE_REQUEST;
		WebAsyncManager asyncManager = (WebAsyncManager) webRequest.getAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE, scope);
		if (asyncManager == null) {
			asyncManager = new WebAsyncManager();
			webRequest.setAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE, asyncManager, scope);
		}
		return asyncManager;
	}

	/**
	 * Create an AsyncWebRequest instance.
	 * <p>By default an instance of {@link StandardServletAsyncWebRequest} is created
	 * if running in Servlet 3.0 (or higher) environment or as a fallback option an
	 * instance of {@link NoSupportAsyncWebRequest} is returned.
	 * @param request the current request
	 * @param response the current response
	 * @return an AsyncWebRequest instance, never {@code null}
	 */
	public static AsyncWebRequest createAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
		return ClassUtils.hasMethod(ServletRequest.class, "startAsync") ?
				createStandardServletAsyncWebRequest(request, response) : new NoSupportAsyncWebRequest(request, response);
	}

	private static AsyncWebRequest createStandardServletAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
		try {
			String className = "org.springframework.web.context.request.async.StandardServletAsyncWebRequest";
			Class<?> clazz = ClassUtils.forName(className, AsyncWebUtils.class.getClassLoader());
			Constructor<?> constructor = clazz.getConstructor(HttpServletRequest.class, HttpServletResponse.class);
			return (AsyncWebRequest) BeanUtils.instantiateClass(constructor, request, response);
		}
		catch (Throwable t) {
			throw new IllegalStateException("Failed to instantiate StandardServletAsyncWebRequest", t);
		}
	}

}
