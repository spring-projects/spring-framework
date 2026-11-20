/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.accept;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A test fixture with a test subclass of AbstractMappingContentNegotiationStrategy.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
class MappingContentNegotiationStrategyTests {

	@Test
	void resolveMediaTypesFromRegisteredExtensions() throws Exception {
		Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		TestMappingContentNegotiationStrategy strategy = new TestMappingContentNegotiationStrategy("json", mapping);

		List<MediaType> mediaTypes = strategy.resolveMediaTypes(null);

		assertThat(mediaTypes).hasSize(1);
		assertThat(mediaTypes.get(0).toString()).isEqualTo("application/json");
	}

	@Test
	void resolveMediaTypesViaFactory() throws Exception {
		Map<String, MediaType> mapping = null;
		TestMappingContentNegotiationStrategy strategy = new TestMappingContentNegotiationStrategy("xml", mapping);

		List<MediaType> mediaTypes = strategy.resolveMediaTypes(null);

		assertThat(mediaTypes).hasSize(1);
		assertThat(mediaTypes.get(0).toString()).isEqualTo("application/xml");
	}

	@Test
	void resolveMediaTypesViaFactoryDoesNotAddMapping() throws Exception {
		Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		TestMappingContentNegotiationStrategy strategy = new TestMappingContentNegotiationStrategy("xml", mapping);

		assertThat(strategy.resolveMediaTypes(null)).containsExactly(MediaType.APPLICATION_XML);
		assertThat(strategy.getMediaTypes()).containsOnlyKeys("json");
		assertThat(strategy.getAllFileExtensions()).containsExactly("json");
		assertThat(strategy.resolveFileExtensions(MediaType.APPLICATION_XML)).isEmpty();
	}

	@Test
	void resolveMediaTypesUnknownKey() {
		Map<String, MediaType> mapping = null;
		TestMappingContentNegotiationStrategy strategy = new TestMappingContentNegotiationStrategy("blah", mapping);

		assertThatThrownBy(() -> strategy.resolveMediaTypes(null))
				.isInstanceOf(HttpMediaTypeNotAcceptableException.class);
	}

	@Test
	void resolveMediaTypesInvalidKey() {
		Map<String, MediaType> mapping = null;
		TestMappingContentNegotiationStrategy strategy = new TestMappingContentNegotiationStrategy("not.json", mapping);

		assertThatThrownBy(() -> strategy.resolveMediaTypes(null))
				.isInstanceOf(HttpMediaTypeNotAcceptableException.class);
	}

	@Test
	void resolveMediaTypesNullKey() throws Exception {
		Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		TestMappingContentNegotiationStrategy strategy = new TestMappingContentNegotiationStrategy(null, mapping);

		List<MediaType> mediaTypes = strategy.resolveMediaTypes(null);

		assertThat(mediaTypes).isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);
	}


	private static class TestMappingContentNegotiationStrategy extends AbstractMappingContentNegotiationStrategy {

		private final String extension;

		public TestMappingContentNegotiationStrategy(String extension, Map<String, MediaType> mapping) {
			super(mapping);
			this.extension = extension;
		}

		@Override
		protected String getMediaTypeKey(NativeWebRequest request) {
			return this.extension;
		}

	}

}
