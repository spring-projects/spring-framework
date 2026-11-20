/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.context.request;

/**
 * Extends {@code WebRequestInterceptor} with a callback method invoked during
 * asynchronous request handling.
 *
 * <p>When a handler starts asynchronous request handling, the DispatcherServlet
 * exits without invoking {@code postHandle} and {@code afterCompletion}, as it
 * normally does, since the results of request handling (for example, ModelAndView) are
 * not available in the current thread and handling is not yet complete.
 * In such scenarios, the {@link #afterConcurrentHandlingStarted(WebRequest)}
 * method is invoked instead allowing implementations to perform tasks such as
 * cleaning up thread bound attributes.
 *
 * <p>When asynchronous handling completes, the request is dispatched to the
 * container for further processing. At this stage the DispatcherServlet invokes
 * {@code preHandle}, {@code postHandle} and {@code afterCompletion} as usual.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 *
 * @see org.springframework.web.context.request.async.WebAsyncManager
 */
public interface AsyncWebRequestInterceptor extends WebRequestInterceptor{

	/**
	 * Called instead of {@code postHandle} and {@code afterCompletion}, when the
	 * handler started handling the request concurrently.
	 * @param request the current request
	 */
	void afterConcurrentHandlingStarted(WebRequest request);

}
