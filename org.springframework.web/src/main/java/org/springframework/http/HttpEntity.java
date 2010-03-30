/*
 * Copyright 2002-2010 the original author or authors.
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

import java.util.Map;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Represents an HTTP request or response entity, consisting of headers and body.
 *
 * <p>Typically used in combination with the {@link org.springframework.web.client.RestTemplate RestTemplate}, like so:
 * <pre class="code">
 * HttpEntity&lt;String&gt; entity = new HttpEntity&lt;String&gt;(helloWorld, MediaType.TEXT_PLAIN);
 * URI location = template.postForLocation("http://example.com", entity);
 * </pre>
 * or
 * <pre class="code">
 * HttpEntity&lt;String&gt; entity = template.getForEntity("http://example.com", String.class);
 * String body = entity.getBody();
 * MediaType contentType = entity.getHeaders().getContentType();
 * </pre>
 *
 * @author Arjen Poutsma
 * @since 3.0.2
 * @see org.springframework.web.client.RestTemplate
 * @see #getBody()
 * @see #getHeaders()
 */
public class HttpEntity<T> {

	/**
	 * The empty {@code HttpEntity}, with no body or headers.
	 */
	public static final HttpEntity EMPTY = new HttpEntity();


	private final HttpHeaders headers;

	private final T body;


	/**
	 * Create a new, empty {@code HttpEntity}.
	 */
	private HttpEntity() {
		this(null, (MultiValueMap<String, String>) null);
	}

	/**
	 * Create a new {@code HttpEntity} with the given body and no headers.
	 * @param body the entity body
	 */
	public HttpEntity(T body) {
		this(body, (MultiValueMap<String, String>) null);
	}

	/**
	 * Create a new {@code HttpEntity} with the given headers and no body.
	 * @param headers the entity headers
	 */
	public HttpEntity(Map<String, String> headers) {
		this(null, toMultiValueMap(headers));
	}

	/**
	 * Create a new {@code HttpEntity} with the given headers and no body.
	 * @param headers the entity headers
	 */
	public HttpEntity(MultiValueMap<String, String> headers) {
		this(null, headers);
	}

	/**
	 * Create a new {@code HttpEntity} with the given body and {@code Content-Type} header value.
	 * @param body the entity body
	 * @param contentType the value of the {@code Content-Type header}
	 */
	public HttpEntity(T body, MediaType contentType) {
		this(body, toMultiValueMap(contentType));
	}

	/**
	 * Create a new {@code HttpEntity} with the given body and headers.
	 * @param body the entity body
	 * @param headers the entity headers
	 */
	public HttpEntity(T body, Map<String, String> headers) {
		this(body, toMultiValueMap(headers));
	}

	/**
	 * Create a new {@code HttpEntity} with the given body and headers.
	 * @param body the entity body
	 * @param headers the entity headers
	 */
	public HttpEntity(T body, MultiValueMap<String, String> headers) {
		this.body = body;
		HttpHeaders tempHeaders = new HttpHeaders();
		if (headers != null) {
			tempHeaders.putAll(headers);
		}
		this.headers = HttpHeaders.readOnlyHttpHeaders(tempHeaders);
	}


	/**
	 * Returns the headers of this entity.
	 */
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	/**
	 * Returns the body of this entity.
	 */
	public T getBody() {
		return this.body;
	}

	/**
	 * Indicates whether this entity has a body.
	 */
	public boolean hasBody() {
		return (this.body != null);
	}


	private static MultiValueMap<String, String> toMultiValueMap(Map<String, String> map) {
		if (map == null) {
			return null;
		}
		else {
			MultiValueMap<String, String> result = new LinkedMultiValueMap<String, String>(map.size());
			result.setAll(map);
			return result;
		}
	}

	private static MultiValueMap<String, String> toMultiValueMap(MediaType contentType) {
		if (contentType == null) {
			return null;
		}
		else {
			HttpHeaders result = new HttpHeaders();
			result.setContentType(contentType);
			return result;
		}
	}

}
