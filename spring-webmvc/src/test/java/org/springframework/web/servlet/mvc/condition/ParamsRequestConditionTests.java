/*
 * Copyright 2002-2024 the original author or authors.
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

import org.springframework.web.servlet.mvc.condition.ParamsRequestCondition.ParamExpression;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ParamsRequestCondition}.
 *
 * @author Arjen Poutsma
 * @author Stephane Nicoll
 */
class ParamsRequestConditionTests {

	@Test
	void paramEquals() {
		assertThat(new ParamsRequestCondition("foo")).isEqualTo(new ParamsRequestCondition("foo"));
		assertThat(new ParamsRequestCondition("foo")).isNotEqualTo(new ParamsRequestCondition("bar"));
		assertThat(new ParamsRequestCondition("foo")).isNotEqualTo(new ParamsRequestCondition("FOO"));
		assertThat(new ParamsRequestCondition("foo=bar")).isEqualTo(new ParamsRequestCondition("foo=bar"));
		assertThat(new ParamsRequestCondition("foo=bar")).isNotEqualTo(new ParamsRequestCondition("FOO=bar"));
	}

	@Test
	void paramPresent() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("foo", "");

		assertThat(new ParamsRequestCondition("foo").getMatchingCondition(request)).isNotNull();
	}

	@Test // SPR-15831
	void paramPresentNullValue() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("foo", (String) null);

		assertThat(new ParamsRequestCondition("foo").getMatchingCondition(request)).isNotNull();
	}

	@Test
	void paramPresentNoMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("bar", "");

		assertThat(new ParamsRequestCondition("foo").getMatchingCondition(request)).isNull();
	}

	@Test
	void paramNotPresent() {
		ParamsRequestCondition condition = new ParamsRequestCondition("!foo");
		MockHttpServletRequest request = new MockHttpServletRequest();

		assertThat(condition.getMatchingCondition(request)).isNotNull();
	}

	@Test
	void paramValueMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("foo", "bar");

		assertThat(new ParamsRequestCondition("foo=bar").getMatchingCondition(request)).isNotNull();
	}

	@Test
	void paramValueNoMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("foo", "bazz");

		assertThat(new ParamsRequestCondition("foo=bar").getMatchingCondition(request)).isNull();
	}

	@Test
	void compareTo() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		ParamsRequestCondition condition1 = new ParamsRequestCondition("foo", "bar", "baz");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("foo=a", "bar");

		int result = condition1.compareTo(condition2, request);
		assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);

		result = condition2.compareTo(condition1, request);
		assertThat(result).as("Invalid comparison result: " + result).isGreaterThan(0);
	}

	@Test // SPR-16674
	void compareToWithMoreSpecificMatchByValue() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		ParamsRequestCondition condition1 = new ParamsRequestCondition("response_type=code");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("response_type");

		int result = condition1.compareTo(condition2, request);
		assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);
	}

	@Test
	void compareToWithNegatedMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		ParamsRequestCondition condition1 = new ParamsRequestCondition("response_type!=code");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("response_type");

		assertThat(condition1.compareTo(condition2, request)).as("Negated match should not count as more specific").isEqualTo(0);
	}

	@Test
	void combineWithOtherEmpty() {
		ParamsRequestCondition condition1 = new ParamsRequestCondition("foo=bar");
		ParamsRequestCondition condition2 = new ParamsRequestCondition();

		ParamsRequestCondition result = condition1.combine(condition2);
		assertThat(result).isEqualTo(condition1);
	}

	@Test
	void combineWithThisEmpty() {
		ParamsRequestCondition condition1 = new ParamsRequestCondition();
		ParamsRequestCondition condition2 = new ParamsRequestCondition("foo=bar");

		ParamsRequestCondition result = condition1.combine(condition2);
		assertThat(result).isEqualTo(condition2);
	}

	@Test
	void combine() {
		ParamsRequestCondition condition1 = new ParamsRequestCondition("foo=bar");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("foo=baz");

		ParamsRequestCondition result = condition1.combine(condition2);
		Collection<ParamExpression> conditions = result.getContent();
		assertThat(conditions).hasSize(2);
	}

}
