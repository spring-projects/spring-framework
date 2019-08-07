/*
 * Copyright 2002-2015 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.test.MockHttpServletRequest;

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


	@Before
	public void setup() {

		this.cache = new ConcurrentMapCache("resourceCache");

		List<ResourceResolver> resolvers = new ArrayList<>();
		resolvers.add(new CachingResourceResolver(this.cache));
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
		this.cache.put(CachingResourceResolver.RESOLVED_URL_PATH_CACHE_KEY_PREFIX + "imaginary.css", expected);
		String actual = this.chain.resolveUrlPath("imaginary.css", this.locations);

		assertEquals(expected, actual);
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

}
