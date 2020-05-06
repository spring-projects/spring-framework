/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet;

import java.util.HashMap;

import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
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
 * recipient. On a redirect, the target URL is known and a FlashMap can be
 * updated with that information. This is done automatically when the
 * {@code org.springframework.web.servlet.view.RedirectView} is used.
 *
 * <p>Note: annotated controllers will usually not use FlashMap directly.
 * See {@code org.springframework.web.servlet.mvc.support.RedirectAttributes}
 * for an overview of using flash attributes in annotated controllers.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see FlashMapManager
 */
@SuppressWarnings("serial")
public final class FlashMap extends HashMap<String, Object> implements Comparable<FlashMap> {

	@Nullable
	private String targetRequestPath;

	private final MultiValueMap<String, String> targetRequestParams = new LinkedMultiValueMap<>(4);

	private long expirationTime = -1;


	/**
	 * Provide a URL path to help identify the target request for this FlashMap.
	 * <p>The path may be absolute (e.g. "/application/resource") or relative to the
	 * current request (e.g. "../resource").
	 */
	public void setTargetRequestPath(@Nullable String path) {
		this.targetRequestPath = path;
	}

	/**
	 * Return the target URL path (or {@code null} if none specified).
	 */
	@Nullable
	public String getTargetRequestPath() {
		return this.targetRequestPath;
	}

	/**
	 * Provide request parameters identifying the request for this FlashMap.
	 * @param params a Map with the names and values of expected parameters
	 */
	public FlashMap addTargetRequestParams(@Nullable MultiValueMap<String, String> params) {
		if (params != null) {
			params.forEach((key, values) -> {
				for (String value : values) {
					addTargetRequestParam(key, value);
				}
			});
		}
		return this;
	}

	/**
	 * Provide a request parameter identifying the request for this FlashMap.
	 * @param name the expected parameter name (skipped if empty)
	 * @param value the expected value (skipped if empty)
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
		return this.targetRequestParams;
	}

	/**
	 * Start the expiration period for this instance.
	 * @param timeToLive the number of seconds before expiration
	 */
	public void startExpirationPeriod(int timeToLive) {
		this.expirationTime = System.currentTimeMillis() + timeToLive * 1000;
	}

	/**
	 * Set the expiration time for the FlashMap. This is provided for serialization
	 * purposes but can also be used instead {@link #startExpirationPeriod(int)}.
	 * @since 4.2
	 */
	public void setExpirationTime(long expirationTime) {
		this.expirationTime = expirationTime;
	}

	/**
	 * Return the expiration time for the FlashMap or -1 if the expiration
	 * period has not started.
	 * @since 4.2
	 */
	public long getExpirationTime() {
		return this.expirationTime;
	}

	/**
	 * Return whether this instance has expired depending on the amount of
	 * elapsed time since the call to {@link #startExpirationPeriod}.
	 */
	public boolean isExpired() {
		return (this.expirationTime != -1 && System.currentTimeMillis() > this.expirationTime);
	}


	/**
	 * Compare two FlashMaps and prefer the one that specifies a target URL
	 * path or has more target URL parameters. Before comparing FlashMap
	 * instances ensure that they match a given request.
	 */
	@Override
	public int compareTo(FlashMap other) {
		int thisUrlPath = (this.targetRequestPath != null ? 1 : 0);
		int otherUrlPath = (other.targetRequestPath != null ? 1 : 0);
		if (thisUrlPath != otherUrlPath) {
			return otherUrlPath - thisUrlPath;
		}
		else {
			return other.targetRequestParams.size() - this.targetRequestParams.size();
		}
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof FlashMap)) {
			return false;
		}
		FlashMap otherFlashMap = (FlashMap) other;
		return (super.equals(otherFlashMap) &&
				ObjectUtils.nullSafeEquals(this.targetRequestPath, otherFlashMap.targetRequestPath) &&
				this.targetRequestParams.equals(otherFlashMap.targetRequestParams));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.targetRequestPath);
		result = 31 * result + this.targetRequestParams.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "FlashMap [attributes=" + super.toString() + ", targetRequestPath=" +
				this.targetRequestPath + ", targetRequestParams=" + this.targetRequestParams + "]";
	}

}
