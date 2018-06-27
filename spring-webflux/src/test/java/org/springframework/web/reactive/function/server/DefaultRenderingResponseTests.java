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

package org.springframework.web.reactive.function.server;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.result.view.AbstractView;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.reactive.result.view.ViewResolverSupport;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Arjen Poutsma
 */
public class DefaultRenderingResponseTests {

	@Test
	public void create() throws Exception {
		String name = "foo";
		Mono<RenderingResponse> result = RenderingResponse.create(name).build();
		StepVerifier.create(result)
				.expectNextMatches(response -> name.equals(response.name()))
				.expectComplete()
				.verify();
	}

	@Test
	public void headers() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		Mono<RenderingResponse> result = RenderingResponse.create("foo").headers(headers).build();
		StepVerifier.create(result)
				.expectNextMatches(response -> headers.equals(response.headers()))
				.expectComplete()
				.verify();

	}

	@Test
	public void modelAttribute() throws Exception {
		Mono<RenderingResponse> result = RenderingResponse.create("foo")
				.modelAttribute("foo", "bar").build();
		StepVerifier.create(result)
				.expectNextMatches(response -> "bar".equals(response.model().get("foo")))
				.expectComplete()
				.verify();
	}

	@Test
	public void modelAttributeConventions() throws Exception {
		Mono<RenderingResponse> result = RenderingResponse.create("foo")
				.modelAttribute("bar").build();
		StepVerifier.create(result)
				.expectNextMatches(response -> "bar".equals(response.model().get("string")))
				.expectComplete()
				.verify();
	}

	@Test
	public void modelAttributes() throws Exception {
		Map<String, String> model = Collections.singletonMap("foo", "bar");
		Mono<RenderingResponse> result = RenderingResponse.create("foo")
				.modelAttributes(model).build();
		StepVerifier.create(result)
				.expectNextMatches(response -> "bar".equals(response.model().get("foo")))
				.expectComplete()
				.verify();
	}

	@Test
	public void modelAttributesConventions() throws Exception {
		Set<String> model = Collections.singleton("bar");
		Mono<RenderingResponse> result = RenderingResponse.create("foo")
				.modelAttributes(model).build();
		StepVerifier.create(result)
				.expectNextMatches(response -> "bar".equals(response.model().get("string")))
				.expectComplete()
				.verify();
	}

	@Test
	public void cookies() throws Exception {
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
	public void render() throws Exception {
		Map<String, Object> model = Collections.singletonMap("foo", "bar");
		Mono<RenderingResponse> result = RenderingResponse.create("view").modelAttributes(model).build();

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost"));
		ViewResolver viewResolver = mock(ViewResolver.class);
		View view = mock(View.class);
		when(viewResolver.resolveViewName("view", Locale.ENGLISH)).thenReturn(Mono.just(view));
		when(view.render(model, null, exchange)).thenReturn(Mono.empty());

		List<ViewResolver> viewResolvers = new ArrayList<>();
		viewResolvers.add(viewResolver);

		HandlerStrategies mockConfig = mock(HandlerStrategies.class);
		when(mockConfig.viewResolvers()).thenReturn(viewResolvers);

		StepVerifier.create(result)
				.expectNextMatches(response -> "view".equals(response.name()) &&
						model.equals(response.model()))
				.expectComplete()
				.verify();
	}

	@Test
	public void defaultContentType() throws Exception {
		Mono<RenderingResponse> result = RenderingResponse.create("view").build();

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost"));
		TestView view = new TestView();
		ViewResolver viewResolver = mock(ViewResolver.class);
		when(viewResolver.resolveViewName(any(), any())).thenReturn(Mono.just(view));

		List<ViewResolver> viewResolvers = new ArrayList<>();
		viewResolvers.add(viewResolver);

		ServerResponse.Context context = mock(ServerResponse.Context.class);
		when(context.viewResolvers()).thenReturn(viewResolvers);

		StepVerifier.create(result.flatMap(response -> response.writeTo(exchange, context)))
				.verifyComplete();

		assertEquals(ViewResolverSupport.DEFAULT_CONTENT_TYPE, exchange.getResponse().getHeaders().getContentType());
	}


	private static class TestView extends AbstractView {

		@Override
		protected Mono<Void> renderInternal(Map<String, Object> renderAttributes,
				MediaType contentType, ServerWebExchange exchange) {

			return Mono.empty();
		}

	}

	@Test
	public void notModifiedEtag() {
		String etag = "\"foo\"";
		RenderingResponse responseMono = RenderingResponse.create("bar")
				.header(HttpHeaders.ETAG, etag)
				.build()
				.block();

		MockServerHttpRequest request = MockServerHttpRequest.get("http://example.com")
				.header(HttpHeaders.IF_NONE_MATCH, etag)
				.build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		responseMono.writeTo(exchange, DefaultServerResponseBuilderTests.EMPTY_CONTEXT);

		MockServerHttpResponse response = exchange.getResponse();
		assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
		StepVerifier.create(response.getBody())
				.expectError(IllegalStateException.class)
				.verify();
	}

	@Test
	public void notModifiedLastModified() {
		ZonedDateTime now = ZonedDateTime.now();
		ZonedDateTime oneMinuteBeforeNow = now.minus(1, ChronoUnit.MINUTES);

		RenderingResponse responseMono = RenderingResponse.create("bar")
				.header(HttpHeaders.LAST_MODIFIED, DateTimeFormatter.RFC_1123_DATE_TIME.format(oneMinuteBeforeNow))
				.build()
				.block();

		MockServerHttpRequest request = MockServerHttpRequest.get("http://example.com")
				.header(HttpHeaders.IF_MODIFIED_SINCE,
						DateTimeFormatter.RFC_1123_DATE_TIME.format(now))
				.build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		responseMono.writeTo(exchange, DefaultServerResponseBuilderTests.EMPTY_CONTEXT);

		MockServerHttpResponse response = exchange.getResponse();
		assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
		StepVerifier.create(response.getBody())
				.expectError(IllegalStateException.class)
				.verify();
	}


}
