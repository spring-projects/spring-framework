/*
 * Copyright 2002-2019 the original author or authors.
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.reactive.resource.GzipSupport.GzippedFiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EncodedResourceResolver}.
 *
 * @author Rossen Stoyanchev
 */
@ExtendWith(GzipSupport.class)
public class EncodedResourceResolverTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(5);


	private ResourceResolverChain resolver;

	private List<Resource> locations;


	@BeforeEach
	public void setup() {
		Cache cache = new ConcurrentMapCache("resourceCache");

		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));

		List<ResourceResolver> resolvers = new ArrayList<>();
		resolvers.add(new CachingResourceResolver(cache));
		resolvers.add(new EncodedResourceResolver());
		resolvers.add(versionResolver);
		resolvers.add(new PathResourceResolver());
		this.resolver = new DefaultResourceResolverChain(resolvers);

		this.locations = new ArrayList<>();
		this.locations.add(new ClassPathResource("test/", getClass()));
		this.locations.add(new ClassPathResource("testalternatepath/", getClass()));
	}


	@Test
	public void resolveGzipped(GzippedFiles gzippedFiles) {

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("").header("Accept-Encoding", "gzip"));

		String file = "js/foo.js";
		gzippedFiles.create(file);
		Resource actual = this.resolver.resolveResource(exchange, file, this.locations).block(TIMEOUT);

		assertThat(actual.getDescription()).isEqualTo(getResource(file + ".gz").getDescription());
		assertThat(actual.getFilename()).isEqualTo(getResource(file).getFilename());

		boolean condition = actual instanceof HttpResource;
		assertThat(condition).isTrue();
		HttpHeaders headers = ((HttpResource) actual).getResponseHeaders();
		assertThat(headers.getFirst(HttpHeaders.CONTENT_ENCODING)).isEqualTo("gzip");
		assertThat(headers.getFirst(HttpHeaders.VARY)).isEqualTo("Accept-Encoding");
	}

	@Test
	public void resolveGzippedWithVersion(GzippedFiles gzippedFiles) {

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("").header("Accept-Encoding", "gzip"));
		gzippedFiles.create("foo.css");
		String file = "foo-e36d2e05253c6c7085a91522ce43a0b4.css";
		Resource actual = this.resolver.resolveResource(exchange, file, this.locations).block(TIMEOUT);

		assertThat(actual.getDescription()).isEqualTo(getResource("foo.css.gz").getDescription());
		assertThat(actual.getFilename()).isEqualTo(getResource("foo.css").getFilename());
		boolean condition = actual instanceof HttpResource;
		assertThat(condition).isTrue();
	}

	@Test
	public void resolveFromCacheWithEncodingVariants(GzippedFiles gzippedFiles) {

		// 1. Resolve, and cache .gz variant

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("").header("Accept-Encoding", "gzip"));

		String file = "js/foo.js";
		gzippedFiles.create(file);
		Resource resolved = this.resolver.resolveResource(exchange, file, this.locations).block(TIMEOUT);

		assertThat(resolved.getDescription()).isEqualTo(getResource(file + ".gz").getDescription());
		assertThat(resolved.getFilename()).isEqualTo(getResource(file).getFilename());
		boolean condition = resolved instanceof HttpResource;
		assertThat(condition).isTrue();

		// 2. Resolve unencoded resource

		exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/js/foo.js"));
		resolved = this.resolver.resolveResource(exchange, file, this.locations).block(TIMEOUT);

		assertThat(resolved.getDescription()).isEqualTo(getResource(file).getDescription());
		assertThat(resolved.getFilename()).isEqualTo(getResource(file).getFilename());
		boolean condition1 = resolved instanceof HttpResource;
		assertThat(condition1).isFalse();
	}

	@Test  // SPR-13149
	public void resolveWithNullRequest() {

		String file = "js/foo.js";
		Resource resolved = this.resolver.resolveResource(null, file, this.locations).block(TIMEOUT);

		assertThat(resolved.getDescription()).isEqualTo(getResource(file).getDescription());
		assertThat(resolved.getFilename()).isEqualTo(getResource(file).getFilename());
	}

	private Resource getResource(String filePath) {
		return new ClassPathResource("test/" + filePath, getClass());
	}

}
