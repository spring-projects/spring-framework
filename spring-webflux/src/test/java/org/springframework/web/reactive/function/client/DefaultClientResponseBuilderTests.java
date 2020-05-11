/*
 * Copyright 2002-2020 the original author or authors.
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
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Arjen Poutsma
 */
public class DefaultClientResponseBuilderTests {

	private final DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();


	@Test
	public void normal() {
		Flux<DataBuffer> body = Flux.just("baz")
				.map(s -> s.getBytes(StandardCharsets.UTF_8))
				.map(dataBufferFactory::wrap);

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
	public void mutate() {

		ClientResponse originalResponse = ClientResponse
				.create(HttpStatus.BAD_REQUEST, ExchangeStrategies.withDefaults())
				.header("foo", "bar")
				.header("bar", "baz")
				.cookie("baz", "qux")
				.body(Flux.just("foobar".getBytes(StandardCharsets.UTF_8)).map(dataBufferFactory::wrap))
				.build();

		ClientResponse result = originalResponse.mutate()
				.statusCode(HttpStatus.OK)
				.headers(headers -> headers.set("foo", "baar"))
				.cookies(cookies -> cookies.set("baz", ResponseCookie.from("baz", "quux").build()))
				.build();

		assertThat(result.statusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.headers().asHttpHeaders().size()).isEqualTo(2);
		assertThat(result.headers().asHttpHeaders().getFirst("foo")).isEqualTo("baar");
		assertThat(result.headers().asHttpHeaders().getFirst("bar")).isEqualTo("baz");
		assertThat(result.cookies().size()).isEqualTo(1);
		assertThat(result.cookies().getFirst("baz").getValue()).isEqualTo("quux");

		StepVerifier.create(result.bodyToFlux(String.class))
				.expectNext("foobar")
				.verifyComplete();
	}

	@Test
	public void mutateWithCustomStatus() {
		ClientResponse other = ClientResponse.create(499, ExchangeStrategies.withDefaults()).build();
		ClientResponse result = other.mutate().build();

		assertThat(result.rawStatusCode()).isEqualTo(499);
		assertThatIllegalArgumentException().isThrownBy(result::statusCode);
	}

}
