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

import org.junit.Test;

import org.springframework.core.io.Resource;
import org.springframework.mock.web.test.MockServletContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
		assertEquals(resource, new ServletContextResource(sc, "org/springframework/core/../core/io/./Resource.class"));
	}

	@Test
	public void testServletContextResourceWithRelativePath() throws IOException {
		MockServletContext sc = new MockServletContext();
		Resource resource = new ServletContextResource(sc, "dir/");
		Resource relative = resource.createRelative("subdir");
		assertEquals(new ServletContextResource(sc, "dir/subdir"), relative);
	}

	private void doTestResource(Resource resource) throws IOException {
		assertEquals("Resource.class", resource.getFilename());
		assertTrue(resource.getURL().getFile().endsWith("Resource.class"));

		Resource relative1 = resource.createRelative("ClassPathResource.class");
		assertEquals("ClassPathResource.class", relative1.getFilename());
		assertTrue(relative1.getURL().getFile().endsWith("ClassPathResource.class"));
		assertTrue(relative1.exists());

		Resource relative2 = resource.createRelative("support/ResourcePatternResolver.class");
		assertEquals("ResourcePatternResolver.class", relative2.getFilename());
		assertTrue(relative2.getURL().getFile().endsWith("ResourcePatternResolver.class"));
		assertTrue(relative2.exists());
	}
}
