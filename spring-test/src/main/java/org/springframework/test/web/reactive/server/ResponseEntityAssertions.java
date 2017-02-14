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

import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;

import static org.springframework.web.reactive.function.BodyExtractors.toFlux;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;

/**
 * Assertions on the response body decoded as a single entity.
 *
 * @param <T> the response entity type
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ResponseEntityAssertions<T> extends ObjectAssertions<T, ResponseEntityAssertions<T>> {

	private final ResolvableType entityType;


	ResponseEntityAssertions(ExchangeActions actions, ResolvableType entityType) {
		super(actions, () -> initEntity(actions, entityType), "Response body");
		this.entityType = entityType;
	}

	private static <T> T initEntity(ExchangeActions exchangeActions, ResolvableType entityType) {
		ExchangeInfo info = exchangeActions.andReturn();
		Mono<T> mono = info.getResponse().body(toMono(entityType));
		return mono.block(info.getResponseTimeout());
	}


	/**
	 * Assert the response decoded as a List of entities of the given type.
	 */
	public ListAssertions<T> list() {
		Flux<T> flux = getResponse().body(toFlux(this.entityType));
		List<T> list = flux.collectList().block(getTimeout());
		return new ListAssertions<T>(getExchangeActions(), list, "Response entity collection");
	}

	/**
	 * Assert the response decoded as a Map of entities with String keys.
	 */
	public MapAssertions<String, T> map() {
		ResolvableType keyType = ResolvableType.forClass(String.class);
		ResolvableType type = ResolvableType.forClassWithGenerics(Map.class, keyType, this.entityType);
		Mono<Map<String, T>> mono = getResponse().body(toMono(type));
		Map<String, T> map = mono.block(getTimeout());
		return new MapAssertions<>(getExchangeActions(), map, "Response entity map");
	}

	/**
	 * Assert the response content using a {@link StepVerifier}.
	 */
	public StepVerifier.FirstStep<T> stepVerifier() {
		Flux<T> flux = getResponse().body(toFlux(this.entityType));
		return StepVerifier.create(flux);
	}

}
