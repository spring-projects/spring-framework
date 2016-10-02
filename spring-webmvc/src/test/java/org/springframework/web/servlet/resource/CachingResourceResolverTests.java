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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.util.DigestUtils;

import static org.junit.Assert.*;

/**
 * Unit tests for
 * {@link org.springframework.web.servlet.resource.CachingResourceResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class CachingResourceResolverTests {

	private Cache cache;

	private ResourceResolverChain chain;

	private List<Resource> locations;
	private CachingResourceResolver cachingResourceResolver;


	@Before
	public void setup() {

		this.cache = new ConcurrentMapCache("resourceCache");

		List<ResourceResolver> resolvers = new ArrayList<>();
		cachingResourceResolver = new CachingResourceResolver(this.cache);
		resolvers.add(cachingResourceResolver);
		resolvers.add(new PathResourceResolver());
		this.chain = new DefaultResourceResolverChain(resolvers);

		this.locations = new ArrayList<>();
		this.locations.add(new ClassPathResource("test/", getClass()));
	}


	@Test
	public void resolveResourceInternal() {
		String file = "bar.css";
		Resource expected = new ClassPathResource("test/" + file, getClass());
		Resource actual = this.chain.resolveResource(null, file, this.locations);

		assertEquals(expected, actual);
	}

	@Test
	public void resolveResourceInternalFromCache() {

		Resource expected = Mockito.mock(Resource.class);
		this.cache.put(CachingResourceResolver.RESOLVED_RESOURCE_CACHE_KEY_PREFIX + "bar.css", expected);

		String file = "bar.css";
		Resource actual = this.chain.resolveResource(null, file, this.locations);

		assertSame(expected, actual);
	}

	@Test
	public void resolveResourceInternalNoMatch() {
		assertNull(this.chain.resolveResource(null, "invalid.css", this.locations));
	}

	@Test
	public void resolverUrlPath() {
		String expected = "/foo.css";
		String actual = this.chain.resolveUrlPath(expected, this.locations);

		assertEquals(expected, actual);
	}

	@Test
	public void resolverUrlPathFromCache() {
		String expected = "cached-imaginary.css";

		String locationDigest = cachingResourceResolver.computeLocationDerivedDigest(this.locations);

		this.cache.put(CachingResourceResolver.RESOLVED_URL_PATH_CACHE_KEY_PREFIX + "imaginary.css+locationDigest=" + locationDigest, expected);
		String actual = this.chain.resolveUrlPath("imaginary.css", this.locations);

		assertEquals(expected, actual);
	}

	@Test
	public void resolveUrlPathFromCacheWithSamePathButDifferentLocations() {
		String firstLocationDigest = cachingResourceResolver.computeLocationDerivedDigest(this.locations);

		List<Resource> secondLocationList = Collections.singletonList(new ClassPathResource("test-second/", getClass()));
		String secondLocationDigest = cachingResourceResolver.computeLocationDerivedDigest(secondLocationList);

		String firstCacheKey = CachingResourceResolver.RESOLVED_URL_PATH_CACHE_KEY_PREFIX + "imaginary.css+locationDigest=" + firstLocationDigest;
		String secondCacheKey = CachingResourceResolver.RESOLVED_URL_PATH_CACHE_KEY_PREFIX + "imaginary.css+locationDigest=" + secondLocationDigest;

		assertNull(this.cache.get(firstCacheKey));
		assertNull(this.cache.get(secondCacheKey));

		this.chain.resolveUrlPath("imaginary.css", this.locations);
		this.chain.resolveUrlPath("imaginary.css", secondLocationList);

		assertNotNull(this.cache.get(firstCacheKey));
		assertNotNull(this.cache.get(secondCacheKey));
	}

	@Test
	public void resolverUrlPathNoMatch() {
		assertNull(this.chain.resolveUrlPath("invalid.css", this.locations));
	}

	@Test
	public void resolveResourceAcceptEncodingInCacheKey() {
		String file = "bar.css";

		MockHttpServletRequest request = new MockHttpServletRequest("GET", file);
		request.addHeader("Accept-Encoding", "gzip");
		Resource expected = this.chain.resolveResource(request, file, this.locations);
		String cacheKey = CachingResourceResolver.RESOLVED_RESOURCE_CACHE_KEY_PREFIX + file + "+encoding=gzip";

		assertEquals(expected, this.cache.get(cacheKey).get());
	}

	@Test
	public void resolveResourceNoAcceptEncodingInCacheKey() {
		String file = "bar.css";

		MockHttpServletRequest request = new MockHttpServletRequest("GET", file);
		Resource expected = this.chain.resolveResource(request, file, this.locations);
		String cacheKey = CachingResourceResolver.RESOLVED_RESOURCE_CACHE_KEY_PREFIX + file;

		assertEquals(expected, this.cache.get(cacheKey).get());
	}

	@Test
	public void resolveResourceMatchingEncoding() {
		Resource resource = Mockito.mock(Resource.class);
		Resource gzResource = Mockito.mock(Resource.class);
		this.cache.put(CachingResourceResolver.RESOLVED_RESOURCE_CACHE_KEY_PREFIX + "bar.css", resource);
		this.cache.put(CachingResourceResolver.RESOLVED_RESOURCE_CACHE_KEY_PREFIX + "bar.css+encoding=gzip", gzResource);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "bar.css");
		assertSame(resource, this.chain.resolveResource(request,"bar.css", this.locations));

		request = new MockHttpServletRequest("GET", "bar.css");
		request.addHeader("Accept-Encoding", "gzip");
		assertSame(gzResource, this.chain.resolveResource(request, "bar.css", this.locations));
	}

	@Test
	public void computeLocationDerivedDigest() {
		ClassPathResource firstResource = new ClassPathResource("fake/path", getClass());
		ClassPathResource secondResource = new ClassPathResource("another/sample/sample", getClass());

		String concatResourceDescriptions = firstResource.getDescription() + "," + secondResource.getDescription();
		String expectedLocationDigest = DigestUtils.md5DigestAsHex(concatResourceDescriptions.getBytes());

		String actualLocationDigest = cachingResourceResolver.computeLocationDerivedDigest(Arrays.asList(firstResource, secondResource));

		assertEquals(expectedLocationDigest, actualLocationDigest);
	}

}
