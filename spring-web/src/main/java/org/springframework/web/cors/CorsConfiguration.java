/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.cors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpMethod;

/**
 * A container for CORS configuration also providing methods to check actual or
 * or requested origin, HTTP method, and headers.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.2
 * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommandation</a>
 */
public class CorsConfiguration {

	private List<String> allowedOrigins;

	private List<String> allowedMethods;

	private List<String> allowedHeaders;

	private List<String> exposedHeaders;

	private Boolean allowCredentials;

	private Long maxAge;


	/**
	 * Default constructor.
	 */
	public CorsConfiguration() {
	}

	/**
	 * Copy constructor.
	 */
	public CorsConfiguration(CorsConfiguration other) {
		this.allowedOrigins = other.allowedOrigins;
		this.allowedMethods = other.allowedMethods;
		this.allowedHeaders = other.allowedHeaders;
		this.exposedHeaders = other.exposedHeaders;
		this.allowCredentials = other.allowCredentials;
		this.maxAge = other.maxAge;
	}

	/**
	 * Combine the specified {@link CorsConfiguration} with this one.
	 * Properties of this configuration are overridden only by non-null properties
	 * of the provided one.
	 * @return the combined {@link CorsConfiguration}
	 */
	public CorsConfiguration combine(CorsConfiguration other) {
		if (other == null) {
			return this;
		}
		CorsConfiguration config = new CorsConfiguration(this);
		config.setAllowedOrigins(combine(this.getAllowedOrigins(), other.getAllowedOrigins()));
		config.setAllowedMethods(combine(this.getAllowedMethods(), other.getAllowedMethods()));
		config.setAllowedHeaders(combine(this.getAllowedHeaders(), other.getAllowedHeaders()));
		config.setExposedHeaders(combine(this.getExposedHeaders(), other.getExposedHeaders()));
		Boolean allowCredentials = other.getAllowCredentials();
		if (allowCredentials != null) {
			config.setAllowCredentials(allowCredentials);
		}
		Long maxAge = other.getMaxAge();
		if (maxAge != null) {
			config.setMaxAge(maxAge);
		}
		return config;
	}

	private List<String> combine(List<String> source, List<String> other) {
		if (other == null) {
			return source;
		}
		if (source == null || source.contains("*")) {
			return other;
		}
		List<String> combined = new ArrayList<String>(source);
		combined.addAll(other);
		return combined;
	}

	/**
	 * Configure origins to allow, e.g. "http://domain1.com". The special value
	 * "*" allows all domains.
	 * <p>By default this is not set.
	 */
	public void setAllowedOrigins(List<String> origins) {
		this.allowedOrigins = origins;
	}

	/**
	 * Add an origin to allow.
	 */
	public void addAllowedOrigin(String origin) {
		if (this.allowedOrigins == null) {
			this.allowedOrigins = new ArrayList<String>();
		}
		this.allowedOrigins.add(origin);
	}

	/**
	 * Return the configured origins to allow, possibly {@code null}.
	 */
	public List<String> getAllowedOrigins() {
		return this.allowedOrigins;
	}

	/**
	 * Configure HTTP methods to allow, e.g. "GET", "POST", "PUT". The special
	 * value "*" allows all method. When not set only "GET is allowed.
	 * <p>By default this is not set.
	 */
	public void setAllowedMethods(List<String> methods) {
		this.allowedMethods = methods;
	}

	/**
	 * Add an HTTP method to allow.
	 */
	public void addAllowedMethod(String method) {
		if (this.allowedMethods == null) {
			this.allowedMethods = new ArrayList<String>();
		}
		this.allowedMethods.add(method);
	}

	/**
	 * Return the allowed HTTP methods, possibly {@code null} in which case only
	 * HTTP GET is allowed.
	 */
	public List<String> getAllowedMethods() {
		return this.allowedMethods;
	}

	/**
	 * Configure the list of headers that a pre-flight request can list as allowed
	 * for use during an actual request. The special value of "*" allows actual
	 * requests to send any header. A header name is not required to be listed if
	 * it is one of: Cache-Control, Content-Language, Expires, Last-Modified, Pragma.
	 * <p>By default this is not set.
	 */
	public void setAllowedHeaders(List<String> allowedHeaders) {
		this.allowedHeaders = allowedHeaders;
	}

	/**
	 * Add one actual request header to allow.
	 */
	public void addAllowedHeader(String allowedHeader) {
		if (this.allowedHeaders == null) {
			this.allowedHeaders = new ArrayList<String>();
		}
		this.allowedHeaders.add(allowedHeader);
	}

	/**
	 * Return the allowed actual request headers, possibly {@code null}.
	 */
	public List<String> getAllowedHeaders() {
		return this.allowedHeaders;
	}

	/**
	 * Configure the list of response headers other than simple headers (i.e.
	 * Cache-Control, Content-Language, Content-Type, Expires, Last-Modified,
	 * Pragma) that an actual response might have and can be exposed.
	 * <p>By default this is not set.
	 */
	public void setExposedHeaders(List<String> exposedHeaders) {
		if (exposedHeaders != null && exposedHeaders.contains("*")) {
			throw new IllegalArgumentException("'*' is not a valid exposed header value");
		}
		this.exposedHeaders = exposedHeaders;
	}

	/**
	 * Add a single response header to expose.
	 */
	public void addExposedHeader(String exposedHeader) {
		if ("*".equals(exposedHeader)) {
			throw new IllegalArgumentException("'*' is not a valid exposed header value");
		}
		if (this.exposedHeaders == null) {
			this.exposedHeaders = new ArrayList<String>();
		}
		this.exposedHeaders.add(exposedHeader);
	}

	/**
	 * Return the configured response headers to expose, possibly {@code null}.
	 */
	public List<String> getExposedHeaders() {
		return this.exposedHeaders;
	}

	/**
	 * Whether user credentials are supported.
	 * <p>By default this is not set (i.e. user credentials not supported).
	 */
	public void setAllowCredentials(Boolean allowCredentials) {
		this.allowCredentials = allowCredentials;
	}

	/**
	 * Return the configured allowCredentials, possibly {@code null}.
	 */
	public Boolean getAllowCredentials() {
		return this.allowCredentials;
	}

	/**
	 * Configure how long, in seconds, the response from a pre-flight request
	 * can be cached by clients.
	 * <p>By default this is not set.
	 */
	public void setMaxAge(Long maxAge) {
		this.maxAge = maxAge;
	}

	/**
	 * Return the configure maxAge value, possibly {@code null}.
	 */
	public Long getMaxAge() {
		return maxAge;
	}


	/**
	 * Check the origin of the request against the configured allowed origins.
	 * @param requestOrigin the origin to check.
	 * @return the origin to use for the response, possibly {@code null} which
	 * means the request origin is not allowed.
	 */
	public String checkOrigin(String requestOrigin) {
		if (requestOrigin == null) {
			return null;
		}
		List<String> allowedOrigins = this.allowedOrigins == null ?
				new ArrayList<String>() : this.allowedOrigins;
		if (allowedOrigins.contains("*")) {
			if (this.allowCredentials == null || !this.allowCredentials) {
				return "*";
			} else {
				return requestOrigin;
			}
		}
		for (String allowedOrigin : allowedOrigins) {
			if (requestOrigin.equalsIgnoreCase(allowedOrigin)) {
				return requestOrigin;
			}
		}
		return null;
	}

	/**
	 * Check the request HTTP method (or the method from the
	 * Access-Control-Request-Method header on a pre-flight request) against the
	 * configured allowed methods.
	 * @param requestMethod the HTTP method to check.
	 * @return the list of HTTP methods to list in the response of a pre-flight
	 * request, or {@code null} if the requestMethod is not allowed.
	 */
	public List<HttpMethod> checkHttpMethod(HttpMethod requestMethod) {
		if (requestMethod == null) {
			return null;
		}
		List<String> allowedMethods = this.allowedMethods == null ?
				new ArrayList<String>() : this.allowedMethods;
		if (allowedMethods.contains("*")) {
			return Arrays.asList(requestMethod);
		}
		if (allowedMethods.isEmpty()) {
			allowedMethods.add(HttpMethod.GET.name());
		}
		List<HttpMethod> result = new ArrayList<HttpMethod>(allowedMethods.size());
		boolean allowed = false;
		for (String method : allowedMethods) {
			if (method.equals(requestMethod.name())) {
				allowed = true;
			}
			result.add(HttpMethod.valueOf(method));
		}
		return allowed ? result : null;
	}

	/**
	 * Check the request headers (or the headers listed in the
	 * Access-Control-Request-Headers of a pre-flight request) against the
	 * configured allowed headers.
	 * @param requestHeaders the headers to check.
	 * @return the list of allowed headers to list in the response of a pre-flight
	 * request, or {@code null} if a requestHeader is not allowed.
	 */
	public List<String> checkHeaders(List<String> requestHeaders) {
		if (requestHeaders == null) {
			return null;
		}
		if (requestHeaders.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> allowedHeaders = this.allowedHeaders == null ?
				new ArrayList<String>() : this.allowedHeaders;
		boolean allowAnyHeader = allowedHeaders.contains("*");
		List<String> result = new ArrayList<String>();
		for (String requestHeader : requestHeaders) {
			requestHeader = requestHeader.trim();
			for (String allowedHeader : allowedHeaders) {
				if (allowAnyHeader || requestHeader.equalsIgnoreCase(allowedHeader)) {
					result.add(requestHeader);
					break;
				}
			}
		}
		return result.isEmpty() ? null : result;
	}

}
