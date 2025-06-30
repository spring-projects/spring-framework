/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.http.client.reactive;

import java.net.URI;
import java.util.Map;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.util.MultiValueMap;

/**
 * Represents a client-side reactive HTTP request.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 5.0
 */
public interface ClientHttpRequest extends ReactiveHttpOutputMessage {

	/**
	 * Return the HTTP method of the request.
	 */
	HttpMethod getMethod();

	/**
	 * Return the URI of the request.
	 */
	URI getURI();

	/**
	 * Return a mutable map of request cookies to send to the server.
	 */
	MultiValueMap<String, HttpCookie> getCookies();

	/**
	 * Return a mutable map of the request attributes.
	 * @since 6.2
	 */
	Map<String, Object> getAttributes();

	/**
	 * Return the request from the underlying HTTP library.
	 * @param <T> the expected type of the request to cast to
	 * @since 5.3
	 */
	<T> T getNativeRequest();

}
