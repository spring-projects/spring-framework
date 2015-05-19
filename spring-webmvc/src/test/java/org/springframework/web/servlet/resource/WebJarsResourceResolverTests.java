/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet.resource;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Unit tests for
 * {@link org.springframework.web.servlet.resource.WebJarsResourceResolver}.
 *
 * @author Brian Clozel
 */
public class WebJarsResourceResolverTests {

	private List<Resource> locations;

	private WebJarsResourceResolver resolver;

	private ResourceResolverChain chain;

	@Before
	public void setup() {
		// for this to work, an actual WebJar must be on the test classpath
		this.locations = Collections.singletonList(new ClassPathResource("/META-INF/resources/webjars/"));
		this.resolver = new WebJarsResourceResolver();
		this.chain = mock(ResourceResolverChain.class);
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
		String file = "/foo/foo.txt";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);

		String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain);

		assertNull(actual);
		verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
		verify(this.chain, never()).resolveUrlPath("/foo/2.3/foo.txt", this.locations);
	}

	@Test
	public void resolveUrlWebJarResource() {
		String file = "/underscorejs/underscore.js";
		String expected = "/underscorejs/1.8.2/underscore.js";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);
		given(this.chain.resolveUrlPath(expected, this.locations)).willReturn(expected);

		String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain);

		assertEquals(expected, actual);
		verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
		verify(this.chain, times(1)).resolveUrlPath(expected, this.locations);
	}

	@Test
	public void resolverUrlWebJarResourceNotFound() {
		String file = "/something/something.js";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);

		String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain);

		assertNull(actual);
		verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
	}

}
