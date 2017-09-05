/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.test.web.servlet.request;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;

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

import static org.junit.Assert.*;

/**
 * Unit tests for building a {@link MockHttpServletRequest} with
 * {@link MockHttpServletRequestBuilder}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class MockHttpServletRequestBuilderTests {

	private final ServletContext servletContext = new MockServletContext();

	private MockHttpServletRequestBuilder builder;


	@Before
	public void setUp() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/foo/bar");
	}


	@Test
	public void method() {
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("GET", request.getMethod());
	}

	@Test
	public void uri() {
		String uri = "https://java.sun.com:8080/javase/6/docs/api/java/util/BitSet.html?foo=bar#and(java.util.BitSet)";
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, uri);
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("https", request.getScheme());
		assertEquals("foo=bar", request.getQueryString());
		assertEquals("java.sun.com", request.getServerName());
		assertEquals(8080, request.getServerPort());
		assertEquals("/javase/6/docs/api/java/util/BitSet.html", request.getRequestURI());
		assertEquals("https://java.sun.com:8080/javase/6/docs/api/java/util/BitSet.html",
				request.getRequestURL().toString());
	}

	@Test
	public void requestUriWithEncoding() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/foo bar");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("/foo%20bar", request.getRequestURI());
	}

	@Test  // SPR-13435
	public void requestUriWithDoubleSlashes() throws URISyntaxException {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, new URI("/test//currentlyValid/0"));
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("/test//currentlyValid/0", request.getRequestURI());
	}

	@Test
	public void contextPathEmpty() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/foo");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("", request.getContextPath());
		assertEquals("", request.getServletPath());
		assertEquals("/foo", request.getPathInfo());
	}

	@Test
	public void contextPathServletPathEmpty() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/travel/hotels/42");
		this.builder.contextPath("/travel");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("/travel", request.getContextPath());
		assertEquals("", request.getServletPath());
		assertEquals("/hotels/42", request.getPathInfo());
	}

	@Test
	public void contextPathServletPath() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/travel/main/hotels/42");
		this.builder.contextPath("/travel");
		this.builder.servletPath("/main");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("/travel", request.getContextPath());
		assertEquals("/main", request.getServletPath());
		assertEquals("/hotels/42", request.getPathInfo());
	}

	@Test
	public void contextPathServletPathInfoEmpty() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/travel/hotels/42");
		this.builder.contextPath("/travel");
		this.builder.servletPath("/hotels/42");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("/travel", request.getContextPath());
		assertEquals("/hotels/42", request.getServletPath());
		assertNull(request.getPathInfo());
	}

	@Test
	public void contextPathServletPathInfo() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/");
		this.builder.servletPath("/index.html");
		this.builder.pathInfo(null);
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("", request.getContextPath());
		assertEquals("/index.html", request.getServletPath());
		assertNull(request.getPathInfo());
	}

	@Test
	public void contextPathServletPathInvalid() {
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
			assertEquals(message, ex.getMessage());
		}
	}

	@Test
	public void requestUriAndFragment() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/foo#bar");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("/foo", request.getRequestURI());
	}

	@Test
	public void requestParameter() {
		this.builder.param("foo", "bar", "baz");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		Map<String, String[]> parameterMap = request.getParameterMap();

		assertArrayEquals(new String[] {"bar", "baz"}, parameterMap.get("foo"));
	}

	@Test
	public void requestParameterFromQuery() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/?foo=bar&foo=baz");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		Map<String, String[]> parameterMap = request.getParameterMap();

		assertArrayEquals(new String[] {"bar", "baz"}, parameterMap.get("foo"));
		assertEquals("foo=bar&foo=baz", request.getQueryString());
	}

	@Test
	public void requestParameterFromQueryList() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/?foo[0]=bar&foo[1]=baz");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("foo%5B0%5D=bar&foo%5B1%5D=baz", request.getQueryString());
		assertEquals("bar", request.getParameter("foo[0]"));
		assertEquals("baz", request.getParameter("foo[1]"));
	}

	@Test
	public void requestParameterFromQueryWithEncoding() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/?foo={value}", "bar=baz");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("foo=bar%3Dbaz", request.getQueryString());
		assertEquals("bar=baz", request.getParameter("foo"));
	}

	@Test  // SPR-11043
	public void requestParameterFromQueryNull() {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/?foo");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		Map<String, String[]> parameterMap = request.getParameterMap();

		assertArrayEquals(new String[] {null}, parameterMap.get("foo"));
		assertEquals("foo", request.getQueryString());
	}

	@Test  // SPR-13801
	public void requestParameterFromMultiValueMap() throws Exception {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("foo", "bar");
		params.add("foo", "baz");
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.POST, "/foo");
		this.builder.params(params);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertArrayEquals(new String[] {"bar", "baz"}, request.getParameterMap().get("foo"));
	}

	@Test
	public void requestParameterFromRequestBodyFormData() throws Exception {
		String contentType = "application/x-www-form-urlencoded;charset=UTF-8";
		String body = "name+1=value+1&name+2=value+A&name+2=value+B&name+3";

		MockHttpServletRequest request = new MockHttpServletRequestBuilder(HttpMethod.POST, "/foo")
				.contentType(contentType).content(body.getBytes(StandardCharsets.UTF_8))
				.buildRequest(this.servletContext);

		assertArrayEquals(new String[] {"value 1"}, request.getParameterMap().get("name 1"));
		assertArrayEquals(new String[] {"value A", "value B"}, request.getParameterMap().get("name 2"));
		assertArrayEquals(new String[] {null}, request.getParameterMap().get("name 3"));
	}

	@Test
	public void acceptHeader() {
		this.builder.accept(MediaType.TEXT_HTML, MediaType.APPLICATION_XML);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		List<String> accept = Collections.list(request.getHeaders("Accept"));
		List<MediaType> result = MediaType.parseMediaTypes(accept.get(0));

		assertEquals(1, accept.size());
		assertEquals("text/html", result.get(0).toString());
		assertEquals("application/xml", result.get(1).toString());
	}

	@Test
	public void contentType() {
		this.builder.contentType(MediaType.TEXT_HTML);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		String contentType = request.getContentType();
		List<String> contentTypes = Collections.list(request.getHeaders("Content-Type"));

		assertEquals("text/html", contentType);
		assertEquals(1, contentTypes.size());
		assertEquals("text/html", contentTypes.get(0));
	}

	@Test
	public void contentTypeViaString() {
		this.builder.contentType("text/html");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		String contentType = request.getContentType();
		List<String> contentTypes = Collections.list(request.getHeaders("Content-Type"));

		assertEquals("text/html", contentType);
		assertEquals(1, contentTypes.size());
		assertEquals("text/html", contentTypes.get(0));
	}

	@Test  // SPR-11308
	public void contentTypeViaHeader() {
		this.builder.header("Content-Type", MediaType.TEXT_HTML_VALUE);
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		String contentType = request.getContentType();

		assertEquals("text/html", contentType);
	}

	@Test  // SPR-11308
	public void contentTypeViaMultipleHeaderValues() {
		this.builder.header("Content-Type", MediaType.TEXT_HTML_VALUE, MediaType.ALL_VALUE);
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("text/html", request.getContentType());
	}

	@Test
	public void body() throws IOException {
		byte[] body = "Hello World".getBytes("UTF-8");
		this.builder.content(body);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		byte[] result = FileCopyUtils.copyToByteArray(request.getInputStream());

		assertArrayEquals(body, result);
	}

	@Test
	public void header() {
		this.builder.header("foo", "bar", "baz");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		List<String> headers = Collections.list(request.getHeaders("foo"));

		assertEquals(2, headers.size());
		assertEquals("bar", headers.get(0));
		assertEquals("baz", headers.get(1));
	}

	@Test
	public void headers() {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		httpHeaders.put("foo", Arrays.asList("bar", "baz"));
		this.builder.headers(httpHeaders);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		List<String> headers = Collections.list(request.getHeaders("foo"));

		assertEquals(2, headers.size());
		assertEquals("bar", headers.get(0));
		assertEquals("baz", headers.get(1));
		assertEquals(MediaType.APPLICATION_JSON.toString(), request.getHeader("Content-Type"));
	}

	@Test
	public void cookie() {
		Cookie cookie1 = new Cookie("foo", "bar");
		Cookie cookie2 = new Cookie("baz", "qux");
		this.builder.cookie(cookie1, cookie2);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		Cookie[] cookies = request.getCookies();

		assertEquals(2, cookies.length);
		assertEquals("foo", cookies[0].getName());
		assertEquals("bar", cookies[0].getValue());
		assertEquals("baz", cookies[1].getName());
		assertEquals("qux", cookies[1].getValue());
	}

	@Test
	public void noCookies() {
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		assertNull(request.getCookies());
	}

	@Test
	public void locale() {
		Locale locale = new Locale("nl", "nl");
		this.builder.locale(locale);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals(locale, request.getLocale());
	}

	@Test
	public void characterEncoding() {
		String encoding = "UTF-8";
		this.builder.characterEncoding(encoding);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals(encoding, request.getCharacterEncoding());
	}

	@Test
	public void requestAttribute() {
		this.builder.requestAttr("foo", "bar");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("bar", request.getAttribute("foo"));
	}

	@Test
	public void sessionAttribute() {
		this.builder.sessionAttr("foo", "bar");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("bar", request.getSession().getAttribute("foo"));
	}

	@Test
	public void sessionAttributes() {
		Map<String, Object> map = new HashMap<>();
		map.put("foo", "bar");
		this.builder.sessionAttrs(map);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("bar", request.getSession().getAttribute("foo"));
	}

	@Test
	public void session() {
		MockHttpSession session = new MockHttpSession(this.servletContext);
		session.setAttribute("foo", "bar");
		this.builder.session(session);
		this.builder.sessionAttr("baz", "qux");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals(session, request.getSession());
		assertEquals("bar", request.getSession().getAttribute("foo"));
		assertEquals("qux", request.getSession().getAttribute("baz"));
	}

	@Test
	public void flashAttribute() {
		this.builder.flashAttr("foo", "bar");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		FlashMap flashMap = new SessionFlashMapManager().retrieveAndUpdate(request, null);
		assertNotNull(flashMap);
		assertEquals("bar", flashMap.get("foo"));
	}

	@Test
	public void principal() {
		User user = new User();
		this.builder.principal(user);
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals(user, request.getUserPrincipal());
	}

	@Test  // SPR-12945
	public void mergeInvokesDefaultRequestPostProcessorFirst() {
		final String ATTR = "ATTR";
		final String EXPECTED = "override";

		MockHttpServletRequestBuilder defaultBuilder =
				new MockHttpServletRequestBuilder(HttpMethod.GET, "/foo/bar")
						.with(requestAttr(ATTR).value("default"))
						.with(requestAttr(ATTR).value(EXPECTED));

		builder.merge(defaultBuilder);

		MockHttpServletRequest request = builder.buildRequest(servletContext);
		request = builder.postProcessRequest(request);

		assertEquals(EXPECTED, request.getAttribute(ATTR));
	}

	@Test  // SPR-13719
	public void arbitraryMethod() {
		String httpMethod = "REPort";
		URI url = UriComponentsBuilder.fromPath("/foo/{bar}").buildAndExpand(42).toUri();
		this.builder = new MockHttpServletRequestBuilder(httpMethod, url);
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals(httpMethod, request.getMethod());
		assertEquals("/foo/42", request.getPathInfo());
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

		public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
			request.setAttribute(attr, value);
			return request;
		}
	}

}
