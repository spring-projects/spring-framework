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

import org.springframework.util.MultiValueMap;

/**
 * Extension of {@link HttpEntity} that adds a {@link HttpStatus} status code.
 *
 * <p>Returned by {@link org.springframework.web.client.RestTemplate#getForEntity}:
 * <pre class="code">
 * ResponseEntity&lt;String&gt; entity = template.getForEntity("http://example.com", String.class);
 * String body = entity.getBody();
 * MediaType contentType = entity.getHeaders().getContentType();
 * HttpStatus statusCode = entity.getStatusCode();
 * </pre>
 * <p>Can also be used in Spring MVC, as a return value from a @Controller method:
 * <pre class="code">
 * &#64;RequestMapping("/handle")
 * public ResponseEntity&lt;String&gt; handle() {
 *   HttpHeaders responseHeaders = new HttpHeaders();
 *   responseHeaders.set("MyResponseHeader", "MyValue");
 *   return new ResponseEntity&lt;String&gt;("Hello World", responseHeaders, HttpStatus.CREATED);
 * }
 * </pre>
 *
 * @author Arjen Poutsma
 * @since 3.0.2
 * @see #getStatusCode()
 */
public class ResponseEntity<T> extends HttpEntity<T> {

	private final HttpStatus statusCode;

	/**
	 * Create a new {@code ResponseEntity} with the given status code, and no body nor headers.
	 * @param statusCode the status code
	 */
	public ResponseEntity(HttpStatus statusCode) {
		super();
		this.statusCode = statusCode;
	}

	/**
	 * Create a new {@code ResponseEntity} with the given body and status code, and no headers.
	 * @param body the entity body
	 * @param statusCode the status code
	 */
	public ResponseEntity(T body, HttpStatus statusCode) {
		super(body);
		this.statusCode = statusCode;
	}

	/**
	 * Create a new {@code HttpEntity} with the given headers and status code, and no body.
	 * @param headers the entity headers
	 * @param statusCode the status code
	 */
	public ResponseEntity(MultiValueMap<String, String> headers, HttpStatus statusCode) {
		super(headers);
		this.statusCode = statusCode;
	}

	/**
	 * Create a new {@code HttpEntity} with the given body, headers, and status code.
	 * @param body the entity body
	 * @param headers the entity headers
	 * @param statusCode the status code
	 */
	public ResponseEntity(T body, MultiValueMap<String, String> headers, HttpStatus statusCode) {
		super(body, headers);
		this.statusCode = statusCode;
	}

	/**
	 * Return the HTTP status code of the response.
	 * @return the HTTP status as an HttpStatus enum value
	 */
	public HttpStatus getStatusCode() {
		return statusCode;
	}
}
