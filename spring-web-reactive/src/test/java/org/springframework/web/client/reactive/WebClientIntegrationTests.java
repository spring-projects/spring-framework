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

import static org.junit.Assert.*;
import static org.springframework.web.client.reactive.HttpRequestBuilders.*;
import static org.springframework.web.client.reactive.WebResponseExtractors.*;

import java.util.function.Consumer;

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
import reactor.core.test.TestSubscriber;

import org.springframework.http.codec.Pojo;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorHttpClientRequestFactory;

/**
 * @author Brian Clozel
 */
public class WebClientIntegrationTests {

	private MockWebServer server;

	private WebClient webClient;

	@Before
	public void setup() {
		this.server = new MockWebServer();
		this.webClient = new WebClient(new ReactorHttpClientRequestFactory());
	}

	@Test
	public void shouldGetHeaders() throws Exception {

		HttpUrl baseUrl = server.url("/greeting?name=Spring");
		this.server.enqueue(new MockResponse().setHeader("Content-Type", "text/plain").setBody("Hello Spring!"));

		Mono<HttpHeaders> result = this.webClient
				.perform(get(baseUrl.toString()))
				.extract(headers());

		TestSubscriber
				.subscribe(result)
				.awaitAndAssertNextValuesWith(
					httpHeaders -> {
						assertEquals(MediaType.TEXT_PLAIN, httpHeaders.getContentType());
						assertEquals(13L, httpHeaders.getContentLength());
					})
				.assertComplete();

		RecordedRequest request = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("*/*", request.getHeader(HttpHeaders.ACCEPT));
		assertEquals("/greeting?name=Spring", request.getPath());
	}

	@Test
	public void shouldGetPlainTextResponseAsObject() throws Exception {

		HttpUrl baseUrl = server.url("/greeting?name=Spring");
		this.server.enqueue(new MockResponse().setBody("Hello Spring!"));

		Mono<String> result = this.webClient
				.perform(get(baseUrl.toString())
						.header("X-Test-Header", "testvalue"))
				.extract(body(String.class));


		TestSubscriber
				.subscribe(result)
				.awaitAndAssertNextValues("Hello Spring!")
				.assertComplete();

		RecordedRequest request = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("testvalue", request.getHeader("X-Test-Header"));
		assertEquals("*/*", request.getHeader(HttpHeaders.ACCEPT));
		assertEquals("/greeting?name=Spring", request.getPath());
	}

	@Test
	public void shouldGetPlainTextResponse() throws Exception {

		HttpUrl baseUrl = server.url("/greeting?name=Spring");
		this.server.enqueue(new MockResponse().setHeader("Content-Type", "text/plain").setBody("Hello Spring!"));

		Mono<ResponseEntity<String>> result = this.webClient
				.perform(get(baseUrl.toString())
						.accept(MediaType.TEXT_PLAIN))
				.extract(response(String.class));

		TestSubscriber
				.subscribe(result)
				.awaitAndAssertNextValuesWith((Consumer<ResponseEntity<String>>) response -> {
					assertEquals(200, response.getStatusCode().value());
					assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
					assertEquals("Hello Spring!", response.getBody());
		});
		RecordedRequest request = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("/greeting?name=Spring", request.getPath());
		assertEquals("text/plain", request.getHeader(HttpHeaders.ACCEPT));
	}

	@Test
	public void shouldGetJsonAsMonoOfString() throws Exception {

		HttpUrl baseUrl = server.url("/json");
		String content = "{\"bar\":\"barbar\",\"foo\":\"foofoo\"}";
		this.server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
				.setBody(content));

		Mono<String> result = this.webClient
				.perform(get(baseUrl.toString())
						.accept(MediaType.APPLICATION_JSON))
				.extract(body(String.class));

		TestSubscriber
				.subscribe(result)
				.awaitAndAssertNextValues(content)
				.assertComplete();
		RecordedRequest request = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("/json", request.getPath());
		assertEquals("application/json", request.getHeader(HttpHeaders.ACCEPT));
	}

	@Test
	public void shouldGetJsonAsMonoOfPojo() throws Exception {

		HttpUrl baseUrl = server.url("/pojo");
		this.server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
				.setBody("{\"bar\":\"barbar\",\"foo\":\"foofoo\"}"));

		Mono<Pojo> result = this.webClient
				.perform(get(baseUrl.toString())
						.accept(MediaType.APPLICATION_JSON))
				.extract(body(Pojo.class));

		TestSubscriber
				.subscribe(result)
				.awaitAndAssertNextValuesWith(p -> assertEquals("barbar", p.getBar()))
				.assertComplete();
		RecordedRequest request = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("/pojo", request.getPath());
		assertEquals("application/json", request.getHeader(HttpHeaders.ACCEPT));
	}

	@Test
	public void shouldGetJsonAsFluxOfPojos() throws Exception {

		HttpUrl baseUrl = server.url("/pojos");
		this.server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
				.setBody("[{\"bar\":\"bar1\",\"foo\":\"foo1\"},{\"bar\":\"bar2\",\"foo\":\"foo2\"}]"));

		Flux<Pojo> result = this.webClient
				.perform(get(baseUrl.toString())
						.accept(MediaType.APPLICATION_JSON))
				.extract(bodyStream(Pojo.class));

		TestSubscriber
				.subscribe(result)
				.awaitAndAssertNextValuesWith(
					p -> assertThat(p.getBar(), Matchers.is("bar1")),
					p -> assertThat(p.getBar(), Matchers.is("bar2")))
				.assertValueCount(2)
				.assertComplete();
		RecordedRequest request = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("/pojos", request.getPath());
		assertEquals("application/json", request.getHeader(HttpHeaders.ACCEPT));
	}

	@Test
	public void shouldGetJsonAsResponseOfPojosStream() throws Exception {

		HttpUrl baseUrl = server.url("/pojos");
		this.server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
				.setBody("[{\"bar\":\"bar1\",\"foo\":\"foo1\"},{\"bar\":\"bar2\",\"foo\":\"foo2\"}]"));

		Mono<ResponseEntity<Flux<Pojo>>> result = this.webClient
				.perform(get(baseUrl.toString())
						.accept(MediaType.APPLICATION_JSON))
				.extract(responseStream(Pojo.class));

		TestSubscriber
				.subscribe(result)
				.awaitAndAssertNextValuesWith(
					response -> {
						assertEquals(200, response.getStatusCode().value());
						assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
					})
				.assertComplete();
		RecordedRequest request = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("/pojos", request.getPath());
		assertEquals("application/json", request.getHeader(HttpHeaders.ACCEPT));
	}

	@Test
	public void shouldPostPojoAsJson() throws Exception {

		HttpUrl baseUrl = server.url("/pojo/capitalize");
		this.server.enqueue(new MockResponse().setBody("{\"bar\":\"BARBAR\",\"foo\":\"FOOFOO\"}"));

		Pojo spring = new Pojo("foofoo", "barbar");
		Mono<Pojo> result = this.webClient
				.perform(post(baseUrl.toString())
						.content(spring)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.extract(body(Pojo.class));

		TestSubscriber
				.subscribe(result)
				.awaitAndAssertNextValuesWith(p -> assertEquals("BARBAR", p.getBar()))
				.assertComplete();

		RecordedRequest request = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("/pojo/capitalize", request.getPath());
		assertEquals("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}", request.getBody().readUtf8());
		assertEquals("chunked", request.getHeader(HttpHeaders.TRANSFER_ENCODING));
		assertEquals("application/json", request.getHeader(HttpHeaders.ACCEPT));
		assertEquals("application/json", request.getHeader(HttpHeaders.CONTENT_TYPE));
	}

	@Test
	public void shouldGetErrorWhen404() throws Exception {

		HttpUrl baseUrl = server.url("/greeting?name=Spring");
		this.server.enqueue(new MockResponse().setResponseCode(404));

		Mono<String> result = this.webClient
				.perform(get(baseUrl.toString()))
				.extract(body(String.class));

		// TODO: error message should be converted to a ClientException
		TestSubscriber
				.subscribe(result)
				.await()
				.assertError();

		RecordedRequest request = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("*/*", request.getHeader(HttpHeaders.ACCEPT));
		assertEquals("/greeting?name=Spring", request.getPath());
	}

	@After
	public void tearDown() throws Exception {
		this.server.shutdown();
	}

}
