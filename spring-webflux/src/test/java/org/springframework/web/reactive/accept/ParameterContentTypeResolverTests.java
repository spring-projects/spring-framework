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
package org.springframework.web.reactive.accept;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link ParameterContentTypeResolver}.
 * @author Rossen Stoyanchev
 */
public class ParameterContentTypeResolverTests {

	@Test
	public void noKey() throws Exception {
		ParameterContentTypeResolver resolver = new ParameterContentTypeResolver(Collections.emptyMap());
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);

		assertEquals(0, mediaTypes.size());
	}

	@Test(expected = NotAcceptableStatusException.class)
	public void noMatchForKey() throws Exception {
		ParameterContentTypeResolver resolver = new ParameterContentTypeResolver(Collections.emptyMap());
		List<MediaType> mediaTypes = resolver.resolveMediaTypes(createExchange("blah"));

		assertEquals(0, mediaTypes.size());
	}

	@Test
	public void resolveKeyFromRegistrations() throws Exception {
		ServerWebExchange exchange = createExchange("html");

		Map<String, MediaType> mapping = Collections.emptyMap();
		RequestedContentTypeResolver resolver = new ParameterContentTypeResolver(mapping);
		List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);
		assertEquals(Collections.singletonList(new MediaType("text", "html")), mediaTypes);

		mapping = Collections.singletonMap("HTML", MediaType.APPLICATION_XHTML_XML);
		resolver = new ParameterContentTypeResolver(mapping);
		mediaTypes = resolver.resolveMediaTypes(exchange);
		assertEquals(Collections.singletonList(new MediaType("application", "xhtml+xml")), mediaTypes);
	}

	@Test
	public void resolveKeyThroughMediaTypeFactory() throws Exception {
		ServerWebExchange exchange = createExchange("xls");
		RequestedContentTypeResolver resolver = new ParameterContentTypeResolver(Collections.emptyMap());
		List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);

		assertEquals(Collections.singletonList(new MediaType("application", "vnd.ms-excel")), mediaTypes);
	}

	@Test // SPR-13747
	public void resolveKeyIsCaseInsensitive() {
		ServerWebExchange exchange = createExchange("JSoN");
		Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		ParameterContentTypeResolver resolver = new ParameterContentTypeResolver(mapping);
		List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);

		assertEquals(Collections.singletonList(MediaType.APPLICATION_JSON), mediaTypes);
	}

	private MockServerWebExchange createExchange(String format) {
		return MockServerWebExchange.from(MockServerHttpRequest.get("/path?format=" + format));
	}

}
