/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.function.server;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.codec.StringDecoder;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class RequestPredicateAttributesTests {

	private DefaultServerRequest request;

	@Before
	public void createRequest() {
		MockServerHttpRequest request = MockServerHttpRequest.get("https://example.com/path").build();
		MockServerWebExchange webExchange = MockServerWebExchange.from(request);
		webExchange.getAttributes().put("exchange", "bar");

		this.request = new DefaultServerRequest(webExchange,
				Collections.singletonList(
						new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));
	}


	@Test
	public void negateSucceed() {
		RequestPredicate predicate = new AddAttributePredicate(false, "predicate", "baz").negate();

		boolean result = predicate.test(this.request);
		assertTrue(result);

		assertEquals("bar", this.request.attributes().get("exchange"));
		assertEquals("baz", this.request.attributes().get("predicate"));
	}

	@Test
	public void negateFail() {
		RequestPredicate predicate = new AddAttributePredicate(true, "predicate", "baz").negate();

		boolean result = predicate.test(this.request);
		assertFalse(result);

		assertEquals("bar", this.request.attributes().get("exchange"));
		assertFalse(this.request.attributes().containsKey("baz"));
	}

	@Test
	public void andBothSucceed() {
		RequestPredicate left = new AddAttributePredicate(true, "left", "baz");
		RequestPredicate right = new AddAttributePredicate(true, "right", "qux");
		RequestPredicate predicate = new RequestPredicates.AndRequestPredicate(left, right);

		boolean result = predicate.test(this.request);
		assertTrue(result);

		assertEquals("bar", this.request.attributes().get("exchange"));
		assertEquals("baz", this.request.attributes().get("left"));
		assertEquals("qux", this.request.attributes().get("right"));
	}

	@Test
	public void andLeftSucceed() {
		RequestPredicate left = new AddAttributePredicate(true, "left", "bar");
		RequestPredicate right = new AddAttributePredicate(false, "right", "qux");
		RequestPredicate predicate = new RequestPredicates.AndRequestPredicate(left, right);

		boolean result = predicate.test(this.request);
		assertFalse(result);

		assertEquals("bar", this.request.attributes().get("exchange"));
		assertFalse(this.request.attributes().containsKey("left"));
		assertFalse(this.request.attributes().containsKey("right"));
	}

	@Test
	public void andRightSucceed() {
		RequestPredicate left = new AddAttributePredicate(false, "left", "bar");
		RequestPredicate right = new AddAttributePredicate(true, "right", "qux");
		RequestPredicate predicate = new RequestPredicates.AndRequestPredicate(left, right);

		boolean result = predicate.test(this.request);
		assertFalse(result);

		assertEquals("bar", this.request.attributes().get("exchange"));
		assertFalse(this.request.attributes().containsKey("left"));
		assertFalse(this.request.attributes().containsKey("right"));
	}

	@Test
	public void andBothFail() {
		RequestPredicate left = new AddAttributePredicate(false, "left", "bar");
		RequestPredicate right = new AddAttributePredicate(false, "right", "qux");
		RequestPredicate predicate = new RequestPredicates.AndRequestPredicate(left, right);

		boolean result = predicate.test(this.request);
		assertFalse(result);

		assertEquals("bar", this.request.attributes().get("exchange"));
		assertFalse(this.request.attributes().containsKey("left"));
		assertFalse(this.request.attributes().containsKey("right"));
	}

	@Test
	public void orBothSucceed() {
		RequestPredicate left = new AddAttributePredicate(true, "left", "baz");
		RequestPredicate right = new AddAttributePredicate(true, "right", "qux");
		RequestPredicate predicate = new RequestPredicates.OrRequestPredicate(left, right);

		boolean result = predicate.test(this.request);
		assertTrue(result);

		assertEquals("bar", this.request.attributes().get("exchange"));
		assertEquals("baz", this.request.attributes().get("left"));
		assertFalse(this.request.attributes().containsKey("right"));
	}

	@Test
	public void orLeftSucceed() {
		RequestPredicate left = new AddAttributePredicate(true, "left", "baz");
		RequestPredicate right = new AddAttributePredicate(false, "right", "qux");
		RequestPredicate predicate = new RequestPredicates.OrRequestPredicate(left, right);

		boolean result = predicate.test(this.request);
		assertTrue(result);

		assertEquals("bar", this.request.attributes().get("exchange"));
		assertEquals("baz", this.request.attributes().get("left"));
		assertFalse(this.request.attributes().containsKey("right"));
	}

	@Test
	public void orRightSucceed() {
		RequestPredicate left = new AddAttributePredicate(false, "left", "baz");
		RequestPredicate right = new AddAttributePredicate(true, "right", "qux");
		RequestPredicate predicate = new RequestPredicates.OrRequestPredicate(left, right);

		boolean result = predicate.test(this.request);
		assertTrue(result);

		assertEquals("bar", this.request.attributes().get("exchange"));
		assertFalse(this.request.attributes().containsKey("left"));
		assertEquals("qux", this.request.attributes().get("right"));
	}

	@Test
	public void orBothFail() {
		RequestPredicate left = new AddAttributePredicate(false, "left", "baz");
		RequestPredicate right = new AddAttributePredicate(false, "right", "qux");
		RequestPredicate predicate = new RequestPredicates.OrRequestPredicate(left, right);

		boolean result = predicate.test(this.request);
		assertFalse(result);

		assertEquals("bar", this.request.attributes().get("exchange"));
		assertFalse(this.request.attributes().containsKey("baz"));
		assertFalse(this.request.attributes().containsKey("quux"));
	}


	private static class AddAttributePredicate implements RequestPredicate {

		private boolean result;

		private final String key;

		private final String value;

		private AddAttributePredicate(boolean result, String key, String value) {
			this.result = result;
			this.key = key;
			this.value = value;
		}

		@Override
		public boolean test(ServerRequest request) {
			request.attributes().put(key, value);
			return this.result;
		}
	}

}
