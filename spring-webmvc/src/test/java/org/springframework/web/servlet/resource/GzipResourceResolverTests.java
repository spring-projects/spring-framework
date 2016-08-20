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

import java.io.FileOutputStream;
import java.io.IOException;
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

import static org.junit.Assert.*;


/**
 * Unit tests for
 * {@link org.springframework.web.servlet.resource.GzipResourceResolver}.
 *
 * @author Jeremy Grelle
 */
public class GzipResourceResolverTests {

	private ResourceResolverChain resolver;

	private List<Resource> locations;

	private Cache cache;

	@BeforeClass
	public static void createGzippedResources() throws IOException {
		Resource location = new ClassPathResource("test/", GzipResourceResolverTests.class);
		Resource jsFile = new FileSystemResource(location.createRelative("/js/foo.js").getFile());
		Resource gzJsFile = jsFile.createRelative("foo.js.gz");
		Resource fingerPrintedFile = new FileSystemResource(location.createRelative("foo-e36d2e05253c6c7085a91522ce43a0b4.css").getFile());
		Resource gzFingerPrintedFile = fingerPrintedFile.createRelative("foo-e36d2e05253c6c7085a91522ce43a0b4.css.gz");

		if (gzJsFile.getFile().createNewFile()) {
			GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(gzJsFile.getFile()));
			FileCopyUtils.copy(jsFile.getInputStream(), out);
		}

		if (gzFingerPrintedFile.getFile().createNewFile()) {
			GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(gzFingerPrintedFile.getFile()));
			FileCopyUtils.copy(fingerPrintedFile.getInputStream(), out);
		}

		assertTrue(gzJsFile.exists());
		assertTrue(gzFingerPrintedFile.exists());
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
		resolver = new DefaultResourceResolverChain(resolvers);
		locations = new ArrayList<>();
		locations.add(new ClassPathResource("test/", getClass()));
		locations.add(new ClassPathResource("testalternatepath/", getClass()));
	}

	@Test
	public void resolveGzippedFile() throws IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept-Encoding", "gzip");
		String file = "js/foo.js";
		String gzFile = file+".gz";
		Resource resource = new ClassPathResource("test/"+gzFile, getClass());
		Resource resolved = resolver.resolveResource(request, file, locations);

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
		String gzFile = file+".gz";
		Resource resource = new ClassPathResource("test/"+gzFile, getClass());
		Resource resolved = resolver.resolveResource(request, file, locations);

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
		String gzFile = file+".gz";
		Resource resource = new ClassPathResource("test/"+file, getClass());
		Resource gzResource = new ClassPathResource("test/"+gzFile, getClass());

		// resolved resource is now cached in CachingResourceResolver
		Resource resolved = resolver.resolveResource(request, file, locations);

		assertEquals(gzResource.getDescription(), resolved.getDescription());
		assertEquals(new ClassPathResource("test/" + file).getFilename(), resolved.getFilename());
		assertTrue("Expected " + resolved + " to be of type " + EncodedResource.class,
				resolved instanceof EncodedResource);

		request = new MockHttpServletRequest("GET", "/js/foo.js");
		resolved = resolver.resolveResource(request, file, locations);
		assertEquals(resource.getDescription(), resolved.getDescription());
		assertEquals(new ClassPathResource("test/" + file).getFilename(), resolved.getFilename());
		assertFalse("Expected " + resolved + " to *not* be of type " + EncodedResource.class,
				resolved instanceof EncodedResource);
	}

	// SPR-13149
	@Test
	public void resolveWithNullRequest() throws IOException {

		String file = "js/foo.js";
		String gzFile = file+".gz";
		Resource gzResource = new ClassPathResource("test/"+gzFile, getClass());

		// resolved resource is now cached in CachingResourceResolver
		Resource resolved = resolver.resolveResource(null, file, locations);

		assertEquals(gzResource.getDescription(), resolved.getDescription());
		assertEquals(new ClassPathResource("test/" + file).getFilename(), resolved.getFilename());
		assertTrue("Expected " + resolved + " to be of type " + EncodedResource.class,
				resolved instanceof EncodedResource);
	}
}
