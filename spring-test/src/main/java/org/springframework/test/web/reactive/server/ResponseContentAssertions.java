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

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;

import static org.springframework.web.reactive.function.BodyExtractors.toFlux;

/**
 * Provides options for asserting the content of the response.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ResponseContentAssertions<T> extends ResponseEntityAssertions<T> {


	ResponseContentAssertions(ExchangeInfo exchangeInfo, ResolvableType entityType) {
		super(exchangeInfo, entityType);
	}


	/**
	 * Assert the response as a collection of entities.
	 */
	public ResponseEntityCollectionAssertions<T> collection() {
		return new ResponseEntityCollectionAssertions<>(getExchangeInfo(), getEntityType());
	}

	/**
	 * Assert the response using a {@link StepVerifier}.
	 */
	public StepVerifier.FirstStep<T> stepVerifier() {
		Flux<T> flux = getExchangeInfo().getResponse().body(toFlux(getEntityType()));
		return StepVerifier.create(flux);
	}

}
