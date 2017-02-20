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

import org.springframework.core.ResolvableType;

/**
 * {@code ExchangeResult} variant with the response body as a {@code Flux<T>}.
 *
 * @param <T> the type of elements in the response body
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see EntityExchangeResult
 */
public class FluxExchangeResult<T> extends ExchangeResult {

	private final Flux<T> body;

	private final ResolvableType elementType;


	FluxExchangeResult(ExchangeResult result, Flux<T> body, ResolvableType elementType) {
		super(result);
		this.body = body;
		this.elementType = elementType;
	}


	/**
	 * Return the {@code Flux} of elements decoded from the response body.
	 */
	public Flux<T> getResponseBody() {
		return this.body;
	}

	@Override
	protected String formatResponseBody() {
		return "Flux<" + this.elementType.toString() + ">";
	}

}
