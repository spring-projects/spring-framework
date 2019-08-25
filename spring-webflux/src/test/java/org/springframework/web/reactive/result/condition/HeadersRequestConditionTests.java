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

package org.springframework.web.reactive.result.condition;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.get;

/**
 * Unit tests for {@link HeadersRequestCondition}.
 *
 * @author Rossen Stoyanchev
 */
public class HeadersRequestConditionTests {

	@Test
	public void headerEquals() {
		assertThat(new HeadersRequestCondition("foo")).isEqualTo(new HeadersRequestCondition("foo"));
		assertThat(new HeadersRequestCondition("FOO")).isEqualTo(new HeadersRequestCondition("foo"));
		assertThat(new HeadersRequestCondition("bar")).isNotEqualTo(new HeadersRequestCondition("foo"));
		assertThat(new HeadersRequestCondition("foo=bar")).isEqualTo(new HeadersRequestCondition("foo=bar"));
		assertThat(new HeadersRequestCondition("FOO=bar")).isEqualTo(new HeadersRequestCondition("foo=bar"));
	}

	@Test
	public void headerPresent() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("Accept", ""));
		HeadersRequestCondition condition = new HeadersRequestCondition("accept");

		assertThat(condition.getMatchingCondition(exchange)).isNotNull();
	}

	@Test
	public void headerPresentNoMatch() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("bar", ""));
		HeadersRequestCondition condition = new HeadersRequestCondition("foo");

		assertThat(condition.getMatchingCondition(exchange)).isNull();
	}

	@Test
	public void headerNotPresent() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/"));
		HeadersRequestCondition condition = new HeadersRequestCondition("!accept");

		assertThat(condition.getMatchingCondition(exchange)).isNotNull();
	}

	@Test
	public void headerValueMatch() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("foo", "bar"));
		HeadersRequestCondition condition = new HeadersRequestCondition("foo=bar");

		assertThat(condition.getMatchingCondition(exchange)).isNotNull();
	}

	@Test
	public void headerValueNoMatch() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("foo", "bazz"));
		HeadersRequestCondition condition = new HeadersRequestCondition("foo=bar");

		assertThat(condition.getMatchingCondition(exchange)).isNull();
	}

	@Test
	public void headerCaseSensitiveValueMatch() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("foo", "bar"));
		HeadersRequestCondition condition = new HeadersRequestCondition("foo=Bar");

		assertThat(condition.getMatchingCondition(exchange)).isNull();
	}

	@Test
	public void headerValueMatchNegated() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("foo", "baz"));
		HeadersRequestCondition condition = new HeadersRequestCondition("foo!=bar");

		assertThat(condition.getMatchingCondition(exchange)).isNotNull();
	}

	@Test
	public void headerValueNoMatchNegated() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("foo", "bar"));
		HeadersRequestCondition condition = new HeadersRequestCondition("foo!=bar");

		assertThat(condition.getMatchingCondition(exchange)).isNull();
	}

	@Test
	public void compareTo() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/"));

		HeadersRequestCondition condition1 = new HeadersRequestCondition("foo", "bar", "baz");
		HeadersRequestCondition condition2 = new HeadersRequestCondition("foo=a", "bar");

		int result = condition1.compareTo(condition2, exchange);
		assertThat(result < 0).as("Invalid comparison result: " + result).isTrue();

		result = condition2.compareTo(condition1, exchange);
		assertThat(result > 0).as("Invalid comparison result: " + result).isTrue();
	}

	@Test // SPR-16674
	public void compareToWithMoreSpecificMatchByValue() {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/"));

		HeadersRequestCondition condition1 = new HeadersRequestCondition("foo=a");
		HeadersRequestCondition condition2 = new HeadersRequestCondition("foo");

		int result = condition1.compareTo(condition2, exchange);
		assertThat(result < 0).as("Invalid comparison result: " + result).isTrue();
	}

	@Test
	public void compareToWithNegatedMatch() {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/"));

		HeadersRequestCondition condition1 = new HeadersRequestCondition("foo!=a");
		HeadersRequestCondition condition2 = new HeadersRequestCondition("foo");

		assertThat(condition1.compareTo(condition2, exchange)).as("Negated match should not count as more specific").isEqualTo(0);
	}

	@Test
	public void combine() {
		HeadersRequestCondition condition1 = new HeadersRequestCondition("foo=bar");
		HeadersRequestCondition condition2 = new HeadersRequestCondition("foo=baz");

		HeadersRequestCondition result = condition1.combine(condition2);
		Collection<?> conditions = result.getContent();
		assertThat(conditions.size()).isEqualTo(2);
	}

	@Test
	public void getMatchingCondition() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("foo", "bar"));
		HeadersRequestCondition condition = new HeadersRequestCondition("foo");

		HeadersRequestCondition result = condition.getMatchingCondition(exchange);
		assertThat(result).isEqualTo(condition);

		condition = new HeadersRequestCondition("bar");

		result = condition.getMatchingCondition(exchange);
		assertThat(result).isNull();
	}

}
