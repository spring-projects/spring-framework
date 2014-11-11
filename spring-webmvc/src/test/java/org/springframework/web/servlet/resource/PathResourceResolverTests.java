/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.web.servlet.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

/**
 * Unit tests for
 * {@link org.springframework.web.servlet.resource.PathResourceResolver}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 */
public class PathResourceResolverTests {

	private PathResourceResolver resolver;


	@Before
	public void setup() {
		this.resolver = new PathResourceResolver();
	}

	@Test
	public void resolveFromClasspath() throws IOException {
		Resource location = new ClassPathResource("test/", PathResourceResolver.class);
		String requestPath = "bar.css";
		Resource actual = this.resolver.resolveResource(null, requestPath, Arrays.asList(location), null);
		assertEquals(location.createRelative(requestPath), actual);
	}

	@Test
	public void resolveFromClasspathRoot() throws IOException {
		Resource location = new ClassPathResource("/");
		String requestPath = "org/springframework/web/servlet/resource/test/bar.css";
		Resource actual = this.resolver.resolveResource(null, requestPath, Arrays.asList(location), null);
		assertNotNull(actual);
	}

	@Test
	public void checkResource() throws IOException {
		Resource location = new ClassPathResource("test/", PathResourceResolver.class);
		testCheckResource(location, "../testsecret/secret.txt");
		testCheckResource(location, "test/../../testsecret/secret.txt");

		location = new UrlResource(getClass().getResource("./test/"));
		String secretPath = new UrlResource(getClass().getResource("testsecret/secret.txt")).getURL().getPath();
		testCheckResource(location, "file:" + secretPath);
		testCheckResource(location, "/file:" + secretPath);
		testCheckResource(location, "/" + secretPath);
		testCheckResource(location, "////../.." + secretPath);
		testCheckResource(location, "/%2E%2E/testsecret/secret.txt");
		testCheckResource(location, "/%2e%2e/testsecret/secret.txt");
		testCheckResource(location, " " + secretPath);
		testCheckResource(location, "/  " + secretPath);
		testCheckResource(location, "url:" + secretPath);
	}

	@Test
	public void checkResourceWithAllowedLocations() {
		this.resolver.setAllowedLocations(
				new ClassPathResource("test/", PathResourceResolver.class),
				new ClassPathResource("testalternatepath/", PathResourceResolver.class)
		);

		Resource location = new ClassPathResource("test/main.css", PathResourceResolver.class);
		String actual = this.resolver.resolveUrlPath("../testalternatepath/bar.css", Arrays.asList(location), null);
		assertEquals("../testalternatepath/bar.css", actual);
	}

	private void testCheckResource(Resource location, String requestPath) throws IOException {
		Resource actual = this.resolver.resolveResource(null, requestPath, Arrays.asList(location), null);
		assertTrue(location.createRelative(requestPath).exists());
		assertNull(actual);
	}

}
