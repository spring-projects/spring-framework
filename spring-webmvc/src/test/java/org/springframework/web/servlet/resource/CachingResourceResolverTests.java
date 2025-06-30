/*
 * Copyright 2002-present the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.GzipSupport.GzippedFiles;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CachingResourceResolver}.
 *
 * @author Rossen Stoyanchev
 */
@ExtendWith(GzipSupport.class)
public class CachingResourceResolverTests {

	private Cache cache;

	private ResourceResolverChain chain;

	private List<Resource> locations;


	@BeforeEach
	void setup() {

		this.cache = new ConcurrentMapCache("resourceCache");

		List<ResourceResolver> resolvers = new ArrayList<>();
		resolvers.add(new CachingResourceResolver(this.cache));
		resolvers.add(new PathResourceResolver());
		this.chain = new DefaultResourceResolverChain(resolvers);

		this.locations = new ArrayList<>();
		this.locations.add(new ClassPathResource("test/", getClass()));
	}


	@Test
	void resolveResourceInternal() {
		Resource expected = new ClassPathResource("test/bar.css", getClass());
		Resource actual = this.chain.resolveResource(null, "bar.css", this.locations);

		assertThat(actual).isNotSameAs(expected);
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void resolveResourceInternalFromCache() {
		Resource expected = mock();
		this.cache.put(resourceKey("bar.css"), expected);
		Resource actual = this.chain.resolveResource(null, "bar.css", this.locations);

		assertThat(actual).isSameAs(expected);
	}

	@Test
	void resolveResourceInternalNoMatch() {
		assertThat(this.chain.resolveResource(null, "invalid.css", this.locations)).isNull();
	}

	@Test
	void resolverUrlPath() {
		String expected = "/foo.css";
		String actual = this.chain.resolveUrlPath(expected, this.locations);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void resolverUrlPathFromCache() {
		String expected = "cached-imaginary.css";
		this.cache.put(CachingResourceResolver.RESOLVED_URL_PATH_CACHE_KEY_PREFIX + "imaginary.css", expected);
		String actual = this.chain.resolveUrlPath("imaginary.css", this.locations);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void resolverUrlPathNoMatch() {
		assertThat(this.chain.resolveUrlPath("invalid.css", this.locations)).isNull();
	}

	@Test
	void resolveResourceAcceptEncodingInCacheKey(GzippedFiles gzippedFiles) {

		String file = "bar.css";
		gzippedFiles.create(file);

		// 1. Resolve plain resource

		MockHttpServletRequest request = new MockHttpServletRequest("GET", file);
		Resource expected = this.chain.resolveResource(request, file, this.locations);

		String cacheKey = resourceKey(file);
		assertThat(this.cache.get(cacheKey).get()).isSameAs(expected);

		// 2. Resolve with Accept-Encoding

		request = new MockHttpServletRequest("GET", file);
		request.addHeader("Accept-Encoding", "gzip ; a=b  , deflate ,  br  ; c=d ");
		expected = this.chain.resolveResource(request, file, this.locations);

		cacheKey = resourceKey(file + "+encoding=br,gzip");
		assertThat(this.cache.get(cacheKey).get()).isSameAs(expected);

		// 3. Resolve with Accept-Encoding but no matching codings

		request = new MockHttpServletRequest("GET", file);
		request.addHeader("Accept-Encoding", "deflate");
		expected = this.chain.resolveResource(request, file, this.locations);

		cacheKey = resourceKey(file);
		assertThat(this.cache.get(cacheKey).get()).isSameAs(expected);
	}

	@Test
	void resolveResourceNoAcceptEncoding() {
		String file = "bar.css";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", file);
		Resource expected = this.chain.resolveResource(request, file, this.locations);

		String cacheKey = resourceKey(file);
		Object actual = this.cache.get(cacheKey).get();

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void resolveResourceMatchingEncoding() {
		Resource resource = mock();
		Resource gzipped = mock();
		this.cache.put(resourceKey("bar.css"), resource);
		this.cache.put(resourceKey("bar.css+encoding=gzip"), gzipped);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "bar.css");
		assertThat(this.chain.resolveResource(request, "bar.css", this.locations)).isSameAs(resource);

		request = new MockHttpServletRequest("GET", "bar.css");
		request.addHeader("Accept-Encoding", "gzip");
		assertThat(this.chain.resolveResource(request, "bar.css", this.locations)).isSameAs(gzipped);
	}

	private static String resourceKey(String key) {
		return CachingResourceResolver.RESOLVED_RESOURCE_CACHE_KEY_PREFIX + key;
	}

}
