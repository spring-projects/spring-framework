/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.cors.reactive;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.web.cors.reactive.CorsUtils;

/**
 * Test case for reactive {@link CorsUtils}.
 *
 * @author Sebastien Deleuze
 */
public class CorsUtilsTests {

	@Test
	public void isCorsRequest() {
		MockServerHttpRequest request = new MockServerHttpRequest();
		request.addHeader(HttpHeaders.ORIGIN, "http://domain.com");
		assertTrue(CorsUtils.isCorsRequest(request));
	}

	@Test
	public void isNotCorsRequest() {
		MockServerHttpRequest request = new MockServerHttpRequest();
		assertFalse(CorsUtils.isCorsRequest(request));
	}

	@Test
	public void isPreFlightRequest() {
		MockServerHttpRequest request = new MockServerHttpRequest();
		request.setHttpMethod(HttpMethod.OPTIONS);
		request.addHeader(HttpHeaders.ORIGIN, "http://domain.com");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		assertTrue(CorsUtils.isPreFlightRequest(request));
	}

	@Test
	public void isNotPreFlightRequest() {
		MockServerHttpRequest request = new MockServerHttpRequest();
		assertFalse(CorsUtils.isPreFlightRequest(request));

		request = new MockServerHttpRequest();
		request.setHttpMethod(HttpMethod.OPTIONS);
		request.addHeader(HttpHeaders.ORIGIN, "http://domain.com");
		assertFalse(CorsUtils.isPreFlightRequest(request));

		request = new MockServerHttpRequest();
		request.setHttpMethod(HttpMethod.OPTIONS);
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		assertFalse(CorsUtils.isPreFlightRequest(request));
	}

}
