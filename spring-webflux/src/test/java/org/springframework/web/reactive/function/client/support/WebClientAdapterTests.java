/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.web.reactive.function.client.support;


import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.testfixture.servlet.MockMultipartFile;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Integration tests for {@link HttpServiceProxyFactory HTTP Service proxy}
 * with {@link WebClientAdapter} connecting to {@link MockWebServer}.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 */
class WebClientAdapterTests {

	private static final String ANOTHER_SERVER_RESPONSE_BODY = "Hello Spring 2!";

	private MockWebServer server;

	private MockWebServer anotherServer;


	@BeforeEach
	void setUp() {
		this.server = new MockWebServer();
		this.anotherServer = anotherServer();
	}

	@SuppressWarnings("ConstantConditions")
	@AfterEach
	void shutdown() throws IOException {
		if (this.server != null) {
			this.server.shutdown();
		}

		if (this.anotherServer != null) {
			this.anotherServer.shutdown();
		}
	}


	@Test
	void greeting() {
		prepareResponse(response -> response.setHeader("Content-Type", "text/plain").setBody("Hello Spring!"));

		StepVerifier.create(initService().getGreeting())
				.expectNext("Hello Spring!")
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	void greetingWithRequestAttribute() {
		Map<String, Object> attributes = new HashMap<>();

		WebClient webClient = WebClient.builder()
				.baseUrl(this.server.url("/").toString())
				.filter((request, next) -> {
					attributes.putAll(request.attributes());
					return next.exchange(request);
				})
				.build();

		prepareResponse(response -> response.setHeader("Content-Type", "text/plain").setBody("Hello Spring!"));

		StepVerifier.create(initService(webClient).getGreetingWithAttribute("myAttributeValue"))
				.expectNext("Hello Spring!")
				.expectComplete()
				.verify(Duration.ofSeconds(5));

		assertThat(attributes).containsEntry("myAttribute", "myAttributeValue");
	}

	@Test // gh-29624
	void uri() throws Exception {
		String expectedBody = "hello";
		prepareResponse(response -> response.setResponseCode(200).setBody(expectedBody));

		URI dynamicUri = this.server.url("/greeting/123").uri();
		String actualBody = initService().getGreetingById(dynamicUri, "456");

		assertThat(actualBody).isEqualTo(expectedBody);
		assertThat(this.server.takeRequest().getRequestUrl().uri()).isEqualTo(dynamicUri);
	}

	@Test
	void formData() throws Exception {
		prepareResponse(response -> response.setResponseCode(201));

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("param1", "value 1");
		map.add("param2", "value 2");

		initService().postForm(map);

		RecordedRequest request = this.server.takeRequest();
		assertThat(request.getHeaders().get("Content-Type")).isEqualTo("application/x-www-form-urlencoded");
		assertThat(request.getBody().readUtf8()).isEqualTo("param1=value+1&param2=value+2");
	}

	@Test // gh-30342
	void multipart() throws InterruptedException {
		prepareResponse(response -> response.setResponseCode(201));
		String fileName = "testFileName";
		String originalFileName = "originalTestFileName";
		MultipartFile file = new MockMultipartFile(
				fileName, originalFileName, MediaType.APPLICATION_JSON_VALUE, "test".getBytes());

		initService().postMultipart(file, "test2");

		RecordedRequest request = this.server.takeRequest();
		assertThat(request.getHeaders().get("Content-Type")).startsWith("multipart/form-data;boundary=");
		assertThat(request.getBody().readUtf8())
				.containsSubsequence("Content-Disposition: form-data; name=\"file\"; filename=\"originalTestFileName\"",
						"Content-Type: application/json", "Content-Length: 4", "test",
						"Content-Disposition: form-data; name=\"anotherPart\"",
						"Content-Type: text/plain;charset=UTF-8", "Content-Length: 5", "test2");
	}

	@Test // gh-34793
	void postSet() throws InterruptedException {
		prepareResponse(response -> response.setResponseCode(201));

		Set<Person> persons = new LinkedHashSet<>();
		persons.add(new Person("John"));
		persons.add(new Person("Richard"));

		initService().postPersonSet(persons);

		RecordedRequest request = server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getPath()).isEqualTo("/persons");
		assertThat(request.getBody().readUtf8()).isEqualTo("[{\"name\":\"John\"},{\"name\":\"Richard\"}]");
	}

	@Test
	void uriBuilderFactory() throws Exception {
		String ignoredResponseBody = "hello";
		prepareResponse(response -> response.setResponseCode(200).setBody(ignoredResponseBody));
		UriBuilderFactory factory = new DefaultUriBuilderFactory(this.anotherServer.url("/").toString());

		String actualBody = initService().getWithUriBuilderFactory(factory);

		assertThat(actualBody).isEqualTo(ANOTHER_SERVER_RESPONSE_BODY);
		assertThat(this.anotherServer.takeRequest().getPath()).isEqualTo("/greeting");
		assertThat(this.server.getRequestCount()).isEqualTo(0);
	}

	@Test
	void uriBuilderFactoryWithPathVariableAndRequestParam() throws Exception {
		String ignoredResponseBody = "hello";
		prepareResponse(response -> response.setResponseCode(200).setBody(ignoredResponseBody));
		UriBuilderFactory factory = new DefaultUriBuilderFactory(this.anotherServer.url("/").toString());

		String actualBody = initService().getWithUriBuilderFactory(factory, "123", "test");

		assertThat(actualBody).isEqualTo(ANOTHER_SERVER_RESPONSE_BODY);
		assertThat(this.anotherServer.takeRequest().getPath()).isEqualTo("/greeting/123?param=test");
		assertThat(this.server.getRequestCount()).isEqualTo(0);
	}

	@Test
	void ignoredUriBuilderFactory() throws Exception {
		String expectedResponseBody = "hello";
		prepareResponse(response -> response.setResponseCode(200).setBody(expectedResponseBody));
		URI dynamicUri = this.server.url("/greeting/123").uri();
		UriBuilderFactory factory = new DefaultUriBuilderFactory(this.anotherServer.url("/").toString());

		String actualBody = initService().getWithIgnoredUriBuilderFactory(dynamicUri, factory);

		assertThat(actualBody).isEqualTo(expectedResponseBody);
		assertThat(this.server.takeRequest().getRequestUrl().uri()).isEqualTo(dynamicUri);
		assertThat(this.anotherServer.getRequestCount()).isEqualTo(0);
	}


	private static MockWebServer anotherServer() {
		MockWebServer anotherServer = new MockWebServer();
		MockResponse response = new MockResponse();
		response.setHeader("Content-Type", "text/plain").setBody(ANOTHER_SERVER_RESPONSE_BODY);
		anotherServer.enqueue(response);
		return anotherServer;
	}

	private Service initService() {
		WebClient webClient = WebClient.builder().baseUrl(this.server.url("/").toString()).build();
		return initService(webClient);
	}

	private Service initService(WebClient webClient) {
		WebClientAdapter adapter = WebClientAdapter.create(webClient);
		return HttpServiceProxyFactory.builderFor(adapter).build().createClient(Service.class);
	}

	private void prepareResponse(Consumer<MockResponse> consumer) {
		MockResponse response = new MockResponse();
		consumer.accept(response);
		this.server.enqueue(response);
	}


	private interface Service {

		@GetExchange("/greeting")
		Mono<String> getGreeting();

		@GetExchange("/greeting")
		Mono<String> getGreetingWithAttribute(@RequestAttribute String myAttribute);

		@GetExchange("/greetings/{id}")
		String getGreetingById(@Nullable URI uri, @PathVariable String id);

		@PostExchange(contentType = "application/x-www-form-urlencoded")
		void postForm(@RequestParam MultiValueMap<String, String> params);

		@PostExchange
		void postMultipart(MultipartFile file, @RequestPart String anotherPart);

		@PostExchange("/persons")
		void postPersonSet(@RequestBody Set<Person> set);

		@GetExchange("/greeting")
		String getWithUriBuilderFactory(UriBuilderFactory uriBuilderFactory);

		@GetExchange("/greeting/{id}")
		String getWithUriBuilderFactory(
				UriBuilderFactory uriBuilderFactory, @PathVariable String id, @RequestParam String param);

		@GetExchange("/greeting")
		String getWithIgnoredUriBuilderFactory(URI uri, UriBuilderFactory uriBuilderFactory);
	}


	static final class Person {

		private final String name;

		Person(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

	}

}
