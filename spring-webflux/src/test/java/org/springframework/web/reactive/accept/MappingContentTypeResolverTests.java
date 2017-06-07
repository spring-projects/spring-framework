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
import org.springframework.lang.Nullable;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link AbstractMappingContentTypeResolver}.
 * @author Rossen Stoyanchev
 */
public class MappingContentTypeResolverTests {

	@Test // SPR-13747
	public void resolveCaseInsensitive() {
		Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		TestMappingContentTypeResolver resolver = new TestMappingContentTypeResolver("JSoN", mapping);
		List<MediaType> mediaTypes = resolver.resolve();

		assertEquals(Collections.singletonList(MediaType.APPLICATION_JSON), mediaTypes);
	}

	@Test
	public void resolveMediaTypes() throws Exception {
		Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		TestMappingContentTypeResolver resolver = new TestMappingContentTypeResolver("json", mapping);
		List<MediaType> mediaTypes = resolver.resolve();

		assertEquals(1, mediaTypes.size());
		assertEquals("application/json", mediaTypes.get(0).toString());
	}

	@Test
	public void resolveNoMatch() throws Exception {
		TestMappingContentTypeResolver resolver = new TestMappingContentTypeResolver("blah", Collections.emptyMap());
		List<MediaType> mediaTypes = resolver.resolve();

		assertEquals(0, mediaTypes.size());
	}

	@Test
	public void resolveNoKey() throws Exception {
		Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		TestMappingContentTypeResolver resolver = new TestMappingContentTypeResolver(null, mapping);
		List<MediaType> mediaTypes = resolver.resolve();

		assertEquals(0, mediaTypes.size());
	}

	@Test
	public void resolveMediaTypesHandleNoMatch() throws Exception {
		TestMappingContentTypeResolver resolver = new TestMappingContentTypeResolver("xml", Collections.emptyMap());
		List<MediaType> mediaTypes = resolver.resolve();

		assertEquals(1, mediaTypes.size());
		assertEquals("application/xml", mediaTypes.get(0).toString());
	}


	private static class TestMappingContentTypeResolver extends AbstractMappingContentTypeResolver {

		private final String key;

		TestMappingContentTypeResolver(@Nullable String key, Map<String, MediaType> mapping) {
			super(mapping);
			this.key = key;
		}

		public List<MediaType> resolve() throws NotAcceptableStatusException {
			return super.resolveMediaTypes(MockServerHttpRequest.get("/").toExchange());
		}

		@Override
		protected String getKey(ServerWebExchange exchange) {
			return this.key;
		}
	}

}
