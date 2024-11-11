/*
 * Copyright 2002-2024 the original author or authors.
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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.testfixture.http.client.reactive.MockClientHttpRequest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.POST;

/**
 * Tests for {@link DefaultClientRequestBuilder}.
 *
 * @author Arjen Poutsma
 */
class DefaultClientRequestBuilderTests {

	private static final URI DEFAULT_URL = URI.create("https://example.com");

	@Test
	void from() {
		ClientRequest other = ClientRequest.create(GET, DEFAULT_URL)
				.header("foo", "bar")
				.cookie("baz", "qux")
				.attribute("attributeKey", "attributeValue")
				.attribute("anotherAttributeKey", "anotherAttributeValue")
				.httpRequest(request -> {})
				.build();

		ClientRequest result = ClientRequest.from(other)
				.headers(httpHeaders -> httpHeaders.set("foo", "baar"))
				.cookies(cookies -> cookies.set("baz", "quux"))
				.build();

		assertThat(result.url()).isEqualTo(DEFAULT_URL);
		assertThat(result.method()).isEqualTo(GET);
		assertThat(result.headers()).hasSize(1);
		assertThat(result.headers().getFirst("foo")).isEqualTo("baar");
		assertThat(result.cookies()).hasSize(1);
		assertThat(result.cookies().getFirst("baz")).isEqualTo("quux");
		assertThat(result.httpRequest()).isNotNull();
		assertThat(result.attributes().get("attributeKey")).isEqualTo("attributeValue");
		assertThat(result.attributes().get("anotherAttributeKey")).isEqualTo("anotherAttributeValue");
	}

	@Test
	void fromCopiesBody() {
		String body = "foo";
		BodyInserter<String, ClientHttpRequest> inserter = (response, strategies) -> {
			byte[] bodyBytes = body.getBytes(UTF_8);
			DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(bodyBytes);
			return response.writeWith(Mono.just(buffer));
		};

		ClientRequest other = ClientRequest.create(POST, DEFAULT_URL).body(inserter).build();
		ClientRequest result = ClientRequest.from(other).build();

		List<HttpMessageWriter<?>> messageWriters = new ArrayList<>();
		messageWriters.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));

		ExchangeStrategies strategies = mock();
		given(strategies.messageWriters()).willReturn(messageWriters);

		MockClientHttpRequest request = new MockClientHttpRequest(POST, "/");
		result.writeTo(request, strategies).block();

		String copiedBody = request.getBodyAsString().block();

		assertThat(copiedBody).isEqualTo("foo");
	}

	@Test
	void method() {
		ClientRequest.Builder builder = ClientRequest.create(DELETE, DEFAULT_URL);
		assertThat(builder.build().method()).isEqualTo(DELETE);

		builder.method(OPTIONS);
		assertThat(builder.build().method()).isEqualTo(OPTIONS);
	}

	@Test
	void url() {
		URI url1 = URI.create("https://example.com/foo");
		URI url2 = URI.create("https://example.com/bar");
		ClientRequest.Builder builder = ClientRequest.create(DELETE, url1);
		assertThat(builder.build().url()).isEqualTo(url1);

		builder.url(url2);
		assertThat(builder.build().url()).isEqualTo(url2);
	}

	@Test
	void cookie() {
		ClientRequest result = ClientRequest.create(GET, DEFAULT_URL).cookie("foo", "bar").build();
		assertThat(result.cookies().getFirst("foo")).isEqualTo("bar");
	}

	@Test
	void build() {
		ClientRequest result = ClientRequest.create(GET, DEFAULT_URL)
				.header("MyKey", "MyValue")
				.cookie("foo", "bar")
				.httpRequest(request -> {
					MockClientHttpRequest nativeRequest = request.getNativeRequest();
					nativeRequest.getHeaders().add("MyKey2", "MyValue2");
				})
				.build();

		MockClientHttpRequest request = new MockClientHttpRequest(GET, "/");
		ExchangeStrategies strategies = mock();

		result.writeTo(request, strategies).block();

		assertThat(request.getHeaders().getFirst("MyKey")).isEqualTo("MyValue");
		assertThat(request.getHeaders().getFirst("MyKey2")).isEqualTo("MyValue2");
		assertThat(request.getCookies().getFirst("foo").getValue()).isEqualTo("bar");

		StepVerifier.create(request.getBody()).expectComplete().verify();
	}

	@Test
	void bodyInserter() {
		String body = "foo";
		BodyInserter<String, ClientHttpRequest> inserter = (response, strategies) -> {
			byte[] bodyBytes = body.getBytes(UTF_8);
			DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(bodyBytes);

			return response.writeWith(Mono.just(buffer));
		};

		ClientRequest result = ClientRequest.create(POST, DEFAULT_URL).body(inserter).build();

		List<HttpMessageWriter<?>> messageWriters = new ArrayList<>();
		messageWriters.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));

		ExchangeStrategies strategies = mock();
		given(strategies.messageWriters()).willReturn(messageWriters);

		MockClientHttpRequest request = new MockClientHttpRequest(GET, "/");
		result.writeTo(request, strategies).block();
		assertThat(request.getBody()).isNotNull();

		StepVerifier.create(request.getBody()).expectNextCount(1).verifyComplete();
	}

	@Test
	void bodyClass() {
		String body = "foo";
		Publisher<String> publisher = Mono.just(body);
		ClientRequest result = ClientRequest.create(POST, DEFAULT_URL).body(publisher, String.class).build();

		List<HttpMessageWriter<?>> messageWriters = new ArrayList<>();
		messageWriters.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));

		ExchangeStrategies strategies = mock();
		given(strategies.messageWriters()).willReturn(messageWriters);

		MockClientHttpRequest request = new MockClientHttpRequest(GET, "/");
		result.writeTo(request, strategies).block();
		assertThat(request.getBody()).isNotNull();

		StepVerifier.create(request.getBody()).expectNextCount(1).verifyComplete();
	}

	@Test
	void bodyParameterizedTypeReference() {
		String body = "foo";
		Publisher<String> publisher = Mono.just(body);
		ParameterizedTypeReference<String> typeReference = new ParameterizedTypeReference<>() {};
		ClientRequest result = ClientRequest.create(POST, DEFAULT_URL).body(publisher, typeReference).build();

		List<HttpMessageWriter<?>> messageWriters = new ArrayList<>();
		messageWriters.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));

		ExchangeStrategies strategies = mock();
		given(strategies.messageWriters()).willReturn(messageWriters);

		MockClientHttpRequest request = new MockClientHttpRequest(GET, "/");
		result.writeTo(request, strategies).block();
		assertThat(request.getBody()).isNotNull();

		StepVerifier.create(request.getBody()).expectNextCount(1).verifyComplete();
	}

}
