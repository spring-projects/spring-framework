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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * Tests for {@link VersionResourceResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 */
class VersionResourceResolverTests {

	private List<Resource> locations = List.of(
			new ClassPathResource("test/", getClass()),
			new ClassPathResource("testalternatepath/", getClass()));

	private ResourceResolverChain chain = mock();

	private VersionStrategy versionStrategy = mock();

	private VersionResourceResolver resolver = new VersionResourceResolver();


	@Test
	void resolveResourceExisting() {
		String file = "bar.css";
		Resource expected = new ClassPathResource("test/" + file, getClass());
		given(this.chain.resolveResource(null, file, this.locations)).willReturn(Mono.just(expected));

		this.resolver.setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
		Resource actual = this.resolver
				.resolveResourceInternal(null, file, this.locations, this.chain)
				.block(Duration.ofMillis(5000));

		assertThat(actual).isEqualTo(expected);
		verify(this.chain, times(1)).resolveResource(null, file, this.locations);
		verify(this.versionStrategy, never()).extractVersion(file);
	}

	@Test
	void resolveResourceNoVersionStrategy() {
		String file = "missing.css";
		given(this.chain.resolveResource(null, file, this.locations)).willReturn(Mono.empty());

		this.resolver.setStrategyMap(Collections.emptyMap());
		Resource actual = this.resolver
				.resolveResourceInternal(null, file, this.locations, this.chain)
				.block(Duration.ofMillis(5000));

		assertThat(actual).isNull();
		verify(this.chain, times(1)).resolveResource(null, file, this.locations);
	}

	@Test
	void resolveResourceNoVersionInPath() {
		String file = "bar.css";
		given(this.chain.resolveResource(null, file, this.locations)).willReturn(Mono.empty());
		given(this.versionStrategy.extractVersion(file)).willReturn("");

		this.resolver.setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
		Resource actual = this.resolver
				.resolveResourceInternal(null, file, this.locations, this.chain)
				.block(Duration.ofMillis(5000));

		assertThat(actual).isNull();
		verify(this.chain, times(1)).resolveResource(null, file, this.locations);
		verify(this.versionStrategy, times(1)).extractVersion(file);
	}

	@Test
	void resolveResourceNoResourceAfterVersionRemoved() {
		String versionFile = "bar-version.css";
		String version = "version";
		String file = "bar.css";
		given(this.chain.resolveResource(null, versionFile, this.locations)).willReturn(Mono.empty());
		given(this.chain.resolveResource(null, file, this.locations)).willReturn(Mono.empty());
		given(this.versionStrategy.extractVersion(versionFile)).willReturn(version);
		given(this.versionStrategy.removeVersion(versionFile, version)).willReturn(file);

		this.resolver.setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
		Resource actual = this.resolver
				.resolveResourceInternal(null, versionFile, this.locations, this.chain)
				.block(Duration.ofMillis(5000));

		assertThat(actual).isNull();
		verify(this.versionStrategy, times(1)).removeVersion(versionFile, version);
	}

	@Test
	void resolveResourceVersionDoesNotMatch() {
		String versionFile = "bar-version.css";
		String version = "version";
		String file = "bar.css";
		Resource expected = new ClassPathResource("test/" + file, getClass());
		given(this.chain.resolveResource(null, versionFile, this.locations)).willReturn(Mono.empty());
		given(this.chain.resolveResource(null, file, this.locations)).willReturn(Mono.just(expected));
		given(this.versionStrategy.extractVersion(versionFile)).willReturn(version);
		given(this.versionStrategy.removeVersion(versionFile, version)).willReturn(file);
		given(this.versionStrategy.getResourceVersion(expected)).willReturn(Mono.just("newer-version"));

		this.resolver.setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
		Resource actual = this.resolver
				.resolveResourceInternal(null, versionFile, this.locations, this.chain)
				.block(Duration.ofMillis(5000));

		assertThat(actual).isNull();
		verify(this.versionStrategy, times(1)).getResourceVersion(expected);
	}

	@Test
	void resolveResourceSuccess() {
		String versionFile = "bar-version.css";
		String version = "version";
		String file = "bar.css";
		Resource expected = new ClassPathResource("test/" + file, getClass());
		MockServerHttpRequest request = MockServerHttpRequest.get("/resources/bar-version.css").build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		given(this.chain.resolveResource(exchange, versionFile, this.locations)).willReturn(Mono.empty());
		given(this.chain.resolveResource(exchange, file, this.locations)).willReturn(Mono.just(expected));
		given(this.versionStrategy.extractVersion(versionFile)).willReturn(version);
		given(this.versionStrategy.removeVersion(versionFile, version)).willReturn(file);
		given(this.versionStrategy.getResourceVersion(expected)).willReturn(Mono.just(version));

		this.resolver.setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
		Resource actual = this.resolver
				.resolveResourceInternal(exchange, versionFile, this.locations, this.chain)
				.block(Duration.ofMillis(5000));

		assertThat(actual.getFilename()).isEqualTo(expected.getFilename());
		verify(this.versionStrategy, times(1)).getResourceVersion(expected);
		assertThat(actual).isInstanceOf(HttpResource.class);
		assertThat(((HttpResource) actual).getResponseHeaders().getETag()).isEqualTo(("W/\"" + version + "\""));
	}

	@Test
	void getStrategyForPath() {
		Map<String, VersionStrategy> strategies = new HashMap<>();
		VersionStrategy jsStrategy = mock();
		VersionStrategy catchAllStrategy = mock();
		strategies.put("/**", catchAllStrategy);
		strategies.put("/**/*.js", jsStrategy);
		this.resolver.setStrategyMap(strategies);

		assertThat(this.resolver.getStrategyForPath("foo.css")).isEqualTo(catchAllStrategy);
		assertThat(this.resolver.getStrategyForPath("foo-js.css")).isEqualTo(catchAllStrategy);
		assertThat(this.resolver.getStrategyForPath("foo.js")).isEqualTo(jsStrategy);
		assertThat(this.resolver.getStrategyForPath("bar/foo.js")).isEqualTo(jsStrategy);
	}

	@Test // SPR-13883
	void shouldConfigureFixedPrefixAutomatically() {
		this.resolver.addFixedVersionStrategy("fixedversion", "/js/**", "/css/**", "/fixedversion/css/**");

		assertThat(this.resolver.getStrategyMap()).hasSize(4);

		assertThat(this.resolver.getStrategyForPath("js/something.js"))
				.isInstanceOf(FixedVersionStrategy.class);

		assertThat(this.resolver.getStrategyForPath("fixedversion/js/something.js"))
				.isInstanceOf(FixedVersionStrategy.class);

		assertThat(this.resolver.getStrategyForPath("css/something.css"))
				.isInstanceOf(FixedVersionStrategy.class);

		assertThat(this.resolver.getStrategyForPath("fixedversion/css/something.css"))
				.isInstanceOf(FixedVersionStrategy.class);
	}

	@Test // SPR-15372
	void resolveUrlPathNoVersionStrategy() {
		given(this.chain.resolveUrlPath("/foo.css", this.locations)).willReturn(Mono.just("/foo.css"));
		String resolved = this.resolver.resolveUrlPathInternal("/foo.css", this.locations, this.chain)
				.block(Duration.ofMillis(1000));
		assertThat(resolved).isEqualTo("/foo.css");
	}

}
