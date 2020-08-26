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

package org.springframework.web.context.support;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.Resource;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Chris Beams
 * @see org.springframework.core.io.ResourceTests
 */
public class ResourceTests {

	@Test
	public void testServletContextResource() throws IOException {
		MockServletContext sc = new MockServletContext();
		Resource resource = new ServletContextResource(sc, "org/springframework/core/io/Resource.class");
		doTestResource(resource);
		assertThat(new ServletContextResource(sc, "org/springframework/core/../core/io/./Resource.class")).isEqualTo(resource);
	}

	@Test
	public void testServletContextResourceWithRelativePath() throws IOException {
		MockServletContext sc = new MockServletContext();
		Resource resource = new ServletContextResource(sc, "dir/");
		Resource relative = resource.createRelative("subdir");
		assertThat(relative).isEqualTo(new ServletContextResource(sc, "dir/subdir"));
	}

	private void doTestResource(Resource resource) throws IOException {
		assertThat(resource.getFilename()).isEqualTo("Resource.class");
		assertThat(resource.getURL().getFile().endsWith("Resource.class")).isTrue();

		Resource relative1 = resource.createRelative("ClassPathResource.class");
		assertThat(relative1.getFilename()).isEqualTo("ClassPathResource.class");
		assertThat(relative1.getURL().getFile().endsWith("ClassPathResource.class")).isTrue();
		assertThat(relative1.exists()).isTrue();

		Resource relative2 = resource.createRelative("support/ResourcePatternResolver.class");
		assertThat(relative2.getFilename()).isEqualTo("ResourcePatternResolver.class");
		assertThat(relative2.getURL().getFile().endsWith("ResourcePatternResolver.class")).isTrue();
		assertThat(relative2.exists()).isTrue();
	}
}
