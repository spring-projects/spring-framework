/*
 * Copyright 2002-2012 the original author or authors.
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

import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.reactive.result.condition.ConsumesRequestCondition.ConsumeMediaTypeExpression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Arjen Poutsma
 */
public class ConsumesRequestConditionTests {

	@Test
	public void consumesMatch() throws Exception {
		MockServerWebExchange exchange = postExchange("text/plain");
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain");

		assertNotNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void negatedConsumesMatch() throws Exception {
		MockServerWebExchange exchange = postExchange("text/plain");
		ConsumesRequestCondition condition = new ConsumesRequestCondition("!text/plain");

		assertNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void getConsumableMediaTypesNegatedExpression() throws Exception {
		ConsumesRequestCondition condition = new ConsumesRequestCondition("!application/xml");
		assertEquals(Collections.emptySet(), condition.getConsumableMediaTypes());
	}

	@Test
	public void consumesWildcardMatch() throws Exception {
		MockServerWebExchange exchange = postExchange("text/plain");
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/*");

		assertNotNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void consumesMultipleMatch() throws Exception {
		MockServerWebExchange exchange = postExchange("text/plain");
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain", "application/xml");

		assertNotNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void consumesSingleNoMatch() throws Exception {
		MockServerWebExchange exchange = postExchange("application/xml");
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain");

		assertNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void consumesParseError() throws Exception {
		MockServerWebExchange exchange = postExchange("01");
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain");

		assertNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void consumesParseErrorWithNegation() throws Exception {
		MockServerWebExchange exchange = postExchange("01");
		ConsumesRequestCondition condition = new ConsumesRequestCondition("!text/plain");

		assertNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void compareToSingle() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

		ConsumesRequestCondition condition1 = new ConsumesRequestCondition("text/plain");
		ConsumesRequestCondition condition2 = new ConsumesRequestCondition("text/*");

		int result = condition1.compareTo(condition2, exchange);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1, exchange);
		assertTrue("Invalid comparison result: " + result, result > 0);
	}

	@Test
	public void compareToMultiple() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

		ConsumesRequestCondition condition1 = new ConsumesRequestCondition("*/*", "text/plain");
		ConsumesRequestCondition condition2 = new ConsumesRequestCondition("text/*", "text/plain;q=0.7");

		int result = condition1.compareTo(condition2, exchange);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1, exchange);
		assertTrue("Invalid comparison result: " + result, result > 0);
	}


	@Test
	public void combine() throws Exception {
		ConsumesRequestCondition condition1 = new ConsumesRequestCondition("text/plain");
		ConsumesRequestCondition condition2 = new ConsumesRequestCondition("application/xml");

		ConsumesRequestCondition result = condition1.combine(condition2);
		assertEquals(condition2, result);
	}

	@Test
	public void combineWithDefault() throws Exception {
		ConsumesRequestCondition condition1 = new ConsumesRequestCondition("text/plain");
		ConsumesRequestCondition condition2 = new ConsumesRequestCondition();

		ConsumesRequestCondition result = condition1.combine(condition2);
		assertEquals(condition1, result);
	}

	@Test
	public void parseConsumesAndHeaders() throws Exception {
		String[] consumes = new String[] {"text/plain"};
		String[] headers = new String[]{"foo=bar", "content-type=application/xml,application/pdf"};
		ConsumesRequestCondition condition = new ConsumesRequestCondition(consumes, headers);

		assertConditions(condition, "text/plain", "application/xml", "application/pdf");
	}

	@Test
	public void getMatchingCondition() throws Exception {
		MockServerWebExchange exchange = postExchange("text/plain");
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain", "application/xml");

		ConsumesRequestCondition result = condition.getMatchingCondition(exchange);
		assertConditions(result, "text/plain");

		condition = new ConsumesRequestCondition("application/xml");
		result = condition.getMatchingCondition(exchange);
		assertNull(result);
	}

	private void assertConditions(ConsumesRequestCondition condition, String... expected) {
		Collection<ConsumeMediaTypeExpression> expressions = condition.getContent();
		assertEquals("Invalid amount of conditions", expressions.size(), expected.length);
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
