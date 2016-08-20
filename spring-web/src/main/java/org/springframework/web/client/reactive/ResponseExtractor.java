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

import reactor.core.publisher.Mono;

import org.springframework.http.client.reactive.ClientHttpResponse;

/**
 * A {@code ResponseExtractor} extracts the relevant part of a
 * raw {@link org.springframework.http.client.reactive.ClientHttpResponse},
 * optionally decoding the response body and using a target composition API.
 *
 * <p>See static factory methods in {@link ResponseExtractors} and
 * {@link org.springframework.web.client.reactive.support.RxJava1ResponseExtractors}.
 *
 * @author Brian Clozel
 * @since 5.0
 */
public interface ResponseExtractor<T> {

	/**
	 * Extract content from the response
	 * @param clientResponse the raw HTTP response
	 * @param webClientConfig the {@link WebClient} configuration information
	 * @return the relevant part of the response
	 */
	T extract(Mono<ClientHttpResponse> clientResponse, WebClientConfig webClientConfig);
}
