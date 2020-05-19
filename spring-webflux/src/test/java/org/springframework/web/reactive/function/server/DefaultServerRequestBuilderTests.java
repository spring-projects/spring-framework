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

package org.springframework.web.reactive.function.server;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link DefaultServerRequestBuilder}.
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
public class DefaultServerRequestBuilderTests {

	private final DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();


	@Test
	public void from() {
		MockServerHttpRequest request = MockServerHttpRequest.post("https://example.com")
				.header("foo", "bar")
				.build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		ServerRequest other =
				ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());
		other.attributes().put("attr1", "value1");

		Flux<DataBuffer> body = Flux.just("baz")
				.map(s -> s.getBytes(StandardCharsets.UTF_8))
				.map(dataBufferFactory::wrap);

		ServerRequest result = ServerRequest.from(other)
				.method(HttpMethod.HEAD)
				.headers(httpHeaders -> httpHeaders.set("foo", "baar"))
				.cookies(cookies -> cookies.set("baz", ResponseCookie.from("baz", "quux").build()))
				.attribute("attr2", "value2")
				.attributes(attributes -> attributes.put("attr3", "value3"))
				.body(body)
				.build();

		assertEquals(HttpMethod.HEAD, result.method());
		assertEquals(1, result.headers().asHttpHeaders().size());
		assertEquals("baar", result.headers().asHttpHeaders().getFirst("foo"));
		assertEquals(1, result.cookies().size());
		assertEquals("quux", result.cookies().getFirst("baz").getValue());

		assertEquals(4, result.attributes().size());
		assertEquals(new HashSet<>(Arrays.asList(ServerWebExchange.LOG_ID_ATTRIBUTE, "attr1", "attr2", "attr3")),
				result.attributes().keySet());
		assertEquals("value1", result.attributes().get("attr1"));
		assertEquals("value2", result.attributes().get("attr2"));
		assertEquals("value3", result.attributes().get("attr3"));

		StepVerifier.create(result.bodyToFlux(String.class))
				.expectNext("baz")
				.verifyComplete();
	}

}
