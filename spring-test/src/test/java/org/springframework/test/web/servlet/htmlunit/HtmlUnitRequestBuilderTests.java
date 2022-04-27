/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.web.servlet.htmlunit;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import com.gargoylesoftware.htmlunit.FormEncodingType;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.util.KeyDataPair;
import com.gargoylesoftware.htmlunit.util.MimeType;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import org.apache.commons.io.IOUtils;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
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


	@BeforeEach
	public void setup() throws Exception {
		webRequest = new WebRequest(new URL("https://example.com/test/this/here"));
		webRequest.setHttpMethod(HttpMethod.GET);
		requestBuilder = new HtmlUnitRequestBuilder(sessions, webClient, webRequest);
	}


	// --- constructor

	@Test
	public void constructorNullSessions() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new HtmlUnitRequestBuilder(null, webClient, webRequest));
	}

	@Test
	public void constructorNullWebClient() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new HtmlUnitRequestBuilder(sessions, null, webRequest));
	}

	@Test
	public void constructorNullWebRequest() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new HtmlUnitRequestBuilder(sessions, webClient, null));
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

		assertThat(actualRequest.getAuthType()).isEqualTo("Basic");
		assertThat(actualRequest.getHeader("Authorization")).isEqualTo(authzHeaderValue);
	}

	@Test
	public void buildRequestCharacterEncoding() {
		webRequest.setCharset(StandardCharsets.UTF_8);

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getCharacterEncoding()).isEqualTo("UTF-8");
	}

	@Test
	public void buildRequestDefaultCharacterEncoding() {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getCharacterEncoding()).isEqualTo("ISO-8859-1");
	}

	@Test
	public void buildRequestContentLength() {
		String content = "some content that has length";
		webRequest.setHttpMethod(HttpMethod.POST);
		webRequest.setRequestBody(content);

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getContentLength()).isEqualTo(content.length());
	}

	@Test
	public void buildRequestContentType() {
		String contentType = "text/html;charset=UTF-8";
		webRequest.setAdditionalHeader("Content-Type", contentType);

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getContentType()).isEqualTo(contentType);
		assertThat(actualRequest.getHeader("Content-Type")).isEqualTo(contentType);
	}

	@Test  // SPR-14916
	public void buildRequestContentTypeWithFormSubmission() {
		webRequest.setEncodingType(FormEncodingType.URL_ENCODED);

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getContentType()).isEqualTo("application/x-www-form-urlencoded");
		assertThat(actualRequest.getHeader("Content-Type"))
				.isEqualTo("application/x-www-form-urlencoded;charset=ISO-8859-1");
	}


	@Test
	public void buildRequestContextPathUsesFirstSegmentByDefault() {
		String contextPath = requestBuilder.buildRequest(servletContext).getContextPath();

		assertThat(contextPath).isEqualTo("/test");
	}

	@Test
	public void buildRequestContextPathUsesNoFirstSegmentWithDefault() throws MalformedURLException {
		webRequest.setUrl(new URL("https://example.com/"));
		String contextPath = requestBuilder.buildRequest(servletContext).getContextPath();

		assertThat(contextPath).isEqualTo("");
	}

	@Test
	public void buildRequestContextPathInvalid() {
		requestBuilder.setContextPath("/invalid");

		assertThatIllegalArgumentException().isThrownBy(() ->
				requestBuilder.buildRequest(servletContext).getContextPath());
	}

	@Test
	public void buildRequestContextPathEmpty() {
		String expected = "";
		requestBuilder.setContextPath(expected);

		String contextPath = requestBuilder.buildRequest(servletContext).getContextPath();

		assertThat(contextPath).isEqualTo(expected);
	}

	@Test
	public void buildRequestContextPathExplicit() {
		String expected = "/test";
		requestBuilder.setContextPath(expected);

		String contextPath = requestBuilder.buildRequest(servletContext).getContextPath();

		assertThat(contextPath).isEqualTo(expected);
	}

	@Test
	public void buildRequestContextPathMulti() {
		String expected = "/test/this";
		requestBuilder.setContextPath(expected);

		String contextPath = requestBuilder.buildRequest(servletContext).getContextPath();

		assertThat(contextPath).isEqualTo(expected);
	}

	@Test
	public void buildRequestCookiesNull() {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getCookies()).isNull();
	}

	@Test
	public void buildRequestCookiesSingle() {
		webRequest.setAdditionalHeader("Cookie", "name=value");

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		Cookie[] cookies = actualRequest.getCookies();
		assertThat(cookies.length).isEqualTo(1);
		assertThat(cookies[0].getName()).isEqualTo("name");
		assertThat(cookies[0].getValue()).isEqualTo("value");
	}

	@Test
	public void buildRequestCookiesMulti() {
		webRequest.setAdditionalHeader("Cookie", "name=value; name2=value2");

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		Cookie[] cookies = actualRequest.getCookies();
		assertThat(cookies.length).isEqualTo(2);
		Cookie cookie = cookies[0];
		assertThat(cookie.getName()).isEqualTo("name");
		assertThat(cookie.getValue()).isEqualTo("value");
		cookie = cookies[1];
		assertThat(cookie.getName()).isEqualTo("name2");
		assertThat(cookie.getValue()).isEqualTo("value2");
	}

	@Test
	@SuppressWarnings("deprecation")
	public void buildRequestInputStream() throws Exception {
		String content = "some content that has length";
		webRequest.setHttpMethod(HttpMethod.POST);
		webRequest.setRequestBody(content);

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(IOUtils.toString(actualRequest.getInputStream())).isEqualTo(content);
	}

	@Test
	public void buildRequestLocalAddr() {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getLocalAddr()).isEqualTo("127.0.0.1");
	}

	@Test
	public void buildRequestLocaleDefault() {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getLocale()).isEqualTo(Locale.getDefault());
	}

	@Test
	public void buildRequestLocaleDa() {
		webRequest.setAdditionalHeader("Accept-Language", "da");

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getLocale()).isEqualTo(new Locale("da"));
	}

	@Test
	public void buildRequestLocaleEnGbQ08() {
		webRequest.setAdditionalHeader("Accept-Language", "en-gb;q=0.8");

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getLocale()).isEqualTo(new Locale("en", "gb"));
	}

	@Test
	public void buildRequestLocaleEnQ07() {
		webRequest.setAdditionalHeader("Accept-Language", "en");

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getLocale()).isEqualTo(new Locale("en", ""));
	}

	@Test
	public void buildRequestLocaleEnUs() {
		webRequest.setAdditionalHeader("Accept-Language", "en-US");

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getLocale()).isEqualTo(Locale.US);
	}

	@Test
	public void buildRequestLocaleFr() {
		webRequest.setAdditionalHeader("Accept-Language", "fr");

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getLocale()).isEqualTo(Locale.FRENCH);
	}

	@Test
	public void buildRequestLocaleMulti() {
		webRequest.setAdditionalHeader("Accept-Language", "en-gb;q=0.8, da, en;q=0.7");

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(Collections.list(actualRequest.getLocales()))
				.containsExactly(new Locale("da"), new Locale("en", "gb"), new Locale("en", ""));
	}

	@Test
	public void buildRequestLocalName() {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getLocalName()).isEqualTo("localhost");
	}

	@Test
	public void buildRequestLocalPort() throws Exception {
		webRequest.setUrl(new URL("http://localhost:80/test/this/here"));
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getLocalPort()).isEqualTo(80);
	}

	@Test
	public void buildRequestLocalMissing() throws Exception {
		webRequest.setUrl(new URL("http://localhost/test/this"));
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getLocalPort()).isEqualTo(-1);
	}

	@Test
	public void buildRequestMethods() {
		for (HttpMethod expectedMethod : HttpMethod.values()) {
			webRequest.setHttpMethod(expectedMethod);
			String actualMethod = requestBuilder.buildRequest(servletContext).getMethod();
			assertThat(actualMethod).isEqualTo(expectedMethod.name());
		}
	}

	@Test
	public void buildRequestParameterMapViaWebRequestDotSetRequestParametersWithSingleRequestParam() {
		webRequest.setRequestParameters(Arrays.asList(new NameValuePair("name", "value")));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParameterMap().size()).isEqualTo(1);
		assertThat(actualRequest.getParameter("name")).isEqualTo("value");
	}

	@Test
	public void buildRequestParameterMapViaWebRequestDotSetRequestParametersWithSingleRequestParamWithNullValue() {
		webRequest.setRequestParameters(Arrays.asList(new NameValuePair("name", null)));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParameterMap().size()).isEqualTo(1);
		assertThat(actualRequest.getParameter("name")).isNull();
	}

	@Test
	public void buildRequestParameterMapViaWebRequestDotSetRequestParametersWithSingleRequestParamWithEmptyValue() {
		webRequest.setRequestParameters(Arrays.asList(new NameValuePair("name", "")));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParameterMap().size()).isEqualTo(1);
		assertThat(actualRequest.getParameter("name")).isEqualTo("");
	}

	@Test
	public void buildRequestParameterMapViaWebRequestDotSetRequestParametersWithSingleRequestParamWithValueSetToSpace() {
		webRequest.setRequestParameters(Arrays.asList(new NameValuePair("name", " ")));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParameterMap().size()).isEqualTo(1);
		assertThat(actualRequest.getParameter("name")).isEqualTo(" ");
	}

	@Test
	public void buildRequestParameterMapViaWebRequestDotSetRequestParametersWithMultipleRequestParams() {
		webRequest.setRequestParameters(Arrays.asList(new NameValuePair("name1", "value1"), new NameValuePair("name2", "value2")));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParameterMap().size()).isEqualTo(2);
		assertThat(actualRequest.getParameter("name1")).isEqualTo("value1");
		assertThat(actualRequest.getParameter("name2")).isEqualTo("value2");
	}

	@Test // gh-24926
	public void buildRequestParameterMapViaWebRequestDotSetRequestParametersWithFileToUploadAsParameter() throws Exception {
		webRequest.setRequestParameters(Collections.singletonList(
				new KeyDataPair("key",
						new ClassPathResource("org/springframework/test/web/htmlunit/test.txt").getFile(),
						"test.txt", MimeType.TEXT_PLAIN, StandardCharsets.UTF_8)));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParts()).hasSize(1);
		Part part = actualRequest.getPart("key");
		assertThat(part).isNotNull();
		assertThat(part.getName()).isEqualTo("key");
		assertThat(IOUtils.toString(part.getInputStream(), StandardCharsets.UTF_8)).isEqualTo("test file");
		assertThat(part.getSubmittedFileName()).isEqualTo("test.txt");
		assertThat(part.getContentType()).isEqualTo(MimeType.TEXT_PLAIN);
	}

	@Test // gh-27199
	public void buildRequestParameterMapViaWebRequestDotSetRequestParametersWithFileDataAsParameter() throws Exception {
		String data = "{}";
		KeyDataPair keyDataPair = new KeyDataPair("key", new File("test.json"), null, MimeType.APPLICATION_JSON, StandardCharsets.UTF_8);
		keyDataPair.setData(data.getBytes());

		webRequest.setRequestParameters(Collections.singletonList(keyDataPair));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParts()).hasSize(1);
		Part part = actualRequest.getPart("key");

		assertSoftly(softly -> {
			softly.assertThat(part).as("part").isNotNull();
			softly.assertThat(part.getName()).as("name").isEqualTo("key");
			softly.assertThat(part.getSubmittedFileName()).as("file name").isEqualTo("test.json");
			softly.assertThat(part.getContentType()).as("content type").isEqualTo(MimeType.APPLICATION_JSON);
			try {
				softly.assertThat(IOUtils.toString(part.getInputStream(), StandardCharsets.UTF_8)).as("content").isEqualTo(data);
			}
			catch (IOException ex) {
				softly.fail("failed to get InputStream", ex);
			}
		});
	}

	@Test // gh-26799
	public void buildRequestParameterMapViaWebRequestDotSetRequestParametersWithNullFileToUploadAsParameter() throws Exception {
		webRequest.setRequestParameters(Collections.singletonList(new KeyDataPair("key", null, null, null, (Charset) null)));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParts()).hasSize(1);
		Part part = actualRequest.getPart("key");

		assertSoftly(softly -> {
			softly.assertThat(part).as("part").isNotNull();
			softly.assertThat(part.getName()).as("name").isEqualTo("key");
			softly.assertThat(part.getSize()).as("size").isEqualTo(0);
			try {
				softly.assertThat(part.getInputStream()).as("input stream").isEmpty();
			}
			catch (IOException ex) {
				softly.fail("failed to get InputStream", ex);
			}
			softly.assertThat(part.getSubmittedFileName()).as("filename").isEqualTo("");
			softly.assertThat(part.getContentType()).as("content-type").isEqualTo("application/octet-stream");
		});
	}

	@Test
	public void buildRequestParameterMapFromSingleQueryParam() throws Exception {
		webRequest.setUrl(new URL("https://example.com/example/?name=value"));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParameterMap().size()).isEqualTo(1);
		assertThat(actualRequest.getParameter("name")).isEqualTo("value");
	}

	// SPR-14177
	@Test
	public void buildRequestParameterMapDecodesParameterName() throws Exception {
		webRequest.setUrl(new URL("https://example.com/example/?row%5B0%5D=value"));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParameterMap().size()).isEqualTo(1);
		assertThat(actualRequest.getParameter("row[0]")).isEqualTo("value");
	}

	@Test
	public void buildRequestParameterMapDecodesParameterValue() throws Exception {
		webRequest.setUrl(new URL("https://example.com/example/?name=row%5B0%5D"));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParameterMap().size()).isEqualTo(1);
		assertThat(actualRequest.getParameter("name")).isEqualTo("row[0]");
	}

	@Test
	public void buildRequestParameterMapFromSingleQueryParamWithoutValueAndWithoutEqualsSign() throws Exception {
		webRequest.setUrl(new URL("https://example.com/example/?name"));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParameterMap().size()).isEqualTo(1);
		assertThat(actualRequest.getParameter("name")).isEqualTo("");
	}

	@Test
	public void buildRequestParameterMapFromSingleQueryParamWithoutValueButWithEqualsSign() throws Exception {
		webRequest.setUrl(new URL("https://example.com/example/?name="));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParameterMap().size()).isEqualTo(1);
		assertThat(actualRequest.getParameter("name")).isEqualTo("");
	}

	@Test
	public void buildRequestParameterMapFromSingleQueryParamWithValueSetToEncodedSpace() throws Exception {
		webRequest.setUrl(new URL("https://example.com/example/?name=%20"));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParameterMap().size()).isEqualTo(1);
		assertThat(actualRequest.getParameter("name")).isEqualTo(" ");
	}

	@Test
	public void buildRequestParameterMapFromMultipleQueryParams() throws Exception {
		webRequest.setUrl(new URL("https://example.com/example/?name=value&param2=value+2"));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getParameterMap().size()).isEqualTo(2);
		assertThat(actualRequest.getParameter("name")).isEqualTo("value");
		assertThat(actualRequest.getParameter("param2")).isEqualTo("value 2");
	}

	@Test
	public void buildRequestPathInfo() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getPathInfo()).isNull();
	}

	@Test
	public void buildRequestPathInfoNull() throws Exception {
		webRequest.setUrl(new URL("https://example.com/example"));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getPathInfo()).isNull();
	}

	@Test
	public void buildRequestAndAntPathRequestMatcher() throws Exception {
		webRequest.setUrl(new URL("https://example.com/app/login/authenticate"));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		// verify it is going to work with Spring Security's AntPathRequestMatcher
		assertThat(actualRequest.getPathInfo()).isNull();
		assertThat(actualRequest.getServletPath()).isEqualTo("/login/authenticate");
	}

	@Test
	public void buildRequestProtocol() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getProtocol()).isEqualTo("HTTP/1.1");
	}

	@Test
	public void buildRequestQueryWithSingleQueryParam() throws Exception {
		String expectedQuery = "param=value";
		webRequest.setUrl(new URL("https://example.com/example?" + expectedQuery));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getQueryString()).isEqualTo(expectedQuery);
	}

	@Test
	public void buildRequestQueryWithSingleQueryParamWithoutValueAndWithoutEqualsSign() throws Exception {
		String expectedQuery = "param";
		webRequest.setUrl(new URL("https://example.com/example?" + expectedQuery));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getQueryString()).isEqualTo(expectedQuery);
	}

	@Test
	public void buildRequestQueryWithSingleQueryParamWithoutValueButWithEqualsSign() throws Exception {
		String expectedQuery = "param=";
		webRequest.setUrl(new URL("https://example.com/example?" + expectedQuery));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getQueryString()).isEqualTo(expectedQuery);
	}

	@Test
	public void buildRequestQueryWithSingleQueryParamWithValueSetToEncodedSpace() throws Exception {
		String expectedQuery = "param=%20";
		webRequest.setUrl(new URL("https://example.com/example?" + expectedQuery));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getQueryString()).isEqualTo(expectedQuery);
	}

	@Test
	public void buildRequestQueryWithMultipleQueryParams() throws Exception {
		String expectedQuery = "param1=value1&param2=value2";
		webRequest.setUrl(new URL("https://example.com/example?" + expectedQuery));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getQueryString()).isEqualTo(expectedQuery);
	}

	@Test
	public void buildRequestReader() throws Exception {
		String expectedBody = "request body";
		webRequest.setHttpMethod(HttpMethod.POST);
		webRequest.setRequestBody(expectedBody);

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(IOUtils.toString(actualRequest.getReader())).isEqualTo(expectedBody);
	}

	@Test
	public void buildRequestRemoteAddr() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getRemoteAddr()).isEqualTo("127.0.0.1");
	}

	@Test
	public void buildRequestRemoteHost() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getRemoteAddr()).isEqualTo("127.0.0.1");
	}

	@Test
	public void buildRequestRemotePort() throws Exception {
		webRequest.setUrl(new URL("http://localhost:80/test/this/here"));
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getRemotePort()).isEqualTo(80);
	}

	@Test
	public void buildRequestRemotePort8080() throws Exception {
		webRequest.setUrl(new URL("https://example.com:8080/"));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getRemotePort()).isEqualTo(8080);
	}

	@Test
	public void buildRequestRemotePort80WithDefault() throws Exception {
		webRequest.setUrl(new URL("http://company.example/"));

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getRemotePort()).isEqualTo(80);
	}

	@Test
	public void buildRequestRequestedSessionId() throws Exception {
		String sessionId = "session-id";
		webRequest.setAdditionalHeader("Cookie", "JSESSIONID=" + sessionId);
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getRequestedSessionId()).isEqualTo(sessionId);
	}

	@Test
	public void buildRequestRequestedSessionIdNull() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getRequestedSessionId()).isNull();
	}

	@Test
	public void buildRequestUri() {
		String uri = requestBuilder.buildRequest(servletContext).getRequestURI();
		assertThat(uri).isEqualTo("/test/this/here");
	}

	@Test
	public void buildRequestUrl() {
		String uri = requestBuilder.buildRequest(servletContext).getRequestURL().toString();
		assertThat(uri).isEqualTo("https://example.com/test/this/here");
	}

	@Test
	public void buildRequestSchemeHttp() throws Exception {
		webRequest.setUrl(new URL("http://localhost:80/test/this/here"));
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getScheme()).isEqualTo("http");
	}

	@Test
	public void buildRequestSchemeHttps() throws Exception {
		webRequest.setUrl(new URL("https://example.com/"));
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getScheme()).isEqualTo("https");
	}

	@Test
	public void buildRequestServerName() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getServerName()).isEqualTo("example.com");
	}

	@Test
	public void buildRequestServerPort() throws Exception {
		webRequest.setUrl(new URL("http://localhost:80/test/this/here"));
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getServerPort()).isEqualTo(80);
	}

	@Test
	public void buildRequestServerPortDefault() throws Exception {
		webRequest.setUrl(new URL("https://example.com/"));
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getServerPort()).isEqualTo(-1);
	}

	@Test
	public void buildRequestServletContext() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getServletContext()).isEqualTo(servletContext);
	}

	@Test
	public void buildRequestServletPath() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getServletPath()).isEqualTo("/this/here");
	}

	@Test
	public void buildRequestSession() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		HttpSession newSession = actualRequest.getSession();
		assertThat(newSession).isNotNull();
		assertSingleSessionCookie(
				"JSESSIONID=" + newSession.getId() + "; Path=/test; Domain=example.com");

		webRequest.setAdditionalHeader("Cookie", "JSESSIONID=" + newSession.getId());

		requestBuilder = new HtmlUnitRequestBuilder(sessions, webClient, webRequest);
		actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getSession()).isSameAs(newSession);
	}

	@Test
	public void buildRequestSessionWithExistingSession() throws Exception {
		String sessionId = "session-id";
		webRequest.setAdditionalHeader("Cookie", "JSESSIONID=" + sessionId);
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		HttpSession session = actualRequest.getSession();
		assertThat(session.getId()).isEqualTo(sessionId);
		assertSingleSessionCookie("JSESSIONID=" + session.getId() + "; Path=/test; Domain=example.com");

		requestBuilder = new HtmlUnitRequestBuilder(sessions, webClient, webRequest);
		actualRequest = requestBuilder.buildRequest(servletContext);
		assertThat(actualRequest.getSession()).isEqualTo(session);

		webRequest.setAdditionalHeader("Cookie", "JSESSIONID=" + sessionId + "NEW");
		actualRequest = requestBuilder.buildRequest(servletContext);
		assertThat(actualRequest.getSession()).isNotEqualTo(session);
		assertSingleSessionCookie("JSESSIONID=" + actualRequest.getSession().getId()
				+ "; Path=/test; Domain=example.com");
	}

	@Test
	public void buildRequestSessionTrue() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		HttpSession session = actualRequest.getSession(true);
		assertThat(session).isNotNull();
	}

	@Test
	public void buildRequestSessionFalseIsNull() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		HttpSession session = actualRequest.getSession(false);
		assertThat(session).isNull();
	}

	@Test
	public void buildRequestSessionFalseWithExistingSession() throws Exception {
		String sessionId = "session-id";
		webRequest.setAdditionalHeader("Cookie", "JSESSIONID=" + sessionId);
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		HttpSession session = actualRequest.getSession(false);
		assertThat(session).isNotNull();
	}

	@Test
	public void buildRequestSessionIsNew() throws Exception {
		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getSession().isNew()).isTrue();
	}

	@Test
	public void buildRequestSessionIsNewFalse() throws Exception {
		String sessionId = "session-id";
		webRequest.setAdditionalHeader("Cookie", "JSESSIONID=" + sessionId);

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getSession().isNew()).isFalse();
	}

	@Test
	public void buildRequestSessionInvalidate() throws Exception {
		String sessionId = "session-id";
		webRequest.setAdditionalHeader("Cookie", "JSESSIONID=" + sessionId);

		MockHttpServletRequest actualRequest = requestBuilder.buildRequest(servletContext);
		HttpSession sessionToRemove = actualRequest.getSession();
		sessionToRemove.invalidate();

		assertThat(sessions.containsKey(sessionToRemove.getId())).isFalse();
		assertSingleSessionCookie("JSESSIONID=" + sessionToRemove.getId()
				+ "; Expires=Thu, 01-Jan-1970 00:00:01 GMT; Path=/test; Domain=example.com");

		webRequest.removeAdditionalHeader("Cookie");
		requestBuilder = new HtmlUnitRequestBuilder(sessions, webClient, webRequest);

		actualRequest = requestBuilder.buildRequest(servletContext);

		assertThat(actualRequest.getSession().isNew()).isTrue();
		assertThat(sessions.containsKey(sessionToRemove.getId())).isFalse();
	}

	// --- setContextPath

	@Test
	public void setContextPathNull() {
		requestBuilder.setContextPath(null);

		assertThat(getContextPath()).isNull();
	}

	@Test
	public void setContextPathEmptyString() {
		requestBuilder.setContextPath("");

		assertThat(getContextPath()).isEmpty();
	}

	@Test
	public void setContextPathDoesNotStartWithSlash() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				requestBuilder.setContextPath("abc/def"));
	}

	@Test
	public void setContextPathEndsWithSlash() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				requestBuilder.setContextPath("/abc/def/"));
	}

	@Test
	public void setContextPath() {
		String expectedContextPath = "/abc/def";
		requestBuilder.setContextPath(expectedContextPath);

		assertThat(getContextPath()).isEqualTo(expectedContextPath);
	}

	@Test
	public void mergeHeader() throws Exception {
		String headerName = "PARENT";
		String headerValue = "VALUE";
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HelloController())
					.defaultRequest(get("/").header(headerName, headerValue))
					.build();

		assertThat(mockMvc.perform(requestBuilder).andReturn().getRequest().getHeader(headerName)).isEqualTo(headerValue);
	}

	@Test
	public void mergeSession() throws Exception {
		String attrName = "PARENT";
		String attrValue = "VALUE";
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HelloController())
				.defaultRequest(get("/").sessionAttr(attrName, attrValue))
				.build();

		assertThat(mockMvc.perform(requestBuilder).andReturn().getRequest().getSession().getAttribute(attrName)).isEqualTo(attrValue);
	}

	@Test
	public void mergeSessionNotInitialized() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HelloController())
				.defaultRequest(get("/"))
				.build();

		assertThat(mockMvc.perform(requestBuilder).andReturn().getRequest().getSession(false)).isNull();
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
		assertThat(performedRequest.getParameterValues(paramName)).containsExactly(paramValue, paramValue2);
	}

	@Test
	public void mergeCookie() throws Exception {
		String cookieName = "PARENT";
		String cookieValue = "VALUE";
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HelloController())
				.defaultRequest(get("/").cookie(new Cookie(cookieName, cookieValue)))
				.build();

		Cookie[] cookies = mockMvc.perform(requestBuilder).andReturn().getRequest().getCookies();
		assertThat(cookies).isNotNull();
		assertThat(cookies.length).isEqualTo(1);
		Cookie cookie = cookies[0];
		assertThat(cookie.getName()).isEqualTo(cookieName);
		assertThat(cookie.getValue()).isEqualTo(cookieValue);
	}

	@Test
	public void mergeRequestAttribute() throws Exception {
		String attrName = "PARENT";
		String attrValue = "VALUE";
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HelloController())
				.defaultRequest(get("/").requestAttr(attrName, attrValue))
				.build();

		assertThat(mockMvc.perform(requestBuilder).andReturn().getRequest().getAttribute(attrName)).isEqualTo(attrValue);
	}

	@Test // SPR-14584
	public void mergeDoesNotCorruptPathInfoOnParent() throws Exception {
		String pathInfo = "/foo/bar";
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HelloController())
				.defaultRequest(get("/"))
				.build();

		assertThat(mockMvc.perform(get(pathInfo)).andReturn().getRequest().getPathInfo()).isEqualTo(pathInfo);

		mockMvc.perform(requestBuilder);

		assertThat(mockMvc.perform(get(pathInfo)).andReturn().getRequest().getPathInfo()).isEqualTo(pathInfo);
	}


	private void assertSingleSessionCookie(String expected) {
		com.gargoylesoftware.htmlunit.util.Cookie jsessionidCookie = webClient.getCookieManager().getCookie("JSESSIONID");
		if (expected == null || expected.contains("Expires=Thu, 01-Jan-1970 00:00:01 GMT")) {
			assertThat(jsessionidCookie).isNull();
			return;
		}
		String actual = jsessionidCookie.getValue();
		assertThat("JSESSIONID=" + actual + "; Path=/test; Domain=example.com").isEqualTo(expected);
	}

	private String getContextPath() {
		return (String) ReflectionTestUtils.getField(requestBuilder, "contextPath");
	}

}
