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
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;

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
	public void render() throws Exception {
		Map<String, Object> model = Collections.singletonMap("foo", "bar");
		Mono<RenderingResponse> result = RenderingResponse.create("view").modelAttributes(model).build();

		MockServerHttpRequest request = MockServerHttpRequest.get("http://localhost").build();
		MockServerHttpResponse mockResponse = new MockServerHttpResponse();
		ServerWebExchange exchange =
				new DefaultServerWebExchange(request, mockResponse, new MockWebSessionManager());
		ViewResolver viewResolver = mock(ViewResolver.class);
		View view = mock(View.class);
		when(viewResolver.resolveViewName("view", Locale.ENGLISH)).thenReturn(Mono.just(view));
		when(view.render(model, null, exchange)).thenReturn(Mono.empty());

		List<ViewResolver> viewResolvers = new ArrayList<>();
		viewResolvers.add(viewResolver);

		HandlerStrategies mockConfig = mock(HandlerStrategies.class);
		when(mockConfig.viewResolvers()).thenReturn(viewResolvers::stream);

		StepVerifier.create(result)
				.expectNextMatches(response -> "view".equals(response.name()) &&
						model.equals(response.model()))
				.expectComplete()
				.verify();
	}

}
