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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class ProducesRequestConditionTests {

	@Test
	public void consumesMatch() {
		RequestCondition condition = new ProducesRequestCondition("text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/plain");

		assertTrue(condition.match(request));
	}
	
	@Test
	public void negatedConsumesMatch() {
		RequestCondition condition = new ProducesRequestCondition("!text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/plain");

		assertFalse(condition.match(request));
	}

	@Test
	public void consumesWildcardMatch() {
		RequestCondition condition = new ProducesRequestCondition("text/*");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/plain");

		assertTrue(condition.match(request));
	}

	@Test
	public void consumesMultipleMatch() {
		RequestCondition condition = new ProducesRequestCondition("text/plain", "application/xml");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/plain");

		assertTrue(condition.match(request));
	}

	@Test
	public void consumesSingleNoMatch() {
		RequestCondition condition = new ProducesRequestCondition("text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "application/xml");

		assertFalse(condition.match(request));
	}

	@Test
	public void compareToSingle() {
		ProducesRequestCondition condition1 = new ProducesRequestCondition("text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("text/*");

		List<MediaType> accept = Collections.singletonList(MediaType.TEXT_PLAIN);

		int result = condition1.compareTo(condition2, accept);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1, accept);
		assertTrue("Invalid comparison result: " + result, result > 0);
	}

	@Test
	public void compareToMultiple() {
		ProducesRequestCondition condition1 = new ProducesRequestCondition("*/*", "text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("text/*", "text/plain;q=0.7");

		List<MediaType> accept = Collections.singletonList(MediaType.TEXT_PLAIN);

		int result = condition1.compareTo(condition2, accept);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1, accept);
		assertTrue("Invalid comparison result: " + result, result > 0);

		condition1 = new ProducesRequestCondition("*/*");
		condition2 = new ProducesRequestCondition("text/*");

		accept = Collections.singletonList(new MediaType("text", "*"));

		result = condition1.compareTo(condition2, accept);
		assertTrue("Invalid comparison result: " + result, result > 0);

		result = condition2.compareTo(condition1, accept);
		assertTrue("Invalid comparison result: " + result, result < 0);
	}

	@Test
	public void compareToMultipleAccept() {
		ProducesRequestCondition condition1 = new ProducesRequestCondition("text/*", "text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("application/*", "application/xml");

		List<MediaType> accept = Arrays.asList(MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML);

		int result = condition1.compareTo(condition2, accept);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1, accept);
		assertTrue("Invalid comparison result: " + result, result > 0);

		accept = Arrays.asList(MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN);

		result = condition1.compareTo(condition2, accept);
		assertTrue("Invalid comparison result: " + result, result > 0);

		result = condition2.compareTo(condition1, accept);
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
		ProducesRequestCondition condition = RequestConditionFactory.parseProduces(consumes, headers);

		assertConditions(condition, "text/plain", "application/xml", "application/pdf");
	}

	@Test
	public void parseConsumesDefault() {
		String[] consumes = new String[] {"*/*"};
		String[] headers = new String[0];
		ProducesRequestCondition condition = RequestConditionFactory.parseProduces(consumes, headers);

		assertConditions(condition, "*/*");
	}
	@Test
	public void parseConsumesDefaultAndHeaders() {
		String[] consumes = new String[] {"*/*"};
		String[] headers = new String[]{"foo=bar", "accept=text/plain"};
		ProducesRequestCondition condition = RequestConditionFactory.parseProduces(consumes, headers);

		assertConditions(condition, "text/plain");
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
		Set<ProducesRequestCondition.ProduceRequestCondition> conditions = condition.getConditions();
		assertEquals("Invalid amount of conditions", conditions.size(), expected.length);
		for (String s : expected) {
			boolean found = false;
			for (ProducesRequestCondition.ProduceRequestCondition requestCondition : conditions) {
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
