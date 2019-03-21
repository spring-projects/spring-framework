/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.test.web.client;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockAsyncClientHttpRequest;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for {@link DefaultRequestExpectation}.
 * @author Rossen Stoyanchev
 */
public class DefaultRequestExpectationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();


	@Test
	public void match() throws Exception {
		RequestExpectation expectation = new DefaultRequestExpectation(once(), requestTo("/foo"));
		expectation.match(createRequest(GET, "/foo"));
	}

	@Test
	public void matchWithFailedExpection() throws Exception {
		RequestExpectation expectation = new DefaultRequestExpectation(once(), requestTo("/foo"));
		expectation.andExpect(method(POST));

		this.thrown.expectMessage("Unexpected HttpMethod expected:<POST> but was:<GET>");
		expectation.match(createRequest(GET, "/foo"));
	}

	@Test
	public void hasRemainingCount() throws Exception {
		RequestExpectation expectation = new DefaultRequestExpectation(times(2), requestTo("/foo"));
		expectation.andRespond(withSuccess());

		expectation.createResponse(createRequest(GET, "/foo"));
		assertTrue(expectation.hasRemainingCount());

		expectation.createResponse(createRequest(GET, "/foo"));
		assertFalse(expectation.hasRemainingCount());
	}

	@Test
	public void isSatisfied() throws Exception {
		RequestExpectation expectation = new DefaultRequestExpectation(times(2), requestTo("/foo"));
		expectation.andRespond(withSuccess());

		expectation.createResponse(createRequest(GET, "/foo"));
		assertFalse(expectation.isSatisfied());

		expectation.createResponse(createRequest(GET, "/foo"));
		assertTrue(expectation.isSatisfied());
	}



	private ClientHttpRequest createRequest(HttpMethod method, String url) {
		try {
			return new MockAsyncClientHttpRequest(method,  new URI(url));
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
