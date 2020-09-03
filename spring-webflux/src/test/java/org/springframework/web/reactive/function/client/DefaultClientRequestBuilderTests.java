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

import java.net.URI;
import java.net.URISyntaxException;
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
 * Unit tests for {@link DefaultClientRequestBuilder}.
 * @author Arjen Poutsma
 */
public class DefaultClientRequestBuilderTests {

	@Test
	public void from() throws URISyntaxException {
		ClientRequest other = ClientRequest.create(GET, URI.create("https://example.com"))
				.header("foo", "bar")
				.cookie("baz", "qux")
				.httpRequest(request -> {})
				.build();
		ClientRequest result = ClientRequest.from(other)
				.headers(httpHeaders -> httpHeaders.set("foo", "baar"))
				.cookies(cookies -> cookies.set("baz", "quux"))
				.build();
		assertThat(result.url()).isEqualTo(new URI("https://example.com"));
		assertThat(result.method()).isEqualTo(GET);
		assertThat(result.headers().size()).isEqualTo(1);
		assertThat(result.headers().getFirst("foo")).isEqualTo("baar");
		assertThat(result.cookies().size()).isEqualTo(1);
		assertThat(result.cookies().getFirst("baz")).isEqualTo("quux");
		assertThat(result.httpRequest()).isNotNull();
	}

	@Test
	public void method() throws URISyntaxException {
		URI url = new URI("https://example.com");
		ClientRequest.Builder builder = ClientRequest.create(DELETE, url);
		assertThat(builder.build().method()).isEqualTo(DELETE);

		builder.method(OPTIONS);
		assertThat(builder.build().method()).isEqualTo(OPTIONS);
	}

	@Test
	public void url() throws URISyntaxException {
		URI url1 = new URI("https://example.com/foo");
		URI url2 = new URI("https://example.com/bar");
		ClientRequest.Builder builder = ClientRequest.create(DELETE, url1);
		assertThat(builder.build().url()).isEqualTo(url1);

		builder.url(url2);
		assertThat(builder.build().url()).isEqualTo(url2);
	}

	@Test
	public void cookie() {
		ClientRequest result = ClientRequest.create(GET, URI.create("https://example.com"))
				.cookie("foo", "bar").build();
		assertThat(result.cookies().getFirst("foo")).isEqualTo("bar");
	}

	@Test
	public void build() {
		ClientRequest result = ClientRequest.create(GET, URI.create("https://example.com"))
				.header("MyKey", "MyValue")
				.cookie("foo", "bar")
				.httpRequest(request -> {
					MockClientHttpRequest nativeRequest = (MockClientHttpRequest) request.getNativeRequest();
					nativeRequest.getHeaders().add("MyKey2", "MyValue2");
				})
				.build();

		MockClientHttpRequest request = new MockClientHttpRequest(GET, "/");
		ExchangeStrategies strategies = mock(ExchangeStrategies.class);

		result.writeTo(request, strategies).block();

		assertThat(request.getHeaders().getFirst("MyKey")).isEqualTo("MyValue");
		assertThat(request.getHeaders().getFirst("MyKey2")).isEqualTo("MyValue2");
		assertThat(request.getCookies().getFirst("foo").getValue()).isEqualTo("bar");

		StepVerifier.create(request.getBody()).expectComplete().verify();
	}

	@Test
	public void bodyInserter() {
		String body = "foo";
		BodyInserter<String, ClientHttpRequest> inserter =
				(response, strategies) -> {
					byte[] bodyBytes = body.getBytes(UTF_8);
					DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(bodyBytes);

					return response.writeWith(Mono.just(buffer));
				};

		ClientRequest result = ClientRequest.create(POST, URI.create("https://example.com"))
				.body(inserter).build();

		List<HttpMessageWriter<?>> messageWriters = new ArrayList<>();
		messageWriters.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));

		ExchangeStrategies strategies = mock(ExchangeStrategies.class);
		given(strategies.messageWriters()).willReturn(messageWriters);

		MockClientHttpRequest request = new MockClientHttpRequest(GET, "/");
		result.writeTo(request, strategies).block();
		assertThat(request.getBody()).isNotNull();

		StepVerifier.create(request.getBody())
				.expectNextCount(1)
				.verifyComplete();
	}

	@Test
	public void bodyClass() {
		String body = "foo";
		Publisher<String> publisher = Mono.just(body);
		ClientRequest result = ClientRequest.create(POST, URI.create("https://example.com"))
				.body(publisher, String.class).build();

		List<HttpMessageWriter<?>> messageWriters = new ArrayList<>();
		messageWriters.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));

		ExchangeStrategies strategies = mock(ExchangeStrategies.class);
		given(strategies.messageWriters()).willReturn(messageWriters);

		MockClientHttpRequest request = new MockClientHttpRequest(GET, "/");
		result.writeTo(request, strategies).block();
		assertThat(request.getBody()).isNotNull();

		StepVerifier.create(request.getBody())
				.expectNextCount(1)
				.verifyComplete();
	}

	@Test
	public void bodyParameterizedTypeReference() {
		String body = "foo";
		Publisher<String> publisher = Mono.just(body);
		ParameterizedTypeReference<String> typeReference = new ParameterizedTypeReference<String>() {};
		ClientRequest result = ClientRequest.create(POST, URI.create("https://example.com"))
				.body(publisher, typeReference).build();

		List<HttpMessageWriter<?>> messageWriters = new ArrayList<>();
		messageWriters.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));

		ExchangeStrategies strategies = mock(ExchangeStrategies.class);
		given(strategies.messageWriters()).willReturn(messageWriters);

		MockClientHttpRequest request = new MockClientHttpRequest(GET, "/");
		result.writeTo(request, strategies).block();
		assertThat(request.getBody()).isNotNull();

		StepVerifier.create(request.getBody())
				.expectNextCount(1)
				.verifyComplete();
	}

}
