/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.FastByteArrayOutputStream;

/**
 * {@link jakarta.servlet.http.HttpServletResponse} wrapper that caches all content written to
 * the {@linkplain #getOutputStream() output stream} and {@linkplain #getWriter() writer},
 * and allows this content to be retrieved via a {@linkplain #getContentAsByteArray() byte array}.
 *
 * <p>Used e.g. by {@link org.springframework.web.filter.ShallowEtagHeaderFilter}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.1.3
 * @see ContentCachingRequestWrapper
 */
public class ContentCachingResponseWrapper extends HttpServletResponseWrapper {

	private final FastByteArrayOutputStream content = new FastByteArrayOutputStream(1024);

	@Nullable
	private ServletOutputStream outputStream;

	@Nullable
	private PrintWriter writer;

	@Nullable
	private Integer contentLength;


	/**
	 * Create a new ContentCachingResponseWrapper for the given servlet response.
	 * @param response the original servlet response
	 */
	public ContentCachingResponseWrapper(HttpServletResponse response) {
		super(response);
	}


	@Override
	public void sendError(int sc) throws IOException {
		copyBodyToResponse(false);
		try {
			super.sendError(sc);
		}
		catch (IllegalStateException ex) {
			// Possibly on Tomcat when called too late: fall back to silent setStatus
			super.setStatus(sc);
		}
	}

	@Override
	public void sendError(int sc, String msg) throws IOException {
		copyBodyToResponse(false);
		try {
			super.sendError(sc, msg);
		}
		catch (IllegalStateException ex) {
			// Possibly on Tomcat when called too late: fall back to silent setStatus
			super.setStatus(sc);
		}
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		copyBodyToResponse(false);
		super.sendRedirect(location);
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (this.outputStream == null) {
			this.outputStream = new ResponseServletOutputStream(getResponse().getOutputStream());
		}
		return this.outputStream;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (this.writer == null) {
			String characterEncoding = getCharacterEncoding();
			this.writer = (characterEncoding != null ? new ResponsePrintWriter(characterEncoding) :
					new ResponsePrintWriter(WebUtils.DEFAULT_CHARACTER_ENCODING));
		}
		return this.writer;
	}

	/**
	 * This method neither flushes content to the client nor commits the underlying
	 * response, since the content has not yet been copied to the response.
	 * <p>Invoke {@link #copyBodyToResponse()} to copy the cached body content to
	 * the wrapped response object and flush its buffer.
	 * @see jakarta.servlet.ServletResponseWrapper#flushBuffer()
	 */
	@Override
	public void flushBuffer() throws IOException {
		// no-op
	}

	@Override
	public void setContentLength(int len) {
		if (len > this.content.size()) {
			this.content.resize(len);
		}
		this.contentLength = len;
	}

	@Override
	public void setContentLengthLong(long len) {
		setContentLength(toContentLengthInt(len));
	}

	private int toContentLengthInt(long contentLength) {
		if (contentLength > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Content-Length exceeds ContentCachingResponseWrapper's maximum (" +
					Integer.MAX_VALUE + "): " + contentLength);
		}
		return (int) contentLength;
	}

	@Override
	public boolean containsHeader(String name) {
		if (this.contentLength != null && HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
			return true;
		}
		else {
			return super.containsHeader(name);
		}
	}

	@Override
	public void setHeader(String name, String value) {
		if (HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
			this.contentLength = toContentLengthInt(Long.parseLong(value));
		}
		else {
			super.setHeader(name, value);
		}
	}

	@Override
	public void addHeader(String name, String value) {
		if (HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
			this.contentLength = toContentLengthInt(Long.parseLong(value));
		}
		else {
			super.addHeader(name, value);
		}
	}

	@Override
	public void setIntHeader(String name, int value) {
		if (HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
			this.contentLength = value;
		}
		else {
			super.setIntHeader(name, value);
		}
	}

	@Override
	public void addIntHeader(String name, int value) {
		if (HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
			this.contentLength = value;
		}
		else {
			super.addIntHeader(name, value);
		}
	}

	@Override
	@Nullable
	public String getHeader(String name) {
		if (this.contentLength != null && HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
			return this.contentLength.toString();
		}
		else {
			return super.getHeader(name);
		}
	}

	@Override
	public Collection<String> getHeaders(String name) {
		if (this.contentLength != null && HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
			return Collections.singleton(this.contentLength.toString());
		}
		else {
			return super.getHeaders(name);
		}
	}

	@Override
	public Collection<String> getHeaderNames() {
		Collection<String> headerNames = super.getHeaderNames();
		if (this.contentLength != null) {
			Set<String> result = new LinkedHashSet<>(headerNames);
			result.add(HttpHeaders.CONTENT_LENGTH);
			return result;
		}
		else {
			return headerNames;
		}
	}

	@Override
	public void setBufferSize(int size) {
		if (size > this.content.size()) {
			this.content.resize(size);
		}
	}

	@Override
	public void resetBuffer() {
		this.content.reset();
	}

	@Override
	public void reset() {
		super.reset();
		this.content.reset();
	}

	/**
	 * Return the cached response content as a byte array.
	 */
	public byte[] getContentAsByteArray() {
		return this.content.toByteArray();
	}

	/**
	 * Return an {@link InputStream} to the cached content.
	 * @since 4.2
	 */
	public InputStream getContentInputStream() {
		return this.content.getInputStream();
	}

	/**
	 * Return the current size of the cached content.
	 * @since 4.2
	 */
	public int getContentSize() {
		return this.content.size();
	}

	/**
	 * Copy the complete cached body content to the response.
	 * @since 4.2
	 */
	public void copyBodyToResponse() throws IOException {
		copyBodyToResponse(true);
	}

	/**
	 * Copy the cached body content to the response.
	 * @param complete whether to set a corresponding content length
	 * for the complete cached body content
	 * @since 4.2
	 */
	protected void copyBodyToResponse(boolean complete) throws IOException {
		if (this.content.size() > 0) {
			HttpServletResponse rawResponse = (HttpServletResponse) getResponse();
			if (!rawResponse.isCommitted()) {
				if (complete || this.contentLength != null) {
					if (rawResponse.getHeader(HttpHeaders.TRANSFER_ENCODING) == null) {
						rawResponse.setContentLength(complete ? this.content.size() : this.contentLength);
					}
					this.contentLength = null;
				}
			}
			this.content.writeTo(rawResponse.getOutputStream());
			this.content.reset();
			if (complete) {
				super.flushBuffer();
			}
		}
	}


	private class ResponseServletOutputStream extends ServletOutputStream {

		private final ServletOutputStream os;

		public ResponseServletOutputStream(ServletOutputStream os) {
			this.os = os;
		}

		@Override
		public void write(int b) throws IOException {
			content.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			content.write(b, off, len);
		}

		@Override
		public boolean isReady() {
			return this.os.isReady();
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
			this.os.setWriteListener(writeListener);
		}
	}


	private class ResponsePrintWriter extends PrintWriter {

		public ResponsePrintWriter(String characterEncoding) throws UnsupportedEncodingException {
			super(new OutputStreamWriter(content, characterEncoding));
		}

		@Override
		public void write(char[] buf, int off, int len) {
			super.write(buf, off, len);
			super.flush();
		}

		@Override
		public void write(String s, int off, int len) {
			super.write(s, off, len);
			super.flush();
		}

		@Override
		public void write(int c) {
			super.write(c);
			super.flush();
		}
	}

}
