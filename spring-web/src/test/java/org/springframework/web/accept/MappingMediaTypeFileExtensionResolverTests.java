/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
class MappingMediaTypeFileExtensionResolverTests {

	private static final Map<String, MediaType> DEFAULT_MAPPINGS =
			Collections.singletonMap("json", MediaType.APPLICATION_JSON);


	@Test
	void resolveExtensions() {
		List<String> extensions = new MappingMediaTypeFileExtensionResolver(DEFAULT_MAPPINGS)
				.resolveFileExtensions(MediaType.APPLICATION_JSON);

		assertThat(extensions).containsExactly("json");
	}

	@Test
	void resolveExtensionsNoMatch() {
		assertThat(new MappingMediaTypeFileExtensionResolver(DEFAULT_MAPPINGS)
				.resolveFileExtensions(MediaType.TEXT_HTML)).isEmpty();
	}

	@Test
	void resolveExtensionsWithQualityParameter() {
		List<String> extensions = new MappingMediaTypeFileExtensionResolver(DEFAULT_MAPPINGS)
				.resolveFileExtensions(MediaType.parseMediaType("application/json;q=0.9"));

		assertThat(extensions).containsExactly("json");
	}

	@Test // SPR-13747
	void lookupMediaTypeCaseInsensitive() {
		assertThat(new MappingMediaTypeFileExtensionResolver(DEFAULT_MAPPINGS).lookupMediaType("JSON"))
				.isEqualTo(MediaType.APPLICATION_JSON);
	}

	@Test
	void mappingsAreCaseInsensitive() {
		Map<String, MediaType> map = new HashMap<>();
		map.put("json", MediaType.APPLICATION_JSON);
		map.put("JsOn", MediaType.APPLICATION_JSON);
		map.put("jSoN", MediaType.APPLICATION_JSON);

		MappingMediaTypeFileExtensionResolver resolver = new MappingMediaTypeFileExtensionResolver(map);
		map.forEach(resolver::addMapping);

		assertThat(resolver.getAllFileExtensions()).containsExactly("json");
	}


}
