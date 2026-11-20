/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.assertj;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Tests for {@link AbstractMockHttpServletRequestAssert}.
 *
 * @author Stephane Nicoll
 */
class AbstractMockHttpServletRequestAssertTests {

	@Test
	void requestCanBeAsserted() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		assertThat(request).satisfies(actual -> assertThat(actual).isSameAs(request));
	}


	private static RequestAssert assertThat(MockHttpServletRequest request) {
		return new RequestAssert(request);
	}

	private static final class RequestAssert extends AbstractMockHttpServletRequestAssert<RequestAssert> {

		RequestAssert(MockHttpServletRequest actual) {
			super(actual, RequestAssert.class);
		}
	}

}
