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

	private final Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
	private final MappingMediaTypeFileExtensionResolver resolver = new MappingMediaTypeFileExtensionResolver(this.mapping);

	@Test
	public void resolveExtensions() {
		List<String> extensions = this.resolver.resolveFileExtensions(MediaType.APPLICATION_JSON);

		assertThat(extensions).size().isEqualTo(1);
		assertThat(extensions.get(0)).isEqualTo("json");
	}

	@Test
	public void resolveExtensionsNoMatch() {
		List<String> extensions = this.resolver.resolveFileExtensions(MediaType.TEXT_HTML);

		assertThat(extensions).isEmpty();
	}

	/**
	 * Unit test for SPR-13747 - ensures that reverse lookup of media type from media
	 * type key is case-insensitive.
	 */
	@Test
	public void lookupMediaTypeCaseInsensitive() {
		MediaType mediaType = this.resolver.lookupMediaType("JSON");

		assertThat(mediaType).isEqualTo(MediaType.APPLICATION_JSON);
	}

}
