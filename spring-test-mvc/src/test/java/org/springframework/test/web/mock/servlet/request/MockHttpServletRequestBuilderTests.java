/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.test.web.mock.servlet.request;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

/**
 * Tests building a MockHttpServletRequest with {@link MockHttpServletRequestBuilder}.
 *
 * @author Rossen Stoyanchev
 */
public class MockHttpServletRequestBuilderTests {

	private MockHttpServletRequestBuilder builder;

	private ServletContext servletContext;


	@Before
	public void setUp() throws Exception {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/foo/bar");
		servletContext = new MockServletContext();
	}

	@Test
	public void method() {
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("GET", request.getMethod());
	}

	@Test
	public void uri() throws Exception {
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
	public void requestUriWithEncoding() throws Exception {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/foo bar");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("/foo%20bar", request.getRequestURI());
	}

	@Test
	public void contextPathEmpty() throws Exception {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/foo");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("", request.getContextPath());
		assertEquals("", request.getServletPath());
		assertEquals("/foo", request.getPathInfo());
	}

	@Test
	public void contextPathServletPathEmpty() throws Exception {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/travel/hotels/42");
		this.builder.contextPath("/travel");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("/travel", request.getContextPath());
		assertEquals("", request.getServletPath());
		assertEquals("/hotels/42", request.getPathInfo());
	}

	@Test
	public void contextPathServletPath() throws Exception {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/travel/main/hotels/42");
		this.builder.contextPath("/travel");
		this.builder.servletPath("/main");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("/travel", request.getContextPath());
		assertEquals("/main", request.getServletPath());
		assertEquals("/hotels/42", request.getPathInfo());
	}

	@Test
	public void contextPathServletPathInfoEmpty() throws Exception {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/travel/hotels/42");

		this.builder.contextPath("/travel");
		this.builder.servletPath("/hotels/42");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("/travel", request.getContextPath());
		assertEquals("/hotels/42", request.getServletPath());
		assertNull(request.getPathInfo());
	}

	@Test
	public void contextPathServletPathInfo() throws Exception {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/");
		this.builder.servletPath("/index.html");
		this.builder.pathInfo(null);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("", request.getContextPath());
		assertEquals("/index.html", request.getServletPath());
		assertNull(request.getPathInfo());
	}

	@Test
	public void contextPathServletPathInvalid() throws Exception {

		testContextPathServletPathInvalid("/Foo", "", "requestURI [/foo/bar] does not start with contextPath [/Foo]");
		testContextPathServletPathInvalid("foo", "", "Context path must start with a '/'");
		testContextPathServletPathInvalid("/foo/", "", "Context path must not end with a '/'");

		testContextPathServletPathInvalid("/foo", "/Bar", "Invalid servletPath [/Bar] for requestURI [/foo/bar]");
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
	public void requestUriAndFragment() throws Exception {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/foo#bar");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("/foo", request.getRequestURI());
	}

	@Test
	public void requestParameter() {
		this.builder.param("foo", "bar", "baz");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		Map<String, String[]> parameterMap = request.getParameterMap();

		assertArrayEquals(new String[]{"bar", "baz"}, parameterMap.get("foo"));
	}

	@Test
	public void requestParameterFromQuery() throws Exception {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/?foo=bar&foo=baz");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		Map<String, String[]> parameterMap = request.getParameterMap();

		assertArrayEquals(new String[]{"bar", "baz"}, parameterMap.get("foo"));
		assertEquals("foo=bar&foo=baz", request.getQueryString());
	}

	@Test
	public void requestParameterFromQueryList() throws Exception {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/?foo[0]=bar&foo[1]=baz");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("foo[0]=bar&foo[1]=baz", request.getQueryString());
		assertEquals("bar", request.getParameter("foo[0]"));
		assertEquals("baz", request.getParameter("foo[1]"));
	}

	@Test
	public void requestParameterFromQueryWithEncoding() throws Exception {
		this.builder = new MockHttpServletRequestBuilder(HttpMethod.GET, "/?foo={value}", "bar=baz");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("foo=bar=baz", request.getQueryString());
		assertEquals("bar=baz", request.getParameter("foo"));
	}

    @Test
    public void acceptHeader() throws Exception {
        this.builder.accept(MediaType.TEXT_HTML, MediaType.APPLICATION_XML);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		List<String> accept = Collections.list(request.getHeaders("Accept"));
		List<MediaType> result = MediaType.parseMediaTypes(accept.get(0));

		assertEquals(1, accept.size());
		assertEquals("text/html", result.get(0).toString());
		assertEquals("application/xml", result.get(1).toString());
	}

	@Test
	public void contentType() throws Exception {
		this.builder.contentType(MediaType.TEXT_HTML);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		String contentType = request.getContentType();
		List<String> contentTypes = Collections.list(request.getHeaders("Content-Type"));

		assertEquals("text/html", contentType);
		assertEquals(1, contentTypes.size());
		assertEquals("text/html", contentTypes.get(0));
	}

	@Test
	public void body() throws Exception {
		byte[] body = "Hello World".getBytes("UTF-8");
		this.builder.content(body);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		byte[] result = FileCopyUtils.copyToByteArray(request.getInputStream());

		assertArrayEquals(body, result);
	}

	@Test
	public void header() throws Exception {
		this.builder.header("foo", "bar", "baz");

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);
		List<String> headers = Collections.list(request.getHeaders("foo"));

		assertEquals(2, headers.size());
		assertEquals("bar", headers.get(0));
		assertEquals("baz", headers.get(1));
	}

	@Test
	public void headers() throws Exception {
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
	public void cookie() throws Exception {
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
	public void locale() throws Exception {
		Locale locale = new Locale("nl", "nl");
		this.builder.locale(locale);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals(locale, request.getLocale());
	}

	@Test
	public void characterEncoding() throws Exception {
		String encoding = "UTF-8";
		this.builder.characterEncoding(encoding);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals(encoding, request.getCharacterEncoding());
	}

	@Test
	public void requestAttribute() throws Exception {
		this.builder.requestAttr("foo", "bar");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("bar", request.getAttribute("foo"));
	}

	@Test
	public void sessionAttribute() throws Exception {
		this.builder.sessionAttr("foo", "bar");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("bar", request.getSession().getAttribute("foo"));
	}

	@Test
	public void sessionAttributes() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("foo", "bar");
		this.builder.sessionAttrs(map);

		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals("bar", request.getSession().getAttribute("foo"));
	}

	@Test
	public void session() throws Exception {
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
	public void flashAttribute() throws Exception {
		this.builder.flashAttr("foo", "bar");
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		FlashMap flashMap = new SessionFlashMapManager().retrieveAndUpdate(request, null);
		assertNotNull(flashMap);
		assertEquals("bar", flashMap.get("foo"));
	}

	@Test
	public void principal() throws Exception {
		User user = new User();
		this.builder.principal(user);
		MockHttpServletRequest request = this.builder.buildRequest(this.servletContext);

		assertEquals(user, request.getUserPrincipal());
	}


	private final class User implements Principal {

		public String getName() {
			return "Foo";
		}
	}

}
