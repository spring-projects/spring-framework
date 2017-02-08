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

import java.util.function.Consumer;

import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.test.util.AssertionErrors;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;

/**
 * Provides methods for asserting the response body as a single entity.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ResponseEntityAssertions<T> {

	private final ExchangeInfo exchangeInfo;

	private final ResolvableType entityType;

	private T entity;


	ResponseEntityAssertions(ExchangeInfo info, ResolvableType entityType) {
		this.exchangeInfo = info;
		this.entityType = entityType;
	}


	protected ExchangeInfo getExchangeInfo() {
		return this.exchangeInfo;
	}

	protected ResolvableType getEntityType() {
		return this.entityType;
	}

	private T resolveEntity() {
		if (this.entity == null) {
			Mono<T> mono = this.exchangeInfo.getResponse().body(toMono(entityType));
			this.entity = mono.block(this.exchangeInfo.getResponseTimeout());
		}
		return this.entity;
	}


	/**
	 * Assert the response entity is equal to the given expected entity.
	 */
	public ResponseEntityAssertions<T> isEqualTo(T expected) {
		assertEquals("Response body", expected, resolveEntity());
		return this;
	}

	/**
	 * Assert the response entity is not equal to the given expected entity.
	 */
	public ResponseEntityAssertions<T> isNotEqualTo(T expected) {
		assertEquals("Response body", expected, resolveEntity());
		return this;
	}

	/**
	 * Apply custom assertions on the response entity with the help of
	 * {@link AssertionErrors} or an assertion library such as AssertJ.
	 * <p>Consider using statically imported methods for creating the assertion
	 * consumer to improve readability of tests.
	 * @param assertionConsumer consumer that will apply assertions.
	 */
	public ResponseEntityAssertions<T> andAssert(Consumer<T> assertionConsumer) {
		assertionConsumer.accept(resolveEntity());
		return this;
	}

	/**
	 * Apply custom actions on the response entity collection.
	 * <p>Consider using statically imported methods for creating the assertion
	 * consumer to improve readability of tests.
	 * @param consumer consumer that will apply the custom action
	 */
	public ResponseEntityAssertions<T> andDo(Consumer<T> consumer) {
		consumer.accept(resolveEntity());
		return this;
	}

}
