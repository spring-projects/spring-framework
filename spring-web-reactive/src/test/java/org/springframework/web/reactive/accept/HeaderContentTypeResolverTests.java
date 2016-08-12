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
package org.springframework.web.reactive.accept;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link HeaderContentTypeResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class HeaderContentTypeResolverTests {

	private HeaderContentTypeResolver resolver;


	@Before
	public void setup() {
		this.resolver = new HeaderContentTypeResolver();
	}


	@Test
	public void resolveMediaTypes() throws Exception {
		ServerWebExchange exchange = createExchange("text/plain; q=0.5, text/html, text/x-dvi; q=0.8, text/x-c");
		List<MediaType> mediaTypes = this.resolver.resolveMediaTypes(exchange);

		assertEquals(4, mediaTypes.size());
		assertEquals("text/html", mediaTypes.get(0).toString());
		assertEquals("text/x-c", mediaTypes.get(1).toString());
		assertEquals("text/x-dvi;q=0.8", mediaTypes.get(2).toString());
		assertEquals("text/plain;q=0.5", mediaTypes.get(3).toString());
	}

	@Test(expected = NotAcceptableStatusException.class)
	public void resolveMediaTypesParseError() throws Exception {
		ServerWebExchange exchange = createExchange("textplain; q=0.5");
		this.resolver.resolveMediaTypes(exchange);
	}


	private ServerWebExchange createExchange(String accept) throws URISyntaxException {
		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI("/"));
		if (accept != null) {
			request.getHeaders().add("Accept", accept);
		}
		WebSessionManager sessionManager = new MockWebSessionManager();
		return new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);
	}

}
