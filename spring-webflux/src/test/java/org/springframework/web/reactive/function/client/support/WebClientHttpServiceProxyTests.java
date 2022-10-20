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
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.annotation.GetExchange;
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
	void greeting() throws Exception {

		prepareResponse(response ->
				response.setHeader("Content-Type", "text/plain").setBody("Hello Spring!"));

		StepVerifier.create(initHttpService().getGreeting())
				.expectNext("Hello Spring!")
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	void greetingWithRequestAttribute() throws Exception {

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

	private TestHttpService initHttpService() throws Exception {
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

	}


}
