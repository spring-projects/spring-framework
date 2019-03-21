/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.mvc;

import java.util.Properties;

import org.hamcrest.Matchers;
import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author Rick Evans
 * @author Brian Clozel
 */
public class WebContentInterceptorTests {

	private MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");

	private MockHttpServletResponse response = new MockHttpServletResponse();


	@Test
	public void cacheResourcesConfiguration() throws Exception {
		WebContentInterceptor interceptor = new WebContentInterceptor();
		interceptor.setCacheSeconds(10);

		interceptor.preHandle(request, response, null);

		Iterable<String> cacheControlHeaders = response.getHeaders("Cache-Control");
		assertThat(cacheControlHeaders, Matchers.hasItem("max-age=10"));
	}

	@Test
	public void mappedCacheConfigurationOverridesGlobal() throws Exception {
		Properties mappings = new Properties();
		mappings.setProperty("*/*handle.vm", "-1"); // was **/*handle.vm

		WebContentInterceptor interceptor = new WebContentInterceptor();
		interceptor.setCacheSeconds(10);
		interceptor.setCacheMappings(mappings);

		// request.setRequestURI("http://localhost:7070/example/adminhandle.vm");
		request.setRequestURI("example/adminhandle.vm");
		interceptor.preHandle(request, response, null);

		Iterable<String> cacheControlHeaders = response.getHeaders("Cache-Control");
		assertThat(cacheControlHeaders, Matchers.emptyIterable());

		// request.setRequestURI("http://localhost:7070/example/bingo.html");
		request.setRequestURI("example/bingo.html");
		interceptor.preHandle(request, response, null);

		cacheControlHeaders = response.getHeaders("Cache-Control");
		assertThat(cacheControlHeaders, Matchers.hasItem("max-age=10"));
	}

	@Test
	public void preventCacheConfiguration() throws Exception {
		WebContentInterceptor interceptor = new WebContentInterceptor();
		interceptor.setCacheSeconds(0);

		interceptor.preHandle(request, response, null);

		Iterable<String> cacheControlHeaders = response.getHeaders("Cache-Control");
		assertThat(cacheControlHeaders, Matchers.contains("no-store"));
	}

	@Test
	public void emptyCacheConfiguration() throws Exception {
		WebContentInterceptor interceptor = new WebContentInterceptor();
		interceptor.setCacheSeconds(-1);

		interceptor.preHandle(request, response, null);

		Iterable<String> expiresHeaders = response.getHeaders("Expires");
		assertThat(expiresHeaders, Matchers.emptyIterable());
		Iterable<String> cacheControlHeaders = response.getHeaders("Cache-Control");
		assertThat(cacheControlHeaders, Matchers.emptyIterable());
	}

	// SPR-13252, SPR-14053
	@Test
	public void cachingConfigAndPragmaHeader() throws Exception {
		WebContentInterceptor interceptor = new WebContentInterceptor();
		interceptor.setCacheSeconds(10);
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Expires", "0");

		interceptor.preHandle(request, response, null);

		assertThat(response.getHeader("Pragma"), is(""));
		assertThat(response.getHeader("Expires"), is(""));
	}

	// SPR-13252, SPR-14053
	@SuppressWarnings("deprecation")
	@Test
	public void http10CachingConfigAndPragmaHeader() throws Exception {
		WebContentInterceptor interceptor = new WebContentInterceptor();
		interceptor.setCacheSeconds(10);
		interceptor.setAlwaysMustRevalidate(true);
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Expires", "0");

		interceptor.preHandle(request, response, null);

		assertThat(response.getHeader("Pragma"), is(""));
		assertThat(response.getHeader("Expires"), is(""));
	}

	@SuppressWarnings("deprecation")
	@Test
	public void http10CachingConfigAndSpecificMapping() throws Exception {
		WebContentInterceptor interceptor = new WebContentInterceptor();
		interceptor.setCacheSeconds(0);
		interceptor.setUseExpiresHeader(true);
		interceptor.setAlwaysMustRevalidate(true);
		Properties mappings = new Properties();
		mappings.setProperty("*/*.cache.html", "10"); // was **/*.cache.html
		interceptor.setCacheMappings(mappings);

		// request.setRequestURI("https://example.org/foo/page.html");
		request.setRequestURI("foo/page.html");
		interceptor.preHandle(request, response, null);

		Iterable<String> expiresHeaders = response.getHeaders("Expires");
		assertThat(expiresHeaders, Matchers.iterableWithSize(1));
		Iterable<String> cacheControlHeaders = response.getHeaders("Cache-Control");
		assertThat(cacheControlHeaders, Matchers.contains("no-cache", "no-store"));
		Iterable<String> pragmaHeaders = response.getHeaders("Pragma");
		assertThat(pragmaHeaders, Matchers.contains("no-cache"));

		// request.setRequestURI("https://example.org/page.cache.html");
		request = new MockHttpServletRequest("GET", "foo/page.cache.html");
		response = new MockHttpServletResponse();
		interceptor.preHandle(request, response, null);

		expiresHeaders = response.getHeaders("Expires");
		assertThat(expiresHeaders, Matchers.iterableWithSize(1));
		cacheControlHeaders = response.getHeaders("Cache-Control");
		assertThat(cacheControlHeaders, Matchers.contains("max-age=10, must-revalidate"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void throwsExceptionWithNullPathMatcher() throws Exception {
		WebContentInterceptor interceptor = new WebContentInterceptor();
		interceptor.setPathMatcher(null);
	}

}
