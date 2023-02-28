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

package org.springframework.web.reactive.function.client;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.web.testfixture.http.client.reactive.MockClientHttpRequest;
import org.springframework.web.testfixture.http.client.reactive.MockClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class DefaultClientResponseBuilderTests {

	@Test
	void normal() {
		Flux<DataBuffer> body = Flux.just("baz")
				.map(s -> s.getBytes(StandardCharsets.UTF_8))
				.map(DefaultDataBufferFactory.sharedInstance::wrap);

		ClientResponse response = ClientResponse.create(HttpStatus.BAD_GATEWAY, ExchangeStrategies.withDefaults())
				.header("foo", "bar")
				.cookie("baz", "qux")
				.body(body)
				.build();

		assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
		HttpHeaders responseHeaders = response.headers().asHttpHeaders();
		assertThat(responseHeaders.getFirst("foo")).isEqualTo("bar");
		assertThat(response.cookies().getFirst("baz")).as("qux").isNotNull();
		assertThat(response.cookies().getFirst("baz").getValue()).isEqualTo("qux");

		StepVerifier.create(response.bodyToFlux(String.class))
				.expectNext("baz")
				.verifyComplete();
	}

	@Test
	void mutate() {
		Flux<DataBuffer> otherBody = Flux.just("foo", "bar")
				.map(s -> s.getBytes(StandardCharsets.UTF_8))
				.map(DefaultDataBufferFactory.sharedInstance::wrap);

		HttpRequest mockClientHttpRequest = new MockClientHttpRequest(HttpMethod.GET, "/path");

		MockClientHttpResponse httpResponse = new MockClientHttpResponse(HttpStatus.OK);
		httpResponse.getHeaders().add("foo", "bar");
		httpResponse.getHeaders().add("bar", "baz");
		httpResponse.getCookies().add("baz", ResponseCookie.from("baz", "qux").build());
		httpResponse.setBody(otherBody);

		DefaultClientResponse otherResponse = new DefaultClientResponse(
				httpResponse, ExchangeStrategies.withDefaults(), "my-prefix", "", () -> mockClientHttpRequest);

		ClientResponse result = otherResponse.mutate()
				.statusCode(HttpStatus.BAD_REQUEST)
				.headers(headers -> headers.set("foo", "baar"))
				.cookies(cookies -> cookies.set("baz", ResponseCookie.from("baz", "quux").build()))
				.build();


		assertThat(result.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(result.headers().asHttpHeaders()).hasSize(3);
		assertThat(result.headers().asHttpHeaders().getFirst("foo")).isEqualTo("baar");
		assertThat(result.headers().asHttpHeaders().getFirst("bar")).isEqualTo("baz");
		assertThat(result.cookies()).hasSize(1);
		assertThat(result.cookies().getFirst("baz").getValue()).isEqualTo("quux");
		assertThat(result.logPrefix()).isEqualTo("my-prefix");

		StepVerifier.create(result.bodyToFlux(String.class))
				.expectNext("foobar")
				.verifyComplete();
	}

	@Test
	@SuppressWarnings("deprecation")
	void mutateWithCustomStatus() {
		ClientResponse other = ClientResponse.create(499, ExchangeStrategies.withDefaults()).build();
		ClientResponse result = other.mutate().build();

		assertThat(result.statusCode().value()).isEqualTo(499);
		assertThat(result.statusCode()).isEqualTo(HttpStatusCode.valueOf(499));
	}

}
