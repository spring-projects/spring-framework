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
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link AbstractMappingContentTypeResolver}.
 * @author Rossen Stoyanchev
 */
public class MappingContentTypeResolverTests {

	@Test
	public void resolveExtensions() {
		Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		TestMappingContentTypeResolver resolver = new TestMappingContentTypeResolver("", mapping);
		Set<String> keys = resolver.getKeysFor(MediaType.APPLICATION_JSON);

		assertEquals(1, keys.size());
		assertEquals("json", keys.iterator().next());
	}

	@Test
	public void resolveExtensionsNoMatch() {
		Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		TestMappingContentTypeResolver resolver = new TestMappingContentTypeResolver("", mapping);
		Set<String> keys = resolver.getKeysFor(MediaType.TEXT_HTML);

		assertTrue(keys.isEmpty());
	}

	@Test // SPR-13747
	public void lookupMediaTypeCaseInsensitive() {
		Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		TestMappingContentTypeResolver resolver = new TestMappingContentTypeResolver("", mapping);
		MediaType mediaType = resolver.getMediaType("JSoN");

		assertEquals(mediaType, MediaType.APPLICATION_JSON);
	}

	@Test
	public void resolveMediaTypes() throws Exception {
		Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		TestMappingContentTypeResolver resolver = new TestMappingContentTypeResolver("json", mapping);
		List<MediaType> mediaTypes = resolver.resolveMediaTypes((ServerWebExchange) null);

		assertEquals(1, mediaTypes.size());
		assertEquals("application/json", mediaTypes.get(0).toString());
	}

	@Test
	public void resolveMediaTypesNoMatch() throws Exception {
		TestMappingContentTypeResolver resolver = new TestMappingContentTypeResolver("blah", null);
		List<MediaType> mediaTypes = resolver.resolveMediaTypes((ServerWebExchange) null);

		assertEquals(0, mediaTypes.size());
	}

	@Test
	public void resolveMediaTypesNoKey() throws Exception {
		Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		TestMappingContentTypeResolver resolver = new TestMappingContentTypeResolver(null, mapping);
		List<MediaType> mediaTypes = resolver.resolveMediaTypes((ServerWebExchange) null);

		assertEquals(0, mediaTypes.size());
	}

	@Test
	public void resolveMediaTypesHandleNoMatch() throws Exception {
		TestMappingContentTypeResolver resolver = new TestMappingContentTypeResolver("xml", null);
		List<MediaType> mediaTypes = resolver.resolveMediaTypes((ServerWebExchange) null);

		assertEquals(1, mediaTypes.size());
		assertEquals("application/xml", mediaTypes.get(0).toString());
	}


	private static class TestMappingContentTypeResolver extends AbstractMappingContentTypeResolver {

		private final String key;

		public TestMappingContentTypeResolver(String key, Map<String, MediaType> mapping) {
			super(mapping);
			this.key = key;
		}

		@Override
		protected String extractKey(ServerWebExchange exchange) {
			return this.key;
		}

		@Override
		protected MediaType handleNoMatch(String mappingKey) {
			return "xml".equals(mappingKey) ? MediaType.APPLICATION_XML : null;
		}
	}

}
