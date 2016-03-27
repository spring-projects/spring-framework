/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.context.request;

import java.security.Principal;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * {@link WebRequest} adapter for an {@link javax.servlet.http.HttpServletRequest}.
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author Markus Malkusch
 * @since 2.0
 */
public class ServletWebRequest extends ServletRequestAttributes implements NativeWebRequest {

	private static final String HEADER_ETAG = "ETag";

	private static final String HEADER_IF_MODIFIED_SINCE = "If-Modified-Since";

	private static final String HEADER_IF_UNMODIFIED_SINCE = "If-Unmodified-Since";

	private static final String HEADER_IF_NONE_MATCH = "If-None-Match";

	private static final String HEADER_LAST_MODIFIED = "Last-Modified";

	private static final String METHOD_GET = "GET";

	private static final String METHOD_HEAD = "HEAD";

	private static final String METHOD_POST = "POST";

	private static final String METHOD_PUT = "PUT";

	private static final String METHOD_DELETE = "DELETE";


	/** Checking for Servlet 3.0+ HttpServletResponse.getHeader(String) */
	private static final boolean servlet3Present =
			ClassUtils.hasMethod(HttpServletResponse.class, "getHeader", String.class);

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
		super(request, response);
	}


	@Override
	public Object getNativeRequest() {
		return getRequest();
	}

	@Override
	public Object getNativeResponse() {
		return getResponse();
	}

	@Override
	public <T> T getNativeRequest(Class<T> requiredType) {
		return WebUtils.getNativeRequest(getRequest(), requiredType);
	}

	@Override
	public <T> T getNativeResponse(Class<T> requiredType) {
		return WebUtils.getNativeResponse(getResponse(), requiredType);
	}

	/**
	 * Return the HTTP method of the request.
	 * @since 4.0.2
	 */
	public HttpMethod getHttpMethod() {
		return HttpMethod.resolve(getRequest().getMethod());
	}

	@Override
	public String getHeader(String headerName) {
		return getRequest().getHeader(headerName);
	}

	@Override
	public String[] getHeaderValues(String headerName) {
		String[] headerValues = StringUtils.toStringArray(getRequest().getHeaders(headerName));
		return (!ObjectUtils.isEmpty(headerValues) ? headerValues : null);
	}

	@Override
	public Iterator<String> getHeaderNames() {
		return CollectionUtils.toIterator(getRequest().getHeaderNames());
	}

	@Override
	public String getParameter(String paramName) {
		return getRequest().getParameter(paramName);
	}

	@Override
	public String[] getParameterValues(String paramName) {
		return getRequest().getParameterValues(paramName);
	}

	@Override
	public Iterator<String> getParameterNames() {
		return CollectionUtils.toIterator(getRequest().getParameterNames());
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		return getRequest().getParameterMap();
	}

	@Override
	public Locale getLocale() {
		return getRequest().getLocale();
	}

	@Override
	public String getContextPath() {
		return getRequest().getContextPath();
	}

	@Override
	public String getRemoteUser() {
		return getRequest().getRemoteUser();
	}

	@Override
	public Principal getUserPrincipal() {
		return getRequest().getUserPrincipal();
	}

	@Override
	public boolean isUserInRole(String role) {
		return getRequest().isUserInRole(role);
	}

	@Override
	public boolean isSecure() {
		return getRequest().isSecure();
	}


	@Override
	public boolean checkNotModified(long lastModifiedTimestamp) {
		HttpServletResponse response = getResponse();
		if (lastModifiedTimestamp >= 0 && !this.notModified) {
			if (isCompatibleWithConditionalRequests(response)) {
				this.notModified = isTimestampNotModified(lastModifiedTimestamp);
				if (response != null) {
					if (supportsNotModifiedStatus()) {
						if (this.notModified) {
							response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
						}
						if (isHeaderAbsent(response, HEADER_LAST_MODIFIED)) {
							response.setDateHeader(HEADER_LAST_MODIFIED, lastModifiedTimestamp);
						}
					}
					else if (supportsConditionalUpdate()) {
						if (this.notModified) {
							response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
						}
					}
				}
			}
		}
		return this.notModified;
	}

	@Override
	public boolean checkNotModified(String etag) {
		HttpServletResponse response = getResponse();
		if (StringUtils.hasLength(etag) && !this.notModified) {
			if (isCompatibleWithConditionalRequests(response)) {
				etag = addEtagPadding(etag);
				this.notModified = isEtagNotModified(etag);
				if (response != null) {
					if (this.notModified && supportsNotModifiedStatus()) {
						response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
					}
					if (isHeaderAbsent(response, HEADER_ETAG)) {
						response.setHeader(HEADER_ETAG, etag);
					}
				}
			}
		}
		return this.notModified;
	}

	@Override
	public boolean checkNotModified(String etag, long lastModifiedTimestamp) {
		HttpServletResponse response = getResponse();
		if (StringUtils.hasLength(etag) && !this.notModified) {
			if (isCompatibleWithConditionalRequests(response)) {
				etag = addEtagPadding(etag);
				this.notModified = isEtagNotModified(etag) && isTimestampNotModified(lastModifiedTimestamp);
				if (response != null) {
					if (supportsNotModifiedStatus()) {
						if (this.notModified) {
							response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
						}
						if (isHeaderAbsent(response, HEADER_ETAG)) {
							response.setHeader(HEADER_ETAG, etag);
						}
						if (isHeaderAbsent(response, HEADER_LAST_MODIFIED)) {
							response.setDateHeader(HEADER_LAST_MODIFIED, lastModifiedTimestamp);
						}
					}
					else if (supportsConditionalUpdate()) {
						if (this.notModified) {
							response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
						}
					}
				}
			}
		}
		return this.notModified;
	}

	public boolean isNotModified() {
		return this.notModified;
	}


	private boolean isCompatibleWithConditionalRequests(HttpServletResponse response) {
		try {
			if (response == null || !servlet3Present) {
				// Can't check response.getStatus() - let's assume we're good
				return true;
			}
			return HttpStatus.valueOf(response.getStatus()).is2xxSuccessful();
		}
		catch (IllegalArgumentException e) {
			return true;
		}
	}

	private boolean isHeaderAbsent(HttpServletResponse response, String header) {
		if (response == null || !servlet3Present) {
			// Can't check response.getHeader(header) - let's assume it's not set
			return true;
		}
		return (response.getHeader(header) == null);
	}

	private boolean supportsNotModifiedStatus() {
		String method = getRequest().getMethod();
		return (METHOD_GET.equals(method) || METHOD_HEAD.equals(method));
	}

	private boolean supportsConditionalUpdate() {
		String method = getRequest().getMethod();
		String ifUnmodifiedHeader = getRequest().getHeader(HEADER_IF_UNMODIFIED_SINCE);
		return (METHOD_POST.equals(method) || METHOD_PUT.equals(method) || METHOD_DELETE.equals(method))
				&& StringUtils.hasLength(ifUnmodifiedHeader);
	}

	private boolean isTimestampNotModified(long lastModifiedTimestamp) {
		long ifModifiedSince = parseDateHeader(HEADER_IF_MODIFIED_SINCE);
		if (ifModifiedSince != -1) {
			return (ifModifiedSince >= (lastModifiedTimestamp / 1000 * 1000));
		}
		long ifUnmodifiedSince = parseDateHeader(HEADER_IF_UNMODIFIED_SINCE);
		if (ifUnmodifiedSince != -1) {
			return (ifUnmodifiedSince < (lastModifiedTimestamp / 1000 * 1000));
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	private long parseDateHeader(String headerName) {
		long dateValue = -1;
		try {
			dateValue = getRequest().getDateHeader(headerName);
		}
		catch (IllegalArgumentException ex) {
			String headerValue = getRequest().getHeader(headerName);
			// Possibly an IE 10 style value: "Wed, 09 Apr 2014 09:57:42 GMT; length=13774"
			int separatorIndex = headerValue.indexOf(';');
			if (separatorIndex != -1) {
				String datePart = headerValue.substring(0, separatorIndex);
				try {
					dateValue = Date.parse(datePart);
				}
				catch (IllegalArgumentException ex2) {
					// Giving up
				}
			}
		}
		return dateValue;
	}

	private boolean isEtagNotModified(String etag) {
		if (StringUtils.hasLength(etag)) {
			String ifNoneMatch = getRequest().getHeader(HEADER_IF_NONE_MATCH);
			if (StringUtils.hasLength(ifNoneMatch)) {
				String[] clientEtags = StringUtils.delimitedListToStringArray(ifNoneMatch, ",", " ");
				for (String clientEtag : clientEtags) {
					// compare weak/strong ETag as per https://tools.ietf.org/html/rfc7232#section-2.3
					if (StringUtils.hasLength(clientEtag) &&
							(clientEtag.replaceFirst("^W/", "").equals(etag.replaceFirst("^W/", "")) ||
									clientEtag.equals("*"))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private String addEtagPadding(String etag) {
		if (!(etag.startsWith("\"") || etag.startsWith("W/\"")) || !etag.endsWith("\"")) {
			etag = "\"" + etag + "\"";
		}
		return etag;
	}


	@Override
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
