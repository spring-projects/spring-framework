/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
class AbstractHttpServletRequestAssertTests {

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
