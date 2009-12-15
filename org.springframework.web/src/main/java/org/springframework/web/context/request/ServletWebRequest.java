/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.web.context.request;

import java.security.Principal;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link WebRequest} adapter for an {@link javax.servlet.http.HttpServletRequest}.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public class ServletWebRequest extends ServletRequestAttributes implements NativeWebRequest {

	private static final String HEADER_IF_MODIFIED_SINCE = "If-Modified-Since";

	private static final String HEADER_LAST_MODIFIED = "Last-Modified";


	private HttpServletResponse response;

	private boolean notModified = false;


	/**
	 * Create a new ServletWebRequest instance for the given request.
	 * @param request current HTTP request
	 */
	public ServletWebRequest(HttpServletRequest request) {
		super(request);
	}

	/**
	 * Create a new ServletWebRequest instance for the given request/response pair.
	 * @param request current HTTP request
	 * @param response current HTTP response (for automatic last-modified handling)
	 */
	public ServletWebRequest(HttpServletRequest request, HttpServletResponse response) {
		this(request);
		this.response = response;
	}


	/**
	 * Exposes the native {@link HttpServletRequest} that we're wrapping (if any).
	 */
	public final HttpServletResponse getResponse() {
		return this.response;
	}

	public Object getNativeRequest() {
		return getRequest();
	}

	public Object getNativeResponse() {
		return getResponse();
	}


	public String getHeader(String headerName) {
		return getRequest().getHeader(headerName);
	}

	@SuppressWarnings("unchecked")
	public String[] getHeaderValues(String headerName) {
		String[] headerValues = StringUtils.toStringArray(getRequest().getHeaders(headerName));
		return (!ObjectUtils.isEmpty(headerValues) ? headerValues : null);
	}

	@SuppressWarnings("unchecked")
	public Iterator<String> getHeaderNames() {
		return CollectionUtils.toIterator(getRequest().getHeaderNames());
	}

	public String getParameter(String paramName) {
		return getRequest().getParameter(paramName);
	}

	public String[] getParameterValues(String paramName) {
		return getRequest().getParameterValues(paramName);
	}

	@SuppressWarnings("unchecked")
	public Iterator<String> getParameterNames() {
		return CollectionUtils.toIterator(getRequest().getParameterNames());
	}

	@SuppressWarnings("unchecked")
	public Map<String, String[]> getParameterMap() {
		return getRequest().getParameterMap();
	}

	public Locale getLocale() {
		return getRequest().getLocale();
	}

	public String getContextPath() {
		return getRequest().getContextPath();
	}

	public String getRemoteUser() {
		return getRequest().getRemoteUser();
	}

	public Principal getUserPrincipal() {
		return getRequest().getUserPrincipal();
	}

	public boolean isUserInRole(String role) {
		return getRequest().isUserInRole(role);
	}

	public boolean isSecure() {
		return getRequest().isSecure();
	}

	public boolean checkNotModified(long lastModifiedTimestamp) {
		if (lastModifiedTimestamp >= 0 && !this.notModified &&
				(this.response == null || !this.response.containsHeader(HEADER_LAST_MODIFIED))) {
			long ifModifiedSince = getRequest().getDateHeader(HEADER_IF_MODIFIED_SINCE);
			this.notModified = (ifModifiedSince >= (lastModifiedTimestamp / 1000 * 1000));
			if (this.response != null) {
				if (this.notModified) {
					this.response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				}
				else {
					this.response.setDateHeader(HEADER_LAST_MODIFIED, lastModifiedTimestamp);
				}
			}
		}
		return this.notModified;
	}

	public boolean isNotModified() {
		return this.notModified;
	}

	public String getDescription(boolean includeClientInfo) {
		HttpServletRequest request = getRequest();
		StringBuilder sb = new StringBuilder();
		sb.append("uri=").append(request.getRequestURI());
		if (includeClientInfo) {
			String client = request.getRemoteAddr();
			if (StringUtils.hasLength(client)) {
				sb.append(";client=").append(client);
			}
			HttpSession session = request.getSession(false);
			if (session != null) {
				sb.append(";session=").append(session.getId());
			}
			String user = request.getRemoteUser();
			if (StringUtils.hasLength(user)) {
				sb.append(";user=").append(user);
			}
		}
		return sb.toString();
	}


	@Override
	public String toString() {
		return "ServletWebRequest: " + getDescription(true);
	}

}
