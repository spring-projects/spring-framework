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
 * @sine 5.0
 */
public class ResponseEntityAssertions<T> extends ObjectAssertions<T, ResponseEntityAssertions<T>> {

	private final ExchangeInfo exchangeInfo;

	private final ResolvableType entityType;


	ResponseEntityAssertions(ExchangeActions actions, ExchangeInfo info, ResolvableType entityType) {
		super(actions, () -> initEntity(info, entityType), "Response body");
		this.exchangeInfo = info;
		this.entityType = entityType;
	}

	private static <T> T initEntity(ExchangeInfo exchangeInfo, ResolvableType entityType) {
		Mono<T> mono = exchangeInfo.getResponse().body(toMono(entityType));
		return mono.block(exchangeInfo.getResponseTimeout());
	}


	/**
	 * Assert the response decoded as a Collection of entities of the given type.
	 */
	public ListAssertions<T> list() {
		Flux<T> flux = this.exchangeInfo.getResponse().body(toFlux(this.entityType));
		List<T> list = flux.collectList().block(this.exchangeInfo.getResponseTimeout());
		return new ListAssertions<T>(getExchangeActions(), list, "Response entity collection");
	}

	/**
	 * Assert the response content using a {@link StepVerifier}.
	 */
	public StepVerifier.FirstStep<T> stepVerifier() {
		Flux<T> flux = this.exchangeInfo.getResponse().body(toFlux(this.entityType));
		return StepVerifier.create(flux);
	}

}
