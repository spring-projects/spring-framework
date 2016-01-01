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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;

import static org.junit.Assert.*;

/**
 * Unit tests for {@code ResourceUrlEncodingFilter}.
 *
 * @author Brian Clozel
 */
public class ResourceUrlEncodingFilterTests {

	private ResourceUrlEncodingFilter filter;

	private ResourceUrlProvider resourceUrlProvider;

	@Before
	public void createFilter() throws Exception {
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));
		PathResourceResolver pathResolver = new PathResourceResolver();
		pathResolver.setAllowedLocations(new ClassPathResource("test/", getClass()));
		List<ResourceResolver> resolvers = Arrays.asList(versionResolver, pathResolver);

		this.filter = new ResourceUrlEncodingFilter();
		this.resourceUrlProvider = createResourceUrlProvider(resolvers);
	}

	@Test
	public void encodeURL() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.setAttribute(ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR, this.resourceUrlProvider);
		MockHttpServletResponse response = new MockHttpServletResponse();

		this.filter.doFilterInternal(request, response, new FilterChain() {
			@Override
			public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
				String result = ((HttpServletResponse)response).encodeURL("/resources/bar.css");
				assertEquals("/resources/bar-11e16cf79faee7ac698c805cf28248d2.css", result);
			}
		});
	}

	@Test
	public void encodeURLWithContext() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/context/foo");
		request.setContextPath("/context");
		request.setAttribute(ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR, this.resourceUrlProvider);
		MockHttpServletResponse response = new MockHttpServletResponse();

		this.filter.doFilterInternal(request, response, new FilterChain() {
			@Override
			public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
				String result = ((HttpServletResponse)response).encodeURL("/context/resources/bar.css");
				assertEquals("/context/resources/bar-11e16cf79faee7ac698c805cf28248d2.css", result);
			}
		});
	}

	// SPR-13757
	@Test
	public void encodeContextPathUrlWithoutSuffix() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/context");
		request.setContextPath("/context");
		request.setAttribute(ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR, this.resourceUrlProvider);
		MockHttpServletResponse response = new MockHttpServletResponse();

		this.filter.doFilterInternal(request, response, (request1, response1) -> {
			String result = ((HttpServletResponse) response1).encodeURL("/context/resources/bar.css");
			assertEquals("/context/resources/bar-11e16cf79faee7ac698c805cf28248d2.css", result);
		});
	}

	@Test
	public void encodeContextPathUrlWithSuffix() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/context/");
		request.setContextPath("/context");
		request.setAttribute(ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR, this.resourceUrlProvider);
		MockHttpServletResponse response = new MockHttpServletResponse();

		this.filter.doFilterInternal(request, response, (request1, response1) -> {
			String result = ((HttpServletResponse) response1).encodeURL("/context/resources/bar.css");
			assertEquals("/context/resources/bar-11e16cf79faee7ac698c805cf28248d2.css", result);
		});
	}

	// SPR-13018
	@Test
	public void encodeEmptyURLWithContext() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/context/foo");
		request.setContextPath("/context");
		request.setAttribute(ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR, this.resourceUrlProvider);
		MockHttpServletResponse response = new MockHttpServletResponse();

		this.filter.doFilterInternal(request, response, new FilterChain() {
			@Override
			public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
				String result = ((HttpServletResponse)response).encodeURL("?foo=1");
				assertEquals("?foo=1", result);
			}
		});
	}

	// SPR-13374
	@Test
	public void encodeURLWithRequestParams() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setContextPath("/");
		request.setAttribute(ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR, this.resourceUrlProvider);
		MockHttpServletResponse response = new MockHttpServletResponse();

		this.filter.doFilterInternal(request, response, new FilterChain() {
			@Override
			public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
				String result = ((HttpServletResponse)response).encodeURL("/resources/bar.css?foo=bar&url=http://example.org");
				assertEquals("/resources/bar-11e16cf79faee7ac698c805cf28248d2.css?foo=bar&url=http://example.org", result);
			}
		});
	}

	protected ResourceUrlProvider createResourceUrlProvider(List<ResourceResolver> resolvers) {
		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setLocations(Arrays.asList(new ClassPathResource("test/", getClass())));
		handler.setResourceResolvers(resolvers);
		ResourceUrlProvider urlProvider = new ResourceUrlProvider();
		urlProvider.setHandlerMap(Collections.singletonMap("/resources/**", handler));
		return urlProvider;
	}

}
