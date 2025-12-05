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

package org.springframework.test.web.servlet.client;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;
import java.util.stream.Stream;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.JettyClientHttpRequestFactory;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import static org.junit.jupiter.params.provider.Arguments.argumentSet;

/**
 * Integration tests for {@link RestTestClient} against a live server.
 */
class RestTestClientIntegrationTests {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@ParameterizedTest
	@MethodSource("clientHttpRequestFactories")
	@interface ParameterizedRestClientTest {
	}

	static Stream<Arguments> clientHttpRequestFactories() {
		return Stream.of(
				argumentSet("JDK HttpURLConnection", new SimpleClientHttpRequestFactory()),
				argumentSet("Jetty", new JettyClientHttpRequestFactory()),
				argumentSet("JDK HttpClient", new JdkClientHttpRequestFactory()),
				argumentSet("Reactor Netty", new ReactorClientHttpRequestFactory()),
				argumentSet("HttpComponents", new HttpComponentsClientHttpRequestFactory())
		);
	}

	@AutoClose
	private MockWebServer server = new MockWebServer();

	private RestTestClient testClient;


	private void startServer(ClientHttpRequestFactory requestFactory) throws IOException {
		this.server.start();
		this.testClient = RestTestClient.bindToServer(requestFactory)
				.baseUrl(this.server.url("/").toString())
				.build();
	}

	@ParameterizedRestClientTest // gh-35784
	void sequentialRequestsNotConsumingBody(ClientHttpRequestFactory requestFactory) throws IOException {
		startServer(requestFactory);
		for (int i = 0; i < 10; i++) {
			prepareResponse(builder ->
					builder.setHeader("Content-Type", "text/plain").body("Hello Spring!"));
			this.testClient.get().uri("/").exchange().expectStatus().isOk();
		}
	}

	private void prepareResponse(Function<MockResponse.Builder, MockResponse.Builder> f) {
		MockResponse.Builder builder = new MockResponse.Builder();
		this.server.enqueue(f.apply(builder).build());
	}

}
