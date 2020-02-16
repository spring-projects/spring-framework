/*
 * Copyright 2002-2020 the original author or authors.
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
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;

/**
 * {@link javax.servlet.http.HttpServletRequest} wrapper that caches all content from
 * the {@linkplain #getInputStream() input stream} and {@linkplain #getReader() reader},
 * and make {@linkplain #getInputStream() input stream} and {@linkplain #getReader() reader}
 * can be repeatable read, and allows this content to be retrieved via a
 * {@link #getContentAsByteArray() byte array},
 *
 * <p>Used e.g. by {@link org.springframework.web.filter.RepeatableReadInputStreamAndContentCacheFilter}.
 *
 * @author Zhilong Li
 */
public class RepeatableReadInputStreamAndContentCacheRequestWrapper extends HttpServletRequestWrapper {

	private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";


	@Nullable
	private byte[] contentBytes;

	private HttpServletRequest request;


	/**
	 * Create a new RepeatableReadInputStreamAndContentCacheRequestWrapper for the given servlet request.
	 *
	 * @param request the original servlet request
	 */
	public RepeatableReadInputStreamAndContentCacheRequestWrapper(HttpServletRequest request) {
		super(request);
		this.request = request;
	}

	@Override
	public ServletInputStream getInputStream() {
		cacheContentAndInitParams();
		return new ByteServletInputStream(contentBytes);
	}

	@Override
	public String getCharacterEncoding() {
		String enc = super.getCharacterEncoding();
		return (enc != null ? enc : WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	@Override
	public BufferedReader getReader() throws IOException {
		cacheContentAndInitParams();
		return new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding()));
	}

	private void cacheContentAndInitParams() {
		if (contentBytes != null)
			return;

		if (isFormPost()) {
			writeRequestParametersToCachedContent();
		} else {
			try {
				contentBytes = IOUtils.toByteArray(request.getInputStream());
			} catch (IOException e) {
				throw new IllegalStateException("Failed to read request inputStream to cached content", e);
			}

		}
	}

	@Override
	public String getParameter(String name) {
		cacheContentAndInitParams();
		return super.getParameter(name);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		cacheContentAndInitParams();
		return super.getParameterMap();
	}

	@Override
	public Enumeration<String> getParameterNames() {
		cacheContentAndInitParams();
		return super.getParameterNames();
	}

	@Override
	public String[] getParameterValues(String name) {
		cacheContentAndInitParams();
		return super.getParameterValues(name);
	}

	private boolean isFormPost() {
		String contentType = getContentType();
		return (contentType != null && contentType.contains(FORM_CONTENT_TYPE) &&
				HttpMethod.POST.matches(getMethod()));
	}

	private void writeRequestParametersToCachedContent() {
		try {
			if (this.contentBytes == null) {
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				String requestEncoding = getCharacterEncoding();
				Map<String, String[]> form = super.getParameterMap();
				for (Iterator<String> nameIterator = form.keySet().iterator(); nameIterator.hasNext(); ) {
					String name = nameIterator.next();
					List<String> values = Arrays.asList(form.get(name));
					for (Iterator<String> valueIterator = values.iterator(); valueIterator.hasNext(); ) {
						String value = valueIterator.next();
						byteArrayOutputStream.write(URLEncoder.encode(name, requestEncoding).getBytes());
						if (value != null) {
							byteArrayOutputStream.write('=');
							byteArrayOutputStream.write(URLEncoder.encode(value, requestEncoding).getBytes());
							if (valueIterator.hasNext()) {
								byteArrayOutputStream.write('&');
							}
						}
					}
					if (nameIterator.hasNext()) {
						byteArrayOutputStream.write('&');
					}
				}
				contentBytes = byteArrayOutputStream.toByteArray();
			}
		} catch (IOException ex) {
			throw new IllegalStateException("Failed to write request parameters to cached content", ex);
		}
	}

	/**
	 * Return the cached request content as a byte array.
	 */
	public byte[] getContentAsByteArray() {
		return this.contentBytes;
	}


	private class ByteServletInputStream extends ServletInputStream {

		private final InputStream byteInputStream;

		private ByteServletInputStream(byte[] content) {
			this.byteInputStream = new ByteArrayInputStream(content);
		}

		@Override
		public boolean isFinished() {
			return true;
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setReadListener(ReadListener readListener) {

		}

		@Override
		public int read() throws IOException {
			return this.byteInputStream.read();
		}

		@Override
		public void close() throws IOException {
			super.close();
			byteInputStream.close();
		}
	}
}
