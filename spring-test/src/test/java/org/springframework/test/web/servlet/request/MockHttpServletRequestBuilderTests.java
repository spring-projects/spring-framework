/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.test.web.servlet.request;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for building a {@link MockHttpServletRequest} with
 * {@link MockHttpServletRequestBuilder}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class MockHttpServletRequestBuilderTests {

	private final ServletContext servletContext = new MockServletContext();

	private MockHttpServletRequestBuilder builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/foo/bar");


	@Test
	void method() {
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getMethod()).isEqualTo("GET");
	}

	@Test
	void uri() {
		String uri = "https://java.sun.com:8080/javase/6/docs/api/java/util/BitSet.html?foo=bar#and(java.util.BitSet)";
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, uri);
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getScheme()).isEqualTo("https");
		assertThat(request.getQueryString()).isEqualTo("foo=bar");
		assertThat(request.getServerName()).isEqualTo("java.sun.com");
		assertThat(request.getServerPort()).isEqualTo(8080);
		assertThat(request.getRequestURI()).isEqualTo("/javase/6/docs/api/java/util/BitSet.html");
		assertThat(request.getRequestURL().toString())
				.isEqualTo("https://java.sun.com:8080/javase/6/docs/api/java/util/BitSet.html");
	}

	@Test
	void requestUriWithEncoding() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/foo bar");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getRequestURI()).isEqualTo("/foo%20bar");
	}

	@Test  // SPR-13435
	void requestUriWithDoubleSlashes() throws URISyntaxException {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, new URI("/test//currentlyValid/0"));
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getRequestURI()).isEqualTo("/test//currentlyValid/0");
	}

	@Test // gh-24556
	void requestUriWithoutScheme() {
		assertThatIllegalArgumentException().isThrownBy(() -> MockMvcRequestBuilders.get("localhost:8080/path"))
				.withMessage("'url' should start with a path or be a complete HTTP URL: localhost:8080/path");
	}

	@Test
	void contextPathEmpty() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/foo");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getContextPath()).isEqualTo("");
		assertThat(request.getServletPath()).isEqualTo("");
		assertThat(request.getPathInfo()).isEqualTo("/foo");
	}

	@Test
	void contextPathServletPathEmpty() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/travel/hotels/42");
		this.builder.contextPath("/travel");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getContextPath()).isEqualTo("/travel");
		assertThat(request.getServletPath()).isEqualTo("");
		assertThat(request.getPathInfo()).isEqualTo("/hotels/42");
	}

	@Test
	void contextPathServletPath() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/travel/main/hotels/42");
		this.builder.contextPath("/travel");
		this.builder.servletPath("/main");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getContextPath()).isEqualTo("/travel");
		assertThat(request.getServletPath()).isEqualTo("/main");
		assertThat(request.getPathInfo()).isEqualTo("/hotels/42");
	}

	@Test
	void contextPathServletPathInfoEmpty() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/travel/hotels/42");
		this.builder.contextPath("/travel");
		this.builder.servletPath("/hotels/42");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getContextPath()).isEqualTo("/travel");
		assertThat(request.getServletPath()).isEqualTo("/hotels/42");
		assertThat(request.getPathInfo()).isNull();
	}

	@Test
	void contextPathServletPathInfo() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/");
		this.builder.servletPath("/index.html");
		this.builder.pathInfo(null);
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getContextPath()).isEqualTo("");
		assertThat(request.getServletPath()).isEqualTo("/index.html");
		assertThat(request.getPathInfo()).isNull();
	}

	@Test // SPR-16453
	void pathInfoIsDecoded() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/travel/hotels 42");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getPathInfo()).isEqualTo("/travel/hotels 42");
	}

	@Test
	void contextPathServletPathInvalid() {
		testContextPathServletPathInvalid("/Foo", "", "Request URI [/foo/bar] does not start with context path [/Foo]");
		testContextPathServletPathInvalid("foo", "", "Context path must start with a '/'");
		testContextPathServletPathInvalid("/foo/", "", "Context path must not end with a '/'");

		testContextPathServletPathInvalid("/foo", "/Bar", "Invalid servlet path [/Bar] for request URI [/foo/bar]");
		testContextPathServletPathInvalid("/foo", "bar", "Servlet path must start with a '/'");
		testContextPathServletPathInvalid("/foo", "/bar/", "Servlet path must not end with a '/'");
	}

	private void testContextPathServletPathInvalid(String contextPath, String servletPath, String message) {
		try {
			this.builder.contextPath(contextPath);
			this.builder.servletPath(servletPath);
			this.builder.buildRequest(this.servletContext);
		}
		catch (IllegalArgumentException ex) {
			assertThat(ex.getMessage()).isEqualTo(message);
		}
	}

	@Test
	void requestUriAndFragment() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/foo#bar");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getRequestURI()).isEqualTo("/foo");
	}

	@Test
	void requestParameter() {
		this.builder.param("foo", "bar", "baz");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		Map<String, String[]> parameterMap = request.getParameterMap();

		assertThat(parameterMap.get("foo")).containsExactly("bar", "baz");
	}

	@Test
	void requestParameterFromQuery() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/?foo=bar&foo=baz");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		Map<String, String[]> parameterMap = request.getParameterMap();

		assertThat(parameterMap.get("foo")).containsExactly("bar", "baz");
		assertThat(request.getQueryString()).isEqualTo("foo=bar&foo=baz");
	}

	@Test
	void requestParameterFromQueryList() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/?foo[0]=bar&foo[1]=baz");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getQueryString()).isEqualTo("foo%5B0%5D=bar&foo%5B1%5D=baz");
		assertThat(request.getParameter("foo[0]")).isEqualTo("bar");
		assertThat(request.getParameter("foo[1]")).isEqualTo("baz");
	}

	@Test
	void queryParameter() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/");
		this.builder.queryParam("foo", "bar");
		this.builder.queryParam("foo", "baz");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getParameterMap().get("foo")).containsExactly("bar", "baz");
		assertThat(request.getQueryString()).isEqualTo("foo=bar&foo=baz");
	}

	@Test
	void queryParameterMap() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/");
		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
		List<String> values = new ArrayList<>();
		values.add("bar");
		values.add("baz");
		queryParams.put("foo", values);
		this.builder.queryParams(queryParams);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getParameterMap().get("foo")).containsExactly("bar", "baz");
		assertThat(request.getQueryString()).isEqualTo("foo=bar&foo=baz");
	}

	@Test
	void queryParameterList() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/");
		this.builder.queryParam("foo[0]", "bar");
		this.builder.queryParam("foo[1]", "baz");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getQueryString()).isEqualTo("foo%5B0%5D=bar&foo%5B1%5D=baz");
		assertThat(request.getParameter("foo[0]")).isEqualTo("bar");
		assertThat(request.getParameter("foo[1]")).isEqualTo("baz");
	}

	@Test
	void requestParameterFromQueryWithEncoding() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/?foo={value}", "bar=baz");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getQueryString()).isEqualTo("foo=bar%3Dbaz");
		assertThat(request.getParameter("foo")).isEqualTo("bar=baz");
	}

	@Test  // SPR-11043
	void requestParameterFromQueryNull() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/?foo");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		Map<String, String[]> parameterMap = request.getParameterMap();

		assertThat(parameterMap.get("foo")).containsExactly((String) null);
		assertThat(request.getQueryString()).isEqualTo("foo");
	}

	@Test  // SPR-13801
	void requestParameterFromMultiValueMap() throws Exception {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("foo", "bar");
		params.add("foo", "baz");
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.POST, "/foo");
		this.builder.params(params);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getParameterMap().get("foo")).containsExactly("bar", "baz");
	}

	@Test
	void requestParameterFromRequestBodyFormData() throws Exception {
		String contentType = "application/x-www-form-urlencoded;charset=UTF-8";
		String body = "name+1=value+1&name+2=value+A&name+2=value+B&name+3";

		MockHttpServletRequest request = new MockHttpServletRequestBuilder(HttpMethod.POST, "/foo")
				.contentType(contentType).content(body.getBytes(StandardCharsets.UTF_8))
				.buildRequest(this.servletContext);

		assertThat(request.getParameterMap().get("name 1")).containsExactly("value 1");
		assertThat(request.getParameterMap().get("name 2")).containsExactly("value A", "value B");
		assertThat(request.getParameterMap().get("name 3")).containsExactly((String) null);
	}

	@Test
	void acceptHeader() {
		this.builder.accept(MediaType.TEXT_HTML, MediaType.APPLICATION_XML);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		List<String> accept = Collections.list(request.getHeaders("Accept"));
		List<MediaType> result = MediaType.parseMediaTypes(accept.get(0));

		assertThat(accept.size()).isEqualTo(1);
		assertThat(result.get(0).toString()).isEqualTo("text/html");
		assertThat(result.get(1).toString()).isEqualTo("application/xml");
	}

	@Test // gh-2079
	void acceptHeaderWithInvalidValues() {
		this.builder.accept("any", "any2");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		assertThat(request.getHeader("Accept")).isEqualTo("any, any2");
	}

	@Test
	void contentType() {
		this.builder.contentType(MediaType.TEXT_HTML);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		String contentType = request.getContentType();
		List<String> contentTypes = Collections.list(request.getHeaders("Content-Type"));

		assertThat(contentType).isEqualTo("text/html");
		assertThat(contentTypes.size()).isEqualTo(1);
		assertThat(contentTypes.get(0)).isEqualTo("text/html");
	}

	@Test
	void contentTypeViaString() {
		this.builder.contentType("text/html");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		String contentType = request.getContentType();
		List<String> contentTypes = Collections.list(request.getHeaders("Content-Type"));

		assertThat(contentType).isEqualTo("text/html");
		assertThat(contentTypes.size()).isEqualTo(1);
		assertThat(contentTypes.get(0)).isEqualTo("text/html");
	}

	@Test // gh-2079
	void contentTypeWithInvalidValue() {
		this.builder.contentType("any");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		assertThat(request.getContentType()).isEqualTo("any");
	}

	@Test  // SPR-11308
	void contentTypeViaHeader() {
		this.builder.header("Content-Type", MediaType.TEXT_HTML_VALUE);
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		String contentType = request.getContentType();

		assertThat(contentType).isEqualTo("text/html");
	}

	@Test // gh-2079
	void contentTypeViaHeaderWithInvalidValue() {
		this.builder.header("Content-Type", "yaml");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		assertThat(request.getContentType()).isEqualTo("yaml");
	}

	@Test  // SPR-11308
	void contentTypeViaMultipleHeaderValues() {
		this.builder.header("Content-Type", MediaType.TEXT_HTML_VALUE, MediaType.ALL_VALUE);
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getContentType()).isEqualTo("text/html");
	}

	@Test
	void body() throws IOException {
		byte[] body = "Hello World".getBytes("UTF-8");
		this.builder.content(body);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		byte[] result = FileCopyUtils.copyToByteArray(request.getInputStream());

		assertThat(result).isEqualTo(body);
		assertThat(request.getContentLength()).isEqualTo(body.length);
	}

	@Test
	void header() {
		this.builder.header("foo", "bar", "baz");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		List<String> headers = Collections.list(request.getHeaders("foo"));

		assertThat(headers.size()).isEqualTo(2);
		assertThat(headers.get(0)).isEqualTo("bar");
		assertThat(headers.get(1)).isEqualTo("baz");
	}

	@Test
	void headers() {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		httpHeaders.put("foo", Arrays.asList("bar", "baz"));
		this.builder.headers(httpHeaders);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		List<String> headers = Collections.list(request.getHeaders("foo"));

		assertThat(headers.size()).isEqualTo(2);
		assertThat(headers.get(0)).isEqualTo("bar");
		assertThat(headers.get(1)).isEqualTo("baz");
		assertThat(request.getHeader("Content-Type")).isEqualTo(MediaType.APPLICATION_JSON.toString());
	}

	@Test
	void cookie() {
		Cookie cookie1 = new Cookie("foo", "bar");
		Cookie cookie2 = new Cookie("baz", "qux");
		this.builder.cookie(cookie1, cookie2);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		Cookie[] cookies = request.getCookies();

		assertThat(cookies.length).isEqualTo(2);
		assertThat(cookies[0].getName()).isEqualTo("foo");
		assertThat(cookies[0].getValue()).isEqualTo("bar");
		assertThat(cookies[1].getName()).isEqualTo("baz");
		assertThat(cookies[1].getValue()).isEqualTo("qux");
	}

	@Test
	void noCookies() {
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		assertThat(request.getCookies()).isNull();
	}

	@Test
	void locale() {
		Locale locale = new Locale("nl", "nl");
		this.builder.locale(locale);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getLocale()).isEqualTo(locale);
	}

	@Test
	void characterEncoding() {
		String encoding = "UTF-8";

		this.builder.characterEncoding(encoding);
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		assertThat(request.getCharacterEncoding()).isEqualTo(encoding);

		this.builder.characterEncoding(StandardCharsets.ISO_8859_1);
		request = this.builder.buildRequest(this.servletContext);
		assertThat(request.getCharacterEncoding()).isEqualTo(StandardCharsets.ISO_8859_1.name());
	}

	@Test
	void requestAttribute() {
		this.builder.requestAttr("foo", "bar");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getAttribute("foo")).isEqualTo("bar");
	}

	@Test
	void sessionAttribute() {
		this.builder.sessionAttr("foo", "bar");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getSession().getAttribute("foo")).isEqualTo("bar");
	}

	@Test
	void sessionAttributes() {
		Map<String, Object> map = new HashMap<>();
		map.put("foo", "bar");
		this.builder.sessionAttrs(map);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getSession().getAttribute("foo")).isEqualTo("bar");
	}

	@Test
	void session() {
		MockHttpSession session = new MockHttpSession(this.servletContext);
		session.setAttribute("foo", "bar");
		this.builder.session(session);
		this.builder.sessionAttr("baz", "qux");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getSession()).isEqualTo(session);
		assertThat(request.getSession().getAttribute("foo")).isEqualTo("bar");
		assertThat(request.getSession().getAttribute("baz")).isEqualTo("qux");
	}

	@Test
	void flashAttribute() {
		this.builder.flashAttr("foo", "bar");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		FlashMap flashMap = new SessionFlashMapManager().retrieveAndUpdate(request, null);
		assertThat((Object) flashMap).isNotNull();
		assertThat(flashMap.get("foo")).isEqualTo("bar");
	}

	@Test
	void principal() {
		User user = new User();
		this.builder.principal(user);
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getUserPrincipal()).isEqualTo(user);
	}

	@Test  // SPR-12945
	void mergeInvokesDefaultRequestPostProcessorFirst() {
		final String ATTR = "ATTR";
		final String EXPECTED = "override";

		MockHttpServletRequestBuilder defaultBuilder =
				new MockHttpServletRequestBuilder(HttpMethod.GET, "/foo/bar")
						.with(requestAttr(ATTR).value("default"))
						.with(requestAttr(ATTR).value(EXPECTED));

		builder.merge(defaultBuilder);

		MockHttpServletRequest request = builder.buildRequest(servletContext);
		request = builder.postProcessRequest(request);

		assertThat(request.getAttribute(ATTR)).isEqualTo(EXPECTED);
	}

	@Test  // SPR-13719
	void arbitraryMethod() {
		String httpMethod = "REPort";
		URI url = UriComponentsBuilder.fromPath("/foo/{bar}").buildAndExpand(42).toUri();
		this.builder = new MockHttpServletRequestBuilder(httpMethod, url);
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertThat(request.getMethod()).isEqualTo(httpMethod);
		assertThat(request.getPathInfo()).isEqualTo("/foo/42");
	}


	private static RequestAttributePostProcessor requestAttr(String attrName) {
		return new RequestAttributePostProcessor().attr(attrName);
	}


	private final class User implements Principal {

		@Override
		public String getName() {
			return "Foo";
		}
	}


	private static class RequestAttributePostProcessor implements RequestPostProcessor {

		String attr;

		String value;

		public RequestAttributePostProcessor attr(String attr) {
			this.attr = attr;
			return this;
		}

		public RequestAttributePostProcessor value(String value) {
			this.value = value;
			return this;
		}

		@Override
		public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
			request.setAttribute(attr, value);
			return request;
		}
	}

}
