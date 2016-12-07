/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.client.reactive;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.codec.BodyInserter;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.web.client.reactive.test.MockClientHttpRequest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Arjen Poutsma
 */
public class DefaultClientRequestBuilderTests {

	@Test
	public void from() throws Exception {
		ClientRequest<Void> other = ClientRequest.GET("http://example.com")
				.header("foo", "bar")
				.cookie("baz", "qux").build();
		ClientRequest<Void> result = ClientRequest.from(other).build();
		assertEquals(new URI("http://example.com"), result.url());
		assertEquals(HttpMethod.GET, result.method());
		assertEquals("bar", result.headers().getFirst("foo"));
		assertEquals("qux", result.cookies().getFirst("baz"));
	}

	@Test
	public void method() throws Exception {
		URI url = new URI("http://example.com");
		ClientRequest<Void> result = ClientRequest.method(HttpMethod.DELETE, url).build();
		assertEquals(url, result.url());
		assertEquals(HttpMethod.DELETE, result.method());
	}

	@Test
	public void GET() throws Exception {
		URI url = new URI("http://example.com");
		ClientRequest<Void> result = ClientRequest.GET(url.toString()).build();
		assertEquals(url, result.url());
		assertEquals(HttpMethod.GET, result.method());
	}

	@Test
	public void HEAD() throws Exception {
		URI url = new URI("http://example.com");
		ClientRequest<Void> result = ClientRequest.HEAD(url.toString()).build();
		assertEquals(url, result.url());
		assertEquals(HttpMethod.HEAD, result.method());
	}

	@Test
	public void POST() throws Exception {
		URI url = new URI("http://example.com");
		ClientRequest<Void> result = ClientRequest.POST(url.toString()).build();
		assertEquals(url, result.url());
		assertEquals(HttpMethod.POST, result.method());
	}

	@Test
	public void PUT() throws Exception {
		URI url = new URI("http://example.com");
		ClientRequest<Void> result = ClientRequest.PUT(url.toString()).build();
		assertEquals(url, result.url());
		assertEquals(HttpMethod.PUT, result.method());
	}

	@Test
	public void PATCH() throws Exception {
		URI url = new URI("http://example.com");
		ClientRequest<Void> result = ClientRequest.PATCH(url.toString()).build();
		assertEquals(url, result.url());
		assertEquals(HttpMethod.PATCH, result.method());
	}

	@Test
	public void DELETE() throws Exception {
		URI url = new URI("http://example.com");
		ClientRequest<Void> result = ClientRequest.DELETE(url.toString()).build();
		assertEquals(url, result.url());
		assertEquals(HttpMethod.DELETE, result.method());
	}

	@Test
	public void OPTIONS() throws Exception {
		URI url = new URI("http://example.com");
		ClientRequest<Void> result = ClientRequest.OPTIONS(url.toString()).build();
		assertEquals(url, result.url());
		assertEquals(HttpMethod.OPTIONS, result.method());
	}

	@Test
	public void accept() throws Exception {
		MediaType json = MediaType.APPLICATION_JSON;
		ClientRequest<Void> result = ClientRequest.GET("http://example.com").accept(json).build();
		assertEquals(Collections.singletonList(json), result.headers().getAccept());
	}

	@Test
	public void acceptCharset() throws Exception {
		Charset charset = Charset.defaultCharset();
		ClientRequest<Void> result = ClientRequest.GET("http://example.com")
				.acceptCharset(charset).build();
		assertEquals(Collections.singletonList(charset), result.headers().getAcceptCharset());
	}

	@Test
	public void ifModifiedSince() throws Exception {
		ZonedDateTime now = ZonedDateTime.now();
		ClientRequest<Void> result = ClientRequest.GET("http://example.com")
				.ifModifiedSince(now).build();
		assertEquals(now.toInstant().toEpochMilli()/1000, result.headers().getIfModifiedSince()/1000);
	}

	@Test
	public void ifNoneMatch() throws Exception {
		ClientRequest<Void> result = ClientRequest.GET("http://example.com")
				.ifNoneMatch("\"v2.7\"", "\"v2.8\"").build();
		assertEquals(Arrays.asList("\"v2.7\"", "\"v2.8\""), result.headers().getIfNoneMatch());
	}

	@Test
	public void cookie() throws Exception {
		ClientRequest<Void> result = ClientRequest.GET("http://example.com")
				.cookie("foo", "bar").build();
		assertEquals("bar", result.cookies().getFirst("foo"));
	}

	@Test
	public void build() throws Exception {
		ClientRequest<Void> result = ClientRequest.GET("http://example.com")
				.header("MyKey", "MyValue")
				.cookie("foo", "bar")
				.build();

		MockClientHttpRequest request = new MockClientHttpRequest();
		WebClientStrategies strategies = mock(WebClientStrategies.class);

		result.writeTo(request, strategies).block();

		assertEquals("MyValue", request.getHeaders().getFirst("MyKey"));
		assertEquals("bar", request.getCookies().getFirst("foo").getValue());
		assertNull(request.getBody());
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

		ClientRequest<String> result = ClientRequest.POST("http://example.com")
				.body(inserter);

		MockClientHttpRequest request = new MockClientHttpRequest();

		List<HttpMessageWriter<?>> messageWriters = new ArrayList<>();
		messageWriters.add(new EncoderHttpMessageWriter<CharSequence>(new CharSequenceEncoder()));

		WebClientStrategies strategies = mock(WebClientStrategies.class);
		when(strategies.messageWriters()).thenReturn(messageWriters::stream);

		result.writeTo(request, strategies).block();
		assertNotNull(request.getBody());
	}

}