/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.http.client.reactive;

import java.net.URI;

import org.reactivestreams.Publisher;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * Factory for {@link ClientHttpRequest} objects.
 *
 * @author Brian Clozel
 */
public interface ClientHttpRequestFactory {

	/**
	 * Create a new {@link ClientHttpRequest} for the specified HTTP method, URI and headers
	 * <p>The returned request can be {@link ClientHttpRequest#writeWith(Publisher) written to},
	 * and then executed by calling {@link ClientHttpRequest#execute()}
	 *
	 * @param httpMethod the HTTP method to execute
	 * @param uri the URI to create a request for
	 * @param headers the HTTP request headers
	 */
	ClientHttpRequest createRequest(HttpMethod httpMethod, URI uri, HttpHeaders headers);

}
