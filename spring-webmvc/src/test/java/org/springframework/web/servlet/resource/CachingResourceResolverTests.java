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

import java.io.IOException;
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
 * @author Brian Clozel
 */
@ExtendWith(GzipSupport.class)
class CachingResourceResolverTests {

	private Cache cache;

	private CachingResourceResolver cachingResolver;

	private ResourceResolverChain chain;

	private List<Resource> locations;


	@BeforeEach
	void setup() {

		this.cache = new ConcurrentMapCache("resourceCache");

		List<ResourceResolver> resolvers = new ArrayList<>();
		this.cachingResolver = new CachingResourceResolver(this.cache);
		resolvers.add(this.cachingResolver);
		resolvers.add(new EncodedResourceResolver());
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
		this.cache.put(this.cachingResolver.computeKey(null, "bar.css", this.locations), expected);
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
	void resolveResourceAcceptEncodingInCacheKey(GzippedFiles gzippedFiles) throws IOException {

		String file = "bar.css";
		gzippedFiles.create(file);

		// 1. Resolve plain resource

		MockHttpServletRequest request = new MockHttpServletRequest("GET", file);
		this.chain.resolveResource(request, file, this.locations);

		Resource actual = getFromResourceCache(request, file);
		assertThat(actual.getFile().getName()).isEqualTo("bar.css");

		// 2. Resolve with Accept-Encoding

		request = new MockHttpServletRequest("GET", file);
		request.addHeader("Accept-Encoding", "gzip ; a=b  , deflate ,  br  ; c=d ");
		this.chain.resolveResource(request, file, this.locations);

		actual = getFromResourceCache(request, file);
		assertThat(actual.getFile().getName()).isEqualTo("bar.css.gz");

		// 3. Resolve with Accept-Encoding but no matching codings

		request = new MockHttpServletRequest("GET", file);
		request.addHeader("Accept-Encoding", "deflate");
		this.chain.resolveResource(request, file, this.locations);

		actual = getFromResourceCache(request, file);
		assertThat(actual.getFile().getName()).isEqualTo("bar.css");
	}

	@Test
	void resolveResourceNoAcceptEncoding() throws IOException {
		String file = "bar.css";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", file);
		this.chain.resolveResource(request, file, this.locations);

		Resource actual = getFromResourceCache(request, file);
		assertThat(actual.getFile().getName()).isEqualTo("bar.css");
	}

	@Test
	void resolveResourceMatchingEncoding() {
		Resource resource = mock();
		Resource gzipped = mock();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "bar.css");
		this.cache.put(this.cachingResolver.computeKey(request, "bar.css", this.locations), resource);

		MockHttpServletRequest gzipRequest = new MockHttpServletRequest("GET", "bar.css");
		gzipRequest.addHeader("Accept-Encoding", "gzip");
		this.cache.put(this.cachingResolver.computeKey(gzipRequest, "bar.css", this.locations), gzipped);

		assertThat(this.chain.resolveResource(request, "bar.css", this.locations)).isSameAs(resource);
		assertThat(this.chain.resolveResource(gzipRequest, "bar.css", this.locations)).isSameAs(gzipped);
	}

	@Test
	void shareCacheBetweenResourceLocations() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "bar.css");

		List<Resource> firstLocations = List.of(new ClassPathResource("testalternatepath/", getClass()));
		Resource firstResource = this.chain.resolveResource(request, "bar.css", firstLocations);

		List<Resource> secondLocations = List.of(new ClassPathResource("test/", getClass()));
		Resource secondResource = this.chain.resolveResource(request, "bar.css", secondLocations);

		assertThat(firstResource).isNotSameAs(secondResource);
	}

	private Resource getFromResourceCache(MockHttpServletRequest request, String file) {
		String cacheKey = this.cachingResolver.computeKey(request, file, this.locations);
		return this.cache.get(cacheKey, Resource.class);
	}

}
