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

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.testfixture.servlet.MockMultipartFile;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link HttpServiceProxyFactory HTTP Service proxy} using
 * {@link RestTemplate} and {@link MockWebServer}.
 *
 * @author Olga Maciaszek-Sharma
 */
class RestTemplateHttpServiceProxyTests {

	private MockWebServer server;

	private TestService testService;

	@BeforeEach
	void setUp() {
		this.server = new MockWebServer();
		prepareResponse();
		this.testService = initTestService();
	}

	private TestService initTestService() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(this.server.url("/").toString()));
		return HttpServiceProxyFactory.builder()
			.exchangeAdapter(RestTemplateAdapter.forTemplate(restTemplate))
			.build()
			.createClient(TestService.class);
	}

	@SuppressWarnings("ConstantConditions")
	@AfterEach
	void shutDown() throws IOException {
		if (this.server != null) {
			this.server.shutdown();
		}
	}

	@Test
	void getRequest() throws InterruptedException {
		String response = testService.getRequest();

		RecordedRequest request = this.server.takeRequest();
		assertThat(response).isEqualTo("Hello Spring!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/test");
	}

	@Test
	void getRequestWithPathVariable() throws InterruptedException {
		ResponseEntity<String> response = testService.getRequestWithPathVariable("456");

		RecordedRequest request = this.server.takeRequest();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo("Hello Spring!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/test/456");
	}

	@Test
	void getRequestWithDynamicUri() throws InterruptedException {
		URI dynamicUri = this.server.url("/greeting/123").uri();

		Optional<String> response = testService.getRequestWithDynamicUri(dynamicUri, "456");

		RecordedRequest request = this.server.takeRequest();
		assertThat(response.orElse("empty")).isEqualTo("Hello Spring!");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getRequestUrl().uri()).isEqualTo(dynamicUri);
	}

	@Test
	void postWithRequestHeader() throws InterruptedException {
		testService.postRequestWithHeader("testHeader", "testBody");

		RecordedRequest request = this.server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getPath()).isEqualTo("/test");
		assertThat(request.getHeaders().get("testHeaderName")).isEqualTo("testHeader");
		assertThat(request.getBody().readUtf8()).isEqualTo("testBody");
	}

	@Test
	void formData() throws Exception {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("param1", "value 1");
		map.add("param2", "value 2");

		testService.postForm(map);

		RecordedRequest request = this.server.takeRequest();
		assertThat(request.getHeaders().get("Content-Type"))
			.isEqualTo("application/x-www-form-urlencoded;charset=UTF-8");
		assertThat(request.getBody().readUtf8()).isEqualTo("param1=value+1&param2=value+2");
	}

	@Test // gh-30342
	void multipart() throws InterruptedException {
		String fileName = "testFileName";
		String originalFileName = "originalTestFileName";
		MultipartFile file = new MockMultipartFile(fileName, originalFileName, MediaType.APPLICATION_JSON_VALUE,
				"test".getBytes());

		testService.postMultipart(file, "test2");

		RecordedRequest request = this.server.takeRequest();
		assertThat(request.getHeaders().get("Content-Type")).startsWith("multipart/form-data;boundary=");
		assertThat(request.getBody().readUtf8()).containsSubsequence(
				"Content-Disposition: form-data; name=\"file\"; filename=\"originalTestFileName\"",
				"Content-Type: application/json", "Content-Length: 4", "test",
				"Content-Disposition: form-data; name=\"anotherPart\"", "Content-Type: text/plain;charset=UTF-8",
				"Content-Length: 5", "test2");
	}

	@Test
	void putRequestWithCookies() throws InterruptedException {
		testService.putRequestWithCookies("test1", "test2");

		RecordedRequest request = this.server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("PUT");
		assertThat(request.getHeader("Cookie")).isEqualTo("firstCookie=test1; secondCookie=test2");
	}

	@Test
	void putRequestWithSameNameCookies() throws InterruptedException {
		testService.putRequestWithSameNameCookies("test1", "test2");

		RecordedRequest request = this.server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("PUT");
		assertThat(request.getHeader("Cookie")).isEqualTo("testCookie=test1; testCookie=test2");
	}

	private void prepareResponse() {
		MockResponse response = new MockResponse();
		response.setHeader("Content-Type", "text/plain").setBody("Hello Spring!");
		this.server.enqueue(response);
	}

	private interface TestService {

		@GetExchange("/test")
		String getRequest();

		@GetExchange("/test/{id}")
		ResponseEntity<String> getRequestWithPathVariable(@PathVariable String id);

		@GetExchange("/test/{id}")
		Optional<String> getRequestWithDynamicUri(@Nullable URI uri, @PathVariable String id);

		@PostExchange("/test")
		void postRequestWithHeader(@RequestHeader("testHeaderName") String testHeader,
				@RequestBody String requestBody);

		@PostExchange(contentType = "application/x-www-form-urlencoded")
		void postForm(@RequestParam MultiValueMap<String, String> params);

		@PostExchange
		void postMultipart(MultipartFile file, @RequestPart String anotherPart);

		@PutExchange
		void putRequestWithCookies(@CookieValue String firstCookie,
				@CookieValue String secondCookie);

		@PutExchange
		void putRequestWithSameNameCookies(@CookieValue("testCookie") String firstCookie,
				@CookieValue("testCookie") String secondCookie);

	}

}
