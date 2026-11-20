/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.context.request.async;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

/**
 * Utility methods related to processing asynchronous web requests.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.2
 */
public abstract class WebAsyncUtils {

	/**
	 * The name attribute containing the {@link WebAsyncManager}.
	 */
	public static final String WEB_ASYNC_MANAGER_ATTRIBUTE =
			WebAsyncManager.class.getName() + ".WEB_ASYNC_MANAGER";


	/**
	 * Obtain the {@link WebAsyncManager} for the current request, or if not
	 * found, create and associate it with the request.
	 */
	public static WebAsyncManager getAsyncManager(ServletRequest servletRequest) {
		WebAsyncManager asyncManager = null;
		Object asyncManagerAttr = servletRequest.getAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE);
		if (asyncManagerAttr instanceof WebAsyncManager wam) {
			asyncManager = wam;
		}
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
		WebAsyncManager asyncManager = null;
		Object asyncManagerAttr = webRequest.getAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE, scope);
		if (asyncManagerAttr instanceof WebAsyncManager wam) {
			asyncManager = wam;
		}
		if (asyncManager == null) {
			asyncManager = new WebAsyncManager();
			webRequest.setAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE, asyncManager, scope);
		}
		return asyncManager;
	}

	/**
	 * Create an AsyncWebRequest instance. By default, an instance of
	 * {@link StandardServletAsyncWebRequest} gets created.
	 * @param request the current request
	 * @param response the current response
	 * @return an AsyncWebRequest instance (never {@code null})
	 */
	public static AsyncWebRequest createAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
		AsyncWebRequest prev = getAsyncManager(request).getAsyncWebRequest();
		return (prev instanceof StandardServletAsyncWebRequest standardRequest ?
				new StandardServletAsyncWebRequest(request, response, standardRequest) :
				new StandardServletAsyncWebRequest(request, response));
	}

}
