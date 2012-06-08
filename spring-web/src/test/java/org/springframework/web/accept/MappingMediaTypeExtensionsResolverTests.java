/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.web.accept;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.http.MediaType;

/**
 * Test fixture for MappingMediaTypeExtensionsResolver.
 *
 * @author Rossen Stoyanchev
 */
public class MappingMediaTypeExtensionsResolverTests {

	@Test
	public void resolveExtensions() {
		Map<String, String> mapping = Collections.singletonMap("json", "application/json");
		MappingMediaTypeExtensionsResolver resolver = new MappingMediaTypeExtensionsResolver(mapping);
		List<String> extensions = resolver.resolveExtensions(MediaType.APPLICATION_JSON);

		assertEquals(1, extensions.size());
		assertEquals("json", extensions.get(0));
	}

}
