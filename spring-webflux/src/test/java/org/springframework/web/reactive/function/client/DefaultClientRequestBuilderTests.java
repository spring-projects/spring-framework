/*
 * Copyright 2002-2017 the original author or authors.
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

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.mock.http.client.reactive.test.MockClientHttpRequest;
import org.springframework.web.reactive.function.BodyInserter;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpMethod.*;

/**
 * @author Arjen Poutsma
 */
public class DefaultClientRequestBuilderTests {

	@Test
	public void from() throws Exception {
		ClientRequest other = ClientRequest.method(GET, URI.create("http://example.com"))
				.header("foo", "bar")
				.cookie("baz", "qux").build();
		ClientRequest result = ClientRequest.from(other).build();
		assertEquals(new URI("http://example.com"), result.url());
		assertEquals(GET, result.method());
		assertEquals("bar", result.headers().getFirst("foo"));
		assertEquals("qux", result.cookies().getFirst("baz"));
	}

	@Test
	public void method() throws Exception {
		URI url = new URI("http://example.com");
		ClientRequest result = ClientRequest.method(DELETE, url).build();
		assertEquals(url, result.url());
		assertEquals(DELETE, result.method());
	}

	@Test
	public void cookie() throws Exception {
		ClientRequest result = ClientRequest.method(GET, URI.create("http://example.com"))
				.cookie("foo", "bar").build();
		assertEquals("bar", result.cookies().getFirst("foo"));
	}

	@Test
	public void build() throws Exception {
		ClientRequest result = ClientRequest.method(GET, URI.create("http://example.com"))
				.header("MyKey", "MyValue")
				.cookie("foo", "bar")
				.build();

		MockClientHttpRequest request = new MockClientHttpRequest(GET, "/");
		ExchangeStrategies strategies = mock(ExchangeStrategies.class);

		result.writeTo(request, strategies).block();

		assertEquals("MyValue", request.getHeaders().getFirst("MyKey"));
		assertEquals("bar", request.getCookies().getFirst("foo").getValue());
		StepVerifier.create(request.getBody()).expectComplete().verify();
	}

	@Test
	public void bodyInserter() throws Exception {
		String body = "foo";
		BodyInserter<String, ClientHttpRequest> inserter =
				(response, strategies) -> {
					byte[] bodyBytes = body.getBytes(UTF_8);
					ByteBuffer byteBuffer = ByteBuffer.wrap(bodyBytes);
					DataBuffer buffer = new DefaultDataBufferFactory().wrap(byteBuffer);

					return response.writeWith(Mono.just(buffer));
				};

		ClientRequest result = ClientRequest.method(POST, URI.create("http://example.com"))
				.body(inserter).build();

		List<HttpMessageWriter<?>> messageWriters = new ArrayList<>();
		messageWriters.add(new EncoderHttpMessageWriter<>(new CharSequenceEncoder()));

		ExchangeStrategies strategies = mock(ExchangeStrategies.class);
		when(strategies.messageWriters()).thenReturn(messageWriters::stream);

		MockClientHttpRequest request = new MockClientHttpRequest(GET, "/");
		result.writeTo(request, strategies).block();
		assertNotNull(request.getBody());
	}

}