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

package org.springframework.web.client.reactive;

import java.net.URI;
import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpResponse;

/**
 * Contract for chain-based, interception processing of client http requests
 * that may be used to implement cross-cutting requirements such
 * as security, timeouts, caching, and others.
 *
 * <p>Implementations of this interface can be
 * {@link WebClient#setInterceptors(List) registered} with the {@link WebClient}.
 *
 * @author Brian Clozel
 * @see org.springframework.web.client.reactive.WebClient
 * @since 5.0
 */
@FunctionalInterface
public interface ClientHttpRequestInterceptor {

	/**
	 * Intercept the client HTTP request
	 *
	 * <p>The provided {@link ClientHttpRequestInterceptionChain}
	 * instance allows the interceptor to delegate the request
	 * to the next interceptor in the chain.
	 *
	 * <p>An implementation might follow this pattern:
	 * <ol>
	 * <li>Examine the {@link HttpMethod method} and {@link URI uri}</li>
	 * <li>Optionally change those when delegating to the next interceptor
	 * with the {@code ClientHttpRequestInterceptionChain}.</li>
	 * <li>Optionally transform the HTTP message given as an
	 * argument of the request callback in
	 * {@code chain.intercept(method, uri, requestCallback)}.</li>
	 * <li>Optionally transform the response before returning it.</li>
	 * </ol>
	 *
	 * @param method the HTTP request method
	 * @param uri the HTTP request URI
	 * @param chain the request interception chain
	 * @return a publisher of the {@link ClientHttpResponse}
	 */
	Mono<ClientHttpResponse> intercept(HttpMethod method, URI uri, ClientHttpRequestInterceptionChain chain);
}