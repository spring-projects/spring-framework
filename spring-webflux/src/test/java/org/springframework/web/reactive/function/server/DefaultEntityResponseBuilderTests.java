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

package org.springframework.web.reactive.function.server;

import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerWebExchange;
import org.springframework.web.reactive.function.BodyInserter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * @author Arjen Poutsma
 */
public class DefaultEntityResponseBuilderTests {

	@Test
	public void fromObject() throws Exception {
		String body = "foo";
		EntityResponse<String> response = EntityResponse.fromObject(body).build().block();
		assertSame(body, response.entity());
	}

	@Test
	public void fromPublisherClass() throws Exception {
		Flux<String> body = Flux.just("foo", "bar");
		EntityResponse<Flux<String>> response = EntityResponse.fromPublisher(body, String.class).build().block();
		assertSame(body, response.entity());
	}

	@Test
	public void fromPublisherResolvableType() throws Exception {
		Flux<String> body = Flux.just("foo", "bar");
		ResolvableType type = ResolvableType.forClass(String.class);
		EntityResponse<Flux<String>> response = EntityResponse.fromPublisher(body, type).build().block();
		assertSame(body, response.entity());
	}

	@Test
	public void status() throws Exception {
		String body = "foo";
		Mono<EntityResponse<String>> result = EntityResponse.fromObject(body).status(HttpStatus.CREATED).build();
		StepVerifier.create(result)
				.expectNextMatches(response -> HttpStatus.CREATED.equals(response.statusCode()))
				.expectComplete()
				.verify();
	}

	@Test
	public void allow() throws Exception {
		String body = "foo";
		Mono<EntityResponse<String>> result = EntityResponse.fromObject(body).allow(HttpMethod.GET).build();
		Set<HttpMethod> expected = EnumSet.of(HttpMethod.GET);
		StepVerifier.create(result)
				.expectNextMatches(response -> expected.equals(response.headers().getAllow()))
				.expectComplete()
				.verify();
	}

	@Test
	public void contentLength() throws Exception {
		String body = "foo";
		Mono<EntityResponse<String>> result = EntityResponse.fromObject(body).contentLength(42).build();
		StepVerifier.create(result)
				.expectNextMatches(response -> Long.valueOf(42).equals(response.headers().getContentLength()))
				.expectComplete()
				.verify();
	}

	@Test
	public void contentType() throws Exception {
		String body = "foo";
		Mono<EntityResponse<String>>
				result = EntityResponse.fromObject(body).contentType(MediaType.APPLICATION_JSON).build();
		StepVerifier.create(result)
				.expectNextMatches(response -> MediaType.APPLICATION_JSON.equals(response.headers().getContentType()))
				.expectComplete()
				.verify();
	}

	@Test
	public void etag() throws Exception {
		String body = "foo";
		Mono<EntityResponse<String>> result = EntityResponse.fromObject(body).eTag("foo").build();
		StepVerifier.create(result)
				.expectNextMatches(response -> "\"foo\"".equals(response.headers().getETag()))
				.expectComplete()
				.verify();
	}


	@Test
	public void lastModified() throws Exception {
		ZonedDateTime now = ZonedDateTime.now();
		String body = "foo";
		Mono<EntityResponse<String>> result = EntityResponse.fromObject(body).lastModified(now).build();
		Long expected = now.toInstant().toEpochMilli() / 1000;
		StepVerifier.create(result)
				.expectNextMatches(response -> expected.equals(response.headers().getLastModified() / 1000))
				.expectComplete()
				.verify();
	}

	@Test
	public void cacheControlTag() throws Exception {
		String body = "foo";
		Mono<EntityResponse<String>>
				result = EntityResponse.fromObject(body).cacheControl(CacheControl.noCache()).build();
		StepVerifier.create(result)
				.expectNextMatches(response -> "no-cache".equals(response.headers().getCacheControl()))
				.expectComplete()
				.verify();
	}

	@Test
	public void varyBy() throws Exception {
		String body = "foo";
		Mono<EntityResponse<String>> result = EntityResponse.fromObject(body).varyBy("foo").build();
		List<String> expected = Collections.singletonList("foo");
		StepVerifier.create(result)
				.expectNextMatches(response -> expected.equals(response.headers().getVary()))
				.expectComplete()
				.verify();
	}

	@Test
	public void headers() throws Exception {
		String body = "foo";
		HttpHeaders headers = new HttpHeaders();
		Mono<EntityResponse<String>> result = EntityResponse.fromObject(body).headers(headers).build();
		StepVerifier.create(result)
				.expectNextMatches(response -> headers.equals(response.headers()))
				.expectComplete()
				.verify();
	}

	@Test
	public void bodyInserter() throws Exception {
		String body = "foo";
		Publisher<String> publisher = Mono.just(body);
		BiFunction<ServerHttpResponse, BodyInserter.Context, Mono<Void>> writer =
				(response, strategies) -> {
					byte[] bodyBytes = body.getBytes(UTF_8);
					ByteBuffer byteBuffer = ByteBuffer.wrap(bodyBytes);
					DataBuffer buffer = new DefaultDataBufferFactory().wrap(byteBuffer);

					return response.writeWith(Mono.just(buffer));
				};

		Mono<EntityResponse<Publisher<String>>> result = EntityResponse.fromPublisher(publisher, String.class).build();

		MockServerWebExchange exchange = MockServerHttpRequest.get("http://localhost").toExchange();

		HandlerStrategies strategies = HandlerStrategies.empty()
				.messageWriter(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()))
				.build();

		StepVerifier.create(result)
				.consumeNextWith(response -> {
					StepVerifier.create(response.entity())
							.expectNext(body)
							.expectComplete()
							.verify();
					response.writeTo(exchange, strategies);
				})
				.expectComplete()
				.verify();

		assertNotNull(exchange.getResponse().getBody());
	}

}
