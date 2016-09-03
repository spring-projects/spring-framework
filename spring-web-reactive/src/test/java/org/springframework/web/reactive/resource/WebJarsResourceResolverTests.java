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

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;

/**
 * Unit tests for {@link WebJarsResourceResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class WebJarsResourceResolverTests {

	private List<Resource> locations;

	private WebJarsResourceResolver resolver;

	private ResourceResolverChain chain;

	private ServerWebExchange exchange;


	@Before
	public void setup() {
		// for this to work, an actual WebJar must be on the test classpath
		this.locations = Collections.singletonList(new ClassPathResource("/META-INF/resources/webjars"));
		this.resolver = new WebJarsResourceResolver();
		this.chain = mock(ResourceResolverChain.class);

		MockServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, "");
		ServerHttpResponse response = new MockServerHttpResponse();
		WebSessionManager manager = new DefaultWebSessionManager();
		this.exchange = new DefaultServerWebExchange(request, response, manager);
	}


	@Test
	public void resolveUrlExisting() {
		this.locations = Collections.singletonList(new ClassPathResource("/META-INF/resources/webjars/", getClass()));
		String file = "/foo/2.3/foo.txt";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(file);

		String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain);

		assertEquals(file, actual);
		verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
	}

	@Test
	public void resolveUrlExistingNotInJarFile() {
		this.locations = Collections.singletonList(new ClassPathResource("/META-INF/resources/webjars/", getClass()));
		String file = "foo/foo.txt";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);

		String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain);

		assertNull(actual);
		verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
		verify(this.chain, never()).resolveUrlPath("foo/2.3/foo.txt", this.locations);
	}

	@Test
	public void resolveUrlWebJarResource() {
		String file = "underscorejs/underscore.js";
		String expected = "underscorejs/1.8.3/underscore.js";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);
		given(this.chain.resolveUrlPath(expected, this.locations)).willReturn(expected);

		String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain);

		assertEquals(expected, actual);
		verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
		verify(this.chain, times(1)).resolveUrlPath(expected, this.locations);
	}

	@Test
	public void resolveUrlWebJarResourceNotFound() {
		String file = "something/something.js";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);

		String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain);

		assertNull(actual);
		verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
		verify(this.chain, never()).resolveUrlPath(null, this.locations);
	}

	@Test
	public void resolveResourceExisting() {
		Resource expected = mock(Resource.class);
		this.locations = Collections.singletonList(new ClassPathResource("/META-INF/resources/webjars/", getClass()));
		String file = "foo/2.3/foo.txt";
		given(this.chain.resolveResource(this.exchange, file, this.locations)).willReturn(expected);

		Resource actual = this.resolver.resolveResource(this.exchange, file, this.locations, this.chain);

		assertEquals(expected, actual);
		verify(this.chain, times(1)).resolveResource(this.exchange, file, this.locations);
	}

	@Test
	public void resolveResourceNotFound() {
		String file = "something/something.js";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);

		Resource actual = this.resolver.resolveResource(this.exchange, file, this.locations, this.chain);

		assertNull(actual);
		verify(this.chain, times(1)).resolveResource(this.exchange, file, this.locations);
		verify(this.chain, never()).resolveResource(this.exchange, null, this.locations);
	}

	@Test
	public void resolveResourceWebJar() {
		Resource expected = mock(Resource.class);
		String file = "underscorejs/underscore.js";
		String expectedPath = "underscorejs/1.8.3/underscore.js";
		this.locations = Collections.singletonList(new ClassPathResource("/META-INF/resources/webjars/", getClass()));
		given(this.chain.resolveResource(this.exchange, expectedPath, this.locations)).willReturn(expected);

		Resource actual = this.resolver.resolveResource(this.exchange, file, this.locations, this.chain);

		assertEquals(expected, actual);
		verify(this.chain, times(1)).resolveResource(this.exchange, file, this.locations);
	}

}
