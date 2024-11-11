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

package org.springframework.web.reactive.result.condition;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;
import static org.springframework.web.bind.annotation.RequestMethod.OPTIONS;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * Tests for {@link RequestMethodsRequestCondition}.
 *
 * @author Rossen Stoyanchev
 */
class RequestMethodsRequestConditionTests {

	@Test
	void getMatchingCondition() {
		testMatch(new RequestMethodsRequestCondition(GET), GET);
		testMatch(new RequestMethodsRequestCondition(GET, POST), GET);
		testNoMatch(new RequestMethodsRequestCondition(GET), POST);
	}

	@Test
	void getMatchingConditionWithHttpHead() {
		testMatch(new RequestMethodsRequestCondition(HEAD), HEAD);
		testMatch(new RequestMethodsRequestCondition(GET), GET);
		testNoMatch(new RequestMethodsRequestCondition(POST), HEAD);
	}

	@Test
	void getMatchingConditionWithEmptyConditions() {
		RequestMethodsRequestCondition condition = new RequestMethodsRequestCondition();
		for (RequestMethod method : RequestMethod.values()) {
			if (method != OPTIONS) {
				ServerWebExchange exchange = getExchange(method.name());
				assertThat(condition.getMatchingCondition(exchange)).isNotNull();
			}
		}
		testNoMatch(condition, OPTIONS);
	}

	@Test
	void getMatchingConditionWithCustomMethod() {
		ServerWebExchange exchange = getExchange("PROPFIND");
		assertThat(new RequestMethodsRequestCondition().getMatchingCondition(exchange)).isNotNull();
		assertThat(new RequestMethodsRequestCondition(GET, POST).getMatchingCondition(exchange)).isNull();
	}

	@Test
	void getMatchingConditionWithCorsPreFlight() {
		MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.valueOf("OPTIONS"), "/")
				.header("Origin", "https://example.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
				.build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);

		assertThat(new RequestMethodsRequestCondition().getMatchingCondition(exchange)).isNotNull();
		assertThat(new RequestMethodsRequestCondition(PUT).getMatchingCondition(exchange)).isNotNull();
		assertThat(new RequestMethodsRequestCondition(DELETE).getMatchingCondition(exchange)).isNull();
	}

	@Test
	void compareTo() {
		RequestMethodsRequestCondition c1 = new RequestMethodsRequestCondition(GET, HEAD);
		RequestMethodsRequestCondition c2 = new RequestMethodsRequestCondition(POST);
		RequestMethodsRequestCondition c3 = new RequestMethodsRequestCondition();

		ServerWebExchange exchange = getExchange("GET");

		int result = c1.compareTo(c2, exchange);
		assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);

		result = c2.compareTo(c1, exchange);
		assertThat(result).as("Invalid comparison result: " + result).isGreaterThan(0);

		result = c2.compareTo(c3, exchange);
		assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);

		result = c1.compareTo(c1, exchange);
		assertThat(result).as("Invalid comparison result ").isEqualTo(0);
	}

	@Test
	void combine() {
		RequestMethodsRequestCondition condition1 = new RequestMethodsRequestCondition(GET);
		RequestMethodsRequestCondition condition2 = new RequestMethodsRequestCondition(POST);

		RequestMethodsRequestCondition result = condition1.combine(condition2);
		assertThat(result.getContent()).hasSize(2);
	}


	private void testMatch(RequestMethodsRequestCondition condition, RequestMethod method) {
		ServerWebExchange exchange = getExchange(method.name());
		RequestMethodsRequestCondition actual = condition.getMatchingCondition(exchange);
		assertThat(actual).isNotNull();
		assertThat(actual.getContent()).isEqualTo(Collections.singleton(method));
	}

	private void testNoMatch(RequestMethodsRequestCondition condition, RequestMethod method) {
		ServerWebExchange exchange = getExchange(method.name());
		assertThat(condition.getMatchingCondition(exchange)).isNull();
	}

	private ServerWebExchange getExchange(String method) {
		return MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.valueOf(method), "/"));
	}

}
