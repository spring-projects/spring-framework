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

package org.springframework.web.reactive.function.client;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.Pojo;

import static org.junit.Assert.*;

/**
 * Integration tests using a {@link ExchangeFunction} through {@link WebClient}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 */
public class WebClientIntegrationTests {

	private MockWebServer server;

	private WebClient webClient;

	@Before
	public void setup() {
		this.server = new MockWebServer();
		this.webClient = WebClient.create(this.server.url("/").toString());
	}

	@After
	public void shutdown() throws Exception {
		this.server.shutdown();
	}

	@Test
	public void shouldReceiveResponseHeaders() throws Exception {
		prepareResponse(response -> response
				.setHeader("Content-Type", "text/plain")
				.setBody("Hello Spring!"));

		Mono<HttpHeaders> result = this.webClient.get()
				.uri("/greeting?name=Spring")
				.exchange()
				.map(response -> response.headers().asHttpHeaders());

		StepVerifier.create(result)
				.consumeNextWith(
						httpHeaders -> {
							assertEquals(MediaType.TEXT_PLAIN, httpHeaders.getContentType());
							assertEquals(13L, httpHeaders.getContentLength());
						})
				.expectComplete().verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertEquals("*/*", request.getHeader(HttpHeaders.ACCEPT));
			assertEquals("/greeting?name=Spring", request.getPath());
		});
	}

	@Test
	public void shouldReceivePlainText() throws Exception {
		prepareResponse(response -> response.setBody("Hello Spring!"));

		Mono<String> result = this.webClient.get()
				.uri("/greeting?name=Spring")
				.header("X-Test-Header", "testvalue")
				.exchange()
				.flatMap(response -> response.bodyToMono(String.class));

		StepVerifier.create(result)
				.expectNext("Hello Spring!")
				.expectComplete().verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertEquals("testvalue", request.getHeader("X-Test-Header"));
			assertEquals("*/*", request.getHeader(HttpHeaders.ACCEPT));
			assertEquals("/greeting?name=Spring", request.getPath());
		});
	}

	@Test
	public void shouldReceiveJsonAsString() throws Exception {
		String content = "{\"bar\":\"barbar\",\"foo\":\"foofoo\"}";
		prepareResponse(response -> response
				.setHeader("Content-Type", "application/json").setBody(content));

		Mono<String> result = this.webClient.get()
				.uri("/json").accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectNext(content)
				.expectComplete().verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertEquals("/json", request.getPath());
			assertEquals("application/json", request.getHeader(HttpHeaders.ACCEPT));
		});
	}

	@Test
	public void shouldReceiveJsonAsTypeReferenceString() throws Exception {
		String content = "{\"bar\":\"barbar\",\"foo\":\"foofoo\"}";
		prepareResponse(response -> response
				.setHeader("Content-Type", "application/json").setBody(content));

		Mono<String> result = this.webClient.get()
				.uri("/json").accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(new ParameterizedTypeReference<String>() {
				});

		StepVerifier.create(result)
				.expectNext(content)
				.expectComplete().verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertEquals("/json", request.getPath());
			assertEquals("application/json", request.getHeader(HttpHeaders.ACCEPT));
		});
	}

	@Test
	public void shouldReceiveJsonAsResponseEntityString() throws Exception {
		String content = "{\"bar\":\"barbar\",\"foo\":\"foofoo\"}";
		prepareResponse(response -> response
				.setHeader("Content-Type", "application/json").setBody(content));

		Mono<ResponseEntity<String>> result = this.webClient.get()
				.uri("/json").accept(MediaType.APPLICATION_JSON)
				.exchange()
				.flatMap(response -> response.toEntity(String.class));

		StepVerifier.create(result)
				.consumeNextWith(entity -> {
					assertEquals(HttpStatus.OK, entity.getStatusCode());
					assertEquals(MediaType.APPLICATION_JSON, entity.getHeaders().getContentType());
					assertEquals(31, entity.getHeaders().getContentLength());
					assertEquals(content, entity.getBody());
				})
				.expectComplete().verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertEquals("/json", request.getPath());
			assertEquals("application/json", request.getHeader(HttpHeaders.ACCEPT));
		});
	}

	@Test
	public void shouldReceiveJsonAsResponseEntityList() throws Exception {
		String content = "[{\"bar\":\"bar1\",\"foo\":\"foo1\"}, {\"bar\":\"bar2\",\"foo\":\"foo2\"}]";
		prepareResponse(response -> response
				.setHeader("Content-Type", "application/json").setBody(content));

		Mono<ResponseEntity<List<Pojo>>> result = this.webClient.get()
				.uri("/json").accept(MediaType.APPLICATION_JSON)
				.exchange()
				.flatMap(response -> response.toEntityList(Pojo.class));

		StepVerifier.create(result)
				.consumeNextWith(entity -> {
					assertEquals(HttpStatus.OK, entity.getStatusCode());
					assertEquals(MediaType.APPLICATION_JSON, entity.getHeaders().getContentType());
					assertEquals(58, entity.getHeaders().getContentLength());
					Pojo pojo1 = new Pojo("foo1", "bar1");
					Pojo pojo2 = new Pojo("foo2", "bar2");
					assertEquals(Arrays.asList(pojo1, pojo2), entity.getBody());
				})
				.expectComplete().verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertEquals("/json", request.getPath());
			assertEquals("application/json", request.getHeader(HttpHeaders.ACCEPT));
		});
	}

	@Test
	public void shouldReceiveJsonAsFluxString() throws Exception {
		String content = "{\"bar\":\"barbar\",\"foo\":\"foofoo\"}";
		prepareResponse(response -> response
				.setHeader("Content-Type", "application/json").setBody(content));

		Flux<String> result = this.webClient.get()
				.uri("/json").accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToFlux(String.class);

		StepVerifier.create(result)
				.expectNext(content)
				.expectComplete().verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertEquals("/json", request.getPath());
			assertEquals("application/json", request.getHeader(HttpHeaders.ACCEPT));
		});
	}

	@Test
	public void shouldReceiveJsonAsPojo() throws Exception {
		prepareResponse(response -> response
				.setHeader("Content-Type", "application/json")
				.setBody("{\"bar\":\"barbar\",\"foo\":\"foofoo\"}"));

		Mono<Pojo> result = this.webClient.get()
				.uri("/pojo")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.flatMap(response -> response.bodyToMono(Pojo.class));

		StepVerifier.create(result)
				.consumeNextWith(p -> assertEquals("barbar", p.getBar()))
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertEquals("/pojo", request.getPath());
			assertEquals("application/json", request.getHeader(HttpHeaders.ACCEPT));
		});
	}

	@Test
	public void shouldReceiveJsonAsFluxPojo() throws Exception {
		prepareResponse(response -> response
				.setHeader("Content-Type", "application/json")
				.setBody("[{\"bar\":\"bar1\",\"foo\":\"foo1\"},{\"bar\":\"bar2\",\"foo\":\"foo2\"}]"));

		Flux<Pojo> result = this.webClient.get()
				.uri("/pojos")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.flatMapMany(response -> response.bodyToFlux(Pojo.class));

		StepVerifier.create(result)
				.consumeNextWith(p -> assertThat(p.getBar(), Matchers.is("bar1")))
				.consumeNextWith(p -> assertThat(p.getBar(), Matchers.is("bar2")))
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertEquals("/pojos", request.getPath());
			assertEquals("application/json", request.getHeader(HttpHeaders.ACCEPT));
		});
	}

	@Test
	public void shouldSendPojoAsJson() throws Exception {
		prepareResponse(response -> response.setHeader("Content-Type", "application/json")
				.setBody("{\"bar\":\"BARBAR\",\"foo\":\"FOOFOO\"}"));

		Mono<Pojo> result = this.webClient.post()
				.uri("/pojo/capitalize")
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.syncBody(new Pojo("foofoo", "barbar"))
				.exchange()
				.flatMap(response -> response.bodyToMono(Pojo.class));

		StepVerifier.create(result)
				.consumeNextWith(p -> assertEquals("BARBAR", p.getBar()))
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertEquals("/pojo/capitalize", request.getPath());
			assertEquals("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}", request.getBody().readUtf8());
			assertEquals("chunked", request.getHeader(HttpHeaders.TRANSFER_ENCODING));
			assertEquals("application/json", request.getHeader(HttpHeaders.ACCEPT));
			assertEquals("application/json", request.getHeader(HttpHeaders.CONTENT_TYPE));
		});
	}

	@Test
	public void shouldSendCookies() throws Exception {
		prepareResponse(response -> response
				.setHeader("Content-Type", "text/plain").setBody("test"));

		Mono<String> result = this.webClient.get()
				.uri("/test")
				.cookie("testkey", "testvalue")
				.exchange()
				.flatMap(response -> response.bodyToMono(String.class));

		StepVerifier.create(result)
				.expectNext("test")
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertEquals("/test", request.getPath());
			assertEquals("testkey=testvalue", request.getHeader(HttpHeaders.COOKIE));
		});
	}

	@Test
	public void shouldReceive404Response() throws Exception {
		prepareResponse(response -> response.setResponseCode(404)
				.setHeader("Content-Type", "text/plain").setBody("Not Found"));

		Mono<ClientResponse> result = this.webClient.get().uri("/greeting?name=Spring").exchange();

		StepVerifier.create(result)
				.consumeNextWith(response -> assertEquals(HttpStatus.NOT_FOUND, response.statusCode()))
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertEquals("*/*", request.getHeader(HttpHeaders.ACCEPT));
			assertEquals("/greeting?name=Spring", request.getPath());
		});
	}

	@Test
	public void shouldGetErrorSignalOn404() throws Exception {
		prepareResponse(response -> response.setResponseCode(404)
				.setHeader("Content-Type", "text/plain").setBody("Not Found"));

		Mono<String> result = this.webClient.get()
				.uri("/greeting?name=Spring")
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectError(WebClientException.class)
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertEquals("*/*", request.getHeader(HttpHeaders.ACCEPT));
			assertEquals("/greeting?name=Spring", request.getPath());
		});
	}

	@Test // SPR-15946
	public void shouldGetErrorSignalOnEmptyErrorResponse() throws Exception {
		prepareResponse(response -> response.setResponseCode(404)
				.setHeader("Content-Type", "text/plain"));

		Mono<String> result = this.webClient.get().uri("/greeting")
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectError(WebClientException.class)
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertEquals("*/*", request.getHeader(HttpHeaders.ACCEPT));
			assertEquals("/greeting", request.getPath());
		});
	}

	@Test
	public void shouldGetInternalServerErrorSignal() throws Exception {
		String errorMessage = "Internal Server error";
		prepareResponse(response -> response.setResponseCode(500)
				.setHeader("Content-Type", "text/plain").setBody(errorMessage));

		Mono<String> result = this.webClient.get()
				.uri("/greeting?name=Spring")
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectErrorSatisfies(throwable -> {
					assertTrue(throwable instanceof WebClientResponseException);
					WebClientResponseException ex = (WebClientResponseException) throwable;
					assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
					assertEquals(MediaType.TEXT_PLAIN, ex.getHeaders().getContentType());
					assertEquals(errorMessage, ex.getResponseBodyAsString());
				})
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertEquals("*/*", request.getHeader(HttpHeaders.ACCEPT));
			assertEquals("/greeting?name=Spring", request.getPath());
		});
	}

	@Test
	public void shouldApplyCustomStatusHandler() throws Exception {
		prepareResponse(response -> response.setResponseCode(500)
				.setHeader("Content-Type", "text/plain").setBody("Internal Server error"));

		Mono<String> result = this.webClient.get()
				.uri("/greeting?name=Spring")
				.retrieve()
				.onStatus(HttpStatus::is5xxServerError, response -> Mono.just(new MyException("500 error!")))
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectError(MyException.class)
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertEquals("*/*", request.getHeader(HttpHeaders.ACCEPT));
			assertEquals("/greeting?name=Spring", request.getPath());
		});
	}

	@Test
	public void shouldApplyCustomStatusHandlerParameterizedTypeReference() throws Exception {
		prepareResponse(response -> response.setResponseCode(500)
				.setHeader("Content-Type", "text/plain").setBody("Internal Server error"));

		Mono<String> result = this.webClient.get()
				.uri("/greeting?name=Spring")
				.retrieve()
				.onStatus(HttpStatus::is5xxServerError, response -> Mono.just(new MyException("500 error!")))
				.bodyToMono(new ParameterizedTypeReference<String>() {});

		StepVerifier.create(result)
				.expectError(MyException.class)
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertEquals("*/*", request.getHeader(HttpHeaders.ACCEPT));
			assertEquals("/greeting?name=Spring", request.getPath());
		});
	}

	@Test
	public void shouldReceiveNotFoundEntity() throws Exception {
		prepareResponse(response -> response.setResponseCode(404)
				.setHeader("Content-Type", "text/plain").setBody("Not Found"));

		Mono<ResponseEntity<String>> result = this.webClient.get()
				.uri("/greeting?name=Spring")
				.exchange()
				.flatMap(response -> response.toEntity(String.class));

		StepVerifier.create(result)
				.consumeNextWith(response -> assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode()))
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertEquals("*/*", request.getHeader(HttpHeaders.ACCEPT));
			assertEquals("/greeting?name=Spring", request.getPath());
		});
	}

	@Test
	public void shouldApplyExchangeFilter() throws Exception {
		prepareResponse(response -> response.setHeader("Content-Type", "text/plain")
				.setBody("Hello Spring!"));

		WebClient filteredClient = this.webClient.mutate()
				.filter((request, next) -> {
					ClientRequest filteredRequest =
							ClientRequest.from(request).header("foo", "bar").build();
					return next.exchange(filteredRequest);
				})
				.build();

		Mono<String> result = filteredClient.get()
				.uri("/greeting?name=Spring")
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectNext("Hello Spring!")
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> assertEquals("bar", request.getHeader("foo")));
	}

	@Test
	public void shouldApplyErrorHandlingFilter() throws Exception {

		ExchangeFilterFunction filter = ExchangeFilterFunction.ofResponseProcessor(
				clientResponse -> {
					List<String> headerValues = clientResponse.headers().header("Foo");
					return headerValues.isEmpty() ? Mono.error(
							new MyException("Response does not contain Foo header")) :
							Mono.just(clientResponse);
				}
		);

		WebClient filteredClient = this.webClient.mutate().filter(filter).build();

		// header not present
		prepareResponse(response -> response
				.setHeader("Content-Type", "text/plain").setBody("Hello Spring!"));

		Mono<String> result = filteredClient.get()
				.uri("/greeting?name=Spring")
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectError(MyException.class).verify(Duration.ofSeconds(3));

		// header present

		prepareResponse(response -> response.setHeader("Content-Type", "text/plain")
				.setHeader("Foo", "Bar")
				.setBody("Hello Spring!"));

		result = filteredClient.get()
				.uri("/greeting?name=Spring")
				.retrieve().bodyToMono(String.class);

		StepVerifier.create(result)
				.expectNext("Hello Spring!")
				.expectComplete().verify(Duration.ofSeconds(3));

		expectRequestCount(2);
	}

	@Test
	public void shouldReceiveEmptyResponse() throws Exception {
		prepareResponse(response -> response.setHeader("Content-Length", "0").setBody(""));

		Mono<ResponseEntity<Void>> result = this.webClient.get()
				.uri("/noContent")
				.exchange()
				.flatMap(response -> response.toEntity(Void.class));

		StepVerifier.create(result).assertNext(r -> {
			assertTrue(r.getStatusCode().is2xxSuccessful());
		}).verifyComplete();
	}

	@Test // SPR-15782
	public void shouldFailWithRelativeUrls() throws Exception {
		String uri = "/api/v4/groups/1";
		Mono<ClientResponse> responseMono = WebClient.builder().build().get().uri(uri).exchange();

		StepVerifier.create(responseMono)
				.expectErrorMessage("URI is not absolute: " + uri)
				.verify(Duration.ofSeconds(5));
	}

	private void prepareResponse(Consumer<MockResponse> consumer) {
		MockResponse response = new MockResponse();
		consumer.accept(response);
		this.server.enqueue(response);
	}

	private void expectRequest(Consumer<RecordedRequest> consumer) throws InterruptedException {
		consumer.accept(this.server.takeRequest());
	}

	private void expectRequestCount(int count) {
		assertEquals(count, this.server.getRequestCount());
	}


	@SuppressWarnings("serial")
	private static class MyException extends RuntimeException {

		MyException(String message) {
			super(message);
		}
	}

}
