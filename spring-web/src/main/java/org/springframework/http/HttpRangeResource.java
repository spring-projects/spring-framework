/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Holder that combines a {@link Resource} descriptor with {@link HttpRange}
 * information to be used for reading selected parts of the resource.
 *
 * <p>Used as an argument for partial conversion operations in
 * {@link org.springframework.http.converter.ResourceHttpMessageConverter}.
 *
 * @author Brian Clozel
 * @since 4.3
 * @see HttpRange
 */
public class HttpRangeResource implements Resource {

	private final List<HttpRange> httpRanges;

	private final Resource resource;


	public HttpRangeResource(List<HttpRange> httpRanges, Resource resource) {
		Assert.notEmpty(httpRanges, "List of HTTP Ranges should not be empty");
		this.httpRanges = httpRanges;
		this.resource = resource;
	}


	/**
	 * Return the list of HTTP (byte) ranges describing the requested
	 * parts of the Resource, as provided by the HTTP Range request.
	 */
	public final List<HttpRange> getHttpRanges() {
		return this.httpRanges;
	}


	@Override
	public boolean exists() {
		return this.resource.exists();
	}

	@Override
	public boolean isReadable() {
		return this.resource.isReadable();
	}

	@Override
	public boolean isOpen() {
		return this.resource.isOpen();
	}

	@Override
	public URL getURL() throws IOException {
		return this.resource.getURL();
	}

	@Override
	public URI getURI() throws IOException {
		return this.resource.getURI();
	}

	@Override
	public File getFile() throws IOException {
		return this.resource.getFile();
	}

	@Override
	public long contentLength() throws IOException {
		return this.resource.contentLength();
	}

	@Override
	public long lastModified() throws IOException {
		return this.resource.lastModified();
	}

	@Override
	public Resource createRelative(String relativePath) throws IOException {
		return this.resource.createRelative(relativePath);
	}

	@Override
	public String getFilename() {
		return this.resource.getFilename();
	}

	@Override
	public String getDescription() {
		return this.resource.getDescription();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return this.resource.getInputStream();
	}

}
