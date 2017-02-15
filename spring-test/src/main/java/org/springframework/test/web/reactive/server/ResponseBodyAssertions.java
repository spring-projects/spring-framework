/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.test.web.reactive.server;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.web.reactive.function.client.ClientResponse;

import static org.springframework.web.reactive.function.BodyExtractors.toMono;

/**
 * Assertions on the body of the response.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ResponseBodyAssertions {

	private final ExchangeActions exchangeActions;


	public ResponseBodyAssertions(ExchangeActions exchangeActions) {
		this.exchangeActions = exchangeActions;
	}


	/**
	 * Assert the response does not have any content.
	 */
	public ExchangeActions isEmpty() {
		Flux<?> body = getResponse().bodyToFlux(ByteBuffer.class);
		StepVerifier.create(body).expectComplete().verify(getTimeout());
		return this.exchangeActions;
	}

	/**
	 * Assert the response decoded as a Map of String key-value pairs.
	 */
	public MapAssertions<String, String> asMap() {
		ResolvableType type = ResolvableType.forClassWithGenerics(Map.class, String.class, String.class);
		Mono<Map<String, String>> mono = getResponse().body(toMono(type));
		Map<String, String> map = mono.block(getTimeout());
		return new MapAssertions<>(this.exchangeActions, map, "Response body map");
	}


	private ClientResponse getResponse() {
		return this.exchangeActions.andReturn().getResponse();
	}

	private Duration getTimeout() {
		return this.exchangeActions.andReturn().getResponseTimeout();
	}

}
