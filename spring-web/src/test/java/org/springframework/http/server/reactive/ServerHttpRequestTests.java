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

package org.springframework.http.server.reactive;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.test.DelegatingServletInputStream;
import org.springframework.mock.web.test.MockAsyncContext;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link AbstractServerHttpRequest}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class ServerHttpRequestTests {

	@Test
	public void queryParamsNone() throws Exception {
		MultiValueMap<String, String> params = createHttpRequest("/path").getQueryParams();
		assertThat(params.size()).isEqualTo(0);
	}

	@Test
	public void queryParams() throws Exception {
		MultiValueMap<String, String> params = createHttpRequest("/path?a=A&b=B").getQueryParams();
		assertThat(params.size()).isEqualTo(2);
		assertThat(params.get("a")).isEqualTo(Collections.singletonList("A"));
		assertThat(params.get("b")).isEqualTo(Collections.singletonList("B"));
	}

	@Test
	public void queryParamsWithMultipleValues() throws Exception {
		MultiValueMap<String, String> params = createHttpRequest("/path?a=1&a=2").getQueryParams();
		assertThat(params.size()).isEqualTo(1);
		assertThat(params.get("a")).isEqualTo(Arrays.asList("1", "2"));
	}

	@Test  // SPR-15140
	public void queryParamsWithEncodedValue() throws Exception {
		MultiValueMap<String, String> params = createHttpRequest("/path?a=%20%2B+%C3%A0").getQueryParams();
		assertThat(params.size()).isEqualTo(1);
		assertThat(params.get("a")).isEqualTo(Collections.singletonList(" + \u00e0"));
	}

	@Test
	public void queryParamsWithEmptyValue() throws Exception {
		MultiValueMap<String, String> params = createHttpRequest("/path?a=").getQueryParams();
		assertThat(params.size()).isEqualTo(1);
		assertThat(params.get("a")).isEqualTo(Collections.singletonList(""));
	}

	@Test
	public void queryParamsWithNoValue() throws Exception {
		MultiValueMap<String, String> params = createHttpRequest("/path?a").getQueryParams();
		assertThat(params.size()).isEqualTo(1);
		assertThat(params.get("a")).isEqualTo(Collections.singletonList(null));
	}

	@Test
	public void mutateRequest() throws Exception {
		SslInfo sslInfo = mock(SslInfo.class);
		ServerHttpRequest request = createHttpRequest("/").mutate().sslInfo(sslInfo).build();
		assertThat(request.getSslInfo()).isSameAs(sslInfo);

		request = createHttpRequest("/").mutate().method(HttpMethod.DELETE).build();
		assertThat(request.getMethod()).isEqualTo(HttpMethod.DELETE);

		String baseUri = "https://aaa.org:8080/a";

		request = createHttpRequest(baseUri).mutate().uri(URI.create("https://bbb.org:9090/b")).build();
		assertThat(request.getURI().toString()).isEqualTo("https://bbb.org:9090/b");

		request = createHttpRequest(baseUri).mutate().path("/b/c/d").build();
		assertThat(request.getURI().toString()).isEqualTo("https://aaa.org:8080/b/c/d");

		request = createHttpRequest(baseUri).mutate().path("/app/b/c/d").contextPath("/app").build();
		assertThat(request.getURI().toString()).isEqualTo("https://aaa.org:8080/app/b/c/d");
		assertThat(request.getPath().contextPath().value()).isEqualTo("/app");
	}

	@Test
	public void mutateWithInvalidPath() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				createHttpRequest("/").mutate().path("foo-bar"));
	}

	@Test  // SPR-16434
	public void mutatePathWithEncodedQueryParams() throws Exception {
		ServerHttpRequest request = createHttpRequest("/path?name=%E6%89%8E%E6%A0%B9");
		request = request.mutate().path("/mutatedPath").build();

		assertThat(request.getURI().getRawPath()).isEqualTo("/mutatedPath");
		assertThat(request.getURI().getRawQuery()).isEqualTo("name=%E6%89%8E%E6%A0%B9");
	}

	@Test
	public void mutateHeadersViaConsumer() throws Exception {
		String headerName = "key";
		String headerValue1 = "value1";
		String headerValue2 = "value2";

		ServerHttpRequest request = createHttpRequest("/path");
		assertThat(request.getHeaders().get(headerName)).isNull();

		request = request.mutate().headers(headers -> headers.add(headerName, headerValue1)).build();

		assertThat(request.getHeaders().get(headerName)).containsExactly(headerValue1);

		request = request.mutate().headers(headers -> headers.add(headerName, headerValue2)).build();

		assertThat(request.getHeaders().get(headerName)).containsExactly(headerValue1, headerValue2);
	}

	@Test
	public void mutateHeaderBySettingHeaderValues() throws Exception {
		String headerName = "key";
		String headerValue1 = "value1";
		String headerValue2 = "value2";
		String headerValue3 = "value3";

		ServerHttpRequest request = createHttpRequest("/path");
		assertThat(request.getHeaders().get(headerName)).isNull();

		request = request.mutate().header(headerName, headerValue1, headerValue2).build();

		assertThat(request.getHeaders().get(headerName)).containsExactly(headerValue1, headerValue2);

		request = request.mutate().header(headerName, headerValue3).build();

		assertThat(request.getHeaders().get(headerName)).containsExactly(headerValue3);
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
