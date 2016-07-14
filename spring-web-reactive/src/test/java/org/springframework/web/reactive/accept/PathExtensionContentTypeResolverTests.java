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
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
 * Unit tests for {@link PathExtensionContentTypeResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class PathExtensionContentTypeResolverTests {

	@Test
	public void resolveMediaTypesFromMapping() throws Exception {
		ServerWebExchange exchange = createExchange("/test.html");
		PathExtensionContentTypeResolver resolver = new PathExtensionContentTypeResolver();
		List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);

		assertEquals(Collections.singletonList(new MediaType("text", "html")), mediaTypes);

		Map<String, MediaType> mapping = Collections.singletonMap("HTML", MediaType.APPLICATION_XHTML_XML);
		resolver = new PathExtensionContentTypeResolver(mapping);
		mediaTypes = resolver.resolveMediaTypes(exchange);

		assertEquals(Collections.singletonList(new MediaType("application", "xhtml+xml")), mediaTypes);
	}

	@Test
	public void resolveMediaTypesFromJaf() throws Exception {
		ServerWebExchange exchange = createExchange("test.xls");
		PathExtensionContentTypeResolver resolver = new PathExtensionContentTypeResolver();
		List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);

		assertEquals(Collections.singletonList(new MediaType("application", "vnd.ms-excel")), mediaTypes);
	}

	// SPR-10334

	@Test
	public void getMediaTypeFromFilenameNoJaf() throws Exception {
		ServerWebExchange exchange = createExchange("test.json");
		PathExtensionContentTypeResolver resolver = new PathExtensionContentTypeResolver();
		resolver.setUseJaf(false);
		List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);

		assertEquals(Collections.<MediaType>emptyList(), mediaTypes);
	}

	// SPR-9390

	@Test
	public void getMediaTypeFilenameWithEncodedURI() throws Exception {
		ServerWebExchange exchange = createExchange("/quo%20vadis%3f.html");
		PathExtensionContentTypeResolver resolver = new PathExtensionContentTypeResolver();
		List<MediaType> result = resolver.resolveMediaTypes(exchange);

		assertEquals("Invalid content type", Collections.singletonList(new MediaType("text", "html")), result);
	}

	// SPR-10170

	@Test
	public void resolveMediaTypesIgnoreUnknownExtension() throws Exception {
		ServerWebExchange exchange = createExchange("test.xyz");
		PathExtensionContentTypeResolver resolver = new PathExtensionContentTypeResolver();
		List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);

		assertEquals(Collections.<MediaType>emptyList(), mediaTypes);
	}

	@Test(expected = NotAcceptableStatusException.class)
	public void resolveMediaTypesDoNotIgnoreUnknownExtension() throws Exception {
		ServerWebExchange exchange = createExchange("test.xyz");
		PathExtensionContentTypeResolver resolver = new PathExtensionContentTypeResolver();
		resolver.setIgnoreUnknownExtensions(false);
		resolver.resolveMediaTypes(exchange);
	}


	private ServerWebExchange createExchange(String path) throws URISyntaxException {
		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI(path));
		WebSessionManager sessionManager = new MockWebSessionManager();
		return new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);
	}

}
