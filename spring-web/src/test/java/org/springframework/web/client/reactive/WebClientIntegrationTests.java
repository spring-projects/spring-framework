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

package org.springframework.web.client.reactive;

import java.time.Duration;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.subscriber.ScriptedSubscriber;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.BodyExtractors;
import org.springframework.http.codec.BodyInserters;
import org.springframework.http.codec.Pojo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.springframework.http.codec.BodyExtractors.toFlux;
import static org.springframework.http.codec.BodyExtractors.toMono;

/**
 * {@link WebClient} integration tests with the {@code Flux} and {@code Mono} API.
 *
 * @author Brian Clozel
 */
public class WebClientIntegrationTests {

	private MockWebServer server;

	private WebClient webClient;

	@Before
	public void setup() {
		this.server = new MockWebServer();
		this.webClient = WebClient.create(new ReactorClientHttpConnector());
	}

	@Test
	public void headers() throws Exception {
		HttpUrl baseUrl = server.url("/greeting?name=Spring");
		this.server.enqueue(new MockResponse().setHeader("Content-Type", "text/plain").setBody("Hello Spring!"));

		ClientRequest<Void> request = ClientRequest.GET(baseUrl.toString()).build();
		Mono<HttpHeaders> result = this.webClient
				.exchange(request)
				.map(response -> response.headers().asHttpHeaders());

		ScriptedSubscriber.<HttpHeaders>create()
				.consumeNextWith(
						httpHeaders -> {
							assertEquals(MediaType.TEXT_PLAIN, httpHeaders.getContentType());
							assertEquals(13L, httpHeaders.getContentLength());
						})
				.expectComplete()
				.verify(result);

		RecordedRequest recordedRequest = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("*/*", recordedRequest.getHeader(HttpHeaders.ACCEPT));
		assertEquals("/greeting?name=Spring", recordedRequest.getPath());
	}

	@Test
	public void plainText() throws Exception {
		HttpUrl baseUrl = server.url("/greeting?name=Spring");
		this.server.enqueue(new MockResponse().setBody("Hello Spring!"));

		ClientRequest<Void> request = ClientRequest.GET(baseUrl.toString())
				.header("X-Test-Header", "testvalue")
				.build();

		Mono<String> result = this.webClient
				.exchange(request)
				.then(response -> response.body(toMono(String.class)));

		ScriptedSubscriber
				.<String>create()
				.expectNext("Hello Spring!")
				.expectComplete()
				.verify(result);

		RecordedRequest recordedRequest = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("testvalue", recordedRequest.getHeader("X-Test-Header"));
		assertEquals("*/*", recordedRequest.getHeader(HttpHeaders.ACCEPT));
		assertEquals("/greeting?name=Spring", recordedRequest.getPath());
	}

	@Test
	public void retrieveMono() throws Exception {
		HttpUrl baseUrl = server.url("/greeting?name=Spring");
		this.server.enqueue(new MockResponse().setBody("Hello Spring!"));

		ClientRequest<Void> request = ClientRequest.GET(baseUrl.toString()).build();

		Mono<String> result = this.webClient
				.retrieveMono(request, String.class);

		ScriptedSubscriber
				.<String>create()
				.expectNext("Hello Spring!")
				.expectComplete()
				.verify(result);

		RecordedRequest recordedRequest = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("*/*", recordedRequest.getHeader(HttpHeaders.ACCEPT));
		assertEquals("/greeting?name=Spring", recordedRequest.getPath());
	}

	@Test
	public void retrieveFlux() throws Exception {
		HttpUrl baseUrl = server.url("/greeting?name=Spring");
		this.server.enqueue(new MockResponse().setBody("Hello Spring!"));

		ClientRequest<Void> request = ClientRequest.GET(baseUrl.toString()).build();

		Flux<String> result = this.webClient
				.retrieveFlux(request, String.class);

		ScriptedSubscriber
				.<String>create()
				.expectNext("Hello Spring!")
				.expectComplete()
				.verify(result);

		RecordedRequest recordedRequest = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("*/*", recordedRequest.getHeader(HttpHeaders.ACCEPT));
		assertEquals("/greeting?name=Spring", recordedRequest.getPath());
	}

	@Test
	public void jsonString() throws Exception {
		HttpUrl baseUrl = server.url("/json");
		String content = "{\"bar\":\"barbar\",\"foo\":\"foofoo\"}";
		this.server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
				.setBody(content));

		ClientRequest<Void> request = ClientRequest.GET(baseUrl.toString())
				.accept(MediaType.APPLICATION_JSON)
				.build();

		Mono<String> result = this.webClient
				.exchange(request)
				.then(response -> response.body(toMono(String.class)));

		ScriptedSubscriber
				.<String>create()
				.expectNext(content)
				.expectComplete()
				.verify(result);

		RecordedRequest recordedRequest = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("/json", recordedRequest.getPath());
		assertEquals("application/json", recordedRequest.getHeader(HttpHeaders.ACCEPT));
	}

	@Test
	public void jsonPojoMono() throws Exception {
		HttpUrl baseUrl = server.url("/pojo");
		this.server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
				.setBody("{\"bar\":\"barbar\",\"foo\":\"foofoo\"}"));

		ClientRequest<Void> request = ClientRequest.GET(baseUrl.toString())
				.accept(MediaType.APPLICATION_JSON)
				.build();

		Mono<Pojo> result = this.webClient
				.exchange(request)
				.then(response -> response.body(toMono(Pojo.class)));

		ScriptedSubscriber
				.<Pojo>create()
				.consumeNextWith(p -> assertEquals("barbar", p.getBar()))
				.expectComplete()
				.verify(result);

		RecordedRequest recordedRequest = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("/pojo", recordedRequest.getPath());
		assertEquals("application/json", recordedRequest.getHeader(HttpHeaders.ACCEPT));
	}

	@Test
	public void jsonPojoFlux() throws Exception {
		HttpUrl baseUrl = server.url("/pojos");
		this.server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
				.setBody("[{\"bar\":\"bar1\",\"foo\":\"foo1\"},{\"bar\":\"bar2\",\"foo\":\"foo2\"}]"));

		ClientRequest<Void> request = ClientRequest.GET(baseUrl.toString())
				.accept(MediaType.APPLICATION_JSON)
				.build();

		Flux<Pojo> result = this.webClient
				.exchange(request)
				.flatMap(response -> response.body(toFlux(Pojo.class)));

		ScriptedSubscriber
				.<Pojo>create()
				.consumeNextWith(p -> assertThat(p.getBar(), Matchers.is("bar1")))
				.consumeNextWith(p -> assertThat(p.getBar(), Matchers.is("bar2")))
				.expectComplete()
				.verify(result);

		RecordedRequest recordedRequest = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("/pojos", recordedRequest.getPath());
		assertEquals("application/json", recordedRequest.getHeader(HttpHeaders.ACCEPT));
	}

	@Test
	public void postJsonPojo() throws Exception {
		HttpUrl baseUrl = server.url("/pojo/capitalize");
		this.server.enqueue(new MockResponse()
				.setHeader("Content-Type", "application/json")
				.setBody("{\"bar\":\"BARBAR\",\"foo\":\"FOOFOO\"}"));

		Pojo spring = new Pojo("foofoo", "barbar");
		ClientRequest<Pojo> request = ClientRequest.POST(baseUrl.toString())
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.body(BodyInserters.fromObject(spring));

		Mono<Pojo> result = this.webClient
				.exchange(request)
				.then(response -> response.body(BodyExtractors.toMono(Pojo.class)));

		ScriptedSubscriber
				.<Pojo>create()
				.consumeNextWith(p -> assertEquals("BARBAR", p.getBar()))
				.expectComplete()
				.verify(result);

		RecordedRequest recordedRequest = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("/pojo/capitalize", recordedRequest.getPath());
		assertEquals("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}", recordedRequest.getBody().readUtf8());
		assertEquals("chunked", recordedRequest.getHeader(HttpHeaders.TRANSFER_ENCODING));
		assertEquals("application/json", recordedRequest.getHeader(HttpHeaders.ACCEPT));
		assertEquals("application/json", recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE));
	}

	@Test
	public void cookies() throws Exception {
		HttpUrl baseUrl = server.url("/test");
		this.server.enqueue(new MockResponse()
				.setHeader("Content-Type", "text/plain").setBody("test"));

		ClientRequest<Void> request = ClientRequest.GET(baseUrl.toString())
				.cookie("testkey", "testvalue")
				.build();

		Mono<String> result = this.webClient
				.exchange(request)
				.then(response -> response.body(toMono(String.class)));

		ScriptedSubscriber
				.<String>create()
				.expectNext("test")
				.expectComplete()
				.verify(result);

		RecordedRequest recordedRequest = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("/test", recordedRequest.getPath());
		assertEquals("testkey=testvalue", recordedRequest.getHeader(HttpHeaders.COOKIE));
	}

	@Test
	public void notFound() throws Exception {
		HttpUrl baseUrl = server.url("/greeting?name=Spring");
		this.server.enqueue(new MockResponse().setResponseCode(404)
				.setHeader("Content-Type", "text/plain").setBody("Not Found"));

		ClientRequest<Void> request = ClientRequest.GET(baseUrl.toString()).build();

		Mono<ClientResponse> result = this.webClient
				.exchange(request);

		ScriptedSubscriber
				.<ClientResponse>create()
				.consumeNextWith(response -> {
					assertEquals(HttpStatus.NOT_FOUND, response.statusCode());
				})
				.expectComplete()
				.verify(result, Duration.ofSeconds(3));

		RecordedRequest recordedRequest = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("*/*", recordedRequest.getHeader(HttpHeaders.ACCEPT));
		assertEquals("/greeting?name=Spring", recordedRequest.getPath());
	}

	@Test
	public void retrieveNotFound() throws Exception {
		HttpUrl baseUrl = server.url("/greeting?name=Spring");
		this.server.enqueue(new MockResponse().setResponseCode(404)
				.setHeader("Content-Type", "text/plain").setBody("Not Found"));

		ClientRequest<Void> request = ClientRequest.GET(baseUrl.toString()).build();

		Mono<String> result = this.webClient
				.retrieveMono(request, String.class);

		ScriptedSubscriber
				.<String>create()
				.expectError(WebClientException.class)
				.verify(result, Duration.ofSeconds(3));

		RecordedRequest recordedRequest = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("*/*", recordedRequest.getHeader(HttpHeaders.ACCEPT));
		assertEquals("/greeting?name=Spring", recordedRequest.getPath());
	}

	@Test
	public void retrieveServerError() throws Exception {
		HttpUrl baseUrl = server.url("/greeting?name=Spring");
		this.server.enqueue(new MockResponse().setResponseCode(500)
				.setHeader("Content-Type", "text/plain").setBody("Not Found"));

		ClientRequest<Void> request = ClientRequest.GET(baseUrl.toString()).build();

		Mono<String> result = this.webClient
				.retrieveMono(request, String.class);

		ScriptedSubscriber
				.<String>create()
				.expectError(WebClientException.class)
				.verify(result, Duration.ofSeconds(3));

		RecordedRequest recordedRequest = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("*/*", recordedRequest.getHeader(HttpHeaders.ACCEPT));
		assertEquals("/greeting?name=Spring", recordedRequest.getPath());
	}

	@Test
	public void filter() throws Exception {
		HttpUrl baseUrl = server.url("/greeting?name=Spring");
		this.server.enqueue(new MockResponse().setHeader("Content-Type", "text/plain").setBody("Hello Spring!"));

		ExchangeFilterFunction filter = (request, next) -> {
			ClientRequest<?> filteredRequest = ClientRequest.from(request)
					.header("foo", "bar").build();
			return next.exchange(filteredRequest);
		};
		WebClient filteredClient = WebClient.builder(new ReactorClientHttpConnector())
				.filter(filter).build();

		ClientRequest<Void> request = ClientRequest.GET(baseUrl.toString()).build();

		Mono<String> result = filteredClient.exchange(request)
				.then(response -> response.body(toMono(String.class)));

		ScriptedSubscriber
				.<String>create()
				.expectNext("Hello Spring!")
				.expectComplete()
				.verify(result);

		RecordedRequest recordedRequest = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("bar", recordedRequest.getHeader("foo"));

	}

	@After
	public void tearDown() throws Exception {
		this.server.shutdown();
	}
}