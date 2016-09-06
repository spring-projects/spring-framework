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

package org.springframework.web.reactive.function;

import java.net.URI;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Arjen Poutsma
 */
public class RenderingResponseTests {

	private final Map<String, Object> model = Collections.singletonMap("foo", "bar");

	private final RenderingResponse renderingResponse = new RenderingResponse(200, new HttpHeaders(), "view",
			model);

	@Test
	public void body() throws Exception {
		assertEquals("view", renderingResponse.body().name());
		assertEquals(model, renderingResponse.body().model());
	}

	@Test
	public void writeTo() throws Exception {
		MockServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, URI.create("http://localhost"));
		MockServerHttpResponse response = new MockServerHttpResponse();
		ServerWebExchange exchange = new DefaultServerWebExchange(request, response, new MockWebSessionManager());
		ViewResolver viewResolver = mock(ViewResolver.class);
		View view = mock(View.class);
		when(viewResolver.resolveViewName("view", Locale.ENGLISH)).thenReturn(Mono.just(view));
		when(view.render(model, null, exchange)).thenReturn(Mono.empty());
		exchange.getAttributes().put(Router.VIEW_RESOLVERS_ATTRIBUTE,
				(Supplier<Stream<ViewResolver>>) () -> Collections
						.singleton(viewResolver).stream());


		renderingResponse.writeTo(exchange).block();
	}


}