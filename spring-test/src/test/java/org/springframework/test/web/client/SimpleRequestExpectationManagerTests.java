/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.web.client;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockAsyncClientHttpRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for {@link AbstractRequestExpectationManager}.
 * @author Rossen Stoyanchev
 */
public class SimpleRequestExpectationManagerTests {

	private SimpleRequestExpectationManager manager = new SimpleRequestExpectationManager();


	@Test
	public void validateWithUnexpectedRequest() throws Exception {
		try {
			this.manager.validateRequest(request(HttpMethod.GET, "/foo"));
		}
		catch (AssertionError error) {
			assertEquals("No further requests expected: HTTP GET /foo\n" +
					"0 out of 0 were executed", error.getMessage());
		}
	}

	@Test
	public void verify() throws Exception {
		this.manager.expectRequest(anything()).andRespond(withSuccess());
		this.manager.expectRequest(anything()).andRespond(withSuccess());

		this.manager.validateRequest(request(HttpMethod.GET, "/foo"));
		this.manager.validateRequest(request(HttpMethod.POST, "/bar"));
		this.manager.verify();
	}

	@Test
	public void verifyWithZeroExpectations() throws Exception {
		this.manager.verify();
	}

	@Test
	public void verifyWithRemainingExpectations() throws Exception {
		this.manager.expectRequest(anything()).andRespond(withSuccess());
		this.manager.expectRequest(anything()).andRespond(withSuccess());

		this.manager.validateRequest(request(HttpMethod.GET, "/foo"));
		try {
			this.manager.verify();
		}
		catch (AssertionError error) {
			assertTrue(error.getMessage(), error.getMessage().contains("1 out of 2 were executed"));
		}
	}

	private ClientHttpRequest request(HttpMethod method, String url) {
		try {
			return new MockAsyncClientHttpRequest(method,  new URI(url));
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
