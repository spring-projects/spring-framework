/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.test.web.reactive.server;

import java.net.URI;

import org.junit.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpResponse;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link StatusAssertions}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class StatusAssertionTests {

	@Test
	public void isEqualTo() throws Exception {

		StatusAssertions assertions = statusAssertions(HttpStatus.CONFLICT);

		// Success
		assertions.isEqualTo(HttpStatus.CONFLICT);
		assertions.isEqualTo(409);

		try {
			assertions.isEqualTo(HttpStatus.REQUEST_TIMEOUT);
			fail("Wrong status expected");
		}
		catch (AssertionError error) {
			// Expected
		}

		try {
			assertions.isEqualTo(408);
			fail("Wrong status value expected");
		}
		catch (AssertionError error) {
			// Expected
		}
	}

	@Test
	public void reasonEquals() throws Exception {

		StatusAssertions assertions = statusAssertions(HttpStatus.CONFLICT);

		// Success
		assertions.reasonEquals("Conflict");

		try {
			assertions.reasonEquals("Request Timeout");
			fail("Wrong reason expected");
		}
		catch (AssertionError error) {
			// Expected
		}
	}

	@Test
	public void statusSerius1xx() throws Exception {
		StatusAssertions assertions = statusAssertions(HttpStatus.CONTINUE);

		// Success
		assertions.is1xxInformational();

		try {
			assertions.is2xxSuccessful();
			fail("Wrong series expected");
		}
		catch (AssertionError error) {
			// Expected
		}
	}

	@Test
	public void statusSerius2xx() throws Exception {
		StatusAssertions assertions = statusAssertions(HttpStatus.OK);

		// Success
		assertions.is2xxSuccessful();

		try {
			assertions.is5xxServerError();
			fail("Wrong series expected");
		}
		catch (AssertionError error) {
			// Expected
		}
	}

	@Test
	public void statusSerius3xx() throws Exception {
		StatusAssertions assertions = statusAssertions(HttpStatus.PERMANENT_REDIRECT);

		// Success
		assertions.is3xxRedirection();

		try {
			assertions.is2xxSuccessful();
			fail("Wrong series expected");
		}
		catch (AssertionError error) {
			// Expected
		}
	}

	@Test
	public void statusSerius4xx() throws Exception {
		StatusAssertions assertions = statusAssertions(HttpStatus.BAD_REQUEST);

		// Success
		assertions.is4xxClientError();

		try {
			assertions.is2xxSuccessful();
			fail("Wrong series expected");
		}
		catch (AssertionError error) {
			// Expected
		}
	}

	@Test
	public void statusSerius5xx() throws Exception {
		StatusAssertions assertions = statusAssertions(HttpStatus.INTERNAL_SERVER_ERROR);

		// Success
		assertions.is5xxServerError();

		try {
			assertions.is2xxSuccessful();
			fail("Wrong series expected");
		}
		catch (AssertionError error) {
			// Expected
		}
	}


	private StatusAssertions statusAssertions(HttpStatus status) {

		MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("/"));
		MockClientHttpResponse response = new MockClientHttpResponse(status);

		WiretapClientHttpRequest wiretapRequest = new WiretapClientHttpRequest(request);
		WiretapClientHttpResponse wiretapResponse = new WiretapClientHttpResponse(response);

		ExchangeResult exchangeResult = new ExchangeResult(wiretapRequest, wiretapResponse);
		return new StatusAssertions(exchangeResult, mock(WebTestClient.ResponseSpec.class));
	}

}
