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

package org.springframework.web.servlet.mvc.condition;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition.HeaderExpression;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
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
		HeadersRequestCondition condition = new HeadersRequestCondition("accept");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "");

		assertThat(condition.getMatchingCondition(request)).isNotNull();
	}

	@Test
	public void headerPresentNoMatch() {
		HeadersRequestCondition condition = new HeadersRequestCondition("foo");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("bar", "");

		assertThat(condition.getMatchingCondition(request)).isNull();
	}

	@Test
	public void headerNotPresent() {
		HeadersRequestCondition condition = new HeadersRequestCondition("!accept");

		MockHttpServletRequest request = new MockHttpServletRequest();

		assertThat(condition.getMatchingCondition(request)).isNotNull();
	}

	@Test
	public void headerValueMatch() {
		HeadersRequestCondition condition = new HeadersRequestCondition("foo=bar");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("foo", "bar");

		assertThat(condition.getMatchingCondition(request)).isNotNull();
	}

	@Test
	public void headerValueNoMatch() {
		HeadersRequestCondition condition = new HeadersRequestCondition("foo=bar");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("foo", "bazz");

		assertThat(condition.getMatchingCondition(request)).isNull();
	}

	@Test
	public void headerCaseSensitiveValueMatch() {
		HeadersRequestCondition condition = new HeadersRequestCondition("foo=Bar");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("foo", "bar");

		assertThat(condition.getMatchingCondition(request)).isNull();
	}

	@Test
	public void headerValueMatchNegated() {
		HeadersRequestCondition condition = new HeadersRequestCondition("foo!=bar");
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("foo", "baz");

		assertThat(condition.getMatchingCondition(request)).isNotNull();
	}

	@Test
	public void headerValueNoMatchNegated() {
		HeadersRequestCondition condition = new HeadersRequestCondition("foo!=bar");
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("foo", "bar");

		assertThat(condition.getMatchingCondition(request)).isNull();
	}

	@Test
	public void compareTo() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		HeadersRequestCondition condition1 = new HeadersRequestCondition("foo", "bar", "baz");
		HeadersRequestCondition condition2 = new HeadersRequestCondition("foo=a", "bar");

		int result = condition1.compareTo(condition2, request);
		assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);

		result = condition2.compareTo(condition1, request);
		assertThat(result).as("Invalid comparison result: " + result).isGreaterThan(0);
	}

	@Test // SPR-16674
	public void compareToWithMoreSpecificMatchByValue() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		HeadersRequestCondition condition1 = new HeadersRequestCondition("foo=a");
		HeadersRequestCondition condition2 = new HeadersRequestCondition("foo");

		int result = condition1.compareTo(condition2, request);
		assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);

		result = condition2.compareTo(condition1, request);
		assertThat(result).as("Invalid comparison result: " + result).isGreaterThan(0);
	}

	@Test
	public void compareToWithNegatedMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		HeadersRequestCondition condition1 = new HeadersRequestCondition("foo!=a");
		HeadersRequestCondition condition2 = new HeadersRequestCondition("foo");

		assertThat(condition1.compareTo(condition2, request)).as("Negated match should not count as more specific").isEqualTo(0);
	}

	@Test
	public void combine() {
		HeadersRequestCondition condition1 = new HeadersRequestCondition("foo=bar");
		HeadersRequestCondition condition2 = new HeadersRequestCondition("foo=baz");

		HeadersRequestCondition result = condition1.combine(condition2);
		Collection<HeaderExpression> conditions = result.getContent();
		assertThat(conditions).hasSize(2);
	}

	@Test
	public void getMatchingCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("foo", "bar");

		HeadersRequestCondition condition = new HeadersRequestCondition("foo");

		HeadersRequestCondition result = condition.getMatchingCondition(request);
		assertThat(result).isEqualTo(condition);

		condition = new HeadersRequestCondition("bar");

		result = condition.getMatchingCondition(request);
		assertThat(result).isNull();
	}



}
