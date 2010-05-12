/*
 * Copyright 2002-2010 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.web.util.WebUtils;

/**
 * Mock implementation of the {@link javax.servlet.http.HttpServletResponse}
 * interface. Supports the Servlet 2.5 API level.
 *
 * <p>Used for testing the web framework; also useful for testing
 * application controllers.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 1.0.2
 */
public class MockHttpServletResponse implements HttpServletResponse {

	public static final int DEFAULT_SERVER_PORT = 80;

	private static final String CHARSET_PREFIX = "charset=";


	//---------------------------------------------------------------------
	// ServletResponse properties
	//---------------------------------------------------------------------

	private boolean outputStreamAccessAllowed = true;

	private boolean writerAccessAllowed = true;

	private String characterEncoding = WebUtils.DEFAULT_CHARACTER_ENCODING;

	private final ByteArrayOutputStream content = new ByteArrayOutputStream();

	private final ServletOutputStream outputStream = new ResponseServletOutputStream(this.content);

	private PrintWriter writer;

	private int contentLength = 0;

	private String contentType;

	private int bufferSize = 4096;

	private boolean committed;

	private Locale locale = Locale.getDefault();


	//---------------------------------------------------------------------
	// HttpServletResponse properties
	//---------------------------------------------------------------------

	private final List<Cookie> cookies = new ArrayList<Cookie>();

	private final Map<String, HeaderValueHolder> headers = new LinkedCaseInsensitiveMap<HeaderValueHolder>();

	private int status = HttpServletResponse.SC_OK;

	private String errorMessage;

	private String redirectedUrl;

	private String forwardedUrl;

	private final List<String> includedUrls = new ArrayList<String>();


	//---------------------------------------------------------------------
	// ServletResponse interface
	//---------------------------------------------------------------------

	/**
	 * Set whether {@link #getOutputStream()} access is allowed.
	 * <p>Default is <code>true</code>.
	 */
	public void setOutputStreamAccessAllowed(boolean outputStreamAccessAllowed) {
		this.outputStreamAccessAllowed = outputStreamAccessAllowed;
	}

	/**
	 * Return whether {@link #getOutputStream()} access is allowed.
	 */
	public boolean isOutputStreamAccessAllowed() {
		return this.outputStreamAccessAllowed;
	}

	/**
	 * Set whether {@link #getWriter()} access is allowed.
	 * <p>Default is <code>true</code>.
	 */
	public void setWriterAccessAllowed(boolean writerAccessAllowed) {
		this.writerAccessAllowed = writerAccessAllowed;
	}

	/**
	 * Return whether {@link #getOutputStream()} access is allowed.
	 */
	public boolean isWriterAccessAllowed() {
		return this.writerAccessAllowed;
	}

	public void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = characterEncoding;
	}

	public String getCharacterEncoding() {
		return this.characterEncoding;
	}

	public ServletOutputStream getOutputStream() {
		if (!this.outputStreamAccessAllowed) {
			throw new IllegalStateException("OutputStream access not allowed");
		}
		return this.outputStream;
	}

	public PrintWriter getWriter() throws UnsupportedEncodingException {
		if (!this.writerAccessAllowed) {
			throw new IllegalStateException("Writer access not allowed");
		}
		if (this.writer == null) {
			Writer targetWriter = (this.characterEncoding != null ?
					new OutputStreamWriter(this.content, this.characterEncoding) : new OutputStreamWriter(this.content));
			this.writer = new ResponsePrintWriter(targetWriter);
		}
		return this.writer;
	}

	public byte[] getContentAsByteArray() {
		flushBuffer();
		return this.content.toByteArray();
	}

	public String getContentAsString() throws UnsupportedEncodingException {
		flushBuffer();
		return (this.characterEncoding != null) ?
				this.content.toString(this.characterEncoding) : this.content.toString();
	}

	public void setContentLength(int contentLength) {
		this.contentLength = contentLength;
	}

	public int getContentLength() {
		return this.contentLength;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
		if (contentType != null) {
			int charsetIndex = contentType.toLowerCase().indexOf(CHARSET_PREFIX);
			if (charsetIndex != -1) {
				String encoding = contentType.substring(charsetIndex + CHARSET_PREFIX.length());
				setCharacterEncoding(encoding);
			}
		}
	}

	public String getContentType() {
		return this.contentType;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public int getBufferSize() {
		return this.bufferSize;
	}

	public void flushBuffer() {
		setCommitted(true);
	}

	public void resetBuffer() {
		if (isCommitted()) {
			throw new IllegalStateException("Cannot reset buffer - response is already committed");
		}
		this.content.reset();
	}

	private void setCommittedIfBufferSizeExceeded() {
		int bufSize = getBufferSize();
		if (bufSize > 0 && this.content.size() > bufSize) {
			setCommitted(true);
		}
	}

	public void setCommitted(boolean committed) {
		this.committed = committed;
	}

	public boolean isCommitted() {
		return this.committed;
	}

	public void reset() {
		resetBuffer();
		this.characterEncoding = null;
		this.contentLength = 0;
		this.contentType = null;
		this.locale = null;
		this.cookies.clear();
		this.headers.clear();
		this.status = HttpServletResponse.SC_OK;
		this.errorMessage = null;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public Locale getLocale() {
		return this.locale;
	}


	//---------------------------------------------------------------------
	// HttpServletResponse interface
	//---------------------------------------------------------------------

	public void addCookie(Cookie cookie) {
		Assert.notNull(cookie, "Cookie must not be null");
		this.cookies.add(cookie);
	}

	public Cookie[] getCookies() {
		return this.cookies.toArray(new Cookie[this.cookies.size()]);
	}

	public Cookie getCookie(String name) {
		Assert.notNull(name, "Cookie name must not be null");
		for (Cookie cookie : this.cookies) {
			if (name.equals(cookie.getName())) {
				return cookie;
			}
		}
		return null;
	}

	public boolean containsHeader(String name) {
		return (HeaderValueHolder.getByName(this.headers, name) != null);
	}

	/**
	 * Return the names of all specified headers as a Set of Strings.
	 * @return the <code>Set</code> of header name <code>Strings</code>, or an empty <code>Set</code> if none
	 */
	public Set<String> getHeaderNames() {
		return this.headers.keySet();
	}

	/**
	 * Return the primary value for the given header, if any.
	 * <p>Will return the first value in case of multiple values.
	 * @param name the name of the header
	 * @return the associated header value, or <code>null<code> if none
	 */
	public Object getHeader(String name) {
		HeaderValueHolder header = HeaderValueHolder.getByName(this.headers, name);
		return (header != null ? header.getValue() : null);
	}

	/**
	 * Return all values for the given header as a List of value objects.
	 * @param name the name of the header
	 * @return the associated header values, or an empty List if none
	 */
	public List<Object> getHeaders(String name) {
		HeaderValueHolder header = HeaderValueHolder.getByName(this.headers, name);
		return (header != null ? header.getValues() : Collections.emptyList());
	}

	/**
	 * The default implementation returns the given URL String as-is.
	 * <p>Can be overridden in subclasses, appending a session id or the like.
	 */
	public String encodeURL(String url) {
		return url;
	}

	/**
	 * The default implementation delegates to {@link #encodeURL},
	 * returning the given URL String as-is.
	 * <p>Can be overridden in subclasses, appending a session id or the like
	 * in a redirect-specific fashion. For general URL encoding rules,
	 * override the common {@link #encodeURL} method instead, appyling
	 * to redirect URLs as well as to general URLs.
	 */
	public String encodeRedirectURL(String url) {
		return encodeURL(url);
	}

	public String encodeUrl(String url) {
		return encodeURL(url);
	}

	public String encodeRedirectUrl(String url) {
		return encodeRedirectURL(url);
	}

	public void sendError(int status, String errorMessage) throws IOException {
		if (isCommitted()) {
			throw new IllegalStateException("Cannot set error status - response is already committed");
		}
		this.status = status;
		this.errorMessage = errorMessage;
		setCommitted(true);
	}

	public void sendError(int status) throws IOException {
		if (isCommitted()) {
			throw new IllegalStateException("Cannot set error status - response is already committed");
		}
		this.status = status;
		setCommitted(true);
	}

	public void sendRedirect(String url) throws IOException {
		if (isCommitted()) {
			throw new IllegalStateException("Cannot send redirect - response is already committed");
		}
		Assert.notNull(url, "Redirect URL must not be null");
		this.redirectedUrl = url;
		setCommitted(true);
	}

	public String getRedirectedUrl() {
		return this.redirectedUrl;
	}

	public void setDateHeader(String name, long value) {
		setHeaderValue(name, value);
	}

	public void addDateHeader(String name, long value) {
		addHeaderValue(name, value);
	}

	public void setHeader(String name, String value) {
		setHeaderValue(name, value);
	}

	public void addHeader(String name, String value) {
		addHeaderValue(name, value);
	}

	public void setIntHeader(String name, int value) {
		setHeaderValue(name, value);
	}

	public void addIntHeader(String name, int value) {
		addHeaderValue(name, value);
	}

	private void setHeaderValue(String name, Object value) {
		doAddHeaderValue(name, value, true);
	}

	private void addHeaderValue(String name, Object value) {
		doAddHeaderValue(name, value, false);
	}

	private void doAddHeaderValue(String name, Object value, boolean replace) {
		HeaderValueHolder header = HeaderValueHolder.getByName(this.headers, name);
		Assert.notNull(value, "Header value must not be null");
		if (header == null) {
			header = new HeaderValueHolder();
			this.headers.put(name, header);
		}
		if (replace) {
			header.setValue(value);
		}
		else {
			header.addValue(value);
		}
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public void setStatus(int status, String errorMessage) {
		this.status = status;
		this.errorMessage = errorMessage;
	}

	public int getStatus() {
		return this.status;
	}

	public String getErrorMessage() {
		return this.errorMessage;
	}


	//---------------------------------------------------------------------
	// Methods for MockRequestDispatcher
	//---------------------------------------------------------------------

	public void setForwardedUrl(String forwardedUrl) {
		this.forwardedUrl = forwardedUrl;
	}

	public String getForwardedUrl() {
		return this.forwardedUrl;
	}

	public void setIncludedUrl(String includedUrl) {
		this.includedUrls.clear();
		if (includedUrl != null) {
			this.includedUrls.add(includedUrl);
		}
	}

	public String getIncludedUrl() {
		int count = this.includedUrls.size();
		if (count > 1) {
			throw new IllegalStateException(
					"More than 1 URL included - check getIncludedUrls instead: " + this.includedUrls);
		}
		return (count == 1 ? this.includedUrls.get(0) : null);
	}

	public void addIncludedUrl(String includedUrl) {
		Assert.notNull(includedUrl, "Included URL must not be null");
		this.includedUrls.add(includedUrl);
	}

	public List<String> getIncludedUrls() {
		return this.includedUrls;
	}


	/**
	 * Inner class that adapts the ServletOutputStream to mark the
	 * response as committed once the buffer size is exceeded.
	 */
	private class ResponseServletOutputStream extends DelegatingServletOutputStream {

		public ResponseServletOutputStream(OutputStream out) {
			super(out);
		}

		public void write(int b) throws IOException {
			super.write(b);
			super.flush();
			setCommittedIfBufferSizeExceeded();
		}

		public void flush() throws IOException {
			super.flush();
			setCommitted(true);
		}
	}


	/**
	 * Inner class that adapts the PrintWriter to mark the
	 * response as committed once the buffer size is exceeded.
	 */
	private class ResponsePrintWriter extends PrintWriter {

		public ResponsePrintWriter(Writer out) {
			super(out, true);
		}

		public void write(char buf[], int off, int len) {
			super.write(buf, off, len);
			super.flush();
			setCommittedIfBufferSizeExceeded();
		}

		public void write(String s, int off, int len) {
			super.write(s, off, len);
			super.flush();
			setCommittedIfBufferSizeExceeded();
		}

		public void write(int c) {
			super.write(c);
			super.flush();
			setCommittedIfBufferSizeExceeded();
		}

		public void flush() {
			super.flush();
			setCommitted(true);
		}
	}

}
