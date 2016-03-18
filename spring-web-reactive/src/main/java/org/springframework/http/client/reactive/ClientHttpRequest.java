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

import reactor.core.publisher.Mono;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.util.MultiValueMap;

/**
 * Represents a reactive client-side HTTP request.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
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
	 * Execute this request, resulting in a reactive stream of a single
	 * {@link org.springframework.http.client.ClientHttpResponse}.
	 *
	 * @return a {@code Mono<ClientHttpResponse>} that signals when the the response
	 * status and headers have been received. The response body is made available with
	 * a separate Publisher within the {@code ClientHttpResponse}.
	 */
	Mono<ClientHttpResponse> execute();

}
