/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.http;

import java.net.URI;

/**
 * Represents an HTTP request message, consisting of a
 * {@linkplain #getMethod() method} and a {@linkplain #getURI() URI}.
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
public interface HttpRequest extends HttpMessage {

	/**
	 * Return the HTTP method of the request.
	 * @return the HTTP method as an HttpMethod value
	 * @see HttpMethod#valueOf(String)
	 */
	HttpMethod getMethod();

	/**
	 * Return the URI of the request (including a query string if any,
	 * but only if it is well-formed for a URI representation).
	 * @return the URI of the request (never {@code null})
	 */
	URI getURI();

}
