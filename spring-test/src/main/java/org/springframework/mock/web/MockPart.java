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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import jakarta.servlet.http.Part;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Mock implementation of {@code jakarta.servlet.http.Part}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.3.12
 * @see MockHttpServletRequest#addPart
 * @see MockMultipartFile
 */
public class MockPart implements Part {

	private final String name;

	@Nullable
	private final String filename;

	private final byte[] content;

	private final HttpHeaders headers = new HttpHeaders();


	/**
	 * Constructor for a part with a name and content only.
	 * @see #getHeaders()
	 */
	public MockPart(String name, @Nullable byte[] content) {
		this(name, null, content);
	}

	/**
	 * Constructor for a part with a name, filename, and content.
	 * @see #getHeaders()
	 */
	public MockPart(String name, @Nullable String filename, @Nullable byte[] content) {
		this(name, filename, content, null);
	}

	/**
	 * Constructor for a part with a name, filename, content, and content type.
	 * @since 6.1.2
	 * @see #getHeaders()
	 */
	public MockPart(String name, @Nullable String filename, @Nullable byte[] content, @Nullable MediaType contentType) {
		Assert.hasLength(name, "'name' must not be empty");
		this.name = name;
		this.filename = filename;
		this.content = (content != null ? content : new byte[0]);
		this.headers.setContentDispositionFormData(name, filename);
		this.headers.setContentType(contentType);
	}


	@Override
	public String getName() {
		return this.name;
	}

	@Override
	@Nullable
	public String getSubmittedFileName() {
		return this.filename;
	}

	@Override
	@Nullable
	public String getContentType() {
		MediaType contentType = this.headers.getContentType();
		return (contentType != null ? contentType.toString() : null);
	}

	@Override
	public long getSize() {
		return this.content.length;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(this.content);
	}

	@Override
	public void write(String fileName) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Nullable
	public String getHeader(String name) {
		return this.headers.getFirst(name);
	}

	@Override
	public Collection<String> getHeaders(String name) {
		Collection<String> headerValues = this.headers.get(name);
		return (headerValues != null ? headerValues : Collections.emptyList());
	}

	@Override
	public Collection<String> getHeaderNames() {
		return this.headers.keySet();
	}

	/**
	 * Return the {@link HttpHeaders} backing header related accessor methods,
	 * allowing for populating selected header entries.
	 */
	public final HttpHeaders getHeaders() {
		return this.headers;
	}

}
