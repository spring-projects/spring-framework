/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.client.support;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.testfixture.servlet.MockMultipartFile;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link HttpServiceProxyFactory} with {@link RestClient}
 * and {@link RestTemplate} connecting to {@link MockWebServer}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
@SuppressWarnings("JUnitMalformedDeclaration")
class RestClientAdapterTests {

	private final MockWebServer anotherServer = anotherServer();


	@SuppressWarnings("ConstantValue")
	@AfterEach
	void shutdown() throws IOException {
		if (this.anotherServer != null) {
			this.anotherServer.shutdown();
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@ParameterizedTest
	@MethodSource("arguments")
	@interface ParameterizedAdapterTest {
	}

	public static Stream<Object[]> arguments() {
		return Stream.of(
				args((url, observationRegistry) -> {
					RestClient restClient = RestClient.builder().baseUrl(url).observationRegistry(observationRegistry).build();
					return RestClientAdapter.create(restClient);
				}),
				args((url, observationRegistry) -> {
					RestTemplate restTemplate = new RestTemplate();
					restTemplate.setObservationRegistry(observationRegistry);
					restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(url));
					return RestTemplateAdapter.create(restTemplate);
				}));
	}

	@SuppressWarnings("resource")
	private static Object[] args(BiFunction<String, TestObservationRegistry, HttpExchangeAdapter> adapterFactory) {
		MockWebServer server = new MockWebServer();

		MockResponse response = new MockResponse();
		response.setHeader("Content-Type", "text/plain").setBody("Hello Spring!");
		server.enqueue(response);

		TestObservationRegistry observationRegistry = TestObservationRegistry.create();

		HttpExchangeAdapter adapter = adapterFactory.apply(server.url("/").toString(), observationRegistry);
		Service service = HttpServiceProxyFactory.builderFor(adapter).build().createClient(Service.class);

		return new Object[] { server, service, observationRegistry };
	}


	@ParameterizedAdapterTest
	void greeting(
			MockWebServer server, Service service, TestObservationRegistry observationRegistry) throws Exception {

		String response = service.getGreeting();

		RecordedRequest request = server.takeRequest();
		assertThat(response).isEqualTo("Hello Spring!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/greeting");
		TestObservationRegistryAssert.assertThat(observationRegistry)
				.hasObservationWithNameEqualTo("http.client.requests")
				.that().hasLowCardinalityKeyValue("uri", "/greeting");
	}

	@ParameterizedAdapterTest
	void greetingById(
			MockWebServer server, Service service, TestObservationRegistry observationRegistry) throws Exception {

		ResponseEntity<String> response = service.getGreetingById("456");

		RecordedRequest request = server.takeRequest();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo("Hello Spring!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/greeting/456");
		TestObservationRegistryAssert.assertThat(observationRegistry)
				.hasObservationWithNameEqualTo("http.client.requests")
				.that().hasLowCardinalityKeyValue("uri", "/greeting/{id}");
	}

	@ParameterizedAdapterTest
	void greetingWithDynamicUri(
			MockWebServer server, Service service, TestObservationRegistry observationRegistry) throws Exception {

		URI dynamicUri = server.url("/greeting/123").uri();
		Optional<String> response = service.getGreetingWithDynamicUri(dynamicUri, "456");

		RecordedRequest request = server.takeRequest();
		assertThat(response.orElse("empty")).isEqualTo("Hello Spring!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getRequestUrl().uri()).isEqualTo(dynamicUri);
		TestObservationRegistryAssert.assertThat(observationRegistry)
				.hasObservationWithNameEqualTo("http.client.requests")
				.that().hasLowCardinalityKeyValue("uri", "none");
	}

	@ParameterizedAdapterTest
	void postWithHeader(MockWebServer server, Service service) throws Exception {
		service.postWithHeader("testHeader", "testBody");

		RecordedRequest request = server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getPath()).isEqualTo("/greeting");
		assertThat(request.getHeaders().get("testHeaderName")).isEqualTo("testHeader");
		assertThat(request.getBody().readUtf8()).isEqualTo("testBody");
	}

	@ParameterizedAdapterTest
	void formData(MockWebServer server, Service service) throws Exception {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("param1", "value 1");
		map.add("param2", "value 2");

		service.postForm(map);

		RecordedRequest request = server.takeRequest();
		assertThat(request.getHeaders().get("Content-Type")).isEqualTo("application/x-www-form-urlencoded");
		assertThat(request.getBody().readUtf8()).isEqualTo("param1=value+1&param2=value+2");
	}

	@ParameterizedAdapterTest // gh-30342
	void multipart(MockWebServer server, Service service) throws Exception {
		MultipartFile file = new MockMultipartFile(
				"testFileName", "originalTestFileName", MediaType.APPLICATION_JSON_VALUE, "test".getBytes());

		service.postMultipart(file, "test2");

		RecordedRequest request = server.takeRequest();
		assertThat(request.getHeaders().get("Content-Type")).startsWith("multipart/form-data;boundary=");
		assertThat(request.getBody().readUtf8()).containsSubsequence(
				"Content-Disposition: form-data; name=\"file\"; filename=\"originalTestFileName\"",
				"Content-Type: application/json", "Content-Length: 4", "test",
				"Content-Disposition: form-data; name=\"anotherPart\"", "Content-Type: text/plain;charset=UTF-8",
				"Content-Length: 5", "test2");
	}

	@ParameterizedAdapterTest
	void putWithCookies(MockWebServer server, Service service) throws Exception {
		service.putWithCookies("test1", "test2");

		RecordedRequest request = server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("PUT");
		assertThat(request.getHeader("Cookie")).isEqualTo("firstCookie=test1; secondCookie=test2");
	}

	@ParameterizedAdapterTest
	void putWithSameNameCookies(MockWebServer server, Service service) throws Exception {
		service.putWithSameNameCookies("test1", "test2");

		RecordedRequest request = server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("PUT");
		assertThat(request.getHeader("Cookie")).isEqualTo("testCookie=test1; testCookie=test2");
	}

	@ParameterizedAdapterTest
	void getWithUriBuilderFactory(MockWebServer server, Service service) throws InterruptedException {
		String url = this.anotherServer.url("/").toString();
		UriBuilderFactory factory = new DefaultUriBuilderFactory(url);

		ResponseEntity<String> actualResponse = service.getWithUriBuilderFactory(factory);

		RecordedRequest request = this.anotherServer.takeRequest();
		assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(actualResponse.getBody()).isEqualTo("Hello Spring 2!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/greeting");
		assertThat(server.getRequestCount()).isEqualTo(0);
	}

	@ParameterizedAdapterTest
	void getWithFactoryPathVariableAndRequestParam(MockWebServer server, Service service) throws InterruptedException {
		String url = this.anotherServer.url("/").toString();
		UriBuilderFactory factory = new DefaultUriBuilderFactory(url);

		ResponseEntity<String> actualResponse = service.getWithUriBuilderFactory(factory, "123", "test");

		RecordedRequest request = this.anotherServer.takeRequest();
		assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(actualResponse.getBody()).isEqualTo("Hello Spring 2!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/greeting/123?param=test");
		assertThat(server.getRequestCount()).isEqualTo(0);
	}

	@ParameterizedAdapterTest
	void getWithIgnoredUriBuilderFactory(MockWebServer server, Service service) throws InterruptedException {
		URI dynamicUri = server.url("/greeting/123").uri();
		UriBuilderFactory factory = new DefaultUriBuilderFactory(this.anotherServer.url("/").toString());

		ResponseEntity<String> actualResponse = service.getWithIgnoredUriBuilderFactory(dynamicUri, factory);

		RecordedRequest request = server.takeRequest();
		assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(actualResponse.getBody()).isEqualTo("Hello Spring!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/greeting/123");
		assertThat(this.anotherServer.getRequestCount()).isEqualTo(0);
	}


	private static MockWebServer anotherServer() {
		MockWebServer server = new MockWebServer();
		MockResponse response = new MockResponse();
		response.setHeader("Content-Type", "text/plain").setBody("Hello Spring 2!");
		server.enqueue(response);
		return server;
	}


	private interface Service {

		@GetExchange("/greeting")
		String getGreeting();

		@GetExchange("/greeting/{id}")
		ResponseEntity<String> getGreetingById(@PathVariable String id);

		@GetExchange("/greeting/{id}")
		Optional<String> getGreetingWithDynamicUri(@Nullable URI uri, @PathVariable String id);

		@PostExchange("/greeting")
		void postWithHeader(@RequestHeader("testHeaderName") String testHeader, @RequestBody String requestBody);

		@PostExchange(contentType = "application/x-www-form-urlencoded")
		void postForm(@RequestParam MultiValueMap<String, String> params);

		@PostExchange
		void postMultipart(MultipartFile file, @RequestPart String anotherPart);

		@PutExchange
		void putWithCookies(@CookieValue String firstCookie, @CookieValue String secondCookie);

		@PutExchange
		void putWithSameNameCookies(
				@CookieValue("testCookie") String firstCookie, @CookieValue("testCookie") String secondCookie);

		@GetExchange("/greeting")
		ResponseEntity<String> getWithUriBuilderFactory(UriBuilderFactory uriBuilderFactory);

		@GetExchange("/greeting/{id}")
		ResponseEntity<String> getWithUriBuilderFactory(UriBuilderFactory uriBuilderFactory,
				@PathVariable String id, @RequestParam String param);

		@GetExchange("/greeting")
		ResponseEntity<String> getWithIgnoredUriBuilderFactory(URI uri, UriBuilderFactory uriBuilderFactory);
	}

}
