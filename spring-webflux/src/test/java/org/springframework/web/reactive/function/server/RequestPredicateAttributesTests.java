/*
 * Copyright 2002-2019 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.codec.StringDecoder;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class RequestPredicateAttributesTests {

	private DefaultServerRequest request;

	@BeforeEach
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
		assertThat(result).isTrue();

		assertThat(this.request.attributes().get("exchange")).isEqualTo("bar");
		assertThat(this.request.attributes().get("predicate")).isEqualTo("baz");
	}

	@Test
	public void negateFail() {
		RequestPredicate predicate = new AddAttributePredicate(true, "predicate", "baz").negate();

		boolean result = predicate.test(this.request);
		assertThat(result).isFalse();

		assertThat(this.request.attributes().get("exchange")).isEqualTo("bar");
		assertThat(this.request.attributes().containsKey("baz")).isFalse();
	}

	@Test
	public void andBothSucceed() {
		RequestPredicate left = new AddAttributePredicate(true, "left", "baz");
		RequestPredicate right = new AddAttributePredicate(true, "right", "qux");
		RequestPredicate predicate = new RequestPredicates.AndRequestPredicate(left, right);

		boolean result = predicate.test(this.request);
		assertThat(result).isTrue();

		assertThat(this.request.attributes().get("exchange")).isEqualTo("bar");
		assertThat(this.request.attributes().get("left")).isEqualTo("baz");
		assertThat(this.request.attributes().get("right")).isEqualTo("qux");
	}

	@Test
	public void andLeftSucceed() {
		RequestPredicate left = new AddAttributePredicate(true, "left", "bar");
		RequestPredicate right = new AddAttributePredicate(false, "right", "qux");
		RequestPredicate predicate = new RequestPredicates.AndRequestPredicate(left, right);

		boolean result = predicate.test(this.request);
		assertThat(result).isFalse();

		assertThat(this.request.attributes().get("exchange")).isEqualTo("bar");
		assertThat(this.request.attributes().containsKey("left")).isFalse();
		assertThat(this.request.attributes().containsKey("right")).isFalse();
	}

	@Test
	public void andRightSucceed() {
		RequestPredicate left = new AddAttributePredicate(false, "left", "bar");
		RequestPredicate right = new AddAttributePredicate(true, "right", "qux");
		RequestPredicate predicate = new RequestPredicates.AndRequestPredicate(left, right);

		boolean result = predicate.test(this.request);
		assertThat(result).isFalse();

		assertThat(this.request.attributes().get("exchange")).isEqualTo("bar");
		assertThat(this.request.attributes().containsKey("left")).isFalse();
		assertThat(this.request.attributes().containsKey("right")).isFalse();
	}

	@Test
	public void andBothFail() {
		RequestPredicate left = new AddAttributePredicate(false, "left", "bar");
		RequestPredicate right = new AddAttributePredicate(false, "right", "qux");
		RequestPredicate predicate = new RequestPredicates.AndRequestPredicate(left, right);

		boolean result = predicate.test(this.request);
		assertThat(result).isFalse();

		assertThat(this.request.attributes().get("exchange")).isEqualTo("bar");
		assertThat(this.request.attributes().containsKey("left")).isFalse();
		assertThat(this.request.attributes().containsKey("right")).isFalse();
	}

	@Test
	public void orBothSucceed() {
		RequestPredicate left = new AddAttributePredicate(true, "left", "baz");
		RequestPredicate right = new AddAttributePredicate(true, "right", "qux");
		RequestPredicate predicate = new RequestPredicates.OrRequestPredicate(left, right);

		boolean result = predicate.test(this.request);
		assertThat(result).isTrue();

		assertThat(this.request.attributes().get("exchange")).isEqualTo("bar");
		assertThat(this.request.attributes().get("left")).isEqualTo("baz");
		assertThat(this.request.attributes().containsKey("right")).isFalse();
	}

	@Test
	public void orLeftSucceed() {
		RequestPredicate left = new AddAttributePredicate(true, "left", "baz");
		RequestPredicate right = new AddAttributePredicate(false, "right", "qux");
		RequestPredicate predicate = new RequestPredicates.OrRequestPredicate(left, right);

		boolean result = predicate.test(this.request);
		assertThat(result).isTrue();

		assertThat(this.request.attributes().get("exchange")).isEqualTo("bar");
		assertThat(this.request.attributes().get("left")).isEqualTo("baz");
		assertThat(this.request.attributes().containsKey("right")).isFalse();
	}

	@Test
	public void orRightSucceed() {
		RequestPredicate left = new AddAttributePredicate(false, "left", "baz");
		RequestPredicate right = new AddAttributePredicate(true, "right", "qux");
		RequestPredicate predicate = new RequestPredicates.OrRequestPredicate(left, right);

		boolean result = predicate.test(this.request);
		assertThat(result).isTrue();

		assertThat(this.request.attributes().get("exchange")).isEqualTo("bar");
		assertThat(this.request.attributes().containsKey("left")).isFalse();
		assertThat(this.request.attributes().get("right")).isEqualTo("qux");
	}

	@Test
	public void orBothFail() {
		RequestPredicate left = new AddAttributePredicate(false, "left", "baz");
		RequestPredicate right = new AddAttributePredicate(false, "right", "qux");
		RequestPredicate predicate = new RequestPredicates.OrRequestPredicate(left, right);

		boolean result = predicate.test(this.request);
		assertThat(result).isFalse();

		assertThat(this.request.attributes().get("exchange")).isEqualTo("bar");
		assertThat(this.request.attributes().containsKey("baz")).isFalse();
		assertThat(this.request.attributes().containsKey("quux")).isFalse();
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
