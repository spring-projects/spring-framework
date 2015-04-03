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

/**
 * Represents the CORS configuration that stores various properties used to check if a
 * CORS request is allowed and to generate CORS response headers.
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


	public CorsConfiguration() {
	}


	public CorsConfiguration(CorsConfiguration config) {
		if (config.allowedOrigins != null) {
			this.allowedOrigins = new ArrayList<String>(config.allowedOrigins);
		}
		if (config.allowCredentials != null) {
			this.allowCredentials = config.allowCredentials;
		}
		if (config.exposedHeaders != null) {
			this.exposedHeaders = new ArrayList<String>(config.exposedHeaders);
		}
		if (config.allowedMethods != null) {
			this.allowedMethods = new ArrayList<String>(config.allowedMethods);
		}
		if (config.allowedHeaders != null) {
			this.allowedHeaders = new ArrayList<String>(config.allowedHeaders);
		}
		if (config.maxAge != null) {
			this.maxAge = config.maxAge;
		}
	}

	public CorsConfiguration combine(CorsConfiguration other) {
		CorsConfiguration config = new CorsConfiguration(this);

		if (other.getAllowedOrigins() != null) {
			config.setAllowedOrigins(other.getAllowedOrigins());
		}
		if (other.getAllowedMethods() != null) {
			config.setAllowedMethods(other.getAllowedMethods());
		}
		if (other.getAllowedHeaders() != null) {
			config.setAllowedHeaders(other.getAllowedHeaders());
		}
		if (other.getExposedHeaders() != null) {
			config.setExposedHeaders(other.getExposedHeaders());
		}
		if (other.getMaxAge() != null) {
			config.setMaxAge(other.getMaxAge());
		}
		if (other.isAllowCredentials() != null) {
			config.setAllowCredentials(other.isAllowCredentials());
		}
		return config;
	}

	/**
	 * @see #setAllowedOrigins(java.util.List)
	 */
	public List<String> getAllowedOrigins() {
		if (this.allowedOrigins != null) {
			return this.allowedOrigins.contains("*") ? Arrays.asList("*") : Collections.unmodifiableList(this.allowedOrigins);
		}
		return null;
	}

	/**
	 * Set allowed allowedOrigins that will define Access-Control-Allow-Origin response
	 * header values (mandatory). For example "http://domain1.com", "http://domain2.com" ...
	 * "*" means that all domains are allowed.
	 */
	public void setAllowedOrigins(List<String> allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	/**
	 * @see #setAllowedOrigins(java.util.List)
	 */
	public void addAllowedOrigin(String allowedOrigin) {
		if (this.allowedOrigins == null) {
			this.allowedOrigins = new ArrayList<String>();
		}
		this.allowedOrigins.add(allowedOrigin);
	}

	/**
	 * @see #setAllowedMethods(java.util.List)
	 */
	public List<String> getAllowedMethods() {
		return this.allowedMethods == null ? null : Collections.unmodifiableList(this.allowedMethods);
	}

	/**
	 * Set allow methods that will define Access-Control-Allow-Methods response header
	 * values. For example "GET", "POST", "PUT" ... "*" means that all methods requested
	 * by the client are allowed. If not set, allowed method is set to "GET".
	 *
	 */
	public void setAllowedMethods(List<String> allowedMethods) {
		this.allowedMethods = allowedMethods;
	}

	/**
	 * @see #setAllowedMethods(java.util.List)
	 */
	public void addAllowedMethod(String allowedMethod) {
		if (this.allowedMethods == null) {
			this.allowedMethods = new ArrayList<String>();
		}
		this.allowedMethods.add(allowedMethod);
	}

	/**
	 * @see #setAllowedHeaders(java.util.List)
	 */
	public List<String> getAllowedHeaders() {
		return this.allowedHeaders == null ? null : Collections.unmodifiableList(this.allowedHeaders);
	}

	/**
	 * Set a list of request headers that will define Access-Control-Allow-Methods response
	 * header values. If a header field name is one of the following, it is not required
	 * to be listed: Cache-Control, Content-Language, Expires, Last-Modified, Pragma.
	 * "*" means that all headers asked by the client will be allowed.
	 */
	public void setAllowedHeaders(List<String> allowedHeaders) {
		this.allowedHeaders = allowedHeaders;
	}

	/**
	 * @see #setAllowedHeaders(java.util.List)
	 */
	public void addAllowedHeader(String allowedHeader) {
		if (this.allowedHeaders == null) {
			this.allowedHeaders = new ArrayList<String>();
		}
		this.allowedHeaders.add(allowedHeader);
	}

	/**
	 * @see #setExposedHeaders(java.util.List)
	 */
	public List<String> getExposedHeaders() {
		return this.exposedHeaders == null ? null : Collections.unmodifiableList(this.exposedHeaders);
	}

	/**
	 * Set a list of response headers other than simple headers that the resource might use
	 * and can be exposed. Simple response headers are: Cache-Control, Content-Language,
	 * Content-Type, Expires, Last-Modified, Pragma.
	 */
	public void setExposedHeaders(List<String> exposedHeaders) {
		this.exposedHeaders = exposedHeaders;
	}

	/**
	 * @see #setExposedHeaders(java.util.List)
	 */
	public void addExposedHeader(String exposedHeader) {
		if (this.exposedHeaders == null) {
			this.exposedHeaders = new ArrayList<String>();
		}
		this.exposedHeaders.add(exposedHeader);
	}

	/**
	 * @see #setAllowCredentials(Boolean)
	 */
	public Boolean isAllowCredentials() {
		return this.allowCredentials;
	}

	/**
	 * Indicates whether the resource supports user credentials.
	 * Set the value of Access-Control-Allow-Credentials response header.
	 */
	public void setAllowCredentials(Boolean allowCredentials) {
		this.allowCredentials = allowCredentials;
	}

	/**
	 * @see #setMaxAge(Long)
	 */
	public Long getMaxAge() {
		return maxAge;
	}

	/**
	 * Indicates how long (seconds) the results of a preflight request can be cached
	 * in a preflight result cache.
	 */
	public void setMaxAge(Long maxAge) {
		this.maxAge = maxAge;
	}

}
