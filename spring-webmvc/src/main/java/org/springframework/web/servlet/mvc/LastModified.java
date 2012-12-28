/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.web.servlet.mvc;

import javax.servlet.http.HttpServletRequest;

/**
 * Supports last-modified HTTP requests to facilitate content caching.
 * Same contract as for the Servlet API's {@code getLastModified} method.
 *
 * <p>Delegated to by a {@link org.springframework.web.servlet.HandlerAdapter#getLastModified}
 * implementation. By default, any Controller or HttpRequestHandler within Spring's
 * default framework can implement this interface to enable last-modified checking.
 *
 * <p><b>Note:</b> Alternative handler implementation approaches have different
 * last-modified handling styles. For example, Spring 2.5's annotated controller
 * approach (using {@code @RequestMapping}) provides last-modified support
 * through the {@link org.springframework.web.context.request.WebRequest#checkNotModified}
 * method, allowing for last-modified checking within the main handler method.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see javax.servlet.http.HttpServlet#getLastModified
 * @see Controller
 * @see SimpleControllerHandlerAdapter
 * @see org.springframework.web.HttpRequestHandler
 * @see HttpRequestHandlerAdapter
 */
public interface LastModified {

	/**
	 * Same contract as for HttpServlet's {@code getLastModified} method.
	 * Invoked <b>before</b> request processing.
	 * <p>The return value will be sent to the HTTP client as Last-Modified header,
	 * and compared with If-Modified-Since headers that the client sends back.
	 * The content will only get regenerated if there has been a modification.
	 * @param request current HTTP request
	 * @return the time the underlying resource was last modified, or -1
	 * meaning that the content must always be regenerated
	 * @see org.springframework.web.servlet.HandlerAdapter#getLastModified
	 * @see javax.servlet.http.HttpServlet#getLastModified
	 */
	long getLastModified(HttpServletRequest request);

}
