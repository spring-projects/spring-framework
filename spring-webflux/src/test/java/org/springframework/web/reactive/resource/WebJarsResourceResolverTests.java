/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.List;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link WebJarsResourceResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
@SuppressWarnings("removal")
class WebJarsResourceResolverTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(1);


	private List<Resource> locations = List.of(new ClassPathResource("/META-INF/resources/webjars"));

	// for this to work, an actual WebJar must be on the test classpath
	private WebJarsResourceResolver resolver = new WebJarsResourceResolver();

	private ResourceResolverChain chain = mock();

	private ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));


	@Test
	void resolveUrlExisting() {
		String file = "/foo/2.3/foo.txt";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(Mono.just(file));

		String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain).block(TIMEOUT);

		assertThat(actual).isEqualTo(file);
		verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
	}

	@Test
	void resolveUrlExistingNotInJarFile() {
		String file = "foo/foo.txt";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(Mono.empty());

		String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain).block(TIMEOUT);

		assertThat(actual).isNull();
		verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
		verify(this.chain, never()).resolveUrlPath("foo/2.3/foo.txt", this.locations);
	}

	@Test
	void resolveUrlWebJarResource() {
		String file = "underscorejs/underscore.js";
		String expected = "underscorejs/1.8.3/underscore.js";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(Mono.empty());
		given(this.chain.resolveUrlPath(expected, this.locations)).willReturn(Mono.just(expected));

		String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain).block(TIMEOUT);

		assertThat(actual).isEqualTo(expected);
		verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
		verify(this.chain, times(1)).resolveUrlPath(expected, this.locations);
	}

	@Test
	void resolveUrlWebJarResourceNotFound() {
		String file = "something/something.js";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(Mono.empty());

		String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain).block(TIMEOUT);

		assertThat(actual).isNull();
		verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
		verify(this.chain, never()).resolveUrlPath(null, this.locations);
	}

	@Test
	void resolveResourceExisting() {
		Resource expected = mock();
		String file = "foo/2.3/foo.txt";
		given(this.chain.resolveResource(this.exchange, file, this.locations)).willReturn(Mono.just(expected));

		Resource actual = this.resolver
				.resolveResource(this.exchange, file, this.locations, this.chain)
				.block(TIMEOUT);

		assertThat(actual).isEqualTo(expected);
		verify(this.chain, times(1)).resolveResource(this.exchange, file, this.locations);
	}

	@Test
	void resolveResourceNotFound() {
		String file = "something/something.js";
		given(this.chain.resolveResource(this.exchange, file, this.locations)).willReturn(Mono.empty());

		Resource actual = this.resolver
				.resolveResource(this.exchange, file, this.locations, this.chain)
				.block(TIMEOUT);

		assertThat(actual).isNull();
		verify(this.chain, times(1)).resolveResource(this.exchange, file, this.locations);
		verify(this.chain, never()).resolveResource(this.exchange, null, this.locations);
	}

	@Test
	void resolveResourceWebJar() {
		String file = "underscorejs/underscore.js";
		given(this.chain.resolveResource(this.exchange, file, this.locations)).willReturn(Mono.empty());

		Resource expected = mock();
		String expectedPath = "underscorejs/1.8.3/underscore.js";
		given(this.chain.resolveResource(this.exchange, expectedPath, this.locations))
				.willReturn(Mono.just(expected));

		Resource actual = this.resolver
				.resolveResource(this.exchange, file, this.locations, this.chain)
				.block(TIMEOUT);

		assertThat(actual).isEqualTo(expected);
		verify(this.chain, times(1)).resolveResource(this.exchange, file, this.locations);
	}

}
