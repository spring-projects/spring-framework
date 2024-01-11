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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition.ProduceMediaTypeExpression;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProducesRequestCondition}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
class ProducesRequestConditionTests {

	@Test
	void match() {
		ProducesRequestCondition condition = new ProducesRequestCondition("text/plain");
		HttpServletRequest request = createRequest("text/plain");

		assertThat(condition.getMatchingCondition(request)).isNotNull();
	}

	@Test
	void matchNegated() {
		ProducesRequestCondition condition = new ProducesRequestCondition("!text/plain");
		HttpServletRequest request = createRequest("text/plain");

		assertThat(condition.getMatchingCondition(request)).isNull();
	}

	@Test
	void matchNegatedWithoutAcceptHeader() {
		ProducesRequestCondition condition = new ProducesRequestCondition("!text/plain");

		assertThat(condition.getMatchingCondition(new MockHttpServletRequest())).isNotNull();
		assertThat(condition.getProducibleMediaTypes()).isEqualTo(Collections.emptySet());
	}

	@Test
	void getProducibleMediaTypes() {
		ProducesRequestCondition condition = new ProducesRequestCondition("!application/xml");
		assertThat(condition.getProducibleMediaTypes()).isEqualTo(Collections.emptySet());
	}

	@Test
	void matchWildcard() {
		ProducesRequestCondition condition = new ProducesRequestCondition("text/*");
		HttpServletRequest request = createRequest("text/plain");

		assertThat(condition.getMatchingCondition(request)).isNotNull();
	}

	@Test
	void matchMultiple() {
		ProducesRequestCondition condition = new ProducesRequestCondition("text/plain", "application/xml");
		HttpServletRequest request = createRequest("text/plain");

		assertThat(condition.getMatchingCondition(request)).isNotNull();
	}

	@Test
	void matchSingle() {
		ProducesRequestCondition condition = new ProducesRequestCondition("text/plain");
		HttpServletRequest request = createRequest("application/xml");

		assertThat(condition.getMatchingCondition(request)).isNull();
	}

	@Test // gh-21670
	public void matchWithParameters() {
		String base = "application/atom+xml";
		ProducesRequestCondition condition = new ProducesRequestCondition(base + ";type=feed");
		HttpServletRequest request = createRequest(base + ";type=entry");
		assertThat(condition.getMatchingCondition(request)).isNull();

		condition = new ProducesRequestCondition(base + ";type=feed");
		request = createRequest(base + ";type=feed");
		assertThat(condition.getMatchingCondition(request)).isNotNull();

		condition = new ProducesRequestCondition(base + ";type=feed");
		request = createRequest(base);
		assertThat(condition.getMatchingCondition(request)).isNotNull();

		condition = new ProducesRequestCondition(base);
		request = createRequest(base + ";type=feed");
		assertThat(condition.getMatchingCondition(request)).isNotNull();
	}

	@Test
	void matchParseError() {
		ProducesRequestCondition condition = new ProducesRequestCondition("text/plain");
		HttpServletRequest request = createRequest("bogus");

		assertThat(condition.getMatchingCondition(request)).isNull();
	}

	@Test
	void matchParseErrorWithNegation() {
		ProducesRequestCondition condition = new ProducesRequestCondition("!text/plain");
		HttpServletRequest request = createRequest("bogus");

		assertThat(condition.getMatchingCondition(request)).isNull();
	}

	@Test
	void matchByRequestParameter() {
		String[] produces = {"text/plain"};
		String[] headers = {};
		ProducesRequestCondition condition = new ProducesRequestCondition(produces, headers);
		HttpServletRequest request = new MockHttpServletRequest("GET", "/foo.txt");

		assertThat(condition.getMatchingCondition(request)).isNotNull();
	}

	@Test // SPR-17550
	public void matchWithNegationAndMediaTypeAllWithQualityParameter() {
		ProducesRequestCondition condition = new ProducesRequestCondition("!application/json");
		HttpServletRequest request = createRequest(
				"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");

		assertThat(condition.getMatchingCondition(request)).isNotNull();
	}

	@Test // gh-22853
	public void matchAndCompare() {
		ContentNegotiationManager manager = new ContentNegotiationManager(
				new HeaderContentNegotiationStrategy(),
				new FixedContentNegotiationStrategy(MediaType.TEXT_HTML));

		ProducesRequestCondition none = new ProducesRequestCondition(new String[0], null, manager);
		ProducesRequestCondition html = new ProducesRequestCondition(new String[] {"text/html"}, null, manager);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "*/*");

		ProducesRequestCondition noneMatch = none.getMatchingCondition(request);
		ProducesRequestCondition htmlMatch = html.getMatchingCondition(request);

		assertThat(noneMatch.compareTo(htmlMatch, request)).isEqualTo(1);
	}

	@Test
	void compareTo() {
		ProducesRequestCondition html = new ProducesRequestCondition("text/html");
		ProducesRequestCondition xml = new ProducesRequestCondition("application/xml");
		ProducesRequestCondition none = new ProducesRequestCondition();

		HttpServletRequest request = createRequest("application/xml, text/html");

		assertThat(html.compareTo(xml, request)).isGreaterThan(0);
		assertThat(xml.compareTo(html, request)).isLessThan(0);
		assertThat(xml.compareTo(none, request)).isLessThan(0);
		assertThat(none.compareTo(xml, request)).isGreaterThan(0);
		assertThat(html.compareTo(none, request)).isLessThan(0);
		assertThat(none.compareTo(html, request)).isGreaterThan(0);

		request = createRequest("application/xml, text/*");

		assertThat(html.compareTo(xml, request)).isGreaterThan(0);
		assertThat(xml.compareTo(html, request)).isLessThan(0);

		request = createRequest("application/pdf");

		assertThat(html.compareTo(xml, request)).isEqualTo(0);
		assertThat(xml.compareTo(html, request)).isEqualTo(0);

		// See SPR-7000
		request = createRequest("text/html;q=0.9,application/xml");

		assertThat(html.compareTo(xml, request)).isGreaterThan(0);
		assertThat(xml.compareTo(html, request)).isLessThan(0);
	}

	@Test
	void compareToWithSingleExpression() {
		HttpServletRequest request = createRequest("text/plain");

		ProducesRequestCondition condition1 = new ProducesRequestCondition("text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("text/*");

		int result = condition1.compareTo(condition2, request);
		assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);

		result = condition2.compareTo(condition1, request);
		assertThat(result).as("Invalid comparison result: " + result).isGreaterThan(0);
	}

	@Test
	void compareToMultipleExpressions() {
		ProducesRequestCondition condition1 = new ProducesRequestCondition("*/*", "text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("text/*", "text/plain;q=0.7");

		HttpServletRequest request = createRequest("text/plain");

		int result = condition1.compareTo(condition2, request);
		assertThat(result).as("Invalid comparison result: " + result).isGreaterThan(0);

		result = condition2.compareTo(condition1, request);
		assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);
	}

	@Test
	void compareToMultipleExpressionsAndMultipleAcceptHeaderValues() {
		ProducesRequestCondition condition1 = new ProducesRequestCondition("text/*", "text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("application/*", "application/xml");

		HttpServletRequest request = createRequest("text/plain", "application/xml");

		int result = condition1.compareTo(condition2, request);
		assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);

		result = condition2.compareTo(condition1, request);
		assertThat(result).as("Invalid comparison result: " + result).isGreaterThan(0);

		request = createRequest("application/xml", "text/plain");

		result = condition1.compareTo(condition2, request);
		assertThat(result).as("Invalid comparison result: " + result).isGreaterThan(0);

		result = condition2.compareTo(condition1, request);
		assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);
	}

	// SPR-8536

	@Test
	void compareToMediaTypeAll() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		ProducesRequestCondition condition1 = new ProducesRequestCondition();
		ProducesRequestCondition condition2 = new ProducesRequestCondition("application/json");

		assertThat(condition1.compareTo(condition2, request)).as("Should have picked '*/*' condition as an exact match")
				.isLessThan(0);
		assertThat(condition2.compareTo(condition1, request)).as("Should have picked '*/*' condition as an exact match")
				.isGreaterThan(0);

		condition1 = new ProducesRequestCondition("*/*");
		condition2 = new ProducesRequestCondition("application/json");

		assertThat(condition1.compareTo(condition2, request)).isLessThan(0);
		assertThat(condition2.compareTo(condition1, request)).isGreaterThan(0);

		request.addHeader("Accept", "*/*");

		condition1 = new ProducesRequestCondition();
		condition2 = new ProducesRequestCondition("application/json");

		assertThat(condition1.compareTo(condition2, request)).isLessThan(0);
		assertThat(condition2.compareTo(condition1, request)).isGreaterThan(0);

		condition1 = new ProducesRequestCondition("*/*");
		condition2 = new ProducesRequestCondition("application/json");

		assertThat(condition1.compareTo(condition2, request)).isLessThan(0);
		assertThat(condition2.compareTo(condition1, request)).isGreaterThan(0);
	}

	// SPR-9021

	@Test
	void compareToMediaTypeAllWithParameter() {
		HttpServletRequest request = createRequest("*/*;q=0.9");

		ProducesRequestCondition condition1 = new ProducesRequestCondition();
		ProducesRequestCondition condition2 = new ProducesRequestCondition("application/json");

		assertThat(condition1.compareTo(condition2, request)).isLessThan(0);
		assertThat(condition2.compareTo(condition1, request)).isGreaterThan(0);
	}

	@Test
	void compareToEqualMatch() {
		HttpServletRequest request = createRequest("text/*");

		ProducesRequestCondition condition1 = new ProducesRequestCondition("text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("text/xhtml");

		int result = condition1.compareTo(condition2, request);
		assertThat(result).as("Should have used MediaType.equals(Object) to break the match").isLessThan(0);

		result = condition2.compareTo(condition1, request);
		assertThat(result).as("Should have used MediaType.equals(Object) to break the match").isGreaterThan(0);
	}

	@Test
	void compareEmptyInvalidAccept() {
		HttpServletRequest request = createRequest("foo");

		ProducesRequestCondition condition1 = new ProducesRequestCondition();
		ProducesRequestCondition condition2 = new ProducesRequestCondition();

		int result = condition1.compareTo(condition2, request);
		assertThat(result).isEqualTo(0);
	}

	@Test
	void combine() {
		ProducesRequestCondition condition1 = new ProducesRequestCondition("text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("application/xml");

		ProducesRequestCondition result = condition1.combine(condition2);
		assertThat(result).isEqualTo(condition2);
	}

	@Test
	void combineWithDefault() {
		ProducesRequestCondition condition1 = new ProducesRequestCondition("text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition();

		ProducesRequestCondition result = condition1.combine(condition2);
		assertThat(result).isEqualTo(condition1);
	}

	@Test
	void instantiateWithProducesAndHeaderConditions() {
		String[] produces = new String[] {"text/plain"};
		String[] headers = new String[]{"foo=bar", "accept=application/xml,application/pdf"};
		ProducesRequestCondition condition = new ProducesRequestCondition(produces, headers);

		assertConditions(condition, "text/plain", "application/xml", "application/pdf");
	}

	@Test
	void getMatchingCondition() {
		HttpServletRequest request = createRequest("text/plain");

		ProducesRequestCondition condition = new ProducesRequestCondition("text/plain", "application/xml");

		ProducesRequestCondition result = condition.getMatchingCondition(request);
		assertConditions(result, "text/plain");

		condition = new ProducesRequestCondition("application/xml");

		result = condition.getMatchingCondition(request);
		assertThat(result).isNull();
	}


	private MockHttpServletRequest createRequest(String... headerValue) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		Arrays.stream(headerValue).forEach(value -> request.addHeader("Accept", headerValue));
		return request;
	}

	private void assertConditions(ProducesRequestCondition condition, String... expected) {
		Collection<ProduceMediaTypeExpression> expressions = condition.getContent();
		assertThat(expressions.stream().map(expr -> expr.getMediaType().toString()))
			.containsExactlyInAnyOrder(expected);
	}

}
