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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.DefaultWebSessionManager;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;

/**
 * Unit tests for {@link VersionResourceResolver}.
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class VersionResourceResolverTests {

	private List<Resource> locations;

	private VersionResourceResolver resolver;

	private ResourceResolverChain chain;

	private VersionStrategy versionStrategy;


	@Before
	public void setup() {
		this.locations = new ArrayList<>();
		this.locations.add(new ClassPathResource("test/", getClass()));
		this.locations.add(new ClassPathResource("testalternatepath/", getClass()));

		this.resolver = new VersionResourceResolver();
		this.chain = mock(ResourceResolverChain.class);
		this.versionStrategy = mock(VersionStrategy.class);
	}

	@Test
	public void resolveResourceExisting() throws Exception {
		String file = "bar.css";
		Resource expected = new ClassPathResource("test/" + file, getClass());
		given(this.chain.resolveResource(null, file, this.locations)).willReturn(expected);

		this.resolver.setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
		Resource actual = this.resolver.resolveResourceInternal(null, file, this.locations, this.chain);
		assertEquals(expected, actual);
		verify(this.chain, times(1)).resolveResource(null, file, this.locations);
		verify(this.versionStrategy, never()).extractVersion(file);
	}

	@Test
	public void resolveResourceNoVersionStrategy() throws Exception {
		String file = "missing.css";
		given(this.chain.resolveResource(null, file, this.locations)).willReturn(null);

		this.resolver.setStrategyMap(Collections.emptyMap());
		Resource actual = this.resolver.resolveResourceInternal(null, file, this.locations, this.chain);
		assertNull(actual);
		verify(this.chain, times(1)).resolveResource(null, file, this.locations);
	}

	@Test
	public void resolveResourceNoVersionInPath() throws Exception {
		String file = "bar.css";
		given(this.chain.resolveResource(null, file, this.locations)).willReturn(null);
		given(this.versionStrategy.extractVersion(file)).willReturn("");

		this.resolver.setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
		Resource actual = this.resolver.resolveResourceInternal(null, file, this.locations, this.chain);
		assertNull(actual);
		verify(this.chain, times(1)).resolveResource(null, file, this.locations);
		verify(this.versionStrategy, times(1)).extractVersion(file);
	}

	@Test
	public void resolveResourceNoResourceAfterVersionRemoved() throws Exception {
		String versionFile = "bar-version.css";
		String version = "version";
		String file = "bar.css";
		given(this.chain.resolveResource(null, versionFile, this.locations)).willReturn(null);
		given(this.chain.resolveResource(null, file, this.locations)).willReturn(null);
		given(this.versionStrategy.extractVersion(versionFile)).willReturn(version);
		given(this.versionStrategy.removeVersion(versionFile, version)).willReturn(file);

		this.resolver.setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
		Resource actual = this.resolver.resolveResourceInternal(null, versionFile, this.locations, this.chain);
		assertNull(actual);
		verify(this.versionStrategy, times(1)).removeVersion(versionFile, version);
	}

	@Test
	public void resolveResourceVersionDoesNotMatch() throws Exception {
		String versionFile = "bar-version.css";
		String version = "version";
		String file = "bar.css";
		Resource expected = new ClassPathResource("test/" + file, getClass());
		given(this.chain.resolveResource(null, versionFile, this.locations)).willReturn(null);
		given(this.chain.resolveResource(null, file, this.locations)).willReturn(expected);
		given(this.versionStrategy.extractVersion(versionFile)).willReturn(version);
		given(this.versionStrategy.removeVersion(versionFile, version)).willReturn(file);
		given(this.versionStrategy.getResourceVersion(expected)).willReturn("newer-version");

		this.resolver.setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
		Resource actual = this.resolver.resolveResourceInternal(null, versionFile, this.locations, this.chain);
		assertNull(actual);
		verify(this.versionStrategy, times(1)).getResourceVersion(expected);
	}

	@Test
	public void resolveResourceSuccess() throws Exception {
		String versionFile = "bar-version.css";
		String version = "version";
		String file = "bar.css";
		Resource expected = new ClassPathResource("test/" + file, getClass());
		MockServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, "/resources/bar-version.css");
		MockServerHttpResponse response = new MockServerHttpResponse();
		DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
		ServerWebExchange exchange = new DefaultServerWebExchange(request, response, sessionManager);
		given(this.chain.resolveResource(exchange, versionFile, this.locations)).willReturn(null);
		given(this.chain.resolveResource(exchange, file, this.locations)).willReturn(expected);
		given(this.versionStrategy.extractVersion(versionFile)).willReturn(version);
		given(this.versionStrategy.removeVersion(versionFile, version)).willReturn(file);
		given(this.versionStrategy.getResourceVersion(expected)).willReturn(version);

		this.resolver.setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
		Resource actual = this.resolver.resolveResourceInternal(exchange, versionFile, this.locations, this.chain);
		assertEquals(expected.getFilename(), actual.getFilename());
		verify(this.versionStrategy, times(1)).getResourceVersion(expected);
		assertThat(actual, instanceOf(ResolvedResource.class));
		assertEquals("\"" + version + "\"", ((ResolvedResource)actual).getResponseHeaders().getETag());
	}

	@Test
	public void getStrategyForPath() throws Exception {
		Map<String, VersionStrategy> strategies = new HashMap<>();
		VersionStrategy jsStrategy = mock(VersionStrategy.class);
		VersionStrategy catchAllStrategy = mock(VersionStrategy.class);
		strategies.put("/**", catchAllStrategy);
		strategies.put("/**/*.js", jsStrategy);
		this.resolver.setStrategyMap(strategies);

		assertEquals(catchAllStrategy, this.resolver.getStrategyForPath("foo.css"));
		assertEquals(catchAllStrategy, this.resolver.getStrategyForPath("foo-js.css"));
		assertEquals(jsStrategy, this.resolver.getStrategyForPath("foo.js"));
		assertEquals(jsStrategy, this.resolver.getStrategyForPath("bar/foo.js"));
	}

	@Test // SPR-13883
	public void shouldConfigureFixedPrefixAutomatically() throws Exception {

		this.resolver.addFixedVersionStrategy("fixedversion", "/js/**", "/css/**", "/fixedversion/css/**");

		assertThat(this.resolver.getStrategyMap().size(), is(4));

		assertThat(this.resolver.getStrategyForPath("js/something.js"),
				Matchers.instanceOf(FixedVersionStrategy.class));

		assertThat(this.resolver.getStrategyForPath("fixedversion/js/something.js"),
				Matchers.instanceOf(FixedVersionStrategy.class));

		assertThat(this.resolver.getStrategyForPath("css/something.css"),
				Matchers.instanceOf(FixedVersionStrategy.class));

		assertThat(this.resolver.getStrategyForPath("fixedversion/css/something.css"),
				Matchers.instanceOf(FixedVersionStrategy.class));
	}


}
