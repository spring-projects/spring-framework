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

package org.springframework.test.web.servlet.assertj;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link AbstractHttpServletRequestAssert}.
 *
 * @author Stephane Nicoll
 */
public class AbstractHttpServletRequestAssertTests {

	@Nested
	class AttributesTests {

		@Test
		void attributesAreCopied() {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("one", 1);
			map.put("two", 2);
			assertThat(createRequest(map)).attributes()
					.containsExactly(entry("one", 1), entry("two", 2));
		}

		@Test
		void attributesWithWrongKey() {
			HttpServletRequest request = createRequest(Map.of("one", 1));
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(request).attributes().containsKey("two"))
					.withMessageContainingAll("Request Attributes", "two", "one");
		}

		private HttpServletRequest createRequest(Map<String, Object> attributes) {
			MockHttpServletRequest request = new MockHttpServletRequest();
			attributes.forEach(request::setAttribute);
			return request;
		}

	}

	@Nested
	class SessionAttributesTests {

		@Test
		void sessionAttributesAreCopied() {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("one", 1);
			map.put("two", 2);
			assertThat(createRequest(map)).sessionAttributes()
					.containsExactly(entry("one", 1), entry("two", 2));
		}

		@Test
		void sessionAttributesWithWrongKey() {
			HttpServletRequest request = createRequest(Map.of("one", 1));
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(request).sessionAttributes().containsKey("two"))
					.withMessageContainingAll("Session Attributes", "two", "one");
		}


		private HttpServletRequest createRequest(Map<String, Object> attributes) {
			MockHttpServletRequest request = new MockHttpServletRequest();
			HttpSession session = request.getSession();
			attributes.forEach(session::setAttribute);
			return request;
		}

	}

	@Test
	void hasAsyncStartedTrue() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAsyncStarted(true);
		assertThat(request).hasAsyncStarted(true);
	}

	@Test
	void hasAsyncStartedTrueWithFalse() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAsyncStarted(false);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(request).hasAsyncStarted(true))
				.withMessage("Async expected to have started");
	}

	@Test
	void hasAsyncStartedFalse() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAsyncStarted(false);
		assertThat(request).hasAsyncStarted(false);
	}

	@Test
	void hasAsyncStartedFalseWithTrue() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAsyncStarted(true);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(request).hasAsyncStarted(false))
				.withMessage("Async expected not to have started");
	}


	private static RequestAssert assertThat(HttpServletRequest request) {
		return new RequestAssert(request);
	}


	private static final class RequestAssert extends AbstractHttpServletRequestAssert<RequestAssert, HttpServletRequest> {

		RequestAssert(HttpServletRequest actual) {
			super(actual, RequestAssert.class);
		}
	}

}
