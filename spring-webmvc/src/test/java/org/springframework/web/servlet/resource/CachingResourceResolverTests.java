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

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

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

}
