/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.reactive.accept;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link ParameterContentTypeResolver}.
 * @author Rossen Stoyanchev
 */
public class ParameterContentTypeResolverTests {

	@Test
	public void noKey() {
		ParameterContentTypeResolver resolver = new ParameterContentTypeResolver(Collections.emptyMap());
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);

		assertThat(mediaTypes).isEqualTo(RequestedContentTypeResolver.MEDIA_TYPE_ALL_LIST);
	}

	@Test
	public void noMatchForKey() {
		ParameterContentTypeResolver resolver = new ParameterContentTypeResolver(Collections.emptyMap());
		assertThatExceptionOfType(NotAcceptableStatusException.class).isThrownBy(() ->
				resolver.resolveMediaTypes(createExchange("blah")));
	}

	@Test
	public void resolveKeyFromRegistrations() {
		ServerWebExchange exchange = createExchange("html");

		Map<String, MediaType> mapping = Collections.emptyMap();
		RequestedContentTypeResolver resolver = new ParameterContentTypeResolver(mapping);
		List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);
		assertThat(mediaTypes).isEqualTo(Collections.singletonList(new MediaType("text", "html")));

		mapping = Collections.singletonMap("HTML", MediaType.APPLICATION_XHTML_XML);
		resolver = new ParameterContentTypeResolver(mapping);
		mediaTypes = resolver.resolveMediaTypes(exchange);
		assertThat(mediaTypes).isEqualTo(Collections.singletonList(new MediaType("application", "xhtml+xml")));
	}

	@Test
	public void resolveKeyThroughMediaTypeFactory() {
		ServerWebExchange exchange = createExchange("xls");
		RequestedContentTypeResolver resolver = new ParameterContentTypeResolver(Collections.emptyMap());
		List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);

		assertThat(mediaTypes).isEqualTo(Collections.singletonList(new MediaType("application", "vnd.ms-excel")));
	}

	@Test // SPR-13747
	public void resolveKeyIsCaseInsensitive() {
		ServerWebExchange exchange = createExchange("JSoN");
		Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		ParameterContentTypeResolver resolver = new ParameterContentTypeResolver(mapping);
		List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);

		assertThat(mediaTypes).isEqualTo(Collections.singletonList(MediaType.APPLICATION_JSON));
	}

	private MockServerWebExchange createExchange(String format) {
		return MockServerWebExchange.from(MockServerHttpRequest.get("/path?format=" + format));
	}

}
