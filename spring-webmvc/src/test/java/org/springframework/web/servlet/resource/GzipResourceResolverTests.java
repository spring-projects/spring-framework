/*
 * Copyright 2002-2016 the original author or authors.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.util.FileCopyUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Unit tests for {@link GzipResourceResolver}.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 */
public class GzipResourceResolverTests {

	private ResourceResolverChain resolver;

	private List<Resource> locations;

	private Cache cache;


	@BeforeClass
	public static void createGzippedResources() throws IOException {
		createGzFile("/js/foo.js");
		createGzFile("foo-e36d2e05253c6c7085a91522ce43a0b4.css");
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
	public void setUp() {
		this.cache = new ConcurrentMapCache("resourceCache");

		Map<String, VersionStrategy> versionStrategyMap = new HashMap<>();
		versionStrategyMap.put("/**", new ContentVersionStrategy());
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(versionStrategyMap);

		List<ResourceResolver> resolvers = new ArrayList<>();
		resolvers.add(new CachingResourceResolver(this.cache));
		resolvers.add(new GzipResourceResolver());
		resolvers.add(versionResolver);
		resolvers.add(new PathResourceResolver());
		this.resolver = new DefaultResourceResolverChain(resolvers);

		this.locations = new ArrayList<>();
		this.locations.add(new ClassPathResource("test/", getClass()));
		this.locations.add(new ClassPathResource("testalternatepath/", getClass()));
	}


	@Test
	public void resolveGzippedFile() throws IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept-Encoding", "gzip");
		String file = "js/foo.js";
		Resource resolved = this.resolver.resolveResource(request, file, this.locations);

		String gzFile = file + ".gz";
		Resource resource = new ClassPathResource("test/"+gzFile, getClass());
		assertEquals(resource.getDescription(), resolved.getDescription());
		assertEquals(new ClassPathResource("test/" + file).getFilename(), resolved.getFilename());
		assertTrue("Expected " + resolved + " to be of type " + EncodedResource.class,
				resolved instanceof EncodedResource);
	}

	@Test
	public void resolveFingerprintedGzippedFile() throws IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept-Encoding", "gzip");
		String file = "foo-e36d2e05253c6c7085a91522ce43a0b4.css";
		Resource resolved = this.resolver.resolveResource(request, file, this.locations);

		String gzFile = file + ".gz";
		Resource resource = new ClassPathResource("test/"+gzFile, getClass());
		assertEquals(resource.getDescription(), resolved.getDescription());
		assertEquals(new ClassPathResource("test/"+file).getFilename(), resolved.getFilename());
		assertTrue("Expected " + resolved + " to be of type " + EncodedResource.class,
				resolved instanceof EncodedResource);
	}

	@Test
	public void resolveFromCacheWithEncodingVariants() throws IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/js/foo.js");
		request.addHeader("Accept-Encoding", "gzip");
		String file = "js/foo.js";
		Resource resolved = this.resolver.resolveResource(request, file, this.locations);

		String gzFile = file + ".gz";
		Resource gzResource = new ClassPathResource("test/"+gzFile, getClass());
		assertEquals(gzResource.getDescription(), resolved.getDescription());
		assertEquals(new ClassPathResource("test/" + file).getFilename(), resolved.getFilename());
		assertTrue("Expected " + resolved + " to be of type " + EncodedResource.class,
				resolved instanceof EncodedResource);

		// resolved resource is now cached in CachingResourceResolver

		request = new MockHttpServletRequest("GET", "/js/foo.js");
		resolved = this.resolver.resolveResource(request, file, this.locations);

		Resource resource = new ClassPathResource("test/"+file, getClass());
		assertEquals(resource.getDescription(), resolved.getDescription());
		assertEquals(new ClassPathResource("test/" + file).getFilename(), resolved.getFilename());
		assertFalse("Expected " + resolved + " to *not* be of type " + EncodedResource.class,
				resolved instanceof EncodedResource);
	}

	@Test // SPR-13149
	public void resolveWithNullRequest() throws IOException {
		String file = "js/foo.js";
		Resource resolved = this.resolver.resolveResource(null, file, this.locations);

		String gzFile = file+".gz";
		Resource gzResource = new ClassPathResource("test/"+gzFile, getClass());
		assertEquals(gzResource.getDescription(), resolved.getDescription());
		assertEquals(new ClassPathResource("test/" + file).getFilename(), resolved.getFilename());
		assertTrue("Expected " + resolved + " to be of type " + EncodedResource.class,
				resolved instanceof EncodedResource);
	}

}
