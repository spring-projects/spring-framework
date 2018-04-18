/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.servlet.support;

import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponents;

import static org.junit.Assert.*;

/**
 * Unit tests for
 * {@link org.springframework.web.servlet.support.ServletUriComponentsBuilder}.
 *
 * @author Rossen Stoyanchev
 */
public class ServletUriComponentsBuilderTests {

	private MockHttpServletRequest request;


	@Before
	public void setup() {
		this.request = new MockHttpServletRequest();
		this.request.setScheme("http");
		this.request.setServerName("localhost");
		this.request.setServerPort(-1);
		this.request.setRequestURI("/mvc-showcase");
		this.request.setContextPath("/mvc-showcase");
	}


	@Test
	public void fromRequest() {
		this.request.setRequestURI("/mvc-showcase/data/param");
		this.request.setQueryString("foo=123");
		String result = ServletUriComponentsBuilder.fromRequest(this.request).build().toUriString();
		assertEquals("http://localhost/mvc-showcase/data/param?foo=123", result);
	}

	@Test
	public void fromRequestEncodedPath() {
		this.request.setRequestURI("/mvc-showcase/data/foo%20bar");
		String result = ServletUriComponentsBuilder.fromRequest(this.request).build().toUriString();
		assertEquals("http://localhost/mvc-showcase/data/foo%20bar", result);
	}

	@Test
	public void fromRequestAtypicalHttpPort() {
		this.request.setServerPort(8080);
		String result = ServletUriComponentsBuilder.fromRequest(this.request).build().toUriString();
		assertEquals("http://localhost:8080/mvc-showcase", result);
	}

	@Test
	public void fromRequestAtypicalHttpsPort() {
		this.request.setScheme("https");
		this.request.setServerPort(9043);
		String result = ServletUriComponentsBuilder.fromRequest(this.request).build().toUriString();
		assertEquals("https://localhost:9043/mvc-showcase", result);
	}

	// Most X-Forwarded-* tests in UriComponentsBuilderTests

	@Test
	public void fromRequestWithForwardedHostAndPort() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(80);
		request.setRequestURI("/mvc-showcase");
		request.addHeader("X-Forwarded-Proto", "https");
		request.addHeader("X-Forwarded-Host", "84.198.58.199");
		request.addHeader("X-Forwarded-Port", "443");
		UriComponents result =  ServletUriComponentsBuilder.fromRequest(request).build();
		assertEquals("https://84.198.58.199/mvc-showcase", result.toString());
	}

	@Test
	public void fromRequestUri() {
		this.request.setRequestURI("/mvc-showcase/data/param");
		this.request.setQueryString("foo=123");
		String result = ServletUriComponentsBuilder.fromRequestUri(this.request).build().toUriString();
		assertEquals("http://localhost/mvc-showcase/data/param", result);
	}

	@Test // SPR-16650
	public void fromRequestWithForwardedPrefix() {
		this.request.addHeader("X-Forwarded-Prefix", "/prefix");
		this.request.setContextPath("/mvc-showcase");
		this.request.setRequestURI("/mvc-showcase/bar");
		UriComponents result = ServletUriComponentsBuilder.fromRequest(this.request).build();
		assertEquals("http://localhost/prefix/bar", result.toUriString());
	}

	@Test // SPR-16650
	public void fromRequestWithForwardedPrefixTrailingSlash() {
		this.request.addHeader("X-Forwarded-Prefix", "/foo/");
		this.request.setContextPath("/spring-mvc-showcase");
		this.request.setRequestURI("/spring-mvc-showcase/bar");
		UriComponents result = ServletUriComponentsBuilder.fromRequest(this.request).build();
		assertEquals("http://localhost/foo/bar", result.toUriString());
	}

	@Test // SPR-16650
	public void fromRequestWithForwardedPrefixRoot() {
		this.request.addHeader("X-Forwarded-Prefix", "/");
		this.request.setContextPath("/mvc-showcase");
		this.request.setRequestURI("/mvc-showcase/bar");
		UriComponents result = ServletUriComponentsBuilder.fromRequest(this.request).build();
		assertEquals("http://localhost/bar", result.toUriString());
	}

	@Test
	public void fromContextPath() {
		this.request.setRequestURI("/mvc-showcase/data/param");
		this.request.setQueryString("foo=123");
		String result = ServletUriComponentsBuilder.fromContextPath(this.request).build().toUriString();
		assertEquals("http://localhost/mvc-showcase", result);
	}

	@Test // SPR-16650
	public void fromContextPathWithForwardedPrefix() {
		this.request.addHeader("X-Forwarded-Prefix", "/prefix");
		this.request.setContextPath("/mvc-showcase");
		this.request.setRequestURI("/mvc-showcase/simple");
		String result = ServletUriComponentsBuilder.fromContextPath(this.request).build().toUriString();
		assertEquals("http://localhost/prefix", result);
	}

	@Test
	public void fromServletMapping() {
		this.request.setRequestURI("/mvc-showcase/app/simple");
		this.request.setServletPath("/app");
		this.request.setQueryString("foo=123");
		String result = ServletUriComponentsBuilder.fromServletMapping(this.request).build().toUriString();
		assertEquals("http://localhost/mvc-showcase/app", result);
	}

	@Test // SPR-16650
	public void fromServletMappingWithForwardedPrefix() {
		this.request.addHeader("X-Forwarded-Prefix", "/prefix");
		this.request.setContextPath("/mvc-showcase");
		this.request.setServletPath("/app");
		this.request.setRequestURI("/mvc-showcase/app/simple");
		String result = ServletUriComponentsBuilder.fromServletMapping(this.request).build().toUriString();
		assertEquals("http://localhost/prefix/app", result);
	}

	@Test
	public void fromCurrentRequest() {
		this.request.setRequestURI("/mvc-showcase/data/param");
		this.request.setQueryString("foo=123");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(this.request));
		try {
			String result = ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString();
			assertEquals("http://localhost/mvc-showcase/data/param?foo=123", result);
		}
		finally {
			RequestContextHolder.resetRequestAttributes();
		}
	}

	@Test // SPR-10272
	public void pathExtension() {
		this.request.setRequestURI("/rest/books/6.json");
		ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromRequestUri(this.request);
		String extension = builder.removePathExtension();
		String result = builder.path("/pages/1.{ext}").buildAndExpand(extension).toUriString();
		assertEquals("http://localhost/rest/books/6/pages/1.json", result);
	}

	@Test
	public void pathExtensionNone() {
		this.request.setRequestURI("/rest/books/6");
		ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromRequestUri(this.request);
		assertNull(builder.removePathExtension());
	}
}
