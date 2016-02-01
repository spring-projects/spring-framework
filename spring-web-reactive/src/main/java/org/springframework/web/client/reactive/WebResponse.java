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

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.core.codec.Decoder;
import org.springframework.http.client.reactive.ClientHttpResponse;

/**
 * Result of a {@code ClientHttpRequest} sent to a remote server by the {@code WebClient}
 *
 * <p>Contains all the required information to extract relevant information from the raw response.
 *
 * @author Brian Clozel
 */
public interface WebResponse {

	/**
	 * Return the raw response received by the {@code WebClient}
	 */
	Mono<ClientHttpResponse> getClientResponse();

	/**
	 * Return the configured list of {@link Decoder}s that can be used to decode the raw response body
	 */
	List<Decoder<?>> getMessageDecoders();
}
