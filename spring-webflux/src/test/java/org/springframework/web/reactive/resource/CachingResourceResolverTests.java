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

package org.springframework.web.reactive.resource;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.reactive.resource.GzipSupport.GzippedFiles;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.get;

/**
 * Tests for {@link CachingResourceResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
@ExtendWith(GzipSupport.class)
class CachingResourceResolverTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(5);


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
		MockServerWebExchange exchange = MockServerWebExchange.from(get(""));
		Resource actual = this.chain.resolveResource(exchange, "bar.css", this.locations).block(TIMEOUT);

		assertThat(actual).isNotSameAs(expected);
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void resolveResourceInternalFromCache() {
		Resource expected = mock();
		this.cache.put(this.cachingResolver.computeKey(null, "bar.css", this.locations), expected);

		MockServerWebExchange exchange = MockServerWebExchange.from(get(""));
		Resource actual = this.chain.resolveResource(exchange, "bar.css", this.locations).block(TIMEOUT);

		assertThat(actual).isSameAs(expected);
	}

	@Test
	void resolveResourceInternalNoMatch() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get(""));
		assertThat(this.chain.resolveResource(exchange, "invalid.css", this.locations).block(TIMEOUT)).isNull();
	}

	@Test
	void resolverUrlPath() {
		String expected = "/foo.css";
		String actual = this.chain.resolveUrlPath(expected, this.locations).block(TIMEOUT);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void resolverUrlPathFromCache() {
		String expected = "cached-imaginary.css";
		this.cache.put(CachingResourceResolver.RESOLVED_URL_PATH_CACHE_KEY_PREFIX + "imaginary.css", expected);
		String actual = this.chain.resolveUrlPath("imaginary.css", this.locations).block(TIMEOUT);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void resolverUrlPathNoMatch() {
		assertThat(this.chain.resolveUrlPath("invalid.css", this.locations).block(TIMEOUT)).isNull();
	}

	@Test
	void resolveResourceAcceptEncodingInCacheKey(GzippedFiles gzippedFiles) throws IOException {

		String file = "bar.css";
		gzippedFiles.create(file);

		// 1. Resolve plain resource

		MockServerWebExchange exchange = MockServerWebExchange.from(get(file));
		this.chain.resolveResource(exchange, file, this.locations).block(TIMEOUT);

		Resource actual = getFromResourceCache(exchange, file);
		assertThat(actual.getFile().getName()).isEqualTo("bar.css");

		// 2. Resolve with Accept-Encoding

		exchange = MockServerWebExchange.from(get(file)
				.header("Accept-Encoding", "gzip ; a=b  , deflate ,  br  ; c=d "));
		this.chain.resolveResource(exchange, file, this.locations).block(TIMEOUT);

		actual = getFromResourceCache(exchange, file);
		assertThat(actual.getFile().getName()).isEqualTo("bar.css.gz");

		// 3. Resolve with Accept-Encoding but no matching codings

		exchange = MockServerWebExchange.from(get(file).header("Accept-Encoding", "deflate"));
		this.chain.resolveResource(exchange, file, this.locations).block(TIMEOUT);

		actual = getFromResourceCache(exchange, file);
		assertThat(actual.getFile().getName()).isEqualTo("bar.css");
	}

	@Test
	void resolveResourceNoAcceptEncoding() throws IOException {
		String file = "bar.css";
		MockServerWebExchange exchange = MockServerWebExchange.from(get(file));
		this.chain.resolveResource(exchange, file, this.locations).block(TIMEOUT);

		Resource actual = getFromResourceCache(exchange, file);
		assertThat(actual.getFile().getName()).isEqualTo("bar.css");
	}

	@Test
	void resolveResourceMatchingEncoding() {
		Resource resource = mock();
		Resource gzipped = mock();

		MockServerWebExchange exchange = MockServerWebExchange.from(get("bar.css"));
		this.cache.put(this.cachingResolver.computeKey(exchange, "bar.css", this.locations), resource);

		MockServerWebExchange gzipExchange = MockServerWebExchange.from(get("bar.css").header("Accept-Encoding", "gzip"));
		this.cache.put(this.cachingResolver.computeKey(gzipExchange, "bar.css", this.locations), gzipped);

		assertThat(this.chain.resolveResource(exchange, "bar.css", this.locations).block(TIMEOUT)).isSameAs(resource);
		assertThat(this.chain.resolveResource(gzipExchange, "bar.css", this.locations).block(TIMEOUT)).isSameAs(gzipped);
	}

	@Test
	void shareCacheBetweenResourceLocations() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("bar.css"));

		List<Resource> firstLocations = List.of(new ClassPathResource("testalternatepath/", getClass()));
		Resource firstResource = this.chain.resolveResource(exchange, "bar.css", firstLocations).block(TIMEOUT);

		List<Resource> secondLocations = List.of(new ClassPathResource("test/", getClass()));
		Resource secondResource = this.chain.resolveResource(exchange, "bar.css", secondLocations).block(TIMEOUT);

		assertThat(firstResource).isNotSameAs(secondResource);
	}

	private Resource getFromResourceCache(MockServerWebExchange exchange, String file) {
		String cacheKey = this.cachingResolver.computeKey(exchange, file, this.locations);
		return this.cache.get(cacheKey, Resource.class);
	}

}
