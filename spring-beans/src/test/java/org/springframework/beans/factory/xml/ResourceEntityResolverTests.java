/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.beans.factory.xml;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.InputSource;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for ResourceEntityResolver.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.0.4
 */
class ResourceEntityResolverTests {

	@ParameterizedTest
	@ValueSource(strings = { "https://example.org/schema/", "https://example.org/schema.xml" })
	void resolveEntityDoesNotCallFallbackIfNotSchema(String systemId) throws Exception {
		ConfigurableFallbackEntityResolver resolver = new ConfigurableFallbackEntityResolver(true);

		assertThat(resolver.resolveEntity("testPublicId", systemId)).isNull();
		assertThat(resolver.fallbackInvoked).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = { "https://example.org/schema.dtd", "https://example.org/schema.xsd" })
	void resolveEntityCallsFallbackThatReturnsNull(String systemId) throws Exception {
		ConfigurableFallbackEntityResolver resolver = new ConfigurableFallbackEntityResolver(null);

		assertThat(resolver.resolveEntity("testPublicId", systemId)).isNull();
		assertThat(resolver.fallbackInvoked).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "https://example.org/schema.dtd", "https://example.org/schema.xsd" })
	void resolveEntityCallsFallbackThatThrowsException(String systemId) {
		ConfigurableFallbackEntityResolver resolver = new ConfigurableFallbackEntityResolver(true);

		assertThatExceptionOfType(ResolutionRejectedException.class)
				.isThrownBy(() -> resolver.resolveEntity("testPublicId", systemId));
		assertThat(resolver.fallbackInvoked).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "https://example.org/schema.dtd", "https://example.org/schema.xsd" })
	void resolveEntityCallsFallbackThatReturnsInputSource(String systemId) throws Exception {
		InputSource expected = mock();
		ConfigurableFallbackEntityResolver resolver = new ConfigurableFallbackEntityResolver(expected);

		assertThat(resolver.resolveEntity("testPublicId", systemId)).isSameAs(expected);
		assertThat(resolver.fallbackInvoked).isTrue();
	}


	private static final class NoOpResourceLoader implements ResourceLoader {

		@Override
		public Resource getResource(String location) {
			return null;
		}

		@Override
		public ClassLoader getClassLoader() {
			return ResourceEntityResolverTests.class.getClassLoader();
		}
	}


	private static class ConfigurableFallbackEntityResolver extends ResourceEntityResolver {

		private final boolean shouldThrow;

		@Nullable
		private final InputSource returnValue;

		boolean fallbackInvoked = false;

		private ConfigurableFallbackEntityResolver(boolean shouldThrow) {
			super(new NoOpResourceLoader());
			this.shouldThrow = shouldThrow;
			this.returnValue = null;
		}

		private ConfigurableFallbackEntityResolver(@Nullable InputSource returnValue) {
			super(new NoOpResourceLoader());
			this.shouldThrow = false;
			this.returnValue = returnValue;
		}

		@Override
		@Nullable
		protected InputSource resolveSchemaEntity(String publicId, String systemId) {
			this.fallbackInvoked = true;
			if (this.shouldThrow) {
				throw new ResolutionRejectedException();
			}
			return this.returnValue;
		}
	}


	@SuppressWarnings("serial")
	static class ResolutionRejectedException extends RuntimeException {}

}
