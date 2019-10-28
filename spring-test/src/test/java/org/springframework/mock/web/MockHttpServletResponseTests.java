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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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
 * @since 19.02.2006
 */
class MockHttpServletResponseTests {

	private MockHttpServletResponse response = new MockHttpServletResponse();


	@Test
	void setContentType() {
		String contentType = "test/plain";
		response.setContentType(contentType);
		assertThat(response.getContentType()).isEqualTo(contentType);
		assertThat(response.getHeader("Content-Type")).isEqualTo(contentType);
		assertThat(response.getCharacterEncoding()).isEqualTo(WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	@Test
	void setContentTypeUTF8() {
		String contentType = "test/plain;charset=UTF-8";
		response.setContentType(contentType);
		assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
		assertThat(response.getContentType()).isEqualTo(contentType);
		assertThat(response.getHeader("Content-Type")).isEqualTo(contentType);
	}

	@Test
	void contentTypeHeader() {
		String contentType = "test/plain";
		response.addHeader("Content-Type", contentType);
		assertThat(response.getContentType()).isEqualTo(contentType);
		assertThat(response.getHeader("Content-Type")).isEqualTo(contentType);
		assertThat(response.getCharacterEncoding()).isEqualTo(WebUtils.DEFAULT_CHARACTER_ENCODING);

		response = new MockHttpServletResponse();
		response.setHeader("Content-Type", contentType);
		assertThat(response.getContentType()).isEqualTo(contentType);
		assertThat(response.getHeader("Content-Type")).isEqualTo(contentType);
		assertThat(response.getCharacterEncoding()).isEqualTo(WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	@Test
	void contentTypeHeaderUTF8() {
		String contentType = "test/plain;charset=UTF-8";
		response.setHeader("Content-Type", contentType);
		assertThat(response.getContentType()).isEqualTo(contentType);
		assertThat(response.getHeader("Content-Type")).isEqualTo(contentType);
		assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");

		response = new MockHttpServletResponse();
		response.addHeader("Content-Type", contentType);
		assertThat(response.getContentType()).isEqualTo(contentType);
		assertThat(response.getHeader("Content-Type")).isEqualTo(contentType);
		assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
	}

	@Test  // SPR-12677
	void contentTypeHeaderWithMoreComplexCharsetSyntax() {
		String contentType = "test/plain;charset=\"utf-8\";foo=\"charset=bar\";foocharset=bar;foo=bar";
		response.setHeader("Content-Type", contentType);
		assertThat(response.getContentType()).isEqualTo(contentType);
		assertThat(response.getHeader("Content-Type")).isEqualTo(contentType);
		assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");

		response = new MockHttpServletResponse();
		response.addHeader("Content-Type", contentType);
		assertThat(response.getContentType()).isEqualTo(contentType);
		assertThat(response.getHeader("Content-Type")).isEqualTo(contentType);
		assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
	}

	@Test
	void setContentTypeThenCharacterEncoding() {
		response.setContentType("test/plain");
		response.setCharacterEncoding("UTF-8");
		assertThat(response.getContentType()).isEqualTo("test/plain");
		assertThat(response.getHeader("Content-Type")).isEqualTo("test/plain;charset=UTF-8");
		assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
	}

	@Test
	void setCharacterEncodingThenContentType() {
		response.setCharacterEncoding("UTF-8");
		response.setContentType("test/plain");
		assertThat(response.getContentType()).isEqualTo("test/plain");
		assertThat(response.getHeader("Content-Type")).isEqualTo("test/plain;charset=UTF-8");
		assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
	}

	@Test
	void contentLength() {
		response.setContentLength(66);
		assertThat(response.getContentLength()).isEqualTo(66);
		assertThat(response.getHeader("Content-Length")).isEqualTo("66");
	}

	@Test
	void contentLengthHeader() {
		response.addHeader("Content-Length", "66");
		assertThat(response.getContentLength()).isEqualTo(66);
		assertThat(response.getHeader("Content-Length")).isEqualTo("66");
	}

	@Test
	void contentLengthIntHeader() {
		response.addIntHeader("Content-Length", 66);
		assertThat(response.getContentLength()).isEqualTo(66);
		assertThat(response.getHeader("Content-Length")).isEqualTo("66");
	}

	@Test
	void httpHeaderNameCasingIsPreserved() throws Exception {
		final String headerName = "Header1";
		response.addHeader(headerName, "value1");
		Collection<String> responseHeaders = response.getHeaderNames();
		assertThat(responseHeaders).isNotNull();
		assertThat(responseHeaders.size()).isEqualTo(1);
		assertThat(responseHeaders.iterator().next()).as("HTTP header casing not being preserved").isEqualTo(headerName);
	}

	@Test
	void cookies() {
		Cookie cookie = new Cookie("foo", "bar");
		cookie.setPath("/path");
		cookie.setDomain("example.com");
		cookie.setMaxAge(0);
		cookie.setSecure(true);
		cookie.setHttpOnly(true);

		response.addCookie(cookie);

		assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isEqualTo(("foo=bar; Path=/path; Domain=example.com; " +
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
		assertThat(response.getContentAsByteArray().length).isEqualTo((size + 1));
	}

	@Test
	void servletOutputStreamCommittedOnFlushBuffer() throws IOException {
		assertThat(response.isCommitted()).isFalse();
		response.getOutputStream().write('X');
		assertThat(response.isCommitted()).isFalse();
		response.flushBuffer();
		assertThat(response.isCommitted()).isTrue();
		assertThat(response.getContentAsByteArray().length).isEqualTo(1);
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
		assertThat(response.getContentAsByteArray().length).isEqualTo((size + 1));
	}

	@Test
	void servletOutputStreamCommittedOnOutputStreamFlush() throws IOException {
		assertThat(response.isCommitted()).isFalse();
		response.getOutputStream().write('X');
		assertThat(response.isCommitted()).isFalse();
		response.getOutputStream().flush();
		assertThat(response.isCommitted()).isTrue();
		assertThat(response.getContentAsByteArray().length).isEqualTo(1);
	}

	@Test
	void servletWriterCommittedOnWriterFlush() throws IOException {
		assertThat(response.isCommitted()).isFalse();
		response.getWriter().write("X");
		assertThat(response.isCommitted()).isFalse();
		response.getWriter().flush();
		assertThat(response.isCommitted()).isTrue();
		assertThat(response.getContentAsByteArray().length).isEqualTo(1);
	}

	@Test // SPR-16683
	void servletWriterCommittedOnWriterClose() throws IOException {
		assertThat(response.isCommitted()).isFalse();
		response.getWriter().write("X");
		assertThat(response.isCommitted()).isFalse();
		response.getWriter().close();
		assertThat(response.isCommitted()).isTrue();
		assertThat(response.getContentAsByteArray().length).isEqualTo(1);
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
		assertThat(response.getHeader("Location")).isEqualTo(redirectUrl);
		assertThat(response.getRedirectedUrl()).isEqualTo(redirectUrl);
		assertThat(response.isCommitted()).isTrue();
	}

	@Test
	void locationHeaderUpdatesGetRedirectedUrl() {
		String redirectUrl = "/redirect";
		response.setHeader("Location", redirectUrl);
		assertThat(response.getRedirectedUrl()).isEqualTo(redirectUrl);
	}

	@Test
	void setDateHeader() {
		response.setDateHeader("Last-Modified", 1437472800000L);
		assertThat(response.getHeader("Last-Modified")).isEqualTo("Tue, 21 Jul 2015 10:00:00 GMT");
	}

	@Test
	void addDateHeader() {
		response.addDateHeader("Last-Modified", 1437472800000L);
		response.addDateHeader("Last-Modified", 1437472801000L);
		assertThat(response.getHeaders("Last-Modified").get(0)).isEqualTo("Tue, 21 Jul 2015 10:00:00 GMT");
		assertThat(response.getHeaders("Last-Modified").get(1)).isEqualTo("Tue, 21 Jul 2015 10:00:01 GMT");
	}

	@Test
	void getDateHeader() {
		long time = 1437472800000L;
		response.setDateHeader("Last-Modified", time);
		assertThat(response.getHeader("Last-Modified")).isEqualTo("Tue, 21 Jul 2015 10:00:00 GMT");
		assertThat(response.getDateHeader("Last-Modified")).isEqualTo(time);
	}

	@Test
	void getInvalidDateHeader() {
		response.setHeader("Last-Modified", "invalid");
		assertThat(response.getHeader("Last-Modified")).isEqualTo("invalid");
		assertThatIllegalArgumentException().isThrownBy(() ->
				response.getDateHeader("Last-Modified"));
	}

	@Test  // SPR-16160
	void getNonExistentDateHeader() {
		assertThat(response.getHeader("Last-Modified")).isNull();
		assertThat(response.getDateHeader("Last-Modified")).isEqualTo(-1);
	}

	@Test  // SPR-10414
	void modifyStatusAfterSendError() throws IOException {
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
		response.setStatus(HttpServletResponse.SC_OK);
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
	}

	@Test  // SPR-10414
	@SuppressWarnings("deprecation")
	void modifyStatusMessageAfterSendError() throws IOException {
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Server Error");
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
	}

	/**
	 * @since 5.1.10
	 */
	@Test
	void setCookieHeader() {
		response.setHeader(HttpHeaders.SET_COOKIE, "SESSION=123; Path=/; Secure; HttpOnly; SameSite=Lax");
		assertNumCookies(1);
		assertPrimarySessionCookie("123");

		// Setting the Set-Cookie header a 2nd time should overwrite the previous value
		response.setHeader(HttpHeaders.SET_COOKIE, "SESSION=999; Path=/; Secure; HttpOnly; SameSite=Lax");
		assertNumCookies(1);
		assertPrimarySessionCookie("999");
	}

	@Test
	void addCookieHeader() {
		response.addHeader(HttpHeaders.SET_COOKIE, "SESSION=123; Path=/; Secure; HttpOnly; SameSite=Lax");
		assertNumCookies(1);
		assertPrimarySessionCookie("123");

		// Adding a 2nd cookie header should result in 2 cookies.
		response.addHeader(HttpHeaders.SET_COOKIE, "SESSION=999; Path=/; Secure; HttpOnly; SameSite=Lax");
		assertNumCookies(2);
		assertPrimarySessionCookie("123");
		assertCookieValues("123", "999");
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
		assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isEqualTo(("SESSION=123; Path=/; Domain=example.com; Max-Age=0; " +
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

	private void assertPrimarySessionCookie(String expectedValue) {
		Cookie cookie = this.response.getCookie("SESSION");
		assertThat(cookie).isInstanceOf(MockCookie.class);
		assertThat(cookie.getName()).isEqualTo("SESSION");
		assertThat(cookie.getValue()).isEqualTo(expectedValue);
		assertThat(cookie.getPath()).isEqualTo("/");
		assertThat(cookie.getSecure()).isTrue();
		assertThat(cookie.isHttpOnly()).isTrue();
		assertThat(((MockCookie) cookie).getSameSite()).isEqualTo("Lax");
	}

}
