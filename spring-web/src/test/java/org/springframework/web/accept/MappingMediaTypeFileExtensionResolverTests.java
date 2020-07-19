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

package org.springframework.web.accept;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link MappingMediaTypeFileExtensionResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Melissa Hartsock
 */
public class MappingMediaTypeFileExtensionResolverTests {

	private static final Map<String, MediaType> DEFAULT_MAPPINGS =
			Collections.singletonMap("json", MediaType.APPLICATION_JSON);


	@Test
	public void resolveExtensions() {
		List<String> extensions = new MappingMediaTypeFileExtensionResolver(DEFAULT_MAPPINGS)
				.resolveFileExtensions(MediaType.APPLICATION_JSON);

		assertThat(extensions).hasSize(1);
		assertThat(extensions.get(0)).isEqualTo("json");
	}

	@Test
	public void resolveExtensionsNoMatch() {
		assertThat(new MappingMediaTypeFileExtensionResolver(DEFAULT_MAPPINGS)
				.resolveFileExtensions(MediaType.TEXT_HTML)).isEmpty();
	}

	@Test // SPR-13747
	public void lookupMediaTypeCaseInsensitive() {
		assertThat(new MappingMediaTypeFileExtensionResolver(DEFAULT_MAPPINGS).lookupMediaType("JSON"))
				.isEqualTo(MediaType.APPLICATION_JSON);
	}

	@Test
	public void allFileExtensions() {
		Map<String, MediaType> mappings = new HashMap<>();
		mappings.put("json", MediaType.APPLICATION_JSON);
		mappings.put("JsOn", MediaType.APPLICATION_JSON);
		mappings.put("jSoN", MediaType.APPLICATION_JSON);

		MappingMediaTypeFileExtensionResolver resolver = new MappingMediaTypeFileExtensionResolver(mappings);
		assertThat(resolver.getAllFileExtensions()).containsExactly("json");
	}
}
