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
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.webjars.WebJarAssetLocator;

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
class WebJarsResourceResolverTests {

	private List<Resource> locations = List.of(new ClassPathResource("/META-INF/resources/webjars"));

	private ResourceResolverChain chain = mock();

	private HttpServletRequest request = new MockHttpServletRequest();


	@SuppressWarnings("removal")
	private static Stream<WebJarsResourceResolver> webJarsResourceResolvers() {
		return Stream.of(
				new WebJarsResourceResolver(),
				new WebJarsResourceResolver(new WebJarAssetLocator())
		);
	}

	@ParameterizedTest
	@MethodSource("webJarsResourceResolvers")
	void resolveUrlExisting(WebJarsResourceResolver resolver) {
		String file = "/foo/2.3/foo.txt";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(file);

		String actual = resolver.resolveUrlPath(file, this.locations, this.chain);

		assertThat(actual).isEqualTo(file);
		verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
	}

	@ParameterizedTest
	@MethodSource("webJarsResourceResolvers")
	void resolveUrlExistingNotInJarFile(WebJarsResourceResolver resolver) {
		String file = "foo/foo.txt";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);

		String actual = resolver.resolveUrlPath(file, this.locations, this.chain);

		assertThat(actual).isNull();
		verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
		verify(this.chain, never()).resolveUrlPath("foo/2.3/foo.txt", this.locations);
	}

	@ParameterizedTest
	@MethodSource("webJarsResourceResolvers")
	void resolveUrlWebJarResource(WebJarsResourceResolver resolver) {
		String file = "underscorejs/underscore.js";
		String expected = "underscorejs/1.8.3/underscore.js";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);
		given(this.chain.resolveUrlPath(expected, this.locations)).willReturn(expected);

		String actual = resolver.resolveUrlPath(file, this.locations, this.chain);

		assertThat(actual).isEqualTo(expected);
		verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
		verify(this.chain, times(1)).resolveUrlPath(expected, this.locations);
	}

	@ParameterizedTest
	@MethodSource("webJarsResourceResolvers")
	void resolveUrlWebJarResourceNotFound(WebJarsResourceResolver resolver) {
		String file = "something/something.js";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);

		String actual = resolver.resolveUrlPath(file, this.locations, this.chain);

		assertThat(actual).isNull();
		verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
		verify(this.chain, never()).resolveUrlPath(null, this.locations);
	}

	@ParameterizedTest
	@MethodSource("webJarsResourceResolvers")
	void resolveResourceExisting(WebJarsResourceResolver resolver) {
		Resource expected = mock();
		String file = "foo/2.3/foo.txt";
		given(this.chain.resolveResource(this.request, file, this.locations)).willReturn(expected);

		Resource actual = resolver.resolveResource(this.request, file, this.locations, this.chain);

		assertThat(actual).isEqualTo(expected);
		verify(this.chain, times(1)).resolveResource(this.request, file, this.locations);
	}

	@ParameterizedTest
	@MethodSource("webJarsResourceResolvers")
	void resolveResourceNotFound(WebJarsResourceResolver resolver) {
		String file = "something/something.js";
		given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);

		Resource actual = resolver.resolveResource(this.request, file, this.locations, this.chain);

		assertThat(actual).isNull();
		verify(this.chain, times(1)).resolveResource(this.request, file, this.locations);
		verify(this.chain, never()).resolveResource(this.request, null, this.locations);
	}

	@ParameterizedTest
	@MethodSource("webJarsResourceResolvers")
	void resolveResourceWebJar(WebJarsResourceResolver resolver) {
		Resource expected = mock();
		String file = "underscorejs/underscore.js";
		String expectedPath = "underscorejs/1.8.3/underscore.js";
		given(this.chain.resolveResource(this.request, expectedPath, this.locations)).willReturn(expected);

		Resource actual = resolver.resolveResource(this.request, file, this.locations, this.chain);

		assertThat(actual).isEqualTo(expected);
		verify(this.chain, times(1)).resolveResource(this.request, file, this.locations);
	}

}
