/*
 * Copyright 2002-present the original author or authors.
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

	@Test
	void assertResourceLocationShouldRejectUnsafeContextResource() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				ResourceHandlerUtils.assertResourceLocation(new ServletContextResource(new MockServletContext(), "/")))
				.withMessageContaining("is considered unsafe");
	}

}
