/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.function.client;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.client.ClientHttpRequestInterceptor;

/**
 * {@link ClientHttpRequestInterceptor} to limit response body.
 *
 * @author Sergey Galkin
 * @since 5.1
 */
public class ResponseBodyLimitFilterFunction implements ExchangeFilterFunction {

	private final long bodyByteLimit;

	public ResponseBodyLimitFilterFunction(long bodyByteLimit) {
		if (bodyByteLimit < 0) {
			throw new IllegalArgumentException(
					"Response body limit should be non-negative, but " + bodyByteLimit + "given"
			);
		}

		this.bodyByteLimit = bodyByteLimit;
	}

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		return next.exchange(request).flatMap(this::filter);
	}

	private Mono<ClientResponse> filter(ClientResponse response) {
		Flux<DataBuffer> buffers = response.body(
				(message, ctx) -> DataBufferUtils.takeUntilByteCount(message.getBody(), this.bodyByteLimit)
		);
		return Mono.just(ClientResponse.create(response.statusCode()).body(buffers).build());
	}
}
