/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.reactive.result.condition;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.get;

/**
 * Tests for {@link ParamsRequestCondition}.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Nicoll
 */
class ParamsRequestConditionTests {

	@Test
	void paramEquals() {
		assertThat(new ParamsRequestCondition("foo")).isEqualTo(new ParamsRequestCondition("foo"));
		assertThat(new ParamsRequestCondition("foo")).isNotEqualTo(new ParamsRequestCondition("bar"));
		assertThat(new ParamsRequestCondition("foo")).isNotEqualTo(new ParamsRequestCondition("FOO"));
		assertThat(new ParamsRequestCondition("foo=bar")).isEqualTo(new ParamsRequestCondition("foo=bar"));
		assertThat(new ParamsRequestCondition("foo=bar")).isNotEqualTo(new ParamsRequestCondition("FOO=bar"));
	}

	@Test
	void paramPresent() {
		ParamsRequestCondition condition = new ParamsRequestCondition("foo");
		assertThat(condition.getMatchingCondition(MockServerWebExchange.from(get("/path?foo=")))).isNotNull();
	}

	@Test // SPR-15831
	void paramPresentNullValue() {
		ParamsRequestCondition condition = new ParamsRequestCondition("foo");
		assertThat(condition.getMatchingCondition(MockServerWebExchange.from(get("/path?foo")))).isNotNull();
	}

	@Test
	void paramPresentNoMatch() {
		ParamsRequestCondition condition = new ParamsRequestCondition("foo");
		assertThat(condition.getMatchingCondition(MockServerWebExchange.from(get("/path?bar=")))).isNull();
	}

	@Test
	void paramNotPresent() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/"));
		assertThat(new ParamsRequestCondition("!foo").getMatchingCondition(exchange)).isNotNull();
	}

	@Test
	void paramValueMatch() {
		ParamsRequestCondition condition = new ParamsRequestCondition("foo=bar");
		assertThat(condition.getMatchingCondition(MockServerWebExchange.from(get("/path?foo=bar")))).isNotNull();
	}

	@Test
	void paramValueNoMatch() {
		ParamsRequestCondition condition = new ParamsRequestCondition("foo=bar");
		assertThat(condition.getMatchingCondition(MockServerWebExchange.from(get("/path?foo=bazz")))).isNull();
	}

	@Test
	void compareTo() {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/"));

		ParamsRequestCondition condition1 = new ParamsRequestCondition("foo", "bar", "baz");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("foo", "bar");

		int result = condition1.compareTo(condition2, exchange);
		assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);

		result = condition2.compareTo(condition1, exchange);
		assertThat(result).as("Invalid comparison result: " + result).isGreaterThan(0);
	}

	@Test // SPR-16674
	void compareToWithMoreSpecificMatchByValue() {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/"));

		ParamsRequestCondition condition1 = new ParamsRequestCondition("response_type=code");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("response_type");

		int result = condition1.compareTo(condition2, exchange);
		assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);
	}

	@Test
	void compareToWithNegatedMatch() {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/"));

		ParamsRequestCondition condition1 = new ParamsRequestCondition("response_type!=code");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("response_type");

		assertThat(condition1.compareTo(condition2, exchange)).as("Negated match should not count as more specific").isEqualTo(0);
	}

	@Test
	void combineWithOtherEmpty() {
		ParamsRequestCondition condition1 = new ParamsRequestCondition("foo=bar");
		ParamsRequestCondition condition2 = new ParamsRequestCondition();

		ParamsRequestCondition result = condition1.combine(condition2);
		assertThat(result).isEqualTo(condition1);
	}

	@Test
	void combineWithThisEmpty() {
		ParamsRequestCondition condition1 = new ParamsRequestCondition();
		ParamsRequestCondition condition2 = new ParamsRequestCondition("foo=bar");

		ParamsRequestCondition result = condition1.combine(condition2);
		assertThat(result).isEqualTo(condition2);
	}

	@Test
	void combine() {
		ParamsRequestCondition condition1 = new ParamsRequestCondition("foo=bar");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("foo=baz");

		ParamsRequestCondition result = condition1.combine(condition2);
		Collection<?> conditions = result.getContent();
		assertThat(conditions).hasSize(2);
	}

}
