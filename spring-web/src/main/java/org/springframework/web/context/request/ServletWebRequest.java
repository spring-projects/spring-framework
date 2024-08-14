/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.context.request;

import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.ETag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * {@link WebRequest} adapter for an {@link jakarta.servlet.http.HttpServletRequest}.
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author Markus Malkusch
 * @since 2.0
 */
public class ServletWebRequest extends ServletRequestAttributes implements NativeWebRequest {

	private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD");

	/**
	 * Date formats as specified in the HTTP RFC.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">Section 7.1.1.1 of RFC 7231</a>
	 */
	private static final String[] DATE_FORMATS = new String[] {
			"EEE, dd MMM yyyy HH:mm:ss zzz",
			"EEE, dd-MMM-yy HH:mm:ss zzz",
			"EEE MMM dd HH:mm:ss yyyy"
	};

	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

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
	public ServletWebRequest(HttpServletRequest request, @Nullable HttpServletResponse response) {
		super(request, response);
	}


	@Override
	public Object getNativeRequest() {
		return getRequest();
	}

	@Override
	@Nullable
	public Object getNativeResponse() {
		return getResponse();
	}

	@Override
	@Nullable
	public <T> T getNativeRequest(@Nullable Class<T> requiredType) {
		return WebUtils.getNativeRequest(getRequest(), requiredType);
	}

	@Override
	@Nullable
	public <T> T getNativeResponse(@Nullable Class<T> requiredType) {
		HttpServletResponse response = getResponse();
		return (response != null ? WebUtils.getNativeResponse(response, requiredType) : null);
	}

	/**
	 * Return the HTTP method of the request.
	 * @since 4.0.2
	 */
	public HttpMethod getHttpMethod() {
		return HttpMethod.valueOf(getRequest().getMethod());
	}

	@Override
	@Nullable
	public String getHeader(String headerName) {
		return getRequest().getHeader(headerName);
	}

	@Override
	@Nullable
	public String[] getHeaderValues(String headerName) {
		String[] headerValues = StringUtils.toStringArray(getRequest().getHeaders(headerName));
		return (!ObjectUtils.isEmpty(headerValues) ? headerValues : null);
	}

	@Override
	public Iterator<String> getHeaderNames() {
		return CollectionUtils.toIterator(getRequest().getHeaderNames());
	}

	@Override
	@Nullable
	public String getParameter(String paramName) {
		return getRequest().getParameter(paramName);
	}

	@Override
	@Nullable
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
	@Nullable
	public String getRemoteUser() {
		return getRequest().getRemoteUser();
	}

	@Override
	@Nullable
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
		return checkNotModified(null, lastModifiedTimestamp);
	}

	@Override
	public boolean checkNotModified(String etag) {
		return checkNotModified(etag, -1);
	}

	@Override
	public boolean checkNotModified(@Nullable String etag, long lastModifiedTimestamp) {
		HttpServletResponse response = getResponse();
		if (this.notModified || (response != null && HttpStatus.OK.value() != response.getStatus())) {
			return this.notModified;
		}
		// Evaluate conditions in order of precedence.
		// See https://datatracker.ietf.org/doc/html/rfc9110#section-13.2.2
		if (validateIfMatch(etag)) {
			updateResponseStateChanging(etag, lastModifiedTimestamp);
			return this.notModified;
		}
		// 2) If-Unmodified-Since
		else if (validateIfUnmodifiedSince(lastModifiedTimestamp)) {
			updateResponseStateChanging(etag, lastModifiedTimestamp);
			return this.notModified;
		}
		// 3) If-None-Match
		if (!validateIfNoneMatch(etag)) {
			// 4) If-Modified-Since
			validateIfModifiedSince(lastModifiedTimestamp);
		}
		updateResponseIdempotent(etag, lastModifiedTimestamp);
		return this.notModified;
	}

	private boolean validateIfMatch(@Nullable String etag) {
		if (SAFE_METHODS.contains(getRequest().getMethod())) {
			return false;
		}
		Enumeration<String> ifMatchHeaders = getRequest().getHeaders(HttpHeaders.IF_MATCH);
		if (!ifMatchHeaders.hasMoreElements()) {
			return false;
		}
		this.notModified = matchRequestedETags(ifMatchHeaders, etag, false);
		return true;
	}

	private boolean validateIfNoneMatch(@Nullable String etag) {
		Enumeration<String> ifNoneMatchHeaders = getRequest().getHeaders(HttpHeaders.IF_NONE_MATCH);
		if (!ifNoneMatchHeaders.hasMoreElements()) {
			return false;
		}
		this.notModified = !matchRequestedETags(ifNoneMatchHeaders, etag, true);
		return true;
	}

	private boolean matchRequestedETags(Enumeration<String> requestedETags, @Nullable String etag, boolean weakCompare) {
		etag = padEtagIfNecessary(etag);
		while (requestedETags.hasMoreElements()) {
			// Compare weak/strong ETags as per https://datatracker.ietf.org/doc/html/rfc9110#section-8.8.3
			for (ETag requestedETag : ETag.parse(requestedETags.nextElement())) {
				// only consider "lost updates" checks for unsafe HTTP methods
				if (requestedETag.isWildcard() && StringUtils.hasLength(etag)
						&& !SAFE_METHODS.contains(getRequest().getMethod())) {
					return false;
				}
				if (weakCompare) {
					if (etagWeakMatch(etag, requestedETag.formattedTag())) {
						return false;
					}
				}
				else {
					if (etagStrongMatch(etag, requestedETag.formattedTag())) {
						return false;
					}
				}
			}
		}
		return true;
	}

	@Nullable
	private String padEtagIfNecessary(@Nullable String etag) {
		if (!StringUtils.hasLength(etag)) {
			return etag;
		}
		if ((etag.startsWith("\"") || etag.startsWith("W/\"")) && etag.endsWith("\"")) {
			return etag;
		}
		return "\"" + etag + "\"";
	}

	private boolean etagStrongMatch(@Nullable String first, @Nullable String second) {
		if (!StringUtils.hasLength(first) || first.startsWith("W/")) {
			return false;
		}
		return first.equals(second);
	}

	private boolean etagWeakMatch(@Nullable String first, @Nullable String second) {
		if (!StringUtils.hasLength(first) || !StringUtils.hasLength(second)) {
			return false;
		}
		if (first.startsWith("W/")) {
			first = first.substring(2);
		}
		if (second.startsWith("W/")) {
			second = second.substring(2);
		}
		return first.equals(second);
	}

	private void updateResponseStateChanging(@Nullable String etag, long lastModifiedTimestamp) {
		if (this.notModified && getResponse() != null) {
			getResponse().setStatus(HttpStatus.PRECONDITION_FAILED.value());
		}
		else {
			addCachingResponseHeaders(etag, lastModifiedTimestamp);
		}
	}

	private boolean validateIfUnmodifiedSince(long lastModifiedTimestamp) {
		if (lastModifiedTimestamp < 0) {
			return false;
		}
		long ifUnmodifiedSince = parseDateHeader(HttpHeaders.IF_UNMODIFIED_SINCE);
		if (ifUnmodifiedSince == -1) {
			return false;
		}
		this.notModified = (ifUnmodifiedSince < (lastModifiedTimestamp / 1000 * 1000));
		return true;
	}

	private void validateIfModifiedSince(long lastModifiedTimestamp) {
		if (lastModifiedTimestamp < 0) {
			return;
		}
		long ifModifiedSince = parseDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
		if (ifModifiedSince != -1) {
			// We will perform this validation...
			this.notModified = ifModifiedSince >= (lastModifiedTimestamp / 1000 * 1000);
		}
	}

	private void updateResponseIdempotent(@Nullable String etag, long lastModifiedTimestamp) {
		if (getResponse() != null) {
			boolean isHttpGetOrHead = SAFE_METHODS.contains(getRequest().getMethod());
			if (this.notModified) {
				getResponse().setStatus(isHttpGetOrHead ?
						HttpStatus.NOT_MODIFIED.value() : HttpStatus.PRECONDITION_FAILED.value());
			}
			addCachingResponseHeaders(etag, lastModifiedTimestamp);
		}
	}

	private void addCachingResponseHeaders(@Nullable String etag, long lastModifiedTimestamp) {
		if (getResponse() != null && SAFE_METHODS.contains(getRequest().getMethod())) {
			if (lastModifiedTimestamp > 0 && parseDateValue(getResponse().getHeader(HttpHeaders.LAST_MODIFIED)) == -1) {
				getResponse().setDateHeader(HttpHeaders.LAST_MODIFIED, lastModifiedTimestamp);
			}
			if (StringUtils.hasLength(etag) && getResponse().getHeader(HttpHeaders.ETAG) == null) {
				getResponse().setHeader(HttpHeaders.ETAG, padEtagIfNecessary(etag));
			}
		}
	}

	public boolean isNotModified() {
		return this.notModified;
	}

	private long parseDateHeader(String headerName) {
		long dateValue = -1;
		try {
			dateValue = getRequest().getDateHeader(headerName);
		}
		catch (IllegalArgumentException ex) {
			String headerValue = getHeader(headerName);
			// Possibly an IE 10 style value: "Wed, 09 Apr 2014 09:57:42 GMT; length=13774"
			if (headerValue != null) {
				int separatorIndex = headerValue.indexOf(';');
				if (separatorIndex != -1) {
					String datePart = headerValue.substring(0, separatorIndex);
					dateValue = parseDateValue(datePart);
				}
			}
		}
		return dateValue;
	}

	private long parseDateValue(@Nullable String headerValue) {
		if (headerValue == null) {
			// No header value sent at all
			return -1;
		}
		if (headerValue.length() >= 3) {
			// Short "0" or "-1" like values are never valid HTTP date headers...
			// Let's only bother with SimpleDateFormat parsing for long enough values.
			for (String dateFormat : DATE_FORMATS) {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.US);
				simpleDateFormat.setTimeZone(GMT);
				try {
					return simpleDateFormat.parse(headerValue).getTime();
				}
				catch (ParseException ex) {
					// ignore
				}
			}
		}
		return -1;
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
