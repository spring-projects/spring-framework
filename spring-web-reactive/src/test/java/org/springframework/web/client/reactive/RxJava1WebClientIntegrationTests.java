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
import static org.springframework.web.client.reactive.support.RxJava1ClientWebRequestBuilders.*;
import static org.springframework.web.client.reactive.support.RxJava1ResponseExtractors.*;

import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import rx.Observable;
import rx.Single;
import rx.observers.TestSubscriber;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.Pojo;

/**
 * {@link WebClient} integration tests with the {@code Obserable} and {@code Single} API.
 *
 * @author Brian Clozel
 */
public class RxJava1WebClientIntegrationTests {

	private MockWebServer server;

	private WebClient webClient;

	@Before
	public void setup() {
		this.server = new MockWebServer();
		this.webClient = new WebClient(new ReactorClientHttpConnector());
	}

	@Test
	public void shouldGetHeaders() throws Exception {

		HttpUrl baseUrl = server.url("/greeting?name=Spring");
		this.server.enqueue(new MockResponse().setHeader("Content-Type", "text/plain").setBody("Hello Spring!"));

		Single<HttpHeaders> result = this.webClient
				.perform(get(baseUrl.toString()))
				.extract(headers());

		TestSubscriber<HttpHeaders> ts = new TestSubscriber<HttpHeaders>();
		result.subscribe(ts);
		ts.awaitTerminalEvent(2, TimeUnit.SECONDS);

		HttpHeaders httpHeaders = ts.getOnNextEvents().get(0);
		assertEquals(MediaType.TEXT_PLAIN, httpHeaders.getContentType());
		assertEquals(13L, httpHeaders.getContentLength());
		ts.assertValueCount(1);
		ts.assertCompleted();

		RecordedRequest request = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("*/*", request.getHeader(HttpHeaders.ACCEPT));
		assertEquals("/greeting?name=Spring", request.getPath());
	}

	@Test
	public void shouldGetPlainTextResponseAsObject() throws Exception {

		HttpUrl baseUrl = server.url("/greeting?name=Spring");
		this.server.enqueue(new MockResponse().setBody("Hello Spring!"));

		Single<String> result = this.webClient
				.perform(get(baseUrl.toString())
						.header("X-Test-Header", "testvalue"))
				.extract(body(String.class));

		TestSubscriber<String> ts = new TestSubscriber<String>();
		result.subscribe(ts);
		ts.awaitTerminalEvent(2, TimeUnit.SECONDS);

		String response = ts.getOnNextEvents().get(0);
		assertEquals("Hello Spring!", response);
		ts.assertValueCount(1);
		ts.assertCompleted();

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

		Single<ResponseEntity<String>> result = this.webClient
				.perform(get(baseUrl.toString())
						.accept(MediaType.TEXT_PLAIN))
				.extract(response(String.class));

		TestSubscriber<ResponseEntity<String>> ts = new TestSubscriber<ResponseEntity<String>>();
		result.subscribe(ts);
		ts.awaitTerminalEvent(2, TimeUnit.SECONDS);

		ResponseEntity<String> response = ts.getOnNextEvents().get(0);
		assertEquals(200, response.getStatusCode().value());
		assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
		assertEquals("Hello Spring!", response.getBody());
		ts.assertValueCount(1);
		ts.assertCompleted();

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

		Single<String> result = this.webClient
				.perform(get(baseUrl.toString())
						.accept(MediaType.APPLICATION_JSON))
				.extract(body(String.class));

		TestSubscriber<String> ts = new TestSubscriber<String>();
		result.subscribe(ts);
		ts.awaitTerminalEvent(2, TimeUnit.SECONDS);

		String response = ts.getOnNextEvents().get(0);
		assertEquals(content, response);
		ts.assertValueCount(1);
		ts.assertCompleted();

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

		Single<Pojo> result = this.webClient
				.perform(get(baseUrl.toString())
						.accept(MediaType.APPLICATION_JSON))
				.extract(body(Pojo.class));

		TestSubscriber<Pojo> ts = new TestSubscriber<Pojo>();
		result.subscribe(ts);
		ts.awaitTerminalEvent(2, TimeUnit.SECONDS);

		Pojo response = ts.getOnNextEvents().get(0);
		assertEquals("barbar", response.getBar());
		ts.assertValueCount(1);
		ts.assertCompleted();

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

		Observable<Pojo> result = this.webClient
				.perform(get(baseUrl.toString())
						.accept(MediaType.APPLICATION_JSON))
				.extract(bodyStream(Pojo.class));

		TestSubscriber<Pojo> ts = new TestSubscriber<Pojo>();
		result.subscribe(ts);
		ts.awaitTerminalEvent(2, TimeUnit.SECONDS);

		assertThat(ts.getOnNextEvents().get(0).getBar(), Matchers.is("bar1"));
		assertThat(ts.getOnNextEvents().get(1).getBar(), Matchers.is("bar2"));
		ts.assertValueCount(2);
		ts.assertCompleted();

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

		Single<ResponseEntity<Observable<Pojo>>> result = this.webClient
				.perform(get(baseUrl.toString())
						.accept(MediaType.APPLICATION_JSON))
				.extract(responseStream(Pojo.class));

		TestSubscriber<ResponseEntity<Observable<Pojo>>> ts = new TestSubscriber<ResponseEntity<Observable<Pojo>>>();
		result.subscribe(ts);
		ts.awaitTerminalEvent(2, TimeUnit.SECONDS);

		ResponseEntity<Observable<Pojo>> response = ts.getOnNextEvents().get(0);
		assertEquals(200, response.getStatusCode().value());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
		ts.assertValueCount(1);
		ts.assertCompleted();

		RecordedRequest request = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("/pojos", request.getPath());
		assertEquals("application/json", request.getHeader(HttpHeaders.ACCEPT));
	}

	@Test
	public void shouldPostPojoAsJson() throws Exception {

		HttpUrl baseUrl = server.url("/pojo/capitalize");
		this.server.enqueue(new MockResponse()
				.setHeader("Content-Type", "application/json")
				.setBody("{\"bar\":\"BARBAR\",\"foo\":\"FOOFOO\"}"));

		Pojo spring = new Pojo("foofoo", "barbar");
		Single<Pojo> result = this.webClient
				.perform(post(baseUrl.toString())
						.body(spring)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.extract(body(Pojo.class));

		TestSubscriber<Pojo> ts = new TestSubscriber<Pojo>();
		result.subscribe(ts);
		ts.awaitTerminalEvent(2, TimeUnit.SECONDS);

		assertThat(ts.getOnNextEvents().get(0).getBar(), Matchers.is("BARBAR"));
		ts.assertValueCount(1);
		ts.assertCompleted();

		RecordedRequest request = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("/pojo/capitalize", request.getPath());
		assertEquals("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}", request.getBody().readUtf8());
		assertEquals("chunked", request.getHeader(HttpHeaders.TRANSFER_ENCODING));
		assertEquals("application/json", request.getHeader(HttpHeaders.ACCEPT));
		assertEquals("application/json", request.getHeader(HttpHeaders.CONTENT_TYPE));
	}

	@Test
	public void shouldSendCookieHeader() throws Exception {
		HttpUrl baseUrl = server.url("/test");
		this.server.enqueue(new MockResponse()
				.setHeader("Content-Type", "text/plain").setBody("test"));

		Single<String> result = this.webClient
				.perform(get(baseUrl.toString())
						.cookie("testkey", "testvalue"))
				.extract(body(String.class));

		TestSubscriber<String> ts = new TestSubscriber<String>();
		result.subscribe(ts);
		ts.awaitTerminalEvent(2, TimeUnit.SECONDS);

		String response = ts.getOnNextEvents().get(0);
		assertEquals("test", response);
		ts.assertValueCount(1);
		ts.assertCompleted();

		RecordedRequest request = server.takeRequest();
		assertEquals(1, server.getRequestCount());
		assertEquals("/test", request.getPath());
		assertEquals("testkey=testvalue", request.getHeader(HttpHeaders.COOKIE));
	}

	@Test
	@Ignore
	public void shouldGetErrorWhen404() throws Exception {

		HttpUrl baseUrl = server.url("/greeting?name=Spring");
		this.server.enqueue(new MockResponse().setResponseCode(404));

		Single<String> result = this.webClient
				.perform(get(baseUrl.toString()))
				.extract(body(String.class));

		// TODO: error message should be converted to a ClientException
		TestSubscriber<String> ts = new TestSubscriber<String>();
		result.subscribe(ts);
		ts.awaitTerminalEvent(2, TimeUnit.SECONDS);

		ts.assertError(WebClientException.class);

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
