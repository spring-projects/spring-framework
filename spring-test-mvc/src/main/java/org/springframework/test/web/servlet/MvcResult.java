/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License; Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing; software
 * distributed under the License is distributed on an "AS IS" BASIS;
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND; either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.servlet;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Provides access to the result of an executed request.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public interface MvcResult {

	/**
	 * Return the performed request.
	 * @return the request, never {@code null}
	 */
	MockHttpServletRequest getRequest();

	/**
	 * Return the resulting response.
	 * @return the response, never {@code null}
	 */
	MockHttpServletResponse getResponse();

	/**
	 * Return the executed handler.
	 * @return the handler, possibly {@code null} if none were executed
	 */
	Object getHandler();

	/**
	 * Return interceptors around the handler.
	 * @return interceptors, or {@code null} if none were selected
	 */
	HandlerInterceptor[] getInterceptors();

	/**
	 * Return the {@code ModelAndView} prepared by the handler.
	 * @return a {@code ModelAndView}, or {@code null}
	 */
	ModelAndView getModelAndView();

	/**
	 * Return any exception raised by a handler and successfully resolved
	 * through a {@link HandlerExceptionResolver}.
	 *
	 * @return an exception, possibly {@code null}
	 */
 	Exception getResolvedException();

	/**
	 * Return the "output" flash attributes saved during request processing.
	 * @return the {@code FlashMap}, possibly empty
	 */
	FlashMap getFlashMap();

	/**
	 * Get the result of asynchronous execution or {@code null} if concurrent
	 * handling did not start. This method will hold and await the completion
	 * of concurrent handling.
	 *
	 * @throws IllegalStateException if concurrent handling does not complete
	 * within the allocated async timeout value.
	 */
	Object getAsyncResult();

	/**
	 * Get the result of asynchronous execution or {@code null} if concurrent
	 * handling did not start. This method will wait for up to the given timeout
	 * for the completion of concurrent handling.
	 *
	 * @param timeout how long to wait for the async result to be set in
	 * milliseconds; if -1, the wait will be as long as the async timeout set
	 * on the Servlet request
	 */
	Object getAsyncResult(long timeout);

}
