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

package org.springframework.web.servlet.mvc.method.condition;

import java.util.Set;

import org.junit.Test;

import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class ConsumesRequestConditionTests {

	@Test
	public void consumesMatch() {
		RequestCondition condition = new ConsumesRequestCondition("text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("text/plain");

		assertTrue(condition.match(request));
	}
	
	@Test
	public void negatedConsumesMatch() {
		RequestCondition condition = new ConsumesRequestCondition("!text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("text/plain");

		assertFalse(condition.match(request));
	}

	@Test
	public void consumesWildcardMatch() {
		RequestCondition condition = new ConsumesRequestCondition("text/*");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("text/plain");

		assertTrue(condition.match(request));
	}

	@Test
	public void consumesMultipleMatch() {
		RequestCondition condition = new ConsumesRequestCondition("text/plain", "application/xml");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("text/plain");

		assertTrue(condition.match(request));
	}

	@Test
	public void consumesSingleNoMatch() {
		RequestCondition condition = new ConsumesRequestCondition("text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("application/xml");

		assertFalse(condition.match(request));
	}

	@Test
	public void compareToSingle() {
		ConsumesRequestCondition condition1 = new ConsumesRequestCondition("text/plain");
		ConsumesRequestCondition condition2 = new ConsumesRequestCondition("text/*");

		int result = condition1.compareTo(condition2);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1);
		assertTrue("Invalid comparison result: " + result, result > 0);
	}

	@Test
	public void compareToMultiple() {
		ConsumesRequestCondition condition1 = new ConsumesRequestCondition("*/*", "text/plain");
		ConsumesRequestCondition condition2 = new ConsumesRequestCondition("text/*", "text/plain;q=0.7");

		int result = condition1.compareTo(condition2);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1);
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
		ConsumesRequestCondition condition2 = new ConsumesRequestCondition("*/*");

		ConsumesRequestCondition result = condition1.combine(condition2);
		assertEquals(condition1, result);
	}

	@Test
	public void parseConsumesAndHeaders() {
		String[] consumes = new String[] {"text/plain"};
		String[] headers = new String[]{"foo=bar", "content-type=application/xml,application/pdf"};
		ConsumesRequestCondition condition = RequestConditionFactory.parseConsumes(consumes, headers);

		assertConditions(condition, "text/plain", "application/xml", "application/pdf");
	}

	@Test
	public void parseConsumesDefault() {
		String[] consumes = new String[] {"*/*"};
		String[] headers = new String[0];
		ConsumesRequestCondition condition = RequestConditionFactory.parseConsumes(consumes, headers);

		assertConditions(condition, "*/*");
	}
	@Test
	public void parseConsumesDefaultAndHeaders() {
		String[] consumes = new String[] {"*/*"};
		String[] headers = new String[]{"foo=bar", "content-type=text/plain"};
		ConsumesRequestCondition condition = RequestConditionFactory.parseConsumes(consumes, headers);

		assertConditions(condition, "text/plain");
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
		Set<ConsumesRequestCondition.ConsumeRequestCondition> conditions = condition.getConditions();
		assertEquals("Invalid amount of conditions", conditions.size(), expected.length);
		for (String s : expected) {
			boolean found = false;
			for (ConsumesRequestCondition.ConsumeRequestCondition requestCondition : conditions) {
				String conditionMediaType = requestCondition.getMediaType().toString();
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
