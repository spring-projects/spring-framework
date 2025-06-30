/*
 * Copyright 2002-present the original author or authors.
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

import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer.MockRestServiceServerBuilder;
import org.springframework.test.web.client.response.ExecutingResponseCreator;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link MockRestServiceServer}.
 *
 * @author Rossen Stoyanchev
 */
class MockRestServiceServerTests {

	private final RestTemplate restTemplate = new RestTemplate();


	@Test
	void buildMultipleTimes() {
		MockRestServiceServerBuilder builder = MockRestServiceServer.bindTo(this.restTemplate);

		MockRestServiceServer server = builder.build();
		server.expect(requestTo("/foo")).andRespond(withSuccess());
		this.restTemplate.getForObject("/foo", Void.class);
		server.verify();

		server = builder.ignoreExpectOrder(true).build();
		server.expect(requestTo("/foo")).andRespond(withSuccess());
		server.expect(requestTo("/bar")).andRespond(withSuccess());
		this.restTemplate.getForObject("/bar", Void.class);
		this.restTemplate.getForObject("/foo", Void.class);
		server.verify();

		server = builder.build();
		server.expect(requestTo("/bar")).andRespond(withSuccess());
		this.restTemplate.getForObject("/bar", Void.class);
		server.verify();
	}

	@Test
	void exactExpectOrder() {
		MockRestServiceServer server = MockRestServiceServer.bindTo(this.restTemplate)
				.ignoreExpectOrder(false).build();

		server.expect(requestTo("/foo")).andRespond(withSuccess());
		server.expect(requestTo("/bar")).andRespond(withSuccess());
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				this.restTemplate.getForObject("/bar", Void.class));
	}

	@Test
	void ignoreExpectOrder() {
		MockRestServiceServer server = MockRestServiceServer.bindTo(this.restTemplate)
				.ignoreExpectOrder(true).build();

		server.expect(requestTo("/foo")).andRespond(withSuccess());
		server.expect(requestTo("/bar")).andRespond(withSuccess());
		this.restTemplate.getForObject("/bar", Void.class);
		this.restTemplate.getForObject("/foo", Void.class);
		server.verify();
	}

	@Test
	void executingResponseCreator() {
		RestTemplate restTemplate = createEchoRestTemplate();
		ExecutingResponseCreator withActualCall = new ExecutingResponseCreator(restTemplate.getRequestFactory());

		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
		server.expect(requestTo("/profile")).andRespond(withSuccess());
		server.expect(requestTo("/quoteOfTheDay")).andRespond(withActualCall);

		var response1 = restTemplate.getForEntity("/profile", String.class);
		var response2 = restTemplate.getForEntity("/quoteOfTheDay", String.class);
		server.verify();

		assertThat(response1.getStatusCode().value()).isEqualTo(200);
		assertThat(response1.getBody()).isNullOrEmpty();
		assertThat(response2.getStatusCode().value()).isEqualTo(300);
		assertThat(response2.getBody()).isEqualTo("echo from /quoteOfTheDay");
	}

	private static RestTemplate createEchoRestTemplate() {
		ClientHttpRequestFactory requestFactory = (uri, httpMethod) -> {
			MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri);
			ClientHttpResponse response = new MockClientHttpResponse(
					("echo from " + uri.getPath()).getBytes(StandardCharsets.UTF_8),
					HttpStatus.MULTIPLE_CHOICES); // use a different status on purpose
			response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
			request.setResponse(response);
			return request;
		};
		return new RestTemplate(requestFactory);
	}

	@Test
	void resetAndReuseServer() {
		MockRestServiceServer server = MockRestServiceServer.bindTo(this.restTemplate).build();

		server.expect(requestTo("/foo")).andRespond(withSuccess());
		this.restTemplate.getForObject("/foo", Void.class);
		server.verify();
		server.reset();

		server.expect(requestTo("/bar")).andRespond(withSuccess());
		this.restTemplate.getForObject("/bar", Void.class);
		server.verify();
	}

	@Test
	void resetAndReuseServerWithUnorderedExpectationManager() {
		MockRestServiceServer server = MockRestServiceServer.bindTo(this.restTemplate)
				.ignoreExpectOrder(true).build();

		server.expect(requestTo("/foo")).andRespond(withSuccess());
		this.restTemplate.getForObject("/foo", Void.class);
		server.verify();
		server.reset();

		server.expect(requestTo("/foo")).andRespond(withSuccess());
		server.expect(requestTo("/bar")).andRespond(withSuccess());
		this.restTemplate.getForObject("/bar", Void.class);
		this.restTemplate.getForObject("/foo", Void.class);
		server.verify();
	}

	@Test  // gh-24486
	void resetClearsRequestFailures() {
		MockRestServiceServer server = MockRestServiceServer.bindTo(this.restTemplate).build();
		server.expect(once(), requestTo("/remoteurl")).andRespond(withSuccess());
		this.restTemplate.postForEntity("/remoteurl", null, String.class);
		server.verify();

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> this.restTemplate.postForEntity("/remoteurl", null, String.class))
				.withMessageStartingWith("No further requests expected");

		server.reset();

		server.expect(once(), requestTo("/remoteurl")).andRespond(withSuccess());
		this.restTemplate.postForEntity("/remoteurl", null, String.class);
		server.verify();
	}

	@Test  // SPR-16132
	void followUpRequestAfterFailure() {
		MockRestServiceServer server = MockRestServiceServer.bindTo(this.restTemplate).build();

		server.expect(requestTo("/some-service/some-endpoint"))
				.andRespond(request -> { throw new SocketException("pseudo network error"); });

		server.expect(requestTo("/reporting-service/report-error"))
				.andExpect(method(POST)).andRespond(withSuccess());

		try {
			this.restTemplate.getForEntity("/some-service/some-endpoint", String.class);
			fail("Expected exception");
		}
		catch (Exception ex) {
			this.restTemplate.postForEntity("/reporting-service/report-error", ex.toString(), String.class);
		}

		server.verify();
	}

	@Test  // gh-21799
	void verifyShouldFailIfRequestsFailed() {
		MockRestServiceServer server = MockRestServiceServer.bindTo(this.restTemplate).build();
		server.expect(once(), requestTo("/remoteurl")).andRespond(withSuccess());

		this.restTemplate.postForEntity("/remoteurl", null, String.class);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> this.restTemplate.postForEntity("/remoteurl", null, String.class))
				.withMessageStartingWith("No further requests expected");

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(server::verify)
				.withMessageStartingWith("Some requests did not execute successfully");
	}

	@Test
	void verifyWithTimeout() {
		MockRestServiceServerBuilder builder = MockRestServiceServer.bindTo(this.restTemplate);

		MockRestServiceServer server1 = builder.build();
		server1.expect(requestTo("/foo")).andRespond(withSuccess());
		server1.expect(requestTo("/bar")).andRespond(withSuccess());
		this.restTemplate.getForObject("/foo", Void.class);

		assertThatThrownBy(() -> server1.verify(Duration.ofMillis(100))).hasMessage("""
				Further request(s) expected leaving 1 unsatisfied expectation(s).
				1 request(s) executed:
				GET /foo, headers: [Accept:"application/json, application/*+json"]
				""");

		MockRestServiceServer server2 = builder.build();
		server2.expect(requestTo("/foo")).andRespond(withSuccess());
		server2.expect(requestTo("/bar")).andRespond(withSuccess());
		this.restTemplate.getForObject("/foo", Void.class);
		this.restTemplate.getForObject("/bar", Void.class);

		server2.verify(Duration.ofMillis(100));
	}

}
