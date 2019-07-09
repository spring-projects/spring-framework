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

package org.springframework.mock.web;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.Cookie;

import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link MockHttpServletRequest}.
 *
 * @author Rick Evans
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Brian Clozel
 * @author Jakub Narloch
 * @author Av Pinzur
 */
public class MockHttpServletRequestTests {

	private static final String HOST = "Host";

	private final MockHttpServletRequest request = new MockHttpServletRequest();


	@Test
	public void protocolAndScheme() {
		assertThat(request.getProtocol()).isEqualTo(MockHttpServletRequest.DEFAULT_PROTOCOL);
		assertThat(request.getScheme()).isEqualTo(MockHttpServletRequest.DEFAULT_SCHEME);
		request.setProtocol("HTTP/2.0");
		request.setScheme("https");
		assertThat(request.getProtocol()).isEqualTo("HTTP/2.0");
		assertThat(request.getScheme()).isEqualTo("https");
	}

	@Test
	public void setContentAndGetInputStream() throws IOException {
		byte[] bytes = "body".getBytes(Charset.defaultCharset());
		request.setContent(bytes);
		assertThat(request.getContentLength()).isEqualTo(bytes.length);
		assertThat(StreamUtils.copyToString(request.getInputStream(), Charset.defaultCharset())).isEqualTo("body");

		request.setContent(bytes);
		assertThat(request.getContentLength()).isEqualTo(bytes.length);
		assertThat(StreamUtils.copyToString(request.getInputStream(), Charset.defaultCharset())).isEqualTo("body");
	}

	@Test
	public void setContentAndGetReader() throws IOException {
		byte[] bytes = "body".getBytes(Charset.defaultCharset());
		request.setContent(bytes);
		assertThat(request.getContentLength()).isEqualTo(bytes.length);
		assertThat(FileCopyUtils.copyToString(request.getReader())).isEqualTo("body");

		request.setContent(bytes);
		assertThat(request.getContentLength()).isEqualTo(bytes.length);
		assertThat(FileCopyUtils.copyToString(request.getReader())).isEqualTo("body");
	}

	@Test
	public void setContentAndGetContentAsByteArray() {
		byte[] bytes = "request body".getBytes();
		request.setContent(bytes);
		assertThat(request.getContentLength()).isEqualTo(bytes.length);
		assertThat(request.getContentAsByteArray()).isEqualTo(bytes);
	}

	@Test
	public void getContentAsStringWithoutSettingCharacterEncoding() throws IOException {
		assertThatIllegalStateException().isThrownBy(
				request::getContentAsString)
			.withMessageContaining("Cannot get content as a String for a null character encoding");
	}

	@Test
	public void setContentAndGetContentAsStringWithExplicitCharacterEncoding() throws IOException {
		String palindrome = "ablE was I ere I saw Elba";
		byte[] bytes = palindrome.getBytes("UTF-16");
		request.setCharacterEncoding("UTF-16");
		request.setContent(bytes);
		assertThat(request.getContentLength()).isEqualTo(bytes.length);
		assertThat(request.getContentAsString()).isEqualTo(palindrome);
	}

	@Test
	public void noContent() throws IOException {
		assertThat(request.getContentLength()).isEqualTo(-1);
		assertThat(request.getInputStream().read()).isEqualTo(-1);
		assertThat(request.getContentAsByteArray()).isNull();
	}

	@Test  // SPR-16505
	public void getReaderTwice() throws IOException {
		byte[] bytes = "body".getBytes(Charset.defaultCharset());
		request.setContent(bytes);
		assertThat(request.getReader()).isSameAs(request.getReader());
	}

	@Test  // SPR-16505
	public void getInputStreamTwice() throws IOException {
		byte[] bytes = "body".getBytes(Charset.defaultCharset());
		request.setContent(bytes);
		assertThat(request.getInputStream()).isSameAs(request.getInputStream());
	}

	@Test  // SPR-16499
	public void getReaderAfterGettingInputStream() throws IOException {
		request.getInputStream();
		assertThatIllegalStateException().isThrownBy(
				request::getReader)
			.withMessageContaining("Cannot call getReader() after getInputStream() has already been called for the current request");
	}

	@Test  // SPR-16499
	public void getInputStreamAfterGettingReader() throws IOException {
		request.getReader();
		assertThatIllegalStateException().isThrownBy(
				request::getInputStream)
			.withMessageContaining("Cannot call getInputStream() after getReader() has already been called for the current request");
	}

	@Test
	public void setContentType() {
		String contentType = "test/plain";
		request.setContentType(contentType);
		assertThat(request.getContentType()).isEqualTo(contentType);
		assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(contentType);
		assertThat(request.getCharacterEncoding()).isNull();
	}

	@Test
	public void setContentTypeUTF8() {
		String contentType = "test/plain;charset=UTF-8";
		request.setContentType(contentType);
		assertThat(request.getContentType()).isEqualTo(contentType);
		assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(contentType);
		assertThat(request.getCharacterEncoding()).isEqualTo("UTF-8");
	}

	@Test
	public void contentTypeHeader() {
		String contentType = "test/plain";
		request.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
		assertThat(request.getContentType()).isEqualTo(contentType);
		assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(contentType);
		assertThat(request.getCharacterEncoding()).isNull();
	}

	@Test
	public void contentTypeHeaderUTF8() {
		String contentType = "test/plain;charset=UTF-8";
		request.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
		assertThat(request.getContentType()).isEqualTo(contentType);
		assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(contentType);
		assertThat(request.getCharacterEncoding()).isEqualTo("UTF-8");
	}

	@Test  // SPR-12677
	public void setContentTypeHeaderWithMoreComplexCharsetSyntax() {
		String contentType = "test/plain;charset=\"utf-8\";foo=\"charset=bar\";foocharset=bar;foo=bar";
		request.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
		assertThat(request.getContentType()).isEqualTo(contentType);
		assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(contentType);
		assertThat(request.getCharacterEncoding()).isEqualTo("UTF-8");
	}

	@Test
	public void setContentTypeThenCharacterEncoding() {
		request.setContentType("test/plain");
		request.setCharacterEncoding("UTF-8");
		assertThat(request.getContentType()).isEqualTo("test/plain");
		assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo("test/plain;charset=UTF-8");
		assertThat(request.getCharacterEncoding()).isEqualTo("UTF-8");
	}

	@Test
	public void setCharacterEncodingThenContentType() {
		request.setCharacterEncoding("UTF-8");
		request.setContentType("test/plain");
		assertThat(request.getContentType()).isEqualTo("test/plain");
		assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo("test/plain;charset=UTF-8");
		assertThat(request.getCharacterEncoding()).isEqualTo("UTF-8");
	}

	@Test
	public void httpHeaderNameCasingIsPreserved() {
		String headerName = "Header1";
		request.addHeader(headerName, "value1");
		Enumeration<String> requestHeaders = request.getHeaderNames();
		assertThat(requestHeaders.nextElement()).as("HTTP header casing not being preserved").isEqualTo(headerName);
	}

	@Test
	public void setMultipleParameters() {
		request.setParameter("key1", "value1");
		request.setParameter("key2", "value2");
		Map<String, Object> params = new HashMap<>(2);
		params.put("key1", "newValue1");
		params.put("key3", new String[] { "value3A", "value3B" });
		request.setParameters(params);
		String[] values1 = request.getParameterValues("key1");
		assertThat(values1.length).isEqualTo(1);
		assertThat(request.getParameter("key1")).isEqualTo("newValue1");
		assertThat(request.getParameter("key2")).isEqualTo("value2");
		String[] values3 = request.getParameterValues("key3");
		assertThat(values3.length).isEqualTo(2);
		assertThat(values3[0]).isEqualTo("value3A");
		assertThat(values3[1]).isEqualTo("value3B");
	}

	@Test
	public void addMultipleParameters() {
		request.setParameter("key1", "value1");
		request.setParameter("key2", "value2");
		Map<String, Object> params = new HashMap<>(2);
		params.put("key1", "newValue1");
		params.put("key3", new String[] { "value3A", "value3B" });
		request.addParameters(params);
		String[] values1 = request.getParameterValues("key1");
		assertThat(values1.length).isEqualTo(2);
		assertThat(values1[0]).isEqualTo("value1");
		assertThat(values1[1]).isEqualTo("newValue1");
		assertThat(request.getParameter("key2")).isEqualTo("value2");
		String[] values3 = request.getParameterValues("key3");
		assertThat(values3.length).isEqualTo(2);
		assertThat(values3[0]).isEqualTo("value3A");
		assertThat(values3[1]).isEqualTo("value3B");
	}

	@Test
	public void removeAllParameters() {
		request.setParameter("key1", "value1");
		Map<String, Object> params = new HashMap<>(2);
		params.put("key2", "value2");
		params.put("key3", new String[] { "value3A", "value3B" });
		request.addParameters(params);
		assertThat(request.getParameterMap().size()).isEqualTo(3);
		request.removeAllParameters();
		assertThat(request.getParameterMap().size()).isEqualTo(0);
	}

	@Test
	public void cookies() {
		Cookie cookie1 = new Cookie("foo", "bar");
		Cookie cookie2 = new Cookie("baz", "qux");
		request.setCookies(cookie1, cookie2);

		Cookie[] cookies = request.getCookies();
		List<String> cookieHeaders = Collections.list(request.getHeaders(HttpHeaders.COOKIE));

		assertThat(cookies)
				.describedAs("Raw cookies stored as is")
				.hasSize(2)
				.satisfies(subject -> {
					assertThat(subject[0].getName()).isEqualTo("foo");
					assertThat(subject[0].getValue()).isEqualTo("bar");
					assertThat(subject[1].getName()).isEqualTo("baz");
					assertThat(subject[1].getValue()).isEqualTo("qux");
				});

		assertThat(cookieHeaders)
				.describedAs("Cookies -> Header conversion works as expected per RFC6265")
				.hasSize(1)
				.hasOnlyOneElementSatisfying(header -> assertThat(header).isEqualTo("foo=bar; baz=qux"));
	}

	@Test
	public void noCookies() {
		assertThat(request.getCookies()).isNull();
	}

	@Test
	public void defaultLocale() {
		Locale originalDefaultLocale = Locale.getDefault();
		try {
			Locale newDefaultLocale = originalDefaultLocale.equals(Locale.GERMANY) ? Locale.FRANCE : Locale.GERMANY;
			Locale.setDefault(newDefaultLocale);
			// Create the request after changing the default locale.
			MockHttpServletRequest request = new MockHttpServletRequest();
			assertThat(newDefaultLocale.equals(request.getLocale())).isFalse();
			assertThat(request.getLocale()).isEqualTo(Locale.ENGLISH);
		}
		finally {
			Locale.setDefault(originalDefaultLocale);
		}
	}

	@Test
	public void setPreferredLocalesWithNullList() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				request.setPreferredLocales(null));
	}

	@Test
	public void setPreferredLocalesWithEmptyList() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				request.setPreferredLocales(new ArrayList<>()));
	}

	@Test
	public void setPreferredLocales() {
		List<Locale> preferredLocales = Arrays.asList(Locale.ITALY, Locale.CHINA);
		request.setPreferredLocales(preferredLocales);
		assertEqualEnumerations(Collections.enumeration(preferredLocales), request.getLocales());
		assertThat(request.getHeader(HttpHeaders.ACCEPT_LANGUAGE)).isEqualTo("it-it, zh-cn");
	}

	@Test
	public void preferredLocalesFromAcceptLanguageHeader() {
		String headerValue = "fr-ch, fr;q=0.9, en-*;q=0.8, de;q=0.7, *;q=0.5";
		request.addHeader("Accept-Language", headerValue);
		List<Locale> actual = Collections.list(request.getLocales());
		assertThat(actual).isEqualTo(Arrays.asList(Locale.forLanguageTag("fr-ch"), Locale.forLanguageTag("fr"),
				Locale.forLanguageTag("en"), Locale.forLanguageTag("de")));
		assertThat(request.getHeader("Accept-Language")).isEqualTo(headerValue);
	}

	@Test
	public void invalidAcceptLanguageHeader() {
		request.addHeader("Accept-Language", "en_US");
		assertThat(request.getLocale()).isEqualTo(Locale.ENGLISH);
		assertThat(request.getHeader("Accept-Language")).isEqualTo("en_US");
	}

	@Test
	public void emptyAcceptLanguageHeader() {
		request.addHeader("Accept-Language", "");
		assertThat(request.getLocale()).isEqualTo(Locale.ENGLISH);
		assertThat(request.getHeader("Accept-Language")).isEqualTo("");
	}

	@Test
	public void getServerNameWithDefaultName() {
		assertThat(request.getServerName()).isEqualTo("localhost");
	}

	@Test
	public void getServerNameWithCustomName() {
		request.setServerName("example.com");
		assertThat(request.getServerName()).isEqualTo("example.com");
	}

	@Test
	public void getServerNameViaHostHeaderWithoutPort() {
		String testServer = "test.server";
		request.addHeader(HOST, testServer);
		assertThat(request.getServerName()).isEqualTo(testServer);
	}

	@Test
	public void getServerNameViaHostHeaderWithPort() {
		String testServer = "test.server";
		request.addHeader(HOST, testServer + ":8080");
		assertThat(request.getServerName()).isEqualTo(testServer);
	}

	@Test
	public void getServerNameViaHostHeaderAsIpv6AddressWithoutPort() {
		String ipv6Address = "[2001:db8:0:1]";
		request.addHeader(HOST, ipv6Address);
		assertThat(request.getServerName()).isEqualTo("2001:db8:0:1");
	}

	@Test
	public void getServerNameViaHostHeaderAsIpv6AddressWithPort() {
		String ipv6Address = "[2001:db8:0:1]:8081";
		request.addHeader(HOST, ipv6Address);
		assertThat(request.getServerName()).isEqualTo("2001:db8:0:1");
	}

	@Test
	public void getServerPortWithDefaultPort() {
		assertThat(request.getServerPort()).isEqualTo(80);
	}

	@Test
	public void getServerPortWithCustomPort() {
		request.setServerPort(8080);
		assertThat(request.getServerPort()).isEqualTo(8080);
	}

	@Test
	public void getServerPortViaHostHeaderAsIpv6AddressWithoutPort() {
		String testServer = "[2001:db8:0:1]";
		request.addHeader(HOST, testServer);
		assertThat(request.getServerPort()).isEqualTo(80);
	}

	@Test
	public void getServerPortViaHostHeaderAsIpv6AddressWithPort() {
		String testServer = "[2001:db8:0:1]";
		int testPort = 9999;
		request.addHeader(HOST, testServer + ":" + testPort);
		assertThat(request.getServerPort()).isEqualTo(testPort);
	}

	@Test
	public void getServerPortViaHostHeaderWithoutPort() {
		String testServer = "test.server";
		request.addHeader(HOST, testServer);
		assertThat(request.getServerPort()).isEqualTo(80);
	}

	@Test
	public void getServerPortViaHostHeaderWithPort() {
		String testServer = "test.server";
		int testPort = 9999;
		request.addHeader(HOST, testServer + ":" + testPort);
		assertThat(request.getServerPort()).isEqualTo(testPort);
	}

	@Test
	public void getRequestURL() {
		request.setServerPort(8080);
		request.setRequestURI("/path");
		assertThat(request.getRequestURL().toString()).isEqualTo("http://localhost:8080/path");

		request.setScheme("https");
		request.setServerName("example.com");
		request.setServerPort(8443);
		assertThat(request.getRequestURL().toString()).isEqualTo("https://example.com:8443/path");
	}

	@Test
	public void getRequestURLWithDefaults() {
		StringBuffer requestURL = request.getRequestURL();
		assertThat(requestURL.toString()).isEqualTo("http://localhost");
	}

	@Test  // SPR-16138
	public void getRequestURLWithHostHeader() {
		String testServer = "test.server";
		request.addHeader(HOST, testServer);
		StringBuffer requestURL = request.getRequestURL();
		assertThat(requestURL.toString()).isEqualTo(("http://" + testServer));
	}

	@Test  // SPR-16138
	public void getRequestURLWithHostHeaderAndPort() {
		String testServer = "test.server:9999";
		request.addHeader(HOST, testServer);
		StringBuffer requestURL = request.getRequestURL();
		assertThat(requestURL.toString()).isEqualTo(("http://" + testServer));
	}

	@Test
	public void getRequestURLWithNullRequestUri() {
		request.setRequestURI(null);
		StringBuffer requestURL = request.getRequestURL();
		assertThat(requestURL.toString()).isEqualTo("http://localhost");
	}

	@Test
	public void getRequestURLWithDefaultsAndHttps() {
		request.setScheme("https");
		request.setServerPort(443);
		StringBuffer requestURL = request.getRequestURL();
		assertThat(requestURL.toString()).isEqualTo("https://localhost");
	}

	@Test
	public void getRequestURLWithNegativePort() {
		request.setServerPort(-99);
		StringBuffer requestURL = request.getRequestURL();
		assertThat(requestURL.toString()).isEqualTo("http://localhost");
	}

	@Test
	public void isSecureWithHttpSchemeAndSecureFlagIsFalse() {
		assertThat(request.isSecure()).isFalse();
		request.setScheme("http");
		request.setSecure(false);
		assertThat(request.isSecure()).isFalse();
	}

	@Test
	public void isSecureWithHttpSchemeAndSecureFlagIsTrue() {
		assertThat(request.isSecure()).isFalse();
		request.setScheme("http");
		request.setSecure(true);
		assertThat(request.isSecure()).isTrue();
	}

	@Test
	public void isSecureWithHttpsSchemeAndSecureFlagIsFalse() {
		assertThat(request.isSecure()).isFalse();
		request.setScheme("https");
		request.setSecure(false);
		assertThat(request.isSecure()).isTrue();
	}

	@Test
	public void isSecureWithHttpsSchemeAndSecureFlagIsTrue() {
		assertThat(request.isSecure()).isFalse();
		request.setScheme("https");
		request.setSecure(true);
		assertThat(request.isSecure()).isTrue();
	}

	@Test
	public void httpHeaderDate() {
		Date date = new Date();
		request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, date);
		assertThat(request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE)).isEqualTo(date.getTime());
	}

	@Test
	public void httpHeaderTimestamp() {
		long timestamp = new Date().getTime();
		request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, timestamp);
		assertThat(request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE)).isEqualTo(timestamp);
	}

	@Test
	public void httpHeaderRfcFormattedDate() {
		request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, "Tue, 21 Jul 2015 10:00:00 GMT");
		assertThat(request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE)).isEqualTo(1437472800000L);
	}

	@Test
	public void httpHeaderFirstVariantFormattedDate() {
		request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, "Tue, 21-Jul-15 10:00:00 GMT");
		assertThat(request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE)).isEqualTo(1437472800000L);
	}

	@Test
	public void httpHeaderSecondVariantFormattedDate() {
		request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, "Tue Jul 21 10:00:00 2015");
		assertThat(request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE)).isEqualTo(1437472800000L);
	}

	@Test
	public void httpHeaderFormattedDateError() {
		request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, "This is not a date");
		assertThatIllegalArgumentException().isThrownBy(() ->
				request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE));
	}

	private void assertEqualEnumerations(Enumeration<?> enum1, Enumeration<?> enum2) {
		int count = 0;
		while (enum1.hasMoreElements()) {
			assertThat(enum2.hasMoreElements()).as("enumerations must be equal in length").isTrue();
			String message = "enumeration element #" + ++count;
			assertThat(enum2.nextElement()).as(message).isEqualTo(enum1.nextElement());
		}
	}

}
