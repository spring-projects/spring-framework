/*
 * Copyright 2002-2022 the original author or authors.
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
import java.util.Map;
import java.util.function.Consumer;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Integration tests for {@link HttpServiceProxyFactory HTTP Service proxy}
 * using {@link WebClient} and {@link MockWebServer}.
 *
 * @author Rossen Stoyanchev
 */
public class WebClientHttpServiceProxyTests {

	private MockWebServer server;


	@BeforeEach
	void setUp() {
		this.server = new MockWebServer();
	}

	@SuppressWarnings("ConstantConditions")
	@AfterEach
	void shutdown() throws IOException {
		if (this.server != null) {
			this.server.shutdown();
		}
	}


	@Test
	void greeting() {
		prepareResponse(response ->
				response.setHeader("Content-Type", "text/plain").setBody("Hello Spring!"));

		StepVerifier.create(initHttpService().getGreeting())
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

		prepareResponse(response ->
				response.setHeader("Content-Type", "text/plain").setBody("Hello Spring!"));

		StepVerifier.create(initHttpService(webClient).getGreetingWithAttribute("myAttributeValue"))
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
		String actualBody = initHttpService().getGreetingById(dynamicUri, "456");

		assertThat(actualBody).isEqualTo(expectedBody);
		assertThat(this.server.takeRequest().getRequestUrl().uri()).isEqualTo(dynamicUri);
	}

	@Test
	void formData() throws Exception {
		prepareResponse(response -> response.setResponseCode(201));

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("param1", "value 1");
		map.add("param2", "value 2");

		initHttpService().postForm(map);

		RecordedRequest request = this.server.takeRequest();
		assertThat(request.getHeaders().get("Content-Type")).isEqualTo("application/x-www-form-urlencoded;charset=UTF-8");
		assertThat(request.getBody().readUtf8()).isEqualTo("param1=value+1&param2=value+2");
	}

	private TestHttpService initHttpService() {
		WebClient webClient = WebClient.builder().baseUrl(this.server.url("/").toString()).build();
		return initHttpService(webClient);
	}

	private TestHttpService initHttpService(WebClient webClient) {
		return HttpServiceProxyFactory.builder()
				.clientAdapter(WebClientAdapter.forClient(webClient))
				.build()
				.createClient(TestHttpService.class);
	}

	private void prepareResponse(Consumer<MockResponse> consumer) {
		MockResponse response = new MockResponse();
		consumer.accept(response);
		this.server.enqueue(response);
	}


	private interface TestHttpService {

		@GetExchange("/greeting")
		Mono<String> getGreeting();

		@GetExchange("/greeting")
		Mono<String> getGreetingWithAttribute(@RequestAttribute String myAttribute);

		@GetExchange("/greetings/{id}")
		String getGreetingById(@Nullable URI uri, @PathVariable String id);

		@PostExchange(contentType = "application/x-www-form-urlencoded")
		void postForm(@RequestParam MultiValueMap<String, String> params);

	}

}
