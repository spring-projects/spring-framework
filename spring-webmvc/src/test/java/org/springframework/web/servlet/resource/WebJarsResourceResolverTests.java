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

package org.springframework.web.servlet.resource;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link WebJarsResourceResolver}.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 */
@SuppressWarnings("removal")
class WebJarsResourceResolverTests {

	private List<Resource> locations = List.of(new ClassPathResource("/META-INF/resources/webjars"));

	// for this to work, an actual WebJar must be on the test classpath
	private WebJarsResourceResolver resolver = new WebJarsResourceResolver();

	private ResourceResolverChain chain = mock();

	private HttpServletRequest request = new MockHttpServletRequest();


	@Test
	void resolveUrlExisting() {
		String file = "/foo/2.3/foo.txt";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(file);

		String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain);

		assertThat(actual).isEqualTo(file);
		verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
	}

	@Test
	void resolveUrlExistingNotInJarFile() {
		String file = "foo/foo.txt";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);

		String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain);

		assertThat(actual).isNull();
		verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
		verify(this.chain, never()).resolveUrlPath("foo/2.3/foo.txt", this.locations);
	}

	@Test
	void resolveUrlWebJarResource() {
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
	void resolveUrlWebJarResourceNotFound() {
		String file = "something/something.js";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);

		String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain);

		assertThat(actual).isNull();
		verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
		verify(this.chain, never()).resolveUrlPath(null, this.locations);
	}

	@Test
	void resolveResourceExisting() {
		Resource expected = mock();
		String file = "foo/2.3/foo.txt";
		given(this.chain.resolveResource(this.request, file, this.locations)).willReturn(expected);

		Resource actual = this.resolver.resolveResource(this.request, file, this.locations, this.chain);

		assertThat(actual).isEqualTo(expected);
		verify(this.chain, times(1)).resolveResource(this.request, file, this.locations);
	}

	@Test
	void resolveResourceNotFound() {
		String file = "something/something.js";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);

		Resource actual = this.resolver.resolveResource(this.request, file, this.locations, this.chain);

		assertThat(actual).isNull();
		verify(this.chain, times(1)).resolveResource(this.request, file, this.locations);
		verify(this.chain, never()).resolveResource(this.request, null, this.locations);
	}

	@Test
	void resolveResourceWebJar() {
		Resource expected = mock();
		String file = "underscorejs/underscore.js";
		String expectedPath = "underscorejs/1.8.3/underscore.js";
		given(this.chain.resolveResource(this.request, expectedPath, this.locations)).willReturn(expected);

		Resource actual = this.resolver.resolveResource(this.request, file, this.locations, this.chain);

		assertThat(actual).isEqualTo(expected);
		verify(this.chain, times(1)).resolveResource(this.request, file, this.locations);
	}

}
