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

package org.springframework.web.servlet.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ResourceUrlEncodingFilter}.
 *
 * @author Brian Clozel
 */
class ResourceUrlEncodingFilterTests {

	private ResourceUrlEncodingFilter filter;

	private ResourceUrlProvider urlProvider;


	@BeforeEach
	void createFilter() {
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));
		PathResourceResolver pathResolver = new PathResourceResolver();
		pathResolver.setAllowedLocations(new ClassPathResource("test/", getClass()));
		List<ResourceResolver> resolvers = new ArrayList<>();
		resolvers.add(versionResolver);
		resolvers.add(pathResolver);

		this.filter = new ResourceUrlEncodingFilter();
		this.urlProvider = createResourceUrlProvider(resolvers);
	}

	private ResourceUrlProvider createResourceUrlProvider(List<ResourceResolver> resolvers) {
		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setLocations(Collections.singletonList(new ClassPathResource("test/", getClass())));
		handler.setResourceResolvers(resolvers);

		ResourceUrlProvider urlProvider = new ResourceUrlProvider();
		urlProvider.setHandlerMap(Collections.singletonMap("/resources/**", handler));
		return urlProvider;
	}


	@Test
	void encodeURL() throws Exception {
		testEncodeUrl(new MockHttpServletRequest("GET", "/"),
				"/resources/bar.css", "/resources/bar-11e16cf79faee7ac698c805cf28248d2.css");
	}

	@Test
	void encodeUrlWithContext() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/context/foo");
		request.setContextPath("/context");

		testEncodeUrl(request, "/context/resources/bar.css",
				"/context/resources/bar-11e16cf79faee7ac698c805cf28248d2.css");
	}


	@Test
	void encodeUrlWithContextAndForwardedRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/context/foo");
		request.setContextPath("/context");

		this.filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
			req.setAttribute(ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR, this.urlProvider);
			request.setRequestURI("/forwarded");
			request.setContextPath("/");
			String result = ((HttpServletResponse) res).encodeURL("/context/resources/bar.css");
			assertThat(result).isEqualTo("/context/resources/bar-11e16cf79faee7ac698c805cf28248d2.css");
		});
	}

	@Test // SPR-13757
	public void encodeContextPathUrlWithoutSuffix() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/context");
		request.setContextPath("/context");

		testEncodeUrl(request, "/context/resources/bar.css",
				"/context/resources/bar-11e16cf79faee7ac698c805cf28248d2.css");
	}

	@Test
	void encodeContextPathUrlWithSuffix() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/context/");
		request.setContextPath("/context");

		testEncodeUrl(request, "/context/resources/bar.css",
				"/context/resources/bar-11e16cf79faee7ac698c805cf28248d2.css");
	}

	@Test // SPR-13018
	public void encodeEmptyUrlWithContext() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/context/foo");
		request.setContextPath("/context");

		testEncodeUrl(request, "?foo=1", "?foo=1");
	}

	@Test // SPR-13374
	public void encodeUrlWithRequestParams() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setContextPath("/");

		testEncodeUrl(request, "/resources/bar.css?foo=bar&url=https://example.org",
				"/resources/bar-11e16cf79faee7ac698c805cf28248d2.css?foo=bar&url=https://example.org");
	}

	@Test // SPR-13847
	public void encodeUrlPreventStringOutOfBounds() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/context-path/index");
		request.setContextPath("/context-path");
		request.setServletPath("");

		testEncodeUrl(request, "index?key=value", "index?key=value");
	}

	@Test // SPR-17535
	public void encodeUrlWithFragment() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setContextPath("/");

		testEncodeUrl(request, "/resources/bar.css#something",
				"/resources/bar-11e16cf79faee7ac698c805cf28248d2.css#something");

		testEncodeUrl(request,
				"/resources/bar.css?foo=bar&url=https://example.org#something",
				"/resources/bar-11e16cf79faee7ac698c805cf28248d2.css?foo=bar&url=https://example.org#something");
	}

	@Test // gh-23508
	public void invalidLookupPath() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/a/b/../logo.png");
		request.setServletPath("/a/logo.png");

		this.filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
			ResourceUrlProviderExposingInterceptor interceptor =
					new ResourceUrlProviderExposingInterceptor(this.urlProvider);

			assertThatThrownBy(() -> interceptor.preHandle((HttpServletRequest) req, (HttpServletResponse) res, null))
					.isInstanceOf(ServletRequestBindingException.class);

		});
	}

	private void testEncodeUrl(MockHttpServletRequest request, String url, String expected)
			throws ServletException, IOException {

		FilterChain chain = (req, res) -> {
			req.setAttribute(ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR, this.urlProvider);
			String result = ((HttpServletResponse) res).encodeURL(url);
			assertThat(result).isEqualTo(expected);
		};
		this.filter.doFilter(request, new MockHttpServletResponse(), chain);
	}

}
