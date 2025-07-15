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

package org.springframework.web.client.support;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import io.micrometer.observation.tck.TestObservationRegistry;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.client.ApiVersionInserter;
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

	private final MockWebServer anotherServer = new MockWebServer();

	@BeforeEach
	void setUp() throws IOException {
		this.anotherServer.start();
	}

	@AfterEach
	void shutdown() {
		this.anotherServer.close();
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@ParameterizedTest
	@MethodSource("arguments")
	@interface ParameterizedAdapterTest {
	}

	public static Stream<Object[]> arguments() throws IOException {
		return Stream.of(
				createArgsForAdapter((url, or) -> {
					RestClient restClient = RestClient.builder().baseUrl(url).observationRegistry(or).build();
					return RestClientAdapter.create(restClient);
				}),
				createArgsForAdapter((url, or) -> {
					RestTemplate restTemplate = new RestTemplate();
					restTemplate.setObservationRegistry(or);
					restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(url));
					return RestTemplateAdapter.create(restTemplate);
				}));
	}

	@SuppressWarnings("resource")
	private static Object[] createArgsForAdapter(
			BiFunction<String, TestObservationRegistry, HttpExchangeAdapter> adapterFactory) throws IOException {

		MockWebServer server = new MockWebServer();
		server.start();

		MockResponse response = new MockResponse.Builder()
				.setHeader("Content-Type", "text/plain")
				.body("Hello Spring!")
				.build();
		server.enqueue(response);

		TestObservationRegistry registry = TestObservationRegistry.create();

		HttpExchangeAdapter adapter = adapterFactory.apply(server.url("/").toString(), registry);
		Service service = HttpServiceProxyFactory.builderFor(adapter).build().createClient(Service.class);

		return new Object[] { server, service, registry };
	}


	@ParameterizedAdapterTest
	void greeting(MockWebServer server, Service service, TestObservationRegistry registry) throws Exception {

		String response = service.getGreeting();

		RecordedRequest request = server.takeRequest();
		assertThat(response).isEqualTo("Hello Spring!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getTarget()).isEqualTo("/greeting");
		assertThat(registry).hasObservationWithNameEqualTo("http.client.requests").that()
				.hasLowCardinalityKeyValue("uri", "/greeting");
	}

	@ParameterizedAdapterTest
	void greetingById(MockWebServer server, Service service, TestObservationRegistry registry) throws Exception {

		ResponseEntity<String> response = service.getGreetingById("456");

		RecordedRequest request = server.takeRequest();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo("Hello Spring!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getTarget()).isEqualTo("/greeting/456");
		assertThat(registry).hasObservationWithNameEqualTo("http.client.requests").that()
				.hasLowCardinalityKeyValue("uri", "/greeting/{id}");
	}

	@ParameterizedAdapterTest
	void greetingWithDynamicUri(MockWebServer server, Service service, TestObservationRegistry registry) throws Exception {

		URI dynamicUri = server.url("/greeting/123").uri();
		Optional<String> response = service.getGreetingWithDynamicUri(dynamicUri, "456");

		RecordedRequest request = server.takeRequest();
		assertThat(response.orElse("empty")).isEqualTo("Hello Spring!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getUrl().uri()).isEqualTo(dynamicUri);
		assertThat(registry).hasObservationWithNameEqualTo("http.client.requests").that()
				.hasLowCardinalityKeyValue("uri", "none");
	}

	@Test
	void greetingWithApiVersion() throws Exception {
		prepareResponse(builder ->
				builder.setHeader("Content-Type", "text/plain").body("Hello Spring 2!"));

		RestClient restClient = RestClient.builder()
				.baseUrl(anotherServer.url("/").toString())
				.apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
				.build();

		RestClientAdapter adapter = RestClientAdapter.create(restClient);
		Service service = HttpServiceProxyFactory.builderFor(adapter).build().createClient(Service.class);

		String actualResponse = service.getGreetingWithVersion();

		RecordedRequest request = anotherServer.takeRequest();
		assertThat(request.getHeaders().get("X-API-Version")).isEqualTo("1.2");
		assertThat(actualResponse).isEqualTo("Hello Spring 2!");
	}

	@ParameterizedAdapterTest
	void getWithUriBuilderFactory(MockWebServer server, Service service) throws InterruptedException {
		prepareResponse(builder ->
				builder.setHeader("Content-Type", "text/plain").body("Hello Spring 2!"));

		String url = this.anotherServer.url("/").toString();
		UriBuilderFactory factory = new DefaultUriBuilderFactory(url);

		ResponseEntity<String> actualResponse = service.getWithUriBuilderFactory(factory);

		RecordedRequest request = this.anotherServer.takeRequest();
		assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(actualResponse.getBody()).isEqualTo("Hello Spring 2!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getTarget()).isEqualTo("/greeting");
		assertThat(server.getRequestCount()).isEqualTo(0);
	}

	@ParameterizedAdapterTest
	void getWithFactoryPathVariableAndRequestParam(MockWebServer server, Service service) throws InterruptedException {
		prepareResponse(builder ->
				builder.setHeader("Content-Type", "text/plain").body("Hello Spring 2!"));

		String url = this.anotherServer.url("/").toString();
		UriBuilderFactory factory = new DefaultUriBuilderFactory(url);

		ResponseEntity<String> actualResponse = service.getWithUriBuilderFactory(factory, "123", "test");

		RecordedRequest request = this.anotherServer.takeRequest();
		assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(actualResponse.getBody()).isEqualTo("Hello Spring 2!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getTarget()).isEqualTo("/greeting/123?param=test");
		assertThat(server.getRequestCount()).isEqualTo(0);
	}

	@ParameterizedAdapterTest
	void getWithIgnoredUriBuilderFactory(MockWebServer server, Service service) throws InterruptedException {
		prepareResponse(builder ->
				builder.setHeader("Content-Type", "text/plain").body("Hello Spring 2!"));

		URI dynamicUri = server.url("/greeting/123").uri();
		UriBuilderFactory factory = new DefaultUriBuilderFactory(this.anotherServer.url("/").toString());

		ResponseEntity<String> actualResponse = service.getWithIgnoredUriBuilderFactory(dynamicUri, factory);

		RecordedRequest request = server.takeRequest();
		assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(actualResponse.getBody()).isEqualTo("Hello Spring!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getTarget()).isEqualTo("/greeting/123");
		assertThat(this.anotherServer.getRequestCount()).isEqualTo(0);
	}

	@ParameterizedAdapterTest
	void postWithHeader(MockWebServer server, Service service) throws Exception {
		service.postWithHeader("testHeader", "testBody");

		RecordedRequest request = server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getTarget()).isEqualTo("/greeting");
		assertThat(request.getHeaders().get("testHeaderName")).isEqualTo("testHeader");
		assertThat(request.getBody().utf8()).isEqualTo("testBody");
	}

	@ParameterizedAdapterTest
	void postFormData(MockWebServer server, Service service) throws Exception {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("param1", "value 1");
		map.add("param2", "value 2");

		service.postForm(map);

		RecordedRequest request = server.takeRequest();
		assertThat(request.getHeaders().get("Content-Type")).isEqualTo("application/x-www-form-urlencoded");
		assertThat(request.getBody().utf8()).isEqualTo("param1=value+1&param2=value+2");
	}

	@ParameterizedAdapterTest // gh-30342
	void postMultipart(MockWebServer server, Service service) throws Exception {
		MultipartFile file = new MockMultipartFile(
				"testFileName", "originalTestFileName", MediaType.APPLICATION_JSON_VALUE, "test".getBytes());

		service.postMultipart(file, "test2");

		RecordedRequest request = server.takeRequest();
		assertThat(request.getHeaders().get("Content-Type")).startsWith("multipart/form-data;boundary=");
		assertThat(request.getBody().utf8()).containsSubsequence(
				"Content-Disposition: form-data; name=\"file\"; filename=\"originalTestFileName\"",
				"Content-Type: application/json", "Content-Length: 4", "test",
				"Content-Disposition: form-data; name=\"anotherPart\"", "Content-Type: text/plain;charset=UTF-8",
				"Content-Length: 5", "test2");
	}

	@ParameterizedAdapterTest // gh-34793
	void postSet(MockWebServer server, Service service) throws InterruptedException {
		Set<Person> persons = new LinkedHashSet<>();
		persons.add(new Person("John"));
		persons.add(new Person("Richard"));
		service.postPersonSet(persons);

		RecordedRequest request = server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getTarget()).isEqualTo("/persons");
		assertThat(request.getBody().utf8()).isEqualTo("[{\"name\":\"John\"},{\"name\":\"Richard\"}]");
	}

	@ParameterizedAdapterTest
	void putWithCookies(MockWebServer server, Service service) throws Exception {
		service.putWithCookies("test1", "test2");

		RecordedRequest request = server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("PUT");
		assertThat(request.getHeaders().get("Cookie")).isEqualTo("firstCookie=test1; secondCookie=test2");
	}

	@ParameterizedAdapterTest
	void putWithSameNameCookies(MockWebServer server, Service service) throws Exception {
		service.putWithSameNameCookies("test1", "test2");

		RecordedRequest request = server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("PUT");
		assertThat(request.getHeaders().get("Cookie")).isEqualTo("testCookie=test1; testCookie=test2");
	}

	@Test
	void getInputStream() throws Exception {
		prepareResponse(builder ->
				builder.setHeader("Content-Type", "text/plain").body("Hello Spring 2!"));

		InputStream inputStream = initService().getInputStream();

		RecordedRequest request = this.anotherServer.takeRequest();
		assertThat(request.getTarget()).isEqualTo("/input-stream");
		assertThat(StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8)).isEqualTo("Hello Spring 2!");
	}

	@Test
	void postOutputStream() throws Exception {
		prepareResponse(builder ->
				builder.setHeader("Content-Type", "text/plain").body("Hello Spring 2!"));

		String body = "test stream";
		initService().postOutputStream(outputStream -> outputStream.write(body.getBytes()));

		RecordedRequest request = this.anotherServer.takeRequest();
		assertThat(request.getTarget()).isEqualTo("/output-stream");
		assertThat(request.getBody().utf8()).isEqualTo(body);
	}

	@Test
	void handleNotFoundException() {
		MockResponse response = new MockResponse.Builder().code(404).build();
		this.anotherServer.enqueue(response);

		RestClientAdapter clientAdapter = RestClientAdapter.create(
				RestClient.builder().baseUrl(this.anotherServer.url("/").toString()).build());

		HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(clientAdapter)
				.exchangeAdapterDecorator(NotFoundRestClientAdapterDecorator::new)
				.build();

		ResponseEntity<String> responseEntity = factory.createClient(Service.class).getGreetingById("1");

		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(responseEntity.getBody()).isNull();
	}

	private Service initService() {
		String url = this.anotherServer.url("/").toString();
		RestClient restClient = RestClient.builder().baseUrl(url).build();
		RestClientAdapter adapter = RestClientAdapter.create(restClient);
		return HttpServiceProxyFactory.builderFor(adapter).build().createClient(Service.class);
	}

	private void prepareResponse(Function<MockResponse.Builder, MockResponse.Builder> f) {
		MockResponse.Builder builder = new MockResponse.Builder();
		this.anotherServer.enqueue(f.apply(builder).build());
	}


	private interface Service {

		@GetExchange("/greeting")
		String getGreeting();

		@GetExchange("/greeting/{id}")
		ResponseEntity<String> getGreetingById(@PathVariable String id);

		@GetExchange("/greeting/{id}")
		Optional<String> getGreetingWithDynamicUri(@Nullable URI uri, @PathVariable String id);

		@GetExchange(url = "/greeting", version = "1.2")
		String getGreetingWithVersion();

		@GetExchange("/greeting")
		ResponseEntity<String> getWithUriBuilderFactory(UriBuilderFactory uriBuilderFactory);

		@GetExchange("/greeting/{id}")
		ResponseEntity<String> getWithUriBuilderFactory(
				UriBuilderFactory uriBuilderFactory, @PathVariable String id, @RequestParam String param);

		@GetExchange("/greeting")
		ResponseEntity<String> getWithIgnoredUriBuilderFactory(URI uri, UriBuilderFactory uriBuilderFactory);

		@PostExchange("/greeting")
		void postWithHeader(@RequestHeader("testHeaderName") String testHeader, @RequestBody String requestBody);

		@PostExchange(contentType = "application/x-www-form-urlencoded")
		void postForm(@RequestParam MultiValueMap<String, String> params);

		@PostExchange
		void postMultipart(MultipartFile file, @RequestPart String anotherPart);

		@PostExchange(url = "/persons", contentType = MediaType.APPLICATION_JSON_VALUE)
		void postPersonSet(@RequestBody Set<Person> set);

		@PutExchange
		void putWithCookies(@CookieValue String firstCookie, @CookieValue String secondCookie);

		@PutExchange
		void putWithSameNameCookies(
				@CookieValue("testCookie") String firstCookie, @CookieValue("testCookie") String secondCookie);

		@GetExchange(url = "/input-stream")
		InputStream getInputStream();

		@PostExchange(url = "/output-stream")
		void postOutputStream(StreamingHttpOutputMessage.Body body);

	}


	record Person(String name) {
	}

}
