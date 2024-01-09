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

package org.springframework.web.cors;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test case for {@link CorsUtils}.
 *
 * @author Sebastien Deleuze
 */
class CorsUtilsTests {

	@Test
	void isCorsRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.ORIGIN, "https://domain.com");
		assertThat(CorsUtils.isCorsRequest(request)).isTrue();
	}

	@Test
	void isNotCorsRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		assertThat(CorsUtils.isCorsRequest(request)).isFalse();
	}

	@Test
	void isPreFlightRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod(HttpMethod.OPTIONS.name());
		request.addHeader(HttpHeaders.ORIGIN, "https://domain.com");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		assertThat(CorsUtils.isPreFlightRequest(request)).isTrue();
	}

	@Test
	void isNotPreFlightRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		assertThat(CorsUtils.isPreFlightRequest(request)).isFalse();

		request = new MockHttpServletRequest();
		request.setMethod(HttpMethod.OPTIONS.name());
		request.addHeader(HttpHeaders.ORIGIN, "https://domain.com");
		assertThat(CorsUtils.isPreFlightRequest(request)).isFalse();
	}

}
