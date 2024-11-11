/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.context.support;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.Resource;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ServletContextResource}.
 *
 * @author Chris Beams
 * @author Brian Clozel
 */
class ServletContextResourceTests {

	private static final String TEST_RESOURCE_PATH = "org/springframework/web/context/support/resource.txt";

	private final MockServletContext servletContext = new MockServletContext();

	@Test
	void resourceShouldHaveExpectedProperties() throws IOException {
		Resource resource = new ServletContextResource(this.servletContext, TEST_RESOURCE_PATH);

		assertThat(resource.getFile()).isNotNull();
		assertThat(resource.exists()).isTrue();
		assertThat(resource.isFile()).isTrue();
		assertThat(resource.getFilename()).isEqualTo("resource.txt");
		assertThat(resource.getURL().getFile()).endsWith("resource.txt");
	}

	@Test
	void relativeResourcesShouldHaveExpectedProperties() throws IOException {
		Resource resource = new ServletContextResource(this.servletContext, TEST_RESOURCE_PATH);
		Resource relative1 = resource.createRelative("relative.txt");
		assertThat(relative1.getFilename()).isEqualTo("relative.txt");
		assertThat(relative1.getURL().getFile()).endsWith("relative.txt");
		assertThat(relative1.exists()).isTrue();

		Resource relative2 = resource.createRelative("folder/other.txt");
		assertThat(relative2.getFilename()).isEqualTo("other.txt");
		assertThat(relative2.getURL().getFile()).endsWith("other.txt");
		assertThat(relative2.exists()).isTrue();
	}

	@Test
	void resourceWithDotPathShouldBeEqual() {
		Resource resource = new ServletContextResource(this.servletContext, TEST_RESOURCE_PATH);
		assertThat(new ServletContextResource(servletContext, "org/springframework/web/context/../context/support/./resource.txt")).isEqualTo(resource);
	}

	@Test
	void resourceWithRelativePathShouldBeEqual() throws IOException {
		Resource resource = new ServletContextResource(this.servletContext, "dir/");
		Resource relative = resource.createRelative("subdir");
		assertThat(relative).isEqualTo(new ServletContextResource(this.servletContext, "dir/subdir"));
	}

	@Test
	void missingResourceShouldHaveExpectedProperties() {
		MockServletContext context = mock();
		given(context.getRealPath(eq("/org/springframework/web/context/support/missing.txt")))
				.willReturn(this.servletContext.getRealPath("org/springframework/web/context/support/") + "missing.txt");
		Resource missing = new ServletContextResource(context, "org/springframework/web/context/support/missing.txt");

		assertThat(missing.exists()).isFalse();
		assertThat(missing.isFile()).isFalse();
	}
}
