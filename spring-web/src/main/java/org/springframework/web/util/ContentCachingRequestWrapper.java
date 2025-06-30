/*
 * Copyright 2002-present the original author or authors.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.FastByteArrayOutputStream;

/**
 * {@link jakarta.servlet.http.HttpServletRequest} wrapper that caches all content read from
 * the {@linkplain #getInputStream() input stream} and {@linkplain #getReader() reader},
 * and allows this content to be retrieved via a {@link #getContentAsByteArray() byte array}.
 *
 * <p>This class acts as an interceptor that only caches content as it is being
 * read but otherwise does not cause content to be read. That means if the request
 * content is not consumed, then the content is not cached, and cannot be
 * retrieved via {@link #getContentAsByteArray()}.
 *
 * <p>Used, for example, by {@link org.springframework.web.filter.AbstractRequestLoggingFilter}.
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 4.1.3
 * @see ContentCachingResponseWrapper
 */
public class ContentCachingRequestWrapper extends HttpServletRequestWrapper {

	private final FastByteArrayOutputStream cachedContent;

	private final @Nullable Integer contentCacheLimit;

	private @Nullable ServletInputStream inputStream;

	private @Nullable BufferedReader reader;


	/**
	 * Create a new ContentCachingRequestWrapper for the given servlet request.
	 * @param request the original servlet request
	 * @param cacheLimit the maximum number of bytes to cache per request;
	 * no limit is set if the value is 0 or less. It is recommended to set a
	 * concrete limit in order to avoid using too much memory.
	 * @since 4.3.6
	 * @see #handleContentOverflow(int)
	 */
	public ContentCachingRequestWrapper(HttpServletRequest request, int cacheLimit) {
		super(request);
		int contentLength = request.getContentLength();
		this.cachedContent = (contentLength > 0 ?
				new FastByteArrayOutputStream((cacheLimit > 0 ? Math.min(contentLength, cacheLimit) : contentLength)) :
				new FastByteArrayOutputStream());
		this.contentCacheLimit = (cacheLimit > 0 ? cacheLimit : null);
	}


	@Override
	public ServletInputStream getInputStream() throws IOException {
		if (this.inputStream == null) {
			this.inputStream = new ContentCachingInputStream(getRequest().getInputStream());
		}
		return this.inputStream;
	}

	@Override
	public String getCharacterEncoding() {
		String enc = super.getCharacterEncoding();
		return (enc != null ? enc : WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	@Override
	public BufferedReader getReader() throws IOException {
		if (this.reader == null) {
			this.reader = new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding()));
		}
		return this.reader;
	}

	@Override
	public String getParameter(String name) {
		if (this.cachedContent.size() == 0 && isFormPost()) {
			writeRequestParametersToCachedContent();
		}
		return super.getParameter(name);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		if (this.cachedContent.size() == 0 && isFormPost()) {
			writeRequestParametersToCachedContent();
		}
		return super.getParameterMap();
	}

	@Override
	public Enumeration<String> getParameterNames() {
		if (this.cachedContent.size() == 0 && isFormPost()) {
			writeRequestParametersToCachedContent();
		}
		return super.getParameterNames();
	}

	@Override
	public String[] getParameterValues(String name) {
		if (this.cachedContent.size() == 0 && isFormPost()) {
			writeRequestParametersToCachedContent();
		}
		return super.getParameterValues(name);
	}


	private boolean isFormPost() {
		String contentType = getContentType();
		return (contentType != null && contentType.contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE) &&
				HttpMethod.POST.matches(getMethod()));
	}

	private void writeRequestParametersToCachedContent() {
		try {
			if (this.cachedContent.size() == 0) {
				String requestEncoding = getCharacterEncoding();
				Map<String, String[]> form = super.getParameterMap();
				for (Iterator<String> nameIterator = form.keySet().iterator(); nameIterator.hasNext(); ) {
					String name = nameIterator.next();
					List<String> values = Arrays.asList(form.get(name));
					for (Iterator<String> valueIterator = values.iterator(); valueIterator.hasNext(); ) {
						String value = valueIterator.next();
						this.cachedContent.write(URLEncoder.encode(name, requestEncoding).getBytes());
						if (value != null) {
							this.cachedContent.write('=');
							this.cachedContent.write(URLEncoder.encode(value, requestEncoding).getBytes());
							if (valueIterator.hasNext()) {
								this.cachedContent.write('&');
							}
						}
					}
					if (nameIterator.hasNext()) {
						this.cachedContent.write('&');
					}
				}
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to write request parameters to cached content", ex);
		}
	}

	/**
	 * Return the cached request content as a byte array.
	 * <p>The returned array will never be larger than the content cache limit.
	 * <p><strong>Note:</strong> The byte array returned from this method
	 * reflects the amount of content that has been read at the time when it
	 * is called. If the application does not read the content, this method
	 * returns an empty array.
	 * @see #ContentCachingRequestWrapper(HttpServletRequest, int)
	 */
	public byte[] getContentAsByteArray() {
		return this.cachedContent.toByteArray();
	}

	/**
	 * Return the cached request content as a String, using the configured
	 * {@link Charset}.
	 * <p><strong>Note:</strong> The String returned from this method
	 * reflects the amount of content that has been read at the time when it
	 * is called. If the application does not read the content, this method
	 * returns an empty String.
	 * @since 6.1
	 * @see #getContentAsByteArray()
	 */
	public String getContentAsString() {
		return this.cachedContent.toString(Charset.forName(getCharacterEncoding()));
	}

	/**
	 * Template method for handling a content overflow: specifically, a request
	 * body being read that exceeds the specified content cache limit.
	 * <p>The default implementation is empty. Subclasses may override this to
	 * throw a payload-too-large exception or the like.
	 * @param contentCacheLimit the maximum number of bytes to cache per request
	 * which has just been exceeded
	 * @since 4.3.6
	 * @see #ContentCachingRequestWrapper(HttpServletRequest, int)
	 */
	protected void handleContentOverflow(int contentCacheLimit) {
	}


	private class ContentCachingInputStream extends ServletInputStream {

		private final ServletInputStream is;

		private boolean overflow = false;

		public ContentCachingInputStream(ServletInputStream is) {
			this.is = is;
		}

		@Override
		public int read() throws IOException {
			int ch = this.is.read();
			if (ch != -1 && !this.overflow) {
				if (contentCacheLimit != null && cachedContent.size() == contentCacheLimit) {
					this.overflow = true;
					handleContentOverflow(contentCacheLimit);
				}
				else {
					cachedContent.write(ch);
				}
			}
			return ch;
		}

		@Override
		public int read(byte[] b) throws IOException {
			int count = this.is.read(b);
			writeToCache(b, 0, count);
			return count;
		}

		private void writeToCache(final byte[] b, final int off, int count) throws IOException{
			if (!this.overflow && count > 0) {
				if (contentCacheLimit != null &&
						count + cachedContent.size() > contentCacheLimit) {
					this.overflow = true;
					cachedContent.write(b, off, contentCacheLimit - cachedContent.size());
					handleContentOverflow(contentCacheLimit);
					return;
				}
				cachedContent.write(b, off, count);
			}
		}

		@Override
		public int read(final byte[] b, final int off, final int len) throws IOException {
			int count = this.is.read(b, off, len);
			writeToCache(b, off, count);
			return count;
		}

		@Override
		public int readLine(final byte[] b, final int off, final int len) throws IOException {
			int count = this.is.readLine(b, off, len);
			writeToCache(b, off, count);
			return count;
		}

		@Override
		public boolean isFinished() {
			return this.is.isFinished();
		}

		@Override
		public boolean isReady() {
			return this.is.isReady();
		}

		@Override
		public void setReadListener(ReadListener readListener) {
			this.is.setReadListener(readListener);
		}
	}

}
