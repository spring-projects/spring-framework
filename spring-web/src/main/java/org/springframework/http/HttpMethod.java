/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.http;

import java.util.HashMap;
import java.util.Map;

/**
 * Java 5 enumeration of HTTP request methods. Intended for use
 * with {@link org.springframework.http.client.ClientHttpRequest}
 * and {@link org.springframework.web.client.RestTemplate}.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public enum HttpMethod {

	GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE;


	private static final Map<String, HttpMethod> mappings = new HashMap<String, HttpMethod>(8);

	static {
		for (HttpMethod httpMethod : values()) {
			mappings.put(httpMethod.name(), httpMethod);
		}
	}


	/**
	 * Resolve the given method value to an {@code HttpMethod}.
	 * @param method the method value as a String
	 * @return the corresponding {@code HttpMethod}, or {@code null} if not found
	 * @since 4.2.4
	 */
	public static HttpMethod resolve(String method) {
		return (method != null ? mappings.get(method) : null);
	}


	/**
	 * Determine whether this {@code HttpMethod} matches the given
	 * method value.
	 * @param method the method value as a String
	 * @return {@code true} if it matches, {@code false} otherwise
	 * @since 4.2.4
	 */
	public boolean matches(String method) {
		return name().equals(method);
	}

}
