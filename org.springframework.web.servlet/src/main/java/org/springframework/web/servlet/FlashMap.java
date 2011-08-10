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

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.BeanUtils;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * Stores attributes that need to be made available in the next request.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class FlashMap extends ModelMap implements Comparable<FlashMap> {

	private static final long serialVersionUID = 1L;
	
	private String expectedUrlPath;
	
	private Map<String, String> expectedRequestParameters = new LinkedHashMap<String, String>();
	
	private UrlPathHelper urlPathHelper = new UrlPathHelper();
	
	private long expirationStartTime;
	
	private int timeToLive;
	
	/**
	 * Provide a URL path to help identify the request this FlashMap should be
	 * made available to. This will usually be the target URL of a redirect. 
	 * If not set, this FlashMap will match to requests with any URL path.
	 * 
	 * <p>If the {@code url} parameter is not a URL path but is an absolute 
	 * or a relative URL, the unnecessary parts of the URL are removed the 
	 * resulting URL path is normalized.
	 * 
	 * @param request the current request
	 * @param url a URL path, an absolute URL, or a relative URL 
	 */
	public void setExpectedUrlPath(HttpServletRequest request, String url) {
		this.expectedUrlPath = (url != null) ? normalizeRelativeUrlPath(request, extractUrlPath(url)) : null;
	}

	private String extractUrlPath(String url) {
		int index = url.indexOf("://");
		if (index != -1) {
			index = url.indexOf("/", index + 3);
			url = (index != -1) ? url.substring(index) : "";
		}
		index = url.indexOf("?");
		return (index != -1) ? url.substring(0, index) : url;  
	}

	private String normalizeRelativeUrlPath(HttpServletRequest request, String path) {
		if (!path.startsWith("/")) {
			String requestUri = this.urlPathHelper.getRequestUri(request);
			path = requestUri.substring(0, requestUri.lastIndexOf('/') + 1) + path;
			path = StringUtils.cleanPath(path);
		}
		return path;
	}
	
	/**
	 * Provide request parameter pairs to help identify the request this FlashMap 
	 * should be made available to. If expected parameters are not set, this 
	 * FlashMap instance will match to requests with any parameters. 
	 * 
	 * <p>Although the provided map contain any Object values, only non-"simple" 
	 * value types as defined in {@link BeanUtils#isSimpleValueType} are used.
	 * 
	 * @param params a Map with the names and values of expected parameters.
	 */
	public void setExpectedRequestParameters(Map<String, ?> params) {
		this.expectedRequestParameters = new LinkedHashMap<String, String>();
		if (params != null) {
			for (String name : params.keySet()) {
				Object value = params.get(name);
				if ((value != null) && BeanUtils.isSimpleValueType(value.getClass())) {
					this.expectedRequestParameters.put(name, value.toString());
				}
			}
		}
	}
	
	/**
	 * Whether this FlashMap matches to the given request by checking 
	 * expectations provided via {@link #setExpectedUrlPath} and 
	 * {@link #setExpectedRequestParameters}.
	 * 
	 * @param request the current request
	 * 
	 * @return "true" if the expectations match or there are no expectations.
	 */
	public boolean matches(HttpServletRequest request) {
		if (this.expectedUrlPath != null) {
			if (!matchPathsIgnoreTrailingSlash(this.urlPathHelper.getRequestUri(request), this.expectedUrlPath)) {
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
		int thisUrlPath = (this.expectedUrlPath != null) ? 1 : 0;
		int otherUrlPath = (other.expectedUrlPath != null) ? 1 : 0;
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
		result.append(", expecteUrlPath=").append(this.expectedUrlPath);
		result.append(", expectedRequestParameters=" + this.expectedRequestParameters.toString()).append("]");
		return result.toString();
	}

}
