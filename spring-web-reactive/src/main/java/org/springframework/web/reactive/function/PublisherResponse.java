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

package org.springframework.web.reactive.function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

/**
 * @author Arjen Poutsma
 */
class PublisherResponse<T, S extends Publisher<T>> extends AbstractHttpMessageWriterResponse<S> {

	private final S body;

	private final ResolvableType bodyType;


	public PublisherResponse(int statusCode, HttpHeaders headers,
									 S body, Class<T> aClass) {
		super(statusCode, headers);
		this.body = body;
		this.bodyType = ResolvableType.forClass(aClass);
	}

	@Override
	public S body() {
		return this.body;
	}

	@Override
	public Mono<Void> writeTo(ServerWebExchange exchange) {
		return writeToInternal(exchange, this.body, this.bodyType);
	}

}
