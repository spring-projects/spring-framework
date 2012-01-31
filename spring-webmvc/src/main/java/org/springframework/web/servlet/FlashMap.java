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

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

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
 * URL information.
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
public final class FlashMap extends HashMap<String, Object> implements Comparable<FlashMap> {

	private static final long serialVersionUID = 1L;
	
	private String targetRequestPath;
	
	private final MultiValueMap<String, String> targetRequestParams = new LinkedMultiValueMap<String, String>();
	
	private long expirationStartTime;
	
	private int timeToLive;

	private final int createdBy;

	/**
	 * Create a new instance with an id uniquely identifying the creator of 
	 * this FlashMap.
	 * @param createdBy identifies the FlashMapManager instance that created
	 * 	and will manage this FlashMap instance (e.g. via a hashCode)  
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
	 * Provide request parameters identifying the request for this FlashMap.
	 * Null or empty keys and values are skipped.
	 * @param params a Map with the names and values of expected parameters.
	 */
	public FlashMap addTargetRequestParams(MultiValueMap<String, String> params) {
		if (params != null) {
			for (String key : params.keySet()) {
				for (String value : params.get(key)) {
					addTargetRequestParam(key, value);
				}
			}
		}
		return this;
	}

	/**
	 * Provide a request parameter identifying the request for this FlashMap.
	 * @param name the expected parameter name, skipped if {@code null}
	 * @param value the expected parameter value, skipped if {@code null}
	 */
	public FlashMap addTargetRequestParam(String name, String value) {
		if (StringUtils.hasText(name) && StringUtils.hasText(value)) {
			this.targetRequestParams.add(name, value);
		}
		return this;
	}
	
	/**
	 * Return the parameters identifying the target request, or an empty map.
	 */
	public MultiValueMap<String, String> getTargetRequestParams() {
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
		StringBuilder sb = new StringBuilder();
		sb.append("[Attributes=").append(super.toString());
		sb.append(", targetRequestPath=").append(this.targetRequestPath);
		sb.append(", targetRequestParams=").append(this.targetRequestParams).append("]");
		return sb.toString();
	}

}
