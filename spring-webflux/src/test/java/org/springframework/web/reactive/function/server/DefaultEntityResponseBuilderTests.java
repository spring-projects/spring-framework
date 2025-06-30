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

package org.springframework.web.reactive.function.server;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class DefaultEntityResponseBuilderTests {

	@Test
	void fromObject() {
		String body = "foo";
		EntityResponse<String> response = EntityResponse.fromObject(body).build().block();
		assertThat(response.entity()).isSameAs(body);
	}

	@Test
	void fromPublisherClass() {
		Flux<String> body = Flux.just("foo", "bar");
		EntityResponse<Flux<String>> response = EntityResponse.fromPublisher(body, String.class).build().block();
		assertThat(response.entity()).isSameAs(body);
	}

	@Test
	void fromPublisher() {
		Flux<String> body = Flux.just("foo", "bar");
		ParameterizedTypeReference<String> typeReference = new ParameterizedTypeReference<>() {};
		EntityResponse<Flux<String>> response = EntityResponse.fromPublisher(body, typeReference).build().block();
		assertThat(response.entity()).isSameAs(body);
	}

	@Test
	void fromProducer() {
		Single<String> body = Single.just("foo");
		ParameterizedTypeReference<String> typeReference = new ParameterizedTypeReference<>() {};
		EntityResponse<Single<String>> response = EntityResponse.fromProducer(body, typeReference).build().block();
		assertThat(response.entity()).isSameAs(body);
	}

	@Test
	void status() {
		String body = "foo";
		Mono<EntityResponse<String>> result = EntityResponse.fromObject(body).status(HttpStatus.CREATED).build();
		StepVerifier.create(result)
				.expectNextMatches(response -> HttpStatus.CREATED.equals(response.statusCode()))
				.expectComplete()
				.verify();
	}

	@Test
	void allow() {
		String body = "foo";
		Mono<EntityResponse<String>> result = EntityResponse.fromObject(body).allow(HttpMethod.GET).build();
		Set<HttpMethod> expected = Set.of(HttpMethod.GET);
		StepVerifier.create(result)
				.expectNextMatches(response -> expected.equals(response.headers().getAllow()))
				.expectComplete()
				.verify();
	}

	@Test
	void contentLength() {
		String body = "foo";
		Mono<EntityResponse<String>> result = EntityResponse.fromObject(body).contentLength(42).build();
		StepVerifier.create(result)
				.expectNextMatches(response -> Long.valueOf(42).equals(response.headers().getContentLength()))
				.expectComplete()
				.verify();
	}

	@Test
	void contentType() {
		String body = "foo";
		Mono<EntityResponse<String>>
				result = EntityResponse.fromObject(body).contentType(MediaType.APPLICATION_JSON).build();
		StepVerifier.create(result)
				.expectNextMatches(response -> MediaType.APPLICATION_JSON.equals(response.headers().getContentType()))
				.expectComplete()
				.verify();
	}

	@Test
	void etag() {
		String body = "foo";
		Mono<EntityResponse<String>> result = EntityResponse.fromObject(body).eTag("foo").build();
		StepVerifier.create(result)
				.expectNextMatches(response -> "\"foo\"".equals(response.headers().getETag()))
				.expectComplete()
				.verify();
	}

	@Test
	void lastModified() {
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
	void cacheControlTag() {
		String body = "foo";
		Mono<EntityResponse<String>>
				result = EntityResponse.fromObject(body).cacheControl(CacheControl.noCache()).build();
		StepVerifier.create(result)
				.expectNextMatches(response -> "no-cache".equals(response.headers().getCacheControl()))
				.expectComplete()
				.verify();
	}

	@Test
	void varyBy() {
		String body = "foo";
		Mono<EntityResponse<String>> result = EntityResponse.fromObject(body).varyBy("foo").build();
		List<String> expected = Collections.singletonList("foo");
		StepVerifier.create(result)
				.expectNextMatches(response -> expected.equals(response.headers().getVary()))
				.expectComplete()
				.verify();
	}

	@Test
	void headers() {
		String body = "foo";
		HttpHeaders headers = new HttpHeaders();
		Mono<EntityResponse<String>> result = EntityResponse.fromObject(body).headers(headers).build();
		StepVerifier.create(result)
				.expectNextMatches(response -> headers.equals(response.headers()))
				.expectComplete()
				.verify();
	}

	@Test
	void cookies() {
		MultiValueMap<String, ResponseCookie> newCookies = new LinkedMultiValueMap<>();
		newCookies.add("name", ResponseCookie.from("name", "value").build());
		Mono<EntityResponse<String>> result =
				EntityResponse.fromObject("foo").cookies(cookies -> cookies.addAll(newCookies)).build();
		StepVerifier.create(result)
				.expectNextMatches(response -> newCookies.equals(response.cookies()))
				.expectComplete()
				.verify();
	}

	@Test
	void bodyInserter() {
		String body = "foo";
		Publisher<String> publisher = Mono.just(body);

		Mono<EntityResponse<Publisher<String>>> result = EntityResponse.fromPublisher(publisher, String.class).build();

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost"));

		ServerResponse.Context context = new ServerResponse.Context() {
			@Override
			public List<HttpMessageWriter<?>> messageWriters() {
				return Collections.singletonList(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));
			}

			@Override
			public List<ViewResolver> viewResolvers() {
				return Collections.emptyList();
			}
		};
		StepVerifier.create(result)
				.consumeNextWith(response -> {
					StepVerifier.create(response.entity())
							.expectNext(body)
							.expectComplete()
							.verify();
					response.writeTo(exchange, context);
				})
				.expectComplete()
				.verify();

		assertThat(exchange.getResponse().getBody()).isNotNull();
	}

	@Test
	void notModifiedEtag() {
		String etag = "\"foo\"";
		EntityResponse<String> responseMono = EntityResponse.fromObject("bar")
				.eTag(etag)
				.build()
				.block();

		MockServerHttpRequest request = MockServerHttpRequest.get("https://example.com")
				.header(HttpHeaders.IF_NONE_MATCH, etag)
				.build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		responseMono.writeTo(exchange, DefaultServerResponseBuilderTests.EMPTY_CONTEXT);

		MockServerHttpResponse response = exchange.getResponse();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
		StepVerifier.create(response.getBody())
				.expectError(IllegalStateException.class)
				.verify();
	}

	@Test
	void notModifiedLastModified() {
		ZonedDateTime now = ZonedDateTime.now();
		ZonedDateTime oneMinuteBeforeNow = now.minusMinutes(1);

		EntityResponse<String> responseMono = EntityResponse.fromObject("bar")
				.lastModified(oneMinuteBeforeNow)
				.build()
				.block();

		MockServerHttpRequest request = MockServerHttpRequest.get("https://example.com")
				.header(HttpHeaders.IF_MODIFIED_SINCE,
						DateTimeFormatter.RFC_1123_DATE_TIME.format(now))
				.build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		responseMono.writeTo(exchange, DefaultServerResponseBuilderTests.EMPTY_CONTEXT);

		MockServerHttpResponse response = exchange.getResponse();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
		StepVerifier.create(response.getBody())
				.expectError(IllegalStateException.class)
				.verify();
	}

}
