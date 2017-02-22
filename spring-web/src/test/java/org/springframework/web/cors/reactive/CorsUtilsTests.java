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

import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.get;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.options;

/**
 * Test case for reactive {@link CorsUtils}.
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class CorsUtilsTests {

	@Test
	public void isCorsRequest() {
		MockServerHttpRequest request = get("/").header(HttpHeaders.ORIGIN, "http://domain.com").build();
		assertTrue(CorsUtils.isCorsRequest(request));
	}

	@Test
	public void isNotCorsRequest() {
		MockServerHttpRequest request = get("/").build();
		assertFalse(CorsUtils.isCorsRequest(request));
	}

	@Test
	public void isPreFlightRequest() {
		MockServerHttpRequest request = options("/")
				.header(HttpHeaders.ORIGIN, "http://domain.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.build();
		assertTrue(CorsUtils.isPreFlightRequest(request));
	}

	@Test
	public void isNotPreFlightRequest() {
		MockServerHttpRequest request = get("/").build();
		assertFalse(CorsUtils.isPreFlightRequest(request));

		request = options("/").header(HttpHeaders.ORIGIN, "http://domain.com").build();
		assertFalse(CorsUtils.isPreFlightRequest(request));

		request = options("/").header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET").build();
		assertFalse(CorsUtils.isPreFlightRequest(request));
	}

}
