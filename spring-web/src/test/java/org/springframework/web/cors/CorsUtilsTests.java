/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
