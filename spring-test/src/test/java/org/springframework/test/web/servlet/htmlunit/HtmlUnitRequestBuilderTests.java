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

package org.springframework.test.web.servlet.htmlunit;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.http.auth.UsernamePasswordCredentials;

import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Unit tests for {@link HtmlUnitRequestBuilder}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 */
public class HtmlUnitRequestBuilderTests {

	private final WebClient webClient = new WebClient();

	private final ServletContext servletContext = new MockServletContext();

	private final Map<String, MockHttpSession> sessions = new HashMap<>();

	private WebRequest webRequest;

	private HtmlUnitRequestBuilder requestBuilder;


	@Before
	public void setUp() throws Exception {
		webRequest = new WebRequest(new URL("http://example.com:80/test/this/here"));
		webRequest.setHttpMethod(HttpMethod.GET);
		requestBuilder = new HtmlUnitRequestBuilder(sessions, webClient, webRequest);
	}

	// --- constructor

	@Test(expected = IllegalArgumentException.class)
	public void constructorNullSessions() {
		new HtmlUnitRequestBuilder(null, webClient, webRequest);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorNullWebClient() {
		new HtmlUnitRequestBuilder(sessions, null, webRequest);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorNullWebRequest() {
		new HtmlUnitRequestBuilder(sessions, webClient, null);
	}

	// --- buildRequest

	@Test
	@SuppressWarnings("deprecation")
	public void buildRequestBasicAuth() {
		String base64Credentials = "dXNlcm5hbWU6cGFzc3dvcmQ=";
		String authzHeaderValue = "Basic: " + base64Credentials;
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(base64Credentials);
		webRequest.setCredentials(credentials);
		webRequest.setAdditionalHeader("Authorization", authzHeaderValue);

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getAuthType(), equalTo("Basic"));
		assertThat(actualRequest.getHeader("Authorization"), equalTo(authzHeaderValue));
	}

	@Test
	public void buildRequestCharacterEncoding() {
		String charset = "UTF-8";
		webRequest.setCharset(charset);

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getCharacterEncoding(), equalTo(charset));
	}

	@Test
	public void buildRequestDefaultCharacterEncoding() {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getCharacterEncoding(), equalTo("ISO-8859-1"));
	}

	@Test
	public void buildRequestContentLength() {
		String content = "some content that has length";
		webRequest.setHttpMethod(HttpMethod.POST);
		webRequest.setRequestBody(content);

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getContentLength(), equalTo(content.length()));
	}

	@Test
	public void buildRequestContentType() {
		String contentType = "text/html;charset=UTF-8";
		webRequest.setAdditionalHeader("Content-Type", contentType);

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getContentType(), equalTo(contentType));
		assertThat(actualRequest.getHeader("Content-Type"), equalTo(contentType));
	}

	@Test
	public void buildRequestContextPathUsesFirstSegmentByDefault() {
		String contextPath = requestBuilder.buildRequest(servletContext).getContextPath();

		assertThat(contextPath, equalTo("/test"));
	}

	@Test
	public void buildRequestContextPathUsesNoFirstSegmentWithDefault() throws MalformedURLException {
		webRequest.setUrl(new URL("http://example.com/"));
		String contextPath = requestBuilder.buildRequest(servletContext).getContextPath();

		assertThat(contextPath, equalTo(""));
	}

	@Test(expected = IllegalArgumentException.class)
	public void buildRequestContextPathInvalid() {
		requestBuilder.setContextPath("/invalid");

		requestBuilder.buildRequest(servletContext).getContextPath();
	}

	@Test
	public void buildRequestContextPathEmpty() {
		String expected = "";
		requestBuilder.setContextPath(expected);

		String contextPath = requestBuilder.buildRequest(servletContext).getContextPath();

		assertThat(contextPath, equalTo(expected));
	}

	@Test
	public void buildRequestContextPathExplicit() {
		String expected = "/test";
		requestBuilder.setContextPath(expected);

		String contextPath = requestBuilder.buildRequest(servletContext).getContextPath();

		assertThat(contextPath, equalTo(expected));
	}

	@Test
	public void buildRequestContextPathMulti() {
		String expected = "/test/this";
		requestBuilder.setContextPath(expected);

		String contextPath = requestBuilder.buildRequest(servletContext).getContextPath();

		assertThat(contextPath, equalTo(expected));
	}

	@Test
	public void buildRequestCookiesNull() {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getCookies(), nullValue());
	}

	@Test
	public void buildRequestCookiesSingle() {
		webRequest.setAdditionalHeader("Cookie", "name=value");

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		Cookie[] cookies = actualRequest.getCookies();
		assertThat(cookies.length, equalTo(1));
		assertThat(cookies[0].getName(), equalTo("name"));
		assertThat(cookies[0].getValue(), equalTo("value"));
	}

	@Test
	public void buildRequestCookiesMulti() {
		webRequest.setAdditionalHeader("Cookie", "name=value; name2=value2");

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		Cookie[] cookies = actualRequest.getCookies();
		assertThat(cookies.length, equalTo(2));
		Cookie cookie = cookies[0];
		assertThat(cookie.getName(), equalTo("name"));
		assertThat(cookie.getValue(), equalTo("value"));
		cookie = cookies[1];
		assertThat(cookie.getName(), equalTo("name2"));
		assertThat(cookie.getValue(), equalTo("value2"));
	}

	@Test
	public void buildRequestInputStream() throws Exception {
		String content = "some content that has length";
		webRequest.setHttpMethod(HttpMethod.POST);
		webRequest.setRequestBody(content);

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(IOUtils.toString(actualRequest.getInputStream()), equalTo(content));
	}

	@Test
	public void buildRequestLocalAddr() {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getLocalAddr(), equalTo("127.0.0.1"));
	}

	@Test
	public void buildRequestLocaleDefault() {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getLocale(), equalTo(Locale.getDefault()));
	}

	@Test
	public void buildRequestLocaleDa() {
		webRequest.setAdditionalHeader("Accept-Language", "da");

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getLocale(), equalTo(new Locale("da")));
	}

	@Test
	public void buildRequestLocaleEnGbQ08() {
		webRequest.setAdditionalHeader("Accept-Language", "en-gb;q=0.8");

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getLocale(), equalTo(new Locale("en", "gb", "0.8")));
	}

	@Test
	public void buildRequestLocaleEnQ07() {
		webRequest.setAdditionalHeader("Accept-Language", "en;q=0.7");

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getLocale(), equalTo(new Locale("en", "", "0.7")));
	}

	@Test
	public void buildRequestLocaleEnUs() {
		webRequest.setAdditionalHeader("Accept-Language", "en-US");

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getLocale(), equalTo(Locale.US));
	}

	@Test
	public void buildRequestLocaleFr() {
		webRequest.setAdditionalHeader("Accept-Language", "fr");

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getLocale(), equalTo(Locale.FRENCH));
	}

	@Test
	public void buildRequestLocaleMulti() {
		webRequest.setAdditionalHeader("Accept-Language", "da, en-gb;q=0.8, en;q=0.7");

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		// Append Locale.ENGLISH since MockHttpServletRequest automatically sets it as the
		// preferred locale.
		List<Locale> expected = asList(new Locale("da"), new Locale("en", "gb", "0.8"), new Locale("en", "", "0.7"), Locale.ENGLISH);
		assertThat(Collections.list(actualRequest.getLocales()), equalTo(expected));
	}

	@Test
	public void buildRequestLocalName() {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getLocalName(), equalTo("localhost"));
	}

	@Test
	public void buildRequestLocalPort() {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getLocalPort(), equalTo(80));
	}

	@Test
	public void buildRequestLocalMissing() throws Exception {
		webRequest.setUrl(new URL("http://localhost/test/this"));
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getLocalPort(), equalTo(-1));
	}

	@Test
	public void buildRequestMethods() {
		for (HttpMethod expectedMethod : HttpMethod.values()) {
			webRequest.setHttpMethod(expectedMethod);
			String actualMethod = requestBuilder.buildRequest(servletContext).getMethod();
			assertThat(actualMethod, equalTo(expectedMethod.name()));
		}
	}

	@Test
	public void buildRequestParameterMapViaWebRequestDotSetRequestParametersWithSingleRequestParam() {
		webRequest.setRequestParameters(asList(new NameValuePair("name", "value")));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParameterMap().size(), equalTo(1));
		assertThat(actualRequest.getParameter("name"), equalTo("value"));
	}

	@Test
	public void buildRequestParameterMapViaWebRequestDotSetRequestParametersWithSingleRequestParamWithNullValue() {
		webRequest.setRequestParameters(asList(new NameValuePair("name", null)));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParameterMap().size(), equalTo(1));
		assertThat(actualRequest.getParameter("name"), nullValue());
	}

	@Test
	public void buildRequestParameterMapViaWebRequestDotSetRequestParametersWithSingleRequestParamWithEmptyValue() {
		webRequest.setRequestParameters(asList(new NameValuePair("name", "")));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParameterMap().size(), equalTo(1));
		assertThat(actualRequest.getParameter("name"), equalTo(""));
	}

	@Test
	public void buildRequestParameterMapViaWebRequestDotSetRequestParametersWithSingleRequestParamWithValueSetToSpace() {
		webRequest.setRequestParameters(asList(new NameValuePair("name", " ")));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParameterMap().size(), equalTo(1));
		assertThat(actualRequest.getParameter("name"), equalTo(" "));
	}

	@Test
	public void buildRequestParameterMapViaWebRequestDotSetRequestParametersWithMultipleRequestParams() {
		webRequest.setRequestParameters(asList(new NameValuePair("name1", "value1"), new NameValuePair("name2", "value2")));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParameterMap().size(), equalTo(2));
		assertThat(actualRequest.getParameter("name1"), equalTo("value1"));
		assertThat(actualRequest.getParameter("name2"), equalTo("value2"));
	}

	@Test
	public void buildRequestParameterMapFromSingleQueryParam() throws Exception {
		webRequest.setUrl(new URL("http://example.com/example/?name=value"));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParameterMap().size(), equalTo(1));
		assertThat(actualRequest.getParameter("name"), equalTo("value"));
	}

	@Test
	public void buildRequestParameterMapFromSingleQueryParamWithoutValueAndWithoutEqualsSign() throws Exception {
		webRequest.setUrl(new URL("http://example.com/example/?name"));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParameterMap().size(), equalTo(1));
		assertThat(actualRequest.getParameter("name"), equalTo(""));
	}

	@Test
	public void buildRequestParameterMapFromSingleQueryParamWithoutValueButWithEqualsSign() throws Exception {
		webRequest.setUrl(new URL("http://example.com/example/?name="));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParameterMap().size(), equalTo(1));
		assertThat(actualRequest.getParameter("name"), equalTo(""));
	}

	@Test
	public void buildRequestParameterMapFromSingleQueryParamWithValueSetToEncodedSpace() throws Exception {
		webRequest.setUrl(new URL("http://example.com/example/?name=%20"));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParameterMap().size(), equalTo(1));
		assertThat(actualRequest.getParameter("name"), equalTo(" "));
	}

	@Test
	public void buildRequestParameterMapFromMultipleQueryParams() throws Exception {
		webRequest.setUrl(new URL("http://example.com/example/?name=value&param2=value+2"));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParameterMap().size(), equalTo(2));
		assertThat(actualRequest.getParameter("name"), equalTo("value"));
		assertThat(actualRequest.getParameter("param2"), equalTo("value 2"));
	}

	@Test
	public void buildRequestPathInfo() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getPathInfo(), nullValue());
	}

	@Test
	public void buildRequestPathInfoNull() throws Exception {
		webRequest.setUrl(new URL("http://example.com/example"));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getPathInfo(), nullValue());
	}

	@Test
	public void buildRequestAndAntPathRequestMatcher() throws Exception {
		webRequest.setUrl(new URL("http://example.com/app/login/authenticate"));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		// verify it is going to work with Spring Security's AntPathRequestMatcher
		assertThat(actualRequest.getPathInfo(), nullValue());
		assertThat(actualRequest.getServletPath(), equalTo("/login/authenticate"));
	}

	@Test
	public void buildRequestProtocol() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getProtocol(), equalTo("HTTP/1.1"));
	}

	@Test
	public void buildRequestQueryWithSingleQueryParam() throws Exception {
		String expectedQuery = "param=value";
		webRequest.setUrl(new URL("http://example.com/example?" + expectedQuery));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getQueryString(), equalTo(expectedQuery));
	}

	@Test
	public void buildRequestQueryWithSingleQueryParamWithoutValueAndWithoutEqualsSign() throws Exception {
		String expectedQuery = "param";
		webRequest.setUrl(new URL("http://example.com/example?" + expectedQuery));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getQueryString(), equalTo(expectedQuery));
	}

	@Test
	public void buildRequestQueryWithSingleQueryParamWithoutValueButWithEqualsSign() throws Exception {
		String expectedQuery = "param=";
		webRequest.setUrl(new URL("http://example.com/example?" + expectedQuery));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getQueryString(), equalTo(expectedQuery));
	}

	@Test
	public void buildRequestQueryWithSingleQueryParamWithValueSetToEncodedSpace() throws Exception {
		String expectedQuery = "param=%20";
		webRequest.setUrl(new URL("http://example.com/example?" + expectedQuery));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getQueryString(), equalTo(expectedQuery));
	}

	@Test
	public void buildRequestQueryWithMultipleQueryParams() throws Exception {
		String expectedQuery = "param1=value1&param2=value2";
		webRequest.setUrl(new URL("http://example.com/example?" + expectedQuery));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getQueryString(), equalTo(expectedQuery));
	}

	@Test
	public void buildRequestReader() throws Exception {
		String expectedBody = "request body";
		webRequest.setHttpMethod(HttpMethod.POST);
		webRequest.setRequestBody(expectedBody);

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(IOUtils.toString(actualRequest.getReader()), equalTo(expectedBody));
	}

	@Test
	public void buildRequestRemoteAddr() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getRemoteAddr(), equalTo("127.0.0.1"));
	}

	@Test
	public void buildRequestRemoteHost() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getRemoteAddr(), equalTo("127.0.0.1"));
	}

	@Test
	public void buildRequestRemotePort() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getRemotePort(), equalTo(80));
	}

	@Test
	public void buildRequestRemotePort8080() throws Exception {
		webRequest.setUrl(new URL("http://example.com:8080/"));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getRemotePort(), equalTo(8080));
	}

	@Test
	public void buildRequestRemotePort80WithDefault() throws Exception {
		webRequest.setUrl(new URL("http://example.com/"));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getRemotePort(), equalTo(80));
	}

	@Test
	public void buildRequestRequestedSessionId() throws Exception {
		String sessionId = "session-id";
		webRequest.setAdditionalHeader("Cookie", "JSESSIONID=" + sessionId);
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getRequestedSessionId(), equalTo(sessionId));
	}

	@Test
	public void buildRequestRequestedSessionIdNull() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getRequestedSessionId(), nullValue());
	}

	@Test
	public void buildRequestUri() {
		String uri = requestBuilder.buildRequest(servletContext).getRequestURI();
		assertThat(uri, equalTo("/test/this/here"));
	}

	@Test
	public void buildRequestUrl() {
		String uri = requestBuilder.buildRequest(servletContext).getRequestURL().toString();
		assertThat(uri, equalTo("http://example.com/test/this/here"));
	}

	@Test
	public void buildRequestSchemeHttp() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getScheme(), equalTo("http"));
	}

	@Test
	public void buildRequestSchemeHttps() throws Exception {
		webRequest.setUrl(new URL("https://example.com/"));
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getScheme(), equalTo("https"));
	}

	@Test
	public void buildRequestServerName() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getServerName(), equalTo("example.com"));
	}

	@Test
	public void buildRequestServerPort() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getServerPort(), equalTo(80));
	}

	@Test
	public void buildRequestServerPortDefault() throws Exception {
		webRequest.setUrl(new URL("https://example.com/"));
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getServerPort(), equalTo(-1));
	}

	@Test
	public void buildRequestServletContext() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getServletContext(), equalTo(servletContext));
	}

	@Test
	public void buildRequestServletPath() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getServletPath(), equalTo("/this/here"));
	}

	@Test
	public void buildRequestSession() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		HttpSession newSession = actualRequest.getSession();
		assertThat(newSession, notNullValue());
		assertSingleSessionCookie(
				"JSESSIONID=" + newSession.getId() + "; Path=/test; Domain=example.com");

		webRequest.setAdditionalHeader("Cookie", "JSESSIONID=" + newSession.getId());

		requestBuilder = new HtmlUnitRequestBuilder(sessions, webClient, webRequest);
		actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getSession(), sameInstance(newSession));
	}

	@Test
	public void buildRequestSessionWithExistingSession() throws Exception {
		String sessionId = "session-id";
		webRequest.setAdditionalHeader("Cookie", "JSESSIONID=" + sessionId);
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		HttpSession session = actualRequest.getSession();
		assertThat(session.getId(), equalTo(sessionId));
		assertSingleSessionCookie("JSESSIONID=" + session.getId() + "; Path=/test; Domain=example.com");

		requestBuilder = new HtmlUnitRequestBuilder(sessions, webClient, webRequest);
		actualRequest = requestBuilder.buildRequest(servletContext);
		assertThat(actualRequest.getSession(), equalTo(session));

		webRequest.setAdditionalHeader("Cookie", "JSESSIONID=" + sessionId + "NEW");
		actualRequest = requestBuilder.buildRequest(servletContext);
		assertThat(actualRequest.getSession(), not(equalTo(session)));
		assertSingleSessionCookie("JSESSIONID=" + actualRequest.getSession().getId()
				+ "; Path=/test; Domain=example.com");
	}

	@Test
	public void buildRequestSessionTrue() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		HttpSession session = actualRequest.getSession(true);
		assertThat(session, notNullValue());
	}

	@Test
	public void buildRequestSessionFalseIsNull() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		HttpSession session = actualRequest.getSession(false);
		assertThat(session, nullValue());
	}

	@Test
	public void buildRequestSessionFalseWithExistingSession() throws Exception {
		String sessionId = "session-id";
		webRequest.setAdditionalHeader("Cookie", "JSESSIONID=" + sessionId);
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		HttpSession session = actualRequest.getSession(false);
		assertThat(session, notNullValue());
	}

	@Test
	public void buildRequestSessionIsNew() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getSession().isNew(), equalTo(true));
	}

	@Test
	public void buildRequestSessionIsNewFalse() throws Exception {
		String sessionId = "session-id";
		webRequest.setAdditionalHeader("Cookie", "JSESSIONID=" + sessionId);

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getSession().isNew(), equalTo(false));
	}

	@Test
	public void buildRequestSessionInvalidate() throws Exception {
		String sessionId = "session-id";
		webRequest.setAdditionalHeader("Cookie", "JSESSIONID=" + sessionId);

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);
		HttpSession sessionToRemove = actualRequest.getSession();
		sessionToRemove.invalidate();

		assertThat(sessions.containsKey(sessionToRemove.getId()), equalTo(false));
		assertSingleSessionCookie("JSESSIONID=" + sessionToRemove.getId()
				+ "; Expires=Thu, 01-Jan-1970 00:00:01 GMT; Path=/test; Domain=example.com");

		webRequest.removeAdditionalHeader("Cookie");
		requestBuilder = new HtmlUnitRequestBuilder(sessions, webClient, webRequest);

		actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getSession().isNew(), equalTo(true));
		assertThat(sessions.containsKey(sessionToRemove.getId()), equalTo(false));
	}

	// --- setContextPath

	@Test
	public void setContextPathNull() {
		requestBuilder.setContextPath(null);

		assertThat(getContextPath(), nullValue());
	}

	@Test
	public void setContextPathEmptyString() {
		requestBuilder.setContextPath("");

		assertThat(getContextPath(), isEmptyString());
	}

	@Test(expected = IllegalArgumentException.class)
	public void setContextPathDoesNotStartWithSlash() {
		requestBuilder.setContextPath("abc/def");
	}

	@Test(expected = IllegalArgumentException.class)
	public void setContextPathEndsWithSlash() {
		requestBuilder.setContextPath("/abc/def/");
	}

	@Test
	public void setContextPath() {
		String expectedContextPath = "/abc/def";
		requestBuilder.setContextPath(expectedContextPath);

		assertThat(getContextPath(), equalTo(expectedContextPath));
	}

	@Test
	public void mergeHeader() throws Exception {
		String headerName = "PARENT";
		String headerValue = "VALUE";
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HelloController())
					.defaultRequest(get("/").header(headerName, headerValue))
					.build();

		assertThat(mockMvc.perform(requestBuilder).andReturn().getRequest().getHeader(headerName), equalTo(headerValue));
	}

	@Test
	public void mergeSession() throws Exception {
		String attrName = "PARENT";
		String attrValue = "VALUE";
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HelloController())
				.defaultRequest(get("/").sessionAttr(attrName, attrValue))
				.build();

		assertThat(mockMvc.perform(requestBuilder).andReturn().getRequest().getSession().getAttribute(attrName), equalTo(attrValue));
	}

	@Test
	public void mergeSessionNotInitialized() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HelloController())
				.defaultRequest(get("/"))
				.build();

		assertThat(mockMvc.perform(requestBuilder).andReturn().getRequest().getSession(false), nullValue());
	}

	@Test
	public void mergeParameter() throws Exception {
		String paramName = "PARENT";
		String paramValue = "VALUE";
		String paramValue2 = "VALUE2";
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HelloController())
				.defaultRequest(get("/").param(paramName, paramValue, paramValue2))
				.build();

		MockHttpServletRequest performedRequest = mockMvc.perform(requestBuilder).andReturn().getRequest();
		assertThat(asList(performedRequest.getParameterValues(paramName)), contains(paramValue, paramValue2));
	}

	@Test
	public void mergeCookie() throws Exception {
		String cookieName = "PARENT";
		String cookieValue = "VALUE";
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HelloController())
				.defaultRequest(get("/").cookie(new Cookie(cookieName, cookieValue)))
				.build();

		Cookie[] cookies = mockMvc.perform(requestBuilder).andReturn().getRequest().getCookies();
		assertThat(cookies, notNullValue());
		assertThat(cookies.length, equalTo(1));
		Cookie cookie = cookies[0];
		assertThat(cookie.getName(), equalTo(cookieName));
		assertThat(cookie.getValue(), equalTo(cookieValue));
	}

	@Test
	public void mergeRequestAttribute() throws Exception {
		String attrName = "PARENT";
		String attrValue = "VALUE";
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HelloController())
				.defaultRequest(get("/").requestAttr(attrName, attrValue))
				.build();

		assertThat(mockMvc.perform(requestBuilder).andReturn().getRequest().getAttribute(attrName), equalTo(attrValue));
	}


	private void assertSingleSessionCookie(String expected) {
		com.gargoylesoftware.htmlunit.util.Cookie jsessionidCookie = webClient.getCookieManager().getCookie("JSESSIONID");
		if (expected == null || expected.contains("Expires=Thu, 01-Jan-1970 00:00:01 GMT")) {
			assertThat(jsessionidCookie, nullValue());
			return;
		}
		String actual = jsessionidCookie.getValue();
		assertThat("JSESSIONID=" + actual + "; Path=/test; Domain=example.com", equalTo(expected));
	}

	private String getContextPath() {
		return (String) ReflectionTestUtils.getField(requestBuilder, "contextPath");
	}

}
