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

import java.util.stream.Stream;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

/**
 * @author Arjen Poutsma
 */
abstract class AbstractHttpMessageWriterResponse<T> extends AbstractResponse<T> {

	protected AbstractHttpMessageWriterResponse(int statusCode, HttpHeaders headers) {
		super(statusCode, headers);
	}

	protected <S> Mono<Void> writeToInternal(ServerWebExchange exchange,
											 Publisher<S> body,
											 ResolvableType bodyType) {
		writeStatusAndHeaders(exchange);
		MediaType contentType = exchange.getResponse().getHeaders().getContentType();
		ServerHttpResponse response = exchange.getResponse();
		return messageWriterStream(exchange)
				.filter(messageWriter -> messageWriter.canWrite(bodyType, contentType))
				.findFirst()
				.map(CastingUtils::cast)
				.map(messageWriter -> messageWriter.write(body, bodyType, contentType, response))
				.orElseGet(() -> {
					response.setStatusCode(HttpStatus.NOT_ACCEPTABLE);
					return response.setComplete();
				});
	}

	private Stream<HttpMessageWriter<?>> messageWriterStream(ServerWebExchange exchange) {
		return exchange.<Stream<HttpMessageWriter<?>>>getAttribute(Router.HTTP_MESSAGE_WRITERS_ATTRIBUTE)
				.orElseThrow(() -> new IllegalStateException("Could not find HttpMessageWriters in ServerWebExchange"));
	}

}
