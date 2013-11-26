/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.support.converter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeTypeUtils;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link DefaultContentTypeResolver}.
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
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON);
		MessageHeaders headers = new MessageHeaders(map);

		assertEquals(MimeTypeUtils.APPLICATION_JSON, this.resolver.resolve(headers));
	}

	@Test
	public void resolveNoContentTypeHeader() {
		MessageHeaders headers = new MessageHeaders(Collections.<String, Object>emptyMap());

		assertNull(this.resolver.resolve(headers));
	}

	@Test
	public void resolveFromDefaultMimeType() {
		this.resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
		MessageHeaders headers = new MessageHeaders(Collections.<String, Object>emptyMap());

		assertEquals(MimeTypeUtils.APPLICATION_JSON, this.resolver.resolve(headers));
	}

}
