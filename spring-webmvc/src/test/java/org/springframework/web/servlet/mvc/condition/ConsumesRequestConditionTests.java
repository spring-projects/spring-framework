/*
 * Copyright 2002-2012 the original author or authors.
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
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition.ConsumeMediaTypeExpression;

/**
 * @author Arjen Poutsma
 */
public class ConsumesRequestConditionTests {

	@Test
	public void consumesMatch() {
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("text/plain");

		assertNotNull(condition.getMatchingCondition(request));
	}

	@Test
	public void negatedConsumesMatch() {
		ConsumesRequestCondition condition = new ConsumesRequestCondition("!text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("text/plain");

		assertNull(condition.getMatchingCondition(request));
	}

	@Test
	public void getConsumableMediaTypesNegatedExpression() {
		ConsumesRequestCondition condition = new ConsumesRequestCondition("!application/xml");
		assertEquals(Collections.emptySet(), condition.getConsumableMediaTypes());
	}

	@Test
	public void consumesWildcardMatch() {
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/*");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("text/plain");

		assertNotNull(condition.getMatchingCondition(request));
	}

	@Test
	public void consumesMultipleMatch() {
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain", "application/xml");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("text/plain");

		assertNotNull(condition.getMatchingCondition(request));
	}

	@Test
	public void consumesSingleNoMatch() {
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("application/xml");

		assertNull(condition.getMatchingCondition(request));
	}

	@Test
	public void consumesParseError() {
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("01");

		assertNull(condition.getMatchingCondition(request));
	}

	@Test
	public void consumesParseErrorWithNegation() {
		ConsumesRequestCondition condition = new ConsumesRequestCondition("!text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("01");

		assertNull(condition.getMatchingCondition(request));
	}

	@Test
	public void compareToSingle() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		ConsumesRequestCondition condition1 = new ConsumesRequestCondition("text/plain");
		ConsumesRequestCondition condition2 = new ConsumesRequestCondition("text/*");

		int result = condition1.compareTo(condition2, request);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1, request);
		assertTrue("Invalid comparison result: " + result, result > 0);
	}

	@Test
	public void compareToMultiple() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		ConsumesRequestCondition condition1 = new ConsumesRequestCondition("*/*", "text/plain");
		ConsumesRequestCondition condition2 = new ConsumesRequestCondition("text/*", "text/plain;q=0.7");

		int result = condition1.compareTo(condition2, request);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1, request);
		assertTrue("Invalid comparison result: " + result, result > 0);
	}


	@Test
	public void combine() {
		ConsumesRequestCondition condition1 = new ConsumesRequestCondition("text/plain");
		ConsumesRequestCondition condition2 = new ConsumesRequestCondition("application/xml");

		ConsumesRequestCondition result = condition1.combine(condition2);
		assertEquals(condition2, result);
	}

	@Test
	public void combineWithDefault() {
		ConsumesRequestCondition condition1 = new ConsumesRequestCondition("text/plain");
		ConsumesRequestCondition condition2 = new ConsumesRequestCondition();

		ConsumesRequestCondition result = condition1.combine(condition2);
		assertEquals(condition1, result);
	}

	@Test
	public void parseConsumesAndHeaders() {
		String[] consumes = new String[] {"text/plain"};
		String[] headers = new String[]{"foo=bar", "content-type=application/xml,application/pdf"};
		ConsumesRequestCondition condition = new ConsumesRequestCondition(consumes, headers);

		assertConditions(condition, "text/plain", "application/xml", "application/pdf");
	}

	@Test
	public void getMatchingCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("text/plain");

		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain", "application/xml");

		ConsumesRequestCondition result = condition.getMatchingCondition(request);
		assertConditions(result, "text/plain");

		condition = new ConsumesRequestCondition("application/xml");

		result = condition.getMatchingCondition(request);
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

}
