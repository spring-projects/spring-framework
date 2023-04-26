/*
 * Copyright 2002-2023 the original author or authors.
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
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.http.HttpHeaders.CONTENT_LANGUAGE;
import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LAST_MODIFIED;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

/**
 * Unit tests for {@link MockHttpServletResponse}.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Sam Brannen
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @author Vedran Pavic
 * @since 19.02.2006
 */
class MockHttpServletResponseTests {

	private MockHttpServletResponse response = new MockHttpServletResponse();


	@ParameterizedTest  // gh-26488
	@ValueSource(strings = {
		CONTENT_TYPE,
		CONTENT_LENGTH,
		CONTENT_LANGUAGE,
		SET_COOKIE,
		"enigma"
	})
	void addHeaderWithNullValue(String headerName) {
		response.addHeader(headerName, null);
		assertThat(response.containsHeader(headerName)).isFalse();
	}

	@ParameterizedTest  // gh-26488
	@ValueSource(strings = {
		CONTENT_TYPE,
		CONTENT_LENGTH,
		CONTENT_LANGUAGE,
		SET_COOKIE,
		"enigma"
	})
	void setHeaderWithNullValue(String headerName) {
		response.setHeader(headerName, null);
		assertThat(response.containsHeader(headerName)).isFalse();
	}

	@Test  // gh-26493
	void setLocaleWithNullValue() {
		assertThat(response.getLocale()).isEqualTo(Locale.getDefault());
		response.setLocale(null);
		assertThat(response.getLocale()).isEqualTo(Locale.getDefault());
	}

	@Test
	void setContentType() {
		String contentType = "test/plain";
		response.setContentType(contentType);
		assertThat(response.getContentType()).isEqualTo(contentType);
		assertThat(response.getHeader(CONTENT_TYPE)).isEqualTo(contentType);
		assertThat(response.getCharacterEncoding()).isEqualTo(WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	@Test
	void setContentTypeUTF8() {
		String contentType = "test/plain;charset=UTF-8";
		response.setContentType(contentType);
		assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
		assertThat(response.getContentType()).isEqualTo(contentType);
		assertThat(response.getHeader(CONTENT_TYPE)).isEqualTo(contentType);
	}

	@Test
	void contentTypeHeader() {
		String contentType = "test/plain";
		response.setHeader(CONTENT_TYPE, contentType);
		assertThat(response.getContentType()).isEqualTo(contentType);
		assertThat(response.getHeader(CONTENT_TYPE)).isEqualTo(contentType);
		assertThat(response.getCharacterEncoding()).isEqualTo(WebUtils.DEFAULT_CHARACTER_ENCODING);

		response = new MockHttpServletResponse();
		response.addHeader(CONTENT_TYPE, contentType);
		assertThat(response.getContentType()).isEqualTo(contentType);
		assertThat(response.getHeader(CONTENT_TYPE)).isEqualTo(contentType);
		assertThat(response.getCharacterEncoding()).isEqualTo(WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	@Test
	void contentTypeHeaderUTF8() {
		String contentType = "test/plain;charset=UTF-8";
		response.setHeader(CONTENT_TYPE, contentType);
		assertThat(response.getContentType()).isEqualTo(contentType);
		assertThat(response.getHeader(CONTENT_TYPE)).isEqualTo(contentType);
		assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");

		response = new MockHttpServletResponse();
		response.addHeader(CONTENT_TYPE, contentType);
		assertThat(response.getContentType()).isEqualTo(contentType);
		assertThat(response.getHeader(CONTENT_TYPE)).isEqualTo(contentType);
		assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
	}

	@Test  // SPR-12677
	void contentTypeHeaderWithMoreComplexCharsetSyntax() {
		String contentType = "test/plain;charset=\"utf-8\";foo=\"charset=bar\";foocharset=bar;foo=bar";
		response.setHeader(CONTENT_TYPE, contentType);
		assertThat(response.getContentType()).isEqualTo(contentType);
		assertThat(response.getHeader(CONTENT_TYPE)).isEqualTo(contentType);
		assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");

		response = new MockHttpServletResponse();
		response.addHeader(CONTENT_TYPE, contentType);
		assertThat(response.getContentType()).isEqualTo(contentType);
		assertThat(response.getHeader(CONTENT_TYPE)).isEqualTo(contentType);
		assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
	}

	@Test  // gh-25281
	void contentLanguageHeaderWithSingleValue() {
		String contentLanguage = "it";
		response.setHeader(CONTENT_LANGUAGE, contentLanguage);
		assertSoftly(softly -> {
			softly.assertThat(response.getHeader(CONTENT_LANGUAGE)).isEqualTo(contentLanguage);
			softly.assertThat(response.getLocale()).isEqualTo(Locale.ITALIAN);
		});
	}

	@Test  // gh-25281
	void contentLanguageHeaderWithMultipleValues() {
		String contentLanguage = "it, en";
		response.setHeader(CONTENT_LANGUAGE, contentLanguage);
		assertSoftly(softly -> {
			softly.assertThat(response.getHeader(CONTENT_LANGUAGE)).isEqualTo(contentLanguage);
			softly.assertThat(response.getLocale()).isEqualTo(Locale.ITALIAN);
		});
	}

	@Test
	void setContentTypeThenCharacterEncoding() {
		response.setContentType("test/plain");
		response.setCharacterEncoding("UTF-8");
		assertThat(response.getContentType()).isEqualTo("test/plain;charset=UTF-8");
		assertThat(response.getHeader(CONTENT_TYPE)).isEqualTo("test/plain;charset=UTF-8");
		assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
	}

	@Test
	void setCharacterEncodingThenContentType() {
		response.setCharacterEncoding("UTF-8");
		response.setContentType("test/plain");
		assertThat(response.getContentType()).isEqualTo("test/plain;charset=UTF-8");
		assertThat(response.getHeader(CONTENT_TYPE)).isEqualTo("test/plain;charset=UTF-8");
		assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
	}

	@Test
	void setCharacterEncodingNull() {
		response.setContentType("test/plain");
		response.setCharacterEncoding("UTF-8");
		assertThat(response.getContentType()).isEqualTo("test/plain;charset=UTF-8");
		assertThat(response.getHeader(CONTENT_TYPE)).isEqualTo("test/plain;charset=UTF-8");
		response.setCharacterEncoding(null);
		assertThat(response.getContentType()).isEqualTo("test/plain");
		assertThat(response.getHeader(CONTENT_TYPE)).isEqualTo("test/plain");
		assertThat(response.getCharacterEncoding()).isEqualTo(WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	@Test
	void defaultCharacterEncoding() {
		assertThat(response.isCharset()).isFalse();
		assertThat(response.getContentType()).isNull();
		assertThat(response.getCharacterEncoding()).isEqualTo("ISO-8859-1");

		response.setDefaultCharacterEncoding("UTF-8");
		assertThat(response.isCharset()).isFalse();
		assertThat(response.getContentType()).isNull();
		assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");

		response.setContentType("text/plain;charset=UTF-16");
		assertThat(response.isCharset()).isTrue();
		assertThat(response.getContentType()).isEqualTo("text/plain;charset=UTF-16");
		assertThat(response.getCharacterEncoding()).isEqualTo("UTF-16");

		response.reset();
		assertThat(response.isCharset()).isFalse();
		assertThat(response.getContentType()).isNull();
		assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");

		response.setCharacterEncoding("FOXTROT");
		assertThat(response.isCharset()).isTrue();
		assertThat(response.getContentType()).isNull();
		assertThat(response.getCharacterEncoding()).isEqualTo("FOXTROT");

		response.setDefaultCharacterEncoding("ENIGMA");
		assertThat(response.getCharacterEncoding()).isEqualTo("FOXTROT");
	}

	@Test
	void contentLength() {
		response.setContentLength(66);
		assertThat(response.getContentLength()).isEqualTo(66);
		assertThat(response.getHeader(CONTENT_LENGTH)).isEqualTo("66");
	}

	@Test
	void contentLengthHeader() {
		response.addHeader(CONTENT_LENGTH, "66");
		assertThat(response.getContentLength()).isEqualTo(66);
		assertThat(response.getHeader(CONTENT_LENGTH)).isEqualTo("66");
	}

	@Test
	void contentLengthIntHeader() {
		response.addIntHeader(CONTENT_LENGTH, 66);
		assertThat(response.getContentLength()).isEqualTo(66);
		assertThat(response.getHeader(CONTENT_LENGTH)).isEqualTo("66");
	}

	@Test
	void httpHeaderNameCasingIsPreserved() throws Exception {
		final String headerName = "Header1";
		response.addHeader(headerName, "value1");
		Collection<String> responseHeaders = response.getHeaderNames();
		assertThat(responseHeaders).isNotNull();
		assertThat(responseHeaders).hasSize(1);
		assertThat(responseHeaders.iterator().next()).as("HTTP header casing not being preserved").isEqualTo(headerName);
	}

	@Test
	void cookies() {
		Cookie cookie = new MockCookie("foo", "bar");
		cookie.setPath("/path");
		cookie.setDomain("example.com");
		cookie.setMaxAge(0);
		cookie.setSecure(true);
		cookie.setHttpOnly(true);

		response.addCookie(cookie);

		assertThat(response.getHeader(SET_COOKIE)).isEqualTo(("foo=bar; Path=/path; Domain=example.com; " +
				"Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; " +
				"Secure; HttpOnly"));
	}

	@Test
	void servletOutputStreamCommittedWhenBufferSizeExceeded() throws IOException {
		assertThat(response.isCommitted()).isFalse();
		response.getOutputStream().write('X');
		assertThat(response.isCommitted()).isFalse();
		int size = response.getBufferSize();
		response.getOutputStream().write(new byte[size]);
		assertThat(response.isCommitted()).isTrue();
		assertThat(response.getContentAsByteArray()).hasSize((size + 1));
	}

	@Test
	void servletOutputStreamCommittedOnFlushBuffer() throws IOException {
		assertThat(response.isCommitted()).isFalse();
		response.getOutputStream().write('X');
		assertThat(response.isCommitted()).isFalse();
		response.flushBuffer();
		assertThat(response.isCommitted()).isTrue();
		assertThat(response.getContentAsByteArray()).hasSize(1);
	}

	@Test
	void servletWriterCommittedWhenBufferSizeExceeded() throws IOException {
		assertThat(response.isCommitted()).isFalse();
		response.getWriter().write("X");
		assertThat(response.isCommitted()).isFalse();
		int size = response.getBufferSize();
		char[] data = new char[size];
		Arrays.fill(data, 'p');
		response.getWriter().write(data);
		assertThat(response.isCommitted()).isTrue();
		assertThat(response.getContentAsByteArray()).hasSize((size + 1));
	}

	@Test
	void servletOutputStreamCommittedOnOutputStreamFlush() throws IOException {
		assertThat(response.isCommitted()).isFalse();
		response.getOutputStream().write('X');
		assertThat(response.isCommitted()).isFalse();
		response.getOutputStream().flush();
		assertThat(response.isCommitted()).isTrue();
		assertThat(response.getContentAsByteArray()).hasSize(1);
	}

	@Test
	void servletWriterCommittedOnWriterFlush() throws IOException {
		assertThat(response.isCommitted()).isFalse();
		response.getWriter().write("X");
		assertThat(response.isCommitted()).isFalse();
		response.getWriter().flush();
		assertThat(response.isCommitted()).isTrue();
		assertThat(response.getContentAsByteArray()).hasSize(1);
	}

	@Test // SPR-16683
	void servletWriterCommittedOnWriterClose() throws IOException {
		assertThat(response.isCommitted()).isFalse();
		response.getWriter().write("X");
		assertThat(response.isCommitted()).isFalse();
		response.getWriter().close();
		assertThat(response.isCommitted()).isTrue();
		assertThat(response.getContentAsByteArray()).hasSize(1);
	}

	@Test  // gh-23219
	void contentAsUtf8() throws IOException {
		String content = "Příliš žluťoučký kůň úpěl ďábelské ódy";
		response.getOutputStream().write(content.getBytes(StandardCharsets.UTF_8));
		assertThat(response.getContentAsString(StandardCharsets.UTF_8)).isEqualTo(content);
	}

	@Test
	void servletWriterAutoFlushedForChar() throws IOException {
		response.getWriter().write('X');
		assertThat(response.getContentAsString()).isEqualTo("X");
	}

	@Test
	void servletWriterAutoFlushedForCharArray() throws IOException {
		response.getWriter().write("XY".toCharArray());
		assertThat(response.getContentAsString()).isEqualTo("XY");
	}

	@Test
	void servletWriterAutoFlushedForString() throws IOException {
		response.getWriter().write("X");
		assertThat(response.getContentAsString()).isEqualTo("X");
	}

	@Test
	void sendRedirect() throws IOException {
		String redirectUrl = "/redirect";
		response.sendRedirect(redirectUrl);
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_MOVED_TEMPORARILY);
		assertThat(response.getHeader(LOCATION)).isEqualTo(redirectUrl);
		assertThat(response.getRedirectedUrl()).isEqualTo(redirectUrl);
		assertThat(response.isCommitted()).isTrue();
	}

	@Test
	void locationHeaderUpdatesGetRedirectedUrl() {
		String redirectUrl = "/redirect";
		response.setHeader(LOCATION, redirectUrl);
		assertThat(response.getRedirectedUrl()).isEqualTo(redirectUrl);
	}

	@Test
	void setDateHeader() {
		response.setDateHeader(LAST_MODIFIED, 1437472800000L);
		assertThat(response.getHeader(LAST_MODIFIED)).isEqualTo("Tue, 21 Jul 2015 10:00:00 GMT");
	}

	@Test
	void addDateHeader() {
		response.addDateHeader(LAST_MODIFIED, 1437472800000L);
		response.addDateHeader(LAST_MODIFIED, 1437472801000L);
		assertThat(response.getHeaders(LAST_MODIFIED).get(0)).isEqualTo("Tue, 21 Jul 2015 10:00:00 GMT");
		assertThat(response.getHeaders(LAST_MODIFIED).get(1)).isEqualTo("Tue, 21 Jul 2015 10:00:01 GMT");
	}

	@Test
	void getDateHeader() {
		long time = 1437472800000L;
		response.setDateHeader(LAST_MODIFIED, time);
		assertThat(response.getHeader(LAST_MODIFIED)).isEqualTo("Tue, 21 Jul 2015 10:00:00 GMT");
		assertThat(response.getDateHeader(LAST_MODIFIED)).isEqualTo(time);
	}

	@Test
	void getInvalidDateHeader() {
		response.setHeader(LAST_MODIFIED, "invalid");
		assertThat(response.getHeader(LAST_MODIFIED)).isEqualTo("invalid");
		assertThatIllegalArgumentException().isThrownBy(() -> response.getDateHeader(LAST_MODIFIED));
	}

	@Test  // SPR-16160
	void getNonExistentDateHeader() {
		assertThat(response.getHeader(LAST_MODIFIED)).isNull();
		assertThat(response.getDateHeader(LAST_MODIFIED)).isEqualTo(-1);
	}

	@Test  // SPR-10414
	void modifyStatusAfterSendError() throws IOException {
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
		response.setStatus(HttpServletResponse.SC_OK);
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
	}

	@Test  // SPR-10414
	void modifyStatusMessageAfterSendError() throws IOException {
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
	}

	/**
	 * @since 5.1.10
	 */
	@Test
	void setCookieHeader() {
		response.setHeader(SET_COOKIE, "SESSION=123; Path=/; Secure; HttpOnly; SameSite=Lax");
		assertNumCookies(1);
		assertPrimarySessionCookie("123");

		// Setting the Set-Cookie header a 2nd time should overwrite the previous value
		response.setHeader(SET_COOKIE, "SESSION=999; Path=/; Secure; HttpOnly; SameSite=Lax");
		assertNumCookies(1);
		assertPrimarySessionCookie("999");
	}

	/**
	 * @since 5.1.11
	 */
	@Test
	void setCookieHeaderWithMaxAgeAndExpiresAttributes() {
		String expiryDate = "Tue, 8 Oct 2019 19:50:00 GMT";
		String cookieValue = "SESSION=123; Path=/; Max-Age=100; Expires=" + expiryDate + "; Secure; HttpOnly; SameSite=Lax";
		response.setHeader(SET_COOKIE, cookieValue);
		assertThat(response.getHeader(SET_COOKIE)).isEqualTo(cookieValue);

		assertNumCookies(1);
		assertThat(response.getCookies()[0]).isInstanceOf(MockCookie.class);
		MockCookie mockCookie = (MockCookie) response.getCookies()[0];
		assertThat(mockCookie.getMaxAge()).isEqualTo(100);
		assertThat(mockCookie.getExpires()).isEqualTo(ZonedDateTime.parse(expiryDate, DateTimeFormatter.RFC_1123_DATE_TIME));
	}

	/**
	 * @since 5.1.12
	 */
	@Test
	void setCookieHeaderWithZeroExpiresAttribute() {
		String cookieValue = "SESSION=123; Path=/; Max-Age=100; Expires=0";
		response.setHeader(SET_COOKIE, cookieValue);
		assertNumCookies(1);
		String header = response.getHeader(SET_COOKIE);
		assertThat(header).isNotEqualTo(cookieValue);
		// We don't assert the actual Expires value since it is based on the current time.
		assertThat(header).startsWith("SESSION=123; Path=/; Max-Age=100; Expires=");
	}

	@Test
	void addCookieHeader() {
		response.addHeader(SET_COOKIE, "SESSION=123; Path=/; Secure; HttpOnly; SameSite=Lax");
		assertNumCookies(1);
		assertPrimarySessionCookie("123");

		// Adding a 2nd cookie header should result in 2 cookies.
		response.addHeader(SET_COOKIE, "SESSION=999; Path=/; Secure; HttpOnly; SameSite=Lax");
		assertNumCookies(2);
		assertPrimarySessionCookie("123");
		assertCookieValues("123", "999");
	}

	/**
	 * @since 5.1.11
	 */
	@Test
	void addCookieHeaderWithMaxAgeAndExpiresAttributes() {
		String expiryDate = "Tue, 8 Oct 2019 19:50:00 GMT";
		String cookieValue = "SESSION=123; Path=/; Max-Age=100; Expires=" + expiryDate + "; Secure; HttpOnly; SameSite=Lax";
		response.addHeader(SET_COOKIE, cookieValue);
		assertThat(response.getHeader(SET_COOKIE)).isEqualTo(cookieValue);

		assertNumCookies(1);
		assertThat(response.getCookies()[0]).isInstanceOf(MockCookie.class);
		MockCookie mockCookie = (MockCookie) response.getCookies()[0];
		assertThat(mockCookie.getMaxAge()).isEqualTo(100);
		assertThat(mockCookie.getExpires()).isEqualTo(ZonedDateTime.parse(expiryDate, DateTimeFormatter.RFC_1123_DATE_TIME));
	}

	/**
	 * @since 5.1.12
	 */
	@Test
	void addCookieHeaderWithMaxAgeAndZeroExpiresAttributes() {
		String cookieValue = "SESSION=123; Path=/; Max-Age=100; Expires=0";
		response.addHeader(SET_COOKIE, cookieValue);
		assertNumCookies(1);
		String header = response.getHeader(SET_COOKIE);
		assertThat(header).isNotEqualTo(cookieValue);
		// We don't assert the actual Expires value since it is based on the current time.
		assertThat(header).startsWith("SESSION=123; Path=/; Max-Age=100; Expires=");
	}

	/**
	 * @since 5.2.14
	 */
	@Test
	void addCookieHeaderWithExpiresAttributeWithoutMaxAgeAttribute() {
		String expiryDate = "Tue, 8 Oct 2019 19:50:00 GMT";
		String cookieValue = "SESSION=123; Path=/; Expires=" + expiryDate;
		response.addHeader(SET_COOKIE, cookieValue);
		assertThat(response.getHeader(SET_COOKIE)).isEqualTo(cookieValue);

		assertNumCookies(1);
		assertThat(response.getCookies()[0]).isInstanceOf(MockCookie.class);
		MockCookie mockCookie = (MockCookie) response.getCookies()[0];
		assertThat(mockCookie.getName()).isEqualTo("SESSION");
		assertThat(mockCookie.getValue()).isEqualTo("123");
		assertThat(mockCookie.getPath()).isEqualTo("/");
		assertThat(mockCookie.getMaxAge()).isEqualTo(-1);
		assertThat(mockCookie.getExpires()).isEqualTo(ZonedDateTime.parse(expiryDate, DateTimeFormatter.RFC_1123_DATE_TIME));
	}

	@Test
	void addCookie() {
		MockCookie mockCookie = new MockCookie("SESSION", "123");
		mockCookie.setPath("/");
		mockCookie.setDomain("example.com");
		mockCookie.setMaxAge(0);
		mockCookie.setSecure(true);
		mockCookie.setHttpOnly(true);
		mockCookie.setSameSite("Lax");

		response.addCookie(mockCookie);

		assertNumCookies(1);
		assertThat(response.getHeader(SET_COOKIE)).isEqualTo(("SESSION=123; Path=/; Domain=example.com; Max-Age=0; " +
				"Expires=Thu, 01 Jan 1970 00:00:00 GMT; Secure; HttpOnly; SameSite=Lax"));

		// Adding a 2nd Cookie should result in 2 Cookies.
		response.addCookie(new MockCookie("SESSION", "999"));
		assertNumCookies(2);
		assertCookieValues("123", "999");
	}

	private void assertNumCookies(int expected) {
		assertThat(this.response.getCookies()).hasSize(expected);
	}

	private void assertCookieValues(String... expected) {
		assertThat(response.getCookies()).extracting(Cookie::getValue).containsExactly(expected);
	}

	@SuppressWarnings("removal")
	private void assertPrimarySessionCookie(String expectedValue) {
		Cookie cookie = this.response.getCookie("SESSION");
		assertThat(cookie).asInstanceOf(type(MockCookie.class)).satisfies(mockCookie -> {
			assertThat(mockCookie.getName()).isEqualTo("SESSION");
			assertThat(mockCookie.getValue()).isEqualTo(expectedValue);
			assertThat(mockCookie.getPath()).isEqualTo("/");
			assertThat(mockCookie.getSecure()).isTrue();
			assertThat(mockCookie.isHttpOnly()).isTrue();
			assertThat(mockCookie.getComment()).isNull();
			assertThat(mockCookie.getExpires()).isNull();
			assertThat(mockCookie.getSameSite()).isEqualTo("Lax");
		});
	}

	@Test  // gh-25501
	void resetResetsCharset() {
		assertThat(response.getContentType()).isNull();
		assertThat(response.getCharacterEncoding()).isEqualTo("ISO-8859-1");
		assertThat(response.isCharset()).isFalse();
		response.setCharacterEncoding("UTF-8");
		assertThat(response.isCharset()).isTrue();
		assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
		response.setContentType("text/plain");
		assertThat(response.getContentType()).isEqualTo("text/plain;charset=UTF-8");
		String contentTypeHeader = response.getHeader(CONTENT_TYPE);
		assertThat(contentTypeHeader).isEqualTo("text/plain;charset=UTF-8");

		response.reset();

		assertThat(response.getContentType()).isNull();
		assertThat(response.getCharacterEncoding()).isEqualTo("ISO-8859-1");
		assertThat(response.isCharset()).isFalse();
		// Do not invoke setCharacterEncoding() since that sets the charset flag to true.
		// response.setCharacterEncoding("UTF-8");
		response.setContentType("text/plain");
		assertThat(response.isCharset()).isFalse(); // should still be false
		assertThat(response.getContentType()).isEqualTo("text/plain");
		contentTypeHeader = response.getHeader(CONTENT_TYPE);
		assertThat(contentTypeHeader).isEqualTo("text/plain");
	}

}
