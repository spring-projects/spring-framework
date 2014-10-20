/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.mock.web.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import javax.servlet.http.Part;

import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * Mock implementation of the {@link Part} interface.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see MockHttpServletRequest
 */
public class MockPart implements Part {

	private static final String CONTENT_TYPE = "Content-Type";

	private final String name;

	private String contentType;

	private final byte[] content;

	/**
	 * Create a new MockPart with the given content.
	 * @param name the name of the part
	 * @param content the content for the part
	 */
	public MockPart(String name, byte[] content) {
		this(name, "", content);
	}

	/**
	 * Create a new MockPart with the given content.
	 * @param name the name of the part
	 * @param contentStream the content of the part as stream
	 * @throws IOException if reading from the stream failed
	 */
	public MockPart(String name, InputStream contentStream) throws IOException {
		this(name, "", FileCopyUtils.copyToByteArray(contentStream));
	}

	/**
	 * Create a new MockPart with the given content.
	 * @param name the name of the file
	 * @param contentType the content type (if known)
	 * @param content the content of the file
	 */
	public MockPart(String name, String contentType, byte[] content) {
		Assert.hasLength(name, "Name must not be null");
		this.name = name;
		this.contentType = contentType;
		this.content = (content != null ? content : new byte[0]);
	}

	/**
	 * Create a new MockPart with the given content.
	 * @param name the name of the file
	 * @param contentType the content type (if known)
	 * @param contentStream the content of the part as stream
	 * @throws IOException if reading from the stream failed
	 */
	public MockPart(String name, String contentType, InputStream contentStream)
			throws IOException {

		this(name, contentType, FileCopyUtils.copyToByteArray(contentStream));
	}


	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getContentType() {
		return this.contentType;
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
	public String getHeader(String name) {
		if (CONTENT_TYPE.equalsIgnoreCase(name)) {
			return this.contentType;
		}
		else {
			return null;
		}
	}

	@Override
	public Collection<String> getHeaders(String name) {
		if (CONTENT_TYPE.equalsIgnoreCase(name)) {
			return Collections.singleton(this.contentType);
		}
		else {
			return null;
		}
	}

	@Override
	public Collection<String> getHeaderNames() {
		return Collections.singleton(CONTENT_TYPE);
	}

	@Override
	public void write(String fileName) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete() throws IOException {
		throw new UnsupportedOperationException();
	}

}
