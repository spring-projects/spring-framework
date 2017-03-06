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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.util.StreamUtils;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link MockHttpServletRequest}.
 *
 * @author Rick Evans
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Brian Clozel
 * @author Jakub Narloch
 */
public class MockHttpServletRequestTests {

	private static final String HOST = "Host";

	private static final String CONTENT_TYPE = "Content-Type";

	private static final String IF_MODIFIED_SINCE = "If-Modified-Since";

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void protocolAndScheme() {
		assertEquals(MockHttpServletRequest.DEFAULT_PROTOCOL, request.getProtocol());
		assertEquals(MockHttpServletRequest.DEFAULT_SCHEME, request.getScheme());
		request.setProtocol("HTTP/2.0");
		request.setScheme("https");
		assertEquals("HTTP/2.0", request.getProtocol());
		assertEquals("https", request.getScheme());
	}

	@Test
	public void setContentAndGetInputStream() throws IOException {
		byte[] bytes = "body".getBytes(Charset.defaultCharset());
		request.setContent(bytes);
		assertEquals(bytes.length, request.getContentLength());
		assertNotNull(request.getInputStream());
		assertEquals("body", StreamUtils.copyToString(request.getInputStream(), Charset.defaultCharset()));
	}

	@Test
	public void setContentAndGetContentAsByteArray() throws IOException {
		byte[] bytes = "request body".getBytes();
		request.setContent(bytes);
		assertEquals(bytes.length, request.getContentLength());
		assertNotNull(request.getContentAsByteArray());
		assertEquals(bytes, request.getContentAsByteArray());
	}

	@Test
	public void getContentAsStringWithoutSettingCharacterEncoding() throws IOException {
		exception.expect(IllegalStateException.class);
		exception.expectMessage("Cannot get content as a String for a null character encoding");
		request.getContentAsString();
	}

	@Test
	public void setContentAndGetContentAsStringWithExplicitCharacterEncoding() throws IOException {
		String palindrome = "ablE was I ere I saw Elba";
		byte[] bytes = palindrome.getBytes("UTF-16");
		request.setCharacterEncoding("UTF-16");
		request.setContent(bytes);
		assertEquals(bytes.length, request.getContentLength());
		assertNotNull(request.getContentAsString());
		assertEquals(palindrome, request.getContentAsString());
	}

	@Test
	public void noContent() throws IOException {
		assertEquals(-1, request.getContentLength());
		assertNotNull(request.getInputStream());
		assertEquals(-1, request.getInputStream().read());
		assertNull(request.getContentAsByteArray());
	}

	@Test
	public void setContentType() {
		String contentType = "test/plain";
		request.setContentType(contentType);
		assertEquals(contentType, request.getContentType());
		assertEquals(contentType, request.getHeader(CONTENT_TYPE));
		assertNull(request.getCharacterEncoding());
	}

	@Test
	public void setContentTypeUTF8() {
		String contentType = "test/plain;charset=UTF-8";
		request.setContentType(contentType);
		assertEquals(contentType, request.getContentType());
		assertEquals(contentType, request.getHeader(CONTENT_TYPE));
		assertEquals("UTF-8", request.getCharacterEncoding());
	}

	@Test
	public void contentTypeHeader() {
		String contentType = "test/plain";
		request.addHeader("Content-Type", contentType);
		assertEquals(contentType, request.getContentType());
		assertEquals(contentType, request.getHeader(CONTENT_TYPE));
		assertNull(request.getCharacterEncoding());
	}

	@Test
	public void contentTypeHeaderUTF8() {
		String contentType = "test/plain;charset=UTF-8";
		request.addHeader("Content-Type", contentType);
		assertEquals(contentType, request.getContentType());
		assertEquals(contentType, request.getHeader(CONTENT_TYPE));
		assertEquals("UTF-8", request.getCharacterEncoding());
	}

	// SPR-12677

	@Test
	public void setContentTypeHeaderWithMoreComplexCharsetSyntax() {
		String contentType = "test/plain;charset=\"utf-8\";foo=\"charset=bar\";foocharset=bar;foo=bar";
		request.addHeader("Content-Type", contentType);
		assertEquals(contentType, request.getContentType());
		assertEquals(contentType, request.getHeader(CONTENT_TYPE));
		assertEquals("UTF-8", request.getCharacterEncoding());
	}

	@Test
	public void setContentTypeThenCharacterEncoding() {
		request.setContentType("test/plain");
		request.setCharacterEncoding("UTF-8");
		assertEquals("test/plain", request.getContentType());
		assertEquals("test/plain;charset=UTF-8", request.getHeader(CONTENT_TYPE));
		assertEquals("UTF-8", request.getCharacterEncoding());
	}

	@Test
	public void setCharacterEncodingThenContentType() {
		request.setCharacterEncoding("UTF-8");
		request.setContentType("test/plain");
		assertEquals("test/plain", request.getContentType());
		assertEquals("test/plain;charset=UTF-8", request.getHeader(CONTENT_TYPE));
		assertEquals("UTF-8", request.getCharacterEncoding());
	}

	@Test
	public void httpHeaderNameCasingIsPreserved() throws Exception {
		String headerName = "Header1";
		request.addHeader(headerName, "value1");
		Enumeration<String> requestHeaders = request.getHeaderNames();
		assertNotNull(requestHeaders);
		assertEquals("HTTP header casing not being preserved", headerName, requestHeaders.nextElement());
	}

	@Test
	public void nullParameterName() {
		assertNull(request.getParameter(null));
		assertNull(request.getParameterValues(null));
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
		assertEquals(1, values1.length);
		assertEquals("newValue1", request.getParameter("key1"));
		assertEquals("value2", request.getParameter("key2"));
		String[] values3 = request.getParameterValues("key3");
		assertEquals(2, values3.length);
		assertEquals("value3A", values3[0]);
		assertEquals("value3B", values3[1]);
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
		assertEquals(2, values1.length);
		assertEquals("value1", values1[0]);
		assertEquals("newValue1", values1[1]);
		assertEquals("value2", request.getParameter("key2"));
		String[] values3 = request.getParameterValues("key3");
		assertEquals(2, values3.length);
		assertEquals("value3A", values3[0]);
		assertEquals("value3B", values3[1]);
	}

	@Test
	public void removeAllParameters() {
		request.setParameter("key1", "value1");
		Map<String, Object> params = new HashMap<>(2);
		params.put("key2", "value2");
		params.put("key3", new String[] { "value3A", "value3B" });
		request.addParameters(params);
		assertEquals(3, request.getParameterMap().size());
		request.removeAllParameters();
		assertEquals(0, request.getParameterMap().size());
	}

	@Test
	public void cookies() {
		Cookie cookie1 = new Cookie("foo", "bar");
		Cookie cookie2 = new Cookie("baz", "qux");
		request.setCookies(cookie1, cookie2);

		Cookie[] cookies = request.getCookies();

		assertEquals(2, cookies.length);
		assertEquals("foo", cookies[0].getName());
		assertEquals("bar", cookies[0].getValue());
		assertEquals("baz", cookies[1].getName());
		assertEquals("qux", cookies[1].getValue());
	}

	@Test
	public void noCookies() {
		assertNull(request.getCookies());
	}

	@Test
	public void defaultLocale() {
		Locale originalDefaultLocale = Locale.getDefault();
		try {
			Locale newDefaultLocale = originalDefaultLocale.equals(Locale.GERMANY) ? Locale.FRANCE : Locale.GERMANY;
			Locale.setDefault(newDefaultLocale);
			// Create the request after changing the default locale.
			MockHttpServletRequest request = new MockHttpServletRequest();
			assertFalse(newDefaultLocale.equals(request.getLocale()));
			assertEquals(Locale.ENGLISH, request.getLocale());
		}
		finally {
			Locale.setDefault(originalDefaultLocale);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void setPreferredLocalesWithNullList() {
		request.setPreferredLocales(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void setPreferredLocalesWithEmptyList() {
		request.setPreferredLocales(new ArrayList<>());
	}

	@Test
	public void setPreferredLocales() {
		List<Locale> preferredLocales = Arrays.asList(Locale.ITALY, Locale.CHINA);
		request.setPreferredLocales(preferredLocales);
		assertEqualEnumerations(Collections.enumeration(preferredLocales), request.getLocales());
	}

	@Test
	public void getServerNameWithDefaultName() {
		assertEquals("localhost", request.getServerName());
	}

	@Test
	public void getServerNameWithCustomName() {
		request.setServerName("example.com");
		assertEquals("example.com", request.getServerName());
	}

	@Test
	public void getServerNameViaHostHeaderWithoutPort() {
		String testServer = "test.server";
		request.addHeader(HOST, testServer);
		assertEquals(testServer, request.getServerName());
	}

	@Test
	public void getServerNameViaHostHeaderWithPort() {
		String testServer = "test.server";
		request.addHeader(HOST, testServer + ":8080");
		assertEquals(testServer, request.getServerName());
	}

	@Test
	public void getServerNameViaHostHeaderAsIpv6AddressWithoutPort() {
		String ipv6Address = "[2001:db8:0:1]";
		request.addHeader(HOST, ipv6Address);
		assertEquals("2001:db8:0:1", request.getServerName());
	}

	@Test
	public void getServerNameViaHostHeaderAsIpv6AddressWithPort() {
		String ipv6Address = "[2001:db8:0:1]:8081";
		request.addHeader(HOST, ipv6Address);
		assertEquals("2001:db8:0:1", request.getServerName());
	}

	@Test
	public void getServerPortWithDefaultPort() {
		assertEquals(80, request.getServerPort());
	}

	@Test
	public void getServerPortWithCustomPort() {
		request.setServerPort(8080);
		assertEquals(8080, request.getServerPort());
	}

	@Test
	public void getServerPortViaHostHeaderAsIpv6AddressWithoutPort() {
		String testServer = "[2001:db8:0:1]";
		request.addHeader(HOST, testServer);
		assertEquals(80, request.getServerPort());
	}

	@Test
	public void getServerPortViaHostHeaderAsIpv6AddressWithPort() {
		String testServer = "[2001:db8:0:1]";
		int testPort = 9999;
		request.addHeader(HOST, testServer + ":" + testPort);
		assertEquals(testPort, request.getServerPort());
	}

	@Test
	public void getServerPortViaHostHeaderWithoutPort() {
		String testServer = "test.server";
		request.addHeader(HOST, testServer);
		assertEquals(80, request.getServerPort());
	}

	@Test
	public void getServerPortViaHostHeaderWithPort() {
		String testServer = "test.server";
		int testPort = 9999;
		request.addHeader(HOST, testServer + ":" + testPort);
		assertEquals(testPort, request.getServerPort());
	}

	@Test
	public void getRequestURL() {
		request.setServerPort(8080);
		request.setRequestURI("/path");
		assertEquals("http://localhost:8080/path", request.getRequestURL().toString());

		request.setScheme("https");
		request.setServerName("example.com");
		request.setServerPort(8443);
		assertEquals("https://example.com:8443/path", request.getRequestURL().toString());
	}

	@Test
	public void getRequestURLWithDefaults() {
		StringBuffer requestURL = request.getRequestURL();
		assertEquals("http://localhost", requestURL.toString());
	}

	@Test
	public void getRequestURLWithNullRequestUri() {
		request.setRequestURI(null);
		StringBuffer requestURL = request.getRequestURL();
		assertEquals("http://localhost", requestURL.toString());
	}

	@Test
	public void getRequestURLWithDefaultsAndHttps() {
		request.setScheme("https");
		request.setServerPort(443);
		StringBuffer requestURL = request.getRequestURL();
		assertEquals("https://localhost", requestURL.toString());
	}

	@Test
	public void getRequestURLWithNegativePort() {
		request.setServerPort(-99);
		StringBuffer requestURL = request.getRequestURL();
		assertEquals("http://localhost", requestURL.toString());
	}

	@Test
	public void isSecureWithHttpSchemeAndSecureFlagIsFalse() {
		assertFalse(request.isSecure());
		request.setScheme("http");
		request.setSecure(false);
		assertFalse(request.isSecure());
	}

	@Test
	public void isSecureWithHttpSchemeAndSecureFlagIsTrue() {
		assertFalse(request.isSecure());
		request.setScheme("http");
		request.setSecure(true);
		assertTrue(request.isSecure());
	}

	@Test
	public void isSecureWithHttpsSchemeAndSecureFlagIsFalse() {
		assertFalse(request.isSecure());
		request.setScheme("https");
		request.setSecure(false);
		assertTrue(request.isSecure());
	}

	@Test
	public void isSecureWithHttpsSchemeAndSecureFlagIsTrue() {
		assertFalse(request.isSecure());
		request.setScheme("https");
		request.setSecure(true);
		assertTrue(request.isSecure());
	}

	@Test
	public void httpHeaderDate() throws Exception {
		Date date = new Date();
		request.addHeader(IF_MODIFIED_SINCE, date);
		assertEquals(date.getTime(), request.getDateHeader(IF_MODIFIED_SINCE));
	}

	@Test
	public void httpHeaderTimestamp() throws Exception {
		long timestamp = new Date().getTime();
		request.addHeader(IF_MODIFIED_SINCE, timestamp);
		assertEquals(timestamp, request.getDateHeader(IF_MODIFIED_SINCE));
	}

	@Test
	public void httpHeaderRfcFormatedDate() throws Exception {
		request.addHeader(IF_MODIFIED_SINCE, "Tue, 21 Jul 2015 10:00:00 GMT");
		assertEquals(1437472800000L, request.getDateHeader(IF_MODIFIED_SINCE));
	}

	@Test
	public void httpHeaderFirstVariantFormatedDate() throws Exception {
		request.addHeader(IF_MODIFIED_SINCE, "Tue, 21-Jul-15 10:00:00 GMT");
		assertEquals(1437472800000L, request.getDateHeader(IF_MODIFIED_SINCE));
	}

	@Test
	public void httpHeaderSecondVariantFormatedDate() throws Exception {
		request.addHeader(IF_MODIFIED_SINCE, "Tue Jul 21 10:00:00 2015");
		assertEquals(1437472800000L, request.getDateHeader(IF_MODIFIED_SINCE));
	}

	@Test(expected = IllegalArgumentException.class)
	public void httpHeaderFormatedDateError() throws Exception {
		request.addHeader(IF_MODIFIED_SINCE, "This is not a date");
		request.getDateHeader(IF_MODIFIED_SINCE);
	}

	private void assertEqualEnumerations(Enumeration<?> enum1, Enumeration<?> enum2) {
		assertNotNull(enum1);
		assertNotNull(enum2);
		int count = 0;
		while (enum1.hasMoreElements()) {
			assertTrue("enumerations must be equal in length", enum2.hasMoreElements());
			assertEquals("enumeration element #" + ++count, enum1.nextElement(), enum2.nextElement());
		}
	}

}
