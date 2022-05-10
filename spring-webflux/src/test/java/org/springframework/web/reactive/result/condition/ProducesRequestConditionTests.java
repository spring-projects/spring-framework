/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.reactive.result.condition;

import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.get;

/**
 * Unit tests for {@link ProducesRequestCondition}.
 *
 * @author Rossen Stoyanchev
 */
public class ProducesRequestConditionTests {

	@Test
	public void match() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("Accept", "text/plain"));
		ProducesRequestCondition condition = new ProducesRequestCondition("text/plain");

		assertThat(condition.getMatchingCondition(exchange)).isNotNull();
	}

	@Test
	public void matchNegated() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("Accept", "text/plain"));
		ProducesRequestCondition condition = new ProducesRequestCondition("!text/plain");

		assertThat(condition.getMatchingCondition(exchange)).isNull();
	}

	@Test
	public void getProducibleMediaTypes() {
		ProducesRequestCondition condition = new ProducesRequestCondition("!application/xml");
		assertThat(condition.getProducibleMediaTypes()).isEqualTo(Collections.emptySet());
	}

	@Test
	public void matchWildcard() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("Accept", "text/plain"));
		ProducesRequestCondition condition = new ProducesRequestCondition("text/*");

		assertThat(condition.getMatchingCondition(exchange)).isNotNull();
	}

	@Test
	public void matchMultiple() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("Accept", "text/plain"));
		ProducesRequestCondition condition = new ProducesRequestCondition("text/plain", "application/xml");

		assertThat(condition.getMatchingCondition(exchange)).isNotNull();
	}

	@Test
	public void matchSingle() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("Accept", "application/xml"));
		ProducesRequestCondition condition = new ProducesRequestCondition("text/plain");

		assertThat(condition.getMatchingCondition(exchange)).isNull();
	}

	@Test // gh-21670
	public void matchWithParameters() {
		String base = "application/atom+xml";
		ProducesRequestCondition condition = new ProducesRequestCondition(base + ";type=feed");
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("Accept", base + ";type=feed"));
		assertThat(condition.getMatchingCondition(exchange)).as("Declared parameter value must match if present in request").isNotNull();

		condition = new ProducesRequestCondition(base + ";type=feed");
		exchange = MockServerWebExchange.from(get("/").header("Accept", base + ";type=entry"));
		assertThat(condition.getMatchingCondition(exchange)).as("Declared parameter value must match if present in request").isNull();

		condition = new ProducesRequestCondition(base + ";type=feed");
		exchange = MockServerWebExchange.from(get("/").header("Accept", base));
		assertThat(condition.getMatchingCondition(exchange)).as("Declared parameter has no impact if not present in request").isNotNull();

		condition = new ProducesRequestCondition(base);
		exchange = MockServerWebExchange.from(get("/").header("Accept", base + ";type=feed"));
		assertThat(condition.getMatchingCondition(exchange)).as("No impact from other parameters in request").isNotNull();
	}

	@Test
	public void matchParseError() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("Accept", "bogus"));
		ProducesRequestCondition condition = new ProducesRequestCondition("text/plain");

		assertThat(condition.getMatchingCondition(exchange)).isNull();
	}

	@Test
	public void matchParseErrorWithNegation() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("Accept", "bogus"));
		ProducesRequestCondition condition = new ProducesRequestCondition("!text/plain");

		assertThat(condition.getMatchingCondition(exchange)).isNull();
	}

	@Test // SPR-17550
	public void matchWithNegationAndMediaTypeAllWithQualityParameter() {
		ProducesRequestCondition condition = new ProducesRequestCondition("!application/json");

		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"));

		assertThat(condition.getMatchingCondition(exchange)).isNotNull();
	}

	@Test // gh-22853
	public void matchAndCompare() {
		RequestedContentTypeResolverBuilder builder = new RequestedContentTypeResolverBuilder();
		builder.headerResolver();
		builder.fixedResolver(MediaType.TEXT_HTML);
		RequestedContentTypeResolver resolver = builder.build();

		ProducesRequestCondition none = new ProducesRequestCondition(new String[0], null, resolver);
		ProducesRequestCondition html = new ProducesRequestCondition(new String[] {"text/html"}, null, resolver);

		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("Accept", "*/*"));

		ProducesRequestCondition noneMatch = none.getMatchingCondition(exchange);
		ProducesRequestCondition htmlMatch = html.getMatchingCondition(exchange);

		assertThat(noneMatch.compareTo(htmlMatch, exchange)).isEqualTo(1);
	}

	@Test
	public void compareTo() {
		ProducesRequestCondition html = new ProducesRequestCondition("text/html");
		ProducesRequestCondition xml = new ProducesRequestCondition("application/xml");
		ProducesRequestCondition none = new ProducesRequestCondition();

		MockServerWebExchange exchange = MockServerWebExchange.from(get("/")
				.header("Accept", "application/xml, text/html"));

		assertThat(html.compareTo(xml, exchange) > 0).isTrue();
		assertThat(xml.compareTo(html, exchange) < 0).isTrue();
		assertThat(xml.compareTo(none, exchange) < 0).isTrue();
		assertThat(none.compareTo(xml, exchange) > 0).isTrue();
		assertThat(html.compareTo(none, exchange) < 0).isTrue();
		assertThat(none.compareTo(html, exchange) > 0).isTrue();

		exchange = MockServerWebExchange.from(
				get("/").header("Accept", "application/xml, text/*"));

		assertThat(html.compareTo(xml, exchange) > 0).isTrue();
		assertThat(xml.compareTo(html, exchange) < 0).isTrue();

		exchange = MockServerWebExchange.from(
				get("/").header("Accept", "application/pdf"));

		assertThat(html.compareTo(xml, exchange)).isEqualTo(0);
		assertThat(xml.compareTo(html, exchange)).isEqualTo(0);

		// See SPR-7000
		exchange = MockServerWebExchange.from(
				get("/").header("Accept", "text/html;q=0.9,application/xml"));

		assertThat(html.compareTo(xml, exchange) > 0).isTrue();
		assertThat(xml.compareTo(html, exchange) < 0).isTrue();
	}

	@Test
	public void compareToWithSingleExpression() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("Accept", "text/plain"));

		ProducesRequestCondition condition1 = new ProducesRequestCondition("text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("text/*");

		int result = condition1.compareTo(condition2, exchange);
		assertThat(result < 0).as("Invalid comparison result: " + result).isTrue();

		result = condition2.compareTo(condition1, exchange);
		assertThat(result > 0).as("Invalid comparison result: " + result).isTrue();
	}

	@Test
	public void compareToMultipleExpressions() {
		ProducesRequestCondition condition1 = new ProducesRequestCondition("*/*", "text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("text/*", "text/plain;q=0.7");

		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("Accept", "text/plain"));

		int result = condition1.compareTo(condition2, exchange);
		assertThat(result).as("Invalid comparison result: " + result).isGreaterThan(0);

		result = condition2.compareTo(condition1, exchange);
		assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);
	}

	@Test
	public void compareToMultipleExpressionsAndMultipleAcceptHeaderValues() {
		ProducesRequestCondition condition1 = new ProducesRequestCondition("text/*", "text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("application/*", "application/xml");

		ServerWebExchange exchange = MockServerWebExchange.from(
				get("/").header("Accept", "text/plain", "application/xml"));

		int result = condition1.compareTo(condition2, exchange);
		assertThat(result < 0).as("Invalid comparison result: " + result).isTrue();

		result = condition2.compareTo(condition1, exchange);
		assertThat(result > 0).as("Invalid comparison result: " + result).isTrue();

		exchange = MockServerWebExchange.from(
				get("/").header("Accept", "application/xml", "text/plain"));

		result = condition1.compareTo(condition2, exchange);
		assertThat(result > 0).as("Invalid comparison result: " + result).isTrue();

		result = condition2.compareTo(condition1, exchange);
		assertThat(result < 0).as("Invalid comparison result: " + result).isTrue();
	}

	// SPR-8536

	@Test
	public void compareToMediaTypeAll() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/"));

		ProducesRequestCondition condition1 = new ProducesRequestCondition();
		ProducesRequestCondition condition2 = new ProducesRequestCondition("application/json");

		assertThat(condition1.compareTo(condition2, exchange) < 0).as("Should have picked '*/*' condition as an exact match").isTrue();
		assertThat(condition2.compareTo(condition1, exchange) > 0).as("Should have picked '*/*' condition as an exact match").isTrue();

		condition1 = new ProducesRequestCondition("*/*");
		condition2 = new ProducesRequestCondition("application/json");

		assertThat(condition1.compareTo(condition2, exchange) < 0).isTrue();
		assertThat(condition2.compareTo(condition1, exchange) > 0).isTrue();

		exchange = MockServerWebExchange.from(
				get("/").header("Accept", "*/*"));

		condition1 = new ProducesRequestCondition();
		condition2 = new ProducesRequestCondition("application/json");

		assertThat(condition1.compareTo(condition2, exchange) < 0).isTrue();
		assertThat(condition2.compareTo(condition1, exchange) > 0).isTrue();

		condition1 = new ProducesRequestCondition("*/*");
		condition2 = new ProducesRequestCondition("application/json");

		assertThat(condition1.compareTo(condition2, exchange) < 0).isTrue();
		assertThat(condition2.compareTo(condition1, exchange) > 0).isTrue();
	}

	// SPR-9021

	@Test
	public void compareToMediaTypeAllWithParameter() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("Accept", "*/*;q=0.9"));

		ProducesRequestCondition condition1 = new ProducesRequestCondition();
		ProducesRequestCondition condition2 = new ProducesRequestCondition("application/json");

		assertThat(condition1.compareTo(condition2, exchange) < 0).isTrue();
		assertThat(condition2.compareTo(condition1, exchange) > 0).isTrue();
	}

	@Test
	public void compareToEqualMatch() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("Accept", "text/*"));

		ProducesRequestCondition condition1 = new ProducesRequestCondition("text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("text/xhtml");

		int result = condition1.compareTo(condition2, exchange);
		assertThat(result < 0).as("Should have used MediaType.equals(Object) to break the match").isTrue();

		result = condition2.compareTo(condition1, exchange);
		assertThat(result > 0).as("Should have used MediaType.equals(Object) to break the match").isTrue();
	}

	@Test
	public void combine() {
		ProducesRequestCondition condition1 = new ProducesRequestCondition("text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition("application/xml");

		ProducesRequestCondition result = condition1.combine(condition2);
		assertThat(result).isEqualTo(condition2);
	}

	@Test
	public void combineWithDefault() {
		ProducesRequestCondition condition1 = new ProducesRequestCondition("text/plain");
		ProducesRequestCondition condition2 = new ProducesRequestCondition();

		ProducesRequestCondition result = condition1.combine(condition2);
		assertThat(result).isEqualTo(condition1);
	}

	@Test
	public void instantiateWithProducesAndHeaderConditions() {
		String[] produces = new String[] {"text/plain"};
		String[] headers = new String[]{"foo=bar", "accept=application/xml,application/pdf"};
		ProducesRequestCondition condition = new ProducesRequestCondition(produces, headers);

		assertConditions(condition, "text/plain", "application/xml", "application/pdf");
	}

	@Test
	public void getMatchingCondition() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("Accept", "text/plain"));

		ProducesRequestCondition condition = new ProducesRequestCondition("text/plain", "application/xml");

		ProducesRequestCondition result = condition.getMatchingCondition(exchange);
		assertConditions(result, "text/plain");

		condition = new ProducesRequestCondition("application/xml");

		result = condition.getMatchingCondition(exchange);
		assertThat(result).isNull();
	}

	private void assertConditions(ProducesRequestCondition condition, String... expected) {
		Collection<ProducesRequestCondition.ProduceMediaTypeExpression> expressions = condition.getContent();
		assertThat(expected.length).as("Invalid number of conditions").isEqualTo(expressions.size());
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

}
