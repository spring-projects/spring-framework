/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.messaging.converter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.messaging.MessageHeaders;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeTypeUtils;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link org.springframework.messaging.converter.DefaultContentTypeResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultContentTypeResolverTests {

	private DefaultContentTypeResolver resolver;


	@Before
	public void setup() {
		this.resolver = new DefaultContentTypeResolver();
	}

	@Test
	public void resolve() {
		Map<String, Object> map = new HashMap<>();
		map.put(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON);
		MessageHeaders headers = new MessageHeaders(map);

		assertEquals(MimeTypeUtils.APPLICATION_JSON, this.resolver.resolve(headers));
	}

	@Test
	public void resolveStringContentType() {
		Map<String, Object> map = new HashMap<>();
		map.put(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE);
		MessageHeaders headers = new MessageHeaders(map);

		assertEquals(MimeTypeUtils.APPLICATION_JSON, this.resolver.resolve(headers));
	}

	@Test(expected = InvalidMimeTypeException.class)
	public void resolveInvalidStringContentType() {
		Map<String, Object> map = new HashMap<>();
		map.put(MessageHeaders.CONTENT_TYPE, "invalidContentType");
		MessageHeaders headers = new MessageHeaders(map);
		this.resolver.resolve(headers);
	}

	@Test(expected = IllegalArgumentException.class)
	public void resolveUnknownHeaderType() {
		Map<String, Object> map = new HashMap<>();
		map.put(MessageHeaders.CONTENT_TYPE, new Integer(1));
		MessageHeaders headers = new MessageHeaders(map);
		this.resolver.resolve(headers);
	}

	@Test
	public void resolveNoContentTypeHeader() {
		MessageHeaders headers = new MessageHeaders(Collections.<String, Object>emptyMap());

		assertNull(this.resolver.resolve(headers));
	}

	@Test
	public void resolveDefaultMimeType() {
		this.resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
		MessageHeaders headers = new MessageHeaders(Collections.<String, Object>emptyMap());

		assertEquals(MimeTypeUtils.APPLICATION_JSON, this.resolver.resolve(headers));
	}

}
