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

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.test.util.AssertionErrors;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.web.reactive.function.BodyExtractors.toFlux;

/**
 * Provides methods for asserting the response body as a collection of entities.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ResponseEntityCollectionAssertions<T> {

	private final Collection<T> entities;


	ResponseEntityCollectionAssertions(ExchangeInfo info, ResolvableType entityType) {
		Flux<T> flux = info.getResponse().body(toFlux(entityType));
		this.entities = flux.collectList().block(info.getResponseTimeout());
	}


	/**
	 * Assert the number of entities.
	 */
	public ResponseEntityCollectionAssertions<T> hasSize(int size) {
		assertEquals("Response entities count", size, this.entities.size());
		return this;
	}

	/**
	 * Assert that the response contains all of the given entities.
	 */
	@SuppressWarnings("unchecked")
	public ResponseEntityCollectionAssertions<T> contains(T... entities) {
		Arrays.stream(entities).forEach(entity ->
				assertTrue("Response does not contain " + entity, this.entities.contains(entity)));
		return this;
	}

	/**
	 * Assert that the response does not contain any of the given entities.
	 */
	@SuppressWarnings("unchecked")
	public ResponseEntityCollectionAssertions<T> doesNotContain(T... entities) {
		Arrays.stream(entities).forEach(entity ->
				assertTrue("Response should not contain " + entity, !this.entities.contains(entity)));
		return this;
	}

	/**
	 * Apply custom assertions on the response entity collection with the help of
	 * {@link AssertionErrors} or an assertion library such as AssertJ.
	 * <p>Consider using statically imported methods for creating the assertion
	 * consumer to improve readability of tests.
	 * @param assertionConsumer consumer that will apply assertions.
	 */
	public ResponseEntityCollectionAssertions<T> andAssert(Consumer<Collection<T>> assertionConsumer) {
		assertionConsumer.accept(this.entities);
		return this;
	}

	/**
	 * Apply custom actions on the response entity collection.
	 * <p>Consider using statically imported methods for creating the assertion
	 * consumer to improve readability of tests.
	 * @param consumer consumer that will apply the custom action
	 */
	public ResponseEntityCollectionAssertions<T> andDo(Consumer<Collection<T>> consumer) {
		consumer.accept(this.entities);
		return this;
	}

}
