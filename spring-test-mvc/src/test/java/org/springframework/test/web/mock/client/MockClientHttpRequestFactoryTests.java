/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.test.web.mock.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.mock.client.match.RequestMatchers.anything;

import java.net.URI;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.mock.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/**
 * Tests for {@link MockClientHttpRequestFactory}.
 *
 * @author Rossen Stoyanchev
 */
public class MockClientHttpRequestFactoryTests {

	private MockRestServiceServer server;

	private ClientHttpRequestFactory factory;


	@Before
	public void setup() {
		RestTemplate restTemplate = new RestTemplate();
		this.server = MockRestServiceServer.createServer(restTemplate);
		this.factory = restTemplate.getRequestFactory();
	}

	@Test
	public void createRequest() throws Exception {
		URI uri = new URI("/foo");
		ClientHttpRequest expected = (ClientHttpRequest) this.server.expect(anything());
		ClientHttpRequest actual = this.factory.createRequest(uri, HttpMethod.GET);

		assertSame(expected, actual);
		assertEquals(uri, actual.getURI());
		assertEquals(HttpMethod.GET, actual.getMethod());
	}

	@Test
	public void noFurtherRequestsExpected() throws Exception {
		try {
			this.factory.createRequest(new URI("/foo"), HttpMethod.GET);
		}
		catch (AssertionError error) {
			assertEquals("No further requests expected", error.getMessage());
		}
	}

	@Test
	public void verifyZeroExpected() throws Exception {
		this.server.verify();
	}

	@Test
	public void verifyExpectedEqualExecuted() throws Exception {
		this.server.expect(anything());
		this.server.expect(anything());

		this.factory.createRequest(new URI("/foo"), HttpMethod.GET);
		this.factory.createRequest(new URI("/bar"), HttpMethod.POST);
	}

	@Test
	public void verifyMoreExpected() throws Exception {
		this.server.expect(anything());
		this.server.expect(anything());

		this.factory.createRequest(new URI("/foo"), HttpMethod.GET);

		try {
			this.server.verify();
		}
		catch (AssertionError error) {
			assertTrue(error.getMessage(), error.getMessage().contains("1 out of 2 were executed"));
		}
	}

}
