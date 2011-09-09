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

import org.springframework.beans.BeanUtils;

/**
 * A FlashMap provides a way for one request to store attributes intended for 
 * use in another. This is most commonly needed when redirecting from one URL
 * to another -- e.g. the Post/Redirect/Get pattern. A FlashMap is saved before 
 * the redirect (typically in the session) and is made available after the 
 * redirect and removed immediately.
 * 
 * <p>A FlashMap can be set up with a request path and request parameters to 
 * help identify the target request. Without this information, a FlashMap is
 * made available to the next request, which may or may not be the intended 
 * recipient. On a redirect, the target URL is known and for example
 * {@code org.springframework.web.servlet.view.RedirectView} has the
 * opportunity to automatically update the current FlashMap with target
 * URL information .
 * 
 * <p>Annotated controllers will usually not use this type directly.
 * See {@code org.springframework.web.servlet.mvc.support.RedirectAttributes}
 * for an overview of using flash attributes in annotated controllers. 
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 * 
 * @see FlashMapManager
 */
public class FlashMap extends HashMap<String, Object> implements Comparable<FlashMap> {

	private static final long serialVersionUID = 1L;
	
	private String targetRequestPath;
	
	private final Map<String, String> targetRequestParams = new LinkedHashMap<String, String>();
	
	private long expirationStartTime;
	
	private int timeToLive;

	private final int createdBy;

	/**
	 * Create a new instance with an id uniquely identifying the creator of 
	 * this FlashMap.
	 */
	public FlashMap(int createdBy) {
		this.createdBy = createdBy;
	}
	
	/**
	 * Create a new instance.
	 */
	public FlashMap() {
		this.createdBy = 0;
	}

	/**
	 * Provide a URL path to help identify the target request for this FlashMap.
	 * The path may be absolute (e.g. /application/resource) or relative to the
	 * current request (e.g. ../resource).
	 * @param path the URI path
	 */
	public void setTargetRequestPath(String path) {
		this.targetRequestPath = path;
	}

	/**
	 * Return the target URL path or {@code null}.
	 */
	public String getTargetRequestPath() {
		return targetRequestPath;
	}

	/**
	 * Provide request parameter pairs to identify the request for this FlashMap. 
	 * Only simple type, non-null parameter values are used.
	 * @param params a Map with the names and values of expected parameters.
	 * @see BeanUtils#isSimpleValueType(Class)
	 */
	public FlashMap addTargetRequestParams(Map<String, ?> params) {
		if (params != null) {
			for (String name : params.keySet()) {
				Object value = params.get(name);
				if ((value != null) && BeanUtils.isSimpleValueType(value.getClass())) {
					this.targetRequestParams.put(name, value.toString());
				}
			}
		}
		return this;
	}

	/**
	 * Provide a request parameter to identify the request for this FlashMap.
	 * @param name the name of the expected parameter, never {@code null}
	 * @param value the value for the expected parameter, never {@code null}
	 */
	public FlashMap addTargetRequestParam(String name, String value) {
		this.targetRequestParams.put(name, value.toString());
		return this;
	}
	
	/**
	 * Return the parameters identifying the target request, or an empty Map.
	 */
	public Map<String, String> getTargetRequestParams() {
		return targetRequestParams;
	}

	/**
	 * Start the expiration period for this instance.
	 * @param timeToLive the number of seconds before expiration
	 */
	public void startExpirationPeriod(int timeToLive) {
		this.expirationStartTime = System.currentTimeMillis();
		this.timeToLive = timeToLive;
	}

	/**
	 * Whether this instance has expired depending on the amount of elapsed
	 * time since the call to {@link #startExpirationPeriod}.
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
	 * Whether the given id matches the id of the creator of this FlashMap.
	 */
	public boolean isCreatedBy(int createdBy) {
		return this.createdBy == createdBy;
	}

	/**
	 * Compare two FlashMaps and prefer the one that specifies a target URL 
	 * path or has more target URL parameters. Before comparing FlashMap
	 * instances ensure that they match a given request.
	 */
	public int compareTo(FlashMap other) {
		int thisUrlPath = (this.targetRequestPath != null) ? 1 : 0;
		int otherUrlPath = (other.targetRequestPath != null) ? 1 : 0;
		if (thisUrlPath != otherUrlPath) {
			return otherUrlPath - thisUrlPath;
		}
		else {
			return other.targetRequestParams.size() - this.targetRequestParams.size();
		}
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("[Attributes=").append(super.toString());
		result.append(", targetRequestPath=").append(this.targetRequestPath);
		result.append(", targetRequestParams=" + this.targetRequestParams.toString()).append("]");
		return result.toString();
	}

}
