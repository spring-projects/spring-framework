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

import java.net.SocketException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MockReactiveRestServiceServerTests {
	@Test
	void buildAndVerifyOnceWithoutBody() {
		MockReactiveRestServiceServer server = MockReactiveRestServiceServer.createServer();
		WebClient webClient = server.createWebClientBuilder().build();

		server.expect(requestTo("/foo"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess());

		ResponseEntity<Void> response = assertSuccess(
				webClient.get()
						.uri("/foo")
						.retrieve()
						.toBodilessEntity());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNull();

		server.verify();
	}

	@Test
	void buildAndVerifyOnceWithBody() {
		MockReactiveRestServiceServer server = MockReactiveRestServiceServer.createServer();
		WebClient webClient = server.createWebClientBuilder().build();

		server.expect(requestTo("/bar/baz"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess()
						.contentType(MediaType.APPLICATION_JSON)
						.body("{\"message\": \"hello, world!\"}"));

		ResponseEntity<SimpleResponseMessage> response = assertSuccess(
				webClient.post()
						.uri("/bar/baz")
						.retrieve()
						.toEntity(SimpleResponseMessage.class));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getMessage()).isNotNull().isEqualTo("hello, world!");

		server.verify();
	}

	@Test
	void ignoreRequestOrder() {
		MockReactiveRestServiceServer server = MockReactiveRestServiceServer.createServer(true);
		WebClient webClient = server.createWebClientBuilder().build();

		server.expect(requestTo("/some/coolApiEndpoint")).andRespond(withSuccess());
		server.expect(requestTo("/another/coolEndpoint")).andRespond(withSuccess());
		assertSuccess(webClient.get().uri("/another/coolEndpoint").retrieve().toBodilessEntity());
		assertSuccess(webClient.get().uri("/some/coolApiEndpoint").retrieve().toBodilessEntity());
		server.verify();
	}

	@Test
	void exactRequestOrder() {
		MockReactiveRestServiceServer server = MockReactiveRestServiceServer.createServer(false);
		WebClient webClient = server.createWebClientBuilder().build();

		server.expect(requestTo("/some/coolApiEndpoint")).andRespond(withSuccess());
		server.expect(requestTo("/another/coolEndpoint")).andRespond(withSuccess());

		assertAssertionError(webClient.get().uri("/another/coolEndpoint").retrieve().toBodilessEntity());
	}

	@Test
	void resetAndReuseIgnoringRequestOrder() {
		MockReactiveRestServiceServer server = MockReactiveRestServiceServer.createServer(true);
		WebClient webClient = server.createWebClientBuilder().build();

		server.expect(requestTo("/some/coolApiEndpoint")).andRespond(withSuccess());
		assertSuccess(webClient.get().uri("/some/coolApiEndpoint").retrieve().toBodilessEntity());
		server.verify();
		server.reset();

		server.expect(requestTo("/some/coolApiEndpoint")).andRespond(withSuccess());
		server.expect(requestTo("/another/coolApiEndpoint")).andRespond(withSuccess());
		assertSuccess(webClient.get().uri("/another/coolApiEndpoint").retrieve().toBodilessEntity());
		assertSuccess(webClient.get().uri("/some/coolApiEndpoint").retrieve().toBodilessEntity());
		server.verify();
	}

	@Test
	void resetClearsRequestFailures() {
		MockReactiveRestServiceServer server = MockReactiveRestServiceServer.createServer();
		WebClient webClient = server.createWebClientBuilder().build();

		server.expect(requestTo("/some/coolApiEndpoint"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess());
		assertAssertionError(webClient.post().uri("/some/coolApiEndpoint").retrieve().toBodilessEntity());
		server.reset();

		server.expect(requestTo("/yetAnotherEndpoint"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess());
		assertSuccess(webClient.post().uri("/yetAnotherEndpoint").retrieve().toBodilessEntity());
	}

	@Test
	void followUpRequestAfterFailure() {
		MockReactiveRestServiceServer server = MockReactiveRestServiceServer.createServer();
		WebClient webClient = server.createWebClientBuilder().build();

		server.expect(requestTo("/some/coolApiEndpoint"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(res -> {
					throw new SocketException("network error");
				});

		server.expect(requestTo("/some/coolApiEndpoint"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess());

		StepVerifier
				.create((webClient.get().uri("/some/coolApiEndpoint").retrieve().toBodilessEntity()))
				.expectErrorSatisfies(ex -> assertThat(ex)
						.isInstanceOf(WebClientRequestException.class)
						.getCause()
						.isInstanceOf(SocketException.class))
				.verify();

		assertSuccess(webClient.get().uri("/some/coolApiEndpoint").retrieve().toBodilessEntity());
	}

	@Test
	void verifyShouldFailIfRequestsFailed() {
		MockReactiveRestServiceServer server = MockReactiveRestServiceServer.createServer(true);
		WebClient webClient = server.createWebClientBuilder().build();

		server.expect(requestTo("/some/coolApiEndpoint"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess());

		assertAssertionError(webClient.post().uri("/some/coolApiEndpoint").retrieve().toBodilessEntity());
		assertSuccess(webClient.get().uri("/some/coolApiEndpoint").retrieve().toBodilessEntity());

		assertThatExceptionOfType(AssertionError.class).isThrownBy(server::verify);
	}

	@Test
	void verifyWithTimeoutShouldFailOnTimeout() {
		MockReactiveRestServiceServer server = MockReactiveRestServiceServer.createServer();
		WebClient webClient = server.createWebClientBuilder().build();

		server.expect(requestTo("/some/coolApiEndpoint"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess());

		long start = System.nanoTime();
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> server.verify(Duration.ofSeconds(1)));
		long end = System.nanoTime();

		// Expect that it waits for at least 1,000,000ns (1 second).
		assertThat(end - start).isGreaterThan(1_000_000L);

		server.reset();

		// This one should pass immediately.
		server.expect(requestTo("/some/coolApiEndpoint"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess());
		assertSuccess(webClient.get().uri("/some/coolApiEndpoint").retrieve().toBodilessEntity());

		server.verify(Duration.ofSeconds(1));
	}

	@Test
	void allRequestHeadersShouldBeSentToTheMockServer() {
		MockReactiveRestServiceServer server = MockReactiveRestServiceServer.createServer();
		WebClient webClient = server.createWebClientBuilder().build();

		server.expect(requestTo("/some/coolApiEndpoint"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("X-Something-Or-Other", "12345678910", "12345"))
				.andExpect(header("X-Another-Thing", "ayaya!"))
				.andExpect(header("X-Duplicated", "first", "second"))
				.andRespond(withSuccess());

		assertSuccess(webClient.get()
				.uri("/some/coolApiEndpoint")
				.header("X-Something-Or-Other", "12345678910", "12345")
				.header("X-Another-Thing", "ayaya!")
				.header("X-Duplicated", "first")
				.header("X-Duplicated", "second")
				.retrieve()
				.toBodilessEntity());

		server.verify();
	}

	@Test
	void allRequestCookiesShouldBeSentToTheMockServer() {
		MockReactiveRestServiceServer server = MockReactiveRestServiceServer.createServer();
		WebClient webClient = server.createWebClientBuilder().build();

		server.expect(requestTo("/some/coolApiEndpoint"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header(HttpHeaders.COOKIE, "session-cookie=1a2b3c4d", "fortune-cookie=you will meet a passing unit test"))
				.andRespond(withSuccess());

		assertSuccess(webClient.get()
				.uri("/some/coolApiEndpoint")
				.cookie("session-cookie", "1a2b3c4d")
				.cookie("fortune-cookie", "you will meet a passing unit test")
				.retrieve()
				.toBodilessEntity());

		server.verify();
	}

	@Test
	void requestBodyShouldBeSentToTheServer() {
		MockReactiveRestServiceServer server = MockReactiveRestServiceServer.createServer();
		WebClient webClient = server.createWebClientBuilder().build();

		server.expect(requestTo("/some/coolApiEndpoint"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(jsonPath("$.greeting").value("Hello!"))
				.andExpect(jsonPath("$.name").value("Spring WebFlux"))
				.andRespond(withSuccess());

		assertSuccess(webClient.post()
				.uri("/some/coolApiEndpoint")
				.bodyValue("{\"name\": \"Spring WebFlux\", \"greeting\": \"Hello!\"}")
				.retrieve()
				.toBodilessEntity());

		server.verify();
	}

	@Test
	void responseStatusShouldBeReturned() {
		MockReactiveRestServiceServer server = MockReactiveRestServiceServer.createServer();
		WebClient webClient = server.createWebClientBuilder().build();

		server.expect(requestTo("/foo/bar/baz"))
				.andRespond(withStatus(HttpStatus.ACCEPTED));

		ResponseEntity<?> result = assertSuccess(webClient.get().uri("/foo/bar/baz").retrieve().toBodilessEntity());
		server.verify();

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
	}

	@Test
	void responseHeadersShouldBeReturned() {
		MockReactiveRestServiceServer server = MockReactiveRestServiceServer.createServer();
		WebClient webClient = server.createWebClientBuilder().build();

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("X-Foo", "bar");
		responseHeaders.add("X-Baz", "bork");
		responseHeaders.add("X-Duplicated", "first");
		responseHeaders.add("X-Duplicated", "second");

		server.expect(requestTo("/foo/bar/baz"))
				.andRespond(withSuccess().headers(responseHeaders));

		ResponseEntity<?> result = assertSuccess(webClient.get().uri("/foo/bar/baz").retrieve().toBodilessEntity());
		server.verify();

		assertThat(result.getHeaders().get("X-Foo")).isNotNull().isEqualTo(Collections.singletonList("bar"));
		assertThat(result.getHeaders().get("X-Baz")).isNotNull().isEqualTo(Collections.singletonList("bork"));
		assertThat(result.getHeaders().get("X-Duplicated")).isNotNull().isEqualTo(Arrays.asList("first", "second"));
	}

	@SuppressWarnings("AssertBetweenInconvertibleTypes")  // false positive
	@Test
	void responseCookiesShouldBeReturned() {
		MockReactiveRestServiceServer server = MockReactiveRestServiceServer.createServer();
		WebClient webClient = server.createWebClientBuilder().build();

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add(HttpHeaders.SET_COOKIE, "foo=bar");
		responseHeaders.add(HttpHeaders.SET_COOKIE, "baz=bork");
		responseHeaders.add(HttpHeaders.SET_COOKIE, "qux=lorem-ipsum");
		responseHeaders.add(HttpHeaders.SET_COOKIE, "qux=dolor-sit-amet");

		server.expect(requestTo("/foo/bar/baz"))
				.andRespond(withSuccess().headers(responseHeaders));

		MultiValueMap<String, ResponseCookie> cookies = assertSuccess(webClient.get().uri("/foo/bar/baz")
				.exchangeToMono(res -> Mono.just(res.cookies())));
		server.verify();

		assertThat(cookies.get("foo").stream().map(ResponseCookie::getValue))
				.isEqualTo(Collections.singletonList("bar"));
		assertThat(cookies.get("baz").stream().map(ResponseCookie::getValue))
				.isEqualTo(Collections.singletonList("bork"));
		assertThat(cookies.get("qux").stream().map(ResponseCookie::getValue))
				.isEqualTo(Arrays.asList("lorem-ipsum", "dolor-sit-amet"));
	}

	@Test
	void responseBodyShouldBeReturned() {
		MockReactiveRestServiceServer server = MockReactiveRestServiceServer.createServer();
		WebClient webClient = server.createWebClientBuilder().build();

		server.expect(requestTo("/foo/bar/baz"))
				.andRespond(withSuccess().body("hello, world!"));

		String responseBody = assertSuccess(webClient.get().uri("/foo/bar/baz").retrieve().bodyToMono(String.class));
		server.verify();

		assertThat(responseBody).isEqualTo("hello, world!");
	}

	@ParameterizedTest(name = "expectErrorResponsesToThrowException for HTTP {0}")
	@ValueSource(ints = {400, 401, 500, 501})
	void expectErrorResponsesToThrowException(int code) {
		HttpStatus responseStatus = HttpStatus.valueOf(code);

		MockReactiveRestServiceServer server = MockReactiveRestServiceServer.createServer();
		WebClient webClient = server.createWebClientBuilder().build();

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("X-Foo", "bar");
		responseHeaders.add("X-Baz", "bork");
		responseHeaders.add("X-Duplicated", "first");
		responseHeaders.add("X-Duplicated", "second");

		server.expect(anything())
				.andRespond(withStatus(responseStatus)
						.headers(responseHeaders)
						.body("something messed up"));

		StepVerifier.create(webClient.get().retrieve().toEntity(String.class))
				.expectErrorSatisfies(ex -> {
					assertThat(ex).isInstanceOf(WebClientResponseException.class);
					WebClientResponseException resEx = (WebClientResponseException) ex;

					assertThat(resEx.getStatusCode()).isEqualTo(responseStatus);
					assertThat(resEx.getRawStatusCode()).isEqualTo(code);
					assertThat(resEx.getStatusText()).isEqualTo(responseStatus.getReasonPhrase());
					assertThat(resEx.getHeaders()).isEqualTo(responseHeaders);
					assertThat(resEx.getResponseBodyAsString()).isEqualTo("something messed up");
				})
				.verify();

		server.verify();
	}

	private static void assertAssertionError(Publisher<?> publisher) {
		StepVerifier
				.create(publisher)
				.expectError(AssertionError.class)
				.verify();
	}

	private static <T> T assertSuccess(Publisher<T> publisher) {
		AtomicReference<T> ref = new AtomicReference<>();

		StepVerifier
				.create(publisher)
				.assertNext(ref::set)
				.expectComplete()
				.verify();

		return ref.get();
	}

	private static class SimpleResponseMessage {
		private String message;

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}
}
