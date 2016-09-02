/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.result.condition;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link ProducesRequestCondition}.
 *
 * @author Rossen Stoyanchev
 */
public class ProducesRequestConditionTests {

	@Test
	public void match() throws Exception {
		ServerWebExchange exchange = createExchange("text/plain");
		ProducesRequestCondition condition = new ProducesRequestCondition("text/plain");

		assertNotNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void matchNegated() throws Exception {
		ServerWebExchange exchange = createExchange("text/plain");
		ProducesRequestCondition condition = new ProducesRequestCondition("!text/plain");

		assertNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void getProducibleMediaTypes() throws Exception {
		ProducesRequestCondition condition = new ProducesRequestCondition("!application/xml");
		assertEquals(Collections.emptySet(), condition.getProducibleMediaTypes());
	}

	@Test
	public void matchWildcard() throws Exception {
		ServerWebExchange exchange = createExchange("text/plain");
		ProducesRequestCondition condition = new ProducesRequestCondition("text/*");

		assertNotNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void matchMultiple() throws Exception {
		ServerWebExchange exchange = createExchange("text/plain");
		ProducesRequestCondition condition = new ProducesRequestCondition("text/plain", "application/xml");

		assertNotNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void matchSingle() throws Exception {
		ServerWebExchange exchange = createExchange("application/xml");
		ProducesRequestCondition condition = new ProducesRequestCondition("text/plain");

		assertNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void matchParseError() throws Exception {
		ServerWebExchange exchange = createExchange("bogus");
		ProducesRequestCondition condition = new ProducesRequestCondition("text/plain");

		assertNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void matchParseErrorWithNegation() throws Exception {
		ServerWebExchange exchange = createExchange("bogus");
		ProducesRequestCondition condition = new ProducesRequestCondition("!text/plain");

		assertNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void compareTo() throws Exception {
		ProducesRequestCondition html = new ProducesRequestCondition("text/html");
		ProducesRequestCondition xml = new ProducesRequestCondition("application/xml");
		ProducesRequestCondition none = new ProducesRequestCondition();

		ServerWebExchange exchange = createExchange("application/xml, text/html");

		assertTrue(html.compareTo(xml, exchange) > 0);
		assertTrue(xml.compareTo(html, exchange) < 0);
		assertTrue(xml.compareTo(none, exchange) < 0);
		assertTrue(none.compareTo(xml, exchange) > 0);
		assertTrue(html.compareTo(none, exchange) < 0);
		assertTrue(none.compareTo(html, exchange) > 0);

		exchange = createExchange("application/xml, text/*");

		assertTrue(html.compareTo(xml, exchange) > 0);
		assertTrue(xml.compareTo(html, exchange) < 0);

		exchange = createExchange("application/pdf");

		assertTrue(html.compareTo(xml, exchange) == 0);
		assertTrue(xml.compareTo(html, exchange) == 0);

		// See SPR-7000
		exchange = createExchange("text/html;q=0.9,application/xml");

		assertTrue(html.compareTo(xml, exchange) > 0);
		assertTrue(xml.compareTo(html, exchange) < 0);
	}

	@Test
	public void compareToWithSingleExpression() throws Exception {
		ServerWebExchange exchange = createExchange("text/plain");

		ProducesRequestCondition condition1 = new ProducesRequestCondition("text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("text/*");

		int result = condition1.compareTo(condition2, exchange);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1, exchange);
		assertTrue("Invalid comparison result: " + result, result > 0);
	}

	@Test
	public void compareToMultipleExpressions() throws Exception {
		ProducesRequestCondition condition1 = new ProducesRequestCondition("*/*", "text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("text/*", "text/plain;q=0.7");

		ServerWebExchange exchange = createExchange("text/plain");

		int result = condition1.compareTo(condition2, exchange);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1, exchange);
		assertTrue("Invalid comparison result: " + result, result > 0);
	}

	@Test
	public void compareToMultipleExpressionsAndMultipeAcceptHeaderValues() throws Exception {
		ProducesRequestCondition condition1 = new ProducesRequestCondition("text/*", "text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("application/*", "application/xml");

		ServerWebExchange exchange = createExchange("text/plain", "application/xml");

		int result = condition1.compareTo(condition2, exchange);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1, exchange);
		assertTrue("Invalid comparison result: " + result, result > 0);

		exchange = createExchange("application/xml", "text/plain");

		result = condition1.compareTo(condition2, exchange);
		assertTrue("Invalid comparison result: " + result, result > 0);

		result = condition2.compareTo(condition1, exchange);
		assertTrue("Invalid comparison result: " + result, result < 0);
	}

	// SPR-8536

	@Test
	public void compareToMediaTypeAll() throws Exception {
		ServerWebExchange exchange = createExchange();

		ProducesRequestCondition condition1 = new ProducesRequestCondition();
		ProducesRequestCondition condition2 = new ProducesRequestCondition("application/json");

		assertTrue("Should have picked '*/*' condition as an exact match",
				condition1.compareTo(condition2, exchange) < 0);
		assertTrue("Should have picked '*/*' condition as an exact match",
				condition2.compareTo(condition1, exchange) > 0);

		condition1 = new ProducesRequestCondition("*/*");
		condition2 = new ProducesRequestCondition("application/json");

		assertTrue(condition1.compareTo(condition2, exchange) < 0);
		assertTrue(condition2.compareTo(condition1, exchange) > 0);

		exchange = createExchange("*/*");

		condition1 = new ProducesRequestCondition();
		condition2 = new ProducesRequestCondition("application/json");

		assertTrue(condition1.compareTo(condition2, exchange) < 0);
		assertTrue(condition2.compareTo(condition1, exchange) > 0);

		condition1 = new ProducesRequestCondition("*/*");
		condition2 = new ProducesRequestCondition("application/json");

		assertTrue(condition1.compareTo(condition2, exchange) < 0);
		assertTrue(condition2.compareTo(condition1, exchange) > 0);
	}

	// SPR-9021

	@Test
	public void compareToMediaTypeAllWithParameter() throws Exception {
		ServerWebExchange exchange = createExchange("*/*;q=0.9");

		ProducesRequestCondition condition1 = new ProducesRequestCondition();
		ProducesRequestCondition condition2 = new ProducesRequestCondition("application/json");

		assertTrue(condition1.compareTo(condition2, exchange) < 0);
		assertTrue(condition2.compareTo(condition1, exchange) > 0);
	}

	@Test
	public void compareToEqualMatch() throws Exception {
		ServerWebExchange exchange = createExchange("text/*");

		ProducesRequestCondition condition1 = new ProducesRequestCondition("text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("text/xhtml");

		int result = condition1.compareTo(condition2, exchange);
		assertTrue("Should have used MediaType.equals(Object) to break the match", result < 0);

		result = condition2.compareTo(condition1, exchange);
		assertTrue("Should have used MediaType.equals(Object) to break the match", result > 0);
	}

	@Test
	public void combine() throws Exception {
		ProducesRequestCondition condition1 = new ProducesRequestCondition("text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("application/xml");

		ProducesRequestCondition result = condition1.combine(condition2);
		assertEquals(condition2, result);
	}

	@Test
	public void combineWithDefault() throws Exception {
		ProducesRequestCondition condition1 = new ProducesRequestCondition("text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition();

		ProducesRequestCondition result = condition1.combine(condition2);
		assertEquals(condition1, result);
	}

	@Test
	public void instantiateWithProducesAndHeaderConditions() throws Exception {
		String[] produces = new String[] {"text/plain"};
		String[] headers = new String[]{"foo=bar", "accept=application/xml,application/pdf"};
		ProducesRequestCondition condition = new ProducesRequestCondition(produces, headers);

		assertConditions(condition, "text/plain", "application/xml", "application/pdf");
	}

	@Test
	public void getMatchingCondition() throws Exception {
		ServerWebExchange exchange = createExchange("text/plain");

		ProducesRequestCondition condition = new ProducesRequestCondition("text/plain", "application/xml");

		ProducesRequestCondition result = condition.getMatchingCondition(exchange);
		assertConditions(result, "text/plain");

		condition = new ProducesRequestCondition("application/xml");

		result = condition.getMatchingCondition(exchange);
		assertNull(result);
	}

	private void assertConditions(ProducesRequestCondition condition, String... expected) {
		Collection<ProducesRequestCondition.ProduceMediaTypeExpression> expressions = condition.getContent();
		assertEquals("Invalid number of conditions", expressions.size(), expected.length);
		for (String s : expected) {
			boolean found = false;
			for (ProducesRequestCondition.ProduceMediaTypeExpression expr : expressions) {
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


	private ServerWebExchange createExchange(String... accept) throws URISyntaxException {
		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, "/");
		if (accept != null) {
			for (String value : accept) {
				request.getHeaders().add("Accept", value);
			}
		}
		WebSessionManager sessionManager = new MockWebSessionManager();
		return new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);
	}

}
