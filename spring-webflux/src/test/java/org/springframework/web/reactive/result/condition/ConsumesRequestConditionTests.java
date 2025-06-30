/*
 * Copyright 2002-present the original author or authors.
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
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.result.condition.ConsumesRequestCondition.ConsumeMediaTypeExpression;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Arjen Poutsma
 */
class ConsumesRequestConditionTests {

	@Test
	void consumesMatch() {
		MockServerWebExchange exchange = postExchange("text/plain");
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain");

		assertThat(condition.getMatchingCondition(exchange)).isNotNull();
	}

	@Test
	void negatedConsumesMatch() {
		MockServerWebExchange exchange = postExchange("text/plain");
		ConsumesRequestCondition condition = new ConsumesRequestCondition("!text/plain");

		assertThat(condition.getMatchingCondition(exchange)).isNull();
	}

	@Test
	void getConsumableMediaTypesNegatedExpression() {
		ConsumesRequestCondition condition = new ConsumesRequestCondition("!application/xml");
		assertThat(condition.getConsumableMediaTypes()).isEqualTo(Collections.emptySet());
	}

	@Test
	void consumesWildcardMatch() {
		MockServerWebExchange exchange = postExchange("text/plain");
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/*");

		assertThat(condition.getMatchingCondition(exchange)).isNotNull();
	}

	@Test
	void consumesMultipleMatch() {
		MockServerWebExchange exchange = postExchange("text/plain");
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain", "application/xml");

		assertThat(condition.getMatchingCondition(exchange)).isNotNull();
	}

	@Test
	void consumesSingleNoMatch() {
		MockServerWebExchange exchange = postExchange("application/xml");
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain");

		assertThat(condition.getMatchingCondition(exchange)).isNull();
	}

	@Test // gh-28024
	public void matchWithParameters() {
		String base = "application/hal+json";
		ConsumesRequestCondition condition = new ConsumesRequestCondition(base + ";profile=\"a\"");
		MockServerWebExchange exchange = postExchange(base + ";profile=\"a\"");
		assertThat(condition.getMatchingCondition(exchange)).isNotNull();

		condition = new ConsumesRequestCondition(base + ";profile=\"a\"");
		exchange = postExchange(base + ";profile=\"b\"");
		assertThat(condition.getMatchingCondition(exchange)).isNull();

		condition = new ConsumesRequestCondition(base + ";profile=\"a\"");
		exchange = postExchange(base);
		assertThat(condition.getMatchingCondition(exchange)).isNotNull();

		condition = new ConsumesRequestCondition(base);
		exchange = postExchange(base + ";profile=\"a\"");
		assertThat(condition.getMatchingCondition(exchange)).isNotNull();

		condition = new ConsumesRequestCondition(base + ";profile=\"a\"");
		exchange = postExchange(base + ";profile=\"A\"");
		assertThat(condition.getMatchingCondition(exchange)).isNotNull();
	}

	@Test
	void consumesParseError() {
		MockServerWebExchange exchange = postExchange("01");
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain");

		assertThat(condition.getMatchingCondition(exchange)).isNull();
	}

	@Test
	void consumesParseErrorWithNegation() {
		MockServerWebExchange exchange = postExchange("01");
		ConsumesRequestCondition condition = new ConsumesRequestCondition("!text/plain");

		assertThat(condition.getMatchingCondition(exchange)).isNull();
	}

	@Test // gh-22010
	public void consumesNoContent() {
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain");
		condition.setBodyRequired(false);

		MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
		assertThat(condition.getMatchingCondition(MockServerWebExchange.from(request))).isNotNull();

		request = MockServerHttpRequest.get("/").header(HttpHeaders.CONTENT_LENGTH, "0").build();
		assertThat(condition.getMatchingCondition(MockServerWebExchange.from(request))).isNotNull();

		request = MockServerHttpRequest.get("/").header(HttpHeaders.CONTENT_LENGTH, "21").build();
		assertThat(condition.getMatchingCondition(MockServerWebExchange.from(request))).isNull();

		request = MockServerHttpRequest.get("/").header(HttpHeaders.TRANSFER_ENCODING, "chunked").build();
		assertThat(condition.getMatchingCondition(MockServerWebExchange.from(request))).isNull();
	}

	@Test
	void compareToSingle() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

		ConsumesRequestCondition condition1 = new ConsumesRequestCondition("text/plain");
		ConsumesRequestCondition condition2 = new ConsumesRequestCondition("text/*");

		int result = condition1.compareTo(condition2, exchange);
		assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);

		result = condition2.compareTo(condition1, exchange);
		assertThat(result).as("Invalid comparison result: " + result).isGreaterThan(0);
	}

	@Test
	void compareToMultiple() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

		ConsumesRequestCondition condition1 = new ConsumesRequestCondition("*/*", "text/plain");
		ConsumesRequestCondition condition2 = new ConsumesRequestCondition("text/*", "text/plain;q=0.7");

		int result = condition1.compareTo(condition2, exchange);
		assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);

		result = condition2.compareTo(condition1, exchange);
		assertThat(result).as("Invalid comparison result: " + result).isGreaterThan(0);
	}


	@Test
	void combine() {
		ConsumesRequestCondition condition1 = new ConsumesRequestCondition("text/plain");
		ConsumesRequestCondition condition2 = new ConsumesRequestCondition("application/xml");

		ConsumesRequestCondition result = condition1.combine(condition2);
		assertThat(result).isEqualTo(condition2);
	}

	@Test
	void combineWithDefault() {
		ConsumesRequestCondition condition1 = new ConsumesRequestCondition("text/plain");
		ConsumesRequestCondition condition2 = new ConsumesRequestCondition();

		ConsumesRequestCondition result = condition1.combine(condition2);
		assertThat(result).isEqualTo(condition1);
	}

	@Test
	void parseConsumesAndHeaders() {
		String[] consumes = new String[] {"text/plain"};
		String[] headers = new String[]{"foo=bar", "content-type=application/xml,application/pdf"};
		ConsumesRequestCondition condition = new ConsumesRequestCondition(consumes, headers);

		assertConditions(condition, "text/plain", "application/xml", "application/pdf");
	}

	@Test
	void getMatchingCondition() {
		MockServerWebExchange exchange = postExchange("text/plain");
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain", "application/xml");

		ConsumesRequestCondition result = condition.getMatchingCondition(exchange);
		assertConditions(result, "text/plain");

		condition = new ConsumesRequestCondition("application/xml");
		result = condition.getMatchingCondition(exchange);
		assertThat(result).isNull();
	}

	private void assertConditions(ConsumesRequestCondition condition, String... expected) {
		Collection<ConsumeMediaTypeExpression> expressions = condition.getContent();
		assertThat(expected).as("Invalid amount of conditions").hasSameSizeAs(expressions);
		for (String s : expected) {
			boolean found = false;
			for (ConsumeMediaTypeExpression expr : expressions) {
				String conditionMediaType = expr.getMediaType().toString();
				if (conditionMediaType.equals(s)) {
					found = true;
					break;

				}
			}
			if (!found) {
				fail("Condition [" + s + "] not found");
			}
		}
	}

	private MockServerWebExchange postExchange(String contentType) {
		return MockServerWebExchange.from(
				MockServerHttpRequest.post("/").header(HttpHeaders.CONTENT_TYPE, contentType));
	}

}
