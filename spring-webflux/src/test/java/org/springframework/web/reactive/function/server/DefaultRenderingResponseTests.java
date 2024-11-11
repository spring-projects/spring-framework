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

package org.springframework.web.reactive.function.server;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.result.view.AbstractView;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.reactive.result.view.ViewResolverSupport;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Arjen Poutsma
 */
class DefaultRenderingResponseTests {

	@Test
	void create() {
		String name = "foo";
		Mono<RenderingResponse> result = RenderingResponse.create(name).build();
		StepVerifier.create(result)
				.expectNextMatches(response -> name.equals(response.name()))
				.expectComplete()
				.verify();
	}

	@Test
	void headers() {
		HttpHeaders headers = new HttpHeaders();
		Mono<RenderingResponse> result = RenderingResponse.create("foo").headers(headers).build();
		StepVerifier.create(result)
				.expectNextMatches(response -> headers.equals(response.headers()))
				.expectComplete()
				.verify();

	}

	@Test
	void modelAttribute() {
		Mono<RenderingResponse> result = RenderingResponse.create("foo")
				.modelAttribute("foo", "bar").build();
		StepVerifier.create(result)
				.expectNextMatches(response -> "bar".equals(response.model().get("foo")))
				.expectComplete()
				.verify();
	}

	@Test
	void modelAttributeConventions() {
		Mono<RenderingResponse> result = RenderingResponse.create("foo")
				.modelAttribute("bar").build();
		StepVerifier.create(result)
				.expectNextMatches(response -> "bar".equals(response.model().get("string")))
				.expectComplete()
				.verify();
	}

	@Test
	void modelAttributes() {
		Map<String, String> model = Collections.singletonMap("foo", "bar");
		Mono<RenderingResponse> result = RenderingResponse.create("foo")
				.modelAttributes(model).build();
		StepVerifier.create(result)
				.expectNextMatches(response -> "bar".equals(response.model().get("foo")))
				.expectComplete()
				.verify();
	}

	@Test
	void modelAttributesConventions() {
		Set<String> model = Collections.singleton("bar");
		Mono<RenderingResponse> result = RenderingResponse.create("foo")
				.modelAttributes(model).build();
		StepVerifier.create(result)
				.expectNextMatches(response -> "bar".equals(response.model().get("string")))
				.expectComplete()
				.verify();
	}

	@Test
	void cookies() {
		MultiValueMap<String, ResponseCookie> newCookies = new LinkedMultiValueMap<>();
		newCookies.add("name", ResponseCookie.from("name", "value").build());
		Mono<RenderingResponse> result =
				RenderingResponse.create("foo").cookies(cookies -> cookies.addAll(newCookies)).build();
		StepVerifier.create(result)
				.expectNextMatches(response -> newCookies.equals(response.cookies()))
				.expectComplete()
				.verify();
	}


	@Test
	void render() {
		Map<String, Object> model = Collections.singletonMap("foo", "bar");
		Mono<RenderingResponse> result = RenderingResponse.create("view").modelAttributes(model).build();

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost"));
		ViewResolver viewResolver = mock();
		View view = mock();
		given(viewResolver.resolveViewName("view", Locale.ENGLISH)).willReturn(Mono.just(view));
		given(view.render(model, null, exchange)).willReturn(Mono.empty());

		List<ViewResolver> viewResolvers = new ArrayList<>();
		viewResolvers.add(viewResolver);

		HandlerStrategies mockConfig = mock();
		given(mockConfig.viewResolvers()).willReturn(viewResolvers);

		StepVerifier.create(result)
				.expectNextMatches(response -> "view".equals(response.name()) &&
						model.equals(response.model()))
				.expectComplete()
				.verify();
	}

	@Test
	void writeTo() {
		Map<String, Object> model = Collections.singletonMap("foo", "bar");
		RenderingResponse renderingResponse = RenderingResponse.create("view")
				.status(HttpStatus.FOUND)
				.modelAttributes(model)
				.build().block(Duration.of(5, ChronoUnit.MILLIS));
		assertThat(renderingResponse).isNotNull();

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost"));
		MediaType contentType = MediaType.APPLICATION_PDF;
		exchange.getResponse().getHeaders().setContentType(contentType);

		ViewResolver viewResolver = mock();
		RedirectView view = mock();
		given(viewResolver.resolveViewName(eq("view"), any())).willReturn(Mono.just(view));
		given(view.render(model, contentType, exchange)).willReturn(Mono.empty());

		List<ViewResolver> viewResolvers = new ArrayList<>();
		viewResolvers.add(viewResolver);

		HandlerStrategies mockConfig = mock();
		given(mockConfig.viewResolvers()).willReturn(viewResolvers);

		ServerResponse.Context context = mock();
		given(context.viewResolvers()).willReturn(viewResolvers);

		Mono<Void> result = renderingResponse.writeTo(exchange, context);
		StepVerifier.create(result)
				.expectComplete()
				.verify();

		verify(view).setStatusCode(HttpStatus.FOUND);
	}

	@Test
	void defaultContentType() {
		Mono<RenderingResponse> result = RenderingResponse.create("view").build();

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost"));
		TestView view = new TestView();
		ViewResolver viewResolver = mock();
		given(viewResolver.resolveViewName(any(), any())).willReturn(Mono.just(view));

		List<ViewResolver> viewResolvers = new ArrayList<>();
		viewResolvers.add(viewResolver);

		ServerResponse.Context context = mock();
		given(context.viewResolvers()).willReturn(viewResolvers);

		StepVerifier.create(result.flatMap(response -> response.writeTo(exchange, context)))
				.verifyComplete();

		assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(ViewResolverSupport.DEFAULT_CONTENT_TYPE);
	}


	private static class TestView extends AbstractView {

		@Override
		protected Mono<Void> renderInternal(Map<String, Object> renderAttributes,
				MediaType contentType, ServerWebExchange exchange) {

			return Mono.empty();
		}

	}

	@Test
	void notModifiedEtag() {
		String etag = "\"foo\"";
		RenderingResponse responseMono = RenderingResponse.create("bar")
				.header(HttpHeaders.ETAG, etag)
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

		RenderingResponse responseMono = RenderingResponse.create("bar")
				.header(HttpHeaders.LAST_MODIFIED, DateTimeFormatter.RFC_1123_DATE_TIME.format(oneMinuteBeforeNow))
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
