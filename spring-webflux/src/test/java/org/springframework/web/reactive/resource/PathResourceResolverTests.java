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
package org.springframework.web.reactive.resource;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Unit tests for {@link PathResourceResolver}.
 * @author Rossen Stoyanchev
 */
public class PathResourceResolverTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(5);


	private final PathResourceResolver resolver = new PathResourceResolver();


	@Test
	public void resolveFromClasspath() throws IOException {
		Resource location = new ClassPathResource("test/", PathResourceResolver.class);
		String path = "bar.css";
		List<Resource> locations = singletonList(location);
		Resource actual = this.resolver.resolveResource(null, path, locations, null).block(TIMEOUT);

		assertThat(actual).isEqualTo(location.createRelative(path));
	}

	@Test
	public void resolveFromClasspathRoot() {
		Resource location = new ClassPathResource("/");
		String path = "org/springframework/web/reactive/resource/test/bar.css";
		List<Resource> locations = singletonList(location);
		Resource actual = this.resolver.resolveResource(null, path, locations, null).block(TIMEOUT);

		assertThat(actual).isNotNull();
	}

	@Test // gh-22272
	public void resolveWithEncodedPath() throws IOException {
		Resource classpathLocation = new ClassPathResource("test/", PathResourceResolver.class);
		testWithEncodedPath(classpathLocation);
		testWithEncodedPath(new FileUrlResource(classpathLocation.getURL()));
	}

	private void testWithEncodedPath(Resource location) throws IOException {
		String path = "foo%20foo.txt";
		List<Resource> locations = singletonList(location);
		Resource actual = this.resolver.resolveResource(null, path, locations, null).block(TIMEOUT);

		assertThat(actual).isNotNull();
		assertThat(actual.getFile().getName()).isEqualTo("foo foo.txt");
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
		List<Resource> locations = singletonList(location);
		Resource actual = this.resolver.resolveResource(null, requestPath, locations, null).block(TIMEOUT);
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
		String actual = this.resolver.resolveUrlPath("../testalternatepath/bar.css",
				singletonList(location), null).block(TIMEOUT);

		assertThat(actual).isEqualTo("../testalternatepath/bar.css");
	}

	@Test // SPR-12624
	public void checkRelativeLocation() throws Exception {
		String locationUrl= new UrlResource(getClass().getResource("./test/")).getURL().toExternalForm();
		Resource location = new UrlResource(locationUrl.replace("/springframework","/../org/springframework"));
		List<Resource> locations = singletonList(location);
		assertThat(this.resolver.resolveResource(null, "main.css", locations, null).block(TIMEOUT)).isNotNull();
	}

	@Test // SPR-12747
	public void checkFileLocation() throws Exception {
		Resource resource = getResource("main.css");
		assertThat(this.resolver.checkResource(resource, resource)).isTrue();
	}

	@Test // SPR-13241
	public void resolvePathRootResource() {
		Resource webjarsLocation = new ClassPathResource("/META-INF/resources/webjars/", PathResourceResolver.class);
		String path = this.resolver.resolveUrlPathInternal(
				"", singletonList(webjarsLocation), null).block(TIMEOUT);

		assertThat(path).isNull();
	}

	private Resource getResource(String filePath) {
		return new ClassPathResource("test/" + filePath, getClass());
	}

}
