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
package org.springframework.test.web.client.response;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.util.Assert;

/**
 * A {@code ResponseCreator} with builder-style methods for adding response details.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class DefaultResponseCreator implements ResponseCreator {

	private byte[] content;

	private Resource contentResource;

	private final HttpHeaders headers = new HttpHeaders();

	private HttpStatus statusCode;


	/**
	 * Protected constructor.
	 * Use static factory methods in {@link MockRestResponseCreators}.
	 */
	protected DefaultResponseCreator(HttpStatus statusCode) {
		Assert.notNull(statusCode);
		this.statusCode = statusCode;
	}

	@Override
	public ClientHttpResponse createResponse(ClientHttpRequest request) throws IOException {
		MockClientHttpResponse response;
		if (this.contentResource != null ){
			InputStream stream = this.contentResource.getInputStream();
			response = new MockClientHttpResponse(stream, this.statusCode);
		}
		else {
			response = new MockClientHttpResponse(this.content, this.statusCode);
		}
		response.getHeaders().putAll(this.headers);
		return response;
	}

	/**
	 * Set the body as a UTF-8 String.
	 */
	public DefaultResponseCreator body(String content) {
		try {
			this.content = content.getBytes("UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			// should not happen, UTF-8 is always supported
			throw new IllegalStateException(e);
		}
		return this;
	}

	/**
	 * Set the body as a byte array.
	 */
	public DefaultResponseCreator body(byte[] content) {
		this.content = content;
		return this;
	}

	/**
	 * Set the body as a {@link Resource}.
	 */
	public DefaultResponseCreator body(Resource resource) {
		this.contentResource = resource;
		return this;
	}

	/**
	 * Set the {@code Content-Type} header.
	 */
	public DefaultResponseCreator contentType(MediaType mediaType) {
		if (mediaType != null) {
			this.headers.setContentType(mediaType);
		}
		return this;
	}

	/**
	 * Set the {@code Location} header.
	 */
	public DefaultResponseCreator location(URI location) {
		this.headers.setLocation(location);
		return this;
	}

	/**
	 * Copy all given headers.
	 */
	public DefaultResponseCreator headers(HttpHeaders headers) {
		for (String headerName : headers.keySet()) {
			for (String headerValue : headers.get(headerName)) {
				this.headers.add(headerName, headerValue);
			}
		}
		return this;
	}

}
