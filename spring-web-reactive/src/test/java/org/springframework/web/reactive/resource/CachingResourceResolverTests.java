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

package org.springframework.web.reactive.resource;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for {@link CachingResourceResolver}.
 * @author Rossen Stoyanchev
 */
public class CachingResourceResolverTests {

	private Cache cache;

	private ResourceResolverChain chain;

	private List<Resource> locations;

	private ServerWebExchange exchange;

	private MockServerHttpRequest request;


	@Before
	public void setup() {

		this.cache = new ConcurrentMapCache("resourceCache");

		List<ResourceResolver> resolvers = new ArrayList<>();
		resolvers.add(new CachingResourceResolver(this.cache));
		resolvers.add(new PathResourceResolver());
		this.chain = new DefaultResourceResolverChain(resolvers);

		this.locations = new ArrayList<>();
		this.locations.add(new ClassPathResource("test/", getClass()));

		this.request = new MockServerHttpRequest(HttpMethod.GET, "");
		ServerHttpResponse response = new MockServerHttpResponse();
		WebSessionManager manager = new DefaultWebSessionManager();
		this.exchange = new DefaultServerWebExchange(request, response, manager);
	}


	@Test
	public void resolveResourceInternal() {
		String file = "bar.css";
		Resource expected = new ClassPathResource("test/" + file, getClass());
		Resource actual = this.chain.resolveResource(this.exchange, file, this.locations);

		assertEquals(expected, actual);
	}

	@Test
	public void resolveResourceInternalFromCache() {

		Resource expected = Mockito.mock(Resource.class);
		this.cache.put(CachingResourceResolver.RESOLVED_RESOURCE_CACHE_KEY_PREFIX + "bar.css", expected);

		String file = "bar.css";
		Resource actual = this.chain.resolveResource(this.exchange, file, this.locations);

		assertSame(expected, actual);
	}

	@Test
	public void resolveResourceInternalNoMatch() {
		assertNull(this.chain.resolveResource(this.exchange, "invalid.css", this.locations));
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
		this.request.setUri(file).setHeader("Accept-Encoding", "gzip");

		Resource expected = this.chain.resolveResource(this.exchange, file, this.locations);
		String cacheKey = CachingResourceResolver.RESOLVED_RESOURCE_CACHE_KEY_PREFIX + file + "+encoding=gzip";

		assertEquals(expected, this.cache.get(cacheKey).get());
	}

	@Test
	public void resolveResourceNoAcceptEncodingInCacheKey() {
		String file = "bar.css";
		this.request.setUri(file);

		Resource expected = this.chain.resolveResource(this.exchange, file, this.locations);
		String cacheKey = CachingResourceResolver.RESOLVED_RESOURCE_CACHE_KEY_PREFIX + file;

		assertEquals(expected, this.cache.get(cacheKey).get());
	}

	@Test
	public void resolveResourceMatchingEncoding() {
		Resource resource = Mockito.mock(Resource.class);
		Resource gzResource = Mockito.mock(Resource.class);
		this.cache.put(CachingResourceResolver.RESOLVED_RESOURCE_CACHE_KEY_PREFIX + "bar.css", resource);
		this.cache.put(CachingResourceResolver.RESOLVED_RESOURCE_CACHE_KEY_PREFIX + "bar.css+encoding=gzip", gzResource);

		String file = "bar.css";
		this.request.setUri(file);
		assertSame(resource, this.chain.resolveResource(this.exchange, file, this.locations));

		request.addHeader("Accept-Encoding", "gzip");
		assertSame(gzResource, this.chain.resolveResource(this.exchange, file, this.locations));
	}

}
