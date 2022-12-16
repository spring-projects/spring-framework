/*
 * Copyright 2002-2022 the original author or authors.
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

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Simon Baslé
 */
class ResourceEntityResolverTests {

	@Test
	void resolveEntityCallsFallbackWithNullOnDtd() throws IOException, SAXException {
		ResourceEntityResolver resolver = new FallingBackEntityResolver(false, null);

		assertThat(resolver.resolveEntity("testPublicId", "https://example.org/exampleschema.dtd"))
				.isNull();
	}

	@Test
	void resolveEntityCallsFallbackWithNullOnXsd() throws IOException, SAXException {
		ResourceEntityResolver resolver = new FallingBackEntityResolver(false, null);

		assertThat(resolver.resolveEntity("testPublicId", "https://example.org/exampleschema.xsd"))
				.isNull();
	}

	@Test
	void resolveEntityCallsFallbackWithThrowOnDtd() {
		ResourceEntityResolver resolver = new FallingBackEntityResolver(true, null);

		assertThatIllegalStateException().isThrownBy(
						() -> resolver.resolveEntity("testPublicId", "https://example.org/exampleschema.dtd"))
				.withMessage("FallingBackEntityResolver that throws");
	}

	@Test
	void resolveEntityCallsFallbackWithThrowOnXsd() {
		ResourceEntityResolver resolver = new FallingBackEntityResolver(true, null);

		assertThatIllegalStateException().isThrownBy(
						() -> resolver.resolveEntity("testPublicId", "https://example.org/exampleschema.xsd"))
				.withMessage("FallingBackEntityResolver that throws");
	}

	@Test
	void resolveEntityCallsFallbackWithInputSourceOnDtd() throws IOException, SAXException {
		InputSource expected = Mockito.mock(InputSource.class);
		ResourceEntityResolver resolver = new FallingBackEntityResolver(false, expected);

		assertThat(resolver.resolveEntity("testPublicId", "https://example.org/exampleschema.dtd"))
				.isNotNull()
				.isSameAs(expected);
	}

	@Test
	void resolveEntityCallsFallbackWithInputSourceOnXsd() throws IOException, SAXException {
		InputSource expected = Mockito.mock(InputSource.class);
		ResourceEntityResolver resolver = new FallingBackEntityResolver(false, expected);

		assertThat(resolver.resolveEntity("testPublicId", "https://example.org/exampleschema.xsd"))
				.isNotNull()
				.isSameAs(expected);
	}

	@Test
	void resolveEntityDoesntCallFallbackIfNotSchema() throws IOException, SAXException {
		ResourceEntityResolver resolver = new FallingBackEntityResolver(true, null);

		assertThat(resolver.resolveEntity("testPublicId", "https://example.org/example.xml"))
				.isNull();
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

	private static class FallingBackEntityResolver extends ResourceEntityResolver {

		private final boolean shouldThrow;
		@Nullable
		private final InputSource returnValue;

		private FallingBackEntityResolver(boolean shouldThrow, @Nullable InputSource returnValue) {
			super(new NoOpResourceLoader());
			this.shouldThrow = shouldThrow;
			this.returnValue = returnValue;
		}

		@Nullable
		@Override
		protected InputSource resolveSchemaEntity(String publicId, String systemId) {
			if (shouldThrow) throw new IllegalStateException("FallingBackEntityResolver that throws");
			return this.returnValue;
		}
	}
}
