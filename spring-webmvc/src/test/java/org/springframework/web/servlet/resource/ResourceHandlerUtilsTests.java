/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.resource;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.testfixture.servlet.MockServletContext;

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
				ResourceHandlerUtils.assertResourceLocation(new ServletContextResource(new MockServletContext(), "/test/")));
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

}
