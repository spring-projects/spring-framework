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

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition.ProduceMediaTypeExpression;

/**
 * @author Arjen Poutsma
 */
public class ProducesRequestConditionTests {

	@Test
	public void consumesMatch() {
		ProducesRequestCondition condition = new ProducesRequestCondition("text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/plain");

		assertNotNull(condition.getMatchingCondition(request));
	}
	
	@Test
	public void negatedConsumesMatch() {
		ProducesRequestCondition condition = new ProducesRequestCondition("!text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/plain");

		assertNull(condition.getMatchingCondition(request));
	}

	@Test
	public void consumesWildcardMatch() {
		ProducesRequestCondition condition = new ProducesRequestCondition("text/*");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/plain");

		assertNotNull(condition.getMatchingCondition(request));
	}

	@Test
	public void consumesMultipleMatch() {
		ProducesRequestCondition condition = new ProducesRequestCondition("text/plain", "application/xml");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/plain");

		assertNotNull(condition.getMatchingCondition(request));
	}

	@Test
	public void consumesSingleNoMatch() {
		ProducesRequestCondition condition = new ProducesRequestCondition("text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "application/xml");

		assertNull(condition.getMatchingCondition(request));
	}

	@Test
	public void compareToSingle() {
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
	public void compareToMultiple() {
		ProducesRequestCondition condition1 = new ProducesRequestCondition("*/*", "text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("text/*", "text/plain;q=0.7");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/plain");

		int result = condition1.compareTo(condition2, request);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1, request);
		assertTrue("Invalid comparison result: " + result, result > 0);

		condition1 = new ProducesRequestCondition("*/*");
		condition2 = new ProducesRequestCondition("text/*");

		request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/*");

		result = condition1.compareTo(condition2, request);
		assertTrue("Invalid comparison result: " + result, result > 0);

		result = condition2.compareTo(condition1, request);
		assertTrue("Invalid comparison result: " + result, result < 0);
	}

	@Test
	public void compareToMultipleAccept() {
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
	public void parseConsumesAndHeaders() {
		String[] consumes = new String[] {"text/plain"};
		String[] headers = new String[]{"foo=bar", "accept=application/xml,application/pdf"};
		ProducesRequestCondition condition = new ProducesRequestCondition(consumes, headers);

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
