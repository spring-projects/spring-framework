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

package org.springframework.web.servlet.resource;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.test.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

	private HttpServletRequest request = new MockHttpServletRequest();


	@BeforeEach
	public void setup() {
		// for this to work, an actual WebJar must be on the test classpath
		this.locations = Collections.singletonList(new ClassPathResource("/META-INF/resources/webjars"));
		this.resolver = new WebJarsResourceResolver();
		this.chain = mock(ResourceResolverChain.class);
	}


	@Test
	public void resolveUrlExisting() {
		this.locations = Collections.singletonList(new ClassPathResource("/META-INF/resources/webjars/", getClass()));
		String file = "/foo/2.3/foo.txt";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(file);

		String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain);

		assertThat(actual).isEqualTo(file);
		verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
	}

	@Test
	public void resolveUrlExistingNotInJarFile() {
		this.locations = Collections.singletonList(new ClassPathResource("/META-INF/resources/webjars/", getClass()));
		String file = "foo/foo.txt";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);

		String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain);

		assertThat(actual).isNull();
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

		assertThat(actual).isEqualTo(expected);
		verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
		verify(this.chain, times(1)).resolveUrlPath(expected, this.locations);
	}

	@Test
	public void resolveUrlWebJarResourceNotFound() {
		String file = "something/something.js";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);

		String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain);

		assertThat(actual).isNull();
		verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
		verify(this.chain, never()).resolveUrlPath(null, this.locations);
	}

	@Test
	public void resolveResourceExisting() {
		Resource expected = mock(Resource.class);
		this.locations = Collections.singletonList(new ClassPathResource("/META-INF/resources/webjars/", getClass()));
		String file = "foo/2.3/foo.txt";
		given(this.chain.resolveResource(this.request, file, this.locations)).willReturn(expected);

		Resource actual = this.resolver.resolveResource(this.request, file, this.locations, this.chain);

		assertThat(actual).isEqualTo(expected);
		verify(this.chain, times(1)).resolveResource(this.request, file, this.locations);
	}

	@Test
	public void resolveResourceNotFound() {
		String file = "something/something.js";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);

		Resource actual = this.resolver.resolveResource(this.request, file, this.locations, this.chain);

		assertThat(actual).isNull();
		verify(this.chain, times(1)).resolveResource(this.request, file, this.locations);
		verify(this.chain, never()).resolveResource(this.request, null, this.locations);
	}

	@Test
	public void resolveResourceWebJar() {
		Resource expected = mock(Resource.class);
		String file = "underscorejs/underscore.js";
		String expectedPath = "underscorejs/1.8.3/underscore.js";
		this.locations = Collections.singletonList(new ClassPathResource("/META-INF/resources/webjars/", getClass()));
		given(this.chain.resolveResource(this.request, expectedPath, this.locations)).willReturn(expected);

		Resource actual = this.resolver.resolveResource(this.request, file, this.locations, this.chain);

		assertThat(actual).isEqualTo(expected);
		verify(this.chain, times(1)).resolveResource(this.request, file, this.locations);
	}

}
