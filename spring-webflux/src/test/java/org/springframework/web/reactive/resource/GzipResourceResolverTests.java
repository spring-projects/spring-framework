/*
 * Copyright 2002-2018 the original author or authors.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.util.FileCopyUtils;

import static org.junit.Assert.*;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.*;


/**
 * Unit tests for {@link GzipResourceResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class GzipResourceResolverTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(5);

	
	private ResourceResolverChain resolver;

	private List<Resource> locations;


	@BeforeClass
	public static void createGzippedResources() throws IOException {
		createGzFile("/js/foo.js");
		createGzFile("foo.css");
	}

	private static void createGzFile(String filePath) throws IOException {
		Resource location = new ClassPathResource("test/", GzipResourceResolverTests.class);
		Resource fileResource = new FileSystemResource(location.createRelative(filePath).getFile());
		Path gzFilePath = Paths.get(fileResource.getFile().getAbsolutePath() + ".gz");
		Files.deleteIfExists(gzFilePath);
		File gzFile = Files.createFile(gzFilePath).toFile();
		GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(gzFile));
		FileCopyUtils.copy(fileResource.getInputStream(), out);
		gzFile.deleteOnExit();
	}


	@Before
	public void setup() {
		Cache cache = new ConcurrentMapCache("resourceCache");

		Map<String, VersionStrategy> versionStrategyMap = new HashMap<>();
		versionStrategyMap.put("/**", new ContentVersionStrategy());
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(versionStrategyMap);

		List<ResourceResolver> resolvers = new ArrayList<>();
		resolvers.add(new CachingResourceResolver(cache));
		resolvers.add(new GzipResourceResolver());
		resolvers.add(versionResolver);
		resolvers.add(new PathResourceResolver());
		this.resolver = new DefaultResourceResolverChain(resolvers);

		this.locations = new ArrayList<>();
		this.locations.add(new ClassPathResource("test/", getClass()));
		this.locations.add(new ClassPathResource("testalternatepath/", getClass()));
	}


	@Test
	public void resolveGzippedFile() {

		MockServerWebExchange exchange = MockServerWebExchange.from(get("").header("Accept-Encoding", "gzip"));
		Resource resolved = this.resolver.resolveResource(exchange, "js/foo.js", this.locations).block(TIMEOUT);

		assertEquals(getResource("js/foo.js.gz").getDescription(), resolved.getDescription());
		assertEquals(getResource("js/foo.js").getFilename(), resolved.getFilename());
		assertTrue(resolved instanceof HttpResource);
	}

	@Test
	public void resolveFingerprintedGzippedFile() {

		String file = "foo-e36d2e05253c6c7085a91522ce43a0b4.css";
		MockServerWebExchange exchange = MockServerWebExchange.from(get("").header("Accept-Encoding", "gzip"));
		Resource resolved = this.resolver.resolveResource(exchange, file, this.locations).block(TIMEOUT);

		assertEquals(getResource("foo.css.gz").getDescription(), resolved.getDescription());
		assertEquals(getResource("foo.css").getFilename(), resolved.getFilename());
		assertTrue(resolved instanceof HttpResource);
	}

	@Test
	public void resolveFromCacheWithEncodingVariants() {

		MockServerWebExchange exchange = MockServerWebExchange.from(get("").header("Accept-Encoding", "gzip"));
		Resource resolved = this.resolver.resolveResource(exchange, "js/foo.js", this.locations).block(TIMEOUT);

		assertEquals(getResource("js/foo.js.gz").getDescription(), resolved.getDescription());
		assertEquals(getResource("js/foo.js").getFilename(), resolved.getFilename());
		assertTrue(resolved instanceof HttpResource);

		// resolved resource is now cached in CachingResourceResolver

		exchange = MockServerWebExchange.from(get("/js/foo.js"));
		resolved = this.resolver.resolveResource(exchange, "js/foo.js", this.locations).block(TIMEOUT);

		assertEquals(getResource("js/foo.js").getDescription(), resolved.getDescription());
		assertEquals(getResource("js/foo.js").getFilename(), resolved.getFilename());
		assertFalse(resolved instanceof HttpResource);
	}

	@Test  // SPR-13149
	public void resolveWithNullRequest() {
		Resource resolved = this.resolver.resolveResource(null, "js/foo.js", this.locations).block(TIMEOUT);

		assertEquals(getResource("js/foo.js.gz").getDescription(), resolved.getDescription());
		assertEquals(getResource("js/foo.js").getFilename(), resolved.getFilename());
		assertTrue(resolved instanceof HttpResource);
	}

	private Resource getResource(String filePath) {
		return new ClassPathResource("test/" + filePath, getClass());
	}

}
