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

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerWebExchange;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link RequestedContentTypeResolverBuilder}.
 *
 * @author Rossen Stoyanchev
 */
public class CompositeContentTypeResolverBuilderTests {

	@Test
	public void defaultSettings() throws Exception {
		RequestedContentTypeResolver resolver = new RequestedContentTypeResolverBuilder().build();

		MockServerWebExchange exchange = MockServerHttpRequest.get("/flower.gif").toExchange();

		assertEquals("Should be able to resolve file extensions by default",
				Collections.singletonList(MediaType.IMAGE_GIF), resolver.resolveMediaTypes(exchange));

		exchange = MockServerHttpRequest.get("/flower.xyz").toExchange();

		assertEquals("Should ignore unknown extensions by default",
				Collections.<MediaType>emptyList(), resolver.resolveMediaTypes(exchange));

		exchange = MockServerHttpRequest.get("/flower?format=gif").toExchange();

		assertEquals("Should not resolve request parameters by default",
				Collections.<MediaType>emptyList(), resolver.resolveMediaTypes(exchange));

		exchange = MockServerHttpRequest.get("/flower").accept(MediaType.IMAGE_GIF).toExchange();

		assertEquals("Should resolve Accept header by default",
				Collections.singletonList(MediaType.IMAGE_GIF), resolver.resolveMediaTypes(exchange));
	}

	@Test
	public void favorPath() throws Exception {
		RequestedContentTypeResolver resolver = new RequestedContentTypeResolverBuilder()
				.favorPathExtension(true)
				.mediaType("foo", new MediaType("application", "foo"))
				.mediaType("bar", new MediaType("application", "bar"))
				.build();

		ServerWebExchange exchange = MockServerHttpRequest.get("/flower.foo").toExchange();
		assertEquals(Collections.singletonList(new MediaType("application", "foo")),
				resolver.resolveMediaTypes(exchange));

		exchange = MockServerHttpRequest.get("/flower.bar").toExchange();
		assertEquals(Collections.singletonList(new MediaType("application", "bar")),
				resolver.resolveMediaTypes(exchange));

		exchange = MockServerHttpRequest.get("/flower.gif").toExchange();
		assertEquals(Collections.singletonList(MediaType.IMAGE_GIF), resolver.resolveMediaTypes(exchange));
	}

	@Test
	public void favorPathWithJafTurnedOff() throws Exception {
		RequestedContentTypeResolver resolver = new RequestedContentTypeResolverBuilder()
				.favorPathExtension(true)
				.useJaf(false)
				.build();

		ServerWebExchange exchange = MockServerHttpRequest.get("/flower.foo").toExchange();
		assertEquals(Collections.emptyList(), resolver.resolveMediaTypes(exchange));

		exchange = MockServerHttpRequest.get("/flower.gif").toExchange();
		assertEquals(Collections.emptyList(), resolver.resolveMediaTypes(exchange));
	}

	@Test(expected = NotAcceptableStatusException.class) // SPR-10170
	public void favorPathWithIgnoreUnknownPathExtensionTurnedOff() throws Exception {
		RequestedContentTypeResolver resolver = new RequestedContentTypeResolverBuilder()
				.favorPathExtension(true)
				.ignoreUnknownPathExtensions(false)
				.build();

		ServerWebExchange exchange = MockServerHttpRequest.get("/flower.xyz?format=json").toExchange();
		resolver.resolveMediaTypes(exchange);
	}

	@Test
	public void favorParameter() throws Exception {
		RequestedContentTypeResolver resolver = new RequestedContentTypeResolverBuilder()
				.favorParameter(true)
				.mediaType("json", MediaType.APPLICATION_JSON)
				.build();

		ServerWebExchange exchange = MockServerHttpRequest.get("/flower?format=json").toExchange();

		assertEquals(Collections.singletonList(MediaType.APPLICATION_JSON), resolver.resolveMediaTypes(exchange));
	}

	@Test(expected = NotAcceptableStatusException.class) // SPR-10170
	public void favorParameterWithUnknownMediaType() throws Exception {
		RequestedContentTypeResolver resolver = new RequestedContentTypeResolverBuilder()
				.favorParameter(true)
				.build();

		ServerWebExchange exchange = MockServerHttpRequest.get("/flower?format=xyz").toExchange();
		resolver.resolveMediaTypes(exchange);
	}

	@Test
	public void ignoreAcceptHeader() throws Exception {
		RequestedContentTypeResolver resolver = new RequestedContentTypeResolverBuilder()
				.ignoreAcceptHeader(true)
				.build();

		ServerWebExchange exchange = MockServerHttpRequest.get("/flower").accept(MediaType.IMAGE_GIF).toExchange();

		assertEquals(Collections.<MediaType>emptyList(), resolver.resolveMediaTypes(exchange));
	}

	@Test // SPR-10513
	public void setDefaultContentType() throws Exception {
		RequestedContentTypeResolver resolver = new RequestedContentTypeResolverBuilder()
				.defaultContentType(MediaType.APPLICATION_JSON)
				.build();

		ServerWebExchange exchange = MockServerHttpRequest.get("/").accept(MediaType.ALL).toExchange();

		assertEquals(Collections.singletonList(MediaType.APPLICATION_JSON), resolver.resolveMediaTypes(exchange));
	}

	@Test // SPR-12286
	public void setDefaultContentTypeWithStrategy() throws Exception {
		RequestedContentTypeResolver resolver = new RequestedContentTypeResolverBuilder()
				.defaultContentTypeResolver(new FixedContentTypeResolver(MediaType.APPLICATION_JSON))
				.build();

		List<MediaType> expected = Collections.singletonList(MediaType.APPLICATION_JSON);

		ServerWebExchange exchange = MockServerHttpRequest.get("/").toExchange();
		assertEquals(expected, resolver.resolveMediaTypes(exchange));

		exchange = MockServerHttpRequest.get("/").accept(MediaType.ALL).toExchange();
		assertEquals(expected, resolver.resolveMediaTypes(exchange));
	}


}
