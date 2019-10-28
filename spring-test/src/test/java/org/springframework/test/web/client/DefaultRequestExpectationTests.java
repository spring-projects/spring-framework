/*
 * Copyright 2002-2019 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.twice;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for {@link DefaultRequestExpectation}.
 * @author Rossen Stoyanchev
 */
public class DefaultRequestExpectationTests {


	@Test
	public void match() throws Exception {
		RequestExpectation expectation = new DefaultRequestExpectation(once(), requestTo("/foo"));
		expectation.match(createRequest(GET, "/foo"));
	}

	@Test
	public void matchWithFailedExpectation() throws Exception {
		RequestExpectation expectation = new DefaultRequestExpectation(once(), requestTo("/foo"));
		expectation.andExpect(method(POST));
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				expectation.match(createRequest(GET, "/foo")))
			.withMessageContaining("Unexpected HttpMethod expected:<POST> but was:<GET>");
	}

	@Test
	public void hasRemainingCount() {
		RequestExpectation expectation = new DefaultRequestExpectation(twice(), requestTo("/foo"));
		expectation.andRespond(withSuccess());

		expectation.incrementAndValidate();
		assertThat(expectation.hasRemainingCount()).isTrue();

		expectation.incrementAndValidate();
		assertThat(expectation.hasRemainingCount()).isFalse();
	}

	@Test
	public void isSatisfied() {
		RequestExpectation expectation = new DefaultRequestExpectation(twice(), requestTo("/foo"));
		expectation.andRespond(withSuccess());

		expectation.incrementAndValidate();
		assertThat(expectation.isSatisfied()).isFalse();

		expectation.incrementAndValidate();
		assertThat(expectation.isSatisfied()).isTrue();
	}


	@SuppressWarnings("deprecation")
	private ClientHttpRequest createRequest(HttpMethod method, String url) {
		try {
			return new org.springframework.mock.http.client.MockAsyncClientHttpRequest(method,  new URI(url));
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
