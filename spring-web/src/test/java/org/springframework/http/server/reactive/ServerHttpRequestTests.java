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

package org.springframework.http.server.reactive;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import org.junit.Test;

import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.test.DelegatingServletInputStream;
import org.springframework.mock.web.test.MockAsyncContext;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AbstractServerHttpRequest}.
 *
 * @author Rossen Stoyanchev
 */
public class ServerHttpRequestTests {

	@Test
	public void queryParamsNone() throws Exception {
		MultiValueMap<String, String> params = createHttpRequest("/path").getQueryParams();
		assertEquals(0, params.size());
	}

	@Test
	public void queryParams() throws Exception {
		MultiValueMap<String, String> params = createHttpRequest("/path?a=A&b=B").getQueryParams();
		assertEquals(2, params.size());
		assertEquals(Collections.singletonList("A"), params.get("a"));
		assertEquals(Collections.singletonList("B"), params.get("b"));
	}

	@Test
	public void queryParamsWithMulitpleValues() throws Exception {
		MultiValueMap<String, String> params = createHttpRequest("/path?a=1&a=2").getQueryParams();
		assertEquals(1, params.size());
		assertEquals(Arrays.asList("1", "2"), params.get("a"));
	}

	@Test  // SPR-15140
	public void queryParamsWithEncodedValue() throws Exception {
		MultiValueMap<String, String> params = createHttpRequest("/path?a=%20%2B+%C3%A0").getQueryParams();
		assertEquals(1, params.size());
		assertEquals(Collections.singletonList(" + \u00e0"), params.get("a"));
	}

	@Test
	public void queryParamsWithEmptyValue() throws Exception {
		MultiValueMap<String, String> params = createHttpRequest("/path?a=").getQueryParams();
		assertEquals(1, params.size());
		assertEquals(Collections.singletonList(""), params.get("a"));
	}

	@Test
	public void queryParamsWithNoValue() throws Exception {
		MultiValueMap<String, String> params = createHttpRequest("/path?a").getQueryParams();
		assertEquals(1, params.size());
		assertEquals(Collections.singletonList(null), params.get("a"));
	}

	@Test
	public void mutateRequest() throws Exception {

		SslInfo sslInfo = mock(SslInfo.class);
		ServerHttpRequest request = createHttpRequest("/").mutate().sslInfo(sslInfo).build();
		assertSame(sslInfo, request.getSslInfo());

		request = createHttpRequest("/").mutate().method(HttpMethod.DELETE).build();
		assertEquals(HttpMethod.DELETE, request.getMethod());

		String baseUri = "http://aaa.org:8080/a";

		request = createHttpRequest(baseUri).mutate().uri(URI.create("http://bbb.org:9090/b")).build();
		assertEquals("http://bbb.org:9090/b", request.getURI().toString());

		request = createHttpRequest(baseUri).mutate().path("/b/c/d").build();
		assertEquals("http://aaa.org:8080/b/c/d", request.getURI().toString());

		request = createHttpRequest(baseUri).mutate().path("/app/b/c/d").contextPath("/app").build();
		assertEquals("http://aaa.org:8080/app/b/c/d", request.getURI().toString());
		assertEquals("/app", request.getPath().contextPath().value());
	}

	@Test(expected = IllegalArgumentException.class)
	public void mutateWithInvalidPath() throws Exception {
		createHttpRequest("/").mutate().path("foo-bar");
	}

	@Test  // SPR-16434
	public void mutatePathWithEncodedQueryParams() throws Exception {
		ServerHttpRequest request = createHttpRequest("/path?name=%E6%89%8E%E6%A0%B9");
		request = request.mutate().path("/mutatedPath").build();

		assertEquals("/mutatedPath", request.getURI().getRawPath());
		assertEquals("name=%E6%89%8E%E6%A0%B9", request.getURI().getRawQuery());
	}

	private ServerHttpRequest createHttpRequest(String uriString) throws Exception {
		URI uri = URI.create(uriString);
		MockHttpServletRequest request = new TestHttpServletRequest(uri);
		AsyncContext asyncContext = new MockAsyncContext(request, new MockHttpServletResponse());
		return new ServletServerHttpRequest(request, asyncContext, "", new DefaultDataBufferFactory(), 1024);
	}


	private static class TestHttpServletRequest extends MockHttpServletRequest {

		TestHttpServletRequest(URI uri) {
			super("GET", uri.getRawPath());
			if (uri.getScheme() != null) {
				setScheme(uri.getScheme());
			}
			if (uri.getHost() != null) {
				setServerName(uri.getHost());
			}
			if (uri.getPort() != -1) {
				setServerPort(uri.getPort());
			}
			if (uri.getRawQuery() != null) {
				setQueryString(uri.getRawQuery());
			}
		}

		@Override
		public ServletInputStream getInputStream() {
			return new DelegatingServletInputStream(new ByteArrayInputStream(new byte[0])) {
				@Override
				public void setReadListener(ReadListener readListener) {
					// Ignore
				}
			};
		}
	}

}
