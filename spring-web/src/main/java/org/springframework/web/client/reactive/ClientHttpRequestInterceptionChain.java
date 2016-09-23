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
import java.util.function.Consumer;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpResponse;

/**
 * Delegate to the next {@link ClientHttpRequestInterceptor} in the chain.
 *
 * @author Brian Clozel
 * @since 5.0
 */
public interface ClientHttpRequestInterceptionChain {

	/**
	 * Delegate to the next {@link ClientHttpRequestInterceptor} in the chain.
	 *
	 * @param method the HTTP request method
	 * @param uri the HTTP request URI
	 * @param requestCallback a function that can customize the request
	 * by changing the HTTP request headers with {@code HttpMessage.getHeaders()}.
	 * @return a publisher of the resulting {@link ClientHttpResponse}
	 */
	Mono<ClientHttpResponse> intercept(HttpMethod method, URI uri, Consumer<? super HttpMessage> requestCallback);

}
