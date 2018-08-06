/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.resource;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.test.server.MockServerWebExchange;

import static org.junit.Assert.*;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.*;

/**
 * Unit tests for {@link CachingResourceResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class CachingResourceResolverTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(5);


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
		Resource expected = new ClassPathResource("test/bar.css", getClass());
		MockServerWebExchange exchange = MockServerWebExchange.from(get(""));
		Resource actual = this.chain.resolveResource(exchange, "bar.css", this.locations).block(TIMEOUT);

		assertNotSame(expected, actual);
		assertEquals(expected, actual);
	}

	@Test
	public void resolveResourceInternalFromCache() {
		Resource expected = Mockito.mock(Resource.class);
		this.cache.put(resourceKey("bar.css"), expected);

		MockServerWebExchange exchange = MockServerWebExchange.from(get(""));
		Resource actual = this.chain.resolveResource(exchange, "bar.css", this.locations).block(TIMEOUT);

		assertSame(expected, actual);
	}

	@Test
	public void resolveResourceInternalNoMatch() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get(""));
		assertNull(this.chain.resolveResource(exchange, "invalid.css", this.locations).block(TIMEOUT));
	}

	@Test
	public void resolverUrlPath() {
		String expected = "/foo.css";
		String actual = this.chain.resolveUrlPath(expected, this.locations).block(TIMEOUT);

		assertEquals(expected, actual);
	}

	@Test
	public void resolverUrlPathFromCache() {
		String expected = "cached-imaginary.css";
		this.cache.put(CachingResourceResolver.RESOLVED_URL_PATH_CACHE_KEY_PREFIX + "imaginary.css", expected);
		String actual = this.chain.resolveUrlPath("imaginary.css", this.locations).block(TIMEOUT);

		assertEquals(expected, actual);
	}

	@Test
	public void resolverUrlPathNoMatch() {
		assertNull(this.chain.resolveUrlPath("invalid.css", this.locations).block(TIMEOUT));
	}

	@Test
	public void resolveResourceAcceptEncodingInCacheKey() throws IOException {

		String file = "bar.css";
		EncodedResourceResolverTests.createGzippedFile(file);

		// 1. Resolve plain resource

		MockServerWebExchange exchange = MockServerWebExchange.from(get(file));
		Resource expected = this.chain.resolveResource(exchange, file, this.locations).block(TIMEOUT);

		String cacheKey = resourceKey(file);
		assertSame(expected, this.cache.get(cacheKey).get());


		// 2. Resolve with Accept-Encoding

		exchange = MockServerWebExchange.from(get(file)
				.header("Accept-Encoding", "gzip ; a=b  , deflate ,  br  ; c=d "));
		expected = this.chain.resolveResource(exchange, file, this.locations).block(TIMEOUT);

		cacheKey = resourceKey(file + "+encoding=br,gzip");
		assertSame(expected, this.cache.get(cacheKey).get());

		// 3. Resolve with Accept-Encoding but no matching codings

		exchange = MockServerWebExchange.from(get(file).header("Accept-Encoding", "deflate"));
		expected = this.chain.resolveResource(exchange, file, this.locations).block(TIMEOUT);

		cacheKey = resourceKey(file);
		assertSame(expected, this.cache.get(cacheKey).get());
	}

	@Test
	public void resolveResourceNoAcceptEncoding() {
		String file = "bar.css";
		MockServerWebExchange exchange = MockServerWebExchange.from(get(file));
		Resource expected = this.chain.resolveResource(exchange, file, this.locations).block(TIMEOUT);

		String cacheKey = resourceKey(file);
		Object actual = this.cache.get(cacheKey).get();

		assertEquals(expected, actual);
	}

	@Test
	public void resolveResourceMatchingEncoding() {
		Resource resource = Mockito.mock(Resource.class);
		Resource gzipped = Mockito.mock(Resource.class);
		this.cache.put(resourceKey("bar.css"), resource);
		this.cache.put(resourceKey("bar.css+encoding=gzip"), gzipped);

		String file = "bar.css";
		MockServerWebExchange exchange = MockServerWebExchange.from(get(file));
		assertSame(resource, this.chain.resolveResource(exchange, file, this.locations).block(TIMEOUT));

		exchange = MockServerWebExchange.from(get(file).header("Accept-Encoding", "gzip"));
		assertSame(gzipped, this.chain.resolveResource(exchange, file, this.locations).block(TIMEOUT));
	}

	private static String resourceKey(String key) {
		return CachingResourceResolver.RESOLVED_RESOURCE_CACHE_KEY_PREFIX + key;
	}

}
