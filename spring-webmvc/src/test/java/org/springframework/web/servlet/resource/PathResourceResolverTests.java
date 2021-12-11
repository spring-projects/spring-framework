/*
 * Copyright 2002-2020 the original author or authors.
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockServletContext;
import org.springframework.web.util.UrlPathHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Unit tests for {@link PathResourceResolver}.
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
		Resource actual = this.resolver.resolveResource(null, requestPath, Collections.singletonList(location), null);

		assertThat(actual).isEqualTo(location.createRelative(requestPath));
	}

	@Test
	public void resolveFromClasspathRoot() {
		Resource location = new ClassPathResource("/");
		String requestPath = "org/springframework/web/servlet/resource/test/bar.css";
		Resource actual = this.resolver.resolveResource(null, requestPath, Collections.singletonList(location), null);

		assertThat(actual).isNotNull();
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
		List<Resource> locations = Collections.singletonList(location);
		Resource actual = this.resolver.resolveResource(null, requestPath, locations, null);
		if (!location.createRelative(requestPath).exists() && !requestPath.contains(":")) {
			fail(requestPath + " doesn't actually exist as a relative path");
		}
		assertThat(actual).isNull();
	}

	@Test // gh-23463
	public void ignoreInvalidEscapeSequence() throws IOException {
		UrlResource location = new UrlResource(getClass().getResource("./test/"));
		Resource resource = location.createRelative("test%file.txt");
		assertThat(this.resolver.checkResource(resource, location)).isTrue();
	}

	@Test
	public void checkResourceWithAllowedLocations() {
		this.resolver.setAllowedLocations(
				new ClassPathResource("test/", PathResourceResolver.class),
				new ClassPathResource("testalternatepath/", PathResourceResolver.class)
		);

		Resource location = getResource("main.css");
		List<Resource> locations = Collections.singletonList(location);
		String actual = this.resolver.resolveUrlPath("../testalternatepath/bar.css", locations, null);
		assertThat(actual).isEqualTo("../testalternatepath/bar.css");
	}

	@Test // SPR-12432
	public void checkServletContextResource() throws Exception {
		Resource classpathLocation = new ClassPathResource("test/", PathResourceResolver.class);
		MockServletContext context = new MockServletContext();

		ServletContextResource servletContextLocation = new ServletContextResource(context, "/webjars/");
		ServletContextResource resource = new ServletContextResource(context, "/webjars/webjar-foo/1.0/foo.js");

		assertThat(this.resolver.checkResource(resource, classpathLocation)).isFalse();
		assertThat(this.resolver.checkResource(resource, servletContextLocation)).isTrue();
	}

	@Test // SPR-12624
	public void checkRelativeLocation() throws Exception {
		String location= new UrlResource(getClass().getResource("./test/")).getURL().toExternalForm();
		location = location.replace("/test/org/springframework","/test/org/../org/springframework");

		Resource actual = this.resolver.resolveResource(
				null, "main.css", Collections.singletonList(new UrlResource(location)), null);

		assertThat(actual).isNotNull();
	}

	@Test // SPR-12747
	public void checkFileLocation() throws Exception {
		Resource resource = getResource("main.css");
		assertThat(this.resolver.checkResource(resource, resource)).isTrue();
	}

	@Test // SPR-13241
	public void resolvePathRootResource() {
		Resource webjarsLocation = new ClassPathResource("/META-INF/resources/webjars/", PathResourceResolver.class);
		String path = this.resolver.resolveUrlPathInternal("", Collections.singletonList(webjarsLocation), null);

		assertThat(path).isNull();
	}

	@Test
	public void relativePathEncodedForUrlResource() throws Exception {
		TestUrlResource location = new TestUrlResource("file:///tmp");
		List<TestUrlResource> locations = Collections.singletonList(location);

		// ISO-8859-1
		this.resolver.setUrlPathHelper(new UrlPathHelper());
		this.resolver.setLocationCharsets(Collections.singletonMap(location, StandardCharsets.ISO_8859_1));
		this.resolver.resolveResource(new MockHttpServletRequest(), "/Ä ;ä.txt", locations, null);

		assertThat(location.getSavedRelativePath()).isEqualTo("%C4%20%3B%E4.txt");

		// UTF-8
		this.resolver.setLocationCharsets(Collections.singletonMap(location, StandardCharsets.UTF_8));
		this.resolver.resolveResource(new MockHttpServletRequest(), "/Ä ;ä.txt", locations, null);

		assertThat(location.getSavedRelativePath()).isEqualTo("%C3%84%20%3B%C3%A4.txt");

		// UTF-8 by default
		this.resolver.setLocationCharsets(Collections.emptyMap());
		this.resolver.resolveResource(new MockHttpServletRequest(), "/Ä ;ä.txt", locations, null);

		assertThat(location.getSavedRelativePath()).isEqualTo("%C3%84%20%3B%C3%A4.txt");
	}

	private Resource getResource(String filePath) {
		return new ClassPathResource("test/" + filePath, getClass());
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
