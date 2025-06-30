/*
 * Copyright 2002-present the original author or authors.
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

import java.util.Collections;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;
import static org.springframework.web.bind.annotation.RequestMethod.OPTIONS;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * @author Arjen Poutsma
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
				HttpServletRequest request = new MockHttpServletRequest(method.name(), "");
				assertThat(condition.getMatchingCondition(request)).isNotNull();
			}
		}
		testNoMatch(condition, OPTIONS);
	}

	@Test
	void getMatchingConditionWithCustomMethod() {
		HttpServletRequest request = new MockHttpServletRequest("PROPFIND", "");
		assertThat(new RequestMethodsRequestCondition().getMatchingCondition(request)).isNotNull();
		assertThat(new RequestMethodsRequestCondition(GET, POST).getMatchingCondition(request)).isNull();
	}

	@Test
	void getMatchingConditionWithCorsPreFlight() {
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "");
		request.addHeader("Origin", "https://example.com");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT");

		assertThat(new RequestMethodsRequestCondition().getMatchingCondition(request)).isNotNull();
		assertThat(new RequestMethodsRequestCondition(PUT).getMatchingCondition(request)).isNotNull();
		assertThat(new RequestMethodsRequestCondition(DELETE).getMatchingCondition(request)).isNull();
	}

	@Test // SPR-14410
	public void getMatchingConditionWithHttpOptionsInErrorDispatch() {
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/path");
		request.setDispatcherType(DispatcherType.ERROR);

		RequestMethodsRequestCondition condition = new RequestMethodsRequestCondition();
		RequestMethodsRequestCondition result = condition.getMatchingCondition(request);

		assertThat(result).isNotNull();
		assertThat(result).isSameAs(condition);
	}

	@Test
	void compareTo() {
		RequestMethodsRequestCondition c1 = new RequestMethodsRequestCondition(GET, HEAD);
		RequestMethodsRequestCondition c2 = new RequestMethodsRequestCondition(POST);
		RequestMethodsRequestCondition c3 = new RequestMethodsRequestCondition();

		MockHttpServletRequest request = new MockHttpServletRequest();

		int result = c1.compareTo(c2, request);
		assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);

		result = c2.compareTo(c1, request);
		assertThat(result).as("Invalid comparison result: " + result).isGreaterThan(0);

		result = c2.compareTo(c3, request);
		assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);

		result = c1.compareTo(c1, request);
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
		MockHttpServletRequest request = new MockHttpServletRequest(method.name(), "");
		RequestMethodsRequestCondition actual = condition.getMatchingCondition(request);
		assertThat(actual).isNotNull();
		assertThat(actual.getContent()).isEqualTo(Collections.singleton(method));
	}

	private void testNoMatch(RequestMethodsRequestCondition condition, RequestMethod method) {
		MockHttpServletRequest request = new MockHttpServletRequest(method.name(), "");
		assertThat(condition.getMatchingCondition(request)).isNull();
	}

}
