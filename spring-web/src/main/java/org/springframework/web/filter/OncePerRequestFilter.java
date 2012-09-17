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

package org.springframework.web.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.request.async.WebAsyncUtils;

/**
 * Filter base class that guarantees to be just executed once per request,
 * on any servlet container. It provides a {@link #doFilterInternal}
 * method with HttpServletRequest and HttpServletResponse arguments.
 *
 * <p>In an async scenario a filter may be invoked again in additional threads
 * as part of an {@linkplain javax.servlet.DispatcherType.ASYNC ASYNC} dispatch.
 * Sub-classes may decide whether to be invoked once per request or once per
 * request thread for as long as the same request is being processed.
 * See {@link #shouldFilterAsyncDispatches()}.
 *
 * <p>The {@link #getAlreadyFilteredAttributeName} method determines how
 * to identify that a request is already filtered. The default implementation
 * is based on the configured name of the concrete filter instance.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 06.12.2003
 */
public abstract class OncePerRequestFilter extends GenericFilterBean {

	/**
	 * Suffix that gets appended to the filter name for the
	 * "already filtered" request attribute.
	 * @see #getAlreadyFilteredAttributeName
	 */
	public static final String ALREADY_FILTERED_SUFFIX = ".FILTERED";


	/**
	 * This <code>doFilter</code> implementation stores a request attribute for
	 * "already filtered", proceeding without filtering again if the
	 * attribute is already there.
	 * @see #getAlreadyFilteredAttributeName
	 * @see #shouldNotFilter
	 * @see #doFilterInternal
	 */
	public final void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
			throw new ServletException("OncePerRequestFilter just supports HTTP requests");
		}
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		boolean processAsyncRequestThread = isAsyncDispatch(httpRequest) && shouldFilterAsyncDispatches();

		String alreadyFilteredAttributeName = getAlreadyFilteredAttributeName();
		boolean hasAlreadyFilteredAttribute = request.getAttribute(alreadyFilteredAttributeName) != null;

		if ((hasAlreadyFilteredAttribute && (!processAsyncRequestThread)) || shouldNotFilter(httpRequest)) {

			// Proceed without invoking this filter...
			filterChain.doFilter(request, response);
		}
		else {
			// Do invoke this filter...
			request.setAttribute(alreadyFilteredAttributeName, Boolean.TRUE);
			try {
				doFilterInternal(httpRequest, httpResponse, filterChain);
			}
			finally {
				if (isLastRequestThread(httpRequest)) {
					// Remove the "already filtered" request attribute for this request.
					request.removeAttribute(alreadyFilteredAttributeName);
				}
			}
		}
	}

	/**
	 * Return the name of the request attribute that identifies that a request
	 * is already filtered.
	 * <p>Default implementation takes the configured name of the concrete filter
	 * instance and appends ".FILTERED". If the filter is not fully initialized,
	 * it falls back to its class name.
	 * @see #getFilterName
	 * @see #ALREADY_FILTERED_SUFFIX
	 */
	protected String getAlreadyFilteredAttributeName() {
		String name = getFilterName();
		if (name == null) {
			name = getClass().getName();
		}
		return name + ALREADY_FILTERED_SUFFIX;
	}

	/**
	 * Can be overridden in subclasses for custom filtering control,
	 * returning <code>true</code> to avoid filtering of the given request.
	 * <p>The default implementation always returns <code>false</code>.
	 * @param request current HTTP request
	 * @return whether the given request should <i>not</i> be filtered
	 * @throws ServletException in case of errors
	 */
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		return false;
	}

	/**
	 * Whether to filter once per request or once per request thread. The dispatcher
	 * type {@code javax.servlet.DispatcherType.ASYNC} introduced in Servlet 3.0
	 * means a filter can be invoked in more than one thread (and exited) over the
	 * course of a single request. Some filters only need to filter the initial
	 * thread (e.g. request wrapping) while others may need to be invoked at least
	 * once in each additional thread for example for setting up thread locals or
	 * to perform final processing at the very end.
	 * <p>Note that although a filter can be mapped to handle specific dispatcher
	 * types via {@code web.xml} or in Java through the {@code ServletContext},
	 * servlet containers may enforce different defaults with regards to dispatcher
	 * types. This flag enforces the design intent of the filter.
	 * <p>The default setting is "false",  which means the filter will be invoked
	 * once only per request and only on the initial request thread. If "true", the
	 * filter will also be invoked once only on each additional thread.
	 *
	 * @see org.springframework.web.context.request.async.WebAsyncManager
	 */
	protected boolean shouldFilterAsyncDispatches() {
		return false;
	}

	/**
	 * Whether the request was dispatched to complete processing of results produced
	 * in another thread. This aligns with the Servlet 3.0 dispatcher type
	 * {@code javax.servlet.DispatcherType.ASYNC} and can be used by filters that
	 * return "true" from {@link #shouldFilterAsyncDispatches()} to detect when
	 * the filter is being invoked subsequently in additional thread(s).
	 *
	 * @see org.springframework.web.context.request.async.WebAsyncManager
	 */
	protected final boolean isAsyncDispatch(HttpServletRequest request) {
		return WebAsyncUtils.getAsyncManager(request).hasConcurrentResult();
	}

	/**
	 * Whether this is the last thread processing the request. Note the returned
	 * value may change from {@code true} to {@code false} if the method is
	 * invoked before and after delegating to the next filter, since the next filter
	 * or servlet may begin concurrent processing. Therefore this method is most
	 * useful after delegation for final, end-of-request type processing.
	 * @param request the current request
	 * @return {@code true} if the response will be committed when the current
	 * 	thread exits; {@code false} if the response will remain open.
	 *
	 * @see org.springframework.web.context.request.async.WebAsyncManager
	 */
	protected final boolean isLastRequestThread(HttpServletRequest request) {
		return (!WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted());
	}

	/**
	 * Same contract as for <code>doFilter</code>, but guaranteed to be
	 * just invoked once per request or once per request thread.
	 * See {@link #shouldFilterAsyncDispatches()} for details.
	 * <p>Provides HttpServletRequest and HttpServletResponse arguments instead of the
	 * default ServletRequest and ServletResponse ones.
	 */
	protected abstract void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException;

}
