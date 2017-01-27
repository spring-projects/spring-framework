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

package org.springframework.web.reactive.function.client;

import java.time.Duration;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.Pojo;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.springframework.web.reactive.function.BodyInserters.fromObject;

/**
 * Integration tests using a {@link WebClient} through {@link WebClientOperations}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 */
public class WebClientIntegrationTests {

	private MockWebServer server;

	private WebClientOperations operations;


	@Before
	public void setup() {
		this.server = new MockWebServer();

		WebClient webClient = WebClient.create(new ReactorClientHttpConnector());
		UriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory(this.server.url("/").toString());

		this.operations = WebClientOperations.builder(webClient)
				.uriBuilderFactory(uriBuilderFactory)
				.build();
	}

	@After
	public void tearDown() throws Exception {
		this.server.shutdown();
	}


	@Test
	public void headers() throws Exception {
		this.server.enqueue(new MockResponse().setHeader("Content-Type", "text/plain").setBody("Hello Spring!"));

		Mono<HttpHeaders> result = this.operations.get()
				.uri("/greeting?name=Spring")
				.exchange()
				.map(response -> response.headers().asHttpHeaders());

		StepVerifier.create(result)
				.consumeNextWith(
						httpHeaders -> {
							assertEquals(MediaType.TEXT_PLAIN, httpHeaders.getContentType());
							assertEquals(13L, httpHeaders.getContentLength());
						})
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		RecordedRequest recordedRequest = server.takeRequest();
		Assert.assertEquals(1, server.getRequestCount());
		Assert.assertEquals("*/*", recordedRequest.getHeader(HttpHeaders.ACCEPT));
		Assert.assertEquals("/greeting?name=Spring", recordedRequest.getPath());
	}

	@Test
	public void plainText() throws Exception {
		this.server.enqueue(new MockResponse().setBody("Hello Spring!"));

		Mono<String> result = this.operations.get()
				.uri("/greeting?name=Spring")
				.header("X-Test-Header", "testvalue")
				.exchange()
				.then(response -> response.bodyToMono(String.class));

		StepVerifier.create(result)
				.expectNext("Hello Spring!")
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		RecordedRequest recordedRequest = server.takeRequest();
		Assert.assertEquals(1, server.getRequestCount());
		Assert.assertEquals("testvalue", recordedRequest.getHeader("X-Test-Header"));
		Assert.assertEquals("*/*", recordedRequest.getHeader(HttpHeaders.ACCEPT));
		Assert.assertEquals("/greeting?name=Spring", recordedRequest.getPath());
	}

	@Test
	public void jsonString() throws Exception {
		String content = "{\"bar\":\"barbar\",\"foo\":\"foofoo\"}";
		this.server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
				.setBody(content));

		Mono<String> result = this.operations.get()
				.uri("/json")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.then(response -> response.bodyToMono(String.class));

		StepVerifier.create(result)
				.expectNext(content)
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		RecordedRequest recordedRequest = server.takeRequest();
		Assert.assertEquals(1, server.getRequestCount());
		Assert.assertEquals("/json", recordedRequest.getPath());
		Assert.assertEquals("application/json", recordedRequest.getHeader(HttpHeaders.ACCEPT));
	}

	@Test
	public void jsonPojoMono() throws Exception {
		this.server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
				.setBody("{\"bar\":\"barbar\",\"foo\":\"foofoo\"}"));

		Mono<Pojo> result = this.operations.get()
				.uri("/pojo")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.then(response -> response.bodyToMono(Pojo.class));

		StepVerifier.create(result)
				.consumeNextWith(p -> assertEquals("barbar", p.getBar()))
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		RecordedRequest recordedRequest = server.takeRequest();
		Assert.assertEquals(1, server.getRequestCount());
		Assert.assertEquals("/pojo", recordedRequest.getPath());
		Assert.assertEquals("application/json", recordedRequest.getHeader(HttpHeaders.ACCEPT));
	}

	@Test
	public void jsonPojoFlux() throws Exception {
		this.server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
				.setBody("[{\"bar\":\"bar1\",\"foo\":\"foo1\"},{\"bar\":\"bar2\",\"foo\":\"foo2\"}]"));

		Flux<Pojo> result = this.operations.get()
				.uri("/pojos")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.flatMap(response -> response.bodyToFlux(Pojo.class));

		StepVerifier.create(result)
				.consumeNextWith(p -> assertThat(p.getBar(), Matchers.is("bar1")))
				.consumeNextWith(p -> assertThat(p.getBar(), Matchers.is("bar2")))
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		RecordedRequest recordedRequest = server.takeRequest();
		Assert.assertEquals(1, server.getRequestCount());
		Assert.assertEquals("/pojos", recordedRequest.getPath());
		Assert.assertEquals("application/json", recordedRequest.getHeader(HttpHeaders.ACCEPT));
	}

	@Test
	public void postJsonPojo() throws Exception {
		this.server.enqueue(new MockResponse()
				.setHeader("Content-Type", "application/json")
				.setBody("{\"bar\":\"BARBAR\",\"foo\":\"FOOFOO\"}"));

		Mono<Pojo> result = this.operations.post()
				.uri("/pojo/capitalize")
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.exchange(fromObject(new Pojo("foofoo", "barbar")))
				.then(response -> response.bodyToMono(Pojo.class));

		StepVerifier.create(result)
				.consumeNextWith(p -> assertEquals("BARBAR", p.getBar()))
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		RecordedRequest recordedRequest = server.takeRequest();
		Assert.assertEquals(1, server.getRequestCount());
		Assert.assertEquals("/pojo/capitalize", recordedRequest.getPath());
		Assert.assertEquals("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}", recordedRequest.getBody().readUtf8());
		Assert.assertEquals("chunked", recordedRequest.getHeader(HttpHeaders.TRANSFER_ENCODING));
		Assert.assertEquals("application/json", recordedRequest.getHeader(HttpHeaders.ACCEPT));
		Assert.assertEquals("application/json", recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE));
	}

	@Test
	public void cookies() throws Exception {
		this.server.enqueue(new MockResponse()
				.setHeader("Content-Type", "text/plain").setBody("test"));

		Mono<String> result = this.operations.get()
				.uri("/test")
				.cookie("testkey", "testvalue")
				.exchange()
				.then(response -> response.bodyToMono(String.class));

		StepVerifier.create(result)
				.expectNext("test")
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		RecordedRequest recordedRequest = server.takeRequest();
		Assert.assertEquals(1, server.getRequestCount());
		Assert.assertEquals("/test", recordedRequest.getPath());
		Assert.assertEquals("testkey=testvalue", recordedRequest.getHeader(HttpHeaders.COOKIE));
	}

	@Test
	public void notFound() throws Exception {
		this.server.enqueue(new MockResponse().setResponseCode(404)
				.setHeader("Content-Type", "text/plain").setBody("Not Found"));

		Mono<ClientResponse> result = this.operations.get().uri("/greeting?name=Spring").exchange();

		StepVerifier.create(result)
				.consumeNextWith(response -> assertEquals(HttpStatus.NOT_FOUND, response.statusCode()))
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		RecordedRequest recordedRequest = server.takeRequest();
		Assert.assertEquals(1, server.getRequestCount());
		Assert.assertEquals("*/*", recordedRequest.getHeader(HttpHeaders.ACCEPT));
		Assert.assertEquals("/greeting?name=Spring", recordedRequest.getPath());
	}

	@Test
	public void buildFilter() throws Exception {
		this.server.enqueue(new MockResponse().setHeader("Content-Type", "text/plain").setBody("Hello Spring!"));

		WebClientOperations filteredClient = this.operations.filter(
				(request, next) -> {
					ClientRequest<?> filteredRequest = ClientRequest.from(request).header("foo", "bar").build();
					return next.exchange(filteredRequest);
				});

		Mono<String> result = filteredClient.get()
				.uri("/greeting?name=Spring")
				.exchange()
				.then(response -> response.bodyToMono(String.class));

		StepVerifier.create(result)
				.expectNext("Hello Spring!")
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		RecordedRequest recordedRequest = server.takeRequest();
		Assert.assertEquals(1, server.getRequestCount());
		Assert.assertEquals("bar", recordedRequest.getHeader("foo"));

	}

	@Test
	public void filter() throws Exception {
		this.server.enqueue(new MockResponse().setHeader("Content-Type", "text/plain").setBody("Hello Spring!"));

		WebClientOperations filteredClient = this.operations.filter(
				(request, next) -> {
					ClientRequest<?> filteredRequest = ClientRequest.from(request).header("foo", "bar").build();
					return next.exchange(filteredRequest);
				});

		Mono<String> result = filteredClient.get()
				.uri("/greeting?name=Spring")
				.exchange()
				.then(response -> response.bodyToMono(String.class));

		StepVerifier.create(result)
				.expectNext("Hello Spring!")
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		RecordedRequest recordedRequest = server.takeRequest();
		Assert.assertEquals(1, server.getRequestCount());
		Assert.assertEquals("bar", recordedRequest.getHeader("foo"));

	}

}