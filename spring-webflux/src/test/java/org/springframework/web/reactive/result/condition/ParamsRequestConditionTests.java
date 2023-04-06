/*
 * Copyright 2002-2023 the original author or authors.
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
 * Unit tests for {@link ParamsRequestCondition}.
 * @author Rossen Stoyanchev
 */
public class ParamsRequestConditionTests {

	@Test
	public void paramEquals() {
		assertThat(new ParamsRequestCondition("foo")).isEqualTo(new ParamsRequestCondition("foo"));
		assertThat(new ParamsRequestCondition("foo").equals(new ParamsRequestCondition("bar"))).isFalse();
		assertThat(new ParamsRequestCondition("foo").equals(new ParamsRequestCondition("FOO"))).isFalse();
		assertThat(new ParamsRequestCondition("foo=bar")).isEqualTo(new ParamsRequestCondition("foo=bar"));
		assertThat(new ParamsRequestCondition("foo=bar").equals(new ParamsRequestCondition("FOO=bar"))).isFalse();
	}

	@Test
	public void paramPresent() throws Exception {
		ParamsRequestCondition condition = new ParamsRequestCondition("foo");
		assertThat(condition.getMatchingCondition(MockServerWebExchange.from(get("/path?foo=")))).isNotNull();
	}

	@Test // SPR-15831
	public void paramPresentNullValue() throws Exception {
		ParamsRequestCondition condition = new ParamsRequestCondition("foo");
		assertThat(condition.getMatchingCondition(MockServerWebExchange.from(get("/path?foo")))).isNotNull();
	}

	@Test
	public void paramPresentNoMatch() throws Exception {
		ParamsRequestCondition condition = new ParamsRequestCondition("foo");
		assertThat(condition.getMatchingCondition(MockServerWebExchange.from(get("/path?bar=")))).isNull();
	}

	@Test
	public void paramNotPresent() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/"));
		assertThat(new ParamsRequestCondition("!foo").getMatchingCondition(exchange)).isNotNull();
	}

	@Test
	public void paramValueMatch() throws Exception {
		ParamsRequestCondition condition = new ParamsRequestCondition("foo=bar");
		assertThat(condition.getMatchingCondition(MockServerWebExchange.from(get("/path?foo=bar")))).isNotNull();
	}

	@Test
	public void paramValueNoMatch() throws Exception {
		ParamsRequestCondition condition = new ParamsRequestCondition("foo=bar");
		assertThat(condition.getMatchingCondition(MockServerWebExchange.from(get("/path?foo=bazz")))).isNull();
	}

	@Test
	public void compareTo() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/"));

		ParamsRequestCondition condition1 = new ParamsRequestCondition("foo", "bar", "baz");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("foo", "bar");

		int result = condition1.compareTo(condition2, exchange);
		assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);

		result = condition2.compareTo(condition1, exchange);
		assertThat(result).as("Invalid comparison result: " + result).isGreaterThan(0);
	}

	@Test // SPR-16674
	public void compareToWithMoreSpecificMatchByValue() {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/"));

		ParamsRequestCondition condition1 = new ParamsRequestCondition("response_type=code");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("response_type");

		int result = condition1.compareTo(condition2, exchange);
		assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);
	}

	@Test
	public void compareToWithNegatedMatch() {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/"));

		ParamsRequestCondition condition1 = new ParamsRequestCondition("response_type!=code");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("response_type");

		assertThat(condition1.compareTo(condition2, exchange)).as("Negated match should not count as more specific").isEqualTo(0);
	}

	@Test
	public void combine() {
		ParamsRequestCondition condition1 = new ParamsRequestCondition("foo=bar");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("foo=baz");

		ParamsRequestCondition result = condition1.combine(condition2);
		Collection<?> conditions = result.getContent();
		assertThat(conditions).hasSize(2);
	}

}
