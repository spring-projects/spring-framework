/*
 * Copyright 2002-2023 the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.util.Optional;
import java.util.stream.Stream;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.cglib.core.internal.Function;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link HttpServiceProxyFactory} with {@link RestClient}
 * and {@link RestTemplate} connecting to {@link MockWebServer}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("JUnitMalformedDeclaration")
class RestClientAdapterTests {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@ParameterizedTest
	@MethodSource("arguments")
	@interface ParameterizedAdapterTest {
	}

	public static Stream<Object[]> arguments() {
		return Stream.of(
				args(url -> {
					RestClient restClient = RestClient.builder().baseUrl(url).build();
					return RestClientAdapter.create(restClient);
				}),
				args(url -> {
					RestTemplate restTemplate = new RestTemplate();
					restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(url));
					return RestTemplateAdapter.create(restTemplate);
				}));
	}

	@SuppressWarnings("resource")
	private static Object[] args(Function<String, HttpExchangeAdapter> adapterFactory) {
		MockWebServer server = new MockWebServer();

		MockResponse response = new MockResponse();
		response.setHeader("Content-Type", "text/plain").setBody("Hello Spring!");
		server.enqueue(response);

		HttpExchangeAdapter adapter = adapterFactory.apply(server.url("/").toString());
		Service service = HttpServiceProxyFactory.builderFor(adapter).build().createClient(Service.class);

		return new Object[] { server, service };
	}


	@ParameterizedAdapterTest
	void greeting(MockWebServer server, Service service) throws Exception {
		String response = service.getGreeting();

		RecordedRequest request = server.takeRequest();
		assertThat(response).isEqualTo("Hello Spring!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/greeting");
	}

	@ParameterizedAdapterTest
	void greetingById(MockWebServer server, Service service) throws Exception {
		ResponseEntity<String> response = service.getGreetingById("456");

		RecordedRequest request = server.takeRequest();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo("Hello Spring!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/greeting/456");
	}

	@ParameterizedAdapterTest
	void greetingWithDynamicUri(MockWebServer server, Service service) throws Exception {
		URI dynamicUri = server.url("/greeting/123").uri();
		Optional<String> response = service.getGreetingWithDynamicUri(dynamicUri, "456");

		RecordedRequest request = server.takeRequest();
		assertThat(response.orElse("empty")).isEqualTo("Hello Spring!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getRequestUrl().uri()).isEqualTo(dynamicUri);
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
		assertThat(request.getHeaders().get("Content-Type")).isEqualTo("application/x-www-form-urlencoded;charset=UTF-8");
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

	}

}
