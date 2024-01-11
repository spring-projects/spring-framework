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
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition.ConsumeMediaTypeExpression;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class ConsumesRequestConditionTests {

	@Test
	void consumesMatch() {
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("text/plain");

		assertThat(condition.getMatchingCondition(request)).isNotNull();
	}

	@Test
	void negatedConsumesMatch() {
		ConsumesRequestCondition condition = new ConsumesRequestCondition("!text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("text/plain");

		assertThat(condition.getMatchingCondition(request)).isNull();
	}

	@Test
	void getConsumableMediaTypesNegatedExpression() {
		ConsumesRequestCondition condition = new ConsumesRequestCondition("!application/xml");
		assertThat(condition.getConsumableMediaTypes()).isEqualTo(Collections.emptySet());
	}

	@Test
	void consumesWildcardMatch() {
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/*");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("text/plain");

		assertThat(condition.getMatchingCondition(request)).isNotNull();
	}

	@Test
	void consumesMultipleMatch() {
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain", "application/xml");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("text/plain");

		assertThat(condition.getMatchingCondition(request)).isNotNull();
	}

	@Test
	void consumesSingleNoMatch() {
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("application/xml");

		assertThat(condition.getMatchingCondition(request)).isNull();
	}

	@Test // gh-28024
	public void matchWithParameters() {
		String base = "application/hal+json";
		ConsumesRequestCondition condition = new ConsumesRequestCondition(base + ";profile=\"a\"");
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType(base + ";profile=\"a\"");
		assertThat(condition.getMatchingCondition(request)).isNotNull();

		condition = new ConsumesRequestCondition(base + ";profile=\"a\"");
		request.setContentType(base + ";profile=\"b\"");
		assertThat(condition.getMatchingCondition(request)).isNull();

		condition = new ConsumesRequestCondition(base + ";profile=\"a\"");
		request.setContentType(base);
		assertThat(condition.getMatchingCondition(request)).isNotNull();

		condition = new ConsumesRequestCondition(base);
		request.setContentType(base + ";profile=\"a\"");
		assertThat(condition.getMatchingCondition(request)).isNotNull();

		condition = new ConsumesRequestCondition(base + ";profile=\"a\"");
		request.setContentType(base + ";profile=\"A\"");
		assertThat(condition.getMatchingCondition(request)).isNotNull();
	}

	@Test
	void consumesParseError() {
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("01");

		assertThat(condition.getMatchingCondition(request)).isNull();
	}

	@Test
	void consumesParseErrorWithNegation() {
		ConsumesRequestCondition condition = new ConsumesRequestCondition("!text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("01");

		assertThat(condition.getMatchingCondition(request)).isNull();
	}

	@Test // gh-22010
	public void consumesNoContent() {
		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain");
		condition.setBodyRequired(false);

		MockHttpServletRequest request = new MockHttpServletRequest();
		assertThat(condition.getMatchingCondition(request)).isNotNull();

		request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.CONTENT_LENGTH, "0");
		assertThat(condition.getMatchingCondition(request)).isNotNull();

		request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.CONTENT_LENGTH, "21");
		assertThat(condition.getMatchingCondition(request)).isNull();

		request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.TRANSFER_ENCODING, "chunked");
		assertThat(condition.getMatchingCondition(request)).isNull();
	}

	@Test
	void compareToSingle() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		ConsumesRequestCondition condition1 = new ConsumesRequestCondition("text/plain");
		ConsumesRequestCondition condition2 = new ConsumesRequestCondition("text/*");

		int result = condition1.compareTo(condition2, request);
		assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);

		result = condition2.compareTo(condition1, request);
		assertThat(result).as("Invalid comparison result: " + result).isGreaterThan(0);
	}

	@Test
	void compareToMultiple() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		ConsumesRequestCondition condition1 = new ConsumesRequestCondition("*/*", "text/plain");
		ConsumesRequestCondition condition2 = new ConsumesRequestCondition("text/*", "text/plain;q=0.7");

		int result = condition1.compareTo(condition2, request);
		assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);

		result = condition2.compareTo(condition1, request);
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
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("text/plain");

		ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain", "application/xml");

		ConsumesRequestCondition result = condition.getMatchingCondition(request);
		assertConditions(result, "text/plain");

		condition = new ConsumesRequestCondition("application/xml");

		result = condition.getMatchingCondition(request);
		assertThat(result).isNull();
	}

	private void assertConditions(ConsumesRequestCondition condition, String... expected) {
		Collection<ConsumeMediaTypeExpression> expressions = condition.getContent();
		assertThat(expressions.stream().map(expr -> expr.getMediaType().toString()))
			.containsExactlyInAnyOrder(expected);
	}

}
