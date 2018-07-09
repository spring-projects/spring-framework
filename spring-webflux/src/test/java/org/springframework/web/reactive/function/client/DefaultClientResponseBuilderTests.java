/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.function.client;

import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class DefaultClientResponseBuilderTests {

	private DataBufferFactory dataBufferFactory;

	@Before
	public void createBufferFactory() {
		this.dataBufferFactory = new DefaultDataBufferFactory();
	}

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

		assertEquals(HttpStatus.BAD_GATEWAY, response.statusCode());
		HttpHeaders responseHeaders = response.headers().asHttpHeaders();
		assertEquals("bar", responseHeaders.getFirst("foo"));
		assertNotNull("qux", response.cookies().getFirst("baz"));
		assertEquals("qux", response.cookies().getFirst("baz").getValue());

		StepVerifier.create(response.bodyToFlux(String.class))
				.expectNext("baz")
				.verifyComplete();
	}

	@Test
	public void from() {
		Flux<DataBuffer> otherBody = Flux.just("foo", "bar")
				.map(s -> s.getBytes(StandardCharsets.UTF_8))
				.map(dataBufferFactory::wrap);

		ClientResponse other = ClientResponse.create(HttpStatus.BAD_REQUEST, ExchangeStrategies.withDefaults())
				.header("foo", "bar")
				.cookie("baz", "qux")
				.body(otherBody)
				.build();

		Flux<DataBuffer> body = Flux.just("baz")
				.map(s -> s.getBytes(StandardCharsets.UTF_8))
				.map(dataBufferFactory::wrap);

		ClientResponse result = ClientResponse.from(other)
				.headers(httpHeaders -> httpHeaders.set("foo", "baar"))
				.cookies(cookies -> cookies.set("baz", ResponseCookie.from("baz", "quux").build()))
				.body(body)
				.build();

		assertEquals(HttpStatus.BAD_REQUEST, result.statusCode());
		assertEquals(1, result.headers().asHttpHeaders().size());
		assertEquals("baar", result.headers().asHttpHeaders().getFirst("foo"));
		assertEquals(1, result.cookies().size());
		assertEquals("quux", result.cookies().getFirst("baz").getValue());

		StepVerifier.create(result.bodyToFlux(String.class))
				.expectNext("baz")
				.verifyComplete();
	}


}
