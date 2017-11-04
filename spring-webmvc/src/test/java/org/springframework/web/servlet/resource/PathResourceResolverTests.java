/*
 * Copyright 2002-2015 the original author or authors.
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.util.UrlPathHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for
 * {@link org.springframework.web.servlet.resource.PathResourceResolver}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 */
public class PathResourceResolverTests {

	private final PathResourceResolver resolver = new PathResourceResolver();


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

	private void testCheckResource(Resource location, String requestPath) throws IOException {
		Resource actual = this.resolver.resolveResource(null, requestPath, Arrays.asList(location), null);
		if (!location.createRelative(requestPath).exists() && !requestPath.contains(":")) {
			fail(requestPath + " doesn't actually exist as a relative path");
		}
		assertNull(actual);
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

	// SPR-12432
	@Test
	public void checkServletContextResource() throws Exception {
		Resource classpathLocation = new ClassPathResource("test/", PathResourceResolver.class);
		MockServletContext context = new MockServletContext();

		ServletContextResource servletContextLocation = new ServletContextResource(context, "/webjars/");
		ServletContextResource resource = new ServletContextResource(context, "/webjars/webjar-foo/1.0/foo.js");

		assertFalse(this.resolver.checkResource(resource, classpathLocation));
		assertTrue(this.resolver.checkResource(resource, servletContextLocation));
	}

	// SPR-12624
	@Test
	public void checkRelativeLocation() throws Exception {
		String locationUrl= new UrlResource(getClass().getResource("./test/")).getURL().toExternalForm();
		Resource location = new UrlResource(locationUrl.replace("/springframework","/../org/springframework"));

		assertNotNull(this.resolver.resolveResource(null, "main.css", Arrays.asList(location), null));
	}

	// SPR-12747
	@Test
	public void checkFileLocation() throws Exception {
		Resource resource = new ClassPathResource("test/main.css", PathResourceResolver.class);
		assertTrue(this.resolver.checkResource(resource, resource));
	}

	// SPR-13241
	@Test
	public void resolvePathRootResource() throws Exception {
		Resource webjarsLocation = new ClassPathResource("/META-INF/resources/webjars/", PathResourceResolver.class);
		String path = this.resolver.resolveUrlPathInternal("", Arrays.asList(webjarsLocation), null);

		assertNull(path);
	}

	@Test
	public void relativePathEncodedForUrlResource() throws Exception {
		TestUrlResource location = new TestUrlResource("file:///tmp");
		List<TestUrlResource> locations = Collections.singletonList(location);

		// ISO-8859-1
		this.resolver.setUrlPathHelper(new UrlPathHelper());
		this.resolver.setLocationCharsets(Collections.singletonMap(location, StandardCharsets.ISO_8859_1));
		this.resolver.resolveResource(new MockHttpServletRequest(), "/Ä ;ä.txt", locations, null);

		assertEquals("%C4%20%3B%E4.txt", location.getSavedRelativePath());

		// UTF-8
		this.resolver.setLocationCharsets(Collections.singletonMap(location, StandardCharsets.UTF_8));
		this.resolver.resolveResource(new MockHttpServletRequest(), "/Ä ;ä.txt", locations, null);

		assertEquals("%C3%84%20%3B%C3%A4.txt", location.getSavedRelativePath());

		// UTF-8 by default
		this.resolver.setLocationCharsets(Collections.emptyMap());
		this.resolver.resolveResource(new MockHttpServletRequest(), "/Ä ;ä.txt", locations, null);

		assertEquals("%C3%84%20%3B%C3%A4.txt", location.getSavedRelativePath());
	}


	private static class TestUrlResource extends UrlResource {

		private String relativePath;


		public TestUrlResource(String path) throws MalformedURLException {
			super(path);
		}


		public String getSavedRelativePath() {
			return this.relativePath;
		}

		@Override
		public Resource createRelative(String relativePath) throws MalformedURLException {
			this.relativePath = relativePath;
			return this;
		}
	}

}
