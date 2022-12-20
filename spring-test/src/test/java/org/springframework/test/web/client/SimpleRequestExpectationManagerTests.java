/*
 * Copyright 2002-2022 the original author or authors.
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

import java.io.IOException;
import java.net.SocketException;
import java.net.URI;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.ExpectedCount.max;
import static org.springframework.test.web.client.ExpectedCount.min;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.ExpectedCount.twice;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for {@link SimpleRequestExpectationManager}.
 *
 * @author Rossen Stoyanchev
 */
class SimpleRequestExpectationManagerTests {

	private final SimpleRequestExpectationManager manager = new SimpleRequestExpectationManager();


	@Test
	void unexpectedRequest() throws Exception {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> this.manager.validateRequest(createRequest(GET, "/foo")))
			.withMessage("""
					No further requests expected: HTTP GET /foo
					0 request(s) executed.
					""");
	}

	@Test
	void zeroExpectedRequests() throws Exception {
		this.manager.verify();
	}

	@Test
	void sequentialRequests() throws Exception {
		this.manager.expectRequest(once(), requestTo("/foo")).andExpect(method(GET)).andRespond(withSuccess());
		this.manager.expectRequest(once(), requestTo("/bar")).andExpect(method(GET)).andRespond(withSuccess());

		this.manager.validateRequest(createRequest(GET, "/foo"));
		this.manager.validateRequest(createRequest(GET, "/bar"));
		this.manager.verify();
	}

	@Test
	void sequentialRequestsTooMany() throws Exception {
		this.manager.expectRequest(max(1), requestTo("/foo")).andExpect(method(GET)).andRespond(withSuccess());
		this.manager.expectRequest(max(1), requestTo("/bar")).andExpect(method(GET)).andRespond(withSuccess());
		this.manager.validateRequest(createRequest(GET, "/foo"));
		this.manager.validateRequest(createRequest(GET, "/bar"));
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> this.manager.validateRequest(createRequest(GET, "/baz")))
			.withMessage("""
					No further requests expected: HTTP GET /baz
					2 request(s) executed:
					GET /foo
					GET /bar
					""");
	}

	@Test
	void sequentialRequestsTooFew() throws Exception {
		this.manager.expectRequest(min(1), requestTo("/foo")).andExpect(method(GET)).andRespond(withSuccess());
		this.manager.expectRequest(min(1), requestTo("/bar")).andExpect(method(GET)).andRespond(withSuccess());
		this.manager.validateRequest(createRequest(GET, "/foo"));
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> this.manager.verify())
			.withMessage("""
					Further request(s) expected leaving 1 unsatisfied expectation(s).
					1 request(s) executed:
					GET /foo
					""");
	}

	@Test
	void repeatedRequests() throws Exception {
		this.manager.expectRequest(times(3), requestTo("/foo")).andExpect(method(GET)).andRespond(withSuccess());
		this.manager.expectRequest(times(3), requestTo("/bar")).andExpect(method(GET)).andRespond(withSuccess());

		this.manager.validateRequest(createRequest(GET, "/foo"));
		this.manager.validateRequest(createRequest(GET, "/bar"));
		this.manager.validateRequest(createRequest(GET, "/foo"));
		this.manager.validateRequest(createRequest(GET, "/bar"));
		this.manager.validateRequest(createRequest(GET, "/foo"));
		this.manager.validateRequest(createRequest(GET, "/bar"));
		this.manager.verify();
	}

	@Test
	void repeatedRequestsTooMany() throws Exception {
		this.manager.expectRequest(max(2), requestTo("/foo")).andExpect(method(GET)).andRespond(withSuccess());
		this.manager.expectRequest(max(2), requestTo("/bar")).andExpect(method(GET)).andRespond(withSuccess());
		this.manager.validateRequest(createRequest(GET, "/foo"));
		this.manager.validateRequest(createRequest(GET, "/bar"));
		this.manager.validateRequest(createRequest(GET, "/foo"));
		this.manager.validateRequest(createRequest(GET, "/bar"));
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> this.manager.validateRequest(createRequest(GET, "/foo")))
			.withMessage("""
					No further requests expected: HTTP GET /foo
					4 request(s) executed:
					GET /foo
					GET /bar
					GET /foo
					GET /bar
					""");
	}

	@Test
	void repeatedRequestsTooFew() throws Exception {
		this.manager.expectRequest(min(2), requestTo("/foo")).andExpect(method(GET)).andRespond(withSuccess());
		this.manager.expectRequest(min(2), requestTo("/bar")).andExpect(method(GET)).andRespond(withSuccess());
		this.manager.validateRequest(createRequest(GET, "/foo"));
		this.manager.validateRequest(createRequest(GET, "/bar"));
		this.manager.validateRequest(createRequest(GET, "/foo"));
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> this.manager.verify())
			.withMessageContaining("""
					3 request(s) executed:
					GET /foo
					GET /bar
					GET /foo
					""");
	}

	@Test
	void repeatedRequestsNotInOrder() throws Exception {
		this.manager.expectRequest(twice(), requestTo("/foo")).andExpect(method(GET)).andRespond(withSuccess());
		this.manager.expectRequest(twice(), requestTo("/bar")).andExpect(method(GET)).andRespond(withSuccess());
		this.manager.expectRequest(twice(), requestTo("/baz")).andExpect(method(GET)).andRespond(withSuccess());
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> this.manager.validateRequest(createRequest(POST, "/foo")))
			.withMessage("Unexpected HttpMethod expected:<GET> but was:<POST>");
	}

	@Test  // SPR-15672
	void sequentialRequestsWithDifferentCount() throws Exception {
		this.manager.expectRequest(times(2), requestTo("/foo")).andExpect(method(GET)).andRespond(withSuccess());
		this.manager.expectRequest(once(), requestTo("/bar")).andExpect(method(GET)).andRespond(withSuccess());

		this.manager.validateRequest(createRequest(GET, "/foo"));
		this.manager.validateRequest(createRequest(GET, "/foo"));
		this.manager.validateRequest(createRequest(GET, "/bar"));
	}

	@Test  // SPR-15719
	void repeatedRequestsInSequentialOrder() throws Exception {
		this.manager.expectRequest(times(2), requestTo("/foo")).andExpect(method(GET)).andRespond(withSuccess());
		this.manager.expectRequest(times(2), requestTo("/bar")).andExpect(method(GET)).andRespond(withSuccess());

		this.manager.validateRequest(createRequest(GET, "/foo"));
		this.manager.validateRequest(createRequest(GET, "/foo"));
		this.manager.validateRequest(createRequest(GET, "/bar"));
		this.manager.validateRequest(createRequest(GET, "/bar"));
	}

	@Test  // SPR-16132
	void sequentialRequestsWithFirstFailing() throws Exception {
		this.manager.expectRequest(once(), requestTo("/foo")).
				andExpect(method(GET)).andRespond(request -> { throw new SocketException("pseudo network error"); });
		this.manager.expectRequest(once(), requestTo("/handle-error")).
				andExpect(method(POST)).andRespond(withSuccess());
		assertThatExceptionOfType(SocketException.class).isThrownBy(() ->
				this.manager.validateRequest(createRequest(GET, "/foo")));
		this.manager.validateRequest(createRequest(POST, "/handle-error"));
		this.manager.verify();
	}


	@Test
	void rejectAndPerformRequestWhenNoImplicitResponseCreator() {
		ResponseActions responseActions = this.manager.expectRequest(once(), requestTo("/bar"))
				.andExpect(method(GET));

		//attempt to perform the "original" request, but the factory hasn't been captured
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(responseActions::andPerformRequest)
				.withMessage("andPerformRequest is only supported if an implicit ResponseCreator is provided at construction, which was not the case");
	}

	@Test
	void delegatingToImplicitResponseCreator() throws IOException {
		final MockClientHttpResponse expectedMockResponse = new MockClientHttpResponse(new byte[0], 404);
		ClientHttpRequestFactory originalFactory = (uri, httpMethod) -> {
			MockClientHttpRequest originalMockRequest = new MockClientHttpRequest(httpMethod, uri);
			originalMockRequest.setResponse(expectedMockResponse);
			return originalMockRequest;
		};
		SimpleRequestExpectationManager managerWithOriginalFactory = new SimpleRequestExpectationManager(originalFactory);

		ResponseActions responseActions = managerWithOriginalFactory.expectRequest(once(), requestTo("/bar"))
				.andExpect(method(GET));

		//set up the implicit ResponseCreator which perform the "original" request
		assertThatCode(responseActions::andPerformRequest).doesNotThrowAnyException();

		//verify validation actually triggers the "original response" creator
		ClientHttpResponse response = managerWithOriginalFactory.validateRequest(createRequest(GET, "/bar"));
		assertThat(response).as("created response").isSameAs(expectedMockResponse);
	}


	private ClientHttpRequest createRequest(HttpMethod method, String url) {
		return new MockClientHttpRequest(method, URI.create(url));
	}

}
