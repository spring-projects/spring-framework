/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.mvc.condition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition.ProduceMediaTypeExpression;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class ProducesRequestConditionTests {
	
	@Test
	public void producesMatch() {
		ProducesRequestCondition condition = new ProducesRequestCondition("text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/plain");

		assertNotNull(condition.getMatchingCondition(request));
	}
	
	@Test
	public void negatedProducesMatch() {
		ProducesRequestCondition condition = new ProducesRequestCondition("!text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/plain");

		assertNull(condition.getMatchingCondition(request));
	}

	@Test
	public void getProducibleMediaTypesNegatedExpression() {
		ProducesRequestCondition condition = new ProducesRequestCondition("!application/xml");
		assertEquals(Collections.emptySet(), condition.getProducibleMediaTypes());
	}
	
	@Test
	public void producesWildcardMatch() {
		ProducesRequestCondition condition = new ProducesRequestCondition("text/*");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/plain");

		assertNotNull(condition.getMatchingCondition(request));
	}

	@Test
	public void producesMultipleMatch() {
		ProducesRequestCondition condition = new ProducesRequestCondition("text/plain", "application/xml");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/plain");

		assertNotNull(condition.getMatchingCondition(request));
	}

	@Test
	public void producesSingleNoMatch() {
		ProducesRequestCondition condition = new ProducesRequestCondition("text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "application/xml");

		assertNull(condition.getMatchingCondition(request));
	}

	@Test
	public void compareTo() {
		ProducesRequestCondition html = new ProducesRequestCondition("text/html");
		ProducesRequestCondition xml = new ProducesRequestCondition("application/xml");
		ProducesRequestCondition none = new ProducesRequestCondition();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "application/xml, text/html");

		assertTrue(html.compareTo(xml, request) > 0);
		assertTrue(xml.compareTo(html, request) < 0);
		assertTrue(xml.compareTo(none, request) < 0);
		assertTrue(none.compareTo(xml, request) > 0);
		assertTrue(html.compareTo(none, request) < 0);
		assertTrue(none.compareTo(html, request) > 0);

		request = new MockHttpServletRequest();
		request.addHeader("Accept", "application/xml, text/*");

		assertTrue(html.compareTo(xml, request) > 0);
		assertTrue(xml.compareTo(html, request) < 0);

		request = new MockHttpServletRequest();
		request.addHeader("Accept", "application/pdf");

		assertTrue(html.compareTo(xml, request) == 0);
		assertTrue(xml.compareTo(html, request) == 0);

		// See SPR-7000
		request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/html;q=0.9,application/xml");

		assertTrue(html.compareTo(xml, request) > 0);
		assertTrue(xml.compareTo(html, request) < 0);
	}
	
	@Test
	public void compareToWithSingleExpression() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/plain");
		
		ProducesRequestCondition condition1 = new ProducesRequestCondition("text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("text/*");

		int result = condition1.compareTo(condition2, request);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1, request);
		assertTrue("Invalid comparison result: " + result, result > 0);
	}

	@Test
	public void compareToMultipleExpressions() {
		ProducesRequestCondition condition1 = new ProducesRequestCondition("*/*", "text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("text/*", "text/plain;q=0.7");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/plain");

		int result = condition1.compareTo(condition2, request);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1, request);
		assertTrue("Invalid comparison result: " + result, result > 0);
	}

	@Test
	public void compareToMultipleExpressionsAndMultipeAcceptHeaderValues() {
		ProducesRequestCondition condition1 = new ProducesRequestCondition("text/*", "text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("application/*", "application/xml");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/plain");
		request.addHeader("Accept", "application/xml");

		int result = condition1.compareTo(condition2, request);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1, request);
		assertTrue("Invalid comparison result: " + result, result > 0);

		request = new MockHttpServletRequest();
		request.addHeader("Accept", "application/xml");
		request.addHeader("Accept", "text/plain");

		result = condition1.compareTo(condition2, request);
		assertTrue("Invalid comparison result: " + result, result > 0);

		result = condition2.compareTo(condition1, request);
		assertTrue("Invalid comparison result: " + result, result < 0);
	}

	// SPR-8536

	@Test
	public void compareToMediaTypeAll() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		
		ProducesRequestCondition condition1 = new ProducesRequestCondition();
		ProducesRequestCondition condition2 = new ProducesRequestCondition("application/json");

		assertTrue("Should have picked '*/*' condition as an exact match",
				condition1.compareTo(condition2, request) < 0);
		assertTrue("Should have picked '*/*' condition as an exact match",
				condition2.compareTo(condition1, request) > 0);

		condition1 = new ProducesRequestCondition("*/*");
		condition2 = new ProducesRequestCondition("application/json");

		assertTrue(condition1.compareTo(condition2, request) < 0);
		assertTrue(condition2.compareTo(condition1, request) > 0);
	
		request.addHeader("Accept", "*/*");

		condition1 = new ProducesRequestCondition();
		condition2 = new ProducesRequestCondition("application/json");

		assertTrue(condition1.compareTo(condition2, request) < 0);
		assertTrue(condition2.compareTo(condition1, request) > 0);

		condition1 = new ProducesRequestCondition("*/*");
		condition2 = new ProducesRequestCondition("application/json");

		assertTrue(condition1.compareTo(condition2, request) < 0);
		assertTrue(condition2.compareTo(condition1, request) > 0);
	}

	// SPR-9021

	@Test
	public void compareToMediaTypeAllWithParameter() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "*/*;q=0.9");

		ProducesRequestCondition condition1 = new ProducesRequestCondition();
		ProducesRequestCondition condition2 = new ProducesRequestCondition("application/json");

		assertTrue(condition1.compareTo(condition2, request) < 0);
		assertTrue(condition2.compareTo(condition1, request) > 0);
	}

	@Test
	public void compareToEqualMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/*");

		ProducesRequestCondition condition1 = new ProducesRequestCondition("text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("text/xhtml");

		int result = condition1.compareTo(condition2, request);
		assertTrue("Should have used MediaType.equals(Object) to break the match", result < 0);

		result = condition2.compareTo(condition1, request);
		assertTrue("Should have used MediaType.equals(Object) to break the match", result > 0);
	}

	@Test
	public void combine() {
		ProducesRequestCondition condition1 = new ProducesRequestCondition("text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("application/xml");

		ProducesRequestCondition result = condition1.combine(condition2);
		assertEquals(condition2, result);
	}
	
	@Test
	public void combineWithDefault() {
		ProducesRequestCondition condition1 = new ProducesRequestCondition("text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition();

		ProducesRequestCondition result = condition1.combine(condition2);
		assertEquals(condition1, result);
	}

	@Test
	public void parseProducesAndHeaders() {
		String[] produces = new String[] {"text/plain"};
		String[] headers = new String[]{"foo=bar", "accept=application/xml,application/pdf"};
		ProducesRequestCondition condition = new ProducesRequestCondition(produces, headers);

		assertConditions(condition, "text/plain", "application/xml", "application/pdf");
	}

	@Test
	public void getMatchingCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/plain");

		ProducesRequestCondition condition = new ProducesRequestCondition("text/plain", "application/xml");

		ProducesRequestCondition result = condition.getMatchingCondition(request);
		assertConditions(result, "text/plain");

		condition = new ProducesRequestCondition("application/xml");

		result = condition.getMatchingCondition(request);
		assertNull(result);
	}

	private void assertConditions(ProducesRequestCondition condition, String... expected) {
		Collection<ProduceMediaTypeExpression> expressions = condition.getContent();
		assertEquals("Invalid amount of conditions", expressions.size(), expected.length);
		for (String s : expected) {
			boolean found = false;
			for (ProduceMediaTypeExpression expr : expressions) {
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

}
