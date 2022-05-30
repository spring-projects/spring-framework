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

package org.springframework.web.servlet.support;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.testfixture.servlet.MockFilterChain;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.util.UriComponents;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for
 * {@link org.springframework.web.servlet.support.ServletUriComponentsBuilder}.
 *
 * @author Rossen Stoyanchev
 */
public class ServletUriComponentsBuilderTests {

	private MockHttpServletRequest request;


	@BeforeEach
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
		assertThat(result).isEqualTo("http://localhost/mvc-showcase/data/param?foo=123");
	}

	@Test
	public void fromRequestEncodedPath() {
		this.request.setRequestURI("/mvc-showcase/data/foo%20bar");
		String result = ServletUriComponentsBuilder.fromRequest(this.request).build().toUriString();
		assertThat(result).isEqualTo("http://localhost/mvc-showcase/data/foo%20bar");
	}

	@Test
	public void fromRequestAtypicalHttpPort() {
		this.request.setServerPort(8080);
		String result = ServletUriComponentsBuilder.fromRequest(this.request).build().toUriString();
		assertThat(result).isEqualTo("http://localhost:8080/mvc-showcase");
	}

	@Test
	public void fromRequestAtypicalHttpsPort() {
		this.request.setScheme("https");
		this.request.setServerPort(9043);
		String result = ServletUriComponentsBuilder.fromRequest(this.request).build().toUriString();
		assertThat(result).isEqualTo("https://localhost:9043/mvc-showcase");
	}

	// Some X-Forwarded-* tests in addition to the ones in UriComponentsBuilderTests

	@Test
	public void fromRequestWithForwardedHostAndPort() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(80);
		request.setRequestURI("/mvc-showcase");
		request.addHeader("X-Forwarded-Proto", "https");
		request.addHeader("X-Forwarded-Host", "84.198.58.199");
		request.addHeader("X-Forwarded-Port", "443");

		HttpServletRequest requestToUse = adaptFromForwardedHeaders(request);
		UriComponents result =  ServletUriComponentsBuilder.fromRequest(requestToUse).build();

		assertThat(result.toString()).isEqualTo("https://84.198.58.199/mvc-showcase");
	}

	@Test
	public void fromRequestUri() {
		this.request.setRequestURI("/mvc-showcase/data/param");
		this.request.setQueryString("foo=123");
		String result = ServletUriComponentsBuilder.fromRequestUri(this.request).build().toUriString();
		assertThat(result).isEqualTo("http://localhost/mvc-showcase/data/param");
	}

	@Test // SPR-16650
	public void fromRequestWithForwardedPrefix() throws Exception {
		this.request.addHeader("X-Forwarded-Prefix", "/prefix");
		this.request.setContextPath("/mvc-showcase");
		this.request.setRequestURI("/mvc-showcase/bar");

		HttpServletRequest requestToUse = adaptFromForwardedHeaders(this.request);
		UriComponents result =  ServletUriComponentsBuilder.fromRequest(requestToUse).build();

		assertThat(result.toUriString()).isEqualTo("http://localhost/prefix/bar");
	}

	@Test // SPR-16650
	public void fromRequestWithForwardedPrefixTrailingSlash() throws Exception {
		this.request.addHeader("X-Forwarded-Prefix", "/foo/");
		this.request.setContextPath("/spring-mvc-showcase");
		this.request.setRequestURI("/spring-mvc-showcase/bar");

		HttpServletRequest requestToUse = adaptFromForwardedHeaders(this.request);
		UriComponents result =  ServletUriComponentsBuilder.fromRequest(requestToUse).build();

		assertThat(result.toUriString()).isEqualTo("http://localhost/foo/bar");
	}

	@Test // SPR-16650
	public void fromRequestWithForwardedPrefixRoot() throws Exception {
		this.request.addHeader("X-Forwarded-Prefix", "/");
		this.request.setContextPath("/mvc-showcase");
		this.request.setRequestURI("/mvc-showcase/bar");

		HttpServletRequest requestToUse = adaptFromForwardedHeaders(this.request);
		UriComponents result =  ServletUriComponentsBuilder.fromRequest(requestToUse).build();

		assertThat(result.toUriString()).isEqualTo("http://localhost/bar");
	}

	@Test
	public void fromContextPath() {
		this.request.setRequestURI("/mvc-showcase/data/param");
		this.request.setQueryString("foo=123");
		String result = ServletUriComponentsBuilder.fromContextPath(this.request).build().toUriString();
		assertThat(result).isEqualTo("http://localhost/mvc-showcase");
	}

	@Test // SPR-16650
	public void fromContextPathWithForwardedPrefix() throws Exception {
		this.request.addHeader("X-Forwarded-Prefix", "/prefix");
		this.request.setContextPath("/mvc-showcase");
		this.request.setRequestURI("/mvc-showcase/simple");

		HttpServletRequest requestToUse = adaptFromForwardedHeaders(this.request);
		String result = ServletUriComponentsBuilder.fromContextPath(requestToUse).build().toUriString();

		assertThat(result).isEqualTo("http://localhost/prefix");
	}

	@Test
	public void fromServletMapping() {
		this.request.setRequestURI("/mvc-showcase/app/simple");
		this.request.setServletPath("/app");
		this.request.setQueryString("foo=123");
		String result = ServletUriComponentsBuilder.fromServletMapping(this.request).build().toUriString();
		assertThat(result).isEqualTo("http://localhost/mvc-showcase/app");
	}

	@Test // SPR-16650
	public void fromServletMappingWithForwardedPrefix() throws Exception {
		this.request.addHeader("X-Forwarded-Prefix", "/prefix");
		this.request.setContextPath("/mvc-showcase");
		this.request.setServletPath("/app");
		this.request.setRequestURI("/mvc-showcase/app/simple");

		HttpServletRequest requestToUse = adaptFromForwardedHeaders(this.request);
		String result = ServletUriComponentsBuilder.fromServletMapping(requestToUse).build().toUriString();

		assertThat(result).isEqualTo("http://localhost/prefix/app");
	}

	@Test
	public void fromCurrentRequest() {
		this.request.setRequestURI("/mvc-showcase/data/param");
		this.request.setQueryString("foo=123");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(this.request));
		try {
			String result = ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString();
			assertThat(result).isEqualTo("http://localhost/mvc-showcase/data/param?foo=123");
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
		assertThat(result).isEqualTo("http://localhost/rest/books/6/pages/1.json");
	}

	@Test
	public void pathExtensionNone() {
		this.request.setRequestURI("/rest/books/6");
		ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromRequestUri(this.request);
		assertThat(builder.removePathExtension()).isNull();
	}

	// SPR-16668
	private HttpServletRequest adaptFromForwardedHeaders(HttpServletRequest request) throws Exception {
		MockFilterChain chain = new MockFilterChain();
		new ForwardedHeaderFilter().doFilter(request, new MockHttpServletResponse(), chain);
		return (HttpServletRequest) chain.getRequest();
	}

}
