/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ContextResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.UrlResource;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link ResourceHandlerUtils}.
 */
class ResourceHandlerUtilsTests {

	@Test
	@SuppressWarnings("removal")
	void assertResourceLocation() throws Exception {
		assertThatNoException().isThrownBy(() ->
				ResourceHandlerUtils.assertResourceLocation(new ClassPathResource("test/")));

		assertThatNoException().isThrownBy(() ->
				ResourceHandlerUtils.assertResourceLocation(new FileSystemResource("test/")));

		assertThatNoException().isThrownBy(() ->
				ResourceHandlerUtils.assertResourceLocation(new PathResource("test/")));

		assertThatNoException().isThrownBy(() ->
				ResourceHandlerUtils.assertResourceLocation(new UrlResource("file:/test/")));

		assertThatNoException().isThrownBy(() ->
				ResourceHandlerUtils.assertResourceLocation(new TestContextResource("/test/")));
	}

	@Test
	void assertResourceLocationShouldRejectNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				ResourceHandlerUtils.assertResourceLocation(null))
				.withMessage("Resource location must not be null");
	}

	@Test
	void assertResourceLocationShouldRejectNotEndWithSlash() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				ResourceHandlerUtils.assertResourceLocation(new ClassPathResource("test")))
				.withMessageContaining("Resource location does not end with slash");
	}

	@Test
	void assertResourceLocationShouldRejectUnsafeClassPathResource() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				ResourceHandlerUtils.assertResourceLocation(new ClassPathResource("")))
				.withMessageContaining("is considered unsafe");

		assertThatIllegalArgumentException().isThrownBy(() ->
				ResourceHandlerUtils.assertResourceLocation(new ClassPathResource("/")))
				.withMessageContaining("is considered unsafe");
	}

	private static class TestContextResource implements ContextResource {

		private final String path;

		TestContextResource(String path) {
			this.path = path;
		}

		@Override
		public String getPathWithinContext() {
			return this.path;
		}

		@Override
		public boolean exists() {
			return false;
		}

		@Override
		public URL getURL() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public URI getURI() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public File getFile() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public long contentLength() throws IOException {
			return 0;
		}

		@Override
		public long lastModified() throws IOException {
			return 0;
		}

		@Override
		public org.springframework.core.io.Resource createRelative(String relativePath) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getFilename() {
			return null;
		}

		@Override
		public String getDescription() {
			return "TestContextResource";
		}

		@Override
		public InputStream getInputStream() throws IOException {
			throw new UnsupportedOperationException();
		}
	}

}
