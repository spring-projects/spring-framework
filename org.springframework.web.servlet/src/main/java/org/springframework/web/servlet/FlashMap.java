/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * Stores attributes that need to be made available in the next request.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class FlashMap extends HashMap<String, Object> implements Comparable<FlashMap> {

	private static final long serialVersionUID = 1L;
	
	private String expectedRequestUri;
	
	private final Map<String, String> expectedRequestParameters = new LinkedHashMap<String, String>();
	
	private long expirationStartTime;
	
	private int timeToLive;

	private final UrlPathHelper urlPathHelper = new UrlPathHelper();
	
	/**
	 * Provide a URL to identify the target request for this FlashMap.
	 * Only the path of the provided URL will be used for matching purposes. 
	 * If the URL is absolute or has a query string, the URL path is 
	 * extracted. Or if the URL is relative, it is appended to the current 
	 * request URI and normalized.  
	 *  
	 * @param request the current request, used to normalize relative URLs
	 * @param url an absolute URL, a URL path, or a relative URL, never {@code null}
	 */
	public FlashMap setExpectedRequestUri(HttpServletRequest request, String url) {
		Assert.notNull(url, "Expected URL must not be null");
		String path = extractRequestUri(url);
		this.expectedRequestUri = path.startsWith("/") ? path : normalizeRelativePath(request, path);
		return this;
	}

	private String extractRequestUri(String url) {
		int index = url.indexOf("?");
		if (index != -1) {
			url = url.substring(0, index);
		}
		index = url.indexOf("://");
		if (index != -1) {
			int pathBegin = url.indexOf("/", index + 3);
			url = (pathBegin != -1 ) ? url.substring(pathBegin) : "";
		}
		return url;
	}

	private String normalizeRelativePath(HttpServletRequest request, String relativeUrl) {
		String requestUri = this.urlPathHelper.getRequestUri(request);
		relativeUrl = requestUri.substring(0, requestUri.lastIndexOf('/') + 1) + relativeUrl;
		return StringUtils.cleanPath(relativeUrl);
	}

	/**
	 * Add a request parameter pair to help identify the request this FlashMap 
	 * should be made available to. If expected parameters are not set, the 
	 * FlashMap instance will match to requests with any parameters.
	 * 
	 * @param name the name of the expected parameter (never {@code null})
	 * @param value the value for the expected parameter (never {@code null})
	 */
	public FlashMap setExpectedRequestParam(String name, String value) {
		this.expectedRequestParameters.put(name, value.toString());
		return this;
	}

	/**
	 * Provide request parameter pairs to help identify the request this FlashMap 
	 * should be made available to. If expected parameters are not set, the 
	 * FlashMap instance will match to requests with any parameters.
	 * 
	 * <p>Although the provided map contain any Object values, only non-"simple" 
	 * value types as defined in {@link BeanUtils#isSimpleValueType} are used.
	 * 
	 * @param params a Map with the names and values of expected parameters.
	 */
	public FlashMap setExpectedRequestParams(Map<String, ?> params) {
		if (params != null) {
			for (String name : params.keySet()) {
				Object value = params.get(name);
				if ((value != null) && BeanUtils.isSimpleValueType(value.getClass())) {
					this.expectedRequestParameters.put(name, value.toString());
				}
			}
		}
		return this;
	}

	/**
	 * Whether this FlashMap matches to the given request by checking 
	 * expectations provided via {@link #setExpectedRequestUri} and 
	 * {@link #setExpectedRequestParams}.
	 * 
	 * @param request the current request
	 * 
	 * @return "true" if the expectations match or there are no expectations.
	 */
	public boolean matches(HttpServletRequest request) {
		if (this.expectedRequestUri != null) {
			String requestUri = this.urlPathHelper.getRequestUri(request);
			if (!matchPathsIgnoreTrailingSlash(requestUri, this.expectedRequestUri)) {
				return false;
			}
		}
		if (this.expectedRequestParameters != null) {
			for (Map.Entry<String, String> entry : this.expectedRequestParameters.entrySet()) {
				if (!entry.getValue().equals(request.getParameter(entry.getKey()))) {
					return false;
				}
			}
		}
		return true;
	}
	
	private boolean matchPathsIgnoreTrailingSlash(String path1, String path2) {
		path1 = path1.endsWith("/") ? path1.substring(0, path1.length() - 1) : path1;
		path2 = path2.endsWith("/") ? path2.substring(0, path2.length() - 1) : path2;
		return path1.equals(path2);
	}
	
	/**
	 * Start the expiration period for this instance. After the given number of 
	 * seconds calls to {@link #isExpired()} will return "true".
	 * @param timeToLive the number of seconds before flash map expires
	 */
	public void startExpirationPeriod(int timeToLive) {
		this.expirationStartTime = System.currentTimeMillis();
		this.timeToLive = timeToLive;
	}

	/**
	 * Whether the flash map has expired depending on the number of seconds 
	 * elapsed since the call to {@link #startExpirationPeriod}.
	 */
	public boolean isExpired() {
		if (this.expirationStartTime != 0) {
			return (System.currentTimeMillis() - this.expirationStartTime) > this.timeToLive * 1000;
		}
		else {
			return false;
		}
	}

	/**
	 * Compare two FlashMap instances. One instance is preferred over the other
	 * if it has an expected URL path or if it has a greater number of expected 
	 * request parameters.
	 * 
	 * <p>It is expected that both instances have been matched against the 
	 * current request via {@link FlashMap#matches}.
	 */
	public int compareTo(FlashMap other) {
		int thisUrlPath = (this.expectedRequestUri != null) ? 1 : 0;
		int otherUrlPath = (other.expectedRequestUri != null) ? 1 : 0;
		if (thisUrlPath != otherUrlPath) {
			return otherUrlPath - thisUrlPath;
		}
		else {
			return other.expectedRequestParameters.size() - this.expectedRequestParameters.size();
		}
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("[Attributes=").append(super.toString());
		result.append(", expecteRequestUri=").append(this.expectedRequestUri);
		result.append(", expectedRequestParameters=" + this.expectedRequestParameters.toString()).append("]");
		return result.toString();
	}

}
