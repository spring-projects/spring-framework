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
import org.springframework.util.Assert;

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
 * result. Before a redirect, the target URL is known and when using the
 * {@code org.springframework.web.servlet.view.RedirectView}, FlashMap 
 * instances are automatically updated with redirect URL information.
 * 
 * <p>Annotated controllers will usually not access a FlashMap directly.. TODO
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
	 * Create a new instance.
	 */
	public FlashMap() {
		this.createdBy = 0;
	}

	/**
	 * Create a new instance with an id uniquely identifying the creator of 
	 * this FlashMap.
	 */
	public FlashMap(int createdBy) {
		this.createdBy = createdBy;
	}

	/**
	 * Provide a URL path to help identify the target request for this FlashMap.
	 * The path may be absolute (e.g. /application/resource) or relative to the
	 * current request (e.g. ../resource).
	 * @param path the URI path, never {@code null}
	 */
	public void setTargetRequestPath(String path) {
		Assert.notNull(path, "Expected path must not be null");
		this.targetRequestPath = path;
	}

	/**
	 * Return the URL path of the target request, or {@code null} if none.
	 */
	public String getTargetRequestPath() {
		return targetRequestPath;
	}

	/**
	 * Provide request parameter pairs to identify the request for this FlashMap. 
	 * If not set, the FlashMap will match to requests with any parameters.
	 * Only simple value types, as defined in {@link BeanUtils#isSimpleValueType}, 
	 * are used.
	 * @param params a Map with the names and values of expected parameters.
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
	 * If not set, the FlashMap will match to requests with any parameters.
	 * 
	 * @param name the name of the expected parameter (never {@code null})
	 * @param value the value for the expected parameter (never {@code null})
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
	 * Whether the given id matches the id of the creator of this FlashMap.
	 */
	public boolean isCreatedBy(int createdBy) {
		return this.createdBy == createdBy;
	}

	/**
	 * Compare two FlashMaps and select the one that has a target URL path or 
	 * has more target request parameters. 
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
		result.append(", expecteRequestUri=").append(this.targetRequestPath);
		result.append(", expectedRequestParameters=" + this.targetRequestParams.toString()).append("]");
		return result.toString();
	}

}
